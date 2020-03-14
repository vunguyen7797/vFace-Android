/*
 * AboutActivity.java
 */
package com.vunguyen.vface.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.vunguyen.vface.R;


/**
 * This class to display the information about the app and contacts
 */
public class AboutActivity extends AppCompatActivity
{
    String account;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //* Hide Notification bar
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_about);
        // get account to verify the database
        account = getIntent().getStringExtra("ACCOUNT");
    }

    public void btnBackClick(View view)
    {
        onBackPressed();
    }

    // go back to previous activity
    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(AboutActivity.this, DashBoardActivity.class);
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
        finish();
    }
}
