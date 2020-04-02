/*
 * CourseManagerActivity.java
 */
package com.vunguyen.vface.ui;

import android.app.Activity;
import android.content.Context;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
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
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.bean.Date;
import com.vunguyen.vface.bean.Face;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.LocaleHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    private static final int MY_REQUEST_CODE = 1000;;
    // Array adapter to connect ListView and data
    private ArrayAdapter<Course> courseArrayAdapter;
    DatabaseReference mDatabase_Course;
    DatabaseReference mDatabase_Student;
    DatabaseReference mDatabase_Face;
    DatabaseReference mDatabase_Date;

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_manager);

        // Get email account
        account = getIntent().getStringExtra("ACCOUNT");
        Log.i("EXECUTE", "Account CM: " + account);
        mDatabase_Course = FirebaseDatabase.getInstance().getReference().child(account).child("course");
        mDatabase_Student = FirebaseDatabase.getInstance().getReference().child(account).child("student");
        mDatabase_Face = FirebaseDatabase.getInstance().getReference().child(account).child("face");
        mDatabase_Date = FirebaseDatabase.getInstance().getReference().child(account).child("date");
        // Set event for the Add Course button
        fabAddCourse = findViewById(R.id.floating_action_button);
        fabAddCourse.setOnClickListener(v -> actionAddCourse());;

        getCourseList();
    }

    public void getCourseList()
    {
        mDatabase_Course.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Course> courseList = new ArrayList<>();
                for(DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Log.i("EXECUTE","DSP: " + dsp.getValue(Course.class));
                    courseList.add(dsp.getValue(Course.class));

                }
                displayCourses(courseList);;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });
    }

    // This method to display courses on list view through adapter
    public void displayCourses(List<Course> courseList)
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
                android.R.layout.simple_list_item_activated_1, android.R.id.text1, courseList)
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

        menu.add(0, MENU_ITEM_VIEW , 0, getResources().getString(R.string.menu_view_course));
        menu.add(0, MENU_ITEM_ADD , 1, getResources().getString(R.string.menu_add_course));
        menu.add(0, MENU_ITEM_EDIT , 2, getResources().getString(R.string.menu_edit_course));
        menu.add(0, MENU_ITEM_DELETE, 4, getResources().getString(R.string.menu_delete_course));
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
        //db_course.deleteCourse(course); // delete course from database
        mDatabase_Course.child(course.getCourseName().toUpperCase() + "-" + courseServerId).removeValue();
        deleteAllStudent(courseServerId);
        Log.i("EXECUTE", "Deleted course: " + course.getCourseName() + "-" + courseServerId);
        //this.courseList.remove(course); // delete course from the list
        new DeleteCourseTask().execute(courseServerId); // delete course from server
        // delete all students in the course
        Log.i("EXECUTE", "Deleted course: " + courseServerId);
        // Refresh ListView.
        this.courseArrayAdapter.notifyDataSetChanged();
    }

    private void deleteAllStudent(String courseServerId)
    {
        mDatabase_Student.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    if (Objects.requireNonNull(dsp.getValue(Student.class)).getCourseServerId().equalsIgnoreCase(courseServerId))
                    {
                        Student student = dsp.getValue(Student.class);
                        assert student != null;
                        String path = Objects.requireNonNull(student.getStudentName().toUpperCase())
                                + "-" + Objects.requireNonNull(student.getStudentServerId());
                        Log.i("EXECUTE", "Path Student: " + path);
                        mDatabase_Student.child(path).removeValue();
                        deleteAllFace(student.getStudentServerId());
                        deleteAllDate(dsp.getValue(Student.class));
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void deleteAllFace(String studentServerId)
    {
        mDatabase_Face.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Log.i("EXECUTE","DSP: " + dsp.getValue(Student.class));
                    if (Objects.requireNonNull(dsp.getValue(Face.class)).getStudentServerId()
                            .equalsIgnoreCase(studentServerId))
                    {
                        mDatabase_Face.child(Objects.requireNonNull(dsp.getValue(Face.class))
                                .getStudentFaceServerId()).removeValue();
                        StorageReference photoRef = FirebaseStorage.getInstance().getReferenceFromUrl
                                (Objects.requireNonNull(dsp.getValue(Face.class)).getStudentFaceUri());
                        photoRef.delete().addOnSuccessListener(aVoid -> Log.i("EXECUTE",
                                "Deleted face from Firebase Storage")).addOnFailureListener(e ->
                                Log.i("EXECUTE", "Cannot delete face from Firebase Storage"));
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
                Log.i("EXECUTE", Objects.requireNonNull(e.getMessage()));
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
