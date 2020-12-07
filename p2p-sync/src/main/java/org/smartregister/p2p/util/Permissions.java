package org.smartregister.p2p.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Enables it easily to:
 * - Get a list of unauthorised permissions from permissions required
 * - Check if a specific permissions is granted
 * - Request for permissions from an activity
 *
 */
public abstract class Permissions {
    public static final String[] CRITICAL_PERMISSIONS = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
    };

    public static final String[] CRITICAL_PERMISSIONS_WITH_STORAGE = new String[] {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Returns {@link android.content.pm.PermissionInfo#PROTECTION_DANGEROUS} permissions which
     * have not been requested yet/denied by the user from the list of #criticalPermissions
     * required
     *
     * @param context
     * @param criticalPermissions array of dangerous permissions required
     * @return list of unauthorised permissions
     */
    public static List<String> getUnauthorizedCriticalPermissions(@NonNull Context context, @NonNull String[] criticalPermissions) {
        List<String> unauthorizedPermissions = new ArrayList<>();
        for (String curPermission : criticalPermissions) {
            if (!isPermissionGranted(context, curPermission)) {
                unauthorizedPermissions.add(curPermission);
            }
        }

        return unauthorizedPermissions;
    }

    /**
     * Checks if a specific application permission is authorised
     *
     * @param context
     * @param permission Permission name from constants in {@link android.Manifest.permission}
     * @return  {@code TRUE} if the permission is authorised
     *          {@code FALSE} if the permission is not authorised
     */
    public static boolean isPermissionGranted(@NonNull Context context, @NonNull String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests for a certain permission from the user by showing a System Dialog box
     *
     * @param activity
     * @param permissions Permission names to request from the user
     * @param requestCode Request code that will be returned on {@link Activity#onActivityResult(int, int, android.content.Intent)}
     */
    public static void request(@NonNull Activity activity, @NonNull String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }
}
