/*
 * DeleteFaceTask.java
 */
package com.vunguyen.vface.helper.asyncTasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.vunguyen.vface.helper.ApiConnector;

import java.util.UUID;

/**
 * This class is a background task to delete a face from a person (Student)
 * on server
 */
public class DeleteFaceTask extends AsyncTask<String, String, String>
{
    final private String courseServerId;
    final private UUID studentServerId;
    @SuppressLint("StaticFieldLeak")
    final private Context context;

    public DeleteFaceTask(String courseServerId, String studentServerId, Context context)
    {
        this.courseServerId = courseServerId;
        this.studentServerId = UUID.fromString(studentServerId);
        this.context = context;
    }

    @Override
    protected String doInBackground(String... params)
    {
        // Connect to server
        FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
        try
        {
            Log.i("EXECUTE","Request: Deleting face " + params[0]);

            UUID faceId = UUID.fromString(params[0]);
            faceServiceClient.deletePersonFaceInLargePersonGroup(courseServerId, studentServerId, faceId);
            return params[0];
        }
        catch (Exception e)
        {
            Log.i("EXECUTE","Error Delete face: " + (e.getMessage()));
            Toast.makeText(this.context,"Cannot delete face", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result)
    {
        if (result != null)
        {
            Log.i("EXECUTE","Face " + result + " successfully deleted");
            Toast.makeText(this.context,"Face deleted successfully", Toast.LENGTH_SHORT).show();
        }
    }
}