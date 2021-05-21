package org.smartregister.p2p.contract;

import android.content.Context;
import androidx.annotation.NonNull;

import org.smartregister.p2p.handler.ActivityRequestPermissionResultHandler;
import org.smartregister.p2p.handler.ActivityResultHandler;
import org.smartregister.p2p.handler.ActivityResumeHandler;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public interface BaseView extends ActivityResultHandler, ActivityResumeHandler, ActivityRequestPermissionResultHandler {

    void initializePresenters();

    void runOnUiThread(@NonNull Runnable runnable);

    @NonNull
    Context getContext();
}
