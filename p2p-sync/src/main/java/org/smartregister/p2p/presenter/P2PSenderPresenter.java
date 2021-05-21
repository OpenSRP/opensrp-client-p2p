package org.smartregister.p2p.presenter;

import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.authenticator.BaseSyncConnectionAuthenticator;
import org.smartregister.p2p.authenticator.SenderConnectionAuthenticator;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.callback.OnResultCallback;
import org.smartregister.p2p.callback.SyncFinishedCallback;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.dialog.SkipQRScanDialog;
import org.smartregister.p2p.fragment.ErrorFragment;
import org.smartregister.p2p.fragment.SyncProgressFragment;
import org.smartregister.p2p.fragment.SyncCompleteTransferFragment;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.P2pReceivedHistory;
import org.smartregister.p2p.sync.ConnectionLevel;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.sync.ISenderSyncLifecycleCallback;
import org.smartregister.p2p.sync.data.SyncPackageManifest;
import org.smartregister.p2p.sync.handler.SyncSenderHandler;
import org.smartregister.p2p.tasks.GenericAsyncTask;
import org.smartregister.p2p.tasks.Tasker;
import org.smartregister.p2p.util.Constants;
import org.smartregister.p2p.util.SyncDataConverterUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 15/03/2019
 */

public class P2PSenderPresenter extends BaseP2pModeSelectPresenter implements ISenderSyncLifecycleCallback, P2pModeSelectContract.SenderPresenter {

    @Nullable
    private DiscoveredDevice currentReceiver;
    private ConnectionLevel connectionLevel;
    private long hashKeyPayloadId;
    private long connectionSignalPayloadId;

    private HashMap<String, Integer> transferItems;

    @Nullable
    private SyncSenderHandler syncSenderHandler;

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
            keepScreenOn(true);
            view.showDiscoveringProgressDialog (new P2pModeSelectContract.View.DialogCancelCallback() {
                @Override
                public void onCancelClicked(DialogInterface dialogInterface) {
                    keepScreenOn(false);
                    interactor.stopDiscovering();
                    dialogInterface.dismiss();
                    stopConnectionTimeout();
                    view.showP2PModeSelectFragment(true);
                }
            });

            interactor.startDiscovering(this);

            // Start timeout
            startConnectionTimeout(new OnConnectionTimeout() {
                @Override
                public void connectionTimeout(long duration, @Nullable Exception e) {
                    if (e == null) {
                        if (interactor != null && interactor.isDiscovering()) {
                            interactor.stopDiscovering();
                            view.removeDiscoveringProgressDialog();
                            view.enableSendReceiveButtons(true);
                            keepScreenOn(false);

                            view.showErrorFragment(view.getString(R.string.no_nearby_devices_found)
                                    , view.getString(R.string.make_sure_peer_device_turned_on_in_range)
                                    , new ErrorFragment.OnOkClickCallback() {
                                        @Override
                                        public void onOkClicked() {
                                            view.showP2PModeSelectFragment(true);
                                        }
                                    });
                        } else {
                            Timber.e(view.getString(R.string.log_discovering_timed_out_while_not_in_discovering_mode));
                        }
                    }
                }
            });
        }
    }

    @Override
    public void sendSyncComplete() {
        // Do nothing for now
        transferItems = (HashMap<String, Integer>) syncSenderHandler.getTransferProgress().clone();

        SyncFinishedCallback syncFinishedCallback = P2PLibrary.getInstance().getSyncFinishedCallback();
        if (syncFinishedCallback != null) {
            syncFinishedCallback.onSuccess(transferItems);
        }

        syncSenderHandler = null;

        // incase the other side has hung at some point
        connectionSignalPayloadId = interactor.sendMessage(Constants.Connection.SYNC_COMPLETE);
    }

    @Override
    public long sendManifest(@NonNull SyncPackageManifest syncPackageManifest) {
        if (getCurrentPeerDevice() != null) {
            return interactor.sendMessage(new Gson().toJson(syncPackageManifest));
        }

        return 0;
    }

    @Override
    public void sendPayload(@NonNull Payload payload) {
        if (getCurrentPeerDevice() != null) {
            interactor.sendPayload(payload);
        }
    }

    @Override
    public void errorOccurredSync(@NonNull Exception e) {
        Timber.e(e);

        SyncFinishedCallback syncFinishedCallback = P2PLibrary.getInstance().getSyncFinishedCallback();
        if (syncFinishedCallback != null) {
            syncFinishedCallback.onFailure(e, transferItems);
        }

        if (syncSenderHandler != null) {
            String peerDeviceName = getCurrentPeerDevice() != null ? getCurrentPeerDevice().getEndpointName() : null;
            getView().showSyncCompleteFragment(false, peerDeviceName, new SyncCompleteTransferFragment.OnCloseClickListener() {
                @Override
                public void onCloseClicked() {
                    getView().showP2PModeSelectFragment(true);
                }
            }, SyncDataConverterUtil.generateSummaryReport(getView().getContext(), true, syncSenderHandler.getTransferProgress()), true);

            syncSenderHandler = null;
        }

        if (getCurrentPeerDevice() != null) {
            interactor.disconnectFromEndpoint(getCurrentPeerDevice().getEndpointId());
            resetState();
        }
    }

    @Override
    public void onStartedDiscovering(@NonNull Object object) {
        // Do nothing here for now
        // Continue showing the progress dialog
    }

    @Override
    public void onDiscoveringFailed(@NonNull Exception exception) {
        stopConnectionTimeout();

        view.removeDiscoveringProgressDialog();
        view.enableSendReceiveButtons(true);
    }

    @Override
    public void onDeviceFound(@NonNull final String endpointId, @NonNull final DiscoveredEndpointInfo discoveredEndpointInfo) {
        stopConnectionTimeout();

        Timber.i(view.getString(R.string.log_endpoint_found)
                , endpointId, discoveredEndpointInfo.getEndpointName(), discoveredEndpointInfo.getServiceId());

        // Reject when already connected or the connecting device is blacklisted
        if (currentReceiver == null && !blacklistedDevices.contains(endpointId)) {
            setCurrentDevice(new DiscoveredDevice(endpointId, discoveredEndpointInfo));
            getCurrentPeerDevice().setUsername(discoveredEndpointInfo.getEndpointName());

            // First stop discovering
            keepScreenOn(false);
            interactor.stopDiscovering();

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
        // We ignore blacklisted devices and do not request a connection with them
    }

    @Override
    public void onRequestConnectionSuccessful(@Nullable Object result) {
        // Just show a success
        view.removeDiscoveringProgressDialog();
    }

    @Override
    public void onRequestConnectionFailed(@NonNull Exception exception) {
        // Show the user an error trying to connect device XYZ
        resetState();
        prepareForDiscovering(false);
    }

    @Override
    public void processReceivedHistory(@NonNull final String endpointId, @NonNull Payload payload) {
        if (currentReceiver != null) {
            connectionLevel = ConnectionLevel.RECEIPT_OF_RECEIVED_HISTORY;

            if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
                try {
                    Type receivedHistoryListType = new TypeToken<ArrayList<P2pReceivedHistory>>() {}.getType();
                    final List<P2pReceivedHistory> receivedHistory = new Gson().fromJson(new String(payload.asBytes()), receivedHistoryListType);

                    Tasker.run(new Callable<TreeSet<DataType>>() {
                        @Override
                        public TreeSet<DataType> call() throws Exception {
                            return P2PLibrary.getInstance().getSenderTransferDao()
                                    .getDataTypes();
                        }
                    }, new GenericAsyncTask.OnFinishedCallback<TreeSet<DataType>>() {
                        @Override
                        public void onSuccess(@Nullable TreeSet<DataType> result) {
                            if (result != null) {
                                syncSenderHandler = new SyncSenderHandler(P2PSenderPresenter.this, result, receivedHistory);
                                syncSenderHandler.startSyncProcess();
                            } else {
                                sendSyncComplete();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            Timber.e(e);
                            disconnectAndReset(endpointId);
                        }
                    });
                } catch (JsonParseException ex) {
                    Timber.e(ex, view.getString(R.string.log_jsonparse_exception_trying_to_process_received_history));
                    disconnectAndReset(endpointId);
                }
            }
        }
    }

    @Override
    public void onConnectionInitiated(@NonNull final String endpointId, @NonNull final ConnectionInfo connectionInfo) {
        // Easier working with the device which we requested connection to, otherwise the callback for error should be called
        // so that we can work with other devices
        if (getCurrentPeerDevice() != null && getCurrentPeerDevice().getEndpointId().equals(endpointId)) {
            getCurrentPeerDevice().setConnectionInfo(connectionInfo);

            interactor.stopDiscovering();
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
        } else {
            //("Connection was initiated by other device");
            Timber.e(view.getString(R.string.log_rejecting_connection_initiated_by_other_device)
                    , endpointId
                    , connectionInfo.getEndpointName()
                    , connectionInfo.getAuthenticationToken());
        }

        // We can add connection support for multiple devices here later
    }

    public void performDeviceAuthentication() {
        // This can be moved to the library for easy customisation by host applications
        BaseSyncConnectionAuthenticator syncConnectionAuthenticator = new SenderConnectionAuthenticator(this);
        syncConnectionAuthenticator.authenticate(getCurrentPeerDevice(), this);
    }

    @Override
    public void onAuthenticationSuccessful() {
        if (getCurrentPeerDevice() != null){
            connectionLevel = ConnectionLevel.AUTHENTICATED;
            startDeviceAuthorization(getCurrentPeerDevice().getEndpointId());
        } else {
            Timber.e(view.getString(R.string.log_onauthenticationsuccessful_without_peer_device));
        }
    }

    @Override
    public void onAuthenticationFailed(@NonNull String reason, @NonNull Exception exception) {
        // Reject the connection
        if (currentReceiver != null) {
            String endpointId = currentReceiver.getEndpointId();
            disconnectAndReset(endpointId);
        }

        view.showErrorFragment(view.getString(R.string.connection_lost), reason, new ErrorFragment.OnOkClickCallback() {
            @Override
            public void onOkClicked() {
                view.showP2PModeSelectFragment(true);
            }
        });

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
            String endpointId = currentReceiver.getEndpointId();
            rejectDeviceOnAuthentication(endpointId);
            disconnectAndReset(endpointId);
        } else {
            Timber.e("onAuthenticationCancelled was called and no peer device is connected");
        }

        // Go back to discovering mode
        Timber.e(view.getString(R.string.authentication_cancelled_with_reason), reason);
    }


    @Override
    public void onConnectionAccepted(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        if (getCurrentPeerDevice() != null) {
            interactor.connectedTo(endpointId);
            connectionLevel = ConnectionLevel.CONNECT_BEFORE_AUTHENTICATE;
            performDeviceAuthentication();
        } else {
            Timber.e(view.getString(R.string.log_onconnectionaccepted_without_peer_device), endpointId);
        }
    }

    public void startDeviceAuthorization(@NonNull String endpointId) {
        connectionLevel = ConnectionLevel.AUTHENTICATED;
        P2PAuthorizationService authorizationService = P2PLibrary.getInstance()
                .getP2PAuthorizationService();
        authorizationService.getAuthorizationDetails(new P2PAuthorizationService.OnAuthorizationDetailsProvidedCallback() {
            @Override
            public void onAuthorizationDetailsProvided(@NonNull Map<String, Object> authorizationDetails) {
                // Send the authorization details
                authorizationDetails.put(Constants.AuthorizationKeys.PEER_STATUS, Constants.PeerStatus.SENDER);
                sendAuthorizationDetails(authorizationDetails);
            }
        });
    }

    @Override
    public void onConnectionRejected(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        // If my device is rejected by another device
        // I should also reject & start blacklisting the device here
        // so that our device is also able to connect to other devices
        if (currentReceiver != null) {
            rejectDeviceOnAuthentication(endpointId);

            resetState();
            startDiscoveringMode();
        } else {
            Timber.e(view.getString(R.string.log_onconnectionrejected_without_peer_device), endpointId);
        }
    }

    @Override
    public void onConnectionUnknownError(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        //Todo: Go back to discovering mode
        //Todo: And show the user an error
        if (getCurrentPeerDevice() != null && endpointId.equals(getCurrentPeerDevice().getEndpointId())) {
            String errorMsg = String.format(view.getString(R.string.please_make_sure_device_is_turned_on_and_in_range), getCurrentPeerDevice().getEndpointName());
            resetState();

            view.showErrorFragment(view.getString(R.string.connection_lost), errorMsg, new ErrorFragment.OnOkClickCallback() {
                @Override
                public void onOkClicked() {
                    view.showP2PModeSelectFragment(true);
                }
            });
        } else {
            Timber.e(view.getString(R.string.log_onconnectionunknownerror_without_peer_device), endpointId);
        }
    }

    @Override
    public void onConnectionBroken(@NonNull String endpointId) {
        if (getCurrentPeerDevice() != null && endpointId.equals(getCurrentPeerDevice().getEndpointId())) {
            String errorMsg = String.format(view.getString(R.string.connection_to_endpoint_broken), endpointId);

            if (connectionLevel == ConnectionLevel.SENT_RECEIVED_HISTORY) {
                errorOccurredSync(new Exception(errorMsg));
            } else {
                errorMsg = String.format(view.getString(R.string.please_make_sure_device_is_turned_on_and_in_range), getCurrentPeerDevice().getEndpointName());
                view.showErrorFragment(view.getString(R.string.connection_lost), errorMsg, new ErrorFragment.OnOkClickCallback() {
                    @Override
                    public void onOkClicked() {
                        view.showP2PModeSelectFragment(true);
                    }
                });

                if (getCurrentPeerDevice() != null) {
                    interactor.disconnectFromEndpoint(getCurrentPeerDevice().getEndpointId());
                    resetState();
                }
            }
        } else {
            Timber.e(view.getString(R.string.log_onconnectionbroken_without_peer_device), endpointId);
        }
    }

    @Override
    public synchronized void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
        if (hashKeyPayloadId != 0 && hashKeyPayloadId == update.getPayloadId()) {
            if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                connectionLevel = ConnectionLevel.SENT_HASH_KEY;
            } else if (update.getStatus() == PayloadTransferUpdate.Status.FAILURE) {
                hashKeyPayloadId = 0;

                //Todo: Should retry sending the hash key if the connection to the device is still alive
            }
        } else if (connectionSignalPayloadId != 0l) {
            if (update.getPayloadId() == connectionSignalPayloadId
                    && currentReceiver != null
                    && update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {

                view.showSyncCompleteFragment(true, currentReceiver.getEndpointName(), new SyncCompleteTransferFragment.OnCloseClickListener() {
                    @Override
                    public void onCloseClicked() {
                        view.showP2PModeSelectFragment(true);
                    }
                }, SyncDataConverterUtil.generateSummaryReport(view.getContext(), true, transferItems), true);
                transferItems = null;
                disconnectAndReset(currentReceiver.getEndpointId(), false);
            }
        } else {
            if (syncSenderHandler != null) {
                syncSenderHandler.onPayloadTransferUpdate(update);
            }
        }
    }

    @Override
    public void performAuthorization(@NonNull Payload payload) {
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
                onConnectionAuthorizationRejected(view.getString(R.string.reason_authorization_rejected_by_receiver_details_invalid));
            } else {
                getCurrentPeerDevice().setAuthorizationDetails(map);
                P2PLibrary.getInstance()
                        .getP2PAuthorizationService()
                        .authorizeConnection(map, P2PSenderPresenter.this);
            }
        } else {
            onConnectionAuthorizationRejected(view.getString(R.string.reason_authorization_rejected_by_receiver_details_invalid));
        }
    }

    @Override
    public void processPayload(@NonNull String endpointId, @NonNull Payload payload) {
        if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null && syncSenderHandler != null) {
            syncSenderHandler.processString(new String(payload.asBytes()));
        }
    }

    @Override
    public void onDisconnected(@NonNull String endpointId) {
        if (getCurrentPeerDevice() != null && endpointId.equals(getCurrentPeerDevice().getEndpointId())) {
            Timber.e(view.getString(R.string.log_disconnected), endpointId);
            resetState();
            prepareForDiscovering(false);
        } else {
            Timber.e(view.getString(R.string.log_ondisconnected_without_peer_device), endpointId);
        }
    }

    @Override
    public void sendAuthorizationDetails(@NonNull Map<String, Object> authorizationDetails) {
        interactor.sendMessage(new Gson().toJson(authorizationDetails));
    }

    @Override
    public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
        Timber.i(view.getString(R.string.log_received_payload_from_endpoint), endpointId);
        if (connectionLevel != null) {
            // We ignore the authorized state since we should not process anything at this point but more
            // at #onConnectionAuthorized
            if (connectionLevel.equals(ConnectionLevel.CONNECT_BEFORE_AUTHENTICATE)) {
                // Process the command
                if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
                    String command = new String(payload.asBytes());

                    if (command.equals(Constants.Connection.SKIP_QR_CODE_SCAN)) {
                        getView().removeQRCodeScanningFragment();
                        getView().showSkipQRScanDialog(Constants.PeerStatus.SENDER
                                , getCurrentPeerDevice().getEndpointName()
                                , new SkipQRScanDialog.SkipDialogCallback() {

                            @Override
                            public void onSkipClicked(@NonNull DialogInterface dialogInterface) {
                                sendConnectionAccept();
                                onAuthenticationSuccessful();
                            }

                            @Override
                            public void onCancelClicked(@NonNull DialogInterface dialogInterface) {
                                onAuthenticationCancelled("User rejected the connection after receiver skipped QR Scanning");
                            }
                        });
                    } else if (command.equals(Constants.Connection.CONNECTION_ACCEPT)) {
                        getView().removeConnectingDialog();
                        onAuthenticationSuccessful();
                    }
                } else {
                    Timber.e("Could not be able to process payload sent while in ConnectionLevel CONNECT_BEFORE_AUTHENTICATE");
                }

            } else if (connectionLevel.equals(ConnectionLevel.AUTHENTICATED)) {
                // Should get the details to authorize
                performAuthorization(payload);
            } else if (connectionLevel.equals(ConnectionLevel.AUTHORIZED) && payload.getType() == Payload.Type.BYTES
                    && payload.asBytes() != null && (new String(payload.asBytes()).equals(Constants.Connection.START_TRANSFER))) {
                startTransfer();
            } else if (connectionLevel.equals(ConnectionLevel.SENT_HASH_KEY)) {
                // Do nothing for now
                processReceivedHistory(endpointId, payload);
            } else if (connectionLevel.equals(ConnectionLevel.RECEIPT_OF_RECEIVED_HISTORY)) {
                processPayload(endpointId, payload);
            }
        }
    }

    private void resetState() {
        hasAcceptedConnection = false;
        connectionLevel = null;
        view.dismissAllDialogs();
        setCurrentDevice(null);
        view.enableSendReceiveButtons(true);
    }

    @Override
    public void onConnectionAuthorized() {
        connectionLevel = ConnectionLevel.AUTHORIZED;

        view.showDevicesConnectedFragment(new P2pModeSelectContract.View.OnStartTransferClicked() {
            @Override
            public void startTransferClicked() {
                startTransfer();
            }
        });
    }

    public void startTransfer() {
        // Send the hash key
        sendBasicDeviceDetails();

        view.showSyncProgressFragment(view.getString(R.string.sending_data), new SyncProgressFragment.SyncProgressDialogCallback() {
            @Override
            public void onCancelClicked() {
                if (interactor !=null && interactor.getCurrentEndpoint() != null) {
                    errorOccurredSync(new Exception("User cancelled sync process"));
                } else {
                    Timber.e("Could not stop sending data because no endpoint exists");
                }
            }
        });
    }

    private void sendBasicDeviceDetails() {
        Map<String, String> basicDeviceDetails = new HashMap<>();
        basicDeviceDetails.put(Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY, P2PLibrary.getInstance().getHashKey());
        basicDeviceDetails.put(Constants.BasicDeviceDetails.KEY_DEVICE_ID, P2PLibrary.getInstance().getDeviceUniqueIdentifier());

        hashKeyPayloadId = sendTextMessage(new Gson().toJson(basicDeviceDetails));
    }

    @Override
    public void onConnectionAuthorizationRejected(@NonNull String reason) {
        // Disconnect from the endpoint
        if (currentReceiver != null) {
            String endpointId = currentReceiver.getEndpointId();
            addDeviceToBlacklist(endpointId);

            disconnectAndReset(endpointId, false);

            view.showErrorFragment(view.getString(R.string.authorization_failed), reason, new ErrorFragment.OnOkClickCallback() {
                @Override
                public void onOkClicked() {
                    view.showP2PModeSelectFragment(true);
                }
            });
        } else {
            resetState();
            prepareForDiscovering(false);
        }
    }

    @Nullable
    @Override
    public DiscoveredDevice getCurrentPeerDevice() {
        return currentReceiver;
    }

    @Override
    public void setCurrentDevice(@Nullable DiscoveredDevice discoveredDevice) {
        currentReceiver = discoveredDevice;
        keepScreenOn(discoveredDevice != null);
    }

    @Override
    public void disconnectAndReset(@NonNull String endpointId, boolean startDiscovering) {

        interactor.disconnectFromEndpoint(endpointId);
        interactor.connectedTo(null);
        connectionSignalPayloadId = 0l;

        resetState();

        if (startDiscovering) {
            prepareForDiscovering(false);
        }
    }

    @Override
    public void disconnectAndReset(@NonNull String endpointId) {
        disconnectAndReset(endpointId, true);
    }

}
