package com.vunguyen.vface.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.vunguyen.vface.R;
import com.vunguyen.vface.utils.base.PublicMethods;

import java.io.ByteArrayOutputStream;

import static com.vunguyen.vface.utils.base.Cons.IMG_FILE;

public class SelectImageActivity extends AppCompatActivity
{
    // Flag to indicate the request of the next task to be performed
    private static final int REQUEST_TAKE_PHOTO = 0;
    private static final int REQUEST_SELECT_IMAGE_IN_ALBUM = 1;
    private static final int PERMISSION_REQUEST_EXTERNAL_STORAGE = 111;

    // The URI of photo taken with camera
    private Uri mUriPhotoTaken;

    // When the activity is created, set all the member variables to initial state.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_image);
    }

    // Save the activity state when it's going to stop.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("ImageUri", mUriPhotoTaken);
    }

    // Recover the saved state when the activity is recreated.
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        mUriPhotoTaken = savedInstanceState.getParcelable("ImageUri");
    }

    // Deal with the result of selection of the photos and faces.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK)
                {
                    String path = data.getStringExtra("PATH");
                    Bitmap imageBitmap = PublicMethods.getBitmapByPath(path, IMG_FILE);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    String newPath = MediaStore.Images.Media.insertImage(getContentResolver(), imageBitmap, "Photo", null);
                    Uri imageUri = Uri.parse(newPath);
                    Intent intent = new Intent();
                    intent.setData(imageUri);
                    setResult(RESULT_OK, intent);
                    finish();
                    /*
                    Log.i("EXECUTE", "Taking photo task done");
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    String path = MediaStore.Images.Media.insertImage(getContentResolver(), imageBitmap, "Photo", null);
                    Uri imageUri = Uri.parse(path);
                    Log.i("EXECUTE", "URI: " + imageUri);
                    Intent intent = new Intent();
                    intent.setData(imageUri);
                    setResult(RESULT_OK, intent);
                    finish(); */
                }
            case REQUEST_SELECT_IMAGE_IN_ALBUM:
                if (resultCode == RESULT_OK)
                {
                    Uri imageUri;
                    if (data == null || data.getData() == null)
                    {
                        imageUri = mUriPhotoTaken;
                    }
                    else
                    {
                        imageUri = data.getData();
                    }
                    Intent intent = new Intent();
                    intent.setData(imageUri);
                    setResult(RESULT_OK, intent);
                    finish();
                }
                break;
            default:
                break;
        }
    }


    // When the button of "Take a Photo with Camera" is pressed.
    public void takePhoto(View view)
    {
       Intent intent = new Intent(this, RealTimeFaceDetectActivity.class);
       startActivityForResult(intent, REQUEST_TAKE_PHOTO);
/*
        Intent intPhotoTake = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // launching the intent by sending the request code for capturing image
        if (intPhotoTake.resolveActivity(getPackageManager()) != null)
        {

            Log.i("EXECUTE", "TAKE PHOTO");
            // Runntime permission checking
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {

                    ActivityCompat.requestPermissions(SelectImageActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_EXTERNAL_STORAGE);
                }
            }
            else
                startActivityForResult(intPhotoTake, REQUEST_TAKE_PHOTO);
        } */
    }

    // When the button of "Select a Photo in Album" is pressed.
    public void selectImageInAlbum(View view)
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null)
        {
            startActivityForResult(intent, REQUEST_SELECT_IMAGE_IN_ALBUM);
        }
    }
}
