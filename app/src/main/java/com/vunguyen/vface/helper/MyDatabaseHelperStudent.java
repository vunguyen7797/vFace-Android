/*
 * MyDatabaseHelperStudent.java
 */
package com.vunguyen.vface.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.view.ViewDebug;

import com.vunguyen.vface.bean.Student;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains methods to work with database of students
 */
public class MyDatabaseHelperStudent extends SQLiteOpenHelper
{
    // version
    private static final int DATABASE_VERSION = 1;

    // Student database;
    private static final String DATABASE_NAME = "Students_Manager";

    private static final String TABLE_STUDENT = "Student";
    private static final String COLUMN_STUDENT_ID ="Student_Id";
    private static final String COLUMN_STUDENT_NUMBERId ="Student_NumberId";
    private static final String COLUMN_STUDENT_COURSEId = "Student_CourseServerId";
    private static final String COLUMN_STUDENT_NAME = "Student_Name";
    private static final String COLUMN_STUDENT_SERVERId = "Student_ServerId";
    private static final String COLUMN_STUDENT_IDENTIFY_FLAG = "Student_IdentifyFlag";

    public MyDatabaseHelperStudent(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Create tables
    @Override
    public void onCreate(SQLiteDatabase db)
    {
        Log.i("EXECUTE", "MyDatabaseHelperStudent.onCreate ... ");
        // Script to create tables
        String script = "CREATE TABLE " + TABLE_STUDENT + "("
                + COLUMN_STUDENT_ID + " INTEGER PRIMARY KEY," + COLUMN_STUDENT_COURSEId + " TEXT,"
                + COLUMN_STUDENT_NUMBERId + " TEXT," + COLUMN_STUDENT_NAME + " TEXT,"
                + COLUMN_STUDENT_SERVERId + " TEXT," + COLUMN_STUDENT_IDENTIFY_FLAG + " TEXT" + ")";
        // execute the script
        db.execSQL(script);
    }

    // Update database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

        Log.i("EXECUTE", "MyDatabaseHelperStudent.onUpgrade ... ");

        // Drop old tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STUDENT);

        // Recreate new one
        onCreate(db);
    }

    // add a student object to database
    public void addStudent(Student student)
    {
        Log.i("EXECUTE", "MyDatabaseHelperStudent.addStudent ... " + student.getStudentIdNumber());

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_STUDENT_NUMBERId, student.getStudentIdNumber());
        values.put(COLUMN_STUDENT_COURSEId, student.getCourseServerId());
        values.put(COLUMN_STUDENT_NAME, student.getStudentName());
        values.put(COLUMN_STUDENT_SERVERId, student.getStudentServerId());
        // initialize the default value for all students that have not been identified yet
        values.put(COLUMN_STUDENT_IDENTIFY_FLAG, "NO");

        // Insert a new line of data into the tables
        db.insert(TABLE_STUDENT, null, values);

        // Close the database connection
        db.close();
    }

    // return all students belong to a course from the course server Id
    public List<Student> getStudentWithCourse(String courseServerId)
    {
        Log.i("EXECUTE", "MyDatabaseHelperStudent.getStudentWithCourse ...");

        List<Student> studentList = new ArrayList<Student>();

        String selectQuery = "SELECT * FROM " + TABLE_STUDENT;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {
                Student student = new Student();
                student.setStudentId(Integer.parseInt(cursor.getString(0)));
                student.setStudentIdNumber(cursor.getString(2));
                student.setCourseServerId(cursor.getString(1));
                student.setStudentName(cursor.getString(3));
                student.setStudentServerId(cursor.getString(4));
                student.setStudentIdentifyFlag(cursor.getString(5));

                // Add a student to the student list if the course Id and the column 1 value matching
                if ((cursor.getString(1)).equalsIgnoreCase(courseServerId))
                {
                    studentList.add(student);
                }
                else
                    Log.i("EXECUTE", "Error: Course Server IDs do not match.");
            } while (cursor.moveToNext());
        }
        else
        {
            Log.i("EXECUTE", "Response: No students are found in this course " + courseServerId);
        }

        // return student list
        return studentList;
    }

    // return all students who are absent from the course.
    public List<Student> getAbsenceStudent(String courseServerId)
    {
        Log.i("EXECUTE", "MyDatabaseHelperStudent.getStudentWithAbsence ...");

        List<Student> studentList = new ArrayList<Student>();

        String selectQuery = "SELECT * FROM " + TABLE_STUDENT;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {
                Student student = new Student();
                student.setStudentId(Integer.parseInt(cursor.getString(0)));
                student.setStudentIdNumber(cursor.getString(2));
                student.setCourseServerId(cursor.getString(1));
                student.setStudentName(cursor.getString(3));
                student.setStudentServerId(cursor.getString(4));
                student.setStudentIdentifyFlag(cursor.getString(5));

                // Add a student to the student list if the course Id and the column 1 data matching
                if ((cursor.getString(1)).equalsIgnoreCase(courseServerId)
                        && (cursor.getString(5).equalsIgnoreCase("NO")))
                {
                    // add student to absence list
                    studentList.add(student);
                }
                else
                    Log.i("EXECUTE", "Error: Course Server IDs do not match. ");
            } while (cursor.moveToNext());
        }
        else
        {
            Log.i("EXECUTE", "Response: No students are found in this course " + courseServerId);
        }

        // return student list
        return studentList;
    }

    // Update student data if there is any changes
    public int updateStudent(Student student)
    {
        Log.i("EXECUTE", "MyDatabaseHelperStudent.updateStudent ... "  + student.getStudentIdNumber());

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_STUDENT_NUMBERId, student.getStudentIdNumber());
        values.put(COLUMN_STUDENT_COURSEId, student.getCourseServerId());
        values.put(COLUMN_STUDENT_NAME, student.getStudentName());
        values.put(COLUMN_STUDENT_SERVERId, student.getStudentServerId());
        values.put(COLUMN_STUDENT_IDENTIFY_FLAG, student.getStudentIdentifyFlag());

        // updating row
        return db.update(TABLE_STUDENT, values, COLUMN_STUDENT_ID + " = ?",
                new String[]{String.valueOf(student.getStudentId())});
    }

    // reset all the student identify flag back to "NO" after an identification task completes
    public void resetStudentFlag(List<Student> studentList)
    {
        Log.i("EXECUTE", "MyDatabaseHelperStudent.getResetFlag ...");
        Log.i("EXECUTE", "SIZE: " + Integer.toString(studentList.size()));
        for (int i = 0; i < studentList.size(); i++ )
        {
            studentList.get(i).setStudentIdentifyFlag("NO");
            updateStudent(studentList.get(i));
        }
    }

    // Delete a student row from database
    public void deleteStudent(Student student)
    {
        Log.i("EXECUTE", "MyDatabaseHelperStudent.deleteStudent ... " + student.getStudentIdNumber() );

        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_STUDENT, COLUMN_STUDENT_ID + " = ?",
                new String[] { String.valueOf(student.getStudentId()) });
        db.close();
    }

    // delete a student in a selected course
    public void deleteStudentWithCourse(String courseServerId, MyDatabaseHelperFace db_face)
    {
        Log.i("EXECUTE", "MyDatabaseHelperStudent.deleteStudentsWithCourse ...");

        String selectQuery = "SELECT * FROM " + TABLE_STUDENT;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {
                if ((cursor.getString(1)).equalsIgnoreCase(courseServerId))
                {
                    db.delete(TABLE_STUDENT, COLUMN_STUDENT_ID + " = ?",
                            new String[] { String.valueOf(cursor.getInt(0)) });
                    // When delete a student, all faces will also be deleted
                    db_face.deleteFacesWithStudent(cursor.getString(4));
                }
                else
                    Log.i("EXECUTE", "Error: Course server IDs do not match.");
            } while (cursor.moveToNext());
        }
        else
            Log.i("EXECUTE", "Response: No students are found in this course.");
        db.close();
        db_face.close();
    }

    // return a student object from its student server ID
    public Student getAStudentWithId(String studentServerId, String courseServerId)
    {
        Log.i("EXECUTE", "MyDatabaseHelperStudent.getStudentWithID ...");

        String selectQuery = "SELECT * FROM " + TABLE_STUDENT;

        Student student = new Student();

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {
                Student student_tmp = new Student();
                student_tmp.setStudentId(Integer.parseInt(cursor.getString(0)));
                student_tmp.setStudentIdNumber(cursor.getString(2));
                student_tmp.setCourseServerId(cursor.getString(1));
                student_tmp.setStudentName(cursor.getString(3));
                student_tmp.setStudentServerId(cursor.getString(4));
                student_tmp.setStudentIdentifyFlag(cursor.getString(5));

                // student server ID matching from database and input
                if ((cursor.getString(4)).equalsIgnoreCase(studentServerId)
                        && cursor.getString(1).equalsIgnoreCase(courseServerId))
                {
                   student = student_tmp;
                }
                else
                    Log.i("EXECUTE", "Error: Student server IDs do not mach");
            } while (cursor.moveToNext());
        }
        else
        {
            Log.i("EXECUTE", "Response: No student is found.");
        }

        // return student object
        return student;
    }
}