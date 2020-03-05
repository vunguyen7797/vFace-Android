package com.vunguyen.vface.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Face;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.MyDatabaseHelperFace;
import com.vunguyen.vface.helper.MyDatabaseHelperStudent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StudentDataActivity extends AppCompatActivity
{
    // Background task of adding a student to a course.
    class AddStudentTask extends AsyncTask<String, String, String>
    {
        // Indicate the next step is to add face in this student, or finish editing this student.
        boolean mAddFace;

        AddStudentTask (boolean addFace) {
            mAddFace = addFace;
        }

        @Override
        protected String doInBackground(String... params)
        {
            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                Log.i("EXECUTE","Request: Creating Student in the course" + params[0]);

                // Start the request to creating a person on server.
                CreatePersonResult createPersonResult = faceServiceClient.createPersonInLargePersonGroup(
                        params[0],
                        getString(R.string.user_provided_person_name),
                        getString(R.string.user_provided_description_data));
                Log.i("EXECUTE","Create Student Done");

                return createPersonResult.personId.toString();
            }
            catch (Exception e)
            {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (result != null)
            {
                Log.i("EXECUTE","Response: Success. Student " + result + " created.");
                student_serverId = result;

                if (mAddFace)
                {
                    addFace();
                } else {
                   // doneAndSave();
                }
            }
        }
    }

    class DeleteFaceTask extends AsyncTask<String, String, String>
    {
        String courseServerId;
        UUID studentServerId;

        DeleteFaceTask(String courseServerId, String studentServerId)
        {
            this.courseServerId = courseServerId;
            this.studentServerId = UUID.fromString(studentServerId);
        }

        @Override
        protected String doInBackground(String... params) {
            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                publishProgress("Deleting selected faces...");
                Log.i("EXECUTE","Request: Deleting face " + params[0]);

                UUID faceId = UUID.fromString(params[0]);
                faceServiceClient.deletePersonFaceInLargePersonGroup(courseServerId, studentServerId, faceId);
                return params[0];
            }
            catch (Exception e)
            {
                publishProgress(e.getMessage());
                Log.i("EXECUTE","ERROR DELETE FACE: " + (e.getMessage()));
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (result != null) {
                Log.i("EXECUTE","Face " + result + " successfully deleted");
                Toast.makeText(getApplicationContext(),"Face Deleted.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    class TrainCourseTask extends AsyncTask<String, String, String>
    {

        @Override
        protected String doInBackground(String... params)
        {
            Log.i("EXECUTE","Request: Training group " + params[0]);

            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                publishProgress("Training person group...");

                faceServiceClient.trainLargePersonGroup(params[0]);
                return params[0];
            }
            catch (Exception e)
            {
                publishProgress(e.getMessage());
                Log.i("EXECUTE","ERROR TRAINING GROUP: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (result != null) {
                Log.i("EXECUTE","Response: Success. Course " + result + " training completed");
                Toast.makeText(getApplicationContext(), "Response: Success. Course " + result + " trained", Toast.LENGTH_LONG).show();

                finish();
            }
        }
    }

    String student_serverId = null;

    private int courseId;
    private String courseServerId;

    private TextInputEditText etStudentID;
    private TextInputEditText etStudentName;

    private boolean needRefresh;
    Student student;

    private static final int MODE_ADD = 1;
    private static final int MODE_EDIT = 2;
    private int mode;
    boolean newStudent;

    String studentNumberId;
    String studentName;

    private static final int REQUEST_SELECT_IMAGE = 0;
    private static final int MENU_ITEM_DELETE = 111;

    GridView gvStudentFace;
    MyDatabaseHelperFace db_face;
    List<Face> faceList = new ArrayList<>();
    private ArrayAdapter<Face> gridViewAdapter;
    List<Boolean> faceChecked;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Set no notification bar on activity
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_student_data);

        // initialize face database
        db_face = new MyDatabaseHelperFace(this);

        etStudentID = findViewById(R.id.etStudentId);
        etStudentName = findViewById(R.id.etStudentName);
        faceChecked = new ArrayList<>();

        // Get student information to edit
        Intent intent_info = this.getIntent();
        this.student = (Student) intent_info.getSerializableExtra("student");

        if (student != null)
        {
            this.mode = MODE_EDIT;
            // Display current student information
            this.etStudentID.setText(student.getStudentIdNumber());
            this.etStudentName.setText(student.getStudentName());

            courseServerId = intent_info.getStringExtra("courseServerId");
            courseId = intent_info.getIntExtra("courseId", 0);

            student_serverId = student.getStudentServerId();
            Log.i("EXECUTE", "Edit Student: " + student_serverId + " in Group: " + courseServerId);

            List<Face> faceList ;
            db_face = new MyDatabaseHelperFace(this);
            faceList = db_face.getFaceWithStudent(student_serverId);
            Log.i("EXECUTE", "Face list SIZE: " + faceList.size());

            gvStudentFace = findViewById(R.id.gvStudentFace);
            displayGridView(student_serverId, 0);
        }
        else
        {
            Bundle bundle = getIntent().getBundleExtra("CourseId");
            courseId = bundle.getInt("courseId");
            courseServerId = bundle.getString("courseServerId");

            this.mode = MODE_ADD;
            gvStudentFace = findViewById(R.id.gvStudentFace);
            displayGridView(student_serverId, 0);
            studentNumberId = etStudentID.getText().toString();
            studentName = etStudentName.getText().toString();
            newStudent = true; // flag
        }
    }



    @Override
    protected void onResume()
    {
        super.onResume();
        Log.i("EXECUTE", "Student on resume Id: " + student_serverId);
        displayGridView(student_serverId, 1);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putString("StudentServerId", student_serverId);
        outState.putString("CourseServerId", courseServerId);
        outState.putString("StudentId", etStudentID.getText().toString());
        outState.putString("StudentName", etStudentName.getText().toString());
    }


    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        student_serverId = savedInstanceState.getString("StudentServerId");
        Log.i("EXECUTE", "RESTORE STUDENT SV ID: " + student_serverId);
        courseServerId = savedInstanceState.getString("CourseServerId");
        etStudentID.setText(savedInstanceState.getString("StudentId"));
        etStudentName.setText(savedInstanceState.getString("StudentName"));
    }


    private void displayGridView(String student_serverId, int request)
    {
        if (student_serverId != null && (request == 0 || newStudent == true))
        {
            List<Face> listFace =  db_face.getFaceWithStudent(student_serverId);
            this.faceList.addAll(listFace);

            gridViewAdapter = new ArrayAdapter<Face>(this,
                    android.R.layout.simple_list_item_activated_1, android.R.id.text1, faceList)
            {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
                {
                    if (convertView == null) {
                        LayoutInflater layoutInflater
                                = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        convertView = layoutInflater.inflate(
                                R.layout.item_face_with_checkbox, parent, false);
                    }
                    convertView.setId(position);

                    Uri uri = Uri.parse(faceList.get(position).getStudentFaceUri());
                    ((ImageView)convertView.findViewById(R.id.image_face)).setImageURI(uri);

                    return convertView;
                }
            };

            if (newStudent == true)
            {
                newStudent = false;
            }

            // Register Adapter cho ListView.
            this.gvStudentFace.setAdapter(this.gridViewAdapter);
            registerForContextMenu(this.gvStudentFace);
        }
        else if (student_serverId != null && request != 0)
        {
            this.faceList.clear();
            MyDatabaseHelperFace db = new MyDatabaseHelperFace(this);
            List<Face> list = db.getFaceWithStudent(student_serverId);
            this.faceList.addAll(list);
            this.gridViewAdapter.notifyDataSetChanged();
            registerForContextMenu(this.gvStudentFace);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo)
    {

        super.onCreateContextMenu(menu, view, menuInfo);

        // groupId, itemId, order, title
        menu.add(0, MENU_ITEM_DELETE, 0, "Delete Student");

    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo
                info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        // Get selected student to apply action
        final Face selectedFace = (Face) this.gvStudentFace.getItemAtPosition(info.position);

        if(item.getItemId() == MENU_ITEM_DELETE)
        {
            // Confirmation dialog before delete
            new AlertDialog.Builder(this)
                    .setMessage("Are you sure you want to delete this face?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id) {
                            deleteFace(selectedFace);
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

    // Delete a student from database
    private void deleteFace(Face face)
    {
        MyDatabaseHelperFace db = new MyDatabaseHelperFace(this);
        db.deleteFace(face);
        this.faceList.remove(face);
        // Refresh ListView.
        this.gridViewAdapter.notifyDataSetChanged();
        new DeleteFaceTask(courseServerId, student_serverId).execute(face.getStudentFaceServerId());
    }

    // Button Add Face click event
    public void addFace(View view)
    {
        if (student_serverId == null)
        {
            new AddStudentTask(true).execute(courseServerId);
        }
        else
        {
            addFace();
        }
    }

    private void addFace()
    {
        Intent intent = new Intent(this, SelectImageActivity.class);
        Log.i("EXECUTE", "START ADDING FACE");
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    @Override // Send intent to Add face activity
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case REQUEST_SELECT_IMAGE:
                if (resultCode == RESULT_OK)
                {
                    Log.i("EXECUTE", "GOT PHOTO FROM CAMERA");
                    Uri uriImagePicked = data.getData();
                    Intent intent = new Intent(this, AddFaceActivity.class);
                    intent.putExtra("StudentServerId", student_serverId);

                    intent.putExtra("CourseServerId", courseServerId);
                    intent.putExtra("ImageUriStr", uriImagePicked.toString());
                    startActivity(intent);
                }
                break;
            default:
                break;
        }
    }

    // Click event for Done button
    public void btnDoneClick(View view)
    {
        MyDatabaseHelperStudent db = new MyDatabaseHelperStudent(this);
        // get input
        String number_id = this.etStudentID.getText().toString();
        String name = this.etStudentName.getText().toString();

        if(number_id.equals("") || name.equals("")) {
            Toast.makeText(getApplicationContext(),
                    "Please enter student ID & student name.", Toast.LENGTH_LONG).show();
            return;
        }

        if(mode == MODE_ADD )
        {
           if (student_serverId == null)
                new AddStudentTask(false).execute(courseServerId);
            Log.i ("EXECUTE", " Add Student Server id: " + student_serverId + " to group: " + courseServerId);
            this.student = new Student(number_id, courseServerId, name, student_serverId);
            db.addStudent(student);
        }
        else
        {
            this.student.setStudentIdNumber(number_id);
            this.student.setStudentName(name);
            db.updateStudent(student);
        }

        new TrainCourseTask().execute(courseServerId);

        this.needRefresh = true;

        // Back to current activity
        onBackPressed();
    }

    // Finish activity
    @Override
    public void finish() {

        // Prepare Intent data
        Intent data = new Intent();
        // Request the List Course refresh
        data.putExtra("needRefresh", needRefresh);

        // Activity complete
        this.setResult(Activity.RESULT_OK, data);
        super.finish();
    }

}
