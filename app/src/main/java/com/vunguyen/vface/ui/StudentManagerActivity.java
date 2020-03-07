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
import android.widget.Spinner;
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

public class StudentManagerActivity extends AppCompatActivity {

    Spinner spinCourses;
    private final List<Course> courseList = new ArrayList<>();

    private List<Student> studentList = new ArrayList<>();
    private ArrayAdapter<Student> gridViewAdapter;
    private MyDatabaseHelperStudent db_student;

    GridView gvStudents;
    String account;

    // Menu request code
    private static final int MENU_ITEM_VIEW = 111;
    private static final int MENU_ITEM_EDIT = 222;
    private static final int MENU_ITEM_ADD = 333;
    private static final int MENU_ITEM_DELETE = 444;
    private static final int MY_REQUEST_CODE = 1000;

    Button btnDone;
    Button btnAddStudent;
    private int courseId = 0;
    private String courseServerId ="";
    AutoCompleteTextView courseMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Set no notification bar on activity
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_student_manager);

        account = getIntent().getStringExtra("ACCOUNT");



        // Display courses on spinner
        //spinCourses = findViewById(R.id.spinClass);
        MyDatabaseHelperCourse db = new MyDatabaseHelperCourse(this);
        List<Course> listCourses=  db.getAllCourses(account);
        this.courseList.addAll(listCourses);
        //ArrayAdapter<Course> spinnerArrayAdapter = new ArrayAdapter<Course>(this, R.layout.spinner_item, listCourses);
       // spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_item);
        //spinCourses.setAdapter(spinnerArrayAdapter);


        courseMenu = findViewById(R.id.filled_exposed_dropdown);
        ArrayAdapter<Course> tvArrayAdapter = new ArrayAdapter<Course>(this, R.layout.dropdown_menu_popup_item, listCourses);
        courseMenu.setAdapter(tvArrayAdapter);


        // Display students on grid view
        gvStudents = findViewById(R.id.gvStudents);
        db_student = new MyDatabaseHelperStudent(this);

        // Default display will be the students of the first course in the list
        if (courseList.size() != 0)
        {
            //courseMenu.setSelection(0);
           // courseMenu.setText(tvArrayAdapter.getItem(0).getCourseName());
            //Course course = (Course) tvArrayAdapter.getItem(0);
            //courseServerId = course.getCourseServerId();
            displayGridView("",0);
                //spinCourses.setSelection(0);
                //Course course = (Course) spinCourses.getItemAtPosition(0);
            courseMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    courseId = (int) parent.getItemIdAtPosition(position); // get the course id database
                    Course course = (Course) parent.getItemAtPosition(position);
                    courseServerId = course.getCourseServerId();    // get course id on server
                    // Log.i("EXECUTE", "Course Selected: " + courseServerId);
                    displayGridView(courseServerId, 1);   // request 1 to notify that selection is changed

                }
            });
/*
            // display list of students on course selection
            spinCourses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                     courseId = (int) parent.getItemIdAtPosition(position); // get the course id database
                     Course course = (Course) parent.getItemAtPosition(position);
                     courseServerId = course.getCourseServerId();    // get course id on server
                    // Log.i("EXECUTE", "Course Selected: " + courseServerId);
                     displayGridView(courseServerId, 1);   // request 1 to notify that selection is changed
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent)
                {

                }
            });
*/
            // Button Add Student event, send the course ids to the student profile activity
            btnAddStudent = findViewById(R.id.btnAddStudent);
            btnAddStudent.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(StudentManagerActivity.this, StudentDataActivity.class);
                    Bundle b = new Bundle();
                    b.putInt("courseId", courseId);
                    b.putString("courseServerId", courseServerId);
                    b.putString("account", account);
                    intent.putExtra("CourseId", b);
                    startActivityForResult(intent, MY_REQUEST_CODE);
                }
            });
        }
        else
        {
            btnAddStudent = findViewById(R.id.btnAddStudent);
            btnAddStudent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getApplicationContext(), "Addd a course before adding students.", Toast.LENGTH_SHORT).show();
                }
            });
        }


        // Button Done Click Event
        btnDone = findViewById(R.id.btnDoneStudentsManager);
        btnDone.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(StudentManagerActivity.this, StudentCoursesActivity.class);
                intent.putExtra("ACCOUNT", account);
                startActivity(intent);
                finish();
            }
        });


    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(StudentManagerActivity.this, StudentCoursesActivity.class);
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo)
    {

        super.onCreateContextMenu(menu, view, menuInfo);

        // groupId, itemId, order, title
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
            new AlertDialog.Builder(this)
                    .setMessage("Student ID Number: " + selectedStudent.getStudentIdNumber() +
                            "\nStudent Name: " + selectedStudent.getStudentName()+
                            "\nStudent UID: " + selectedStudent.getStudentServerId()+
                            "\nCourse UID: " + selectedStudent.getCourseServerId())
                    .setCancelable(false)
                    .setNegativeButton("OK", null)
                    .show();
        }
        else if(item.getItemId() == MENU_ITEM_ADD)
        {
            Intent intent = new Intent(StudentManagerActivity.this, StudentDataActivity.class);
            Bundle b = new Bundle();
            b.putInt("courseId", courseId);
            b.putString("courseServerId", courseServerId);
            b.putString("account", account);
            intent.putExtra("CourseId", b);
            startActivityForResult(intent, MY_REQUEST_CODE);
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
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteStudent(selectedStudent);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();

        }
        else
        {
            return false;
        }
        return true;
    }

    // Display the information of student on GridView based on course selection
    private void displayGridView(String courseServerId, int request)
    {
        if (request == 0) // default data is the student of first course in the list
        {
            List<Student> listStudents =  db_student.getStudentWithCourse(courseServerId);
            this.studentList.addAll(listStudents);

            gridViewAdapter = new ArrayAdapter<Student>(this,
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
            this.gvStudents.setAdapter(this.gridViewAdapter);
            // Register the menu context
            registerForContextMenu(this.gvStudents);
        }
        else if (request == 1) // data changed after user selected another course
        {
            this.studentList.clear();
            MyDatabaseHelperStudent db = new MyDatabaseHelperStudent(this);
            List<Student> list = db.getStudentWithCourse(courseServerId);
            this.studentList.addAll(list);
            this.gridViewAdapter.notifyDataSetChanged();

        }

    }

    static class DeleteStudentTask extends AsyncTask<String, String, String>
    {
        String courseServerId;
        DeleteStudentTask(String courseServerId)
        {
            this.courseServerId = courseServerId;
        }
        @Override
        protected String doInBackground(String... params)
        {
            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                publishProgress("Deleting selected student...");
                Log.i("EXECUTE","Request: Deleting student " + params[0]);

                UUID studentServerId = UUID.fromString(params[0]);
                faceServiceClient.deletePersonInLargePersonGroup(courseServerId, studentServerId);
                return params[0];
            } catch (Exception e) {
                publishProgress(e.getMessage());
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

    // Delete a student from database
    private void deleteStudent(Student student)
    {
        MyDatabaseHelperStudent db = new MyDatabaseHelperStudent(this);
        db.deleteStudent(student);
        this.studentList.remove(student);
        MyDatabaseHelperFace db_face = new MyDatabaseHelperFace(this);
        db_face.deleteFacesWithStudent(student.getStudentServerId());
        // Refresh ListView.
        this.gridViewAdapter.notifyDataSetChanged();
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
                MyDatabaseHelperStudent db = new MyDatabaseHelperStudent(this);
                List<Student> list=  db.getStudentWithCourse(courseServerId);
                this.studentList.addAll(list);
                // Notify data changed to grid view
                this.gridViewAdapter.notifyDataSetChanged();
            }
        }
    }
}
