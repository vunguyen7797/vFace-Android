/*
 * SettingsActivity.java
 */
package com.vunguyen.vface.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.vunguyen.vface.R;
import com.vunguyen.vface.helper.LocaleHelper;

import io.paperdb.Paper;

/**
 * This class implements some sub-features for the Settings feature in the app
 */
public class SettingsActivity extends AppCompatActivity
{
    String account;
    String[] settingMenu;

    ListView lvMenu;
    ListViewAdapter menuAdapter;
    LinearLayout layoutLogOut;

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        // get account to verify the database
        account = getIntent().getStringExtra("ACCOUNT");
        int iconList[] = {R.drawable.baseline_account_circle_24,R.drawable.baseline_settings_24,
                R.drawable.baseline_notification_important_24,R.drawable.baseline_lock_24,
                R.drawable.baseline_announcement_24};
        settingMenu = getResources().getStringArray(R.array.setting_menu);
        displayMenu(iconList);
        layoutLogOut = findViewById(R.id.layoutLogOut);
        layoutLogOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intentWelcome = new Intent(SettingsActivity.this, WelcomeScreenActivity.class);
            startActivity(intentWelcome);
            finish();
        });

    }


    // Display the menu
    private void displayMenu(int iconList[])
    {
        lvMenu = findViewById(R.id.lvSetting);
        menuAdapter = new ListViewAdapter(getApplicationContext(), settingMenu, iconList);
        lvMenu.setAdapter(menuAdapter);
        lvMenu.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0)
            {
                Intent intent = new Intent(SettingsActivity.this, ManageAccountActivity.class);
                goToActivity(intent);
            }
            else if (position == 1)
            {
                Intent intent = new Intent(SettingsActivity.this, GeneralSettingActivity.class);
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

    public class ListViewAdapter extends BaseAdapter
    {
        private Context context;
        private String[] settingMenu;
        private int[] iconList;

        public ListViewAdapter(Context context, String[] settingMenu,int[] iconList)
        {
            this.context = context;
            this.settingMenu = settingMenu;
            this.iconList = iconList;

        }

        public int getCount() {
            // TODO Auto-generated method stub
            return settingMenu.length;
        }

        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            if (convertView == null)
            {
                LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_icon_menu, parent, false);
            }

            convertView.setId(position);

            ((ImageView) convertView.findViewById(R.id.face_thumbnail)).setImageResource(iconList[position]);
            ((TextView) convertView.findViewById(R.id.tvDetectedFace)).setText(settingMenu[position]);
            ((TextView) convertView.findViewById(R.id.tvDetectedFace)).setTextColor(Color.WHITE);

            return convertView;
        }
    }
}
