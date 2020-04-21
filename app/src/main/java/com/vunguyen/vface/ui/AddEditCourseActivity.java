/*
 * AddEditCourseActivity.java
 */
package com.vunguyen.vface.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.LocaleHelper;
import com.vunguyen.vface.helper.ProgressDialogCustom;
import com.vunguyen.vface.helper.asyncTasks.AddCourseTask;

import java.util.Objects;
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
    ProgressDialogCustom progressDialogCustom;

    // Request code
    private static final int MODE_ADD = 1;
    private static final int MODE_EDIT = 2;
    private static final int MODE_IMPORT = 3;

    String courseServerId;
    String courseNumberId;
    String courseName;
    String account;
    private int mode;
    private boolean needRefresh = true; // signal flag to refresh data

    DatabaseReference mDatabase;
    DatabaseReference mDatabase_saved;

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
        mDatabase_saved = FirebaseDatabase.getInstance().getReference().child(account).child("account_storage");
        // set mode of the activity: add a new course or edit an available course
        if(course == null)
        {
            this.mode = MODE_ADD;
            courseNumberId = Objects.requireNonNull(etCourseID.getText()).toString();
            if (!courseNumberId.equalsIgnoreCase(""))
            {
                courseName = Objects.requireNonNull(etCourseName.getText()).toString();
            }
            else
            {
                etCourseName.setEnabled(false);
                etCourseID.requestFocus();
                etCourseID.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after)
                    {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count)
                    {
                       // need to remove this to run only once
                        handler.removeCallbacks(input_finish_checker);
                    }

                    @Override
                    public void afterTextChanged(Editable s)
                    {
                        //avoid triggering event when text is empty
                        if (mode != MODE_IMPORT && Objects.requireNonNull(etCourseName.getText())
                                .toString().equalsIgnoreCase(""))
                        {
                            if (s.length() > 0)
                            {
                                last_text_edit = System.currentTimeMillis();
                                handler.postDelayed(input_finish_checker, delay);
                            }
                            else
                            {
                                etCourseName.setText("");
                                etCourseName.setEnabled(false);
                            }
                        }
                    }
                });
            }
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
        progressDialogCustom = new ProgressDialogCustom(this);
    }

    /**
     * ******************* Event handler methods **********************
     */

    long delay = 1500; // 1 seconds after user stops typing
    long last_text_edit = 0;
    Handler handler = new Handler();

    // verifying available course in data storage when finish typing course ID
    private Runnable input_finish_checker = () -> {
        if (System.currentTimeMillis() > (last_text_edit + delay - 500))
        {
            Log.i("EXECUTE", "FINISH TYPING!!!");
            etCourseName.setEnabled(true);
            progressDialogCustom.startProgressDialog("Verifying ID...");
            verifyCourse(Objects.requireNonNull(etCourseID.getText()).toString());
        }
    };

    private void verifyCourse(String courseId)
    {
        mDatabase_saved.child("course").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                Log.i("EXECUTE", "Starting verifying course....");
                boolean inStorage = false;
                Course course = null;
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Course temp = dsp.getValue(Course.class);
                    assert temp != null;
                    if (courseId.equalsIgnoreCase(temp.getCourseIdNumber()))
                    {
                        inStorage = true;
                        course = temp;
                    }
                }
                Log.i("EXECUTE", "In Storage: " + inStorage);
                if (inStorage)
                {
                    Log.i("EXECUTE", "Start dialog to request import COURSE from storage");
                    Course finalCourse = course;
                    final Handler handler = new Handler();
                    handler.postDelayed(() ->
                    {
                        progressDialogCustom.dismissDialog();
                    new MaterialAlertDialogBuilder(AddEditCourseActivity.this)
                            .setTitle("VFACE - COURSE DATA")
                            .setMessage("Found this course in database.\nDo you want to import available data?")
                            .setCancelable(false)
                            .setPositiveButton("Yes", ((dialog, which) ->
                                    importCourse(finalCourse)) )
                            .setNegativeButton("No", null)
                            .show();
                    }, 2000);
                }
                else
                {
                    final Handler handler = new Handler();
                    handler.postDelayed(() ->
                    {
                        progressDialogCustom.dismissDialog();
                        Toast.makeText(getApplicationContext(), "Checking ID completed", Toast.LENGTH_SHORT).show();
                    }, 2000);
                    Log.i("EXECUTE", "No Course found in storage");
                }

                mDatabase_saved.child("course").removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void importCourse(Course courseImport)
    {
        courseNumberId = courseImport.getCourseIdNumber();
        courseName = courseImport.getCourseName();
        etCourseName.setText(courseName);
        etCourseID.setText(courseNumberId.toUpperCase());
        courseServerId = UUID.randomUUID().toString();
        this.course = new Course(courseNumberId, courseName, courseServerId);
        mode = MODE_IMPORT;
        progressDialogCustom.dismissDialog();
    }

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
       if (mode == MODE_ADD || mode == MODE_EDIT)
           saveData(0);
       else if (mode == MODE_IMPORT)
           saveData(1);


    }

    private void saveData(int request)
    {
        if (request == 0)
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
                    new AddCourseTask(AddEditCourseActivity.this).execute(courseServerId);
                    this.course = new Course(courseNumberId, courseName, courseServerId);
                    mDatabase.child(courseName.toUpperCase() + "-" + courseServerId).setValue(course);
                    addCourseToStorage(courseNumberId, courseName);
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
        }
        else
        {
            // Save data if requested after import data
            new AddCourseTask(AddEditCourseActivity.this).execute(courseServerId);
            new TrainCourseTask().execute(courseServerId);
            mDatabase.child(courseName.toUpperCase() + "-" + courseServerId).setValue(this.course);
        }
        this.needRefresh = true; // flag to refresh the data
        // Go back to previous activity
        Intent intent = new Intent(AddEditCourseActivity.this, CourseManagerActivity.class);
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
        //finish();
    }

    private void addCourseToStorage(String courseNumberId, String courseName)
    {
        mDatabase_saved.child("course").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                boolean inDatabase = false;
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Course temp = dsp.getValue(Course.class);
                    assert temp != null;
                    if (temp.getCourseIdNumber().equalsIgnoreCase(courseNumberId))
                        inDatabase = true;
                }
                if (!inDatabase)
                {
                    String path = courseName.toUpperCase() + "-" + courseServerId;
                    mDatabase_saved.child("course").child(path).setValue(course);
                    new TrainCourseTask().execute(courseServerId);
                }
                mDatabase_saved.child("course").removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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

    class TrainCourseTask extends AsyncTask<String, String, String>
    {
        @Override
        protected String doInBackground(String... params)
        {
            Log.i("EXECUTE","Request: Training group " + params[0]);
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                publishProgress("Training course...");
                faceServiceClient.trainLargePersonGroup(params[0]);
                return params[0];
            }
            catch (Exception e)
            {
                publishProgress(e.getMessage());
                Log.i("EXECUTE","Error training group: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (result != null)
            {
                Log.i("EXECUTE","Response: Success. Course " + result + " training completed");
                finish();
            }
            else
                Toast.makeText(getApplicationContext(), "COURSE IS NOT TRAINED", Toast.LENGTH_SHORT).show();
        }
    }
}
