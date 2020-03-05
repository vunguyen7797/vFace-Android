package com.vunguyen.vface.ui;

import android.app.Activity;
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

public class AddEditCourseActivity extends AppCompatActivity
{
    // Background task of adding a course to server.
    class AddCourseTask extends AsyncTask<String, String, String>
    {
        @Override
        protected String doInBackground(String... params)
        {
            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            Log.i("EXECUTE", "Call Face Service Done");
            try
            {
                Log.i("EXECUTE", "Syncing with server to add course");

                // Start creating a course as a person group in server.
                faceServiceClient.createLargePersonGroup(params[0], "Name", "User Data");
                return params[0];
            }
            catch (Exception e)
            {
                Log.i("EXECUTE", "ERRORS OCCUR!");
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (result != null)
            {
                Log.i("EXECUTE", "Course created successfully on server.");
            }
        }
    }

    // Background task of training a group of student (course) on server
    class TrainCourseTask extends AsyncTask<String, String, String>
    {
        @Override
        protected String doInBackground(String... params)
        {
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                Log.i("EXECUTE", "Training the student group (course)");
                faceServiceClient.trainLargePersonGroup(params[0]);
                return params[0];

            }
            catch (Exception e)
            {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            //progressDialog.dismiss();

            if (result != null)
            {
                Toast.makeText(getApplicationContext(), "Success. Course " + result + " trained", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    Button btnCancel;
    Course course;
    private static final int MODE_ADD = 1;
    private static final int MODE_EDIT = 2;

    String courseServerId = "";
    private int mode;
    private TextInputEditText etCourseID;
    private TextInputEditText etCourseName;

    private boolean needRefresh;

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
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AddEditCourseActivity.this, AddCourseActivity.class));
            }
        });

        etCourseID = findViewById(R.id.etCourseID);
        etCourseName = findViewById(R.id.etCourseName);

        Intent intent = this.getIntent();
        this.course = (Course) intent.getSerializableExtra("course");
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
    }


    // Click event for Save button
    public void btnSaveClick(View view)
    {
        MyDatabaseHelperCourse db = new MyDatabaseHelperCourse(this);

        String  courseNumberId = this.etCourseID.getText().toString();
        String courseName = this.etCourseName.getText().toString();


        if(courseNumberId.equals("") || courseName.equals("")) {
            Toast.makeText(getApplicationContext(),
                    "Please enter course ID & course name.", Toast.LENGTH_LONG).show();
            return;
        }

        if(mode == MODE_ADD )  // Add new course to the database
        {
            // Generate a server string Id for the new course
            courseServerId = UUID.randomUUID().toString();
            Log.i("EXECUTE", "Execute Background task for adding course: " + courseServerId);
            new AddCourseTask().execute(courseServerId);

            this.course = new Course(courseNumberId, courseName, courseServerId);

            Log.i("EXECUTE", "Adding course to database");
            db.addCourse(course);


        }
        else // Update the course data in the database
        {
                this.course.setCourseIdNumber(courseNumberId);
                this.course.setCourseName(courseName);
                db.updateCourse(course);
        }

        this.needRefresh = true;

        // Back to the previous activity
        startActivity(new Intent(AddEditCourseActivity.this, AddCourseActivity.class));
    }

    // Finish activity
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
}
