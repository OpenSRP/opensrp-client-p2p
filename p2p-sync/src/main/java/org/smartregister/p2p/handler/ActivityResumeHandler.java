package org.smartregister.p2p.handler;

import androidx.annotation.NonNull;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 11/03/2019
 */

public interface ActivityResumeHandler {

    boolean addOnResumeHandler(@NonNull OnResumeHandler onResumeHandler);

    boolean removeOnResumeHandler(@NonNull OnResumeHandler onResumeHandler);
}
