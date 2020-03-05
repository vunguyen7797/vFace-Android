package com.vunguyen.vface.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.vunguyen.vface.R;
import com.vunguyen.vface.utils.base.BaseActivity;
import com.vunguyen.vface.utils.base.Cons;
import com.vunguyen.vface.utils.base.PublicMethods;
import com.vunguyen.vface.utils.common.CameraSource;
import com.vunguyen.vface.utils.common.CameraSourcePreview;
import com.vunguyen.vface.utils.common.FrameMetadata;
import com.vunguyen.vface.utils.common.GraphicOverlay;
import com.vunguyen.vface.utils.interfaces.FaceDetectStatus;
import com.vunguyen.vface.utils.interfaces.FrameReturn;
import com.vunguyen.vface.utils.models.RectModel;
import com.vunguyen.vface.utils.visions.FaceDetectionProcessor;

import java.io.IOException;

import static com.vunguyen.vface.utils.base.Cons.IMG_EXTRA_KEY;

public class RealTimeFaceDetectActivity extends BaseActivity
    implements ActivityCompat.OnRequestPermissionsResultCallback, FrameReturn, FaceDetectStatus
{
    private static final String FACE_DETECTION = "Face Detection";
    private static final String TAG = "MLKitTAG";

    Bitmap originalImage = null;
    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private ImageView ivFaceFrame;
    private ImageView ivTest;
    private Button btnTakePhoto;
    private Bitmap croppedImage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_face_detect);

        preview = findViewById(R.id.cameraPreview);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        ivFaceFrame = findViewById(R.id.faceFrame);
        graphicOverlay = findViewById(R.id.graphicFaceOverlay);

        if (PublicMethods.allPermissionsGranted(this))
        {
            createCameraSource();
        }
        else
        {
            PublicMethods.getRuntimePermissions(this);
        }

        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto(v);
            }
        });

    }

    private void createCameraSource()
    {
        if (cameraSource == null)
        {
            cameraSource = new CameraSource(this, graphicOverlay);
        }
        try
        {
            FaceDetectionProcessor processor = new FaceDetectionProcessor(getResources());
            processor.frameHandler = (FrameReturn) this;
            processor.faceDetectStatus = (FaceDetectStatus) this;
            cameraSource.setMachineLearningFrameProcessor(processor);
        }
        catch (Exception e)
        {
            Log.e(TAG, "Can not create image processor: " + FACE_DETECTION, e);
            Toast.makeText(
                    getApplicationContext(),
                    "Can not create image processor: " + e.getMessage(),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void startCameraSource()
    {
        if (cameraSource != null)
        {
            try
            {
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PublicMethods.allPermissionsGranted(this)) {
            createCameraSource();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onFaceLocated(RectModel rectModel)
    {
        ivFaceFrame.setColorFilter(ContextCompat.getColor(this, R.color.red));
        btnTakePhoto.setEnabled(true);

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
       // test.setImageBitmap(croppedImage);
        if (croppedImage != null) {
            String path = PublicMethods.saveToInternalStorage(croppedImage, Cons.IMG_FILE, mActivity);
            Uri imageUri = Uri.parse(path);
            Intent intent = new Intent();
            intent.putExtra("PATH", path);
            setResult(RESULT_OK, intent);
            finish();
            //startActivity(new Intent(mActivity, PhotoViewerActivity.class)
            //   .putExtra(IMG_EXTRA_KEY, path));}
        }
    }


    private void takePhoto(View v) {
        if (croppedImage != null) {

            String path = PublicMethods.saveToInternalStorage(croppedImage, Cons.IMG_FILE, mActivity);
            startActivity(new Intent(mActivity, PhotoViewerActivity.class)
                   .putExtra(IMG_EXTRA_KEY, path));
        }
    }



    @Override
    public void onFaceNotLocated() {
        ivFaceFrame.setColorFilter(ContextCompat.getColor(this, R.color.black));
        btnTakePhoto.setEnabled(false);
    }

    @Override
    public void onFrame(Bitmap image, FirebaseVisionFace face, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay) {
        originalImage = image;
    }
}
