/*
 * MyDatabaseHelperDate.java
 */
package com.vunguyen.vface.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.vunguyen.vface.bean.Date;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains methods to work with database
 * of attendance date of each student
 */
public class MyDatabaseHelperDate extends SQLiteOpenHelper
{
    // version
    private static final int DATABASE_VERSION = 1;

    // Face database;
    private static final String DATABASE_NAME = "Date_Manager";

    private static final String TABLE_DATE = "Date";
    private static final String COLUMN_DATE_ID ="Date_Id";
    private static final String COLUMN_DATE_COURSEId = "Date_CourseServerId";
    private static final String COLUMN_DATE_STUDENTId ="Date_StudentServerId";
    private static final String COLUMN_DATE_DATE_STR = "Date_StringDate";
    private static final String COLUMN_DATE_FLAG = "Date_StudentFlag";


    public MyDatabaseHelperDate(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Create tables
    @Override
    public void onCreate(SQLiteDatabase db)
    {
        Log.i("EXECUTE", "MyDatabaseHelperDate.onCreate ... ");
        // Script to create tables
        String script = "CREATE TABLE " + TABLE_DATE + "("
                + COLUMN_DATE_ID + " INTEGER PRIMARY KEY," + COLUMN_DATE_STUDENTId + " TEXT," + COLUMN_DATE_COURSEId + " TEXT,"
                + COLUMN_DATE_DATE_STR + " TEXT," + COLUMN_DATE_FLAG + " TEXT" + ")";
        // execute the script
        db.execSQL(script);
    }

    // Update database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        Log.i("EXECUTE", "MyDatabaseHelperStudent.onUpgrade ... ");

        // Drop old tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATE);

        // Recreate new one
        onCreate(db);
    }

    // Add a single face into the database
    public void addDate(Date student_date)
    {
        Log.i("EXECUTE", "MyDatabaseHelperDate.addDate ... " + student_date.getStudentServerId());

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE_COURSEId, student_date.getCourseServerId());
        values.put(COLUMN_DATE_STUDENTId, student_date.getStudentServerId());
        values.put(COLUMN_DATE_DATE_STR, student_date.getStudent_date());
        values.put(COLUMN_DATE_FLAG, student_date.getStudentAttendanceStatus());

        // Insert a new line of data into the tables
        db.insert(TABLE_DATE, null, values);
        Log.i("EXECUTE", "ADD DATE DONE");
        // Close the database connection
        db.close();
    }


    // Return all dates belong to one student with student server Id
    public List<Date> getDateWithStudent(String studentServerId, String courseServerId)
    {
        Log.i("EXECUTE", "MyDatabaseHelperDate.getDateWithStudent ...");

        List<Date> dateList = new ArrayList<Date>();

        String selectQuery = "SELECT * FROM " + TABLE_DATE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {
                Date date = new Date();
                date.setDateId(Integer.parseInt(cursor.getString(0)));
                date.setStudentServerId(cursor.getString(1));
                date.setCourseServerId(cursor.getString(2));
                date.setStudent_date(cursor.getString(3));
                date.setStudentAttendanceStatus(cursor.getString(4));

                // add date to the list if the student server Id matching
                if (cursor.getString(1).equalsIgnoreCase(studentServerId)
                        && cursor.getString(2).equalsIgnoreCase(courseServerId))
                {
                    dateList.add(date);
                }
                else
                    Log.i("EXECUTE", "Error: Authentication Ids do not match.");

            } while (cursor.moveToNext());
        }
        else
        {
            Log.i("EXECUTE", "Response: No dates are found for this student: " + studentServerId);
        }
        // return date list
        return dateList;
    }

    // Return attendance status of a student in a course in a particular day
    public String getStudentStatus(String studentServerId, String courseServerId, String date_string)
    {
        Log.i("EXECUTE", "MyDatabaseHelperDate.getDateWithStudent ...");

        String studentStatus = "";

        String selectQuery = "SELECT * FROM " + TABLE_DATE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {
                // add date to the list if the student server Id matching
                if (cursor.getString(1).equalsIgnoreCase(studentServerId)
                        && cursor.getString(2).equalsIgnoreCase(courseServerId)
                        && cursor.getString(3).equalsIgnoreCase(date_string))
                {
                    //dateList.add(date);
                    studentStatus = cursor.getString(4);
                }
                else
                    Log.i("EXECUTE", "Error: Authentication Ids do not match.");

            } while (cursor.moveToNext());
        }
        else
        {
            Log.i("EXECUTE", "Response: No dates are found for this student: " + studentServerId);
        }
        // return student attendance status
        return studentStatus;
    }

    // Get total class absences of a student
    public int getTotalAbsence(String studentServerId, String courseServerId)
    {
        Log.i("EXECUTE", "MyDatabaseHelperDate.getAbsenceWithStudent ...");

        int count = 0;

        String selectQuery = "SELECT * FROM " + TABLE_DATE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {
                Date date = new Date();
                date.setDateId(Integer.parseInt(cursor.getString(0)));
                date.setStudentServerId(cursor.getString(1));
                date.setCourseServerId(cursor.getString(2));
                date.setStudent_date(cursor.getString(3));
                date.setStudentAttendanceStatus(cursor.getString(4));

                // add date to the list if the student server Id matching
                if (cursor.getString(1).equalsIgnoreCase(studentServerId)
                        && cursor.getString(2).equalsIgnoreCase(courseServerId)
                        && cursor.getString(4).equalsIgnoreCase("NO"))
                {
                    count++;
                }
                else
                    Log.i("EXECUTE", "Error: Authentication Ids do not match.");

            } while (cursor.moveToNext());
        }
        else
        {
            Log.i("EXECUTE", "Response: No dates are found for this student: " + studentServerId);
        }
        // return total absences
        return count;
    }

    // Remove a date of a student
    public void deleteDatesWithStudent(String studentServerId, String courseServerId)
    {
        Log.i("EXECUTE", "MyDatabaseHelperFace.deleteDate ...");

        String selectQuery = "SELECT * FROM " + TABLE_DATE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {
                if (cursor.getString(1).equalsIgnoreCase(studentServerId)
                        && cursor.getString(2).equalsIgnoreCase(courseServerId))
                {
                    db.delete(TABLE_DATE, COLUMN_DATE_ID + " = ?",
                            new String[] { String.valueOf(cursor.getInt(0)) });
                }
                else
                    Log.i ("EXECUTE", "Error: Student server Ids do not match.");
            } while (cursor.moveToNext());
        }
        else
        {
            Log.i("EXECUTE", "Response: No dates are found for this student.");
        }
        db.close();
    }

    public void deleteADate(String studentServerId, String courseServerId, String date)
    {
        String selectQuery = "SELECT * FROM " + TABLE_DATE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {
                if (cursor.getString(1).equalsIgnoreCase(studentServerId)
                        && cursor.getString(2).equalsIgnoreCase(courseServerId)
                        && cursor.getString(3).equalsIgnoreCase(date))
                {
                    db.delete(TABLE_DATE, COLUMN_DATE_ID + " = ?",
                            new String[] { String.valueOf(cursor.getInt(0)) });
                }
                else
                    Log.i ("EXECUTE", "Error: Student server Ids do not match.");
            } while (cursor.moveToNext());
        }
        else
        {
            Log.i("EXECUTE", "Response: No dates are found for this student.");
        }
        db.close();
    }

}
