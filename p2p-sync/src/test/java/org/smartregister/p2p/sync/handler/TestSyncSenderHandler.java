package org.smartregister.p2p.sync.handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.P2pReceivedHistory;

import java.util.List;
import java.util.TreeSet;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 2019-06-12
 */

public class TestSyncSenderHandler extends SyncSenderHandler{

    public TestSyncSenderHandler(@NonNull P2pModeSelectContract.SenderPresenter presenter, @NonNull TreeSet<DataType> dataSyncOrder, @Nullable List<P2pReceivedHistory> receivedHistory) {
        super(presenter, dataSyncOrder, receivedHistory);
    }

    @Override
    public void startNewThread(@NonNull Runnable runnable) {
        runnable.run();
    }
}
