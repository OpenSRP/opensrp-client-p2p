package org.smartregister.p2p.authenticator;

import android.support.annotation.NonNull;

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.sync.DiscoveredDevice;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 14/03/2019
 */

public abstract class BaseSyncConnectionAuthenticator {

    protected P2pModeSelectContract.BasePresenter presenter;

    public BaseSyncConnectionAuthenticator(@NonNull P2pModeSelectContract.BasePresenter presenter) {
        this.presenter = presenter;
    }

    public P2pModeSelectContract.BasePresenter getPresenter() {
        return presenter;
    }

    public abstract void authenticate(@NonNull DiscoveredDevice discoveredDevice, @NonNull final AuthenticationCallback authenticationCallback);

    public interface AuthenticationCallback {

        void onAuthenticationSuccessful();

        void onAuthenticationFailed(@NonNull String reason, @NonNull Exception exception);

        void onAuthenticationCancelled(@NonNull String reason);
    }

    public boolean allowSkipQrCodeScan(){
        return P2PLibrary.getInstance().getOptions().allowSkipQrCodeScan();
    }
}
