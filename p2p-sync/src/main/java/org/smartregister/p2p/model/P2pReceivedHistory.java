package org.smartregister.p2p.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */

@Entity(tableName = "p2p_received_history", primaryKeys = {"entity_type", "sending_device_id"})
public class P2pReceivedHistory {

    @NonNull
    @ColumnInfo(name = "sending_device_id")
    private String sendingDeviceId;

    @NonNull
    @ColumnInfo(name = "entity_type")
    private String entityType;

    @ColumnInfo(name = "last_record_id")
    private long lastRecordId;

    public String getSendingDeviceId() {
        return sendingDeviceId;
    }

    public void setSendingDeviceId(String sendingDeviceId) {
        this.sendingDeviceId = sendingDeviceId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public long getLastRecordId() {
        return lastRecordId;
    }

    public void setLastRecordId(long lastRecordId) {
        this.lastRecordId = lastRecordId;
    }
}
