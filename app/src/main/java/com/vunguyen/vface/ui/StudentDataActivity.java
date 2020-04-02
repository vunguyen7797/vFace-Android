/*
 * StudentDataActivity.java
 */
package com.vunguyen.vface.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;
import com.squareup.picasso.Picasso;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.bean.Face;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.ImageEditor;
import com.vunguyen.vface.helper.LocaleHelper;
import com.vunguyen.vface.helper.MyDatabaseHelperFace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * This class implements functions for a student profile activity
 */
public class StudentDataActivity extends AppCompatActivity
{
    String student_serverId = null;
    private String account;
    private String courseServerId;
    String studentNumberId;
    String studentName;
    Student student;
    int courseId;

    private TextInputEditText etStudentID;
    private TextInputEditText etStudentName;
    GridView gvStudentFace;
    TextInputLayout outlinedTextStudentId;

    // request code
    private static final int MODE_ADD = 1;
    private static final int MODE_EDIT = 2;
    private static final int REQUEST_SELECT_IMAGE = 0;
    private static final int MENU_ITEM_DELETE = 111;
    private boolean needRefresh = true;
    private int mode;
    boolean newStudent;

    MyDatabaseHelperFace db_face;
    List<Face> faceList = new ArrayList<>();
    private ArrayAdapter<Face> faceArrayAdapter;

    DatabaseReference mDatabase_Face;
    DatabaseReference mDatabase_student;
    FirebaseStorage mStorage = FirebaseStorage.getInstance();

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_data);

        outlinedTextStudentId = findViewById(R.id.outlinedTextStudentId);
        etStudentID = findViewById(R.id.etStudentId);
        etStudentName = findViewById(R.id.etStudentName);
        gvStudentFace = findViewById(R.id.gvStudentFace);

        // initialize face database
        db_face = new MyDatabaseHelperFace(this);


        // Get student information to edit
        Intent intent_info = this.getIntent();
        this.student = (Student) intent_info.getSerializableExtra("student");

        if (student != null)
        {
            this.mode = MODE_EDIT;
            // Display current student output information
            this.etStudentID.setText(student.getStudentIdNumber());
            this.etStudentName.setText(student.getStudentName());

            // get current student information
            courseServerId = intent_info.getStringExtra("courseServerId");
            courseId = intent_info.getIntExtra("courseId", 0);
            account = intent_info.getStringExtra("account");

            student_serverId = student.getStudentServerId();
            Log.i("EXECUTE", "Edit Student: " + student_serverId + " in Group: " + courseServerId);
        }
        else
        {
            Bundle bundle = getIntent().getBundleExtra("CourseId");
            courseId = bundle.getInt("courseId");
            account = bundle.getString("account");
            courseServerId = bundle.getString("courseServerId");
            this.mode = MODE_ADD;
            studentNumberId = etStudentID.getText().toString();
            studentName = etStudentName.getText().toString();
            newStudent = true; // flag
        }

        //displayGridView(student_serverId, 0);
        mDatabase_Face = FirebaseDatabase.getInstance().getReference().child(account).child("face");
        mDatabase_student = FirebaseDatabase.getInstance().getReference().child(account).child("student");
        getFaceList(student_serverId);
    }

    private void getFaceList(String student_serverId)
    {
        mDatabase_Face.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Face> faceList = new ArrayList<>();
                for(DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Log.i("EXECUTE","DSP: " + dsp.getValue(Student.class));
                    if (Objects.requireNonNull(dsp.getValue(Face.class)).getStudentServerId().equalsIgnoreCase(student_serverId))
                        faceList.add(dsp.getValue(Face.class));
                }

                displayGridView(faceList, student_serverId, 0);
                Log.i("EXECUTE","DSP Size: " + faceList.size());
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // Click event for Done button
    public void btnDoneClick(View view)
    {

        if (student_serverId == null)
        {
            // add student to server when profile is saved
            new AddStudentTask(false).execute(courseServerId);
        }
        else
        {
            saveData(); // if student is already available on server, save changed data.
        }
    }

    // save data for student profile
    private void saveData()
    {
       // MyDatabaseHelperStudent db = new MyDatabaseHelperStudent(this);
        // get input from textbox
        String number_id = Objects.requireNonNull(this.etStudentID.getText()).toString();
        String name = Objects.requireNonNull(this.etStudentName.getText()).toString();

        if(number_id.equals("") || name.equals(""))
        {
            Toast.makeText(getApplicationContext(),
                    "Please enter student ID & student name.", Toast.LENGTH_LONG).show();
            return;
        }

        if(mode == MODE_ADD )
        {
            Log.i ("EXECUTE", " Add Student Server id: " + student_serverId + " to group: " + courseServerId);
            this.student = new Student(number_id, courseServerId, name, student_serverId, "NO");
            //db.addStudent(student);
            mDatabase_student.child(name.toUpperCase() + "-" + student_serverId).setValue(student);
        }
        else
        {
            if (!student.getStudentName().equalsIgnoreCase(name))
            {
                mDatabase_student.child(student.getStudentName().toUpperCase()+"-"+student_serverId).removeValue();
            }
            this.student.setStudentIdNumber(number_id);
            this.student.setStudentName(name);
            mDatabase_student.child(name.toUpperCase() + "-" + student_serverId).setValue(student);
        }

        // Train the course after a student is added or modified.
        new TrainCourseTask().execute(courseServerId);
        this.needRefresh = true;
        // Back to previous activity
        onBackPressed();
    }

    // Delete a student from database
    private void deleteFace(Face face)
    {
        db_face.deleteFace(face);
        this.faceList.remove(face);
        mDatabase_Face.child(face.getStudentFaceServerId()).removeValue();
        StorageReference photoRef = mStorage.getReferenceFromUrl(face.getStudentFaceUri());
        photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i("EXECUTE", "Deleted face from Firebase Storage");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("EXECUTE", "Cannot delete face from Firebase Storage");
            }
        });
        // Refresh ListView.
        this.faceArrayAdapter.notifyDataSetChanged();
        // Delete face from server
        new DeleteFaceTask(courseServerId, student_serverId).execute(face.getStudentFaceServerId());

    }

    // Button Add Face click event
    public void btnAddFace(View view)
    {
        if (etStudentName.getText().toString().equals("") && etStudentID.getText().toString().equals(""))
        {
            Toast.makeText(getApplicationContext(), "Enter student ID & student name\n to add face.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        else if (!etStudentName.getText().toString().equals("") && !etStudentID.getText().toString().equals(""))
        {
            if (student_serverId == null)
            {
                // generate a new student on server
                new AddStudentTask(true).execute(courseServerId);
            }
            else
            {
                btnAddFace();  // if student is available on sever, just save data
            }
        }
    }

    // go to select image screen activity
    private void btnAddFace()
    {
        Intent intent = new Intent(this, SelectImageActivity.class);
        Log.i("EXECUTE", "START ADDING FACE");
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    @Override // response after photo is selected or taken from camera
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    Log.i("EXECUTE", "PHOTO IS AVAILABLE");
                    Uri uriImagePicked = data.getData();
                    Bitmap bitmapImage = null;
                    try {
                        bitmapImage = ImageEditor.handlePhotoAndRotationBitmap(getApplicationContext(), uriImagePicked);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // add face to student
                    AddFaceToStudent addFaceToStudent = new AddFaceToStudent(bitmapImage, student_serverId,
                            courseServerId, mDatabase_Face, getApplicationContext(), StudentDataActivity.this, account);
                    addFaceToStudent.addFaceToPerson();
                }
                break;
            default:
                break;
        }
    }

    // click event for back button on navigation bar
    @Override
    public void onBackPressed()
    {

        if (!this.etStudentID.getText().toString().equals("") || !this.etStudentName.getText().toString().equals(""))
        {
            Toast.makeText(this, getResources().getString(R.string.please_save_data_toast), Toast.LENGTH_SHORT).show();
        }
        else if (this.etStudentID.getText().toString().equals("")
                && this.etStudentName.getText().toString().equals(""))
        {
            //Intent intent = new Intent(StudentDataActivity.this, StudentManagerActivity.class);
           // intent.putExtra("ACCOUNT", account);
            //startActivity(intent);
            finish();
        }
    }

    // Implement actions for menu item
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
            new MaterialAlertDialogBuilder(this)
                    .setTitle("VFACE - STUDENT PROFILE")
                    .setMessage("Are you sure you want to delete this face?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", (dialog, id) -> deleteFace(selectedFace))
                    .setNegativeButton("No", null)
                    .show();
        }
        else
        {
            return false;
        }
        return true;
    }

    public void btnBackClick(View view)
    {
        // Back to previous activity
        onBackPressed();
    }

    /**
     * This class is a background task to add a new student to the server
     * into a large person group (Course).
     */
    class AddStudentTask extends AsyncTask<String, String, String>
    {
        // Indicate the next step is to add face in this student, or finish editing this student.
        boolean addFace;

        AddStudentTask (boolean addFace) {
            this.addFace = addFace;
        }

        @Override
        protected String doInBackground(String... params)
        {
            // Connect to server
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                Log.i("EXECUTE","Request: Creating Student in the course" + params[0]);

                // Request of creating a new person (student) in large person group
                CreatePersonResult createPersonResult = faceServiceClient.createPersonInLargePersonGroup(
                        params[0],
                        getString(R.string.user_provided_person_name),
                        getString(R.string.user_provided_description_data));
                Log.i("EXECUTE","Create Student Done");
                return createPersonResult.personId.toString();
            }
            catch (Exception e)
            {
                Log.i("EXECUTE", "Error: " + e.getMessage());
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
                if (addFace)
                {
                    btnAddFace();
                }
                else
                {
                    saveData();
                }
            }
        }
    }

    /**
     * This class is a background task to delete a face from a person (Student)
     * on server
     */
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
        protected String doInBackground(String... params)
        {
            // Connect to server
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                Log.i("EXECUTE","Request: Deleting face " + params[0]);

                UUID faceId = UUID.fromString(params[0]);
                faceServiceClient.deletePersonFaceInLargePersonGroup(courseServerId, studentServerId, faceId);
                return params[0];
            }
            catch (Exception e)
            {
                Log.i("EXECUTE","Error Delete face: " + (e.getMessage()));
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (result != null)
            {
                Log.i("EXECUTE","Face " + result + " successfully deleted");
                Toast.makeText(getApplicationContext(),"Face Deleted.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * This class is a background task to train the course after data is updated on server
     */
    class TrainCourseTask extends AsyncTask<String, String, String>
    {
        @Override
        protected String doInBackground(String... params)
        {
            Log.i("EXECUTE","Request: Training group " + params[0]);
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                faceServiceClient.trainLargePersonGroup(params[0]);
                return params[0];
            }
            catch (Exception e)
            {
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

    // on resume activity, display the faces available for this student
    @Override
    protected void onResume()
    {
        super.onResume();
        Log.i("EXECUTE", "Student on resume Id: " + student_serverId);
        getFaceList(student_serverId);
        //displayGridView(student_serverId, 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putString("StudentServerId", student_serverId);
        outState.putString("CourseServerId", courseServerId);
        outState.putString("StudentId", etStudentID.getText().toString());
        outState.putString("StudentName", etStudentName.getText().toString());
        outState.putString("account", account);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        student_serverId = savedInstanceState.getString("StudentServerId");
        courseServerId = savedInstanceState.getString("CourseServerId");
        etStudentID.setText(savedInstanceState.getString("StudentId"));
        etStudentName.setText(savedInstanceState.getString("StudentName"));
        account = savedInstanceState.getString("account");
    }

    // display faces on grid view
    private void displayGridView(List<Face> faceList, String student_serverId, int request)
    {
        if (student_serverId != null && (request == 0 || newStudent))
        {
            faceArrayAdapter = new ArrayAdapter<Face>(this,
                    android.R.layout.simple_list_item_activated_1, android.R.id.text1, faceList)
            {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
                {
                    if (convertView == null)
                    {
                        LayoutInflater layoutInflater
                                = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        convertView = layoutInflater.inflate(
                                R.layout.item_face_with_checkbox, parent, false);
                    }
                    convertView.setId(position);

                    Uri uri = Uri.parse(faceList.get(position).getStudentFaceUri());

                    Picasso.get().load(uri).into((ImageView)convertView.findViewById(R.id.image_face));

                    return convertView;
                }
            };

            if (newStudent)
            {
                newStudent = false;
            }

            // Register Adapter cho ListView.
            this.gvStudentFace.setAdapter(this.faceArrayAdapter);
            registerForContextMenu(this.gvStudentFace);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.add(0, MENU_ITEM_DELETE, 0, getResources().getString(R.string.menu_delete_face));
    }

    // Finish activity
    @Override
    public void finish()
    {
        // Prepare Intent data
        Intent data = new Intent(StudentDataActivity.this, StudentManagerActivity.class);
        // Request the grid view refresh
        data.putExtra("needRefresh", needRefresh);
        data.putExtra("ACCOUNT", account);
        // Activity complete
        this.setResult(Activity.RESULT_OK, data);
        super.finish();
    }

}
