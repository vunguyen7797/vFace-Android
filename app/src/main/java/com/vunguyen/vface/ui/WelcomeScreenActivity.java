/*
 * WelcomeScreenActivity.java
 */

package com.vunguyen.vface.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.vunguyen.vface.R;
import com.vunguyen.vface.helper.LocaleHelper;

/**
 * This class is to display the welcome screen of the app
 */
public class WelcomeScreenActivity extends AppCompatActivity
{
    TextView tvRegister;

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        openLoginWindow();
        // event for login button
        Button btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v -> openLoginWindow());

        // Event for register text button
        tvRegister = findViewById(R.id.tvRegister);
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeScreenActivity.this, SignUpActivity.class));
            finish();
        });
    }

    // Open login window for username and password
    public void openLoginWindow()
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(WelcomeScreenActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }, 500);
    }
}
