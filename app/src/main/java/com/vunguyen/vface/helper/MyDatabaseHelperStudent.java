package com.vunguyen.vface.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.vunguyen.vface.bean.Student;

import java.util.ArrayList;
import java.util.List;

public class MyDatabaseHelperStudent extends SQLiteOpenHelper
{
    private static final String TAG = "SQLite";


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

    public MyDatabaseHelperStudent(Context context)  {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Create tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "MyDatabaseHelperStudent.onCreate ... ");
        // Script to create tables
        String script = "CREATE TABLE " + TABLE_STUDENT + "("
                + COLUMN_STUDENT_ID + " INTEGER PRIMARY KEY," + COLUMN_STUDENT_COURSEId + " TEXT," + COLUMN_STUDENT_NUMBERId + " TEXT,"
                + COLUMN_STUDENT_NAME + " TEXT," + COLUMN_STUDENT_SERVERId + " TEXT" + ")";
        // execute the script
        db.execSQL(script);
    }

    // Update database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.i(TAG, "MyDatabaseHelperStudent.onUpgrade ... ");

        // Drop old tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STUDENT);

        // Recreate new one
        onCreate(db);
    }

    public void addStudent(Student student)
    {
        Log.i(TAG, "MyDatabaseHelperStudent.addStudent ... " + student.getStudentIdNumber());

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_STUDENT_NUMBERId, student.getStudentIdNumber());
        values.put(COLUMN_STUDENT_COURSEId, student.getCourseServerId());
        values.put(COLUMN_STUDENT_NAME, student.getStudentName());
        values.put(COLUMN_STUDENT_SERVERId, student.getStudentServerId());

        // Insert a new line of data into the tables
        db.insert(TABLE_STUDENT, null, values);

        // Close the database connection
        db.close();
    }


    // Return all students available in the database
    public List<Student> getAllStudents()
    {
        Log.i(TAG, "MyDatabaseHelperStudent.getAllStudent ... " );

        List<Student> studentList = new ArrayList<Student>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_STUDENT;

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

                // Add to the list
                studentList.add(student);
            } while (cursor.moveToNext());
        }

        // return student list
        return studentList;
    }

    // return all students belong to a course from the course database Id
    public List<Student> getStudentWithCourse(String courseServerId)
    {
        Log.i(TAG, "MyDatabaseHelperStudent.getStudentWithCourse ...");

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

                // Add a student to the student list if the course Id and the column 1 data matching
                if ((cursor.getString(1)).equalsIgnoreCase(courseServerId))
                {
                    Log.i("EQUAL", cursor.getString(1));

                    studentList.add(student);
                }
            } while (cursor.moveToNext());
        }

        // return student list
        return studentList;
    }

    // Update student data if there is any changes
    public int updateStudent(Student student)
    {
        Log.i(TAG, "MyDatabaseHelperStudent.updateStudent ... "  + student.getStudentIdNumber());

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_STUDENT_NUMBERId, student.getStudentIdNumber());
        values.put(COLUMN_STUDENT_COURSEId, student.getCourseServerId());
        values.put(COLUMN_STUDENT_NAME, student.getStudentName());
        values.put(COLUMN_STUDENT_SERVERId, student.getStudentServerId());

        // updating row
        return db.update(TABLE_STUDENT, values, COLUMN_STUDENT_ID + " = ?",
                new String[]{String.valueOf(student.getStudentId())});
    }

    // Delete a student row from database
    public void deleteStudent(Student student) {
        Log.i(TAG, "MyDatabaseHelperStudent.deleteStudent ... " + student.getStudentIdNumber() );

        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_STUDENT, COLUMN_STUDENT_ID + " = ?",
                new String[] { String.valueOf(student.getStudentId()) });
        db.close();
    }

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
                    db_face.deleteFacesWithStudent(cursor.getString(4));
                }
            } while (cursor.moveToNext());
        }
        db.close();
        db_face.close();
    }

    // return all students belong to a course from the course database Id
    public Student getAStudentWithId(String studentServerId)
    {
        Log.i(TAG, "MyDatabaseHelperStudent.getStudentWithCourse ...");

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

                // Add a student to the student list if the course Id and the column 1 data matching
                if ((cursor.getString(4)).equalsIgnoreCase(studentServerId))
                {
                   student = student_tmp;
                }
            } while (cursor.moveToNext());
        }

        // return student list
        return student;
    }
}