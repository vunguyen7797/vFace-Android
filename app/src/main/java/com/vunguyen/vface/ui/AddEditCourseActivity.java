/*
 * AddEditCourseActivity.java
 */
package com.vunguyen.vface.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.LocaleHelper;
import com.vunguyen.vface.helper.asyncTasks.AddCourseTask;

import java.util.Objects;
import java.util.UUID;

import edmt.dev.edmtdevcognitiveface.FaceServiceClient;

/**
 * This class contains background tasks to work with server
 * and implements events for activity properties.
 */
public class AddEditCourseActivity extends AppCompatActivity
{
    Button btnCancel;
    Course course;
    TextInputEditText etCourseID;
    TextInputEditText etCourseName;

    // Request code
    private static final int MODE_ADD = 1;
    private static final int MODE_EDIT = 2;

    String courseServerId;
    String account; // email address of the user
    private int mode;
    private boolean needRefresh = true; // signal flag to refresh data
    DatabaseReference mDatabase;

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_course);

        initView();
        initData();
        initAction();
    }

    private void initAction()
    {
        btnCancel.setOnClickListener(v -> onBackPressed());
    }

    private void initData()
    {
        // Extract data passed from the previous activity
        Intent intentData = this.getIntent();
        this.course = (Course) intentData.getSerializableExtra("course");
        account = getIntent().getStringExtra("ACCOUNT");
        mDatabase = FirebaseDatabase.getInstance().getReference().child(account).child("course");
        // set mode of the activity: add a new course or edit an available course
        if(course == null)
        {
            this.mode = MODE_ADD;
        }
        else
        {
            this.mode = MODE_EDIT;
            // set the course Id field and the course name field with current data
            this.etCourseID.setText(course.getCourseIdNumber());
            this.etCourseName.setText(course.getCourseName());
            courseServerId = course.getCourseServerId();
        }
    }

    private void initView()
    {
        // Set event for Cancel button
        btnCancel = findViewById(R.id.btnCancel);
        etCourseID = findViewById(R.id.etCourseID);
        etCourseName = findViewById(R.id.etCourseName);
    }

    /**
     * ******************* Event handler methods **********************
     */
    @Override
    public void onBackPressed()
    {
        // Go back to the Course Manager activity
        Intent intent = new Intent (AddEditCourseActivity.this, CourseManagerActivity.class);
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
        finish();
    }

    // Set click event for Save button
    public void btnSaveClick(View view)
    {
        // Get input from the edit text fields
        String courseNumberId = Objects.requireNonNull(this.etCourseID.getText()).toString();
        String courseName = Objects.requireNonNull(this.etCourseName.getText()).toString();

        // Check if input are null or not
        if (courseNumberId.equals("") || courseName.equals(""))
        {
            Toast.makeText(getApplicationContext(),
                    "Please enter course ID & course name.", Toast.LENGTH_LONG).show();
        }
        else
        {
            if (mode == MODE_ADD)  // Add new course to the database
            {
                // Generate a server string Id for the new course
                courseServerId = UUID.randomUUID().toString();
                // Execute the background task to create a course on server
                //new AddCourseTask().execute(courseServerId);
                new com.vunguyen.vface.helper.asyncTasks.AddCourseTask(AddEditCourseActivity.this).execute(courseServerId);
                this.course = new Course(courseNumberId, courseName, courseServerId, account);
                mDatabase.child(courseName.toUpperCase() + "-" + courseServerId).setValue(course);

            }
            else // Mode is edit. Update the course data in the database
            {
                if (!course.getCourseName().equalsIgnoreCase(courseName))
                {
                    mDatabase.child(course.getCourseName().toUpperCase() + "-" + courseServerId).removeValue();
                }
                    Log.i("EXECUTE", "Course old name: " + course.getCourseName());
                    this.course.setCourseIdNumber(courseNumberId);
                    this.course.setCourseName(courseName);
                    mDatabase.child(courseName.toUpperCase() + "-" + courseServerId).setValue(course);
            }
        }
        this.needRefresh = true; // flag to refresh the data
        // Go back to previous activity
        Intent intent = new Intent(AddEditCourseActivity.this, CourseManagerActivity.class);
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
        //finish();
    }

    public void btnBackClick (View view)
    {
        onBackPressed();
    }

    // Override Finish method to close the activity and send request refreshing
    // the list of course after adding or deleting a course.
    @Override
    public void finish ()
    {
        // Prepare Intent data
        Intent data = new Intent();
        // Request the List Course refresh
        data.putExtra("needRefresh", needRefresh);
        // Activity complete
        this.setResult(Activity.RESULT_OK, data);
        super.finish();
    }
}
