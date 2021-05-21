package org.smartregister.p2p.handler;

import androidx.annotation.NonNull;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 11/03/2019
 */

public interface ActivityResultHandler {

    boolean addOnActivityResultHandler(@NonNull OnActivityResultHandler onActivityResultHandler);

    boolean removeOnActivityResultHandler(@NonNull OnActivityResultHandler onActivityResultHandler);
}
