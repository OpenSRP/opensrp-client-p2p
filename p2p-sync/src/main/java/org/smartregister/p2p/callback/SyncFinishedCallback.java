package org.smartregister.p2p.callback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import java.util.HashMap;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 10/05/2019
 */

public interface SyncFinishedCallback {

    @UiThread
    void onSuccess(@NonNull HashMap<String, Integer> transferRecords);

    @UiThread
    void onFailure(@NonNull Exception exception, @Nullable HashMap<String, Integer> transferRecords);
}
