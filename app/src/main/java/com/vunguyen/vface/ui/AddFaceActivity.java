package com.vunguyen.vface.ui;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.AddPersistedFaceResult;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.vunguyen.vface.R;
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

public class AddFaceActivity extends AppCompatActivity
{
    com.vunguyen.vface.bean.Face studentFace;

    // Background task of adding a face to student.
    private class AddFaceTask extends AsyncTask<Void, String, Boolean>
    {
        List<Integer> faceIndices;
        public AddFaceTask(List<Integer> faceIndices)
        {
            this.faceIndices = faceIndices;
        }

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
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());

                for (Integer index: faceIndices)
                {
                    FaceRectangle faceRect = faceRectList.get(index);
                    Log.i("EXECUTE","Request: Adding face to student " + studentServerId);
                    // Start the request to add face.
                    AddPersistedFaceResult result = faceServiceClient.addPersonFaceInLargePersonGroup(
                            courseServerId,
                            studentServerId_UUID,
                            imageInputStream,
                            "User data",
                            faceRect);

                    faceIdList.set(index, result.persistedFaceId);
                }
                return true;
            }
            catch (Exception e)
            {
                publishProgress(e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            setUiAfterAddingFace(result, faceIndices);
        }
    }

    // Background task of face detection.
    private class DetectionTask extends AsyncTask<InputStream, String, Face[]>
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


            detectionResult = faces;
            if (detectionResult != null)
            {
                List<Face> facesList = Arrays.asList(detectionResult);
                for (Face face : facesList)
                {
                    try
                    {
                        // Crop face thumbnail with five main landmarks drawn from original image.
                        faceThumbnails.add(ImageEditor.generateFaceThumbnail(
                                mBitmap, face.faceRectangle));

                        faceIdList.add(null);
                        faceRectList.add(face.faceRectangle);

                        faceChecked.add(true);
                    } catch (IOException e)
                    {
                        // Show the exception when generating face thumbnail fails.
                    }
                }


            }

            if (detectionResult != null)
            {
                List<Integer> faceIndices = new ArrayList<>();

                for (int i = 0; i < faceRectList.size(); ++i)
                {
                    if (faceChecked.get(i)) {
                        faceIndices.add(i);
                    }
                }

                if (faceIndices.size() > 0)
                {
                    new AddFaceTask(faceIndices).execute();
                } else {
                    finish();
                }
            }
            //setUiAfterDetection(faces, succeed);


        }
    }
    // Add Face into database and the GridView Adapter
    private void setUiAfterAddingFace(boolean succeed, List<Integer> faceIndices)
    {
        if (succeed)
        {
            String faceIds = "";
            for (Integer index : faceIndices)
            {
                String faceId = faceIdList.get(index).toString();
                faceIds += faceId + ", ";
                FileOutputStream fileOutputStream = null;
                try
                {
                    File file = new File(getApplicationContext().getFilesDir(), faceId);
                    fileOutputStream = new FileOutputStream(file);
                    faceThumbnails.get(index)
                            .compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.flush();

                    Uri uri = Uri.fromFile(file);

                    MyDatabaseHelperFace db = new MyDatabaseHelperFace(this);
                    studentFace = new com.vunguyen.vface.bean.Face(studentServerId, faceId, uri.toString());
                    db.addFace(studentFace);

                }
                catch (IOException e)
                {
                    //setInfo(e.getMessage());
                }
                finally
                {
                    if (fileOutputStream != null)
                    {
                        try
                        {
                            fileOutputStream.close();
                        }
                        catch (IOException e)
                        {
                            //setInfo(e.getMessage());
                        }
                    }
                }
            }
            Log.i("EXECUTE", "Response: Success. Face(s) " + faceIds + "added to student: " + studentServerId);
            finish();
        }
    }


    // Display the detected faces on grid view
    private void setUiAfterDetection(Face[] result, boolean succeed)
    {
        if (succeed)
        {
            /*/ Set the information about the detection result.
            if (result != null) {
                setInfo(result.length + " face"
                        + (result.length != 1 ? "s" : "") + " detected");
            } else {
                setInfo("0 face detected");
            }*/

            // Set the adapter of the ListView which contains the details of the detected faces.
            //detectionResult = result;

            // Show the detailed list of detected faces.
            //GridView gridView = findViewById(R.id.gridView_faces_to_select);
            //gridView.setAdapter(mFaceGridViewAdapter);
        }




    }

    List<UUID> faceIdList = new ArrayList<>();
    List<FaceRectangle> faceRectList = new ArrayList<>();
    List<Bitmap> faceThumbnails = new ArrayList<>();
    List<Boolean> faceChecked = new ArrayList<>();
    Face[] detectionResult;

    String courseServerId;
    String studentServerId;
    String imageUriStr;
    Bitmap mBitmap;
    //FaceGridViewAdapter mFaceGridViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face);


        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            studentServerId = bundle.getString("StudentServerId");
            Log.i("EXECUTE", "ADD FACE ACTIVITY STUDENT ID: " + studentServerId);
            courseServerId = bundle.getString("CourseServerId");
            imageUriStr = bundle.getString("ImageUriStr");
        }
    }

    // Save information in case the activity needs to reload
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putString("StudentServerId", studentServerId);
        outState.putString("CourseServerId", courseServerId);
        outState.putString("ImageUriStr", imageUriStr);
    }

    // Reload the information when the activity resumes.
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        studentServerId = savedInstanceState.getString("StudentServerId");
        courseServerId = savedInstanceState.getString("CourseServerId");
        imageUriStr = savedInstanceState.getString("ImageUriStr");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Uri imageUri = Uri.parse(imageUriStr);
        mBitmap = ImageEditor.loadSizeLimitedBitmapFromUri(
                imageUri, getContentResolver());
        if (mBitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());
            Log.i("EXECUTE","Request: Detecting " + imageUriStr);
            new DetectionTask().execute(imageInputStream);
        }
    }

    /*
    public void doneAndSave(View view) {
        if (mFaceGridViewAdapter != null)
        {
            List<Integer> faceIndices = new ArrayList<>();

            for (int i = 0; i < mFaceGridViewAdapter.faceRectList.size(); ++i)
            {
                if (mFaceGridViewAdapter.faceChecked.get(i)) {
                    faceIndices.add(i);
                }
            }

            if (faceIndices.size() > 0)
            {
                new AddFaceTask(faceIndices).execute();
            } else {
                finish();
            }
        }
    } */


}
