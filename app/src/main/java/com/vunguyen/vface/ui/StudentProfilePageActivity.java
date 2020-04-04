package com.vunguyen.vface.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.bean.Date;
import com.vunguyen.vface.bean.Student;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentProfilePageActivity extends AppCompatActivity {

    String account;
    TextInputEditText etName;
    TextInputEditText etIdNumber;
    TextInputEditText etAbsence;
    Student studentInfo;
    Uri studentUri;
    Integer totalAbsence;
    ImageView profilePhoto;
    ListView lvHistory;
    ArrayAdapter<String> absenceAdapter;

    DatabaseReference mDatabase_date;
    DatabaseReference mDatabase_student;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile_page);

        account = getIntent().getStringExtra("ACCOUNT");
        initView();
        initData();

    }

    @SuppressLint("SetTextI18n")
    private void initData()
    {
        mDatabase_date = FirebaseDatabase.getInstance().getReference().child(account).child("date");
        mDatabase_student = FirebaseDatabase.getInstance().getReference().child(account).child("student");

        studentInfo = (Student) getIntent().getSerializableExtra("Student");
        assert studentInfo != null;
        etName.setText(studentInfo.getStudentName().toUpperCase());
        etIdNumber.setText(studentInfo.getStudentIdNumber().toUpperCase());
        studentUri = getIntent().getData();
        Picasso.get().load(studentUri).into(profilePhoto);
        totalAbsence = (Integer) getIntent().getSerializableExtra("Absence");
        assert totalAbsence != null;
        if (totalAbsence < 2)
            etAbsence.setText(totalAbsence.toString() + " day");
        else
            etAbsence.setText(totalAbsence.toString() + " days");

        getAbsenceHistory(studentInfo.getStudentServerId(), studentInfo.getCourseServerId());
    }

    private void initView() {
        etName = findViewById(R.id.etStudentName);
        etIdNumber = findViewById(R.id.etStudentId);
        profilePhoto = findViewById(R.id.ivProfilePhoto);
        etAbsence = findViewById(R.id.etStudentAbsence);
        lvHistory = findViewById(R.id.lvHistory);
    }

    private void getAbsenceHistory(String studentServerId, String courseServerId)
    {
        mDatabase_date.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                List<String> absenceHistory = new ArrayList<>();
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    if (dsp.getValue(Date.class).getStudentServerId().equalsIgnoreCase(studentServerId)
                         && dsp.getValue(Date.class).getCourseServerId().equalsIgnoreCase(courseServerId)
                         && dsp.getValue(Date.class).getStudentAttendanceStatus().equalsIgnoreCase("NO"))
                    {
                        java.util.Date d = null;
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                        try
                        {
                            d = sdf.parse(dsp.getValue(Date.class).getStudent_date());
                            Log.i("EXECUTE", "Date: " + d);
                        }
                        catch (ParseException ex)
                        {
                            Log.v("Exception", ex.getLocalizedMessage());
                        }
                        sdf.applyPattern("EEE, MMM dd, yyyy - hh:mm");
                        absenceHistory.add(sdf.format(d));

                    }
                }
                displayAbsenceHistory(absenceHistory);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void displayAbsenceHistory(List<String> historyList)
    {
        this.absenceAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_activated_1, android.R.id.text1, historyList)
        {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
            {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                tv.setTextColor(Color.WHITE);
                return view;
            }
        };

        // Register Adapter for ListView.
        this.lvHistory.setAdapter(this.absenceAdapter);
    }



    public void btnBackClick(View view)
    {
        onBackPressed();
    }

    @Override
    public void onBackPressed()
    {
       // Intent intent = new Intent(StudentProfilePageActivity.this, AttendanceActivity.class);
        //intent.putExtra("ACCOUNT", account);
       // startActivity(intent);
        finish();
    }

    // Finish activity
    @Override
    public void finish()
    {
        // Prepare Intent data
        Intent data = new Intent(StudentProfilePageActivity.this, AttendanceActivity.class);
        // Request the grid view refresh
        data.putExtra("ACCOUNT", account);
        // Activity complete
        this.setResult(Activity.RESULT_OK, data);
        super.finish();
    }
}
