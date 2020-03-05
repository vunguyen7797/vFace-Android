package com.vunguyen.vface.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.vunguyen.vface.bean.Face;

import java.util.ArrayList;
import java.util.List;

public class MyDatabaseHelperFace extends SQLiteOpenHelper
{
    private static final String TAG = "SQLite";


    // version
    private static final int DATABASE_VERSION = 1;

    // Face database;
    private static final String DATABASE_NAME = "Face_Manager";

    private static final String TABLE_FACE = "Face";
    private static final String COLUMN_FACE_ID ="Face_Id";
    private static final String COLUMN_FACE_STUDENTId ="Face_StudentId";    // Id string of student on Microsoft server
    private static final String COLUMN_FACE_FACEIdSTRING = "Face_FaceStringId";
    private static final String COLUMN_FACE_URI = "Face_FaceStringUri";

    public MyDatabaseHelperFace(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Create tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("EXECUTE", "MyDatabaseHelperFace.onCreate ... ");
        // Script to create tables
        String script = "CREATE TABLE " + TABLE_FACE + "("
                + COLUMN_FACE_ID + " INTEGER PRIMARY KEY," + COLUMN_FACE_STUDENTId + " TEXT," + COLUMN_FACE_FACEIdSTRING + " TEXT,"
                + COLUMN_FACE_URI + " TEXT" + ")";
        // execute the script
        db.execSQL(script);
    }

    // Update database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

        Log.i("EXECUTE", "MyDatabaseHelperStudent.onUpgrade ... ");

        // Drop old tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FACE);

        // Recreate new one
        onCreate(db);
    }

    // Add a single face into the database
    public void addFace(Face student_face)
    {
        Log.i("EXECUTE", "MyDatabaseHelperFace.addFace ... " + student_face.getStudentFaceServerId());

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_FACE_STUDENTId, student_face.getStudentServerId());
        values.put(COLUMN_FACE_FACEIdSTRING, student_face.getStudentFaceServerId());
        values.put(COLUMN_FACE_URI, student_face.getStudentFaceUri());

        // Insert a new line of data into the tables
        db.insert(TABLE_FACE, null, values);

        // Close the database connection
        db.close();
    }


    // Return a face from the input database Id
    public Face getFace(int id)
    {
        Log.i(TAG, "MyDatabaseHelperFace.getFace ... " + id);

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_FACE, new String[] { COLUMN_FACE_ID,
                        COLUMN_FACE_STUDENTId, COLUMN_FACE_FACEIdSTRING, COLUMN_FACE_URI }, COLUMN_FACE_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Face face = new Face(Integer.parseInt(cursor.getString(0)), cursor.getString(1),
                cursor.getString(2), cursor.getString(3));
        // return face
        return face;
    }

    // Return all faces in the database
    public List<Face> getAllFace()
    {
        Log.i(TAG, "MyDatabaseHelperFace.getAllFaces ... " );

        List<Face> faceList = new ArrayList<Face>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_FACE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {   Face face = new Face();
                face.setFaceId(Integer.parseInt(cursor.getString(0)));
                face.setStudentServerId(cursor.getString(1));
                face.setStudentFaceServerId(cursor.getString(2));
                face.setStudentFaceUri(cursor.getString(3));

                // Add to the list
                faceList.add(face);
            } while (cursor.moveToNext());
        }

        // return face list
        return faceList;
    }

    // Return all faces belong to one student with student server Id
    public List<Face> getFaceWithStudent(String studentServerId)
    {
        Log.i("EXECUTE", "MyDatabaseHelperFace.getFaceWithStudent ...");

        List<Face> faceList = new ArrayList<Face>();

        String selectQuery = "SELECT * FROM " + TABLE_FACE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {
                Face face = new Face();
                face.setFaceId(Integer.parseInt(cursor.getString(0)));
                face.setStudentServerId(cursor.getString(1));
                face.setStudentFaceServerId(cursor.getString(2));
                face.setStudentFaceUri(cursor.getString(3));

                // add face to the list if the student server Id matching
                if (cursor.getString(1).equalsIgnoreCase(studentServerId))
                {
                    faceList.add(face);
                }

            } while (cursor.moveToNext());
        }

        // return face list
        return faceList;
    }

    // return all Faces server Id belong to one student
    public List<String> getAllFaceIds(String studentServerId)
    {
        Log.i("EXECUTE", "MyDatabaseHelperFace.getAllFacesId with student Id ... " );

        List<String> faceIdList = new ArrayList<String>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_FACE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {
                if (cursor.getString(1).equalsIgnoreCase(studentServerId))
                    faceIdList.add(cursor.getString(3));

                // Add to the list
            } while (cursor.moveToNext());
        }

        // return note list
        return faceIdList;
    }

    // update the face information if there is any changes
    public int updateFace(Face face)
    {
        Log.i("EXECUTE", "MyDatabaseHelperFace.updateFace ... "  + face.getStudentServerId());

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_FACE_STUDENTId, face.getStudentServerId());
        values.put(COLUMN_FACE_FACEIdSTRING, face.getStudentFaceServerId());
        values.put(COLUMN_FACE_URI, face.getStudentFaceUri());

        // updating row
        return db.update(TABLE_FACE, values, COLUMN_FACE_ID + " = ?",
                new String[]{String.valueOf(face.getFaceId())});
    }

    // Remove a face from database
    public void deleteFace(Face face)
    {
        Log.i(TAG, "MyDatabaseHelperFace.deleteFace ... " + face.getStudentServerId() );

        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FACE, COLUMN_FACE_ID + " = ?",
                new String[] { String.valueOf(face.getFaceId()) });
        db.close();
    }

    public void deleteFacesWithStudent(String studentServerId)
    {
        Log.i("EXECUTE", "MyDatabaseHelperFace.deleteFacesWithCourse ...");

        String selectQuery = "SELECT * FROM " + TABLE_FACE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {
                if ((cursor.getString(2)).equalsIgnoreCase(studentServerId))
                {
                    db.delete(TABLE_FACE, COLUMN_FACE_ID + " = ?",
                            new String[] { String.valueOf(cursor.getInt(0)) });
                }
            } while (cursor.moveToNext());
        }
        db.close();
    }
}
