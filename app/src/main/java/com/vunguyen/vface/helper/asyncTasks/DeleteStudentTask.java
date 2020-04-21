package com.vunguyen.vface.helper.asyncTasks;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.vunguyen.vface.helper.ApiConnector;

import java.util.UUID;

/**
 * This class is a background task to delete a student and its information on server
 */
public class DeleteStudentTask extends AsyncTask<String, String, String>
{
    private String courseServerId;
    public DeleteStudentTask(String courseServerId)
    {
        this.courseServerId = courseServerId;
    }

    @Override
    protected String doInBackground(String... params)
    {
        // Connect to server
        FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
        try
        {
            Log.i("EXECUTE","Request: Deleting student " + params[0]);
            UUID studentServerId = UUID.fromString(params[0]);
            faceServiceClient.deletePersonInLargePersonGroup(courseServerId, studentServerId);
            return params[0];
        }
        catch (Exception e)
        {
            Log.i("EXECUTE","ERROR DELETE STUDENT FROM SERVER: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result)
    {
        if (result != null)
        {
            Log.i("EXECUTE", "Response: Success. Deleting student " + result + " succeed");
        }
    }
}