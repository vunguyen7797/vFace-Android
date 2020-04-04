/*
 * GroupCheckActivity.java
 */
package com.vunguyen.vface.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.helper.asyncTasks.DetectionTask;
import com.vunguyen.vface.helper.FaceListViewAdapter;
import com.vunguyen.vface.helper.ImageEditor;
import com.vunguyen.vface.helper.LocaleHelper;
import com.vunguyen.vface.helper.callbackInterfaces.CourseListInterface;
import com.vunguyen.vface.helper.callbackInterfaces.FaceListInterface;
import com.vunguyen.vface.helper.callbackInterfaces.StudentListInterface;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This class contains implementations for checking attendance
 * by a group photo.
 * Background tasks implemented to detect faces, and identify faces.
 */
public class GroupCheckActivity extends AppCompatActivity
{
    // UI features
    @SuppressLint("StaticFieldLeak")
    public static Spinner spinViewModes;
    @SuppressLint("StaticFieldLeak")
    public static ListView lvIdentifiedFaces;
    @SuppressLint("StaticFieldLeak")
    public static TextView tvDate;
    @SuppressLint("StaticFieldLeak")
    public static ImageView ivWaitingIdentify;

    ProgressDialog progressDialog;
    AutoCompleteTextView courseMenu;
    Button btnTakePhoto;
    Button btnPickImage;
    ImageView ivClass;

    // Request codes
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int GALLERY_REQUEST_CODE = 1;
    private static final int PERMISSION_CODE = 1000;

    // Database
    DatabaseReference mDatabase_course;
    DatabaseReference mDatabase_face;
    DatabaseReference mDatabase_student;
    DatabaseReference mDatabase_date;

    // Data
    Uri uriImage;
    boolean detected;
    boolean studentListChanged = false;
    String courseServerId;
    String account;
    Course course;
    String date;
    Bitmap bitmapImage; // face image thumbnail

    // Data Adapter
    ArrayAdapter<String> spinnerViewModesAdapter;
    FaceListViewAdapter listViewStudentsAdapter;

    // Data containers
    public static List<Pair<Bitmap, String>> displayIdentifiedList;         // store detected In-class Students
    public static List<Pair<Bitmap, String>> displayUnknownList;         // store detected Unknown Students

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_check);

        // Get email account
        account = getIntent().getStringExtra("ACCOUNT");

        initView();
        initData();
        initAction();
    }

    // initialize all features from layout xml
    private void initView()
    {
        // initialize buttons
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnPickImage = findViewById(R.id.btnGallery);
        ivWaitingIdentify = findViewById(R.id.ivWaitingIdentify);
        tvDate = findViewById(R.id.tvDate);
        courseMenu = findViewById(R.id.filled_exposed_dropdown);
        ivClass = findViewById(R.id.ivClassImage);
        lvIdentifiedFaces = findViewById(R.id.lvIdentifiedFaces);
        // initialize the progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("V.FACE");
        spinViewModes = findViewById(R.id.spinStudentList);
    }

    // initialize data
    private void initData()
    {
        // initialize all data lists
        displayIdentifiedList = new ArrayList<>();
        displayUnknownList = new ArrayList<>();

        mDatabase_course = FirebaseDatabase.getInstance().getReference().child(account).child("course");
        mDatabase_student = FirebaseDatabase.getInstance().getReference().child(account).child("student");
        mDatabase_face = FirebaseDatabase.getInstance().getReference().child(account).child("face");
        mDatabase_date = FirebaseDatabase.getInstance().getReference().child(account).child("date");
    }

    // initialize action when activity first created
    private void initAction()
    {
        //display all available courses on menu
        getCourseListFirebase(new CourseListInterface()
        {
            @Override
            public void getCourseList(List<Course> courseList)
            {
                displayCourseMenu(courseList);
            }
        });

        // display view modes on spinners
        displayViewModesSpinner();

        // set display date
        displayDate(tvDate);
        date = tvDate.getText().toString();
    }

    /**
     ******************* displaying view handlers *********************
     */
    // This method is used to display course list on menu
    private void displayCourseMenu(List<Course> courseList)
    {
        // initialize course menu
        ArrayAdapter<Course> tvArrayAdapter = new ArrayAdapter<>
                (this, R.layout.dropdown_menu_popup_item, courseList);
        courseMenu.setAdapter(tvArrayAdapter);
        if (courseList.size() > 0)
        {
            // handle course selection
            courseMenu.setOnItemClickListener((parent, view, position, id) ->
            {
                course = (Course) parent.getItemAtPosition(position);
                courseServerId = course.getCourseServerId();    // get course id on server
                btnTakePhoto.setEnabled(true);
                btnPickImage.setEnabled(true);
                Log.i("EXECUTE", "Course Selected: " + courseServerId);
            });
        }
        else
        {
            Log.i("EXECUTE", "No courses are available.");
            Toast.makeText(getApplicationContext(), "No courses are available", Toast.LENGTH_SHORT).show();
        }
    }

    // This method is used to display student list view modes on spinner
    private void displayViewModesSpinner()
    {
        // There are 3 modes to display the student list after identify task
        String[] viewModes = getResources().getStringArray(R.array.display_modes);

        // initialize Array adapter
        spinnerViewModesAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, viewModes)
        {
            @Override
            public boolean isEnabled(int position)
            {
                // disable the unknown students option if no unknown found
                return displayUnknownList.size() != 0 || position != 2;
            }

            // set the color change
            @Override
            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent)
            {
                // TODO Auto-generated method stub
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                if ((displayUnknownList.size() == 0 && position == 2))
                {
                    textView.setTextColor(Color.RED);
                }
                else
                {
                    textView.setTextColor(Color.BLACK);
                }
                textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                return textView;
            }
        };

        spinnerViewModesAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinViewModes.setAdapter(spinnerViewModesAdapter);
        spinViewModes.setSelection(0);    // set the first default option is In-class student

        // handling each mode selected
        spinViewModes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (position != 1)  // Absence mode is handled by separate method
                {
                    if (position == 2)  // Display Unknown student list
                    {
                        listViewStudentsAdapter = new FaceListViewAdapter(displayUnknownList, GroupCheckActivity.this);
                        studentListChanged = true; // default option has been changed
                        Toast.makeText(getApplicationContext(), displayUnknownList.size() + " unknown students in class", Toast.LENGTH_SHORT).show();
                        if (displayUnknownList.size() == 0)
                            ivWaitingIdentify.setVisibility(View.VISIBLE);
                        else
                            ivWaitingIdentify.setVisibility(View.GONE);

                    }
                    else if (studentListChanged && position == 0) // Display InClass student list
                    {
                        if (displayIdentifiedList.size() == 0)
                            ivWaitingIdentify.setVisibility(View.VISIBLE);
                        else
                           ivWaitingIdentify.setVisibility(View.GONE);
                        listViewStudentsAdapter = new FaceListViewAdapter(displayIdentifiedList, GroupCheckActivity.this);
                    }

                ListView listView = findViewById(R.id.lvIdentifiedFaces);
                listView.setAdapter(listViewStudentsAdapter);
                }
                else
                {
                    studentListChanged = true;
                    displayAbsentStudent(course);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });
    }

    // This method is used to get absent students information for displaying
    private void displayAbsentStudent(Course course)
    {
       getAbsenceListFirebase(course.getCourseServerId(), new StudentListInterface()
       {
           @Override
           public void getStudentList(List<Student> absenceList) throws IOException
           {
               getFaceFirebase(faceList ->
               {
                   Uri faceThumbnailUri = null;
                   List<Pair<Uri, String>> displayList = new ArrayList<>();
                   for (int i = 0; i < absenceList.size(); i++)
                   {
                       // Student information
                       String studentIdNumber = absenceList.get(i).getStudentIdNumber();
                       String studentServerId = absenceList.get(i).getStudentServerId();
                       String studentName = absenceList.get(i).getStudentName();
                       String identity = studentName + "\n"
                               + "Student ID: " + studentIdNumber + "\n"
                               + "Course: " + course;

                       // get first face of student as a thumbnail
                       int index = 0;
                       while (faceThumbnailUri == null)
                       {
                           if (faceList.get(index).getStudentServerId().equalsIgnoreCase(studentServerId))
                               faceThumbnailUri = Uri.parse(faceList.get(index).getStudentFaceUri());
                           else
                               index++;
                       }

                       mDatabase_date.child(studentName.toUpperCase()+"-"+date.replaceAll("[,]","")).child("studentAttendanceStatus").setValue("NO");
                       // add pair of a thumbnail and student information to the display list
                       Pair<Uri, String> pair = new Pair<>(faceThumbnailUri, identity);
                       displayList.add(pair);
                       faceThumbnailUri = null; // reset face thumbnail
                   }

                   if (displayList.size() > 0)
                   {
                       ivWaitingIdentify.setVisibility(View.GONE);
                       listViewStudentsAdapter = new FaceListViewAdapter(displayList, "uri", GroupCheckActivity.this);
                       ListView listView = findViewById(R.id.lvIdentifiedFaces);
                       listView.setAdapter(listViewStudentsAdapter);
                   }
                   else
                   {
                       ivWaitingIdentify.setVisibility(View.VISIBLE);
                       Toast.makeText(getApplicationContext(),
                               "No absent student.", Toast.LENGTH_SHORT).show();
                   }
               });
           }
       });
    }

    // Display photo on the ImageView
    private void displaySelectedPhoto(Uri uriImage)
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
    public void displayDate(TextView view)
    {
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        String date = formatter.format(today);
        view.setText(date);
    }

    /**
     ******************* get data from Firebase realtime database *********************
     */
    private void getStudentListFirebase(String courseServerId, StudentListInterface callback)
    {
        mDatabase_student.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                List<Student> studentList = new ArrayList<>();
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Student student = dsp.getValue(Student.class);
                    assert student != null;
                    if (student.getCourseServerId().equalsIgnoreCase(courseServerId)
                            && student.getStudentIdentifyFlag().equalsIgnoreCase("NO"))
                        studentList.add(student);
                }
                try {
                    callback.getStudentList(studentList);
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

    // get face list from real time database
    private void getFaceFirebase( FaceListInterface callback)
    {
        mDatabase_face.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                List<com.vunguyen.vface.bean.Face> faceList = new ArrayList<>();
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    com.vunguyen.vface.bean.Face face = dsp.getValue(com.vunguyen.vface.bean.Face.class);
                    faceList.add(face);
                }
                callback.getFaceList(faceList);
                mDatabase_face.removeEventListener(this);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {
            }
        });
    }

    private void getCourseListFirebase(CourseListInterface callback)
    {
        mDatabase_course.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Course> courseList = new ArrayList<>();
                for(DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    courseList.add(dsp.getValue(Course.class));
                }
                callback.getCourseList(courseList);
                mDatabase_course.removeEventListener(this);
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
                try {
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

    /**
     ******************* reset data methods *********************
     */
    // This method is used to reset all the lists or databases into the initial status for a new task
    private void resetAllData()
    {
        ivWaitingIdentify.setVisibility(View.GONE);
        // reset data lists
        displayUnknownList.clear();
        displayIdentifiedList.clear();
        resetStudentDate(courseServerId);
        resetStudentFlag(courseServerId);
        // set default selection for spinner
        spinViewModes.setSelection(0);
    }

    // reset all date if users check attendance more than 1 for the same course
    private void resetStudentDate(String courseServerId)
    {
        getStudentListFirebase(courseServerId, studentList -> {
            if (tvDate.getText().toString().equalsIgnoreCase(date)
                    && courseMenu.getText().toString().equalsIgnoreCase(course.getCourseName()))
            {
                for (Student student : studentList)
                {
                    String path = student.getStudentName().toUpperCase()
                            + "-" + date.replaceAll("[,]", "");
                    mDatabase_date.child(path).removeValue();
                }
            }
        });
    }

    // reset all student flag into "NO"
    private void resetStudentFlag(String courseServerId)
    {
        mDatabase_student.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Student student = dsp.getValue(Student.class);
                    assert student != null;
                    if (student.getCourseServerId().equalsIgnoreCase(courseServerId))
                        mDatabase_student.child(student.getStudentName().toUpperCase() + "-"
                                + student.getStudentServerId()).child("studentIdentifyFlag").setValue("NO");
                }
                mDatabase_student.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {
            }
        });
    }

    /**
     ******************* Event handler methods *********************
     */
    // Event for button taking photo
    public void btnTakePhoto(View view)
    {
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

    // Event for Choose photo from gallery button
    public void btnPickImage(View view)
    {
        // Open the gallery to choose image
        Intent intPickImage = new Intent(Intent.ACTION_PICK);
        intPickImage.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intPickImage.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intPickImage, GALLERY_REQUEST_CODE);
    }

    public void btnBackClick(View view)
    {
        onBackPressed();
    }

    // Click back button on navigation bar
    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(GroupCheckActivity.this, DashBoardActivity.class);
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
        finish();
    }

    // open the device camera for taking photo of classes
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions
            , @NonNull int[] grantResults)
    {
        if (requestCode == PERMISSION_CODE)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                openCamera();
            else
                Toast.makeText(this, "Permission denied...", Toast.LENGTH_LONG).show();
        }
    }

    // Response from picking image and taking photo activities
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        detected = false;
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
        {
            resetAllData();
            displaySelectedPhoto(uriImage);
        }
        else if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK)
        {
            resetAllData();
            Uri selectedImage = null;
            if (data != null) {
                selectedImage = data.getData();
            }
            displaySelectedPhoto(selectedImage);
        }
        else if (resultCode != RESULT_OK)
        {
            Log.i("EXECUTE", "NO IMAGE CHOSEN");
            recreate();
        }
    }

    /**
     ******************* Interact with server to do main jobs *********************
     */
    // This method is to detect faces in the bitmap photo
    private void detect(Bitmap bitmap)
    {
        // Put the image into an input stream for detection task input.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        // Start a background task to detect faces in the image.
        //new DetectionTask().execute(inputStream);
         new DetectionTask(GroupCheckActivity.this
                 , bitmapImage, detected, courseServerId, "GROUP-CHECK").execute(inputStream);
    }
}
