package org.smartregister.p2p.model.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.smartregister.p2p.model.DataType;

import java.io.File;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public interface ReceiverTransferDao {

    @WorkerThread
    TreeSet<DataType> getDataTypes();

    @WorkerThread
    long receiveJson(@NonNull DataType type, @NonNull JSONArray jsonArray);

    /**
     * Process the multimedia and perform other operations as if in a worker thread. This is being called
     * on a worker thread.
     *
     * @param dataType
     * @param file
     * @return > -1 if the process was successful, < 0 if the media was not processed successfully
     */
    @WorkerThread
    long receiveMultimedia(@NonNull DataType dataType, @NonNull File file, @Nullable HashMap<String, Object> multimediaDetails, long fileRecordId);

}
