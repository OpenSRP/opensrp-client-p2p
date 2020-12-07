package org.smartregister.p2p.util;

import android.Manifest;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;

import java.io.File;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 02/05/2019
 */

public class NearbyStorageUtil {

    public static void deleteFilesInNearbyFolder(@NonNull Context context) {

        if (Permissions.isPermissionGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            if (downloadFolder.exists()) {
                File nearbyFolder = new File(downloadFolder, Constants.NEARBY_DIRECTORY);

                if (nearbyFolder.exists()) {
                    File[] nearbyDirectoryFiles = nearbyFolder.listFiles();

                    if (nearbyDirectoryFiles != null)
                        for (File nearbyFile : nearbyDirectoryFiles) {
                            if (nearbyFile.delete()) {
                                Timber.e("Could not delete %s", nearbyFile.getAbsoluteFile());
                            }
                        }
                }
            }

        } else {
            Timber.e("Cannot delete files in nearby folder because storage permissions are not provided");
        }
    }
}
