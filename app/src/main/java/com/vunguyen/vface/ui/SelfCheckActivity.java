/*
 * SelfCheckActivity.java
 */
package com.vunguyen.vface.ui;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.helper.ImageEditor;
import com.vunguyen.vface.helper.LocaleHelper;
import com.vunguyen.vface.helper.asyncTasks.DetectionTask;
import com.vunguyen.vface.helper.callbackInterfaces.CourseListInterface;
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
 * This class implements feature for self checking in the class.
 * So each student will check in by themselves with this mode.
 */
public class SelfCheckActivity extends AppCompatActivity
{
    String account;
    Course course;
    String courseServerId;
    String date;
    Bitmap bitmapImage;
    boolean detected;

    // UI features
    @SuppressLint("StaticFieldLeak")
    public static TextView tvDate;
    @SuppressLint("StaticFieldLeak")
    public static ImageView ivWaitingIdentify;
    @SuppressLint("StaticFieldLeak")
    public static ListView listView;
    AutoCompleteTextView courseMenu;
    ProgressDialog progressDialog;
    Button btnFaceCheck;

    // Data lists
    public static List<String> detectedDetailsList;   // store the students' information after detection
    public static List<Bitmap> detectedFacesList;     // store the students' face in Bitmap after detection

    //Database
    DatabaseReference mDatabase_course;
    DatabaseReference mDatabase_face;
    DatabaseReference mDatabase_student;
    DatabaseReference mDatabase_date;

    // Request codes
    private static final int REQUEST_TAKE_PHOTO = 0;

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

        initView();
        initData();
        initAction();
    }

    private void initData()
    {
        mDatabase_course = FirebaseDatabase.getInstance().getReference().child(account).child("course");
        mDatabase_student = FirebaseDatabase.getInstance().getReference().child(account).child("student");
        mDatabase_face = FirebaseDatabase.getInstance().getReference().child(account).child("face");
        mDatabase_date = FirebaseDatabase.getInstance().getReference().child(account).child("date");
        detectedFacesList = new ArrayList<>();
        detectedDetailsList = new ArrayList<>();
    }

    private void initView()
    {
        tvDate = findViewById(R.id.tvDate);
        listView = findViewById(R.id.lvIdentifiedFaces);
        // get all courses available
        courseMenu = findViewById(R.id.filled_exposed_dropdown);
        btnFaceCheck = findViewById(R.id.btnFaceCheck);
        ivWaitingIdentify = findViewById(R.id.ivWaitingIdentify);
        // initialize the progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("V.FACE");
    }

    private void initAction()
    {
        //display all available courses on menu
        getCourseListFirebase(this::displayCourseMenu);
        setDate(tvDate);
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
                btnFaceCheck.setEnabled(true);
                Toast.makeText(getApplicationContext(), "Attendance data of this course today is reset.", Toast.LENGTH_SHORT).show();
                resetAllData();
                Log.i("EXECUTE", "Course Selected: " + courseServerId);
            });
        }
        else
        {
            Log.i("EXECUTE", "No courses are available.");
            Toast.makeText(getApplicationContext(), "No courses are available", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     ******************* get data from Firebase realtime database *********************
     */
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

    /**
     ******************* reset data methods *********************
     */

    // This method is used to reset all the lists or databases into the initial status for a new task
    private void resetAllData()
    {
        ivWaitingIdentify.setVisibility(View.GONE);
        detectedFacesList.clear();
        detectedDetailsList.clear();
        // reset data lists
        resetStudentDate(courseServerId);
        resetStudentFlag(courseServerId);
        // set default selection for spinner
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
                    if (student.getCourseServerId().equalsIgnoreCase(courseServerId))
                    {
                        String path = student.getStudentName().toUpperCase()
                                + "-" + date.replaceAll("[,]", "") + student.getStudentServerId();
                        mDatabase_date.child(path).removeValue();
                    }
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
    // Response on result of option which was just executed
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_TAKE_PHOTO == requestCode && resultCode == RESULT_OK)
        {
            // The URI of photo taken with camera
            Uri uriTakenPhoto = data.getData();
            detect(uriTakenPhoto);
        }
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
    @RequiresApi(api = Build.VERSION_CODES.N)
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

        new DetectionTask(SelfCheckActivity.this,
                bitmapImage, detected, courseServerId, "SELF-CHECK").execute(inputStream);
    }
}