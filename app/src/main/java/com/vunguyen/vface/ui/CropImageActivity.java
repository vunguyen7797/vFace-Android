/*
 * CropImageActivity.java
 */
package com.vunguyen.vface.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.theartofdev.edmodo.cropper.CropImageView;
import com.vunguyen.vface.R;
import com.vunguyen.vface.helper.ImageEditor;
import com.vunguyen.vface.helper.LocaleHelper;
import com.vunguyen.vface.helper.StorageHelper;
import com.vunguyen.vface.helper.UriPhoto;

import java.io.IOException;

/**
 * This class works as a window to crop the image before assigning
 * the photo to image view
 */
public class CropImageActivity extends AppCompatActivity
{
    CropImageView ivCrop;
    String account;
    Uri uri;

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);

        account = getIntent().getStringExtra("ACCOUNT");
        uri = getIntent().getData();    // get URI of the picked image from ManageAccount Activity

        initializeCropImage();
    }

    // This method is to initialize the crop image view with the need-to-crop image
    private  void initializeCropImage()
    {
        ivCrop = findViewById(R.id.ivCrop);
        ivCrop.setCropShape(CropImageView.CropShape.OVAL);      // set shape of crop view
        ivCrop.setAutoZoomEnabled(true);                        // image auto zoom
        ivCrop.setAspectRatio(1,1);     // ratio square
        ivCrop.setFixedAspectRatio(true);                        // ratio will keep the same
        try
        {
            Bitmap bitmap = ImageEditor.handlePhotoAndRotationBitmap(getApplicationContext(), uri);
            ivCrop.setImageBitmap(bitmap);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // Button Crop event
    public void btnCrop(View view)
    {
        StorageHelper.uploadToFireBaseStorage(
                new UriPhoto()
                {
                                                  @Override
                                                  public void getUriPhoto(Uri uriPhoto)
                                                  {
                                                      Intent intent = new Intent(CropImageActivity.this, ManageAccountActivity.class);
                                                      intent.putExtra("ACCOUNT", account);
                                                      intent.setData(uriPhoto);
                                                      startActivity(intent);
                                                  }
                                              }, ivCrop.getCroppedImage(),
                "profile_photo" + account,
                CropImageActivity.this, account, "profile_photo");


    }

    // Back arrow button
    public void btnBackArrow(View view)
    {
        onBackPressed();
    }

    // go back to previous activity
    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(CropImageActivity.this, ManageAccountActivity.class);
        goToActivity(intent);
    }

    // go to new another activity
    private void goToActivity(Intent intent)
    {
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
        finish();
    }

    public void btnBackClick(View view)
    {
        onBackPressed();
    }
}
