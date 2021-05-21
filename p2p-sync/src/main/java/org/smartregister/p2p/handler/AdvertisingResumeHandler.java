package org.smartregister.p2p.handler;

import androidx.annotation.NonNull;
import org.smartregister.p2p.contract.P2pModeSelectContract;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 11/03/2019
 */

public class AdvertisingResumeHandler implements OnResumeHandler {

    private P2pModeSelectContract.ReceiverPresenter receiverPresenter;
    private P2pModeSelectContract.Interactor interactor;


    public AdvertisingResumeHandler(@NonNull P2pModeSelectContract.ReceiverPresenter receiverPresenter
            , P2pModeSelectContract.Interactor interactor) {
        this.receiverPresenter = receiverPresenter;
        this.interactor = interactor;
    }


    @Override
    public void onResume() {
        if (interactor.isAdvertising()) {
            receiverPresenter.prepareForAdvertising(false);
        }
    }
}
