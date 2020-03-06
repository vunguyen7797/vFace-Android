package com.vunguyen.vface.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.vunguyen.vface.R;

public class DashBoardActivity extends AppCompatActivity {

    TextView tvLogOut;
    FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    CardView cvGroupCheck;
    CardView cvAddStudentCourse;
    String account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //* Hide Notification bar
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_dash_board);

        account = getIntent().getStringExtra("ACCOUNT"); // email to identify database

        if (getString(R.string.subscription_key).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        }

        tvLogOut = findViewById(R.id.tvLogOut);
        tvLogOut.setText(account +"\nTap here to log out?");
        tvLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intToWelcome = new Intent(DashBoardActivity.this, WelcomeScreenActivity.class);
                startActivity(intToWelcome);
                finish();
            }
        });

        cvGroupCheck = findViewById(R.id.cvGroupCheck);
        cvGroupCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(DashBoardActivity.this, GroupCheckActivity.class);
                intent.putExtra("ACCOUNT", account);
                startActivity(intent);
            }
        });

        cvAddStudentCourse = findViewById(R.id.cvAddStudentCourse);
        cvAddStudentCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(DashBoardActivity.this, StudentCoursesActivity.class);
                intent.putExtra("ACCOUNT", account);
                startActivity(intent);
            }
        });

    }
}
