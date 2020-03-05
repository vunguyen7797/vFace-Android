package com.vunguyen.vface.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.vunguyen.vface.R;

public class StudentCoursesActivity extends AppCompatActivity {

    CardView cvAddCourse;
    CardView cvAddStudent;
    ImageView ivBackArrow;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //* Hide Notification bar
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_student_courses);

        cvAddCourse = findViewById(R.id.cvAddCourse);
        cvAddCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StudentCoursesActivity.this, AddCourseActivity.class));
                finish();
            }
        });

        ivBackArrow = findViewById(R.id.ivBackArrow);
        ivBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StudentCoursesActivity.this, MainActivity.class));
                finish();
            }
        });

        cvAddStudent = findViewById(R.id.cvAddStudent);
        cvAddStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StudentCoursesActivity.this, AddStudentActivity.class));
                finish();
            }
        });

    }
}
