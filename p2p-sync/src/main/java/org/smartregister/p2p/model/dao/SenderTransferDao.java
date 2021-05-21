package org.smartregister.p2p.model.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.sync.data.JsonData;
import org.smartregister.p2p.sync.data.MultiMediaData;

import java.util.TreeSet;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public interface SenderTransferDao {

    @WorkerThread
    @Nullable
    TreeSet<DataType> getDataTypes();

    @WorkerThread
    @Nullable
    JsonData getJsonData(@NonNull DataType dataType, long lastRecordId, int batchSize);

    @WorkerThread
    @Nullable
    MultiMediaData getMultiMediaData(@NonNull DataType dataType, long lastRecordId);
}
