package org.smartregister.p2p.handler;

import androidx.annotation.NonNull;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 12/03/2019
 */

public interface OnActivityRequestPermissionHandler {

    int getRequestCode();

    void handlePermissionResult(@NonNull String[] permissions, @NonNull int[] grantResults);
}
