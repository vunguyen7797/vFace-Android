/*
 * SelfCheckActivity.java
 */
package com.vunguyen.vface.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.TrainingStatus;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.ImageEditor;
import com.vunguyen.vface.helper.LocaleHelper;
import com.vunguyen.vface.helper.MyDatabaseHelperCourse;
import com.vunguyen.vface.helper.MyDatabaseHelperDate;
import com.vunguyen.vface.helper.MyDatabaseHelperFace;
import com.vunguyen.vface.helper.MyDatabaseHelperStudent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * This class implements feature for self checking in the class.
 * So each student will check in by themselves with this mode.
 */
public class SelfCheckActivity extends AppCompatActivity
{
    String account;
    Course course;
    String courseServerId;
    String identityInfo;
    String date;
    Bitmap bitmapImage;
    Bitmap bitmapThumbnail;
    boolean detected;

    // UI features
    TextView tvDate;
    AutoCompleteTextView courseMenu;
    ProgressDialog progressDialog;
    Button btnFaceCheck;
    ImageView ivWaitingIdentify;

    // Database
    MyDatabaseHelperStudent db_student;
    MyDatabaseHelperFace db_face;
    MyDatabaseHelperCourse db_course;
    MyDatabaseHelperDate db_date;

    // Data lists
    List<Course> courseList;            // store list of courses objects
    List<Student> studentList;          // store list of students objects
    List<Face> facesList;               // store face objects
    List<Student> absenceStudentList;   // store list of absent students objects
    List<String> detectedDetailsList;   // store the students' information after detection
    List<Bitmap> detectedFacesList;     // store the students' face in Bitmap after detection
    List<IdentifyResult[]> identifyResultsList; // store list of identify results after identify tasks
    List<Pair<Bitmap, String>> studentIdentityList;         // store detected In-class Students
    List<Pair<Bitmap, String>> absenceStudentDisplayList;   // store detected Absent Students
    Pair<Bitmap, String> studentIdentity;

    FaceListViewAdapter listViewAdapter;

    // Request codes
    private static final int REQUEST_TAKE_PHOTO = 0;
    // The URI of photo taken with camera
    private Uri uriTakenPhoto;

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_check);

        // Get email account
        account = getIntent().getStringExtra("ACCOUNT");

        tvDate = findViewById(R.id.tvDate);
        setDate(tvDate);
        date = tvDate.getText().toString();

        courseList = new ArrayList<>();
        studentList = new ArrayList<>();
        facesList = new ArrayList<>();
        absenceStudentList = new ArrayList<>();
        studentIdentityList = new ArrayList<>();
        absenceStudentDisplayList = new ArrayList<>();
        detectedFacesList = new ArrayList<>();
        detectedDetailsList = new ArrayList<>();
        identifyResultsList = new ArrayList<>();
        studentIdentity = new Pair<>(null, "");

        db_student = new MyDatabaseHelperStudent(this);
        db_course = new MyDatabaseHelperCourse(this);
        db_date = new MyDatabaseHelperDate(this);
        // get all courses available
        this.courseList = db_course.getAllCourses(account);
        courseMenu = findViewById(R.id.filled_exposed_dropdown);
        // display data on spinners
        displayCourseMenu(courseList);

        btnFaceCheck = findViewById(R.id.btnFaceCheck);
        ivWaitingIdentify = findViewById(R.id.ivWaitingIdentify);

        // initialize the progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("V.FACE");
    }

    // Response on result of option which was just executed
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_TAKE_PHOTO == requestCode && resultCode == RESULT_OK)
        {
            uriTakenPhoto = data.getData();
            Toast.makeText(getApplicationContext(), uriTakenPhoto.toString(), Toast.LENGTH_SHORT).show();
            detect(uriTakenPhoto);
        }
    }

    // This method is used to display course list on menu
    private void displayCourseMenu(List<Course> courseList)
    {
        // initialize course menu
        ArrayAdapter<Course> tvArrayAdapter = new ArrayAdapter<Course>(this, R.layout.dropdown_menu_popup_item, courseList);
        courseMenu.setAdapter(tvArrayAdapter);

        if (courseList.size() > 0)
        {
            courseMenu.setOnItemClickListener((parent, view, position, id) ->
            {
                course = (Course) parent.getItemAtPosition(position);
                courseServerId = course.getCourseServerId();    // get course id on server
                // get all students in the course
                studentList = db_student.getStudentWithCourse(courseServerId);
                Log.i("EXECUTE", "Course Selected: " + courseServerId);

                btnFaceCheck.setEnabled(true);
                resetAllData();

                List<Student> temp = db_student.getAbsenceStudent(courseServerId);
                for(Student student: temp)
                {
                    Log.i("EXECUTE", "NAME: " + student.getStudentName() + " FLAG: " + student.getStudentIdentifyFlag());
                }

            });
        }
        else
        {
            Log.i("EXECUTE", "No courses are available.");
        }

    }

    // This method is used to reset all the lists or databases into the initial status for a new task
    private void resetAllData()
    {
        // reset data lists
        absenceStudentDisplayList.clear();
        // reset student identify flag to "NO" value
        if (tvDate.getText().toString().equalsIgnoreCase(date) && courseMenu.getText().toString().equalsIgnoreCase(course.getCourseName()))
        {
            for(Student student : studentList)
                db_date.deleteADate(student.getStudentServerId(), student.getCourseServerId(), date);
        }
        db_student.resetStudentFlag(studentList);
    }

    public void btnBackClick(View view)
    {
        onBackPressed();
    }

    // Click back button on navigation bar
    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(SelfCheckActivity.this, DashBoardActivity.class);
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
        finish();
    }

    // set the date on screen
    public void setDate(TextView view)
    {
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        String date = formatter.format(today);
        view.setText(date);
    }

    public void btnFaceCheck(View view)
    {
        Intent intent = new Intent(this, RealTimeFaceDetectActivity.class);
        startActivityForResult(intent, REQUEST_TAKE_PHOTO);
    }

    // This method is to detect faces in the bitmap photo
    private void detect(Uri uriImage)
    {
        try
        {
            bitmapImage = ImageEditor.handlePhotoAndRotationBitmap(this, uriImage);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // Put the image into an input stream for detection task input.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        // Start a background task to detect faces in the image.
        new DetectionTask().execute(inputStream);
        Log.i("EXECUTE","Detected value?: " + detected);
    }

    public void identify()
    {
        // Start detection task only if the image to detect is selected.
        if (detected && courseServerId != null) {
            // Start a background task to identify faces in the image.
            List<UUID> faceIdList = new ArrayList<>();
            for (Face face:  facesList) {
                faceIdList.add(face.faceId);
            }

            new IdentificationTask(courseServerId).execute(
                    faceIdList.toArray(new UUID[faceIdList.size()]));
        }
        else
            {
            // Not detected or person group exists.
            Log.i("EXECUTE","Please select an image and create course first.");
        }
    }

    /**
     * This class is a background task to detect faces from the photo
     */
    class DetectionTask extends AsyncTask<InputStream, String, Face[]>
    {
        @Override
        protected Face[] doInBackground(InputStream... params)
        {
            // Connect to server
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                publishProgress("Detecting...");
                Log.i("EXECUTE", "DETECTING FACE");

                // Start detection process
                return faceServiceClient.detect(
                        params[0],  // stream of image to detect
                        true,    // return face ID
                        false,  // don't return face landmarks
                        null);
            }
            catch (Exception e)
            {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute()
        {
            startProgressDialog();
        }

        @Override
        protected void onProgressUpdate(String... values)
        {
            // Show the status of background detection task on screen
            duringTaskProgressDialog(values[0]);
        }

        @Override
        protected void onPostExecute(Face[] result)
        {
            if (result != null)
            {
                for (Face face : result)
                {
                    try
                    {
                        // generate thumbnails of faces and add to the list
                        //detectedFacesList.add(ImageEditor.generateFaceThumbnail(bitmapImage, face.faceRectangle));
                        bitmapThumbnail = ImageEditor.generateFaceThumbnail(bitmapImage, face.faceRectangle);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                facesList = Arrays.asList(result);

                if (result.length == 0)
                {
                    detected = false;
                    Log.i("EXECUTE", "NO FACE DETECTED!");
                }
                else
                {
                    Log.i("EXECUTE", "FACE DETECTED!");
                    detected = true;
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Your face was detected!" + result.length, Toast.LENGTH_SHORT).show();
                    //new IdentificationTask(courseServerId).execute(1);
                    identify();
                }
            }
            else
            {
                detected = false;
                Log.i("EXECUTE", "FACE DETECTION FAILED!");
            }
        }
    }

    // display the progress dialog when a task is processing
    private void startProgressDialog()
    {
        progressDialog.show();
    }

    private void duringTaskProgressDialog(String progress)
    {
        progressDialog.setMessage(progress);
    }

    /**
     * This class is a customize adapter to display identified students
     * after detection and identification
     */
    class FaceListViewAdapter implements ListAdapter
    {
        List<Bitmap> faceThumbnails;
        List<String> studentInfo;

        public FaceListViewAdapter()
        {
        }

        public FaceListViewAdapter(List<Pair<Bitmap, String>> studentIdentityList)
        {
            faceThumbnails = new ArrayList<>();
            studentInfo = new ArrayList<>();

            if (studentIdentityList != null)
            {
                Log.i("EXECUTE", Integer.toString(studentIdentityList.size()));
                for (Pair<Bitmap, String> pair : studentIdentityList)
                {
                    faceThumbnails.add(pair.first);

                    if (!pair.second.equalsIgnoreCase("UNKNOWN STUDENT"))
                    {
                        int index = pair.second.indexOf('\n');
                        studentInfo.add(pair.second.substring(0, index));
                        Log.i("EXECUTE", pair.second.substring(0, index));
                    }
                    else
                        studentInfo.add(pair.second);
                }
            }
        }

        @Override
        public boolean areAllItemsEnabled()
        {
            return false;
        }

        @Override
        public boolean isEnabled(int position)
        {
            return true;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer)
        {

        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer)
        {

        }

        @Override
        public int getCount()
        {
            return faceThumbnails.size();
        }

        @Override
        public Object getItem(int position)
        {
            return faceThumbnails.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public boolean hasStableIds()
        {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if (convertView == null)
            {
                LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_face_with_description, parent, false);
            }

            convertView.setId(position);

            // set face
            ((ImageView) convertView.findViewById(R.id.face_thumbnail)).setImageBitmap(faceThumbnails.get(position));
            //set info
            ((TextView) convertView.findViewById(R.id.tvDetectedFace)).setText(studentInfo.get(position));
            ((TextView) convertView.findViewById(R.id.tvDetectedFace)).setTextColor(Color.WHITE);

            return convertView;
        }

        @Override
        public int getItemViewType(int position)
        {
            return position;
        }

        @Override
        public int getViewTypeCount()
        {
            return 1;
        }

        @Override
        public boolean isEmpty()
        {
            return false;
        }
    }


    /**
     * This class is to identify faces after the detection process, running in background.
     */
    class IdentificationTask extends AsyncTask<UUID, String, IdentifyResult[]>
    {
        private boolean succeed = true;
        String courseServerId;
        Student student;

        IdentificationTask(String courseServerId)
        {
            this.courseServerId = courseServerId;
        }

        @Override
        protected IdentifyResult[] doInBackground(UUID... params)
        {
            // Connect to server
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                publishProgress("Getting course status...");
                TrainingStatus trainingStatus = faceServiceClient.getLargePersonGroupTrainingStatus(
                        this.courseServerId);
                if (trainingStatus.status != TrainingStatus.Status.Succeeded)
                {
                    publishProgress("Course training status is " + trainingStatus.status);
                    succeed = false;
                    return null;
                }

                publishProgress("Identifying...");
                Log.i("EXECUTE", "IDENTIFYING...");
                // Start identification process
                return faceServiceClient.identityInLargePersonGroup(
                        this.courseServerId,
                        params,             // faceId
                        1);              // maximum of candidates can be returned for one student
            }
            catch (Exception e)
            {
                succeed = false;
                publishProgress(e.getMessage());
                Log.i("EXECUTE", "IDENTIFY ERROR: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute()
        {
            startProgressDialog();
        }

        @Override
        protected void onProgressUpdate(String... values)
        {
            // Show the status of background identify task on screen
            duringTaskProgressDialog(values[0]);
        }

        // Working on student information to display after identify
        @Override
        protected void onPostExecute(IdentifyResult[] result)
        {
            if (result != null)
            {
                identifyResultsList.add(result);

                for (IdentifyResult identifyResult : result)
                {
                    DecimalFormat formatter = new DecimalFormat("#0.00");
                    if (identifyResult.candidates.size() > 0)
                    {
                        String studentServerId =
                                identifyResult.candidates.get(0).personId.toString();

                        String confidence = formatter.format(identifyResult.candidates.get(0).confidence);
                        student = db_student.getAStudentWithId(studentServerId, courseServerId);
                        if (student != null)
                        {
                            // Get information for each identified student
                            String studentIdNumber = student.getStudentIdNumber();
                            String studentName = student.getStudentName();
                            identityInfo = studentName+ "\n" + confidence;

                            // set flag YES to indicate that this student is identified successfully
                            student.setStudentIdentifyFlag("YES");
                            // Process duplicate information for similar faces from different people
                            for (int i = 0; i < detectedDetailsList.size(); i++)
                            {
                                // Only compared with other identified students available in data
                                if (!detectedDetailsList.get(i).equalsIgnoreCase("UNKNOWN STUDENT"))
                                {
                                    int length = detectedDetailsList.get(i).length();
                                    // get the confidence to compare
                                    String comparedConfidence = detectedDetailsList
                                            .get(i).substring(length - 4, length);
                                    Log.i("EXECUTE", "NEW CONFIDENCE: " + comparedConfidence + " CONFIDENCE: " + confidence);
                                    // if there is a duplicate name
                                    if (detectedDetailsList.get(i).contains(studentName))
                                    {
                                            identityInfo = "UNKNOWN STUDENT";
                                            Toast.makeText(getApplicationContext(), "This student was already identified.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }

                            if (!"UNKNOWN STUDENT".equalsIgnoreCase(identityInfo))
                            {
                                Log.i("EXECUTE", "ADD DATE");
                                String date_string = tvDate.getText().toString();
                                com.vunguyen.vface.bean.Date date = new com.vunguyen.vface.bean.Date(courseServerId, studentServerId, date_string, student.getStudentIdentifyFlag());
                                db_date.addDate(date);
                                Log.i("EXECUTE", "ADD DATE" + date);
                            }

                            Log.i("EXECUTE", identityInfo + "\n" + student.getStudentIdentifyFlag());
                            db_student.updateStudent(student);  // update student flag
                            detectedDetailsList.add(identityInfo);  // add new student identity into list
                        }
                        else
                            Log.i("EXECUTE", "STUDENT NULL");
                    }
                    else
                    {
                        detectedDetailsList.add("UNKNOWN STUDENT");
                        identityInfo = "UNKNOWN STUDENT";
                        Log.i("EXECUTE", "UNKNOWN STUDENT");
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), "This student is not identified.", Toast.LENGTH_SHORT).show();
                    }
                }

                if (!"UNKNOWN STUDENT".equalsIgnoreCase(identityInfo))
                {
                    studentIdentity = new Pair<>(bitmapThumbnail, identityInfo);
                    studentIdentityList.add(studentIdentity);
                }

                List<Student> absentList = db_student.getAbsenceStudent(courseServerId);
                Log.i("EXECUTE", "ABSENT LIST SIZE: " + absentList.size());
                for (Student student : absentList)
                {
                    Log.i("EXECUTE", "ADD DATE FOR ABSENT STUDENTS");
                    String date_string = tvDate.getText().toString();
                    com.vunguyen.vface.bean.Date date = new com.vunguyen.vface.bean.Date(courseServerId, student.getStudentServerId()
                            , date_string, student.getStudentIdentifyFlag());
                    db_date.addDate(date);
                }

                setUiAfterIdentification(succeed, studentIdentityList);
            }
            else
            {
                Log.i("EXECUTE", "ERROR IDENTIFY....");
            }
        }
    }

    // display the data on list view
    private void setUiAfterIdentification(boolean succeed, List<Pair<Bitmap, String>> studentIdentityList)
    {
        if (succeed)
        {
            progressDialog.dismiss();
            listViewAdapter = new FaceListViewAdapter(studentIdentityList);
            if(studentIdentityList.size() == 0)
                ivWaitingIdentify.setVisibility(View.VISIBLE);
            else
                ivWaitingIdentify.setVisibility(View.INVISIBLE);
            ListView listView = findViewById(R.id.lvIdentifiedFaces);
            listView.setAdapter(listViewAdapter);
        }
    }
}
