package org.smartregister.p2p.sample.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.dao.ReceiverTransferDao;
import org.smartregister.p2p.sample.util.Constants;

import java.io.File;
import java.util.HashMap;
import java.util.TreeSet;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public class SampleReceiverDao implements ReceiverTransferDao {

    private HashMap<String, Long> lastReceived = new HashMap<>();

    @Override
    public TreeSet<DataType> getDataTypes() {
        TreeSet<DataType> dataTypes = new TreeSet<>();
        dataTypes.add(new DataType(Constants.NAMES, DataType.Type.NON_MEDIA, 0));
        dataTypes.add(new DataType(Constants.PERSONAL_DETAIL, DataType.Type.NON_MEDIA, 1));
        dataTypes.add(new DataType(Constants.PROFILE_PIC, DataType.Type.MEDIA, 2));

        return dataTypes;
    }

    @Override
    public long receiveMultimedia(@NonNull DataType dataType, @NonNull File file, @Nullable HashMap<String, Object> multimediaDetails, long fileRecordId) {
        Timber.e("Received multi-media record %s of type %s", multimediaDetails.get("name"), dataType.getName());

        file.renameTo(new File(String.format("/sdcard/%s.%s", System.currentTimeMillis(), "png")));

        if (multimediaDetails != null) {
            return (new Double((double) multimediaDetails.get("fileRecordId"))).longValue();
        } else {
            return -1;
        }
    }

    @Override
    public long receiveJson(@NonNull DataType type, @NonNull JSONArray jsonArray) {
        Timber.e("Received records %s of type %s", String.valueOf(jsonArray.length()), type.getName());

        Long lastId = lastReceived.get(type.getName());

        lastId = lastId != null ? lastId : 0l;

        long finalLastId = lastId + jsonArray.length();

        lastReceived.put(type.getName(), finalLastId);

        Timber.e("Last record id of received records %s is %s", type.getName(), String.valueOf(finalLastId));

        return finalLastId;
    }
}
