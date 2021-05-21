package org.smartregister.p2p.presenter;

import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.authenticator.BaseSyncConnectionAuthenticator;
import org.smartregister.p2p.authenticator.ReceiverConnectionAuthenticator;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.callback.SyncFinishedCallback;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.dialog.SkipQRScanDialog;
import org.smartregister.p2p.fragment.ErrorFragment;
import org.smartregister.p2p.fragment.SyncProgressFragment;
import org.smartregister.p2p.fragment.SyncCompleteTransferFragment;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.model.AppDatabase;
import org.smartregister.p2p.model.P2pReceivedHistory;
import org.smartregister.p2p.model.SendingDevice;
import org.smartregister.p2p.sync.ConnectionLevel;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.sync.IReceiverSyncLifecycleCallback;
import org.smartregister.p2p.sync.handler.SyncReceiverHandler;
import org.smartregister.p2p.tasks.GenericAsyncTask;
import org.smartregister.p2p.tasks.Tasker;
import org.smartregister.p2p.util.Constants;
import org.smartregister.p2p.util.SyncDataConverterUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 18/03/2019
 */

public class P2PReceiverPresenter extends BaseP2pModeSelectPresenter implements P2pModeSelectContract.ReceiverPresenter
        , IReceiverSyncLifecycleCallback, P2PAuthorizationService.AuthorizationCallback {

    @Nullable
    private DiscoveredDevice currentSender;
    private SendingDevice currentSendingDevice;
    private ConnectionLevel connectionLevel;

    private SyncReceiverHandler syncReceiverHandler;

    public P2PReceiverPresenter(@NonNull P2pModeSelectContract.View view) {
        super(view);
    }

    @VisibleForTesting
    public P2PReceiverPresenter(@NonNull P2pModeSelectContract.View view, @NonNull P2pModeSelectContract.Interactor p2pModeSelectInteractor) {
        super(view, p2pModeSelectInteractor);
    }

    @Override
    public void onReceiveButtonClicked() {
        prepareForAdvertising(false);
    }

    @Override
    public void prepareForAdvertising(boolean returningFromRequestingPermissions) {
        List<String> unauthorisedPermissions = view.getUnauthorisedPermissions();
        // Are all required permissions given
        if (unauthorisedPermissions.size() == 0) {
            // Check if location is enabled
            if (view.isLocationEnabled()) {
                startAdvertisingMode();
            } else {
                view.requestEnableLocation(new P2pModeSelectContract.View.OnLocationEnabled() {
                    @Override
                    public void locationEnabled() {
                        startAdvertisingMode();
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
                        prepareForAdvertising(true);
                    }
                });
                view.requestPermissions(unauthorisedPermissions);
            }
        }
    }

    @Override
    public void startAdvertisingMode() {
        if (interactor!=null && !interactor.isAdvertising()) {
            view.enableSendReceiveButtons(false);
            keepScreenOn(true);
            view.showAdvertisingProgressDialog(new P2pModeSelectContract.View.DialogCancelCallback() {
                @Override
                public void onCancelClicked(DialogInterface dialogInterface) {
                    keepScreenOn(false);
                    interactor.stopAdvertising();
                    dialogInterface.dismiss();
                    view.showP2PModeSelectFragment(true);
                    stopConnectionTimeout();
                }
            });

            interactor.startAdvertising(this);

            startConnectionTimeout(new OnConnectionTimeout() {
                @Override
                public void connectionTimeout(long duration, @Nullable Exception e) {
                    if (e == null) {
                        if (interactor != null && interactor.isAdvertising()) {
                            interactor.stopAdvertising();
                            view.removeAdvertisingProgressDialog();
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
                            Timber.e(view.getString(R.string.log_advertising_timed_while_not_in_advertising_mode));
                        }
                    }
                }
            });
        }
    }

    @Nullable
    @Override
    public SendingDevice getSendingDevice() {
        return currentSendingDevice;
    }

    @Override
    public void onStartedAdvertising(Object result) {
        // Do nothing for now
        // Continue showing the dialog box
    }

    @Override
    public void onAdvertisingFailed(@NonNull Exception e) {
        stopConnectionTimeout();

        view.removeAdvertisingProgressDialog();
        view.enableSendReceiveButtons(true);
    }

    @Override
    public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
        Timber.i(view.getString(R.string.log_connection_initiated_endpoint_auth_code), endpointId, connectionInfo.getEndpointName()
                , connectionInfo.getAuthenticationToken());
        stopConnectionTimeout();

        // Reject when already connected or the connecting device is blacklisted
        if (getCurrentPeerDevice() == null && !blacklistedDevices.contains(endpointId)) {
            setCurrentDevice(new DiscoveredDevice(endpointId, connectionInfo));
            getCurrentPeerDevice().setUsername(connectionInfo.getEndpointName());

            interactor.stopAdvertising();
            interactor.acceptConnection(endpointId, new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                    P2PReceiverPresenter.this.onPayloadReceived(endpointId, payload);
                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
                    // Do nothing for now
                    P2PReceiverPresenter.this.onPayloadTransferUpdate(s, payloadTransferUpdate);
                }
            });
        } else {
            Timber.e(view.getString(R.string.log_rejecting_connection_initiated_by_other_device)
                    , endpointId
                    , connectionInfo.getEndpointName()
                    , connectionInfo.getAuthenticationToken());
            // We can add connection support for multiple devices here later
            interactor.rejectConnection(endpointId);
        }
    }

    @Override
    public void onConnectionAccepted(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        if (currentSender != null) {
            connectionLevel = ConnectionLevel.CONNECT_BEFORE_AUTHENTICATE;
            interactor.connectedTo(endpointId);
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
                authorizationDetails.put(Constants.AuthorizationKeys.PEER_STATUS, Constants.PeerStatus.RECEIVER);
                sendAuthorizationDetails(authorizationDetails);
            }
        });
    }

    @Override
    public void onConnectionRejected(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        if (getCurrentPeerDevice() != null) {
            resetState();
            prepareForAdvertising(false);
        } else {
            Timber.e(view.getString(R.string.log_onconnectionrejected_without_peer_device), endpointId);
        }
    }

    @Override
    public void onConnectionUnknownError(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        //Todo: Go back to advertising mode
        //Todo: And show the user an error
        if (getCurrentPeerDevice() != null && endpointId.equals(getCurrentPeerDevice().getEndpointId())) {
            String errorMsg = String.format(view.getString(R.string.please_make_sure_device_is_turned_on_and_in_range), getCurrentPeerDevice().getEndpointName());

            disconnectAndReset(endpointId);

            view.showErrorFragment(view.getString(R.string.connection_lost), errorMsg, new ErrorFragment.OnOkClickCallback() {
                @Override
                public void onOkClicked() {
                    view.showP2PModeSelectFragment(true);
                }
            });
        } else {
            Timber.e(view.getString(R.string.onconnectionunknownerror_without_peer_device), endpointId);
        }
    }

    @Override
    public void onConnectionBroken(@NonNull String endpointId) {
        if (getCurrentPeerDevice() != null && endpointId.equals(getCurrentPeerDevice().getEndpointId())) {
            String errorMsg = String.format(view.getString(R.string.connection_to_endpoint_broken), endpointId);

            if (connectionLevel == ConnectionLevel.SENT_RECEIVED_HISTORY) {
                onSyncFailed(new Exception(errorMsg));
            } else {
                errorMsg = String.format(view.getString(R.string.please_make_sure_device_is_turned_on_and_in_range), getCurrentPeerDevice().getEndpointName());
                view.showErrorFragment(view.getString(R.string.connection_lost), errorMsg, new ErrorFragment.OnOkClickCallback() {
                    @Override
                    public void onOkClicked() {
                        view.showP2PModeSelectFragment(true);
                    }
                });
                disconnectAndReset(interactor.getCurrentEndpoint(), false);
            }
        } else {
            Timber.e(view.getString(R.string.log_onconnectionbroken_without_peer_device), endpointId);
        }
    }

    @Override
    public void onPayloadReceived(@NonNull final String endpointId, @NonNull Payload payload) {
        Timber.i(view.getString(R.string.log_received_payload_from_endpoint), endpointId);
        if (connectionLevel != null) {
            if (connectionLevel.equals(ConnectionLevel.CONNECT_BEFORE_AUTHENTICATE)) {
                // Process the command
                if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
                    String command = new String(payload.asBytes());

                    if (command.equals(Constants.Connection.SKIP_QR_CODE_SCAN)) {
                        String endpointName = getCurrentPeerDevice() != null ? getCurrentPeerDevice().getEndpointName() : "Unknown";
                        getView().removeQRCodeGeneratorFragment();
                        getView().showSkipQRScanDialog(Constants.PeerStatus.RECEIVER, endpointName, new SkipQRScanDialog.SkipDialogCallback() {
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

            } else if (connectionLevel.equals(ConnectionLevel.SENT_RECEIVED_HISTORY)) {
                processPayload(endpointId, payload);
            } else if (connectionLevel.equals(ConnectionLevel.AUTHENTICATED)) {
                // Authorize the connection from the details received
                performAuthorization(payload);
            } else if (connectionLevel.equals(ConnectionLevel.AUTHORIZED)) {
                if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null && !getView().isSyncProgressFragmentShowing()) {
                    showSyncProgressScreen();
                }

                // Waiting for the hash key
                processHashKey(endpointId, payload);
            }
        }
    }

    @Override
    public void processHashKey(@NonNull final String endpointId, @NonNull Payload payload) {
        if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
            String payloadAsString = new String(payload.asBytes());

            Map<String, Object> basicDeviceDetails = null;
            try {
                basicDeviceDetails = (Map<String, Object>) new Gson()
                        .fromJson(payloadAsString, Map.class);
            } catch (JsonSyntaxException e) {
                Timber.e(e);
            }

            if (basicDeviceDetails == null || basicDeviceDetails.get(Constants.BasicDeviceDetails.KEY_DEVICE_ID) == null) {
                Timber.e(view.getString(R.string.log_hash_key_sent_was_null));
                disconnectAndReset(endpointId);
            } else {
                connectionLevel = ConnectionLevel.RECEIVED_HASH_KEY;

                // Check if the device has been interacting with this app if it's state when it started
                // and now is the same
                // Should be done in the background
                checkIfDeviceKeyHasChanged(basicDeviceDetails, endpointId);
            }

        } else {
            Timber.e(view.getString(R.string.log_hash_key_was_sent_in_an_invalid_format));
            disconnectAndReset(endpointId);
        }
    }

    @Override
    public void sendLastReceivedRecords(@NonNull List<P2pReceivedHistory> receivedHistory) {
        if (getCurrentPeerDevice() != null) {
            interactor.sendMessage(new Gson().toJson(receivedHistory));
            connectionLevel = ConnectionLevel.SENT_RECEIVED_HISTORY;

            syncReceiverHandler = new SyncReceiverHandler(this);
        }
    }

    private SendingDevice registerSendingDevice(Map<String, Object> basicDeviceDetails) {
        SendingDevice sendingDevice = new SendingDevice();
        sendingDevice.setDeviceId((String) basicDeviceDetails.get(Constants.BasicDeviceDetails.KEY_DEVICE_ID));
        sendingDevice.setAppLifetimeKey((String) basicDeviceDetails.get(Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY));

        P2PLibrary.getInstance().getDb()
                .sendingDeviceDao()
                .insert(sendingDevice);

        return sendingDevice;
    }

    @VisibleForTesting
    @Nullable
    protected Integer clearDeviceHistoryAndUpdateDeviceKey(final SendingDevice sendingDevice, String appLifetimeKey) {
        final AppDatabase db = P2PLibrary.getInstance().getDb();
        sendingDevice.setAppLifetimeKey(appLifetimeKey);

        try {
            return db.runInTransaction(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    db.sendingDeviceDao().update(sendingDevice);
                    return db.p2pReceivedHistoryDao()
                            .clearDeviceRecords(sendingDevice.getDeviceId());
                }
            });
        } catch (RuntimeException e) {
            Timber.e(e);
            return null;
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
                onConnectionAuthorizationRejected(view.getString(R.string.reason_authorization_rejected_by_sender_details_invalid));
            } else {
                getCurrentPeerDevice().setAuthorizationDetails(map);
                P2PLibrary.getInstance().getP2PAuthorizationService()
                        .authorizeConnection(map, P2PReceiverPresenter.this);
            }
        } else {
            onConnectionAuthorizationRejected(view.getString(R.string.reason_authorization_rejected_by_sender_details_invalid));
        }
    }

    @Override
    public void processPayload(@NonNull String endpointId, @NonNull Payload payload) {
        if (syncReceiverHandler != null) {
            syncReceiverHandler.processPayload(endpointId, payload);
        }
    }

    @Override
    public void disconnectAndReset(@NonNull String endpointId, boolean startAdvertising) {
        interactor.disconnectFromEndpoint(endpointId);
        interactor.connectedTo(null);
        resetState();

        if (startAdvertising) {
            prepareForAdvertising(false);
        }
    }

    @Override
    public void disconnectAndReset(@NonNull String endpointId) {
        disconnectAndReset(endpointId, true);
    }

    private void checkIfDeviceKeyHasChanged(@NonNull final Map<String, Object> basicDeviceDetails, final @NonNull String endpointId) {
        Tasker.run(new Callable<SendingDevice>() {
            @Override
            public SendingDevice call() throws Exception {
                return P2PLibrary.getInstance().getDb()
                        .sendingDeviceDao()
                        .getSendingDevice((String) basicDeviceDetails.get(Constants.BasicDeviceDetails.KEY_DEVICE_ID));
            }
        }, new GenericAsyncTask.OnFinishedCallback<SendingDevice>() {
            @Override
            public void onSuccess(@Nullable SendingDevice result) {
                if (result != null) {
                    final SendingDevice sendingDevice = result;
                    final String appLifetimeKey = (String) basicDeviceDetails.get(Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY);

                    if (sendingDevice.getAppLifetimeKey()
                            .equals(appLifetimeKey)) {
                        currentSendingDevice = sendingDevice;
                        Tasker.run(new Callable<List<P2pReceivedHistory>>() {
                            @Override
                            public List<P2pReceivedHistory> call() throws Exception {
                                return P2PLibrary.getInstance().getDb()
                                        .p2pReceivedHistoryDao()
                                        .getDeviceReceivedHistory(sendingDevice.getDeviceId());
                            }
                        }, new GenericAsyncTask.OnFinishedCallback<List<P2pReceivedHistory>>() {
                            @Override
                            public void onSuccess(@Nullable List<P2pReceivedHistory> result) {
                                sendLastReceivedRecords(result != null ? result :new ArrayList<P2pReceivedHistory>());
                            }

                            @Override
                            public void onError(Exception e) {
                                Timber.e(e);
                                disconnectAndReset(endpointId);
                            }
                        });

                    } else {
                        // Clear the device history records && update device app key
                        Tasker.run(new Callable<Integer>() {
                            @Override
                            public Integer call() throws Exception {
                                sendingDevice.setAppLifetimeKey(appLifetimeKey);
                                currentSendingDevice = sendingDevice;
                                return clearDeviceHistoryAndUpdateDeviceKey(sendingDevice, appLifetimeKey);
                            }
                        }, new GenericAsyncTask.OnFinishedCallback<Integer>() {
                            @Override
                            public void onSuccess(@Nullable Integer result) {
                                if (result != null) {
                                    Timber.e(view.getString(R.string.log_records_deleted), (int) result);
                                    sendLastReceivedRecords(new ArrayList<P2pReceivedHistory>());
                                } else {
                                    onError(new Exception("Clear device history and update device key Transaction failed!"));
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                Timber.e(view.getString(R.string.log_error_occurred_trying_to_delete_p2p_received_history_on_device)
                                        , sendingDevice.getDeviceId());
                                disconnectAndReset(endpointId);
                            }
                        });

                    }
                } else {
                    // This is a new device we should save it
                    Tasker.run(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            currentSendingDevice = registerSendingDevice(basicDeviceDetails);
                            return null;
                        }
                    }, new GenericAsyncTask.OnFinishedCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void result) {
                            sendLastReceivedRecords(new ArrayList<P2pReceivedHistory>());
                        }

                        @Override
                        public void onError(Exception e) {
                            Timber.e(e);

                            disconnectAndReset(endpointId);
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                Timber.e(e);
                disconnectAndReset(endpointId);
            }
        });
    }

    @Override
    public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
        // Do nothing for now
        if (syncReceiverHandler != null) {
            syncReceiverHandler.onPayloadTransferUpdate(endpointId, update);
        }
    }

    @Override
    public void onDisconnected(@NonNull String endpointId) {
        if (getCurrentPeerDevice() != null && endpointId.equals(getCurrentPeerDevice().getEndpointId())) {
            Timber.e(view.getString(R.string.log_endpoint_lost), endpointId);
            resetState();
            prepareForAdvertising(false);
        } else {
            Timber.e(view.getString(R.string.log_ondisconnected_without_peer_device), endpointId);
        }
    }

    @Override
    public void sendAuthorizationDetails(@NonNull Map<String, Object> authorizationDetails) {
        interactor.sendMessage(new Gson().toJson(authorizationDetails));
    }

    @Override
    public void onAuthenticationSuccessful() {
        if (getCurrentPeerDevice() != null) {
            connectionLevel = ConnectionLevel.AUTHENTICATED;
            startDeviceAuthorization(getCurrentPeerDevice().getEndpointId());
        } else {
            Timber.e(view.getString(R.string.log_onauthenticationsuccessful_without_peer_device));
        }
    }

    @Override
    public void onAuthenticationFailed(@NonNull String reason, @NonNull Exception exception) {
        // Reject the connection
        if (getCurrentPeerDevice() != null) {
            String endpointId = getCurrentPeerDevice().getEndpointId();
            rejectDeviceOnAuthentication(endpointId);
            disconnectAndReset(endpointId);
        }

        view.showErrorFragment(view.getString(R.string.connection_lost), reason, new ErrorFragment.OnOkClickCallback() {
            @Override
            public void onOkClicked() {
                view.showP2PModeSelectFragment(true);
            }
        });

        //Todo: Go back to advertising mode
        Timber.e(exception, view.getString(R.string.authentication_failed));
        // The rest will be handled in the rejectConnection callback
        // Todo: test is this is causing an error where the discovering mode can no longer be restarted
        // if the receiving device app is either removed or discovering cancelled while the receiver is showing
        // the QR code dialog
    }

    @Override
    public void onAuthenticationCancelled(@NonNull String reason) {
        // Reject the connection
        if (getCurrentPeerDevice() != null) {
            String endpointId = getCurrentPeerDevice().getEndpointId();
            rejectDeviceOnAuthentication(endpointId);
            disconnectAndReset(endpointId);
        }

        prepareForAdvertising(false);

        // Go back to discovering mode
        Timber.e(view.getString(R.string.log_authentication_cancelled), reason);
    }

    private void resetState() {
        hasAcceptedConnection = false;
        syncReceiverHandler = null;
        connectionLevel = null;
        view.dismissAllDialogs();
        view.enableSendReceiveButtons(true);
        setCurrentDevice(null);
        currentSendingDevice = null;
    }

    @Override
    public void onConnectionAuthorized() {
        connectionLevel = ConnectionLevel.AUTHORIZED;

        view.showDevicesConnectedFragment(new P2pModeSelectContract.View.OnStartTransferClicked() {
            @Override
            public void startTransferClicked() {
                sendStartTransfer();
                showSyncProgressScreen();
            }
        });
    }

    public void showSyncProgressScreen() {
        view.showSyncProgressFragment(view.getString(R.string.receiving_data), new SyncProgressFragment.SyncProgressDialogCallback() {
            @Override
            public void onCancelClicked() {
                if (interactor != null && interactor.getCurrentEndpoint() != null) {
                    onSyncFailed(new Exception("User cancelled sync process"));
                } else {
                    Timber.e(view.getString(R.string.could_not_disconnection_reset_without_endpoint));
                }
            }
        });
    }

    @Override
    public void sendStartTransfer() {
        interactor.sendMessage(Constants.Connection.START_TRANSFER);
    }

    @Override
    public void onConnectionAuthorizationRejected(@NonNull String reason) {
        //Timber.e(reason);
        // Disconnect from the endpoint
        if (currentSender != null) {
            String endpointId = currentSender.getEndpointId();
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
            prepareForAdvertising(false);
        }
    }

    public void onSyncFailed(@NonNull Exception e) {
        SyncFinishedCallback syncFinishedCallback = P2PLibrary.getInstance().getSyncFinishedCallback();
        if (syncFinishedCallback != null && syncReceiverHandler != null) {
            syncFinishedCallback.onFailure(e, syncReceiverHandler.getTransferProgress());
        }

        if (syncReceiverHandler != null) {
            String peerDeviceName = getCurrentPeerDevice() != null ? getCurrentPeerDevice().getEndpointName() : null;
            getView().showSyncCompleteFragment(false, peerDeviceName, new SyncCompleteTransferFragment.OnCloseClickListener() {
                @Override
                public void onCloseClicked() {
                    getView().showP2PModeSelectFragment(true);
                }
            }, SyncDataConverterUtil.generateSummaryReport(getView().getContext(), false, syncReceiverHandler.getTransferProgress())
            , false);
        }

        disconnectAndReset(interactor.getCurrentEndpoint(), false);
    }

    public void performDeviceAuthentication() {
        // First stop advertising
        keepScreenOn(false);
        view.removeAdvertisingProgressDialog();

        // This can be moved to the library for easy customisation by host applications
        BaseSyncConnectionAuthenticator syncConnectionAuthenticator = new ReceiverConnectionAuthenticator(this);
        syncConnectionAuthenticator.authenticate(getCurrentPeerDevice(), this);
    }

    @Nullable
    @Override
    public DiscoveredDevice getCurrentPeerDevice() {
        return currentSender;
    }

    @Override
    public void setCurrentDevice(@Nullable DiscoveredDevice discoveredDevice) {
        currentSender = discoveredDevice;
        keepScreenOn(discoveredDevice != null);
    }
}
