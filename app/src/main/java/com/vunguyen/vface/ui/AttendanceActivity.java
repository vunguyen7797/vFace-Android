/*
 * AttendanceActivity.java
 */
package com.vunguyen.vface.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.bean.Date;
import com.vunguyen.vface.bean.Face;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.bean.StudentInfoPackage;
import com.vunguyen.vface.helper.LocaleHelper;
import com.vunguyen.vface.helper.callbackInterfaces.CourseListInterface;
import com.vunguyen.vface.helper.callbackInterfaces.DateInterface;
import com.vunguyen.vface.helper.callbackInterfaces.FaceListInterface;
import com.vunguyen.vface.helper.callbackInterfaces.StudentListInterface;
import com.vunguyen.vface.helper.callbackInterfaces.TotalAbsenceInterface;
import com.vunguyen.vface.helper.FaceListViewAdapter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements methods to display in-class and absent students
 * to user in a particular day
 */
public class AttendanceActivity extends AppCompatActivity
{
    // UI feature
    MaterialDatePicker materialDatePicker;
    TextInputLayout textInputLayoutDate;
    TextInputLayout textInputLayoutStudentList;
    EditText etDate;
    AutoCompleteTextView courseMenu;
    AutoCompleteTextView viewModesMenu;
    ListView lvAttendance;
    ImageView ivAttendance;

    //database
    DatabaseReference mDatabase_course;
    DatabaseReference mDatabase_student;
    DatabaseReference mDatabase_date;
    DatabaseReference mDatabase_face;

    //adapter
    FaceListViewAdapter studentListViewAdapter;
    private static final int MY_REQUEST_CODE = 1000;

    // variables
    String account;
    Course course;
    String courseServerId;
    String date="";

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

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
        mDatabase_date = FirebaseDatabase.getInstance().getReference().child(account).child("date");
        mDatabase_face = FirebaseDatabase.getInstance().getReference().child(account).child("face");
    }

    private void initView()
    {
        etDate = findViewById(R.id.etDate);
        lvAttendance = findViewById(R.id.lvAttendance);
        textInputLayoutDate = findViewById(R.id.outlineDatePicker);
        textInputLayoutStudentList = findViewById(R.id.outlineStudentList);
        viewModesMenu = findViewById(R.id.filled_exposed_dropdown_2);
        ivAttendance = findViewById(R.id.ivAttendance);
        courseMenu = findViewById(R.id.filled_exposed_dropdown);
        lvAttendance = findViewById(R.id.lvAttendance);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initAction()
    {
        getCourseListFirebase(this::displayCourseMenu);
        displayViewModes(viewModesMenu);
        setDatePicker();
    }

    /**
     ******************* displaying view handlers *********************
     */
    // display courses on the autocomplete text view menu to pick the course
    private void displayCourseMenu(List<Course> courseList)
    {
        // initialize course menu
        ArrayAdapter<Course> tvArrayAdapter = new ArrayAdapter<>(this,
                R.layout.dropdown_menu_popup_item, courseList);
        courseMenu.setAdapter(tvArrayAdapter);

        if (courseList.size() > 0)
        {
            courseMenu.setOnItemClickListener((parent, view, position, id) ->
            {
                course = (Course) parent.getItemAtPosition(position);
                courseServerId = course.getCourseServerId();    // get course id on server
                List<Pair<Pair<Uri, String>, Pair<Student, Integer>>> studentList = new ArrayList<>();
                studentListViewAdapter = new FaceListViewAdapter(studentList, "uri", AttendanceActivity.this, "withTotalAbsence");
                lvAttendance.setAdapter(studentListViewAdapter);
                ivAttendance.setVisibility(View.VISIBLE);
                Log.i("EXECUTE", "Course Selected: " + courseServerId);
                textInputLayoutDate.setEnabled(true);
                if (!date.equalsIgnoreCase(""))
                    viewModesEventHandler();
            });
        }
        else
        {
            Log.i("EXECUTE", "No courses are available.");
            Toast.makeText(getApplicationContext(), "No courses are available", Toast.LENGTH_SHORT).show();
        }
    }

    // this method is to display the view modes of student list
    private void displayViewModes(AutoCompleteTextView listMenu)
    {
        // There are 3 options to display the student list after identify task
        String[] studentListOptions = getResources().getStringArray(R.array.display_modes_attendance);
        if (date.equalsIgnoreCase(""))
        {
            listMenu.setEnabled(false);
            textInputLayoutStudentList.setEnabled(false);
        }
        ArrayAdapter<String> tvArrayAdapter = new ArrayAdapter<>(this,
                R.layout.dropdown_menu_popup_item, studentListOptions);
        listMenu.setAdapter(tvArrayAdapter);
    }

    // check the current mode that user is using to display student list
    private void viewModesEventHandler()
    {
        if (!viewModesMenu.getText().toString().equalsIgnoreCase(""))
        {
            if (viewModesMenu.getText().toString().equalsIgnoreCase("In-class students"))
                displayStudentOnADate(date, 0);
            else
                displayStudentOnADate(date, 1);
        }
        else
            viewModesMenu.setOnItemClickListener((parent, view, position, id) ->
                    displayStudentOnADate(date, position));

    }

    // display students on a day, request = 0 for in-class, = 1 for absent students
    private void displayStudentOnADate(String date, int request)
    {
        List<Pair<Pair<Uri, String>, Pair<Student, Integer>>> attendanceList = new ArrayList<>();

        getStudentListFirebase(courseServerId, studentList ->
                getFaceFirebase(faceList ->
                {
                    Log.i("EXECUTE", "STUDENT LIST: " + studentList.size());
                    if(request == 0)    // display in-class students
                    {
                        for (Student student : studentList)
                        {
                            getStudentDateFirebase(courseServerId, student.getStudentServerId()
                                    , date, dateObject ->
                                    {
                                        if (dateObject.getStudent_date() != null)
                                        {
                                            Log.i("EXECUTE", "Date executed: " + date + " - " + dateObject.getStudentAttendanceStatus());
                                            if (dateObject.getStudentAttendanceStatus().equalsIgnoreCase("YES"))
                                            {
                                                Face studentFace = null;
                                                for (Face face : faceList)
                                                {
                                                    if (face.getStudentServerId().equalsIgnoreCase(student.getStudentServerId()))
                                                        studentFace = face;
                                                }

                                                Face finalStudentFace = studentFace;
                                                getTotalAbsence(courseServerId, student.getStudentServerId(), counter ->
                                                {
                                                    assert finalStudentFace != null;
                                                    Pair<Uri, String> identity = new Pair<>(Uri.parse(finalStudentFace
                                                            .getStudentFaceUri()), student.getStudentName());
                                                    Pair<Student, Integer> identity2 = new Pair<>(student, counter);
                                                    Pair<Pair<Uri, String>, Pair<Student, Integer>> finalPair = new Pair<>(identity, identity2);
                                                    attendanceList.add(finalPair);
                                                    setStudentArrayAdapter(attendanceList); // set adapter to display on list view
                                                });
                                            } else {
                                                Log.i("EXECUTE", student.getStudentName() + " was absent today.");

                                            }
                                        }
                                        else
                                        {
                                            Log.i("EXECUTE", "Date of this student has not been updated");
                                            Toast.makeText(getApplicationContext(), "Attendance has not been checked for this day", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                    else if (request == 1)
                    {
                        for (Student student : studentList)
                        {
                            getStudentDateFirebase(courseServerId, student.getStudentServerId(), date, dateObject ->
                            {
                                if (dateObject.getStudent_date() != null) {
                                    if (dateObject.getStudentAttendanceStatus().equalsIgnoreCase("NO")) {
                                        Face studentFace = null;
                                        for (Face face : faceList) {
                                            if (face.getStudentServerId().equalsIgnoreCase(student.getStudentServerId()))
                                                studentFace = face;
                                        }

                                        Face finalStudentFace = studentFace;
                                        getTotalAbsence(courseServerId, student.getStudentServerId(), counter ->
                                        {
                                            assert finalStudentFace != null;
                                            Pair<Uri, String> identity = new Pair<>(Uri.parse(finalStudentFace
                                                    .getStudentFaceUri()), student.getStudentName());
                                            Pair<Student, Integer> identity2 = new Pair<>(student, counter);
                                            Pair<Pair<Uri, String>, Pair<Student, Integer>> finalPair = new Pair<>(identity, identity2);
                                            attendanceList.add(finalPair);
                                            setStudentArrayAdapter(attendanceList); // set adapter to display on list view
                                        });
                                    } else
                                        Log.i("EXECUTE", student.getStudentName() + " was not absent today.");
                                }
                                else {
                                    Log.i("EXECUTE", "Date of this student has not been updated");
                                    Toast.makeText(getApplicationContext(), "Attendance has not been checked for this day", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }));
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
                    if (student.getCourseServerId().equalsIgnoreCase(courseServerId))
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

    private void getStudentDateFirebase(String courseServerId, String studentServerId, String date_string, DateInterface callback)
    {
        mDatabase_date.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                Date date = new Date();
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Date temp = dsp.getValue(Date.class);
                    assert temp != null;
                    if (temp.getCourseServerId().equalsIgnoreCase(courseServerId)
                            && temp.getStudentServerId().equalsIgnoreCase(studentServerId)
                            && temp.getStudent_date().equalsIgnoreCase(date_string))
                    {
                        Log.i("EXECUTE", "temp: " + temp);
                        date = temp;
                    }
                    else
                    {
                        Log.i("EXECUTE", "temp is null");
                        Log.i("EXECUTE", "CSI: " + temp.getCourseServerId());
                        Log.i("EXECUTE", "SVI: " + temp.getStudentServerId());
                        Log.i("EXECUTE", "DATE: " + temp.getStudent_date());
                    }

                }
                callback.getDate(date);
                mDatabase_date.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {
            }
        });
    }

    private void getTotalAbsence(String courseServerId, String studentServerId, TotalAbsenceInterface callback)
    {
        mDatabase_date.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                int counter = 0;
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Date temp = dsp.getValue(Date.class);
                    assert temp != null;
                    if (temp.getCourseServerId().equalsIgnoreCase(courseServerId)
                            && temp.getStudentServerId().equalsIgnoreCase(studentServerId)
                            && temp.getStudentAttendanceStatus().equalsIgnoreCase("NO"))
                        counter++;
                }
                callback.getTotalAbsence(counter);
                mDatabase_date.removeEventListener(this);
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

    /**
     ******************* Event handler methods *********************
     */
    private void setStudentArrayAdapter(List<Pair<Pair<Uri, String>, Pair<Student, Integer>>> studentList)
    {
        if (studentList.size() == 0 || date.equalsIgnoreCase("")
                || viewModesMenu.getText().toString().equalsIgnoreCase(""))
            ivAttendance.setVisibility(View.VISIBLE);
        else if (studentList.size() > 0)
        {
            ivAttendance.setVisibility(View.GONE);
            // create adapter for list view of student
            //studentListViewAdapter = new FaceListViewAdapter(studentList);
            studentListViewAdapter = new FaceListViewAdapter(studentList, "uri", AttendanceActivity.this, "withTotalAbsence");
            lvAttendance.setAdapter(studentListViewAdapter);
            lvAttendance.setOnItemClickListener((parent, view, position, id) -> {
                //Log.i("EXECUTE", "Get student: " + student.toString());
                StudentInfoPackage studentInfoPackage = (StudentInfoPackage) parent.getItemAtPosition(position);
                Student student = studentInfoPackage.getStudent().first.first;
                Uri studentUri = studentInfoPackage.getStudent().first.second;
                Integer totalAbsence = studentInfoPackage.getStudent().second;
                Log.i("EXECUTE", "Get student: " + student.toString());
                Intent intent = new Intent(AttendanceActivity.this, StudentProfilePageActivity.class);
                intent.putExtra("ACCOUNT", account);
                intent.putExtra("Student", student);
                intent.setData(studentUri);
                intent.putExtra("Absence", totalAbsence);
                startActivityForResult(intent, MY_REQUEST_CODE);
            });
        }
    }

    // Refresh the student list after adding new student
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == MY_REQUEST_CODE) {
            boolean needRefresh = data.getBooleanExtra("needRefresh", true);
            account = data.getStringExtra("ACCOUNT");

        }
    }

    // event for the date picker
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setDatePicker()
    {
        LocalDateTime local = LocalDateTime.now();
        Object localTime = local.atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC)).toInstant().toEpochMilli();
        MaterialDatePicker.Builder builder = MaterialDatePicker.Builder.datePicker();
        builder.setTitleText("VFACE");
        builder.setSelection(localTime);

        Log.i("EXECUTE", "Time zone: " + local);

        materialDatePicker = builder.build();
        textInputLayoutDate.setEndIconOnClickListener(v ->
                materialDatePicker.show(getSupportFragmentManager(), "DATE_PICKER"));

        materialDatePicker.addOnPositiveButtonClickListener(selection ->
        {
            etDate.setText(materialDatePicker.getHeaderText());
            etDate.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            date = materialDatePicker.getHeaderText();
            textInputLayoutStudentList.setEnabled(true);
            viewModesEventHandler();

        });
    }

    // button back event
    public void btnBackClick(View view)
    {
        onBackPressed();
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(AttendanceActivity.this, DashBoardActivity.class);
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
        finish();
    }
}
