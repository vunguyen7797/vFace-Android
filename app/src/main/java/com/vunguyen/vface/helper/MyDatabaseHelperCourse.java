/*
 * MyDatabaseHelperCourse.java
 */
package com.vunguyen.vface.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.vunguyen.vface.bean.Course;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains the methods to work with database of course
 */
public class MyDatabaseHelperCourse extends SQLiteOpenHelper
{
    // version
    private static final int DATABASE_VERSION = 1;

    // Courses database attributes;
    private static final String DATABASE_NAME = "Courses_Manager";
    private static final String TABLE_COURSE = "Course";
    private static final String COLUMN_COURSE_ID ="Course_Id";
    private static final String COLUMN_COURSE_COURSEId ="Course_NumberID";
    private static final String COLUMN_COURSE_NAME = "Course_Name";
    private static final String COLUMN_COURSE_SERVERId = "Course_ServerId";

    public MyDatabaseHelperCourse(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Create tables
    @Override
    public void onCreate(SQLiteDatabase db)
    {
        Log.i("EXECUTE", "MyDatabaseHelperCourse.onCreate ... ");
        // Script to create tables
        String script = "CREATE TABLE " + TABLE_COURSE + "("
                + COLUMN_COURSE_ID + " INTEGER PRIMARY KEY," + COLUMN_COURSE_COURSEId + " TEXT,"
                + COLUMN_COURSE_NAME + " TEXT," + COLUMN_COURSE_SERVERId + " TEXT" + ")";
        // execute the script
        db.execSQL(script);
    }

    // Update database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

        Log.i("EXECUTE", "MyDatabaseHelperCourse.onUpgrade ... ");

        // Drop old tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COURSE);

        // Recreate new one
        onCreate(db);
    }

    // Add a single course into database
    public void addCourse(Course course)
    {
        Log.i("EXECUTE", "MyDatabaseHelperCourse.addCourse ... " + course.getCourseIdNumber());

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_COURSE_COURSEId, course.getCourseIdNumber());
        values.put(COLUMN_COURSE_NAME, course.getCourseName());
        values.put(COLUMN_COURSE_SERVERId, course.getCourseServerId());

        // Insert a new line of data into the tables
        db.insert(TABLE_COURSE, null, values);

        // Close the database connection
        db.close();
    }

    // Return all courses available in the database
    public List<Course> getAllCourses()
    {
        Log.i("EXECUTE", "MyDatabaseHelperCourse.getAllCourses ... " );

        List<Course> courseList = new ArrayList<Course>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_COURSE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
        {
            do {
                Course course = new Course();
                course.setCourseId(Integer.parseInt(cursor.getString(0)));
                course.setCourseIdNumber(cursor.getString(1));
                course.setCourseName(cursor.getString(2));
                course.setCourseServerId(cursor.getString(3));

                // Add to the list
                courseList.add(course);
            } while (cursor.moveToNext());
        }

        // return course list
        return courseList;
    }

    // Return the number of courses in the database
    public int getCoursesCount()
    {
        Log.i(TAG, "MyDatabaseHelperCourse.getCoursesCount ... " );

        String countQuery = "SELECT  * FROM " + TABLE_COURSE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);

        int count = cursor.getCount();

        cursor.close();
        // return count
        return count;
    }

    // Update the information of a course in database
    public int updateCourse(Course course) {
        Log.i(TAG, "MyDatabaseHelperCourse.updateCourse ... "  + course.getCourseIdNumber());

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_COURSE_COURSEId, course.getCourseIdNumber());
        values.put(COLUMN_COURSE_NAME, course.getCourseName());
        values.put(COLUMN_COURSE_SERVERId, course.getCourseServerId());

        // updating row
        return db.update(TABLE_COURSE, values, COLUMN_COURSE_ID + " = ?",
                new String[]{String.valueOf(course.getCourseId())});
    }

    // Delete a course from database
    public void deleteCourse(Course course) {
        Log.i("EXECUTE", "MyDatabaseHelperCourse.deleteCourse ... " + course.getCourseIdNumber() );

        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_COURSE, COLUMN_COURSE_ID + " = ?",
                new String[] { String.valueOf(course.getCourseId()) });
        db.close();
    }
}
