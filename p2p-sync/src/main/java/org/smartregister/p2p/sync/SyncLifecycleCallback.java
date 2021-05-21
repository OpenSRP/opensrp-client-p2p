package org.smartregister.p2p.sync;


import androidx.annotation.NonNull;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import org.smartregister.p2p.authenticator.BaseSyncConnectionAuthenticator;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;

import java.util.Map;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 15/03/2019
 */

public interface SyncLifecycleCallback extends BaseSyncConnectionAuthenticator.AuthenticationCallback
        , P2PAuthorizationService.AuthorizationCallback {

    void onConnectionInitiated(@NonNull final String endpointId, @NonNull final ConnectionInfo connectionInfo);

    void onConnectionAccepted(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution);

    void onConnectionRejected(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution);

    void onConnectionUnknownError(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution);

    void onConnectionBroken(@NonNull String endpointId);

    void onDisconnected(@NonNull String endpointId);

    void sendAuthorizationDetails(@NonNull Map<String, Object> authorizationDetails);

    void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload);

    void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update);

    void performAuthorization(@NonNull Payload payload);

    void processPayload(@NonNull String endpointId, @NonNull Payload payload);
}
