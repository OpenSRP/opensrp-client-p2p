package org.smartregister.p2p.sync.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.smartregister.p2p.model.DataType;

import java.util.HashMap;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */
public class SyncPackageManifest {

    private long payloadId;
    private String payloadExtension;
    private DataType dataType;
    private int recordsSize;
    private int payloadSize;
    private HashMap<String, Object> payloadDetails;

    public SyncPackageManifest(long payloadId, @NonNull String payloadExtension, @NonNull DataType dataType, int recordsSize) {
        this.payloadId = payloadId;
        this.payloadExtension = payloadExtension;
        this.dataType = dataType;
        this.recordsSize = recordsSize;
    }

    public void setPayloadSize(int payloadSize) {
        this.payloadSize = payloadSize;
    }

    public int getPayloadSize() {
        return payloadSize;
    }

    public long getPayloadId() {
        return payloadId;
    }

    public String getPayloadExtension() {
        return payloadExtension;
    }

    public DataType getDataType() {
        return dataType;
    }

    @Nullable
    public HashMap<String, Object> getPayloadDetails() {
        return payloadDetails;
    }

    public void setPayloadDetails(@Nullable HashMap<String, Object> payloadDetails) {
        this.payloadDetails = payloadDetails;
    }

    public int getRecordsSize() {
        return recordsSize;
    }
}
