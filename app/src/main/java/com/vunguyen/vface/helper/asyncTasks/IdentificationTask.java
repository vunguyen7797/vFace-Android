/*
 * GroupIdentificationTask.java
 */
package com.vunguyen.vface.helper.asyncTasks;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.TrainingStatus;
import com.vunguyen.vface.bean.Date;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.FaceListViewAdapter;
import com.vunguyen.vface.helper.ProgressDialogCustom;
import com.vunguyen.vface.helper.callbackInterfaces.StudentInterface;
import com.vunguyen.vface.helper.callbackInterfaces.StudentListInterface;
import com.vunguyen.vface.ui.GroupCheckActivity;
import com.vunguyen.vface.ui.SelfCheckActivity;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * This class is to identify faces in a group of people after the detection process, running in background.
 */
public class IdentificationTask extends AsyncTask<UUID, String, IdentifyResult[]>
{
    private ProgressDialog progressDialog;
    private ProgressDialogCustom progressDialogCustom;

    private static final String REQUEST = "SELF_CHECK";
    private boolean succeed = true;
    private String courseServerId;
    private String request;
    private int identifyTurn;   // Index of  this task being executed
    private int totalTurn;      // How many times maximum this task being executed

    private DatabaseReference mDatabase_student;
    private DatabaseReference mDatabase_date;

    private List<String> detectedDetailsList;
    private List<Bitmap> detectedFacesList;

    private List<Pair<Bitmap, String>> displayIdentifiedList;        // store detected In-class Students
    private static List<Pair<Bitmap, String>> displayUnknownList;    // store detected Unknown Students

    @SuppressLint("StaticFieldLeak")
    private Context context;

    IdentificationTask(String courseServerId, int identifyTurn, int totalTurn
            , List<Bitmap> detectedFacesList, List<String> detectedDetailsList, Context context, String request)
    {
        this.context = context;
        this.courseServerId = courseServerId;
        this.identifyTurn = identifyTurn;
        this.totalTurn = totalTurn;
        this.request = request;

        this.detectedDetailsList = detectedDetailsList;
        this.detectedFacesList = detectedFacesList;

        progressDialogCustom =  new ProgressDialogCustom((Activity) this.context);
        progressDialog = new ProgressDialog(this.context);

        displayIdentifiedList = new ArrayList<>();
        displayUnknownList = new ArrayList<>();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        String account = Objects.requireNonNull(user.getEmail()).replaceAll("[.]", "");

        mDatabase_student = FirebaseDatabase.getInstance().getReference().child(account).child("student");
        mDatabase_date = FirebaseDatabase.getInstance().getReference().child(account).child("date");
    }

    IdentificationTask(String courseServerId, Context context, String request)
    {
        this.context = context;
        this.courseServerId = courseServerId;
        this.request = request;

        progressDialogCustom =  new ProgressDialogCustom((Activity) this.context);
        progressDialog = new ProgressDialog(this.context);
        displayIdentifiedList = new ArrayList<>();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        String account = Objects.requireNonNull(user.getEmail()).replaceAll("[.]", "");

        mDatabase_student = FirebaseDatabase.getInstance().getReference().child(account).child("student");
        mDatabase_date = FirebaseDatabase.getInstance().getReference().child(account).child("date");
    }

    @Override
    protected IdentifyResult[] doInBackground(UUID... params)
    {
        // Connect to server
        FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
        try
        {
            TrainingStatus trainingStatus = faceServiceClient.getLargePersonGroupTrainingStatus(
                    this.courseServerId);
            if (trainingStatus.status != TrainingStatus.Status.Succeeded)
            {
                //progressDialog.setCancelable(true);
                //publishProgress("Course training status is " + trainingStatus.status);
                progressDialogCustom.setCancelable(true);
                progressDialogCustom.dismissDialog();
                succeed = false;
                return null;
            }

            Log.i("EXECUTE", "IDENTIFYING...");
            // Start identification process
            return faceServiceClient.identityInLargePersonGroup(
                    this.courseServerId,
                    params,             // faceId
                    1);  // maximum of candidates can be returned for one student
        }
        catch (Exception e)
        {
            succeed = false;
            //publishProgress(e.getMessage());
            Toast.makeText(this.context, "This course has no students in database", Toast.LENGTH_SHORT).show();
            Log.i("EXECUTE", "IDENTIFY ERROR: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPreExecute() {
        startProgressDialog();
    }

    @Override
    protected void onProgressUpdate(String... values)
    {
        // Show the status of background identify task on screen
       // duringTaskProgressDialog(values[0]);
    }

    // Working on student information to display after identify
    @Override
    protected void onPostExecute(IdentifyResult[] result)
    {
        if (request.equalsIgnoreCase(REQUEST))
        {
            selfCheckPostExecute(succeed, result);
        }
        else
            groupCheckPostExecute(result);

    }

    private void selfCheckPostExecute(boolean succeed, IdentifyResult[] result)
    {
        if (result != null && succeed)
        {
            Log.i("EXECUTE", "Identified successfully: " + result.length + " faces");
            DecimalFormat formatter = new DecimalFormat("#0.00");
            for (IdentifyResult identifyResult : result)
            {
                if (identifyResult.candidates.size() > 0)
                {
                    String studentServerId = identifyResult.candidates.get(0).personId.toString();
                    String confidence = formatter.format(identifyResult.candidates.get(0).confidence);

                    getSingleStudentFireBase(studentServerId, student ->
                    {
                        if (student != null)
                        {
                            String studentName = student.getStudentName();
                            String identity = studentName + "\n" + confidence;

                            // set flag YES to indicate that this student is identified successfully
                            student.setStudentIdentifyFlag("YES");

                            // Process duplicate information for similar faces from different people
                            for (int i = 0; i < SelfCheckActivity.detectedDetailsList.size(); i++)
                            {
                                // Only compared with other identified students available in data
                                if (!SelfCheckActivity.detectedDetailsList.get(i).equalsIgnoreCase("UNKNOWN STUDENT"))
                                {
                                    int length = SelfCheckActivity.detectedDetailsList.get(i).length();

                                    // if there is a duplicate name
                                    if (SelfCheckActivity.detectedDetailsList.get(i).contains(studentName))
                                    {
                                        identity = "UNKNOWN STUDENT";
                                        Toast.makeText(this.context, "This student was already checked", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }

                            if (!"UNKNOWN STUDENT".equalsIgnoreCase(identity))
                            {
                                String date_string = SelfCheckActivity.tvDate.getText().toString();
                                Date date = new Date(courseServerId, studentServerId, date_string, student.getStudentIdentifyFlag());
                                mDatabase_date.child(studentName.toUpperCase() + "-"
                                        + date_string.replaceAll("[,]","") + studentServerId).setValue(date);
                                // update student flag
                                mDatabase_student.child(student.getStudentName().toUpperCase()
                                        + "-" + studentServerId).setValue(student);
                            }
                            SelfCheckActivity.detectedDetailsList.add(identity);
                        }
                        else
                            Log.i("EXECUTE", "Student is null");
                    });

                    new CountDownTimer(1000, 1000)
                    {
                        @Override
                        public void onTick(long millisUntilFinished)
                        {
                            Log.i("EXECUTE", "Please wait for loading student task completed...");
                        }
                        @Override
                        public void onFinish()
                        {
                            for(int i = 0; i < SelfCheckActivity.detectedDetailsList.size(); i++)
                            {
                                if (!"UNKNOWN STUDENT".equalsIgnoreCase(SelfCheckActivity.detectedDetailsList.get(i)))
                                {
                                    Pair<Bitmap, String> pair = new Pair<>(SelfCheckActivity.detectedFacesList.get(i)
                                            , SelfCheckActivity.detectedDetailsList.get(i));
                                    displayIdentifiedList.add(pair);
                                }
                                else
                                    Log.i("EXECUTE", "Student is null, cannot pair");
                            }

                            getAbsenceListFirebase(courseServerId, absentList ->
                            {
                                for (Student student : absentList)
                                {
                                    String date_string = SelfCheckActivity.tvDate.getText().toString();
                                    Date date = new Date(courseServerId, student.getStudentServerId()
                                            , date_string, student.getStudentIdentifyFlag());
                                    mDatabase_date.child(student.getStudentName().toUpperCase() + "-"
                                            + date_string.replaceAll("[,]", "")+student.getStudentServerId()).setValue(date);
                                }
                            });

                            //progressDialog.dismiss();
                            progressDialogCustom.dismissDialog();
                            // wait until all async tasks completed to update UI view
                            new CountDownTimer(1000, 1000)
                            {
                                @Override
                                public void onTick(long millisUntilFinished)
                                {
                                    Log.i("EXECUTE", "Please wait for all tasks complete...");
                                }
                                @Override
                                public void onFinish()
                                {
                                    Log.i("EXECUTE", "Updating UI....");
                                    setUiAfterIdentification(succeed, displayIdentifiedList, "SELF_CHECK");
                                }
                            }.start();
                        }
                    }.start();
                }
                else
                {
                    Log.i("EXECUTE", "STUDENT NULL");
                    progressDialog.dismiss();
                    //progressDialogCustom.dismissDialog();
                    Toast.makeText(this.context, "This student is not identified. \nPlease try again", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void groupCheckPostExecute(IdentifyResult[] result)
    {
        if (result != null)
        {
            Log.i("EXECUTE", "Identified group successfully: " + result.length + " faces");
            int index = identifyTurn * 10;  // position of each identified students in container.
            for (IdentifyResult identifyResult : result)
            {
                DecimalFormat formatter = new DecimalFormat("#0.00");
                if (identifyResult.candidates.size() > 0)
                {
                    String studentServerId = identifyResult.candidates.get(0).personId.toString();
                    String confidence = formatter.format(identifyResult.candidates.get(0).confidence);
                    Log.i("EXECUTE", "Retrieve Data Student Server ID: " + studentServerId);
                    int finalI = index;
                    getSingleStudentFireBase(studentServerId, student -> {
                        if (student != null)
                        {
                            // Get information for each identified student
                            String studentName = student.getStudentName();
                            String identity = studentName+ "\n" + confidence;
                            // set flag YES to indicate that this student is identified successfully
                            student.setStudentIdentifyFlag("YES");

                            // Process duplicate information for similar faces from different people
                            for (int i = 0; i < detectedDetailsList.size(); i++)
                            {
                                // Only compared with other identified students available in data
                                if (!detectedDetailsList.get(i).equalsIgnoreCase("UNKNOWN STUDENT")
                                        && !detectedDetailsList.get(i).equalsIgnoreCase(""))
                                {
                                    int length = detectedDetailsList.get(i).length();
                                    // get the confidence to compare
                                    String comparedConfidence = detectedDetailsList.get(i).substring(length - 4, length);
                                    // if there is a duplicate name
                                    if (detectedDetailsList.get(i).contains(studentName))
                                    {
                                        // available confidence < this student confidence
                                        // means that the available student was identified wrong
                                        if (comparedConfidence.compareToIgnoreCase(confidence) < 0)
                                        {
                                            // set the student in list as UNKNOWN STUDENT
                                            detectedDetailsList.set(i, "UNKNOWN STUDENT");
                                        }
                                        else if (comparedConfidence.compareToIgnoreCase(confidence) > 0)
                                        {
                                            // if the confidence less than the student in list
                                            // this student is identified as unknown
                                            identity = "UNKNOWN STUDENT";
                                        }
                                    }
                                }
                            }

                            if (!identity.equalsIgnoreCase("UNKNOWN STUDENT"))
                            {
                                String date_string = GroupCheckActivity.tvDate.getText().toString();
                                Date date = new Date(courseServerId, studentServerId,
                                        date_string, student.getStudentIdentifyFlag());
                                // update date of attendance for student
                                mDatabase_date.child(studentName.toUpperCase() + "-"
                                        + date_string.replaceAll("[,]","") + studentServerId).setValue(date);
                                // update student flag
                                mDatabase_student.child(student.getStudentName().toUpperCase()
                                        + "-" + studentServerId).setValue(student);
                            }
                            else
                                Log.i("EXECUTE", "UNKNOWN STUDENT");

                            detectedDetailsList.set(finalI, identity);  // add new student identity into container
                        }
                        else
                        {
                            detectedDetailsList.set(finalI, "UNKNOWN STUDENT");
                        }
                    });
                }
                else
                {
                    detectedDetailsList.set(index, "UNKNOWN STUDENT");
                    Log.i("EXECUTE", "UNKNOWN STUDENT: " + index);
                }
                index++;
            }

            // wait until the identify task is done for all students
            new CountDownTimer(100 * result.length, 1000)
            {
                @Override
                public void onTick(long millisUntilFinished) {
                    Log.i("EXECUTE", "Please wait...");
                }

                @Override
                public void onFinish()
                {
                    //progressDialog.dismiss();
                    progressDialogCustom.dismissDialog();
                    if (identifyTurn == totalTurn)
                    {
                        Log.i("EXECUTE", "Turn equal ==");
                        int i = 0;
                        for (String info : detectedDetailsList)
                        {
                            Pair<Bitmap, String> pair = new Pair<>(detectedFacesList.get(i), info);
                            if (info.equals("UNKNOWN STUDENT"))
                            {
                                // add unknown student to unknown list
                                displayUnknownList.add(pair);
                                Log.i("EXECUTE", "Unknown on Finish: " + i);
                            }
                            else
                            {
                                // add identified student to identified student list
                                displayIdentifiedList.add(pair);
                                Log.i("EXECUTE", "Pair on Finish: " + i);
                            }
                            i++;
                        }

                        getAbsenceListFirebase(courseServerId, absentList -> {
                            for (Student student : absentList)
                            {
                                String date_string = GroupCheckActivity.tvDate.getText().toString();
                                Date date = new Date(courseServerId, student.getStudentServerId()
                                        , date_string, student.getStudentIdentifyFlag());
                                mDatabase_date.child(student.getStudentName().toUpperCase() + "-"
                                        + date_string.replaceAll("[,]", "") + student.getStudentServerId()).setValue(date);
                            }
                        });

                        // wait until all async tasks completed to update UI view
                        new CountDownTimer(1000, 1000)
                        {
                            @Override
                            public void onTick(long millisUntilFinished)
                            {
                                Log.i("EXECUTE", "Please wait for all tasks complete...");
                            }
                            @Override
                            public void onFinish()
                            {
                                Log.i("EXECUTE", "Updating UI....");
                                setUiAfterIdentification(succeed, displayIdentifiedList, "GROUP_CHECK");
                            }
                        }.start();
                    }
                }
            }.start();
        }
        else
        {
            Toast.makeText(this.context, "This course has no students in database", Toast.LENGTH_SHORT).show();
        }
    }
    private void startProgressDialog()
    {
        //progressDialogCustom.startProgressDialog("Identifying...");
        //progressDialog.show();
        progressDialogCustom.startProgressDialog("Identifying...");
    }

    private void duringTaskProgressDialog(String progress)
    {
        progressDialog.setMessage(progress);
    }

    // get one student with its student server id
    private void getSingleStudentFireBase(String studentServerId, StudentInterface callback)
    {
        mDatabase_student.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                Student student = null;
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Student temp = dsp.getValue(Student.class);
                    assert temp != null;
                    if (temp.getStudentServerId().equalsIgnoreCase(studentServerId))
                        student = temp;
                }
                callback.getStudent(student);
                mDatabase_student.removeEventListener(this);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // get a list of absent students in a course
    private void getAbsenceListFirebase(String courseServerId, StudentListInterface callback)
    {
        mDatabase_student.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                List<Student> absentList = new ArrayList<>();
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Student student = dsp.getValue(Student.class);
                    assert student != null;
                    if (student.getCourseServerId().equalsIgnoreCase(courseServerId)
                            && student.getStudentIdentifyFlag().equalsIgnoreCase("NO"))
                        absentList.add(student);
                }
                try
                {
                    callback.getStudentList(absentList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mDatabase_student.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {
            }
        });
    }

    // display the data on list view after identification task completed
    private void setUiAfterIdentification(boolean succeed, List<Pair<Bitmap, String>> studentIdentityList, String request)
    {
        if (succeed)
        {
            //identifyTaskDone = true; // identify process is done for all faces
            FaceListViewAdapter listViewStudentsAdapter = new FaceListViewAdapter(studentIdentityList, this.context);

            if (request.equalsIgnoreCase("GROUP_CHECK"))
            {
                if (studentIdentityList.size() == 0)
                {
                    Toast.makeText(context, "No students are in class today.", Toast.LENGTH_LONG).show();
                }
                else
                {
                    GroupCheckActivity.ivWaitingIdentify.setVisibility(View.GONE);
                    Toast.makeText(context, studentIdentityList.size() + " students are in class today."
                            , Toast.LENGTH_LONG).show();
                }
                GroupCheckActivity.lvIdentifiedFaces.setAdapter(listViewStudentsAdapter);
                GroupCheckActivity.displayUnknownList = displayUnknownList;
                GroupCheckActivity.displayIdentifiedList = displayIdentifiedList;
                Log.i("EXECUTE", "Unknown list size: " + GroupCheckActivity.displayUnknownList);
            }
            else
            {
                SelfCheckActivity.ivWaitingIdentify.setVisibility(View.GONE);
                SelfCheckActivity.listView.setAdapter(listViewStudentsAdapter);
            }
        }
        else
        {

        }
    }
}

