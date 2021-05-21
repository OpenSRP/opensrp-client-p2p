package org.smartregister.p2p.sync;

import androidx.annotation.NonNull;
import com.google.android.gms.nearby.connection.Payload;

import org.smartregister.p2p.model.P2pReceivedHistory;

import java.util.List;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 15/03/2019
 */

public interface IReceiverSyncLifecycleCallback extends SyncLifecycleCallback {

    void onStartedAdvertising(Object result);

    void onAdvertisingFailed(@NonNull Exception e);

    void processHashKey(@NonNull final String endpointId, @NonNull Payload payload);

    void sendLastReceivedRecords(@NonNull List<P2pReceivedHistory> receivedHistory);
}
