/*
 * SelectImageActivity.java
 */
package com.vunguyen.vface.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.vunguyen.vface.R;

/**
 * This class implements events for taking photo and choosing photo options
 */
public class SelectImageActivity extends AppCompatActivity
{
    // Request codes
    private static final int REQUEST_TAKE_PHOTO = 0;
    private static final int REQUEST_SELECT_IMAGE_IN_ALBUM = 1;

    // The URI of photo taken with camera
    private Uri uriTakenPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_select_image);
    }

    // Save the activity state when it's going to stop.
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putParcelable("ImageUri", uriTakenPhoto);
    }

    // Recover the saved state when the activity is recreated.
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        uriTakenPhoto = savedInstanceState.getParcelable("ImageUri");
    }

    // Set event for taking photo button
    public void btnTakePhoto(View view)
    {
        Intent intent = new Intent(this, RealTimeFaceDetectActivity.class);
        startActivityForResult(intent, REQUEST_TAKE_PHOTO);
    }

    // Set event for selecting image from album
    public void selectImageInAlbum(View view)
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null)
        {
            startActivityForResult(intent, REQUEST_SELECT_IMAGE_IN_ALBUM);
        }
    }

    // Response on result of option which was just executed
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK)
                {
                    Uri imageUri = data.getData();
                    setResult(imageUri);
                }
            case REQUEST_SELECT_IMAGE_IN_ALBUM:
                if (resultCode == RESULT_OK)
                {
                    Uri imageUri;
                    if (data == null || data.getData() == null)
                    {
                        imageUri = uriTakenPhoto;
                    }
                    else
                    {
                        imageUri = data.getData();
                    }

                    setResult(imageUri);
                }
                break;
            default:
                break;
        }
    }
    // set result to respond to StudentData activity
    private void setResult(Uri imageUri)
    {
        Intent intent = new Intent();
        intent.setData(imageUri);
        setResult(RESULT_OK, intent);
        finish();
    }

}
