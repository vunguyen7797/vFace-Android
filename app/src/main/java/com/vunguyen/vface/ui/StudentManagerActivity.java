/*
 * StudentManagerActivity.java
 */
package com.vunguyen.vface.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.bean.Date;
import com.vunguyen.vface.bean.Face;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.helper.LocaleHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class implements functions for student manager screen activity
 */
public class StudentManagerActivity extends AppCompatActivity
{
    // array adapter and database
    private ArrayAdapter<Student> studentArrayAdapter;
    ArrayAdapter<Course> tvArrayAdapter;
    DatabaseReference mDatabase_Course;
    DatabaseReference mDatabase_Student;
    DatabaseReference mDatabase_Face;
    DatabaseReference mDatabase_Date;

    // variables
    String account;
    private int courseId = 0;
    private String courseServerId ="";

    // Menu request code
    private static final int MENU_ITEM_VIEW = 111;
    private static final int MENU_ITEM_EDIT = 222;
    //private static final int MENU_ITEM_ADD = 333;
    private static final int MENU_ITEM_DELETE = 444;
    private static final int MY_REQUEST_CODE = 1000;

    // UI
    FloatingActionButton fabAddStudent;
    AutoCompleteTextView courseMenu;
    ListView lvStudents;
    ImageView ivWaiting;

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_manager);

        // get account to identify the database
        account = getIntent().getStringExtra("ACCOUNT");

        initView();
        initData();
    }

    private void initData()
    {
        mDatabase_Course = FirebaseDatabase.getInstance().getReference().child(account).child("course");
        // get all courses belong to this account
        mDatabase_Student = FirebaseDatabase.getInstance().getReference().child(account).child("student");
        mDatabase_Face = FirebaseDatabase.getInstance().getReference().child(account).child("face");
        mDatabase_Date = FirebaseDatabase.getInstance().getReference().child(account).child("date");
        getCourseList();
    }

    private void initView()
    {
        // initialize student list view and student database
        lvStudents = findViewById(R.id.lvStudents);
        lvStudents.setVisibility(View.GONE);
        fabAddStudent = findViewById(R.id.floating_action_button);
        fabAddStudent.setVisibility(View.GONE);
        ivWaiting = findViewById(R.id.ivWaiting);
        courseMenu = findViewById(R.id.filled_exposed_dropdown);
    }


    public void getCourseList()
    {
        mDatabase_Course.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Course> courseList = new ArrayList<>();
                for(DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    courseList.add(dsp.getValue(Course.class));
                }

                tvArrayAdapter = new ArrayAdapter<>(getApplicationContext(),
                        R.layout.dropdown_menu_popup_item, courseList);
                courseMenu.setAdapter(tvArrayAdapter);

                initializeSelection(courseList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void getStudentList(String courseServerId)
    {
        mDatabase_Student.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Student> studentList = new ArrayList<>();
                for(DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    if (Objects.requireNonNull(dsp.getValue(Student.class))
                            .getCourseServerId().equalsIgnoreCase(courseServerId))
                        studentList.add(dsp.getValue(Student.class));
                }
                displayListView(studentList, 0);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // initialize the list view data on the course selection
    private void initializeSelection(List<Course> courseList)
    {
        if (courseList.size() != 0)
        {
            courseMenu.setOnItemClickListener((parent, view, position, id) ->
            {
                courseId = (int) parent.getItemIdAtPosition(position);  // get the course id database
                Course course = (Course) parent.getItemAtPosition(position);
                courseServerId = course.getCourseServerId();            // get course id on server
                getStudentList(courseServerId);
                //displayListView(courseServerId, 1);   // request 1 to notify that selection is changed
                ivWaiting.setVisibility(View.GONE);
                fabAddStudent.setVisibility(View.VISIBLE);
                lvStudents.setVisibility(View.VISIBLE);
            });
            // Button Add Student event, send the course ids to the student profile activity
            fabAddStudent.setOnClickListener(v -> addStudent());
        }
        else
        {
            // If there is no course, the button is disable and show notification
            fabAddStudent.setEnabled(false);
            fabAddStudent.setOnClickListener(v ->
                    Toast.makeText(getApplicationContext(), "Add a course before adding students.",
                    Toast.LENGTH_SHORT).show());
        }
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
        menu.add(0, MENU_ITEM_VIEW , 0, getResources().getString(R.string.menu_view_student));
        //menu.add(0, MENU_ITEM_ADD , 1, getResources().getString(R.string.menu_add_student));
        menu.add(0, MENU_ITEM_EDIT , 2, getResources().getString(R.string.menu_edit_student));
        menu.add(0, MENU_ITEM_DELETE, 4, getResources().getString(R.string.menu_delete_student));
    }

    // Set action for each item selected on menu
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo
                info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        // Get selected student to apply action
        final Student selectedStudent = (Student) this.lvStudents.getItemAtPosition(info.position);

        if(item.getItemId() == MENU_ITEM_VIEW)
        {
            /*new MaterialAlertDialogBuilder(this)
                    .setTitle("VFACE - STUDENT MANAGER")
                    .setMessage("Student ID Number: " + selectedStudent.getStudentIdNumber() +
                            "\nStudent Name: " + selectedStudent.getStudentName())
                    .setCancelable(false)
                    .setNegativeButton("OK", null)
                    .show(); */
            passDataToStudentProfile(selectedStudent, StudentManagerActivity.this);

        }
       // else if(item.getItemId() == MENU_ITEM_ADD)
        //{
        //    addStudent();
        //}
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

    private void passDataToStudentProfile(Student student, Context context)
    {
        mDatabase_Face.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Face face = dsp.getValue(Face.class);
                    assert face != null;
                    if (face.getStudentServerId().equalsIgnoreCase(student.getStudentServerId()))
                    {
                        Uri faceUri = Uri.parse(face.getStudentFaceUri());

                        mDatabase_Date.addValueEventListener(new ValueEventListener()
                        {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                            {
                                int counter = 0;
                                for (DataSnapshot dsp : dataSnapshot.getChildren())
                                {
                                    Date temp = dsp.getValue(Date.class);
                                    assert temp != null;
                                    if (temp.getCourseServerId().equalsIgnoreCase(courseServerId)
                                            && temp.getStudentServerId().equalsIgnoreCase(student.getStudentServerId())
                                            && temp.getStudentAttendanceStatus().equalsIgnoreCase("NO"))
                                        counter++;
                                }
                                Integer totalAbsence = counter;
                                Intent intent = new Intent(context, StudentProfilePageActivity.class);
                                intent.putExtra("ACCOUNT", account);
                                intent.putExtra("Student", student);
                                intent.setData(faceUri);
                                intent.putExtra("Absence", totalAbsence);
                                startActivityForResult(intent, MY_REQUEST_CODE);
                                mDatabase_Date.removeEventListener(this);
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError)
                            {
                            }
                        });
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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
        // Refresh ListView.
        mDatabase_Student.child(student.getStudentName().toUpperCase()+"-"+student
                .getStudentServerId()).removeValue();
        deleteAllFace(student.getStudentServerId());
        deleteAllDate(student);
        this.studentArrayAdapter.notifyDataSetChanged();
        // execute background task to delete student from server
        new com.vunguyen.vface.helper.asyncTasks.DeleteStudentTask(courseServerId).execute(student.getStudentServerId());
    }

    private void deleteAllFace(String studentServerId)
    {
        mDatabase_Face.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    if (Objects.requireNonNull(dsp.getValue(Face.class)).getStudentServerId().equalsIgnoreCase(studentServerId))
                    {
                        mDatabase_Face.child(Objects.requireNonNull(dsp.getValue(Face.class)).getStudentFaceServerId()).removeValue();
                        StorageReference photoRef = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(Objects.requireNonNull(dsp.getValue(Face.class)).getStudentFaceUri());
                        photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.i("EXECUTE", "Deleted face from Firebase Storage");
                            }
                        }).addOnFailureListener(e -> Log.i("EXECUTE", "Cannot delete face from Firebase Storage"));
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void deleteAllDate(Student student)
    {
        mDatabase_Date.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Date date = dsp.getValue(Date.class);
                    assert date != null;
                    if (date.getStudentServerId().equalsIgnoreCase(student.getStudentServerId()))
                    {
                        mDatabase_Date.child(student.getStudentName().toUpperCase() + "-" +
                                date.getStudent_date().replaceAll("[,]", "")).removeValue();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // Refresh the student list after adding new student
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == MY_REQUEST_CODE) {
            boolean needRefresh = data.getBooleanExtra("needRefresh", true);
            account = data.getStringExtra("ACCOUNT");
            // Refresh ListView
            if (needRefresh) {
                this.studentArrayAdapter.notifyDataSetChanged();
            }
        }
    }

    // Display the information of student on GridView based on course selection
    private void displayListView(List<Student> studentList, int request)
    {
        if (request == 0) // default data is the student of first course in the list
        {
            studentArrayAdapter = new ArrayAdapter<Student>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, studentList)
            {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
                {
                    View view = super.getView(position, convertView, parent);
                    TextView tv = view.findViewById(android.R.id.text1);
                    tv.setTextColor(Color.WHITE);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                    tv.setAllCaps(true);
                    return view;
                }
            };

            // Register Adapter cho ListView.
            this.lvStudents.setAdapter(this.studentArrayAdapter);
            // Register the menu context
            registerForContextMenu(this.lvStudents);
        }
    }

    public void btnBackArrow(View view)
    {
        onBackPressed();
    }
}
