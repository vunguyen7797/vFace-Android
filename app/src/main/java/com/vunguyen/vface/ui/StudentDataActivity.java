/*
 * StudentDataActivity.java
 */
package com.vunguyen.vface.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
import com.vunguyen.vface.bean.Face;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.ImageEditor;
import com.vunguyen.vface.helper.LocaleHelper;
import com.vunguyen.vface.helper.ProgressDialogCustom;
import com.vunguyen.vface.helper.asyncTasks.DeleteFaceTask;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * This class implements functions for a student profile activity
 */
public class StudentDataActivity extends AppCompatActivity
{
    String studentServerIdMain = null;
    String studentServerIdImport = null;
    String account;
    String courseServerId;
    String studentNumberId;
    String studentName;
    Student student;
    int courseId;
    public static int numberOfFaces = 0;

    TextInputEditText etStudentID;
    TextInputEditText etStudentName;
    GridView gvStudentFace;
    TextInputLayout outlinedTextStudentId;
    ProgressDialog progressDialog;
    Button btnAddFace;
    ProgressDialogCustom progressDialogCustom;
    ImageView imageView;

    // request code
    private static final int MODE_ADD = 1;
    private static final int MODE_EDIT = 2;
    private static final int MODE_IMPORT = 3;
    private static final int MODE_IMPORT_STORAGE = 4;
    private static final int REQUEST_SELECT_IMAGE = 0;
    private static final int MENU_ITEM_DELETE = 111;
    private boolean needRefresh = true;
    private int mode;
    boolean newStudent;

    List<Face> faceList = new ArrayList<>();
    private ArrayAdapter<Face> faceArrayAdapter;

    DatabaseReference mDatabase_Face;
    DatabaseReference mDatabase_student;
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
        setContentView(R.layout.activity_student_data);

        initView();
        initData();
        initAction();
    }

    private void initAction()
    {
        btnAddFace.setOnClickListener(v ->
        {
            if (numberOfFaces >= 5) // Each student is allowed to add maximum 5 faces
            {
                btnAddFace.setEnabled(false);
                Toast.makeText(getApplicationContext(), "Reach maximum number of faces",
                        Toast.LENGTH_SHORT).show();
            }
            else
            {
                // Request user to enter the input data before adding face
                if (Objects.requireNonNull(etStudentName.getText()).toString().equals("")
                        && Objects.requireNonNull(etStudentID.getText()).toString().equals(""))
                {
                    Toast.makeText(getApplicationContext(),
                            "Enter student ID & student name\n to add face.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                else if (!etStudentName.getText().toString().equals("")
                        && !Objects.requireNonNull(etStudentID.getText()).toString().equals(""))
                {
                    if (studentServerIdMain == null)
                    {
                        // Create a new student to server before adding face
                        createStudent(0);
                    }
                    else
                    {
                        // if student is available on sever, allow to add face
                        btnAddFace();
                    }
                }
            }
        });
    }

    private void initData()
    {
        // Get student information to edit
        Intent intent_info = this.getIntent();
        this.student = (Student) intent_info.getSerializableExtra("student");
        // Initialize base data for each mode
        if (student != null)
            initEditMode(intent_info);
        else
            initAddMode();

        // Initialize paths for databases
        mDatabase_Face = FirebaseDatabase.getInstance().getReference().child(account).child("face");
        mDatabase_student = FirebaseDatabase.getInstance().getReference().child(account).child("student");
        mDatabase_saved = FirebaseDatabase.getInstance().getReference().child(account).child("account_storage");

        // Display the faces available - Edit mode only

        if (student != null && student.getNumberOfFaces() == 0)
        {
            Log.i("EXECUTE", "Call getFaceListFromStorage from InitData");
            getFaceListFromStorage(studentServerIdMain, 1);
        }
        else
        {
            Log.i("EXECUTE", "Call getFaceList from InitData");
            getFaceList(studentServerIdMain,0);
        }

    }

    private void initView()
    {
        outlinedTextStudentId = findViewById(R.id.outlinedTextStudentId);
        etStudentID = findViewById(R.id.etStudentId);
        etStudentName = findViewById(R.id.etStudentName);
        gvStudentFace = findViewById(R.id.gvStudentFace);
        btnAddFace = findViewById(R.id.btnAddFace);
        progressDialogCustom = new ProgressDialogCustom(this);
        imageView = findViewById(R.id.ivFaceWaiting);
    }

    /**
     *********************** Data processors and initializing methods ************************
     */
    private void initAddMode()
    {
        Bundle bundle = getIntent().getBundleExtra("CourseId");
        courseId = bundle.getInt("courseId");
        account = bundle.getString("account");
        courseServerId = bundle.getString("courseServerId");
        this.mode = MODE_ADD;
        studentNumberId = Objects.requireNonNull(etStudentID.getText()).toString();
        if (!studentNumberId.equalsIgnoreCase(""))
        {
            studentName = Objects.requireNonNull(etStudentName.getText()).toString();
        }
        else
        {
            etStudentName.setEnabled(false);
            etStudentID.requestFocus();
            etStudentID.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after)
                {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count)
                {
                    if (s.length() == 8)
                    {
                        etStudentName.setEnabled(true);
                        progressDialogCustom.startProgressDialog("Verifying ID...");
                        verifyStudent(etStudentID.getText().toString(),2);
                    }
                    else
                    {
                        etStudentName.setText("");
                        etStudentName.setEnabled(false);
                    }
                }

                @Override
                public void afterTextChanged(Editable s)
                {
                }
            });
        }
        newStudent = true; // flag
    }

    private void initEditMode(Intent intent_info)
    {
        this.mode = MODE_EDIT;
        // Display current student output information
        this.etStudentID.setText(student.getStudentIdNumber());
        this.etStudentName.setText(student.getStudentName());

        // get current student information
        courseServerId = intent_info.getStringExtra("courseServerId");
        courseId = intent_info.getIntExtra("courseId", 0);
        account = intent_info.getStringExtra("account");

        studentServerIdMain = student.getStudentServerId();
        studentServerIdImport = student.getStudentServerIdImport();
        numberOfFaces = student.getNumberOfFaces();
        studentNumberId = student.getStudentIdNumber();
        Log.i("EXECUTE", "Edit Student: " + studentServerIdMain + " in Group: " + courseServerId);
    }

    // Get face objects belong to a student
    private void getFaceList(String studentServerId, int request)
    {
        mDatabase_Face.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                List<Face> faceList = new ArrayList<>();
                String tmpStudentNumberId = Objects.requireNonNull(etStudentID.getText()).toString();
                String tmpStudentName = Objects.requireNonNull(etStudentName.getText()).toString();
                for(DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Face face = dsp.getValue(Face.class);
                    assert face != null;
                    if (Objects.requireNonNull(face.getStudentServerId().equalsIgnoreCase
                            (studentServerId)) && !Uri.EMPTY.equals(Uri.parse(Objects
                            .requireNonNull(face).getStudentFaceUri())))
                    {
                        // add face from dtb to list if student number ID matching input
                        faceList.add(dsp.getValue(Face.class));
                    }
                }

                /*
                for (int i = 0; i < faceList.size(); i++) {
                    Log.i("EXECUTE", "Face list size: " + faceList.size());
                    String uri = faceList.get(i).getStudentFaceUri();
                    for (int j = 0; j < faceList.size(); j++) {
                        if (faceList.get(j) != faceList.get(i) && faceList.get(j).getStudentFaceUri().equalsIgnoreCase(uri)) {
                            Face face = new Face("","", "", "", "");
                            faceList.set(j, face);
                        }
                    }
                }
                faceList.removeIf(face -> face.getCourseServerId().equalsIgnoreCase(""));
*/
                Log.i("EXECUTE", "Total faces before display: " + faceList.size());
                displayGridView(faceList, studentServerId, 0);

                if (request != 0)   // request != 0, display the faces on grid view and add to server
                {
                    Log.i("EXECUTE", "Imported face list size: " + faceList.size());
                    for (Face face : faceList) {
                        String uriImage = face.getStudentFaceUri();
                        numberOfFaces++;
                        // Add students to server directly without detection process
                        AddFaceToStudent addFaceToStudent = new AddFaceToStudent(tmpStudentNumberId,
                                tmpStudentName, uriImage, studentServerIdMain, courseServerId,
                                getApplicationContext(), StudentDataActivity.this, account,
                                numberOfFaces);
                        addFaceToStudent.addFaceToPersonDirect();
                    }
                }
                //mDatabase_Face.removeEventListener(this);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    // Click event for Done button
    public void btnDoneClick(View view)
    {
        if (studentServerIdMain == null)
        {
            Log.i("EXECUTE", "Verify students button save clicked....");
            verifyStudent(etStudentID.getText().toString(), 1);
        }
        else
        {
            saveData(); // if student is already available on server, save changed data.
        }
    }

    // save data for student profile
    public void saveData()
    {
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
            Log.i ("EXECUTE", " Add Student Server id: " + studentServerIdMain + " to group: " + courseServerId);
            this.student = new Student(number_id, courseServerId, name, studentServerIdMain, "NO");
            //verifyStudent(number_id, name, 1, this.student);
            mDatabase_student.child(name.toUpperCase() + "-" + studentServerIdMain).setValue(student);
            addStudentToStorage(number_id, name);
        }
        else if (mode == MODE_EDIT)
        {
            if (!student.getStudentName().equalsIgnoreCase(name))
            {
                mDatabase_student.child(student.getStudentName().toUpperCase()+"-"+ studentServerIdMain).removeValue();
            }
            this.student.setStudentIdNumber(number_id);
            this.student.setStudentName(name);
            this.student.setNumberOfFaces(numberOfFaces);
            Log.i("EXECUTE", "EDIT number of faces: " + this.student.getNumberOfFaces());
            mDatabase_student.child(name.toUpperCase() + "-" + studentServerIdMain).setValue(student);
        }
        updateDataComplete();
    }

    private void addStudentToStorage(String studentNumberId, String studentName)
    {
        mDatabase_saved.child("student").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                boolean inDatabase = false;
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Student temp = dsp.getValue(Student.class);
                    assert temp != null;
                    if (temp.getStudentIdNumber().equalsIgnoreCase(studentNumberId))
                        inDatabase = true;
                }
                if (!inDatabase)
                {
                    String path = studentName.toUpperCase() + "-" + studentServerIdMain;
                    mDatabase_saved.child("student").child(path).setValue(student);
                    mDatabase_saved.child("student").child(path).child("courseServerId").setValue("");
                }
                mDatabase_saved.child("student").removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
    private void updateDataComplete()
    {
        this.needRefresh = true;
        // Back to previous activity
        // Train the course after a student is added or modified.
        mDatabase_student.child(etStudentName.getText().toString().toUpperCase()+"-"+studentServerIdMain).child("numberOfFaces").setValue(numberOfFaces);
        new TrainCourseTask().execute(courseServerId);
        onBackPressed();
    }

    private void verifyStudent(String studentNumberId, int request)
    {
        mDatabase_student.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                boolean inCourse = false;
                String studentNameDtb = null;
                boolean showDialog = false;
                boolean verifyFromStorage = false;
                Student student = null;
                Log.i("EXECUTE", "Start verifying student...");

                if (dataSnapshot.getChildrenCount() == 0)
                {
                    Log.i("EXECUTE", "Request verify from Storage");
                    verifyFromStorage = true;
                    verifyFromStorage(studentNumberId);
                }
                else
                {
                    for (DataSnapshot dsp : dataSnapshot.getChildren())
                    {
                        Student temp = dsp.getValue(Student.class);
                        assert temp != null;
                        if (temp.getStudentIdNumber().equalsIgnoreCase(studentNumberId)
                                && temp.getCourseServerId().equalsIgnoreCase(courseServerId)) {
                            Log.i("EXECUTE", "Request verify student in course");
                            inCourse = true;
                            studentNameDtb = temp.getStudentName();
                        } else if ((temp.getCourseServerId().equalsIgnoreCase(courseServerId)
                                && !temp.getStudentIdNumber().equalsIgnoreCase(studentNumberId)) ||
                                (!temp.getCourseServerId().equalsIgnoreCase(courseServerId) &&
                                        temp.getStudentIdNumber().equalsIgnoreCase(studentNumberId))
                                        && mode == MODE_ADD)
                        {
                            Log.i("EXECUTE", "Request verify student storage 2");
                            verifyFromStorage = true;
                        } else if (!temp.getStudentIdNumber().equalsIgnoreCase(studentNumberId)
                                && !temp.getCourseServerId().equalsIgnoreCase(courseServerId)) {
                            Log.i("EXECUTE", "Request verify for new student");
                            verifyFromStorage = false;
                        }
                    }
                }

                if (verifyFromStorage && dataSnapshot.getChildrenCount() > 0)
                {
                    verifyFromStorage(studentNumberId);
                }
                else if (inCourse && mode == MODE_ADD) // Student already existed in database
                {
                    Toast.makeText(getApplicationContext(), "This student already existed"
                            , Toast.LENGTH_SHORT).show();
                    etStudentName.setText(studentNameDtb);
                    progressDialogCustom.dismissDialog();
                    Log.i("EXECUTE", "Student is in class");
                }
                else if (inCourse)
                {
                    // information is ready, only saving
                    Log.i("EXECUTE", "Update data only");
                    progressDialogCustom.dismissDialog();
                    updateDataComplete();
                }
                else if (!verifyFromStorage && mode == MODE_ADD)
                {
                    Log.i("EXECUTE", "Verifying in storage again new student...");
                   verifyFromStorage(studentNumberId);
                }

                mDatabase_student.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void createStudent(int request)
    {
        if (request == 1)       // Button save clicked
            new AddStudentTask(false).execute(courseServerId);
        else if (request == 0)  // Button add face clicked
            new AddStudentTask(true).execute(courseServerId);
    }
    private void verifyFromStorage(String studentNumberId)
    {
        Log.i("EXECUTE", "Start verifying student from storage");
        mDatabase_saved.child("student").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                boolean inStorage = false;
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Student temp = dsp.getValue(Student.class);
                    assert temp != null;
                    if (temp.getStudentIdNumber().equalsIgnoreCase(studentNumberId))
                    {
                        inStorage = true;
                        Log.i("EXECUTE", "Start dialog to request import from storage");
                        final Handler handler = new Handler();
                        handler.postDelayed(() ->
                        {
                            progressDialogCustom.dismissDialog();
                            new MaterialAlertDialogBuilder(StudentDataActivity.this)
                                    .setTitle("VFACE - STUDENT DATA")
                                    .setMessage("Found this student in database.\nDo you want to import available data?")
                                    .setCancelable(false)
                                    .setPositiveButton("Yes", ((dialog, which) ->
                                            importData(temp)) )
                                    .setNegativeButton("No", null)
                                    .show();
                        }, 2000);
                    }
                }
                if (!inStorage)
                {
                    final Handler handler = new Handler();
                    handler.postDelayed(() ->
                    {
                        progressDialogCustom.dismissDialog();
                        Toast.makeText(getApplicationContext(), "Verifying ID completed", Toast.LENGTH_SHORT).show();
                        btnAddFace.setEnabled(true);
                    }, 2000);

                }
                mDatabase_saved.child("student").removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void importData(Student finalStudent)
    {
        progressDialogCustom.startProgressDialog("Importing...");
        String studentName = finalStudent.getStudentName();
        String studentFlag = finalStudent.getStudentIdentifyFlag();
        String studentNumberId = finalStudent.getStudentIdNumber();
        Log.i("EXECUTE", "Import data - Student server ID: " + finalStudent.getStudentServerId());
        etStudentName.setText(studentName);

        new AddStudentTask(2, finalStudent.getStudentServerId()).execute(courseServerId);
        // wait until all async tasks completed to update UI view
       // progressDialogCustom.dismissDialog();

        new CountDownTimer(2000, 1000)
        {
            @Override
            public void onTick(long millisUntilFinished)
            {
                Log.i("EXECUTE", "Please wait for task complete importing...");
            }
            @Override
            public void onFinish()
            {
                Log.i("EXECUTE", "After created: " + studentServerIdMain);
                Student newStudent = new Student(finalStudent.getStudentServerId(), studentNumberId, courseServerId, studentName
                        , studentServerIdMain, studentFlag, numberOfFaces);
                 mDatabase_student.child(studentName.toUpperCase() + "-" + studentServerIdMain).setValue(newStudent);
                Log.i("EXECUTE", "Call getFaceList from ImportData");
                getFaceList(studentServerIdMain, 0);
                progressDialogCustom.dismissDialog();
            }
        }.start();
    }

    private void getFaceListFromStorage(String studentServerId, int request)
    {
        mDatabase_saved.child("face").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                List<Face> faceList = new ArrayList<>();
                for(DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    if (Objects.requireNonNull(dsp.getValue(Face.class)).getStudentNumberId()
                            .equalsIgnoreCase(Objects.requireNonNull(etStudentID.getText()).toString()))
                    {
                        faceList.add(dsp.getValue(Face.class));
                    }
                }

                if (request == 0)
                    displayGridView(faceList, studentServerId, 0);
                else
                {
                    displayGridView(faceList, studentServerId, 0);
                    Log.i("EXECUTE", "IMPORT FACE FROM STORAGE " + faceList.size());
                    numberOfFaces = faceList.size();
                    for (Face face : faceList)
                    {
                        Uri uriImage = Uri.parse(face.getStudentFaceUri());
                        AddFaceToStudent addFaceToStudent = new AddFaceToStudent(etStudentID.getText().toString(),etStudentName.getText().toString(), uriImage.toString(), studentServerIdMain,
                                courseServerId, getApplicationContext(), StudentDataActivity.this, account, numberOfFaces);
                        addFaceToStudent.addFaceToPersonDirect();
                        // wait until all async tasks completed to update UI view
                        new CountDownTimer(1000, 1000)
                        {
                            @Override
                            public void onTick(long millisUntilFinished)
                            {
                                Log.i("EXECUTE", "Please wait for async task complete...");
                            }
                            @Override
                            public void onFinish()
                            {
                                Log.i("EXECUTE", "Finish waiting...");
                            }
                        }.start();
                    }

                }

                mDatabase_saved.child("face").removeEventListener(this);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }


    // Delete a student from database
    private void deleteFace(Face face)
    {
        this.faceList.remove(face);
        mDatabase_Face.child(face.getStudentFaceServerId()).removeValue();

        if (numberOfFaces > 0)
            numberOfFaces--;
        if (numberOfFaces < 5)
            btnAddFace.setEnabled(true);
        checkStudentsSameFace(face.getStudentFaceUri(), face);
        mDatabase_student.child(Objects.requireNonNull(etStudentName.getText()).toString().toUpperCase()+"-"
                + studentServerIdMain).child("numberOfFaces").setValue(numberOfFaces);
        // Delete face from server
        new DeleteFaceTask(courseServerId, studentServerIdMain,
                StudentDataActivity.this).execute(face.getStudentFaceServerId());

        this.faceArrayAdapter.notifyDataSetChanged();
    }

    private void deleteFaceFromStorage(String uri)
    {
        mDatabase_saved.child("face").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Face face = dsp.getValue(Face.class);
                    if (face.getStudentFaceUri().equalsIgnoreCase(uri))
                    {
                        Log.i("EXECUTE", "Removed face from storage");
                        mDatabase_saved.child("face").child(face.getStudentFaceServerId()).removeValue();
                    }
                }
                mDatabase_saved.child("face").removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkStudentsSameFace(String uri, Face face)
    {
        mDatabase_Face.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                for(DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Face temp = dsp.getValue(Face.class);
                    if (temp.getStudentFaceUri().equalsIgnoreCase(uri))
                    {
                        mDatabase_Face.child(temp.getStudentFaceServerId()).removeValue();
                        mDatabase_student.child(etStudentName.getText().toString().toUpperCase()+"-"+temp.getStudentServerId()).child("numberOfFaces").setValue(numberOfFaces);
                        //TODO: Delete each face from server after delete in database, add course server id for face objects
                        new DeleteFaceTask(temp.getCourseServerId(), temp.getStudentServerId(), StudentDataActivity.this).execute(temp.getStudentFaceServerId());
                        new CountDownTimer(3000, 1000)
                        {

                            @Override
                            public void onTick(long millisUntilFinished) {
                                Log.i("EXECUTE", "Waiting deleting face...");
                            }

                            @Override
                            public void onFinish() {
                                Log.i("EXECUTE", "Finish delete face...");

                            }
                        };
                    }
                }
                deleteFaceFromStorage(face.getStudentFaceUri());
                StorageReference photoRef = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(Objects.requireNonNull(face.getStudentFaceUri()));
                        photoRef.delete().addOnSuccessListener(aVoid -> Log.i("EXECUTE"
                                , "Deleted face from Firebase Storage")).addOnFailureListener(
                                e -> Log.i("EXECUTE", "Cannot delete face from Firebase Storage"));

                mDatabase_Face.removeEventListener(this);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // go to select image screen activity
    public void btnAddFace()
    {
        Intent intent = new Intent(this, SelectImageActivity.class);
        Log.i("EXECUTE", "START ADDING FACE");
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    @Override // response after photo is selected or taken from camera
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_IMAGE) {
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
                numberOfFaces++;
                AddFaceToStudent addFaceToStudent = new AddFaceToStudent(etStudentID.getText().toString(), etStudentName.getText().toString(), uriImagePicked.toString(), bitmapImage, studentServerIdMain,
                        courseServerId, getApplicationContext(), StudentDataActivity.this, account, numberOfFaces);
                addFaceToStudent.addFaceToPersonAfterDetect();
            }
        }
    }

    // click event for back button on navigation bar
    @Override
    public void onBackPressed()
    {

        if (!this.etStudentID.getText().toString().equals("") || !this.etStudentName.getText().toString().equals(""))
        {
            //Toast.makeText(this, getResources().getString(R.string.please_save_data_toast), Toast.LENGTH_SHORT).show();
        }
        else if (this.etStudentID.getText().toString().equals("")
                && this.etStudentName.getText().toString().equals(""))
        {
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
        int stayActivity = 0;
        String studentServerIdImport;

        AddStudentTask (boolean addFace) {
            this.addFace = addFace;
        }
        AddStudentTask (int stayActivity, String studentServerIdImport)
        {
            this.stayActivity = stayActivity;
            this.studentServerIdImport = studentServerIdImport;
        }

        @Override
        protected String doInBackground(String... params)
        {
            // Connect to server
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                Log.i("EXECUTE","Request: Creating Student in the course" + params[0]);
                publishProgress("Creating student...");
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
                publishProgress(e.getMessage());
                Log.i("EXECUTE", "Error: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            //startProgressDialog();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //duringTaskProgressDialog(values[0]);
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (result != null)
            {
                Log.i("EXECUTE","Response: Success. Student " + result + " created.");
                studentServerIdMain = result;
                if (stayActivity == 0)
                {
                    if (addFace)
                    {
                        btnAddFace();
                    }
                    else
                    {
                        saveData();
                    }
                }
                else if (stayActivity == 1)
                {
                    Log.i("EXECUTE","IMPORT MODE: " + studentServerIdImport);
                    mode = MODE_IMPORT;
                    Log.i("EXECUTE", "Call getFaceList Mode 1 -  Add Student");
                    getFaceList(studentServerIdImport, 1);
                }
                else if (stayActivity == 2)
                {
                    Log.i("EXECUTE","IMPORT STORAGE MODE: " + studentServerIdImport);
                    mode = MODE_IMPORT_STORAGE;
                    getFaceListFromStorage(studentServerIdImport, 1);
                }
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
               // publishProgress("Training course...");
                faceServiceClient.trainLargePersonGroup(params[0]);
                return params[0];
            }
            catch (Exception e)
            {
                //publishProgress(e.getMessage());
                Log.i("EXECUTE","Error training group: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            //startProgressDialog();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //duringTaskProgressDialog(values[0]);
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (result != null)
            {
                //progressDialogCustom.dismissDialog();
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
        if (student != null && numberOfFaces == 0)
        {
            Log.i("EXECUTE", "Call getFaceListFromStorage from Resume");
            getFaceListFromStorage(studentServerIdMain, 0);
        }
        else
        {
            Log.i("EXECUTE", "Call getFaceList from Resume");
            getFaceList(studentServerIdMain,0);
        }
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putString("StudentServerId", studentServerIdMain);
        outState.putString("CourseServerId", courseServerId);
        outState.putString("StudentId", etStudentID.getText().toString());
        outState.putString("StudentName", etStudentName.getText().toString());
        outState.putString("account", account);
        outState.putString("StudentServerIdImport", studentServerIdImport);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        studentServerIdMain = savedInstanceState.getString("StudentServerId");
        courseServerId = savedInstanceState.getString("CourseServerId");
        etStudentID.setText(savedInstanceState.getString("StudentId"));
        etStudentName.setText(savedInstanceState.getString("StudentName"));
        account = savedInstanceState.getString("account");
        studentServerIdImport = savedInstanceState.getString("StudentServerIdImport");
    }

    // display faces on grid view
    private void displayGridView(List<Face> faceList, String student_serverId, int request)
    {
        if (faceList.size() > 0 && student_serverId != null && (request == 0 || newStudent))
        {
            gvStudentFace.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
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
        else if (faceList.size() == 0)
        {
            Log.i("EXECUTE", "All faces delete");

            imageView.setVisibility(View.VISIBLE);
            gvStudentFace.setVisibility(View.INVISIBLE);
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

    // display the progress dialog when a task is processing
    private void startProgressDialog()
    {
        //progressDialogCustom.startProgressDialog("Training course...");
        //progressDialog.show();
    }

    private void duringTaskProgressDialog(String progress)
    {
        //progressDialog.setMessage(progress);
    }
}
