/*
 * CourseManagerActivity.java
 */
package com.vunguyen.vface.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.MyDatabaseHelperCourse;
import com.vunguyen.vface.helper.MyDatabaseHelperFace;
import com.vunguyen.vface.helper.MyDatabaseHelperStudent;

import java.util.ArrayList;
import java.util.List;

/**
 * This activity class implements methods and events for the Course Manager feature.
 * Also, implements several background tasks to work with server.
 */
public class CourseManagerActivity extends AppCompatActivity
{
    ListView lvCourses;
    String account;
    FloatingActionButton fabAddCourse;

    // Request code for the menu
    private static final int MENU_ITEM_VIEW = 111;
    private static final int MENU_ITEM_EDIT = 222;
    private static final int MENU_ITEM_ADD = 333;
    private static final int MENU_ITEM_DELETE = 444;

    private static final int MY_REQUEST_CODE = 1000;
    MyDatabaseHelperCourse db_course;

    //List contains the courses to display on screen
    private List<Course> courseList = new ArrayList<>();
    // Array adapter to connect ListView and data
    private ArrayAdapter<Course> courseArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Set no notification bar on activity
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_course_manager);

        // Get email account
        account = getIntent().getStringExtra("ACCOUNT");

        // Set event for the Add Course button
        fabAddCourse = findViewById(R.id.floating_action_button);
        fabAddCourse.setOnClickListener(v -> actionAddCourse());;

        // Open the course database, transfer all course from database to a course list
        db_course = new MyDatabaseHelperCourse(this);
        this.courseList = db_course.getAllCourses(account);

        // Display courses on the list view
        displayCourses();
    }

    // This method to display courses on list view through adapter
    public void displayCourses()
    {
        // initialize list view
        lvCourses = findViewById(R.id.lvCourses);
        if (courseList.size() == 0)
        {
            ImageView ivWaiting = findViewById(R.id.ivWaitingCourse);
            ivWaiting.setVisibility(View.VISIBLE);
        }
        // create adapter for list view of courses
        this.courseArrayAdapter = new ArrayAdapter<Course>(this,
                android.R.layout.simple_list_item_activated_1, android.R.id.text1, this.courseList)
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
        this.lvCourses.setAdapter(this.courseArrayAdapter);

        // Register the menu context for ListView.
        registerForContextMenu(this.lvCourses);

    }

    // Create Menu context for ListView
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, view, menuInfo);

        menu.add(0, MENU_ITEM_VIEW , 0, "View Course Information");
        menu.add(0, MENU_ITEM_ADD , 1, "Add Course");
        menu.add(0, MENU_ITEM_EDIT , 2, "Edit Course");
        menu.add(0, MENU_ITEM_DELETE, 4, "Delete Course");
    }

    // Implement actions for each menu item
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo
                info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        // get the selected course
        final Course selectedCourse = (Course) this.lvCourses.getItemAtPosition(info.position);

        if(item.getItemId() == MENU_ITEM_VIEW)
        {
            // Display a dialog with student information
            new MaterialAlertDialogBuilder(this)
                    .setTitle("VFACE - COURSE MANAGER")
                    .setMessage("Course ID Number: " + selectedCourse.getCourseIdNumber() +
                            "\nCourse Name: " + selectedCourse.getCourseName())
                    .setCancelable(false)
                    .setNegativeButton("OK", null)
                    .show();
        }
        else if(item.getItemId() == MENU_ITEM_ADD)
        {
            actionAddCourse();
        }
        else if(item.getItemId() == MENU_ITEM_EDIT )
        {
            Intent intent = new Intent(this, AddEditCourseActivity.class);
            intent.putExtra("course", selectedCourse);
            intent.putExtra("ACCOUNT", account);
            this.startActivityForResult(intent,MY_REQUEST_CODE);
        }
        else if(item.getItemId() == MENU_ITEM_DELETE)
        {
            // Confirmation dialog before delete
            new MaterialAlertDialogBuilder(this)
                    .setTitle("VFACE - COURSE MANAGER")
                    .setMessage("Are you sure you want to delete " + selectedCourse.getCourseName() + "?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            deleteCourse(selectedCourse);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
        else
            return false;
        return true;
    }

    // This method to open the add/edit course screen
    public void actionAddCourse()
    {
        Intent intent = new Intent(this, AddEditCourseActivity.class);
        intent.putExtra("ACCOUNT", account);
        this.startActivityForResult(intent, MY_REQUEST_CODE);
    }

    // Delete the course if the user accepts
    private void deleteCourse(Course course)
    {
        String courseServerId = course.getCourseServerId();
        db_course.deleteCourse(course); // delete course from database
        this.courseList.remove(course); // delete course from the list
        new DeleteCourseTask().execute(courseServerId); // delete course from server
        MyDatabaseHelperStudent db_student = new MyDatabaseHelperStudent(this);
        MyDatabaseHelperFace db_face = new MyDatabaseHelperFace(this);
        // delete all students in the course
        db_student.deleteStudentWithCourse(courseServerId, db_face);
        Log.i("EXECUTE", "Deleted course: " + courseServerId);
        // Refresh ListView.
        if (courseList.size() == 0)
        {
            ImageView ivWaiting = findViewById(R.id.ivWaitingCourse);
            ivWaiting.setVisibility(View.VISIBLE);
        }
        this.courseArrayAdapter.notifyDataSetChanged();
    }


    // Response from AddEditCourse Activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == MY_REQUEST_CODE)
        {
            boolean needRefresh = data.getBooleanExtra("needRefresh", true);
            // Refresh ListView
            if (needRefresh)
            {
                this.courseList.clear();
                List<Course> list = db_course.getAllCourses(account);
                this.courseList.addAll(list);
                // Notify the change of data to refresh the listview
                this.courseArrayAdapter.notifyDataSetChanged();
            }
        }
    }

    // Click event arrow button
    public void btnBackArrow(View view)
    {
        onBackPressed();
    }

    // Finish activity and go to previous activity

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(CourseManagerActivity.this, StudentCoursesActivity.class);
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
        finish();
    }

    // Deleting a course from server - running background

    /**
     * This class is to remove a course from server running in background
     */
    private class DeleteCourseTask extends AsyncTask<String, String, String>
    {
        @Override
        protected String doInBackground(String... params)
        {
            // Connect to server
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                faceServiceClient.deleteLargePersonGroup(params[0]);
                return params[0];
            }
            catch (Exception e)
            {
                Log.i("EXECUTE", e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (result != null)
            {
                Toast.makeText(getApplicationContext(),
                        "The course has been deleted.", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(getApplicationContext(),
                        "This course has not been deleted. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
