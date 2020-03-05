
package com.vunguyen.vface.ui;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.MyDatabaseHelperCourse;
import com.vunguyen.vface.helper.MyDatabaseHelperFace;
import com.vunguyen.vface.helper.MyDatabaseHelperStudent;

import java.util.ArrayList;
import java.util.List;

public class AddCourseActivity extends AppCompatActivity
{
    // Background task of deleting a course from server
    class DeleteCourseTask extends AsyncTask<String, String, String>
    {
        @Override
        protected String doInBackground(String... params)
        {
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                publishProgress("Deleting selected course...");
                faceServiceClient.deleteLargePersonGroup(params[0]);
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
            if (result != null) {
               Log.i("EXECUTE","GROUP DELETED");
            }
        }
    }


    Button btnAddCourse;
    ListView lvCourses;
    Button btnDone;

    // Request code for the menu
    private static final int MENU_ITEM_VIEW = 111;
    private static final int MENU_ITEM_EDIT = 222;
    private static final int MENU_ITEM_ADD = 333;
    private static final int MENU_ITEM_DELETE = 444;

    private static final int MY_REQUEST_CODE = 1000;

    private final List<Course> courseList = new ArrayList<>();
    private ArrayAdapter<Course> listViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Set no notification bar on activity
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_add_course);

        // Set event for the Add Course button
        btnAddCourse = findViewById(R.id.btnAddCourse);
        btnAddCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddCourseActivity.this, AddEditCourseActivity.class);
                startActivityForResult(intent, MY_REQUEST_CODE);
                finish();
            }
        });

        // Open the course database, transfer all course from database to a course list
        MyDatabaseHelperCourse db = new MyDatabaseHelperCourse(this);
        List<Course> list=  db.getAllCourses();
        this.courseList.addAll(list);


        lvCourses = findViewById(R.id.lvCourses);

        // create adapter for listview of courses
        this.listViewAdapter = new ArrayAdapter<Course>(this,
                android.R.layout.simple_list_item_activated_1, android.R.id.text1, this.courseList)
        {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                tv.setTextColor(Color.WHITE);
                tv.setAllCaps(true);
                return view;
            }
        };


        // Register Adapter for ListView.
        this.lvCourses.setAdapter(this.listViewAdapter);

        // Register the menu context for ListView.
        registerForContextMenu(this.lvCourses);

        // Set event for the Done button
        btnDone = findViewById(R.id.btnDone);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AddCourseActivity.this, StudentCoursesActivity.class));
                finish();
            }
        });
    }


    // Create Menu for ListView
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo)
    {

        super.onCreateContextMenu(menu, view, menuInfo);

        // groupId, itemId, order, title
        menu.add(0, MENU_ITEM_VIEW , 0, "View Course Information");
        menu.add(0, MENU_ITEM_ADD , 1, "Add Course");
        menu.add(0, MENU_ITEM_EDIT , 2, "Edit Course");
        menu.add(0, MENU_ITEM_DELETE, 4, "Delete Course");
    }

    // Actions when menu selected
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo
                info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        final Course selectedCourse = (Course) this.lvCourses.getItemAtPosition(info.position);

        if(item.getItemId() == MENU_ITEM_VIEW)
        {
            // Display a dialog with student information
            new AlertDialog.Builder(this)
                    .setMessage("Course ID Number: " + selectedCourse.getCourseIdNumber() +
                            "\nCourse Name: " + selectedCourse.getCourseName())
                    .setCancelable(false)
                    .setNegativeButton("OK", null)
                    .show();
        }
        else if(item.getItemId() == MENU_ITEM_ADD)
        {
            Intent intent = new Intent(this, AddEditCourseActivity.class);
            this.startActivityForResult(intent, MY_REQUEST_CODE);
        }
        else if(item.getItemId() == MENU_ITEM_EDIT )
        {
            Intent intent = new Intent(this, AddEditCourseActivity.class);
            intent.putExtra("course", selectedCourse);
            this.startActivityForResult(intent,MY_REQUEST_CODE);
        }
        else if(item.getItemId() == MENU_ITEM_DELETE)
        {
            // Confirmation dialog before delete
            new AlertDialog.Builder(this)
                    .setMessage(selectedCourse.getCourseName() +". Are you sure you want to delete?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            deleteCourse(selectedCourse);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
        else {
            return false;
        }
        return true;
    }

    // Delete the course if the user accepts
    private void deleteCourse(Course course)
    {
        MyDatabaseHelperCourse db = new MyDatabaseHelperCourse(this);
        String courseServerId = course.getCourseServerId();
        db.deleteCourse(course);
        this.courseList.remove(course);
        new DeleteCourseTask().execute(courseServerId);
        MyDatabaseHelperStudent db_student = new MyDatabaseHelperStudent(this);
        MyDatabaseHelperFace db_face = new MyDatabaseHelperFace(this);
        db_student.deleteStudentWithCourse(courseServerId, db_face);
        Log.i("EXECUTE", "DELETE COURSE " + courseServerId);
        // Refresh ListView.
        this.listViewAdapter.notifyDataSetChanged();
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
                MyDatabaseHelperCourse db = new MyDatabaseHelperCourse(this);
                List<Course> list = db.getAllCourses();
                this.courseList.addAll(list);
                // Notify the change of data to refresh the listview
                this.listViewAdapter.notifyDataSetChanged();
            }
        }
    }
}
