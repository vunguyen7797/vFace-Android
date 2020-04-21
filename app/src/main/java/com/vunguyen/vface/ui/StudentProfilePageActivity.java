/*
 * StudentProfilePageActivity.java
 */
package com.vunguyen.vface.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ramijemli.percentagechartview.PercentageChartView;
import com.squareup.picasso.Picasso;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Date;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.helper.LocaleHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * This class implements the profile page of a students with the students absence information
 */
public class StudentProfilePageActivity extends AppCompatActivity {

    String account;
    Student studentInfo;
    Uri studentUri;
    String totalAbsence;
    double  overallAttendance = 0.00;

    TextInputEditText etName;
    TextInputEditText etCourseName;
    TextInputEditText etAbsence;
    ImageView profilePhoto;
    ListView lvHistory;
    TextView tvStudentId;
    TextView tvTotalAbsence;
    PercentageChartView progressBar;

    ArrayAdapter<String> absenceAdapter;

    DatabaseReference mDatabase_date;
    DatabaseReference mDatabase_student;

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile_page);

        account = getIntent().getStringExtra("ACCOUNT");
        initView();
        initData();
        initAction();
    }

    private void initAction()
    {

    }

    @SuppressLint("SetTextI18n")
    private void initData()
    {
        mDatabase_date = FirebaseDatabase.getInstance().getReference().child(account).child("date");
        mDatabase_student = FirebaseDatabase.getInstance().getReference().child(account).child("student");

        studentInfo = (Student) getIntent().getSerializableExtra("Student");
        assert studentInfo != null;
        String idNumber = "ID: " + studentInfo.getStudentIdNumber().toUpperCase();
        String courseName = Objects.requireNonNull(getIntent().getStringExtra("CourseName")).toUpperCase();
        etName.setText(studentInfo.getStudentName().toUpperCase());
        tvStudentId.setText(idNumber);
        etCourseName.setText(courseName);
        studentUri = getIntent().getData();
        Picasso.get().load(studentUri).into(profilePhoto);
        totalAbsence = "(" + getIntent().getSerializableExtra("Absence").toString() + ")";
        tvTotalAbsence.setText(totalAbsence);
        getAbsenceHistory(studentInfo.getStudentServerId(), studentInfo.getCourseServerId());
    }

    private void initView() {
        etName = findViewById(R.id.etStudentName);
        etCourseName = findViewById(R.id.etStudentCourse);
        profilePhoto = findViewById(R.id.ivProfilePhoto);
        etAbsence = findViewById(R.id.etStudentAbsence);
        lvHistory = findViewById(R.id.lvHistory);
        tvStudentId = findViewById(R.id.tvStudentId);
        tvTotalAbsence = findViewById(R.id.tvTotalAbsence);
        progressBar = findViewById(R.id.progressBar);
    }

    private void displayTotalAbsence(int totalDay, int totalAttendance)
    {
        if (totalDay > 0)
            overallAttendance = ((double)totalAttendance / (double)totalDay) * 100;

        @SuppressLint("DefaultLocale")
        String absences = totalAttendance + "/" + totalDay + " - " + String.format("%.2f", overallAttendance) + "%";
        etAbsence.setText(absences);
        progressBar.setProgress((float) overallAttendance, true);
    }

    private void getAbsenceHistory(String studentServerId, String courseServerId)
    {
        mDatabase_date.addValueEventListener(new ValueEventListener()
        {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                int totalDays = 0;
                int totalAttendance = 0;
                List<String> absenceHistory = new ArrayList<>();
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    Date temp = dsp.getValue(Date.class);
                    assert temp != null;
                    if (temp.getStudentServerId().equalsIgnoreCase(studentServerId)
                            && temp.getCourseServerId().equalsIgnoreCase(courseServerId))
                    {
                        if (temp.getStudentAttendanceStatus().equalsIgnoreCase("YES"))
                            totalAttendance++;
                        totalDays++;
                    }

                    if (temp.getStudentServerId().equalsIgnoreCase(studentServerId)
                         && temp.getCourseServerId().equalsIgnoreCase(courseServerId)
                         && temp.getStudentAttendanceStatus().equalsIgnoreCase("NO"))
                    {
                        java.util.Date d = null;
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                        try
                        {
                            d = sdf.parse(Objects.requireNonNull(dsp.getValue(Date.class)).getStudent_date());
                        }
                        catch (ParseException ex)
                        {
                            Log.v("Exception", Objects.requireNonNull(ex.getLocalizedMessage()));
                        }
                        sdf.applyPattern("EEE, MMM dd, yyyy");
                        absenceHistory.add(sdf.format(d));
                    }
                }
                displayAbsenceHistory(absenceHistory);
                displayTotalAbsence(totalDays, totalAttendance);
                mDatabase_date.removeEventListener(this);
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
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
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
