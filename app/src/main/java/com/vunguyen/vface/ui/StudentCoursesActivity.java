package com.vunguyen.vface.ui;

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

public class StudentCoursesActivity extends AppCompatActivity {

    CardView cvAddCourse;
    CardView cvAddStudent;
    ImageView ivBackArrow;
    String account;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //* Hide Notification bar
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_student_courses);

        account = getIntent().getStringExtra("ACCOUNT");
        Toast.makeText(getApplicationContext(), account, Toast.LENGTH_SHORT).show();

        cvAddCourse = findViewById(R.id.cvAddCourse);
        cvAddCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(StudentCoursesActivity.this, CourseManagerActivity.class);
                intent.putExtra("ACCOUNT", account);
                startActivity(intent);
                finish();
            }
        });

        ivBackArrow = findViewById(R.id.ivBackArrow);
        ivBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StudentCoursesActivity.this, DashBoardActivity.class);
                intent.putExtra("ACCOUNT", account);
                startActivity(intent);
                finish();
            }
        });

        cvAddStudent = findViewById(R.id.cvAddStudent);
        cvAddStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(StudentCoursesActivity.this, StudentManagerActivity.class);
                intent.putExtra("ACCOUNT", account);

                startActivity(intent);
                finish();
            }
        });

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(StudentCoursesActivity.this, DashBoardActivity.class);
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
    }
}
