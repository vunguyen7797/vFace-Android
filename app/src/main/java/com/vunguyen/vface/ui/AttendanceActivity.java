package com.vunguyen.vface.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.google.android.gms.maps.model.Dash;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.textfield.TextInputLayout;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.helper.MyDatabaseHelperCourse;
import com.vunguyen.vface.helper.MyDatabaseHelperStudent;

import java.util.ArrayList;
import java.util.List;

public class AttendanceActivity extends AppCompatActivity
{
    // UI feature
    MaterialDatePicker materialDatePicker;
    TextInputLayout textInputLayoutDate;
    EditText etDate;
    AutoCompleteTextView courseMenu;
    AutoCompleteTextView listMenu;

    List<Course> courseList;    // store list of course objects
    List<Student> studentList;  // store list of student in a course

    //database
    MyDatabaseHelperCourse db_course;
    MyDatabaseHelperStudent db_student;

    // variables
    String account;
    Course course;
    String courseServerId;

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

        //get all courses
        this.courseList = db_course.getAllCourses(account);

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
        studentListOptions.add("Unknown students");

        listMenu = findViewById(R.id.filled_exposed_dropdown_2);
        ArrayAdapter<String> tvArrayAdapter = new ArrayAdapter<>(this, R.layout.dropdown_menu_popup_item, studentListOptions);
        listMenu.setAdapter(tvArrayAdapter);


    }

    // display courses on the autocomplete textview menu to pick the course
    private void displayCourseMenu(List<Course> courseList)
    {
        // initialize course menu
        courseMenu = findViewById(R.id.filled_exposed_dropdown);
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

            });
        }
        else
        {
            Log.i("EXECUTE", "No courses are available.");
        }
    }

    private void setDatePicker()
    {
        MaterialDatePicker.Builder builder = MaterialDatePicker.Builder.datePicker();
        builder.setTitleText("SELECT A DATE");
        materialDatePicker = builder.build();

        etDate = findViewById(R.id.etDate);

        textInputLayoutDate = findViewById(R.id.outlineDatePicker);
        textInputLayoutDate.setEndIconOnClickListener(v -> materialDatePicker.show(getSupportFragmentManager(), "DATE_PICKER"));

        materialDatePicker.addOnPositiveButtonClickListener(selection -> {
            etDate.setText(materialDatePicker.getHeaderText());
            etDate.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        });
    }
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
