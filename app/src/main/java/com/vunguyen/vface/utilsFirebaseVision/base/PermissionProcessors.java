/*
 * PermissionProcessors.java
 */
package com.vunguyen.vface.utilsFirebaseVision.base;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements several helper methods to process the permission from users
 * as well as some methods to store data temporarily
 */
public class PermissionProcessors
{
    private static final int PERMISSION_REQUESTS = 1;

    // check if a permission is granted or not
    private static boolean isPermissionAccepted(Context context, String permission)
    {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    // get permission from users
    private static String[] getPermissions(Activity activity)
    {
        try
        {
            PackageInfo info =
                    activity.getPackageManager()
                            .getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0)
            {
                return ps;
            }
            else
            {
                return new String[0];
            }
        }
        catch (Exception e)
        {
            return new String[0];
        }
    }

    // check if all permission is allowed or not
    public static boolean allPermissionsAccepted(Activity activity)
    {
        for (String permission : getPermissions(activity))
        {
            if (!isPermissionAccepted(activity, permission))
            {
                return false;
            }
        }
        return true;
    }

    // ask for permission during runtime
    public static void getRuntimePermissions(Activity activity)
    {
        List<String> allPermissions = new ArrayList<>();
        for (String permission : getPermissions(activity))
        {
            if (!isPermissionAccepted(activity, permission))
            {
                allPermissions.add(permission);
            }
        }

        if (!allPermissions.isEmpty())
        {
            ActivityCompat.requestPermissions(activity, allPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }
}
