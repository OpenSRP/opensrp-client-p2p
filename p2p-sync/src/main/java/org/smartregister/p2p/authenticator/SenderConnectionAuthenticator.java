package org.smartregister.p2p.authenticator;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.vision.barcode.Barcode;

import org.smartregister.p2p.R;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.fragment.QRCodeScanningFragment;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.util.Constants;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 14/03/2019
 */

public class SenderConnectionAuthenticator extends BaseSyncConnectionAuthenticator {

    public SenderConnectionAuthenticator(@NonNull P2pModeSelectContract.SenderPresenter senderPresenter) {
        super(senderPresenter);
    }

    @Override
    public void authenticate(@NonNull final DiscoveredDevice discoveredDevice, @NonNull final AuthenticationCallback authenticationCallback) {
        if (discoveredDevice.getConnectionInfo() != null
                && !discoveredDevice.getConnectionInfo().isIncomingConnection()) {

            if (allowSkipQrCodeScan()) {
                displaySenderApprovalDialog(discoveredDevice, authenticationCallback);
            } else {
                displayQRCodeScanFragment(discoveredDevice, authenticationCallback);
            }

        } else {
            authenticationCallback.onAuthenticationFailed(getPresenter().getView().getString(R.string.device_information_passed_is_invalid)
                    , new Exception("DiscoveredDevice information passed is invalid"));
        }
    }

    private void displayQRCodeScanFragment(@NonNull final DiscoveredDevice discoveredDevice, @NonNull final AuthenticationCallback authenticationCallback) {
        final ConnectionInfo connectionInfo = discoveredDevice.getConnectionInfo();
        getPresenter().getView().showQRCodeScanningFragment(discoveredDevice.getEndpointName(), new QRCodeScanningFragment.QRCodeScanDialogCallback() {
            @Override
            public void qrCodeScanned(final @NonNull SparseArray<Barcode> qrCodeResult) {
                getPresenter().getView().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String authenticationCode = connectionInfo.getAuthenticationToken();
                        boolean authenticationCodeFound = false;

                        for (int i = 0; i < qrCodeResult.size(); i++) {
                            if (authenticationCode.equals(qrCodeResult.valueAt(i).rawValue)) {
                                authenticationCodeFound = true;
                                break;
                            }
                        }

                        if (authenticationCodeFound) {
                            getPresenter().sendConnectionAccept();
                            authenticationCallback.onAuthenticationSuccessful();
                        } else {
                            authenticationCallback.onAuthenticationFailed(getPresenter().getView().getString(R.string.authentication_tokens_dont_match)
                                    , new Exception("Authentication tokens do not match"));
                        }
                    }
                });
            }

            @Override
            public void onErrorOccurred(@NonNull Exception e) {
                Timber.e(e);
            }

            @Override
            public void onSkipClicked() {
                getPresenter().sendSkipClicked();
                getPresenter().getView().showConnectingDialog(new P2pModeSelectContract.View.DialogCancelCallback() {
                    @Override
                    public void onCancelClicked(DialogInterface dialogInterface) {
                        authenticationCallback.onAuthenticationCancelled("User rejected the connection");
                    }
                });
            }
        });
    }

    private void displaySenderApprovalDialog(@NonNull final DiscoveredDevice discoveredDevice, @NonNull final AuthenticationCallback authenticationCallback) {
        getPresenter().getView().showSenderApprovalDialog(discoveredDevice.getConnectionInfo().getAuthenticationToken()
                , discoveredDevice.getEndpointName(),
                new P2pModeSelectContract.View.DialogCancelCallback() {
                    @Override
                    public void onCancelClicked(DialogInterface dialogInterface) {
                        getPresenter().getView().removeDialog(Constants.Dialog.DISPLAY_APPROVAL_KEY);
                        authenticationCallback.onAuthenticationCancelled("User rejected the connection");
                    }
                },
                new P2pModeSelectContract.View.DialogApprovedCallback() {
                    @Override
                    public void onApprovedClicked(DialogInterface dialogInterface) {
                        getPresenter().getView().removeDialog(Constants.Dialog.DISPLAY_APPROVAL_KEY);
                        getPresenter().sendConnectionAccept();
                        authenticationCallback.onAuthenticationSuccessful();
                    }
                });
    }

}
