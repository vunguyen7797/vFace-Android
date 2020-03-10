/*
 * StudentManagerActivity.java
 */
package com.vunguyen.vface.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.MyDatabaseHelperCourse;
import com.vunguyen.vface.helper.MyDatabaseHelperFace;
import com.vunguyen.vface.helper.MyDatabaseHelperStudent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This class implements functions for student manager screen activity
 */
public class StudentManagerActivity extends AppCompatActivity
{
    // data list
    private List<Course> courseList = new ArrayList<>();
    private List<Student> studentList = new ArrayList<>();

    // array adapter and database
    private ArrayAdapter<Student> studentArrayAdapter;
    private MyDatabaseHelperStudent db_student;
    private MyDatabaseHelperCourse db_course;

    // variables
    String account;
    private int courseId = 0;
    private String courseServerId ="";

    // Menu request code
    private static final int MENU_ITEM_VIEW = 111;
    private static final int MENU_ITEM_EDIT = 222;
    private static final int MENU_ITEM_ADD = 333;
    private static final int MENU_ITEM_DELETE = 444;
    private static final int MY_REQUEST_CODE = 1000;

    // functions
    Button btnDone;
    Button btnAddStudent;
    AutoCompleteTextView courseMenu;
    GridView gvStudents;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Set no notification bar on activity
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_student_manager);

        // get account to identify the database
        account = getIntent().getStringExtra("ACCOUNT");

        // get all courses belong to this account
        db_course = new MyDatabaseHelperCourse(this);
        this.courseList = db_course.getAllCourses(account);

        // initialize course menu
        courseMenu = findViewById(R.id.filled_exposed_dropdown);
        ArrayAdapter<Course> tvArrayAdapter = new ArrayAdapter<Course>(this, R.layout.dropdown_menu_popup_item, courseList);
        courseMenu.setAdapter(tvArrayAdapter);

        // initialize student gridview and student database
        gvStudents = findViewById(R.id.gvStudents);
        db_student = new MyDatabaseHelperStudent(this);
        btnAddStudent = findViewById(R.id.btnAddStudent);
        btnAddStudent.setEnabled(false);

        if (courseList.size() != 0)
        {
            // set default
            displayGridView("",0);
            courseMenu.setOnItemClickListener((parent, view, position, id) ->
            {
                courseId = (int) parent.getItemIdAtPosition(position);  // get the course id database
                Course course = (Course) parent.getItemAtPosition(position);
                courseServerId = course.getCourseServerId();            // get course id on server
                displayGridView(courseServerId, 1);   // request 1 to notify that selection is changed
                btnAddStudent.setEnabled(true);

            });

            // Button Add Student event, send the course ids to the student profile activity
            btnAddStudent.setOnClickListener(v ->
                    addStudent());
        }
        else
        {
            // If there is no course, the button is disable and show notification
            btnAddStudent.setEnabled(false);
            btnAddStudent.setOnClickListener(v ->
                    Toast.makeText(getApplicationContext(), "Addd a course before adding students.",
                            Toast.LENGTH_SHORT).show());
        }

        // Button Done Click Event
        btnDone = findViewById(R.id.btnDoneStudentsManager);
        btnDone.setOnClickListener(v ->
                onBackPressed());
    }

    // Event for clicking the back button on navigation bar
    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(StudentManagerActivity.this, StudentCoursesActivity.class);
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
        finish();
    }

    // Create menu for each grid view item
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.add(0, MENU_ITEM_VIEW , 0, "View Student Information");
        menu.add(0, MENU_ITEM_ADD , 1, "Add Student");
        menu.add(0, MENU_ITEM_EDIT , 2, "Edit Student");
        menu.add(0, MENU_ITEM_DELETE, 4, "Delete Student");
    }

    // Set action for each item selected on menu
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo
                info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        // Get selected student to apply action
        final Student selectedStudent = (Student) this.gvStudents.getItemAtPosition(info.position);

        if(item.getItemId() == MENU_ITEM_VIEW)
        {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("VFACE - STUDENT MANAGER")
                    .setMessage("Student ID Number: " + selectedStudent.getStudentIdNumber() +
                            "\nStudent Name: " + selectedStudent.getStudentName())
                    .setCancelable(false)
                    .setNegativeButton("OK", null)
                    .show();
        }
        else if(item.getItemId() == MENU_ITEM_ADD)
        {
            addStudent();
        }
        else if (item.getItemId() == MENU_ITEM_EDIT)
        {
            Intent intent = new Intent(StudentManagerActivity.this, StudentDataActivity.class);
            intent.putExtra("courseServerId", courseServerId);
            intent.putExtra("courseId", courseId);
            intent.putExtra("account", account);
            intent.putExtra("student", selectedStudent);
            startActivityForResult(intent, MY_REQUEST_CODE);
        }
        else if(item.getItemId() == MENU_ITEM_DELETE)
        {
            // Confirmation dialog before delete
            new MaterialAlertDialogBuilder(this)
                    .setTitle("VFACE - STUDENT MANAGER")
                    .setMessage("Are you sure you want to delete " + selectedStudent.getStudentName() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteStudent(selectedStudent))
                    .setNegativeButton("No", null)
                    .show();
        }
        else
        {
            return false;
        }
        return true;
    }

    // Go to the student profile activity to add student
    private void addStudent()
    {
        Intent intent = new Intent(StudentManagerActivity.this, StudentDataActivity.class);
        Bundle b = new Bundle();
        b.putInt("courseId", courseId);
        b.putString("courseServerId", courseServerId);
        b.putString("account", account);
        intent.putExtra("CourseId", b);
        startActivityForResult(intent, MY_REQUEST_CODE);
    }


    // Delete a student from database
    private void deleteStudent(Student student)
    {
        db_student.deleteStudent(student);
        this.studentList.remove(student);
        // delete faces in database that belong to this student
        MyDatabaseHelperFace db_face = new MyDatabaseHelperFace(this);
        db_face.deleteFacesWithStudent(student.getStudentServerId());
        // Refresh ListView.
        this.studentArrayAdapter.notifyDataSetChanged();
        // execute background task to delete student from server
        new DeleteStudentTask(courseServerId).execute(student.getStudentServerId());
    }


    // Refresh the student list after adding new student
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == Activity.RESULT_OK && requestCode == MY_REQUEST_CODE ) {
            boolean needRefresh = data.getBooleanExtra("needRefresh",true);
            // Refresh ListView
            if(needRefresh)
            {
                this.studentList.clear();
                MyDatabaseHelperStudent db_student = new MyDatabaseHelperStudent(this);
                List<Student> list=  db_student.getStudentWithCourse(courseServerId);
                this.studentList.addAll(list);
                // Notify data changed to grid view
                this.studentArrayAdapter.notifyDataSetChanged();
            }
        }
    }

    // Display the information of student on GridView based on course selection
    private void displayGridView(String courseServerId, int request)
    {
        if (request == 0) // default data is the student of first course in the list
        {
            List<Student> listStudents =  db_student.getStudentWithCourse(courseServerId);
            this.studentList.addAll(listStudents);

            studentArrayAdapter = new ArrayAdapter<Student>(this,
                    android.R.layout.simple_list_item_activated_1, android.R.id.text1, studentList)
            {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
                {
                    View view = super.getView(position, convertView, parent);
                    TextView tv = view.findViewById(android.R.id.text1);
                    tv.setTextColor(Color.WHITE);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    tv.setAllCaps(true);
                    return view;
                }
            };

            // Register Adapter cho ListView.
            this.gvStudents.setAdapter(this.studentArrayAdapter);
            // Register the menu context
            registerForContextMenu(this.gvStudents);
        }
        else if (request == 1) // data changed after user selected another course
        {
            this.studentList.clear();
            MyDatabaseHelperStudent db = new MyDatabaseHelperStudent(this);
            List<Student> list = db.getStudentWithCourse(courseServerId);
            this.studentList.addAll(list);
            this.studentArrayAdapter.notifyDataSetChanged();

        }

    }

    /**
     * This class is a background task to delete a student and its information on server
     */
    class DeleteStudentTask extends AsyncTask<String, String, String>
    {
        String courseServerId;
        DeleteStudentTask(String courseServerId)
        {
            this.courseServerId = courseServerId;
        }

        @Override
        protected String doInBackground(String... params)
        {
            // Connect to server
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                Log.i("EXECUTE","Request: Deleting student " + params[0]);
                UUID studentServerId = UUID.fromString(params[0]);
                faceServiceClient.deletePersonInLargePersonGroup(courseServerId, studentServerId);
                return params[0];
            }
            catch (Exception e)
            {
                Log.i("EXECUTE","ERROR DELETE STUDENT FROM SERVER: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (result != null)
            {
                Log.i("EXECUTE", "Response: Success. Deleting student " + result + " succeed");
            }
        }
    }
}
