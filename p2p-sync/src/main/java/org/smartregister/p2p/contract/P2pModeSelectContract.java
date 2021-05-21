package org.smartregister.p2p.contract;

import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;

import org.smartregister.p2p.callback.OnResultCallback;
import org.smartregister.p2p.dialog.SkipQRScanDialog;
import org.smartregister.p2p.fragment.ErrorFragment;
import org.smartregister.p2p.fragment.QRCodeGeneratorFragment;
import org.smartregister.p2p.fragment.QRCodeScanningFragment;
import org.smartregister.p2p.fragment.SyncCompleteTransferFragment;
import org.smartregister.p2p.fragment.SyncProgressFragment;
import org.smartregister.p2p.model.SendingDevice;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.sync.IReceiverSyncLifecycleCallback;
import org.smartregister.p2p.sync.ISenderSyncLifecycleCallback;
import org.smartregister.p2p.sync.data.SyncPackageManifest;

import java.util.List;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public interface P2pModeSelectContract {

    interface View extends BaseView {

        void enableSendReceiveButtons(boolean enable);

        void showP2PModeSelectFragment(boolean enableButtons);

        void showAdvertisingProgressDialog(@NonNull DialogCancelCallback dialogCancelCallback);

        void showSyncProgressFragment(@NonNull String title, @NonNull SyncProgressFragment.SyncProgressDialogCallback syncProgressDialogCallback);

        void updateProgressFragment(@NonNull String progress, @NonNull String summary);

        void updateProgressFragment(int progress);

        boolean removeAdvertisingProgressDialog();

        void showSyncCompleteFragment(boolean isSuccess, @Nullable String deviceName, @NonNull SyncCompleteTransferFragment.OnCloseClickListener onCloseClickListener, @NonNull String summaryReport, boolean isSender);

        void showDiscoveringProgressDialog(@NonNull DialogCancelCallback dialogCancelCallback);

        boolean removeDiscoveringProgressDialog();

        void showQRCodeScanningFragment(@NonNull String deviceName, @NonNull QRCodeScanningFragment.QRCodeScanDialogCallback qrCodeScanDialogCallback);

        void removeQRCodeScanningFragment();

        void showQRCodeGeneratorFragment(@NonNull String authenticationCode, @NonNull String deviceName
                , @NonNull QRCodeGeneratorFragment.QRCodeGeneratorCallback qrCodeGeneratorCallback);

        void removeQRCodeGeneratorFragment();

        void showConnectionAcceptDialog(@NonNull String receiverDeviceName, @NonNull String authenticationCode
                , @NonNull DialogInterface.OnClickListener onClickListener);

        void showConnectingDialog(@NonNull DialogCancelCallback dialogCancelCallback);

        void removeConnectingDialog();

        void showSkipQRScanDialog(@NonNull String peerDeviceStatus, @NonNull String deviceName, @NonNull SkipQRScanDialog.SkipDialogCallback skipDialogCallback);

        boolean removeSkipQRScanDialog();

        void showErrorFragment(@NonNull String title, @NonNull String message, @Nullable ErrorFragment.OnOkClickCallback onOkClickCallback);

        void showDevicesConnectedFragment(@NonNull OnStartTransferClicked onStartTransferClicked);

        boolean isSyncProgressFragmentShowing();

        void requestPermissions(@NonNull List<String> unauthorisedPermissions);

        @NonNull
        List<String> getUnauthorisedPermissions();

        boolean isLocationEnabled();

        void dismissAllDialogs();

        void requestEnableLocation(@NonNull OnLocationEnabled onLocationEnabled);

        @NonNull
        String getString(@StringRes int resId);

        interface DialogCancelCallback {
            void onCancelClicked(DialogInterface dialogInterface);
        }

        interface OnLocationEnabled {
            void locationEnabled();
        }

        interface OnStartTransferClicked {
            void startTransferClicked();
        }

    }

    interface P2PModeSelectView {

        void enableSendReceiveButtons(boolean enable);

    }

    interface BasePresenter {

        void onStop();

        /**
         * Sends a payload to the other device and returns the payloadId which can be used to track the
         * transfer progress of the payload/message
         *
         * @param message the string message
         * @return the payloadId
         */
        long sendTextMessage(@NonNull String message);

        @NonNull
        View getView();

        @Nullable
        DiscoveredDevice getCurrentPeerDevice();

        void setCurrentDevice(@Nullable DiscoveredDevice discoveredDevice);

        void addDeviceToRejectedList(@NonNull String endpointId);

        boolean addDeviceToBlacklist(@NonNull String endpointId);

        void rejectDeviceOnAuthentication(@NonNull String endpointId);

        void disconnectAndReset(@NonNull String endpointId);

        void sendSkipClicked();

        void sendConnectionAccept();

        void startConnectionTimeout(@NonNull OnConnectionTimeout onConnectionTimeout);

        interface OnConnectionTimeout {

            void connectionTimeout(long duration, @Nullable Exception e);
        }
    }

    interface ReceiverPresenter extends BasePresenter {

        void onReceiveButtonClicked();

        void prepareForAdvertising(boolean returningFromRequestingPermissions);

        void startAdvertisingMode();

        @Nullable
        SendingDevice getSendingDevice();

        void disconnectAndReset(@NonNull String endpointId, boolean startAdvertising);

        void sendStartTransfer();

    }

    interface SenderPresenter extends BasePresenter {

        void onSendButtonClicked();

        void prepareForDiscovering(boolean returningFromRequestingPermissions);

        void startDiscoveringMode();

        void sendSyncComplete();

        long sendManifest(@NonNull SyncPackageManifest syncPackageManifest);

        void sendPayload(@NonNull Payload payload);

        void errorOccurredSync(@NonNull Exception e);

        void disconnectAndReset(@NonNull String endpointId, boolean startDiscovering);

        void startTransfer();

    }

    interface Interactor extends BaseInteractor {

        void startAdvertising(@NonNull final IReceiverSyncLifecycleCallback iReceiverSyncLifecycleCallback);

        void stopAdvertising();

        boolean isAdvertising();

        void startDiscovering(@NonNull final ISenderSyncLifecycleCallback iSenderSyncLifecycleCallback);

        void stopDiscovering();

        boolean isDiscovering();

        void closeAllEndpoints();

        void disconnectFromEndpoint(@NonNull String endpointId);

        void cleanOngoingConnectionResources();

        /**
         * Sends a payload to the other device and returns the payloadId which can be used to track the
         * transfer progress of the payload/message
         *
         * @param message the string message
         * @return the payloadId
         */
        long sendMessage(@NonNull String message);

        void sendPayload(@NonNull Payload payload);

        void connectedTo(@Nullable String endpointId);

        @Nullable
        String getCurrentEndpoint();

        @NonNull
        String getAppPackageName();

        @NonNull
        String getUserNickName();

        @NonNull
        Context getContext();

        void requestConnection(@NonNull String endpointId
                , @NonNull OnResultCallback onRequestConnectionResult
                , @NonNull ConnectionLifecycleCallback connectionLifecycleCallback);

        void acceptConnection(@NonNull String endpointId, PayloadCallback payloadCallback);

        void rejectConnection(@NonNull String endpointId);
    }
}
