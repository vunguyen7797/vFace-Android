/*
 * FrameReturn.java
 */
package com.vunguyen.vface.utilsFirebaseVision.interfaces;

import android.graphics.Bitmap;

import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.vunguyen.vface.utilsFirebaseVision.common.FrameMetadata;
import com.vunguyen.vface.utilsFirebaseVision.common.GraphicOverlay;

/**
 * Return frame of the image
 */
public interface FrameReturn
{
    void onFrame(
            Bitmap image,
            FirebaseVisionFace face,
            FrameMetadata frameMetadata,
            GraphicOverlay graphicOverlay
    );
}