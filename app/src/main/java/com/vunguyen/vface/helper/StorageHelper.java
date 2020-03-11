/*
 * StorageHelper.java
 */
package com.vunguyen.vface.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class implements methods to work with local storage
 */
public class StorageHelper
{
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
