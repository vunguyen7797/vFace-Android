/*
 * AttendanceActivity.java
 */
package com.vunguyen.vface.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.Dash;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.textfield.TextInputLayout;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.helper.MyDatabaseHelperCourse;
import com.vunguyen.vface.helper.MyDatabaseHelperDate;
import com.vunguyen.vface.helper.MyDatabaseHelperStudent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    AutoCompleteTextView listMenu;
    ListView lvAttendance;
    ImageView ivAttendance;

    List<Course> courseList;    // store list of course objects
    List<Student> studentList;  // store list of student in a course

    //database
    MyDatabaseHelperCourse db_course;
    MyDatabaseHelperStudent db_student;
    MyDatabaseHelperDate db_date;

    //adapter
    ArrayAdapter<Student> studentArrayAdapter;

    // variables
    String account;
    Course course;
    String courseServerId;
    String date="";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Set no notification bar on activity
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_attendance);

        // Get email account
        account = getIntent().getStringExtra("ACCOUNT");

        // initialize data
        courseList = new ArrayList<>();
        db_course = new MyDatabaseHelperCourse(this);
        db_student = new MyDatabaseHelperStudent(this);
        db_date = new MyDatabaseHelperDate(this);

        //get all courses
        this.courseList = db_course.getAllCourses(account);

        lvAttendance = findViewById(R.id.lvAttendance);
        textInputLayoutDate = findViewById(R.id.outlineDatePicker);
        textInputLayoutStudentList = findViewById(R.id.outlineStudentList);
        listMenu = findViewById(R.id.filled_exposed_dropdown_2);
        ivAttendance = findViewById(R.id.ivAttendance);

        //display data on course menu
        displayCourseMenu(courseList);
        displayListMenu(listMenu);
        setDatePicker();
    }

    // this method is to display the view modes of student list
    private void displayListMenu(AutoCompleteTextView listMenu)
    {
        // There are 3 options to display the student list after identify task
        List<String> studentListOptions = new ArrayList<>();
        studentListOptions.add("In-class students");
        studentListOptions.add("Absent students");
        if (date.equalsIgnoreCase(""))
        {
            listMenu.setEnabled(false);
            textInputLayoutStudentList.setEnabled(false);
        }
        ArrayAdapter<String> tvArrayAdapter = new ArrayAdapter<>(this,
                R.layout.dropdown_menu_popup_item, studentListOptions);
        listMenu.setAdapter(tvArrayAdapter);
    }

    // display courses on the autocomplete textview menu to pick the course
    private void displayCourseMenu(List<Course> courseList)
    {
        // initialize course menu
        courseMenu = findViewById(R.id.filled_exposed_dropdown);
        ArrayAdapter<Course> tvArrayAdapter = new ArrayAdapter<Course>(this,
                R.layout.dropdown_menu_popup_item, courseList);
        courseMenu.setAdapter(tvArrayAdapter);

        if (courseList.size() > 0)
        {
            courseMenu.setOnItemClickListener((parent, view, position, id) ->
            {
                course = (Course) parent.getItemAtPosition(position);
                courseServerId = course.getCourseServerId();    // get course id on server
                // get all students in the course
                studentList = db_student.getStudentWithCourse(courseServerId);
                //displayStudentOnADate(date);
                Log.i("EXECUTE", "Course Selected: " + courseServerId);
                textInputLayoutDate.setEnabled(true);
                if (!date.equalsIgnoreCase(""))
                    checkDisplayOption();
            });
        }
        else
        {
            Log.i("EXECUTE", "No courses are available.");
        }
    }

    // display students on a day, request = 0 for in-class, = 1 for absent students
    private void displayStudentOnADate(String date, int request)
    {
        lvAttendance = findViewById(R.id.lvAttendance);

        List<Student> attendanceList = new ArrayList<>();
        if(request == 0)
        {
            Log.i("EXECUTE", date);
            for (Student student : studentList)
            {
                String status = db_date.getStudentStatus(student.getStudentServerId(),
                        student.getCourseServerId(), date);
                if (status.equalsIgnoreCase("YES"))
                    attendanceList.add(student);
            }
        }
        else if (request == 1)
        {
            for (Student student: studentList)
            {
                String status = db_date.getStudentStatus(student.getStudentServerId(),
                        student.getCourseServerId(), date);
                if (status.equalsIgnoreCase("NO"))
                    attendanceList.add(student);
            }
        }
        setStudentArrayAdapter(attendanceList); // set adapter to display on list view
    }

    private void setStudentArrayAdapter(List<Student> studentList)
    {
        if (studentList.size() == 0 || date.equalsIgnoreCase("")
                || listMenu.getText().toString().equalsIgnoreCase(""))
            ivAttendance.setVisibility(View.VISIBLE);
        else if (studentList.size() > 0)
            ivAttendance.setVisibility(View.GONE);

        // create adapter for list view of student
        this.studentArrayAdapter = new ArrayAdapter<Student>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, studentList)
        {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
            {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                tv.setTextColor(Color.WHITE);
                tv.setAllCaps(true);
                return view;
            }
        };

        // Register Adapter for ListView.
        this.lvAttendance.setAdapter(this.studentArrayAdapter);
    }

    // check the current mode that user is using to display student list
    private void checkDisplayOption()
    {
        if (!listMenu.getText().toString().equalsIgnoreCase(""))
        {
            if (listMenu.getText().toString().equalsIgnoreCase("In-class students"))
                displayStudentOnADate(date, 0);
            else
                displayStudentOnADate(date, 1);
        }
        else
            listMenu.setOnItemClickListener((parent, view, position, id) -> displayStudentOnADate(date, position));

    }

    // event for the date picker
    private void setDatePicker()
    {
        MaterialDatePicker.Builder builder = MaterialDatePicker.Builder.datePicker();
        builder.setTitleText("SELECT A DATE");
        materialDatePicker = builder.build();

        etDate = findViewById(R.id.etDate);
        textInputLayoutDate.setEndIconOnClickListener(v -> {
            materialDatePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        materialDatePicker.addOnPositiveButtonClickListener(selection -> {
            etDate.setText(materialDatePicker.getHeaderText());
            etDate.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            date = materialDatePicker.getHeaderText();
            textInputLayoutStudentList.setEnabled(true);
            checkDisplayOption();

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
