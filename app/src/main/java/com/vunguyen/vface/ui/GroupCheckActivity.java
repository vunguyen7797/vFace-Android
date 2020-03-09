/*
 * GroupCheckActivity.java
 */
package com.vunguyen.vface.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.TrainingStatus;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.ImageEditor;
import com.vunguyen.vface.helper.MyDatabaseHelperCourse;
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
import java.util.UUID;

/**
 * This class contains implementations for checking attendance
 * by a group photo.
 * Background tasks implemented to detect faces, and identify faces.
 */
public class GroupCheckActivity extends AppCompatActivity
{
    // UI features
    Spinner spinCourses;
    Spinner spinStudentList;
    private ImageView ivClass;
    ListView lvIdentifiedFaces;
    Bitmap bitmapImage; // face image thumbnail
    TextView tvDate;
    ProgressDialog progressDialog;

    // Request codes
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int GALLERY_REQUEST_CODE = 1;
    private static final int PERMISSION_CODE = 1000;

    // Database
    MyDatabaseHelperStudent db_student;
    MyDatabaseHelperFace db_face;
    MyDatabaseHelperCourse db_course;

    // Data
    Uri uriImage;
    boolean detected;
    boolean studentListChanged = false;
    boolean identifyTaskDone = false;
    String courseServerId;
    String account;

    // Data Adapter
    ArrayAdapter<Course> spinnerArrayAdapter;
    ArrayAdapter<String> spinnerListOptionArrayAdapter;
    FaceListViewAdapter listViewAdapter;

    // Data lists
    List<Course> courseList;            // store list of courses objects
    List<Student> studentList;          // store list of students objects
    List<Student> absenceStudentList;   // store list of absent students objects
    List<Face> facesList;               // store face objects

    List<IdentifyResult[]> identifyResultsList; // store list of identify results after identify tasks

    List<String> detectedDetailsList;   // store the students' information after detection
    List<Bitmap> detectedFacesList;     // store the students' face in Bitmap after detection

    List<Pair<Bitmap, String>> studentIdentityList;         // store detected In-class Students
    List<Pair<Bitmap, String>> absenceStudentDisplayList;   // store detected Absent Students
    List<Pair<Bitmap, String>> identifyUnknownList;         // store detected Unknown Students


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Set no notification bar on activity
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_group_check);

        // get account to identify database
        account = getIntent().getStringExtra("ACCOUNT");

        // initialize all data lists
        identifyResultsList = new ArrayList<>();
        detectedDetailsList = new ArrayList<>();
        detectedFacesList = new ArrayList<>();
        studentIdentityList = new ArrayList<>();
        facesList = new ArrayList<>();
        absenceStudentList = new ArrayList<>();
        absenceStudentDisplayList = new ArrayList<>();
        courseList = new ArrayList<>();
        studentIdentityList = new ArrayList<>();
        identifyUnknownList = new ArrayList<>();

        // set display date
        tvDate = findViewById(R.id.tvDate);
        setDate(tvDate);

        // open the databases
        db_course = new MyDatabaseHelperCourse(this);
        db_face = new MyDatabaseHelperFace(this);
        db_student = new MyDatabaseHelperStudent(this);

        // get all courses available
        this.courseList = db_course.getAllCourses(account);

        // display data on spinners
        displayCourseSpinner(courseList);
        displayStudentListOptionSpinner();

        ivClass = findViewById(R.id.ivClassImage);
        lvIdentifiedFaces = findViewById(R.id.lvIdentifiedFaces);

        // get all students in the course
        studentList = db_student.getStudentWithCourse(courseServerId);

        // initialize the progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("V.FACE");
    }

    // This method is used to display student list options on spinner
    private void displayStudentListOptionSpinner()
    {
        spinStudentList = findViewById(R.id.spinStudentList);
        // There are 3 options to display the student list after identify task
        List<String> studentListOptions = new ArrayList<>();
        studentListOptions.add("IN-CLASS STUDENTS");
        studentListOptions.add("ABSENCE STUDENTS");
        studentListOptions.add("UNKNOWN STUDENTS");

        // initialize Array adapter
        spinnerListOptionArrayAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, studentListOptions)
        {
            @Override
            public boolean isEnabled(int position)
            {
                // disable the unknown students option if no unknown found
                if (identifyUnknownList.size() == 0 && position == 2)
                {
                    return false;
                }
                return true;
            }

            // set the color change
            @Override
            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent)
            {
                // TODO Auto-generated method stub
                View mView = super.getDropDownView(position, convertView, parent);
                TextView mTextView = (TextView) mView;
                if ((identifyUnknownList.size() == 0 && position == 2))
                {
                    mTextView.setTextColor(Color.RED);
                }
                else
                {
                    mTextView.setTextColor(Color.WHITE);
                }
                return mView;
            }
        };

        spinnerListOptionArrayAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinStudentList.setAdapter(spinnerListOptionArrayAdapter);
        spinStudentList.setSelection(0);    // set the first default option is In-class student

        // handling each option selected
        spinStudentList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (position == 2)  // Display Unknown student list
                {
                    listViewAdapter = new FaceListViewAdapter(identifyUnknownList);
                    studentListChanged = true; // default option has been changed
                }
                else if (studentListChanged && position == 0) // Display InClass student list
                {
                    listViewAdapter = new FaceListViewAdapter(studentIdentityList);
                }
                else if (position == 1) // Display Absence student list
                {
                    if (absenceStudentDisplayList.size() > 0)
                        absenceStudentDisplayList.clear();

                    if (identifyTaskDone)
                    {
                        Course course = (Course) spinCourses.getSelectedItem(); // get selected course
                        // get all absent student who has identify flag is NO in database
                        absenceStudentList = db_student.getAbsenceStudent(course.getCourseServerId());
                        if (absenceStudentList.size() != 0)
                        {
                            getAbsentStudentList(course);

                            listViewAdapter = new FaceListViewAdapter(absenceStudentDisplayList);
                            studentListChanged = true;
                        }
                        else
                            Toast.makeText(getApplicationContext(),
                                    "No absent student.", Toast.LENGTH_SHORT).show();
                    }
                    else
                        studentListChanged = false;
                }

                ListView listView = findViewById(R.id.lvIdentifiedFaces);
                listView.setAdapter(listViewAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    // This method is used to get absent students to store in a list for displaying
    private void getAbsentStudentList(Course course)
    {
        for (int i = 0; i < absenceStudentList.size(); i++)
        {
            Bitmap faceThumbnail;

            // Student information
            String studentIdNumber = absenceStudentList.get(i).getStudentIdNumber();
            String studentName = absenceStudentList.get(i).getStudentName();
            String identity = "Student: " + studentName.toUpperCase() + "\n"
                    + "Student ID: " + studentIdNumber + "\n"
                    + "Course: " + course;

            // get face list of the current student
            List<com.vunguyen.vface.bean.Face> faceList =
                    db_face.getFaceWithStudent(absenceStudentList.get(i).getStudentServerId());

            // set the first face as the thumbnail
            if (faceList.size() > 0)
            {
                faceThumbnail = ImageEditor.loadSizeLimitedBitmapFromUri(Uri.parse(faceList.get(0)
                                .getStudentFaceUri()), getContentResolver());
            } else
                faceThumbnail = null; // no face is found

            // add pair of a thumbnail and student information to the display list
            Pair<Bitmap, String> pair = new Pair<Bitmap, String>(faceThumbnail, identity);
            absenceStudentDisplayList.add(pair);
            //absenceStudentList.get(i).setStudentIdentifyFlag("");
        }

    }

    // This method is used to display course list on spinner
    private void displayCourseSpinner(List<Course> courseList)
    {
        spinCourses = findViewById(R.id.spinClass);
        spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, courseList);
        spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinCourses.setAdapter(spinnerArrayAdapter);

        if (courseList.size() > 0)
        {
            // set the default selection of spinner as the first course in the database
            spinCourses.setSelection(0);
            Course course = (Course) spinCourses.getItemAtPosition(0);
            courseServerId = course.getCourseServerId();

            // set event when other items on spinner get selected
            spinCourses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    Course course = (Course) parent.getItemAtPosition(position);
                    courseServerId = course.getCourseServerId();    // get course id on server
                    Log.i("EXECUTE", "Course Selected: " + courseServerId);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent)
                {

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
        detectedDetailsList.clear();
        detectedFacesList.clear();
        identifyUnknownList.clear();
        absenceStudentDisplayList.clear();
        // reset student identify flag to "NO" value
        db_student.resetStudentFlag(studentList);
        // set default selection for spinner
        spinStudentList.setSelection(0);
    }

    // Event for button taking photo
    public void btnTakePhoto(View view)
    {
        resetAllData();
        // ask for permission to use the camera and write external storage
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            {
                String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permission, PERMISSION_CODE);
            }
        else
        {
            openCamera();
        }
    }

    // open the camera to take photo of the class
    private void openCamera()
    {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Photo");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        uriImage = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // start activity with camera intent to take photo and get the image uri as result.
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriImage);
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case PERMISSION_CODE:
                {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        openCamera();
                    else
                        Toast.makeText(this, "Permission denied...", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Event for Choose photo from gallery button
    public void btnPickImage(View view)
    {
        resetAllData();

        // Open the gallery to choose image
        Intent intPickImage = new Intent(Intent.ACTION_PICK);
        intPickImage.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intPickImage.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intPickImage, GALLERY_REQUEST_CODE);
    }

    // Response from picking image and taking photo activities
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        detected = false;
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
        {
            displayPhoto(uriImage);
        }
        else if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK)
        {
            Uri selectedImage = data.getData();
            displayPhoto(selectedImage);
        }
        else if (resultCode != RESULT_OK)
        {
            Log.i("EXECUTE", "NO IMAGE CHOSEN");
            recreate();
        }
    }

    // Display photo on the ImageView
    private void displayPhoto(Uri uriImage)
    {
        // convert the bitmap loaded from storage into compatible format to display
        try
        {
            bitmapImage = ImageEditor.handlePhotoAndRotationBitmap(this, uriImage);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if (bitmapImage != null)
        {
            // display the captured photo and detect faces in photo
            ivClass.setImageBitmap(bitmapImage);
            detect(bitmapImage);
        }
    }

    // set the date on screen
    public void setDate(TextView view)
    {
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");
        String date = formatter.format(today);
        view.setText(date);
    }

    // This method is to detect faces in the bitmap photo
    private void detect(Bitmap bitmap)
    {
        // Put the image into an input stream for detection task input.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        // Start a background task to detect faces in the image.
        new DetectionTask().execute(inputStream);
        Log.i("EXECUTE","Detected value?: " + detected);
    }

    // This method is to identify a student through the detected face
    private void identify()
    {
        // detected value == true means the face is detected successfully
        if (detected && courseServerId != null)
        {
            int count = 0;  // counter for each faceId set
            int totalTurn = 0;  // counter the numbers of set added to the list

            List<List<UUID>> faceIdList = new ArrayList<>(); // contain multiples faces set of 10 for identify

            // initialize the maximum number of faces can be identified at one time is 100
            for (int i = 0; i < 10; i++)
            {
                List<UUID> faceIds = new ArrayList<>();
                faceIdList.add(faceIds);
            }

            for (Face face : facesList)
            {
                // add 10 faces as an element in faceIds List, since Microsoft sdk limits 10 faces can be identified at one
                if (count < 10)
                {
                    Log.i("EXECUTE" ," FACES NUMBER LESS THAN 10");
                    faceIdList.get(totalTurn).add(face.faceId);
                    count++;
                }
                else if (count == 10)
                {
                    Log.i("EXECUTE" ," FACES NUMBER REACH 10");
                    totalTurn++;    // one set added, move to the next set if necessary
                    count = 0;      // reset count counter
                    faceIdList.get(totalTurn).add(face.faceId);
                    count++;
                }
            }

            // Execute multiple identification tasks for each set of 10 faces
            for (int i = 0; i < totalTurn + 1; i++)
            {
                new IdentificationTask(courseServerId, i, totalTurn)
                        .execute(faceIdList.get(i).toArray(new UUID[faceIdList.get(i).size()]));
            }
        }
        else
        {
            // Not detected or person group exists.
            Log.i("EXECUTE","Please select an image and create course first.");
        }
    }




    // Background task of face identification.
    class IdentificationTask extends AsyncTask<UUID, String, IdentifyResult[]>
    {
        private boolean succeed = true;
        String courseServerId;
        int identifyTurn;   // Index of  this task being executed
        int totalTurn;      // How many times this task being executed
        List<Pair<Bitmap, String>> studentIdentity = new ArrayList<>();
        Student student;

        IdentificationTask(String courseServerId, int identifyTurn, int totalTurn)
        {
            this.courseServerId = courseServerId;
            this.identifyTurn = identifyTurn;
            Log.i("EXECUTE" ," IDENTIFY TURN: " + identifyTurn);
            this.totalTurn = totalTurn;
        }

        @Override
        protected IdentifyResult[] doInBackground(UUID... params)
        {
            String logString = "Request: Identifying faces ";
            for (UUID faceId: params) {
                logString += faceId.toString() + ", ";
            }
            logString += " in group " + courseServerId;
            Log.i("EXECUTE", logString);

            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                publishProgress("Getting course status...");

                TrainingStatus trainingStatus = faceServiceClient.getLargePersonGroupTrainingStatus(
                        this.courseServerId);
                if (trainingStatus.status != TrainingStatus.Status.Succeeded) {
                    publishProgress("Course training status is " + trainingStatus.status);
                    succeed = false;
                    return null;
                }


                publishProgress("Identifying...");
                Log.i("EXECUTE", "IDENTIFYING...");
                // Start identification.
                return faceServiceClient.identityInLargePersonGroup(
                        this.courseServerId,   /* personGroupId */
                        params,                  /* faceIds */
                        1);  /* maxNumOfCandidatesReturned */
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
        protected void onProgressUpdate(String... values) {
            // Show the status of background detection task on screen.a
            duringTaskProgressDialog(values[0]);
        }

        @Override
        protected void onPostExecute(IdentifyResult[] result)
        {
            if (result != null) {
                identifyResultsList.add(result);

                for (IdentifyResult identifyResult : result) {
                    DecimalFormat formatter = new DecimalFormat("#0.00");
                    if (identifyResult.candidates.size() > 0) {
                        String studentServerId =
                                identifyResult.candidates.get(0).personId.toString();

                        String confidence = formatter.format(identifyResult.candidates.get(0).confidence);
                        student = db_student.getAStudentWithId(studentServerId, courseServerId);
                        if (student != null)
                        {
                            String studentIdNumber = student.getStudentIdNumber();
                            String course = spinCourses.getSelectedItem().toString();
                            String studentName = student.getStudentName();
                            String identity = "Student: " + studentName.toUpperCase() + "\n"
                                    + "Student ID: " + studentIdNumber + "\n"
                                    + "Course: " + course +"\n"
                                    + "Confidence: " + confidence;

                            for (int i = 0; i < detectedDetailsList.size(); i++)
                            {

                                if (!detectedDetailsList.get(i).equalsIgnoreCase("UNKNOWN STUDENT"))
                                {
                                    int length = detectedDetailsList.get(i).length();
                                    Log.i("EXECUTE", "STRING: " + detectedDetailsList.get(i));
                                    Log.i("EXECUTE", "COMPARED: " + detectedDetailsList
                                            .get(i).substring(length - 4, length));
                                    String comparedConfidence = detectedDetailsList
                                            .get(i).substring(length - 4, length);

                                    Log.i("EXECUTE", "COMPARED: " + comparedConfidence);
                                    if (detectedDetailsList.get(i).contains(studentName.toUpperCase())) {

                                        if (comparedConfidence.compareToIgnoreCase(confidence) < 0) {
                                            detectedDetailsList.set(i, "UNKNOWN STUDENT");
                                            Log.i("EXECUTE", "SET UNKNOWN");
                                            student.setStudentIdentifyFlag("YES");

                                        } else if (comparedConfidence.compareToIgnoreCase(confidence) > 0) {
                                            identity = "UNKNOWN STUDENT";
                                            student.setStudentIdentifyFlag("NO");
                                            Log.i("EXECUTE", "NO SET");
                                        }
                                    }

                                }
                            }

                            detectedDetailsList.add(identity);
                            db_student.updateStudent(student);

                        }
                        else
                            Log.i("EXECUTE", "STUDENT NULL");

                    } else
                        detectedDetailsList.add("UNKNOWN STUDENT");
                }

                if (identifyTurn == totalTurn)
                {
                    int i = 0;
                    for (String info : detectedDetailsList)
                    {
                        Pair<Bitmap, String> pair = new Pair<Bitmap, String>(detectedFacesList.get(i), info);
                        if (info.equals("UNKNOWN STUDENT"))
                        {
                            Log.i("EXECUTE", "UNKNOWN ADD");
                            identifyUnknownList.add(pair);
                        }
                        else
                        {
                            Log.i("EXECUTE", "PAIR ADD");
                            studentIdentity.add(pair);
                        }

                        i++;
                    }

                    identifyTaskDone = true;
                    studentIdentityList = studentIdentity;
                    setUiAfterIdentification(succeed, studentIdentity);
                }
            }
            else
            {
                Log.i("EXECUTE", "ERROR IDENTIFY....");
            }
        }

    }

    private void setUiAfterIdentification(boolean succeed, List<Pair<Bitmap, String>> studentIdentityList)
    {
        if (succeed)
        {
            progressDialog.dismiss();
            listViewAdapter = new FaceListViewAdapter(studentIdentityList);

            ListView listView = findViewById(R.id.lvIdentifiedFaces);
            listView.setAdapter(listViewAdapter);
        }
    }

    class FaceListViewAdapter implements ListAdapter
    {
        List<Bitmap> faceThumbnails;
        List<String> studentInfo;
        //int requestCode;
        public FaceListViewAdapter()
        {
        }

        public FaceListViewAdapter(List<Pair<Bitmap, String>> studentIdentityList)
        {
            faceThumbnails = new ArrayList<>();
            studentInfo = new ArrayList<>();
            //this.requestCode = requestCode;

            if (studentIdentityList != null)
            {
                for (Pair<Bitmap, String> pair : studentIdentityList)
                {
                        faceThumbnails.add(pair.first);
                        studentInfo.add(pair.second);

                }
            }

        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public int getCount() {
            return faceThumbnails.size();
        }

        @Override
        public Object getItem(int position) {
            return faceThumbnails.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
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
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }


    // Background task of face detection.
    class DetectionTask extends AsyncTask<InputStream, String, Face[]>
    {
        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try{
                publishProgress("Detecting...");
                Log.i("EXECUTE", "DETECTING FACE");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        null);
            }  catch (Exception e) {
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
        protected void onProgressUpdate(String... values) {
            // Show the status of background detection task on screen.a
            duringTaskProgressDialog(values[0]);
        }

        @Override
        protected void onPostExecute(Face[] result) {

            if (result != null)
            {
                for (Face face : result)
                {
                    try
                    {
                        detectedFacesList.add(ImageEditor.generateFaceThumbnail(bitmapImage, face.faceRectangle));

                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }

                facesList = Arrays.asList(result);

                if (result.length == 0) {
                    detected = false;
                    Log.i("EXECUTE", "NO FACE DETECTED!");
                }
                else
                {
                        Log.i("EXECUTE", "FACE DETECTED!");
                    detected = true;
                    progressDialog.dismiss();
                    identify();
                }

            }
            else
            {
                detected = false;
            }
        }
    }



    @Override
    public void onBackPressed() {
        Intent intent = new Intent(GroupCheckActivity.this, DashBoardActivity.class);
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
    }

    private void startProgressDialog()
    {
        progressDialog.show();
    }

    private void duringTaskProgressDialog(String progress)
    {
        progressDialog.setMessage(progress);
    }




}
