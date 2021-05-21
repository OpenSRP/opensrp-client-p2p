package org.smartregister.p2p.sync.handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.util.Constants;

import java.util.HashMap;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 07/05/2019
 */

public class BaseSyncHandler {

    private HashMap<String, Integer> transferProgress = new HashMap<>();

    protected synchronized void updateTransferProgress(@NonNull String dataTypeName, int recordsTransferred) {
        if (transferProgress.containsKey(dataTypeName)) {
            transferProgress.put(dataTypeName, transferProgress.get(dataTypeName) + recordsTransferred);
        } else {
            transferProgress.put(dataTypeName, recordsTransferred);
        }
    }

    public HashMap<String, Integer> getTransferProgress() {
        return transferProgress;
    }

    protected void logTransfer(boolean isSending, @NonNull String dataTypeName, @Nullable DiscoveredDevice peerDevice, int recordsSize) {
        if (peerDevice != null) {
            String miscellaneousDetails = "";

            if (peerDevice.getAuthorizationDetails() != null) {
                miscellaneousDetails = new Gson().toJson(peerDevice.getAuthorizationDetails());
            }

            String username = peerDevice.getUsername() == null ? "" : peerDevice.getUsername();
            logTransfer(isSending, dataTypeName, username, miscellaneousDetails, recordsSize);
        } else {
            logTransfer(isSending, dataTypeName, "", "", recordsSize);
        }
    }

    protected void logTransfer(boolean isSending, @NonNull String dataTypeName, @NonNull String username, @NonNull String miscellaneous, int recordsSize) {
        String actionText = isSending ? P2PLibrary.getInstance().getContext().getString(R.string.sent)
                : P2PLibrary.getInstance().getContext().getString(R.string.received);
        Timber.tag(Constants.RECORDS_TRACK_TAG)
                .i(P2PLibrary.getInstance().getContext().getString(R.string.transfer_records_track_log_format)
                        , actionText, username, dataTypeName, recordsSize, miscellaneous);
        Timber.tag(Constants.RECORDS_TRACK_TAG_HR)
                .i(P2PLibrary.getInstance().getContext().getString(R.string.transfer_records_track_log_hr_format)
                        , username, actionText, recordsSize, dataTypeName, miscellaneous);
    }
}
