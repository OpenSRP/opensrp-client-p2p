package org.smartregister.p2p.presenter;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import android.view.WindowManager;

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.interactor.P2pModeSelectInteractor;
import org.smartregister.p2p.tasks.ConnectionTimeout;
import org.smartregister.p2p.util.Constants;

import java.util.HashMap;
import java.util.HashSet;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public abstract class BaseP2pModeSelectPresenter implements P2pModeSelectContract.BasePresenter {

    protected P2pModeSelectContract.View view;
    protected P2pModeSelectContract.Interactor interactor;

    protected HashMap<String, Long> rejectedDevices = new HashMap<>();
    protected HashSet<String> blacklistedDevices = new HashSet<>();

    private int keepScreenOnCounter;

    protected boolean hasAcceptedConnection = false;
    protected ConnectionTimeout connectionTimeout;

    public BaseP2pModeSelectPresenter(@NonNull P2pModeSelectContract.View view) {
        this(view, new P2pModeSelectInteractor(view.getContext()));
    }

    protected BaseP2pModeSelectPresenter(@NonNull P2pModeSelectContract.View view, @NonNull P2pModeSelectContract.Interactor p2pModeSelectInteractor) {
        this.view = view;
        this.interactor = p2pModeSelectInteractor;

        // This will be added when issue https://github.com/OpenSRP/android-p2p-sync/issues/24
        // is being worked on
        //view.addOnResumeHandler(new AdvertisingResumeHandler(this, interactor));
    }

    @Override
    public long sendTextMessage(@NonNull String message) {
        if (getCurrentPeerDevice() != null && interactor != null) {
            return interactor.sendMessage(message);
        }

        return 0;
    }

    @Override
    public void onStop() {
        view.dismissAllDialogs();
        view.enableSendReceiveButtons(true);

        interactor.stopAdvertising();
        interactor.stopDiscovering();

        if (getCurrentPeerDevice() != null) {
            interactor.disconnectFromEndpoint(getCurrentPeerDevice().getEndpointId());
        }

        setCurrentDevice(null);

        interactor.cleanupResources();
        interactor = null;
    }

    @NonNull
    @Override
    public P2pModeSelectContract.View getView() {
        return view;
    }


    @Override
    public void addDeviceToRejectedList(@NonNull String endpointId) {
        rejectedDevices.put(endpointId, System.currentTimeMillis());
    }

    @Override
    public boolean addDeviceToBlacklist(@NonNull String endpointId) {
        rejectedDevices.remove(endpointId);
        if (blacklistedDevices.contains(endpointId)) {
            return false;
        } else {
            blacklistedDevices.add(endpointId);
            return true;
        }
    }

    @Override
    public void rejectDeviceOnAuthentication(@NonNull String endpointId) {
        Long rejectionTime = rejectedDevices.get(endpointId);
        if (rejectionTime != null) {
            if ((System.currentTimeMillis() - rejectionTime)/1e3 > P2PLibrary.getInstance().getDeviceMaxRetryConnectionDuration()) {
                addDeviceToRejectedList(endpointId);
            } else {
                addDeviceToBlacklist(endpointId);
            }
        } else {
            addDeviceToRejectedList(endpointId);
        }
    }

    /**
     * Enables or disables the keep screen on flag to avoid the device going to sleep while there
     * is a sync happening
     *
     * @param enable {@code TRUE} to enable or {@code FALSE} disable
     */
    protected void keepScreenOn(boolean enable) {
        Context context = getView().getContext();
        if (context instanceof Activity) {
            if (enable) {
                keepScreenOnCounter++;

                if (keepScreenOnCounter == 1) {
                    ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            } else {
                keepScreenOnCounter--;

                if (keepScreenOnCounter == 0) {
                    ((Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        } else {
            Timber.e("Could not %s KEEP_SCREEN_ON because the view-context is not an activity", (enable ? "enable" : "disable"));
        }
    }

    @Override
    public void sendSkipClicked() {
        if (interactor != null) {
            interactor.sendMessage(Constants.Connection.SKIP_QR_CODE_SCAN);
        }
    }

    @Override
    public void sendConnectionAccept() {
        interactor.sendMessage(Constants.Connection.CONNECTION_ACCEPT);
    }

    @Override
    public void startConnectionTimeout(@NonNull final OnConnectionTimeout onConnectionTimeout) {
        connectionTimeout = new ConnectionTimeout(Constants.CONNECTION_TIMEOUT_SECONDS, onConnectionTimeout);
        connectionTimeout.start();
    }

    public void stopConnectionTimeout() {
        if (connectionTimeout != null) {
            connectionTimeout.stop();
        }
    }
}
