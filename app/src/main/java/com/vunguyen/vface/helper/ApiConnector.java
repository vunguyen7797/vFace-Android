/*
 * ApiConnector.java
 */
package com.vunguyen.vface.helper;

import android.app.Application;
import android.content.Context;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;


/**
 * This class is to connect the app with Microsoft Face API service via the endpoint and subscription key
 */
public class ApiConnector extends Application
{
    private static FaceServiceClient sFaceServiceClient;

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        // Connect to Face Service Api
        sFaceServiceClient = new FaceServiceRestClient("", "");
    }

    public static FaceServiceClient getFaceServiceClient()
    {
        return sFaceServiceClient;
    }
}
