package org.smartregister.p2p.model.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.annotation.NonNull;

import org.smartregister.p2p.model.SendingDevice;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */

@Dao
public interface SendingDeviceDao {

    @Insert
    void insert(@NonNull SendingDevice sendingDevice);

    @Update
    void update(@NonNull SendingDevice sendingDevice);

    @Query("SELECT * FROM sending_devices WHERE device_id = :deviceUniqueId LIMIT 1")
    SendingDevice getSendingDevice(@NonNull String deviceUniqueId);
}
