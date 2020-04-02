/*
 * ApiConnector.java
 */
package com.vunguyen.vface.helper;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.vunguyen.vface.R;
import com.vunguyen.vface.ui.GeneralSettingActivity;

import io.paperdb.Paper;

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
        sFaceServiceClient = new FaceServiceRestClient("https://vface-3.cognitiveservices.azure.com/face/v1.0", "16898c9462714802b188f4c6b7a51d17");
    }

    public static FaceServiceClient getFaceServiceClient()
    {
        return sFaceServiceClient;
    }
}
