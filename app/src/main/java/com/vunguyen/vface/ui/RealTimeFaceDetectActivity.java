/*
 * RealTimeFaceDetectActivity.java
 */
package com.vunguyen.vface.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.vunguyen.vface.R;
import com.vunguyen.vface.helper.ImageEditor;
import com.vunguyen.vface.helper.StorageHelper;
import com.vunguyen.vface.utilsFirebaseVision.base.BaseActivity;
import com.vunguyen.vface.utilsFirebaseVision.base.PermissionProcessors;
import com.vunguyen.vface.utilsFirebaseVision.common.CameraSource;
import com.vunguyen.vface.utilsFirebaseVision.common.CameraSourcePreview;
import com.vunguyen.vface.utilsFirebaseVision.common.FrameMetadata;
import com.vunguyen.vface.utilsFirebaseVision.common.GraphicOverlay;
import com.vunguyen.vface.utilsFirebaseVision.interfaces.FaceDetectStatus;
import com.vunguyen.vface.utilsFirebaseVision.interfaces.FrameReturn;
import com.vunguyen.vface.utilsFirebaseVision.models.RectModel;
import com.vunguyen.vface.utilsFirebaseVision.visions.FaceDetectionProcessor;

import java.io.IOException;

/**
 * This class will open the camera and automatically detect the face to fit the quality standard
 * for later identification task.
 * When face is put into the frame, it will be captured automatically and added to student data
 */
public class RealTimeFaceDetectActivity extends BaseActivity
    implements ActivityCompat.OnRequestPermissionsResultCallback, FrameReturn, FaceDetectStatus
{
    private static final String FACE_DETECTION = "Face Detection";
    Bitmap originalImage = null;
    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private ImageView ivFaceFrame;
    private Bitmap croppedImage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_face_detect);

        preview = findViewById(R.id.cameraPreview);

        ivFaceFrame = findViewById(R.id.faceFrame);
        graphicOverlay = findViewById(R.id.graphicFaceOverlay);

        if (PermissionProcessors.allPermissionsAccepted(this))
        {
            createCameraSource();
        }
        else
        {
            PermissionProcessors.getRuntimePermissions(this);
        }


    }

    // This activity will not use camera directly from the phone
    // But using it a source to make a separate camera "app" using
    // front camera only
    private void createCameraSource()
    {
        if (cameraSource == null)
        {
            cameraSource = new CameraSource(this, graphicOverlay);
        }
        try
        {
            FaceDetectionProcessor processor = new FaceDetectionProcessor(getResources());
            processor.frameHandler = this;
            processor.faceDetectStatus = this;
            cameraSource.setMachineLearningFrameProcessor(processor);
        }
        catch (Exception e)
        {
            Log.e("EXECUTE", "Cannot create image processor: " + FACE_DETECTION, e);
            Toast.makeText(
                    getApplicationContext(),
                    "Cannot create image processor: " + e.getMessage(),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    // start the camera source to take photo
    private void startCameraSource()
    {
        if (cameraSource != null)
        {
            try
            {
                preview.start(cameraSource, graphicOverlay);
            }
            catch (IOException e)
            {
                Log.e("EXECUTE", "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        preview.stop();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (cameraSource != null)
        {
            cameraSource.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (PermissionProcessors.allPermissionsAccepted(this))
        {
            createCameraSource();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Process the image when the face is put in the frame
    @Override
    public void onFaceLocated(RectModel rectModel)
    {
        // When face is recognized in the frame, face frame turns to red
        ivFaceFrame.setColorFilter(ContextCompat.getColor(this, R.color.red));

        // processing of cropping the image from the preview
        float left = (float) (originalImage.getWidth() * 0.2);
        float newWidth = (float) (originalImage.getWidth() * 0.6);

        float top = (float) (originalImage.getHeight() * 0.2);
        float newHeight = (float) (originalImage.getHeight() * 0.6);
        croppedImage =
                Bitmap.createBitmap(originalImage,
                        ((int) (left)),
                        (int) (top),
                        ((int) (newWidth)),
                        (int) (newHeight));

        // When image is cropped , respond back to the SelectImage activity for other tasks
        if (croppedImage != null)
        {
            Uri uriImage = StorageHelper.saveToInternalStorageUri(croppedImage, "image.png", this);
            Intent intent = new Intent();
            intent.setData(uriImage);
            setResult(RESULT_OK, intent);
            finish();
        }
        else
            Log.i("EXECUTE", "Failed Cropping Image");
    }

    // When face is not located in the frame
    @Override
    public void onFaceNotLocated()
    {
        // set the frame as black
        ivFaceFrame.setColorFilter(ContextCompat.getColor(this, R.color.black));
    }

    @Override
    public void onFrame(Bitmap image, FirebaseVisionFace face, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay)
    {
        originalImage = image;
    }
}
