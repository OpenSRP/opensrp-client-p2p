package org.smartregister.p2p.sync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;


/**
 * Includes all the callbacks required for implementing sending mode on the library. This also lines up with
 * the expected flow of the app when in the sender mode. This is used internally in the library.
 *
 * Created by Ephraim Kigamba - ekigamba@ona.io on 15/03/2019
 */
public interface ISenderSyncLifecycleCallback extends SyncLifecycleCallback {

    void onStartedDiscovering(@NonNull Object object);

    void onDiscoveringFailed(@NonNull Exception exception);

    void onDeviceFound(@NonNull final String endpointId, @NonNull final DiscoveredEndpointInfo discoveredEndpointInfo);

    void onRequestConnectionSuccessful(@Nullable Object result);

    void onRequestConnectionFailed(@NonNull Exception exception);

    void processReceivedHistory(@NonNull String endpointId, @NonNull Payload payload);
}
