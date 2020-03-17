/*
 * SettingsActivity.java
 */
package com.vunguyen.vface.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.vunguyen.vface.R;

/**
 * This class implements some sub-features for the Settings feature in the app
 */
public class SettingsActivity extends AppCompatActivity
{
    String account;
    String[] settingMenu;
    ListView lvMenu;
    ArrayAdapter<String> menuAdapter;
    Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        // get account to verify the database
        account = getIntent().getStringExtra("ACCOUNT");
        settingMenu = getResources().getStringArray(R.array.setting_menu);
        displayMenu();
        btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intentWelcome = new Intent(SettingsActivity.this, WelcomeScreenActivity.class);
            startActivity(intentWelcome);
            finish();
        });
    }

    // Display the menu
    private void displayMenu()
    {

        lvMenu = findViewById(R.id.lvSetting);
        menuAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1, settingMenu)
        {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
            {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                tv.setTextColor(Color.WHITE);
                tv.setAllCaps(false);
                return view;

            }
        };
        lvMenu.setAdapter(menuAdapter);

        lvMenu.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0)
            {
                Intent intent = new Intent(SettingsActivity.this, ManageAccountActivity.class);
                goToActivity(intent);
            }
        });
    }

    private void goToActivity(Intent intent)
    {
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
        finish();
    }

    // go back to previous activity
    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        goToActivity(intent);
    }

    public void btnBackClick(View view)
    {
        onBackPressed();
    }
}
