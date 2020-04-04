/*
 * DetectionTask.java
 */
package com.vunguyen.vface.helper.asyncTasks;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.ImageEditor;
import com.vunguyen.vface.ui.SelfCheckActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This class implement the face detection task with server API
 */
@SuppressLint("StaticFieldLeak")
public class DetectionTask extends AsyncTask<InputStream, String, Face[]>
{
    private Context context;

    private ProgressDialog progressDialog;

    private List<Bitmap> detectedFacesList;
    private List<String> detectedDetailsList;
    private List<Face> facesList;

    private String courseServerId;
    private String request;
    private Bitmap bitmapImage;
    private boolean detected;

    private static final String REQUEST = "SELF-CHECK";

    public DetectionTask(Context context, Bitmap bitmapImage, boolean detected, String courseServerId, String request)
    {
        this.context = context;
        this.request = request;
        this.bitmapImage = bitmapImage;
        this.detectedFacesList = new ArrayList<>();
        this.detectedDetailsList = new ArrayList<>();
        this.detected = detected;
        this.courseServerId = courseServerId;

        facesList = new ArrayList<>();
        progressDialog = new ProgressDialog(this.context);
        progressDialog.setTitle("V.FACE");
        progressDialog.setCancelable(false);
    }

    @Override
    protected Face[] doInBackground(InputStream... params)
    {
        // Connect to server
        FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();

        try
        {
            publishProgress("Detecting...");
            Log.i("EXECUTE", "Detecting faces...");

            // Start detection process
            return faceServiceClient.detect(
                    params[0],  // stream of image to detect
                    true,    // return face ID
                    false,          // don't return face landmarks
                    null,           // don't return face attributes
                    "recognition_02", // new recognition model
                    "detection_02");    // new detection model
        }
        catch (Exception e)
        {
            publishProgress(e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPreExecute() {
        startProgressDialog();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        // Show the status of background detection task on screen
        duringTaskProgressDialog(values[0]);
    }

    @Override
    protected void onPostExecute(Face[] result)
    {
        if (result != null)
        {
            Log.i("EXECUTE", "Total faces detected: " + result.length);
            for (Face face : result)
            {
                try
                {
                    // generate thumbnails of faces and add to face container
                    Bitmap bitmap = ImageEditor.generateFaceThumbnail(bitmapImage, face.faceRectangle);
                    if (request.equalsIgnoreCase(REQUEST))
                        SelfCheckActivity.detectedFacesList.add(bitmap);
                    else
                        detectedFacesList.add(bitmap);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            facesList = Arrays.asList(result);

            if (result.length == 0)
            {
                this.detected = false;
                Log.i("EXECUTE", "NO FACE DETECTED!");
                progressDialog.dismiss();
                Toast.makeText(context, "No face detected.", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Log.i("EXECUTE", "FACE DETECTED!");
                this.detected = true;
                progressDialog.dismiss();
                if (request.equalsIgnoreCase(REQUEST))
                {
                    selfIdentify();
                }
                else
                    groupIdentify(); // start identify process for these detected face
            }
        }
        else
            this.detected = false;
    }

    // display the progress dialog when a task is processing
    private void startProgressDialog()
    {
        progressDialog.show();
    }

    private void duringTaskProgressDialog(String progress)
    {
        progressDialog.setMessage(progress);
    }

    // This method is to identify a student through the detected face
    private void groupIdentify()
    {
        // detected value == true means the face is detected successfully
        if (detected && courseServerId != null)
        {
            int count = 0;  // counter for each faceId set
            int totalTurn = 0;  // counter the numbers of set added to the list

            List<List<UUID>> faceIdList = new ArrayList<>(); // contain multiples faces set of 10 for identify

            // initialize the maximum number of faces can be identified at one time is 100
            for (int i = 0; i < 10; i++)
            {
                List<UUID> faceIds = new ArrayList<>();
                faceIdList.add(faceIds);
            }

            // initialize the container for identified student information
            for (int i = 0; i < facesList.size(); i++)
                this.detectedDetailsList.add("");

            for (Face face : facesList)
            {
                // add 10 faces as an element in faceIds List,
                // since Microsoft sdk limits 10 faces can be identified at one
                if (count < 10)
                {
                    faceIdList.get(totalTurn).add(face.faceId);
                    count++;
                }
                else if (count == 10)
                {
                    totalTurn++;    // one set added, move to the next set if necessary
                    count = 0;      // reset count counter
                    faceIdList.get(totalTurn).add(face.faceId);
                    count++;
                }
            }

            // Execute multiple identification tasks for each set of 10 faces
            for (int i = 0; i < totalTurn + 1; i++)
            {
                new IdentificationTask(courseServerId, i, totalTurn,
                        detectedFacesList, detectedDetailsList, this.context, "GROUP_CHECK")
                        .execute(faceIdList.get(i).toArray(new UUID[faceIdList.get(i).size()]));
            }
        }
        else
        {
            Toast.makeText(context, "No valid image detected", Toast.LENGTH_SHORT).show();
            // Not detected or person group exists.
            Log.i("EXECUTE","Please select an image and create course first.");
        }
    }

    private void selfIdentify()
    {
        // Start detection task only if the image to detect is selected.
        if (detected && courseServerId != null)
        {
            // Start a background task to identify faces in the image.
            List<UUID> faceIdList = new ArrayList<>();
            for (Face face:  facesList) {
                faceIdList.add(face.faceId);
            }

            new IdentificationTask(courseServerId, this.context,
                    "SELF_CHECK").execute(faceIdList.toArray(new UUID[faceIdList.size()]));
        }
        else
        {
            // Not detected or person group exists.
            Log.i("EXECUTE","Please select an image and create course first.");
        }
    }
}