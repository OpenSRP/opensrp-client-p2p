package org.smartregister.p2p.presenter;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.authenticator.BaseSyncConnectionAuthenticator;
import org.smartregister.p2p.authenticator.SenderConnectionAuthenticator;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.callback.OnResultCallback;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.sync.ConnectionLevel;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.sync.ISenderSyncLifecycleCallback;
import org.smartregister.p2p.util.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 15/03/2019
 */

public class P2PSenderPresenter extends BaseP2pModeSelectPresenter implements ISenderSyncLifecycleCallback, P2pModeSelectContract.SenderPresenter {

    @Nullable
    private DiscoveredDevice currentReceiver;
    private ConnectionLevel connectionLevel;
    private long hashKeyPayloadId;

    public P2PSenderPresenter(@NonNull P2pModeSelectContract.View view) {
        super(view);
    }

    @VisibleForTesting
    public P2PSenderPresenter(@NonNull P2pModeSelectContract.View view, @NonNull P2pModeSelectContract.Interactor p2pModeSelectInteractor) {
        super(view, p2pModeSelectInteractor);
    }

    @Override
    public void onSendButtonClicked() {
        prepareForDiscovering(false);
    }

    @Override
    public void prepareForDiscovering(boolean returningFromRequestingPermissions) {
        List<String> unauthorisedPermissions = view.getUnauthorisedPermissions();
        // Are all required permissions given
        if (unauthorisedPermissions.size() == 0) {
            // Check if location is enabled
            if (view.isLocationEnabled()) {
                startDiscoveringMode();
            } else {
                view.requestEnableLocation(new P2pModeSelectContract.View.OnLocationEnabled() {
                    @Override
                    public void locationEnabled() {
                        startDiscoveringMode();
                    }
                });
            }
        } else {
            if (!returningFromRequestingPermissions) {
                view.addOnActivityRequestPermissionHandler(new OnActivityRequestPermissionHandler() {
                    @Override
                    public int getRequestCode() {
                        return Constants.RqCode.PERMISSIONS;
                    }

                    @Override
                    public void handlePermissionResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
                        view.removeOnActivityRequestPermissionHandler(this);
                        prepareForDiscovering(true);
                    }
                });
                view.requestPermissions(unauthorisedPermissions);
            }
        }
    }

    @Override
    public void startDiscoveringMode() {
        if (!interactor.isDiscovering()) {
            view.enableSendReceiveButtons(false);
            view.showDiscoveringProgressDialog (new P2pModeSelectContract.View.DialogCancelCallback() {
                @Override
                public void onCancelClicked(DialogInterface dialogInterface) {
                    interactor.stopDiscovering();
                    dialogInterface.dismiss();
                    view.enableSendReceiveButtons(true);
                }
            });
            interactor.startDiscovering(this);
        }
    }

    @Override
    public void onStartedDiscovering(@NonNull Object object) {
        // Do nothing here for now
        // Continue showing the progress dialog
    }

    @Override
    public void onDiscoveringFailed(@NonNull Exception exception) {
        view.showToast(view.getString(R.string.error_occurred_cannot_start_sending), Toast.LENGTH_LONG);
        view.removeDiscoveringProgressDialog();
        view.enableSendReceiveButtons(true);
    }

    @Override
    public void onDeviceFound(@NonNull final String endpointId, @NonNull final DiscoveredEndpointInfo discoveredEndpointInfo) {
        Timber.i(view.getString(R.string.log_endpoint_found)
                , endpointId, discoveredEndpointInfo.getEndpointName(), discoveredEndpointInfo.getServiceId());

        if (currentReceiver == null) {
            currentReceiver = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

            // First stop discovering
            interactor.stopDiscovering();
            view.removeDiscoveringProgressDialog();

            interactor.requestConnection(endpointId, new OnResultCallback() {
                @Override
                public void onSuccess(@Nullable Object object) {
                    onRequestConnectionSuccessful(object);
                }

                @Override
                public void onFailure(@NonNull Exception e) {
                    onRequestConnectionFailed(e);
                }
            }, new org.smartregister.p2p.sync.SyncConnectionLifecycleCallback(this));
        }
    }

    @Override
    public void onRequestConnectionSuccessful(@Nullable Object result) {
        // Just show a success
        view.showToast(view.getString(R.string.connection_request_successful), Toast.LENGTH_LONG);

    }

    @Override
    public void onRequestConnectionFailed(@NonNull Exception exception) {
        // Show the user an error trying to connect device XYZ
        view.showToast(view.getString(R.string.could_not_initiate_connection_request_to_device), Toast.LENGTH_LONG);
        resetState();
        startDiscoveringMode();
    }

    @Override
    public void onConnectionInitiated(@NonNull final String endpointId, @NonNull final ConnectionInfo connectionInfo) {
        // Easier working with the device which we requested connection to, otherwise the callback for error should be called
        // so that we can work with other devices
        if (currentReceiver != null && currentReceiver.getEndpointId().equals(endpointId)) {
            currentReceiver.setConnectionInfo(connectionInfo);

            // This can be moved to the library for easy customisation by host applications
            BaseSyncConnectionAuthenticator syncConnectionAuthenticator = new SenderConnectionAuthenticator(this);
            syncConnectionAuthenticator.authenticate(currentReceiver, this);
        } else {
            //("Connection was initiated by other device");
            Timber.e(view.getString(R.string.log_ignoring_connection_initiated_by_other_device)
                    , endpointId
                    , connectionInfo.getEndpointName()
                    , connectionInfo.getAuthenticationToken());
        }

        // We can add connection support for multiple devices here later
    }

    @Override
    public void onAuthenticationSuccessful() {
        if (currentReceiver != null){
            view.showToast(view.getString(R.string.authentication_successful_receiver_can_accept_connection), Toast.LENGTH_LONG);

            interactor.acceptConnection(currentReceiver.getEndpointId(), new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                    P2PSenderPresenter.this.onPayloadReceived(endpointId, payload);
                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
                    // Do nothing for now
                    P2PSenderPresenter.this.onPayloadTransferUpdate(s, payloadTransferUpdate);
                }
            });
        }
    }

    @Override
    public void onAuthenticationFailed(@NonNull Exception exception) {
        // Reject the connection
        if (currentReceiver != null) {
            interactor.rejectConnection(currentReceiver.getEndpointId());
        }

        view.showToast(view.getString(R.string.authentication_failed_connection_rejected), Toast.LENGTH_LONG);

        //Todo: Go back to discovering mode
        Timber.e(exception, view.getString(R.string.authentication_failed));
        // The rest will be handled in the rejectConnection callback
        // Todo: test is this is causing an error where the discovering mode can no longer be restarted
        // if the receiving device app is either removed or advertising cancelled while the sender
        // app is showing the QR code scanning dialog
    }

    @Override
    public void onAuthenticationCancelled(@NonNull String reason) {
        // Reject the connection
        if (currentReceiver != null) {
            interactor.rejectConnection(currentReceiver.getEndpointId());
        }

        // Go back to discovering mode
        Timber.e(view.getString(R.string.authentication_cancelled_with_reason), reason);
    }


    @Override
    public void onConnectionAccepted(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        if (currentReceiver != null) {
            connectionLevel = ConnectionLevel.AUTHENTICATED;
            interactor.connectedTo(endpointId);
            P2PAuthorizationService authorizationService = P2PLibrary.getInstance()
                    .getP2PAuthorizationService();
            authorizationService.getAuthorizationDetails(new P2PAuthorizationService.OnAuthorizationDetailsProvidedCallback() {
                @Override
                public void onAuthorizationDetailsProvided(@NonNull Map<String, Object> authorizationDetails) {
                    // Send the authorization details
                    sendAuthorizationDetails(authorizationDetails);
                }
            });
        }
    }

    @Override
    public void onConnectionRejected(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        view.showToast(view.getString(R.string.receiver_rejected_the_connection), Toast.LENGTH_LONG);
        resetState();
        startDiscoveringMode();
    }

    @Override
    public void onConnectionUnknownError(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        //Todo: Go back to discovering mode
        //Todo: And show the user an error
        view.showToast(view.getString(R.string.an_error_occurred_before_acceptance_or_rejection), Toast.LENGTH_LONG);
        resetState();
        startDiscoveringMode();
    }

    @Override
    public void onConnectionBroken(@NonNull String endpointId) {
        //Todo: Show the user an error
        //Todo: Go back to discovering mode
        resetState();
        view.showToast(String.format(view.getString(R.string.connection_to_endpoint_broken)
                , endpointId), Toast.LENGTH_LONG);
        startDiscoveringMode();
    }

    @Override
    public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
        if (hashKeyPayloadId != 0) {
            if (hashKeyPayloadId == update.getPayloadId()) {
                if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                    connectionLevel = ConnectionLevel.SENT_HASH_KEY;
                } else if (update.getStatus() == PayloadTransferUpdate.Status.FAILURE) {
                    hashKeyPayloadId = 0;

                    //Todo: Should retry sending the hash key if the connection to the device is still alive
                }
            }
        }
    }

    @Override
    public void onDisconnected(@NonNull String endpointId) {
        Timber.e(view.getString(R.string.log_disconnected), endpointId);
        view.displayMessage(view.getString(R.string.disconnected));
        resetState();
        startDiscoveringMode();
    }

    @Override
    public void sendAuthorizationDetails(@NonNull Map<String, Object> authorizationDetails) {
        interactor.sendMessage(new Gson().toJson(authorizationDetails));
    }

    @Override
    public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
        P2PAuthorizationService p2PAuthorizationService = P2PLibrary.getInstance()
                .getP2PAuthorizationService();

        Timber.i(view.getString(R.string.log_received_payload_from_endpoint), endpointId);
        if (connectionLevel != null) {
            if (connectionLevel.equals(ConnectionLevel.AUTHORIZED)) {
                // Do nothing until the hash_key has been received on the other side
            } else if (connectionLevel.equals(ConnectionLevel.AUTHENTICATED)) {
                // Should get the details to authorize
                if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
                    String authenticationDetailsJson = new String(payload.asBytes());

                    // Validate json is a map

                    Map<String, Object> map = null;
                    try {
                        map = (Map<String, Object>) new Gson()
                                .fromJson(authenticationDetailsJson, Map.class);
                    } catch (JsonSyntaxException e) {
                        Timber.e(e);
                    }

                    if (map == null) {
                        onConnectionAuthorizationRejected("Authorization details sent by receiver are invalid");
                    } else {
                        p2PAuthorizationService.authorizeConnection(map
                                , P2PSenderPresenter.this);
                    }
                } else {
                    onConnectionAuthorizationRejected("Authorization details sent by receiver are invalid");
                }
            } else if (connectionLevel.equals(ConnectionLevel.SENT_HASH_KEY)) {
                // Do nothing for now
                if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
                    // Show a simple message of the text sent
                    String message = new String(payload.asBytes());
                    view.showToast(message, Toast.LENGTH_LONG);
                    view.displayMessage(endpointId + ": " + message);
                }
            }
        }
    }

    private void resetState() {
        connectionLevel = null;
        view.dismissAllDialogs();
        currentReceiver = null;
    }

    @Override
    public void onConnectionAuthorized() {
        connectionLevel = ConnectionLevel.AUTHORIZED;

        // Send the hash key
        sendBasicDeviceDetails();

        view.showToast(String.format(view.getString(R.string.you_are_connected_to_receiver)
                , currentReceiver.getEndpointName())
                , Toast.LENGTH_LONG);
        view.displayMessage(view.getString(R.string.connected));
    }

    private void sendBasicDeviceDetails() {
        Map<String, String> basicDeviceDetails = new HashMap<>();
        basicDeviceDetails.put(Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY, P2PLibrary.getInstance().getHashKey());
        basicDeviceDetails.put(Constants.BasicDeviceDetails.KEY_DEVICE_ID, P2PLibrary.getInstance().getDeviceUniqueIdentifier());

        hashKeyPayloadId = sendTextMessage(new Gson().toJson(basicDeviceDetails));
    }

    @Override
    public void onConnectionAuthorizationRejected(@NonNull String reason) {
        interactor.closeAllEndpoints();
        interactor.connectedTo(null);
        view.showToast(String.format(view.getString(R.string.connection_could_not_be_authorized)
                , currentReceiver.getEndpointName())
                , Toast.LENGTH_LONG);

        resetState();
        startDiscoveringMode();
    }

    @Nullable
    @Override
    public DiscoveredDevice getCurrentPeerDevice() {
        return currentReceiver;
    }

}
