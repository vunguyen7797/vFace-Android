/*
 * DashBoardActivity.java
 */
package com.vunguyen.vface.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.vunguyen.vface.R;

/**
 * This class is the implementations for the dash board screen
 */
public class DashBoardActivity extends AppCompatActivity
{
    TextView tvLogOut;
    CardView cvGroupCheck;
    CardView cvAttendance;
    CardView cvAddStudentCourse;
    CardView cvAboutUs;
    String account;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //* Hide Notification bar
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_dash_board);

        // email to identify database
        account = getIntent().getStringExtra("ACCOUNT");

        // subscription notice
        if (getString(R.string.subscription_key).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        }

        // set event for Log Out button
        tvLogOut = findViewById(R.id.tvLogOut);
        tvLogOut.setText(account +"\nTap here to log out?");
        tvLogOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intentWelcome = new Intent(DashBoardActivity.this, WelcomeScreenActivity.class);
            startActivity(intentWelcome);
            finish();
        });

        // set event for each menu item
        cvGroupCheck = findViewById(R.id.cvGroupCheck);
        cvGroupCheck.setOnClickListener(v -> {
            Intent intent = new Intent(DashBoardActivity.this, GroupCheckActivity.class);
            goToAFeature(intent);
        });

        cvAddStudentCourse = findViewById(R.id.cvAddStudentCourse);
        cvAddStudentCourse.setOnClickListener(v -> {
            Intent intent = new Intent(DashBoardActivity.this, StudentCoursesActivity.class);
            goToAFeature(intent);

        });

        cvAttendance = findViewById(R.id.cvAttendance);
        cvAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(DashBoardActivity.this, AttendanceActivity.class);
            goToAFeature(intent);
        });

        cvAboutUs = findViewById(R.id.cvAboutUs);
        cvAboutUs.setOnClickListener(v -> {
            Intent intent = new Intent(DashBoardActivity.this, AboutActivity.class);
            goToAFeature(intent);
        });

    }

    // This method is used to go to other activities
    public void goToAFeature(Intent intent)
    {
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
    }

    @Override
    public void onBackPressed()
    {
        new MaterialAlertDialogBuilder(this)
                .setTitle("VFACE")
                .setMessage("Do you want to exit?")
                .setNegativeButton("No",null)
                .setPositiveButton("Yes", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    startActivity(intent);
                    int pid = android.os.Process.myPid();
                    android.os.Process.killProcess(pid);
                })
                .show();
    }
}
