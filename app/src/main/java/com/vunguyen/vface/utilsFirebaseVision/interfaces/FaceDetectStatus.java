/*
 * FaceDetectStatus.java
 */
package com.vunguyen.vface.utilsFirebaseVision.interfaces;

import com.vunguyen.vface.utilsFirebaseVision.models.RectModel;

/**
 * Interfaces of status indicator for face detection
 */
public interface FaceDetectStatus
{
    void onFaceLocated(RectModel rectModel);
    void onFaceNotLocated() ;
}
