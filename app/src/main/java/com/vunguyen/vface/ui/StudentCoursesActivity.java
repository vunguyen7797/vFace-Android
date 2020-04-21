/*
 * StudentCourseActivity.java
 */
package com.vunguyen.vface.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.vunguyen.vface.R;
import com.vunguyen.vface.helper.LocaleHelper;

/**
 * This class implements functions for the Student + Course feature activity
 */
public class StudentCoursesActivity extends AppCompatActivity
{
    CardView cvAddCourse;
    CardView cvAddStudent;
    ImageView ivBackArrow;
    String account;

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_courses);

        // get account to verify the database
        account = getIntent().getStringExtra("ACCOUNT");
        initView();
        initAction();
    }

    private void initAction()
    {
        cvAddCourse.setOnClickListener(v -> {
            Intent intent = new Intent(StudentCoursesActivity.this, CourseManagerActivity.class);
            intent.putExtra("ACCOUNT", account);
            startActivity(intent);
            finish();
        });
        ivBackArrow.setOnClickListener(v -> onBackPressed());
        cvAddStudent.setOnClickListener(v -> {
            Intent intent = new Intent(StudentCoursesActivity.this, StudentManagerActivity.class);
            intent.putExtra("ACCOUNT", account);
            startActivity(intent);
            finish();
        });
    }

    private void initView()
    {
        // go to course manager
        cvAddCourse = findViewById(R.id.cvAddCourse);
        // go back to dashboard
        ivBackArrow = findViewById(R.id.ivBackArrow);
        // go to student manager activity
        cvAddStudent = findViewById(R.id.cvAddStudent);
    }

    // go back to previous activity
    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(StudentCoursesActivity.this, DashBoardActivity.class);
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
        finish();
    }
}
