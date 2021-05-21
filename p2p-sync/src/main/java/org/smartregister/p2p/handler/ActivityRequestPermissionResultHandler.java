package org.smartregister.p2p.handler;

import androidx.annotation.NonNull;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 12/03/2019
 */

public interface ActivityRequestPermissionResultHandler {

    boolean addOnActivityRequestPermissionHandler(@NonNull OnActivityRequestPermissionHandler onActivityRequestPermissionHandler);

    boolean removeOnActivityRequestPermissionHandler(@NonNull OnActivityRequestPermissionHandler onActivityRequestPermissionHandler);
}
