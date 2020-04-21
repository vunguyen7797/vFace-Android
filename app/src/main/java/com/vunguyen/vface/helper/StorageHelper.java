/*
 * StorageHelper.java
 */
package com.vunguyen.vface.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.vunguyen.vface.helper.callbackInterfaces.UriPhotoInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * This class implements methods to work with local storage
 */
public class StorageHelper
{
    private static FirebaseStorage storage = FirebaseStorage.getInstance();
    private static StorageReference storageRef;

    // Upload Photo to Firebase Storage
    public static void uploadToFireBaseStorage(Bitmap bitmapImage, String filename, String request, UriPhotoInterface callback)
    {
        storageRef = storage.getReferenceFromUrl("gs://vface-a53e6.appspot.com/"+request);
        StorageReference croppedPhotoRef = storageRef.child(filename);
        // Get the data from an ImageView as bytes
        ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
        bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayStream);
        byte[] data = byteArrayStream.toByteArray();

        UploadTask uploadTask = croppedPhotoRef.putBytes(data);

        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>()
        {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw Objects.requireNonNull(task.getException());
                }

                // Continue with the task to get the download URL
                return croppedPhotoRef.getDownloadUrl();
            }
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri uriCroppedImage = task.getResult();
                Log.i("EXECUTE", "DOWNLOAD SUCCESSFULLY " +uriCroppedImage);
                callback.getUriPhoto(uriCroppedImage);
            } else {
                // Handle failures
                // ...
                Log.i("EXECUTE", "CANNOT DOWNLOAD PHOTO");
            }
        });
    }
    // save the image in storage to get URI
    public static Uri saveToInternalStorageUri(Bitmap bitmapImage, String fileName, Context mContext)
    {
        File directory = mContext.getExternalFilesDir(Environment.getDataDirectory()+"/myfolder");
        File imgPath = new File(directory, fileName);
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(imgPath);
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                fos.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        Uri uriImage;
        uriImage = Uri.fromFile(imgPath);
        Log.i("EXECUTE", "Image saved at: " + directory.getAbsolutePath());
        return uriImage;
    }
}
