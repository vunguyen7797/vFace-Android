/*
 * AddFaceToStudent.java
 */
package com.vunguyen.vface.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DatabaseReference;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.AddPersistedFaceResult;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.ImageEditor;
import com.vunguyen.vface.helper.StorageHelper;
import com.vunguyen.vface.helper.callbackInterfaces.UriPhotoInterface;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * This class contains methods to detect a face, adding face to student on
 * server and local database.
 */
class AddFaceToStudent extends ActivityCompat
{
    private Bitmap bitmapImage;
    private Bitmap faceThumbnail;

    private FaceRectangle faceRect;
    private UUID faceId;

    private String studentServerId;
    private String courseServerId;

    private Context context;
    private StudentDataActivity activity;
    private String account;
    private DatabaseReference mDatabase_Face;

    AddFaceToStudent(Bitmap bitmapImage, String studentServerId, String courseServerId,
                     DatabaseReference mDatabase_Face, Context context, StudentDataActivity activity, String account)
    {
        this.bitmapImage = bitmapImage;
        this.studentServerId = studentServerId;
        this.courseServerId = courseServerId;
        this.mDatabase_Face = mDatabase_Face;
        this.context = context;
        this.activity = activity;
        this.account = account;
    }

    // This method to detect face from a bitmap image,
    // then add it to the student's face database.
    void addFaceToPerson()
    {
        if (bitmapImage != null)
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());
            new DetectionTask().execute(imageInputStream);
        }
        else
        {
            activity.onResume();
            Toast.makeText(context, "No input image.", Toast.LENGTH_SHORT).show();
            Log.i("EXECUTE", "Error: Bitmap Image is null");
        }
    }

    // This method to save face into database if detected
    private void savingData(Boolean result)
    {
        if (result)
        {
            String faceIdStr = faceId.toString();
           // Uri uri = StorageHelper.saveToInternalStorageUri(faceThumbnail, faceIdStr+".png", context);

            StorageHelper.uploadToFireBaseStorage(faceThumbnail, faceIdStr+".png", context, account, "face", new UriPhotoInterface() {
                @Override
                public void getUriPhoto(Uri uriPhoto)
                {
                    com.vunguyen.vface.bean.Face studentFace = new com.vunguyen.vface.bean.Face(studentServerId, faceIdStr, uriPhoto.toString());
                    mDatabase_Face.child(faceIdStr).setValue(studentFace);

                    // saving face URI to face database of a student
                    //db_face.addFace(studentFace);
                    Log.i("EXECUTE", "Response: Success. Face(s) " + faceIdStr + "added to student: " + studentServerId);

                    // Resume the StudentData activity after saving
                    activity.onResume();
                    Toast.makeText(context, "Face detected successfully.", Toast.LENGTH_SHORT).show();
                }
            });
        }
        else
        {
            activity.onResume();
            Toast.makeText(context, "An error occurred. No face is saved.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * This class contains background methods to work with server
     * for detecting face in the image.
     */
    class DetectionTask extends AsyncTask<InputStream, String, Face[]>
    {
        private boolean succeed = true;

        @Override
        protected Face[] doInBackground(InputStream... params)
        {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                Log.i("EXECUTE", "Detecting face...");
                // Start detection.
                return faceServiceClient.detect(
                        params[0],       // Face image input from a stream
                        true,        // Return face ID
                        false,      // Do not return face landmarks
                        null, "recognition_02", "detection_02");
            }
            catch (Exception e)
            {
                succeed = false;
                Log.i("EXECUTE", e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Face[] faces)
        {
            if (succeed)
            {
                // Only allow one face detected at one time for each student
                if (faces != null && faces.length == 1)
                {
                    Log.i("EXECUTE", "Response: Face detected.  " + faces.length);
                    List<Face> facesList = Arrays.asList(faces); // convert the result into a list

                    for (Face face : facesList)
                    {
                        try
                        {
                            // Crop face thumbnail with five main landmarks drawn from original image.
                            faceThumbnail = ImageEditor.generateFaceThumbnail(
                                    bitmapImage, face.faceRectangle);

                            faceId = null;
                            faceRect = face.faceRectangle;
                        }
                        catch (IOException e)
                        {
                            Log.i("EXECUTE", e.getMessage()); // generating thumbnail failed.
                        }
                    }

                    // add face to server
                    new AddFaceTask().execute();
                }
                else
                {
                    // if more than one face in the image, return StudentData activity
                    activity.onResume();
                    if (faces!= null && faces.length > 1)
                        Toast.makeText(context, "Image contains more than 1 face." +
                            "\nChoose a photo that contain only 1 face or scan your face.", Toast.LENGTH_LONG).show();
                    else if (faces != null)
                        Toast.makeText(context, "No face detected.", Toast.LENGTH_LONG).show();

                }
            }
            else
            {
                Log.i("EXECUTE", "Response: Detection failed.");
                activity.onResume();
                Toast.makeText(context, "Detection failed.", Toast.LENGTH_LONG).show();
            }

        }
    }

    // Background task of adding a face to student.

    /**
     * This class contains background methods to work with server
     * for adding a face to the Microsoft server.
     */
    @SuppressLint("StaticFieldLeak")
    public class AddFaceTask extends AsyncTask<Void, String, Boolean>
    {
        @Override
        protected Boolean doInBackground(Void... params)
        {
            // Get an instance of face service client to connect to server
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                Log.i("EXECUTE", "Adding face starts...");

                // Parse student server Id from String back to UUID
                UUID studentServerId_UUID = UUID.fromString(studentServerId);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());

                FaceRectangle faceRectangle = faceRect;
                Log.i("EXECUTE", "Request: Adding face to student " + studentServerId);

                // Start the request to add face.
                AddPersistedFaceResult result = faceServiceClient.addPersonFaceInLargePersonGroup(
                        courseServerId,
                        studentServerId_UUID,
                        imageInputStream,
                        "User data",
                        faceRectangle,
                        "detection_02");

                faceId = result.persistedFaceId;
                Log.i("EXECUTE", "Face Id: " + faceId);
                return true;
            }
            catch (Exception e)
            {
                Log.i("EXECUTE", e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            savingData(result);
        }
    }
}

