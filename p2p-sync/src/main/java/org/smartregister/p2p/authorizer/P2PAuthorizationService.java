package org.smartregister.p2p.authorizer;

import androidx.annotation.NonNull;

import java.util.Map;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 20/03/2019
 */

public interface P2PAuthorizationService {

    void authorizeConnection(@NonNull Map<String, Object> authorizationDetails, @NonNull AuthorizationCallback authorizationCallback);

    void getAuthorizationDetails(@NonNull OnAuthorizationDetailsProvidedCallback onAuthorizationDetailsProvidedCallback);

    interface OnAuthorizationDetailsProvidedCallback {

        void onAuthorizationDetailsProvided(@NonNull Map<String, Object> authorizationDetails);
    }

    interface AuthorizationCallback {

        void onConnectionAuthorized();

        void onConnectionAuthorizationRejected(@NonNull String reason);
    }
}
