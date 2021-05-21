package org.smartregister.p2p.interactor;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.smartregister.p2p.BuildConfig;
import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.callback.OnResultCallback;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.sync.IReceiverSyncLifecycleCallback;
import org.smartregister.p2p.sync.ISenderSyncLifecycleCallback;
import org.smartregister.p2p.util.Constants;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public class P2pModeSelectInteractor implements P2pModeSelectContract.Interactor {

    private Context context;
    private String appPackageName;
    private boolean advertising;
    private boolean discovering;

    private ConnectionsClient connectionsClient;

    @Nullable
    private String endpointIdConnected;

    public P2pModeSelectInteractor(@NonNull Context context) {
        this.context = context;
        this.appPackageName = context.getApplicationContext().getPackageName();

        connectionsClient = Nearby.getConnectionsClient(context);
    }

    @NonNull
    @Override
    public String getUserNickName() {
        P2PLibrary p2PLibrary = P2PLibrary.getInstance();
        return p2PLibrary.getUsername();
    }

    @NonNull
    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void requestConnection(@NonNull String endpointId
            , @NonNull final OnResultCallback onRequestConnectionResult, @NonNull ConnectionLifecycleCallback connectionLifecycleCallback) {
        connectionsClient.requestConnection(getUserNickName(), endpointId, connectionLifecycleCallback)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        onRequestConnectionResult.onSuccess(aVoid);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timber.e(e);
                        onRequestConnectionResult.onFailure(e);
                    }
                });
    }

    @Override
    public void acceptConnection(@NonNull String endpointId, PayloadCallback payloadCallback) {
        connectionsClient.acceptConnection(endpointId, payloadCallback);
    }

    @Override
    public void rejectConnection(@NonNull final String endpointId) {
        connectionsClient.rejectConnection(endpointId)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Timber.i(getContext().getString(R.string.log_connection_rejected_successfully), endpointId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timber.e(e, getContext().getString(R.string.log_connection_rejection_failed), endpointId);
                    }
                });
    }

    @Override
    public void startAdvertising(@NonNull final IReceiverSyncLifecycleCallback iReceiverSyncLifecycleCallback) {
        AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder()
                .setStrategy(Constants.STRATEGY)
                .build();

        connectionsClient
                .startAdvertising(getUserNickName(), getAppPackageName()
                        , new org.smartregister.p2p.sync.SyncConnectionLifecycleCallback(iReceiverSyncLifecycleCallback)
                        , advertisingOptions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        advertising = true;
                        // Todo: Fix this, it causes a null object reference on Context
                        // prevent crash when context is void
                        if(context != null){
                            String message = context.getString(R.string.advertising_started);
                            // For now this issue does not deal with this
                            Timber.i(message);
                            showToast(message);
                        }

                        iReceiverSyncLifecycleCallback.onStartedAdvertising(aVoid);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (!(e instanceof ApiException &&
                                ((ApiException) e).getStatusCode() == ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING)) {
                            advertising = false;
                        }
                        String message = context.getString(R.string.advertising_could_not_be_started);
                        showToast(message);

                        iReceiverSyncLifecycleCallback.onAdvertisingFailed(e);
                        Timber.e(e);
                    }
                });
    }

    @Override
    public void stopAdvertising() {
        if (advertising) {
            advertising = false;
            connectionsClient.stopAdvertising();
        }
    }

    @Override
    public boolean isAdvertising() {
        return advertising;
    }

    @Override
    public void startDiscovering(@NonNull final ISenderSyncLifecycleCallback iSenderSyncLifecycleCallback) {
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder()
                .setStrategy(Constants.STRATEGY)
                .build();

        connectionsClient.startDiscovery(getAppPackageName(), new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(@NonNull final String endpointId, @NonNull final DiscoveredEndpointInfo discoveredEndpointInfo) {
                iSenderSyncLifecycleCallback.onDeviceFound(endpointId, discoveredEndpointInfo);
            }

            @Override
            public void onEndpointLost(@NonNull String endpointId) {
                iSenderSyncLifecycleCallback.onDisconnected(endpointId);
            }
        }, discoveryOptions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        discovering = true;
                        String message = getContext().getString(R.string.log_discovery_started_successfully);
                        Timber.i(message);

                        iSenderSyncLifecycleCallback.onStartedDiscovering(aVoid);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (!(e instanceof ApiException &&
                                ((ApiException) e).getStatusCode() == ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING)) {
                            discovering = false;
                        }
                        String message = getContext().getString(R.string.log_discovery_could_not_be_started);
                        Timber.e(e, message);

                        iSenderSyncLifecycleCallback.onDiscoveringFailed(e);
                    }
                });
    }

    @Override
    public void stopDiscovering() {
        if (isDiscovering()) {
            discovering = false;
            connectionsClient.stopDiscovery();
        }
    }

    @Override
    public boolean isDiscovering() {
        return discovering;
    }

    @Override
    public void closeAllEndpoints() {
        connectionsClient.stopAllEndpoints();
    }

    @Override
    public void disconnectFromEndpoint(@NonNull String endpointId) {
        connectionsClient.disconnectFromEndpoint(endpointId);
    }

    @Override
    public void cleanOngoingConnectionResources() {
        // Todo: rename this to reset lost connection resources
    }

    @Override
    public long sendMessage(@NonNull String message) {
        if (endpointIdConnected != null) {
            Payload payload = Payload.fromBytes(message.getBytes());
            connectionsClient.sendPayload(endpointIdConnected, payload);

            return payload.getId();
        }

        return 0;
    }

    @Override
    public void sendPayload(@NonNull Payload payload) {
        if (endpointIdConnected != null) {
            connectionsClient.sendPayload(endpointIdConnected, payload);
        }
    }

    @Override
    public void connectedTo(@Nullable String endpointId) {
        endpointIdConnected = endpointId;
    }

    @Nullable
    @Override
    public String getCurrentEndpoint() {
        return endpointIdConnected;
    }

    @NonNull
    @Override
    public String getAppPackageName() {
        return appPackageName;
    }

    @Override
    public void cleanupResources() {
        connectionsClient = null;
        context = null;
    }

    public void showToast(@NonNull String message) {
        if (BuildConfig.DEBUG) {
            Toast.makeText(context, message, Toast.LENGTH_LONG)
                    .show();
        }
    }
}
