/*
 * DashBoardActivity.java
 */
package com.vunguyen.vface.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.blogspot.atifsoftwares.animatoolib.Animatoo;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;
import com.vunguyen.vface.R;
import com.vunguyen.vface.helper.ImageEditor;
import com.vunguyen.vface.helper.LocaleHelper;

import java.io.IOException;
import java.util.Objects;

/**
 * This class is the implementations for the dash board screen
 */
public class DashBoardActivity extends AppCompatActivity
{
    CardView cvGroupCheck;
    CardView cvAttendance;
    CardView cvAddStudentCourse;
    CardView cvAboutUs;
    CardView cvSettings;
    CardView cvSelfCheck;
    String account;
    FirebaseUser user;
    ImageView ivPhoto;
    TextView tvDisplayName;

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);

        // email to identify database
        account = getIntent().getStringExtra("ACCOUNT");
        initView();
        initData();
        initAction();
    }

    private void initAction()
    {
        cvGroupCheck.setOnClickListener(v -> {
            Intent intent = new Intent(DashBoardActivity.this, GroupCheckActivity.class);
            goToAFeature(intent);
        });

        cvAddStudentCourse.setOnClickListener(v -> {
            Intent intent = new Intent(DashBoardActivity.this, StudentCoursesActivity.class);
            goToAFeature(intent);

        });

        cvAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(DashBoardActivity.this, AttendanceActivity.class);
            goToAFeature(intent);
        });

        cvAboutUs.setOnClickListener(v -> {
            Intent intent = new Intent(DashBoardActivity.this, AboutActivity.class);
            goToAFeature(intent);
        });

        cvSettings.setOnClickListener(v -> {
            Intent intent = new Intent(DashBoardActivity.this, SettingsActivity.class);
            goToAFeature(intent);
        });


        cvSelfCheck.setOnClickListener(v -> {
            Intent intent = new Intent(DashBoardActivity.this, SelfCheckActivity.class);
            goToAFeature(intent);
        });
    }

    private void initData()
    {
        // subscription notice
        if (getString(R.string.subscription_key).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        }

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getPhotoUrl() != null)
        {
            Picasso.get().load(user.getPhotoUrl()).into(ivPhoto);
        }
        else
            Log.i("EXECUTE", "NO PROFILE PHOTO");

        if (user != null && user.getDisplayName() != null)
        {
            String welcome = "Hello " + user.getDisplayName() + "!";
            tvDisplayName.setText(welcome);
        }
        else
        {
            String slogan = getResources().getString(R.string.sloganDashboard);
            tvDisplayName.setText(slogan);
        }
    }

    private void initView()
    {
        ivPhoto = findViewById(R.id.logo);
        tvDisplayName = findViewById(R.id.tvDisplayName);
        // set event for each menu item
        cvGroupCheck = findViewById(R.id.cvGroupCheck);
        cvAddStudentCourse = findViewById(R.id.cvAddStudentCourse);
        cvAttendance = findViewById(R.id.cvAttendance);
        cvAboutUs = findViewById(R.id.cvAboutUs);
        cvSettings = findViewById(R.id.cvSetting);
        cvSelfCheck = findViewById(R.id.cvSelfCheck);
    }

    // This method is used to go to other activities
    public void goToAFeature(Intent intent)
    {
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed()
    {
        new MaterialAlertDialogBuilder(this)
                .setTitle("VFACE")
                .setMessage(getResources().getString(R.string.want_exit_dialog))
                .setNegativeButton(getResources().getString(R.string.no_btn),null)
                .setPositiveButton(getResources().getString(R.string.yes_btn), (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    startActivity(intent);
                    int pid = android.os.Process.myPid();
                    android.os.Process.killProcess(pid);
                })
                .show();
    }
}
