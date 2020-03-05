/*
 * ApiConnector.java
 */
package com.vunguyen.vface.helper;

import android.app.Application;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;

/**
 * This class is to connect the app with Microsoft Face API service via the endpoint and subscription key
 */
public class ApiConnector extends Application
{
    private static FaceServiceClient sFaceServiceClient;

    @Override
    public void onCreate()
    {
        super.onCreate();
        // Connect to Face Service Api
        sFaceServiceClient = new FaceServiceRestClient("https://vface2.cognitiveservices.azure.com/face/v1.0", "d4265de0ba6f464d91758400376e36ef");
    }

    public static FaceServiceClient getFaceServiceClient()
    {
        return sFaceServiceClient;
    }
}
