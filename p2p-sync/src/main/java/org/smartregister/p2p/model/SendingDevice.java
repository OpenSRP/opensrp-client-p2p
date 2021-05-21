package org.smartregister.p2p.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */
@Entity(tableName = "sending_devices")
public class SendingDevice {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "device_id")
    private String deviceId;

    @NonNull
    @ColumnInfo(name = "app_lifetime_key")
    private String appLifetimeKey;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAppLifetimeKey() {
        return appLifetimeKey;
    }

    public void setAppLifetimeKey(String appLifetimeKey) {
        this.appLifetimeKey = appLifetimeKey;
    }
}
