/*
 * AddEditCourseActivity.java
 */
package com.vunguyen.vface.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.MyDatabaseHelperCourse;

import java.util.UUID;

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
    ProgressDialog progressDialog;

    // Request code
    private static final int MODE_ADD = 1;
    private static final int MODE_EDIT = 2;

    String courseServerId;
    private int mode;
    String account; // email address of the user

    private boolean needRefresh; // signal flag to refresh data

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Set no notification bar on activity
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_add_edit_course);

        // Set event for Cancel button
        btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> {
            // Go back to the Course Manager activity
            Intent intent = new Intent (AddEditCourseActivity.this, CourseManagerActivity.class);
            intent.putExtra("ACCOUNT", account);
            startActivity(intent);
        });

        etCourseID = findViewById(R.id.etCourseID);
        etCourseName = findViewById(R.id.etCourseName);

        // Extract data passed from the previous activity
        Intent intentData = this.getIntent();
        this.course = (Course) intentData.getSerializableExtra("course");
        account = getIntent().getStringExtra("ACCOUNT");

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
        }

        // initializing progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("V.FACE");
    }


    // Set click event for Save button
    public void btnSaveClick(View view)
    {
        // Open database Course
        MyDatabaseHelperCourse db = new MyDatabaseHelperCourse(this);

        // Get input from the edit text fields
        String  courseNumberId = this.etCourseID.getText().toString();
        String courseName = this.etCourseName.getText().toString();

        // Check if input are null or not
        if(courseNumberId.equals("") || courseName.equals(""))
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
                new AddCourseTask().execute(courseServerId);

                this.course = new Course(courseNumberId, courseName, courseServerId, account);
                db.addCourse(course);   // add course to database


            }
            else // Mode is edit. Update the course data in the database
            {
                this.course.setCourseIdNumber(courseNumberId);
                this.course.setCourseName(courseName);
                db.updateCourse(course);
            }

            this.needRefresh = true; // flag to refresh the data

            // Go back to previous activity
            Intent intent = new Intent(AddEditCourseActivity.this, CourseManagerActivity.class);
            intent.putExtra("ACCOUNT", account);
            startActivity(intent);
        }
    }

    // Override Finish method to close the activity and send request refreshing
    // the list of course after adding or deleting a course.
    @Override
    public void finish()
    {
        // Prepare Intent data
        Intent data = new Intent();
        // Request the List Course refresh
        data.putExtra("needRefresh", needRefresh);
        // Activity complete
        this.setResult(Activity.RESULT_OK, data);
        super.finish();
    }

    /**
     * This class implements background task to create a course
     * as a large person group on Microsoft Face API server.
     */
    class AddCourseTask extends AsyncTask<String, String, String>
    {
        @Override
        protected String doInBackground(String... params)
        {
            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                publishProgress("Syncing with server...");
                Log.i("EXECUTE", "Syncing with server to add course");

                // Start creating a course as a person group in server.
                faceServiceClient.createLargePersonGroup(params[0], "Name", "User Data");
                return params[0];
            }
            catch (Exception e)
            {
                Log.i("EXECUTE", "Errors: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute()
        {
            startProgressingDialog();
        }

        @Override
        protected void onProgressUpdate(String... values)
        {
            duringTaskProgressDialog(values[0]);
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (result != null)
            {
                progressDialog.dismiss();
                Log.i("EXECUTE", "Course " + result + " created successfully on server.");
            }
            else
            {
                Log.i("EXECUTE", "Response: Course is not created on server.");
            }
        }
    }


    // Showing the progressing dialog when the task starts
    private void startProgressingDialog()
    {
        progressDialog.show();
    }

    // Showing message on progressing dialog during the task
    private void duringTaskProgressDialog(String progress)
    {
        progressDialog.setMessage(progress);
    }
}
