package org.smartregister.p2p.authenticator;

import android.content.DialogInterface;

import androidx.annotation.NonNull;

import org.smartregister.p2p.R;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.fragment.QRCodeGeneratorFragment;
import org.smartregister.p2p.sync.DiscoveredDevice;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 14/03/2019
 */

public class ReceiverConnectionAuthenticator extends BaseSyncConnectionAuthenticator {

    public ReceiverConnectionAuthenticator(@NonNull P2pModeSelectContract.BasePresenter basePresenter) {
        super(basePresenter);
    }

    @Override
    public void authenticate(@NonNull final DiscoveredDevice discoveredDevice, @NonNull final AuthenticationCallback authenticationCallback) {
        if (discoveredDevice.getConnectionInfo() != null
                && discoveredDevice.getConnectionInfo().isIncomingConnection()) {

            getPresenter().getView().showQRCodeGeneratorFragment(discoveredDevice.getConnectionInfo().getAuthenticationToken()
                    , discoveredDevice.getEndpointName()
                    , new QRCodeGeneratorFragment.QRCodeGeneratorCallback() {

                        @Override
                        public void onSkipped() {
                            getPresenter().sendSkipClicked();
                            getPresenter().getView().showConnectingDialog(new P2pModeSelectContract.View.DialogCancelCallback() {
                                @Override
                                public void onCancelClicked(DialogInterface dialogInterface) {
                                    getPresenter().getView().removeConnectingDialog();
                                    authenticationCallback.onAuthenticationCancelled("User rejected the connection");
                                }
                            });
                        }

                        @Override
                        public void onErrorOccurred(@NonNull Exception e) {
                            authenticationCallback.onAuthenticationFailed(getPresenter().getView().getString(R.string.unknown_error_occurred), e);
                        }
                    });

        } else {
            authenticationCallback.onAuthenticationFailed(getPresenter().getView().getString(R.string.device_information_passed_is_invalid)
                    , new Exception("DiscoveredDevice information passed is invalid"));
        }
    }
}
