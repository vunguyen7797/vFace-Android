/*
 * AddCourseTask.java
 */
package com.vunguyen.vface.helper.asyncTasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.vunguyen.vface.helper.ApiConnector;

/**
 * This class implements background task to create a course
 * as a large person group on Microsoft Face API server.
 */
public class AddCourseTask extends AsyncTask<String, String, String>
{
    private ProgressDialog progressDialog;

    public AddCourseTask(Context context)
    {
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("V.FACE");
        progressDialog.setCancelable(false);
    }

    @Override
    protected String doInBackground(String... params)
    {
        // Get an instance of face service client.
        FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
        try
        {
            publishProgress("Syncing with server to add course...");
            Log.i("EXECUTE", "Syncing with server to add course");

            // Start creating a course as a person group in server.
            faceServiceClient.createLargePersonGroup(params[0], "Name", "User Data",
                    "recognition_02");
            return params[0];
        }
        catch (Exception e)
        {
            Log.i("EXECUTE", "Errors: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPreExecute() {
        startProgressDialog();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        duringTaskProgressDialog(values[0]);
    }

    @Override
    protected void onPostExecute(String result)
    {
        if (result != null)
        {
            progressDialog.dismiss();
            Log.i("EXECUTE", "Course " + result + " created successfully on server.");
        }
        else
        {
            Log.i("EXECUTE", "Response: Course is not created on server.");
        }
    }

    private void startProgressDialog()
    {
        progressDialog.show();
    }

    private void duringTaskProgressDialog(String progress)
    {
        progressDialog.setMessage(progress);
    }
}