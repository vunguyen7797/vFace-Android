package com.vunguyen.vface.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.AddPersistedFaceResult;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.ImageEditor;
import com.vunguyen.vface.helper.MyDatabaseHelperFace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

class AddFaceToStudent
{
    private Bitmap bitmapImage;
    private Bitmap faceThumbnail;
    private FaceRectangle faceRect;
    private UUID faceId;
    private String studentServerId;
    private String courseServerId;
    private MyDatabaseHelperFace db_face;
    private Context context;
    private StudentDataActivity activity;

    AddFaceToStudent(Bitmap bitmapImage, String studentServerId, String courseServerId,
                     MyDatabaseHelperFace db_face, Context context, StudentDataActivity activity)
    {
        this.bitmapImage = bitmapImage;
        this.studentServerId = studentServerId;
        this.courseServerId = courseServerId;
        this.db_face = db_face;
        this.context = context;
        this.activity = activity;
    }


    void addFaceToPerson()
    {
        if (bitmapImage != null)
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());
            new DetectionTask().execute(imageInputStream);
        }

    }



    // Background task of face detection.
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
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        null);
            }
            catch (Exception e)
            {
                succeed = false;
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Face[] faces)
        {
            if (succeed)
            {
                Log.i("EXECUTE","Response: Success. Detected " + (faces == null ? 0 : faces.length)
                        + " Face(s)");
            }

            // Show the result on screen when detection is done.

            if (faces != null && faces.length == 1)
            {
                List<Face> facesList = Arrays.asList(faces);

                for (Face face : facesList)
                {
                    try
                    {
                        // Crop face thumbnail with five main landmarks drawn from original image.
                        faceThumbnail = ImageEditor.generateFaceThumbnail(
                                bitmapImage, face.faceRectangle);

                        faceId = null;
                        faceRect = face.faceRectangle;

                        //faceChecked.add(true);
                    } catch (IOException e)
                    {
                        // Show the exception when generating face thumbnail fails.
                    }
                }

                if (faceThumbnail != null)
                {
                    new AddFaceTask().execute();
                }

            }

        }
    }

    // Background task of adding a face to student.
    class AddFaceTask extends AsyncTask<Void, String, Boolean>
    {
        @Override
        protected Boolean doInBackground(Void... params)
        {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                Log.i("EXECUTE","Adding face starts...");
                // Parse student server Id from String back to UUID
                UUID studentServerId_UUID = UUID.fromString(studentServerId);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());

                FaceRectangle faceRectangle = faceRect;
                Log.i("EXECUTE","Request: Adding face to student " + studentServerId);
                // Start the request to add face.
                AddPersistedFaceResult result = faceServiceClient.addPersonFaceInLargePersonGroup(
                            courseServerId,
                            studentServerId_UUID,
                            imageInputStream,
                            "User data",
                            faceRectangle);

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

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private void savingData(Boolean result) {
        if (result)
        {

            String faceIdStr = faceId.toString();

            Uri uri = getImageUri(context, faceThumbnail);
                com.vunguyen.vface.bean.Face studentFace = new com.vunguyen.vface.bean.Face(studentServerId, faceIdStr, uri.toString());

                db_face.addFace(studentFace);
                Log.i("EXECUTE", "Response: Success. Face(s) " + faceIdStr + "added to student: " + studentServerId);

                activity.onResume();

        }
    }

}
