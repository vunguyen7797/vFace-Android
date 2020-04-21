/*
 * AddFaceToStudent.java
 */
package com.vunguyen.vface.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.AddPersistedFaceResult;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.ImageEditor;
import com.vunguyen.vface.helper.ProgressDialogCustom;
import com.vunguyen.vface.helper.StorageHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
    private String studentNumberId;
    private String studentName;
    private String imageUri;

    private Context context;
    private StudentDataActivity activity;
    private DatabaseReference mDatabase_Face;
    private DatabaseReference mDatabase_saved;
    private DatabaseReference mDatabase_student;
    private ProgressDialog progressDialog;
    private ProgressDialogCustom progressDialogCustom;

    private int numberOfFaces;

    AddFaceToStudent(String studentNumberId, String studentName, String uri, Bitmap bitmapImage,
                     String studentServerId, String courseServerId,
                     Context context, StudentDataActivity activity, String account, int numberOfFaces)
    {
        this.studentNumberId = studentNumberId;
        this.bitmapImage = bitmapImage;
        this.studentServerId = studentServerId;
        this.courseServerId = courseServerId;
        this.context = context;
        this.activity = activity;
        this.numberOfFaces = numberOfFaces;
        this.imageUri = uri;
        this.studentName = studentName;
        this.mDatabase_Face = FirebaseDatabase.getInstance().getReference().child(account).child("face");
        this.mDatabase_saved = FirebaseDatabase.getInstance().getReference().child(account).child("account_storage");
        this.mDatabase_student = FirebaseDatabase.getInstance().getReference().child(account).child("student");
        progressDialog = new ProgressDialog(this.context);
        progressDialog.setTitle("V.FACE");
        progressDialog.setCancelable(false);
    }

    AddFaceToStudent(String studentNumberId, String studentName, String uri, String studentServerId, String courseServerId,
                     Context context, StudentDataActivity activity, String account, int numberOfFaces)
    {
        this.studentNumberId = studentNumberId;
        this.studentServerId = studentServerId;
        this.courseServerId = courseServerId;
        this.context = context;
        this.activity = activity;
        this.imageUri = uri;
        this.studentName = studentName;
        this.numberOfFaces = numberOfFaces;
        this.mDatabase_student = FirebaseDatabase.getInstance().getReference().child(account).child("student");
        this.mDatabase_saved = FirebaseDatabase.getInstance().getReference().child(account).child("account_storage");
        this.mDatabase_Face = FirebaseDatabase.getInstance().getReference().child(account).child("face");
        progressDialog = new ProgressDialog(this.context);
        progressDialog.setTitle("V.FACE");
        progressDialog.setCancelable(false);
    }

    // This method to detect face from a bitmap image,
    // then add it to the student's face database.
    void addFaceToPersonAfterDetect()
    {
        if (bitmapImage != null)
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());
            activity.onResume();
            progressDialogCustom = new ProgressDialogCustom(activity);
            progressDialogCustom.startProgressDialog("Adding face...");
            new DetectionTask().execute(imageInputStream);
        }
        else
        {
            activity.onResume();
            Toast.makeText(context, "No input image.", Toast.LENGTH_SHORT).show();
            Log.i("EXECUTE", "Error: Bitmap Image is null");
        }
    }

    void addFaceToPersonDirect()
    {
        if (imageUri != null)
        {
            new AddFaceTask(false, imageUri).execute();
        }
        else
        {
            activity.onResume();
            Toast.makeText(context, "No input image.", Toast.LENGTH_SHORT).show();
            Log.i("EXECUTE", "Error: Uri image is null");
        }
    }

    // This method to save face into database if detected
    private void savingData(Boolean result, int request)
    {
        if (result)
        {
            String faceIdStr = faceId.toString();
            if (request == 0)
            {
                StorageHelper.uploadToFireBaseStorage(faceThumbnail, faceIdStr+".png"
                        , "face", uriPhoto -> {
                            Log.i("EXECUTE", "Student number ID FACE from request 0: " + studentNumberId);
                            com.vunguyen.vface.bean.Face studentFace = new com.vunguyen.vface.bean.Face(courseServerId, studentNumberId,
                                    studentServerId, faceIdStr, uriPhoto.toString());
                            // add face to database and update number of faces of student
                            mDatabase_Face.child(faceIdStr).setValue(studentFace);
                            StudentDataActivity.numberOfFaces = numberOfFaces++;
                            addFaceToStorage(imageUri, faceIdStr, studentFace);
                            Log.i("EXECUTE", "Response: Success. Face(s) " + faceIdStr
                                    + "added to student: " + studentServerId);
                            // Resume the StudentData activity after saving
                            activity.onResume();
                            progressDialogCustom.dismissDialog();
                            Toast.makeText(context, "Face detected successfully", Toast.LENGTH_SHORT).show();
                        });
            }
            else    // import mode
            {
                Log.i("EXECUTE", "Student number ID FACE: " + studentNumberId);
                com.vunguyen.vface.bean.Face studentFace = new com.vunguyen.vface.bean.Face(courseServerId, studentNumberId,
                        studentServerId, faceIdStr, imageUri);
                mDatabase_Face.child(faceIdStr).setValue(studentFace);
                StudentDataActivity.numberOfFaces = numberOfFaces;
                addFaceToStorage(imageUri, faceIdStr, studentFace);
                Log.i("EXECUTE", "Response: Success. Face(s) " + faceIdStr
                        + "added to student: " + studentServerId);
                // Resume the StudentData activity after saving
                activity.onResume();
            }
        }
        else
        {
            activity.onResume();
            progressDialogCustom.dismissDialog();
            Toast.makeText(context, "An error occurred. No face is saved.", Toast.LENGTH_LONG).show();
        }
    }

    private void addFaceToStorage(String imageUri, String faceIdStr, com.vunguyen.vface.bean.Face studentFace)
    {
        mDatabase_saved.child("face").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                boolean existed = false;
                for (DataSnapshot dsp : dataSnapshot.getChildren())
                {
                    com.vunguyen.vface.bean.Face face = dsp.getValue(com.vunguyen.vface.bean.Face.class);
                    assert face != null;
                    if (face.getStudentFaceUri().equalsIgnoreCase(imageUri))
                        existed = true;
                }
                if (!existed)
                {
                    mDatabase_saved.child("face").child(faceIdStr).setValue(studentFace);
                }
                mDatabase_saved.child("face").removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * This class contains background methods to work with server
     * for detecting face in the image.
     */
    @SuppressLint("StaticFieldLeak")
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
                Log.i("EXECUTE", Objects.requireNonNull(e.getMessage()));
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
                            Log.i("EXECUTE", Objects.requireNonNull(e.getMessage())); // generating thumbnail failed.
                        }
                    }
                    // add face to server
                    new AddFaceTask(true).execute();
                }
                else
                {
                    // if more than one face in the image, return StudentData activity
                    activity.onResume();
                    progressDialogCustom.dismissDialog();
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
        boolean detect;
        String uriImage;
        AddFaceTask(boolean detect){this.detect = detect;}
        AddFaceTask(boolean detect, String uriImage)
        {
            this.detect = detect;
            this.uriImage = uriImage;
        }
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

                // add face via stream
                if (bitmapImage != null)
                {
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
                }
                else    // add face via URI
                {
                    FaceRectangle faceRectangle = faceRect;
                    Log.i("EXECUTE", "Request: Adding face to student " + studentServerId);
                    // Start the request to add face.
                    AddPersistedFaceResult result = faceServiceClient.addPersonFaceInLargePersonGroup(
                            courseServerId,
                            studentServerId_UUID,
                            uriImage,
                            "User data",
                            faceRectangle,
                            "detection_02");

                    faceId = result.persistedFaceId;
                }
                Log.i("EXECUTE", "Face Id: " + faceId);
                return true;
            }
            catch (Exception e)
            {
                Log.i("EXECUTE", Objects.requireNonNull(e.getMessage()));
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            if (detect)
                // adding face to server and adding photo to Firebase Storage
            {
                savingData(result, 0);
            }

            else
                // adding face to server only
                savingData(result, 1);
        }
    }
}