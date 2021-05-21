package org.smartregister.p2p.model.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import org.smartregister.p2p.model.P2pReceivedHistory;

import java.util.List;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */

@Dao
public interface P2pReceivedHistoryDao {

    @Insert
    void addReceivedHistory(@NonNull P2pReceivedHistory receivedHistory);

    @Update
    void updateReceivedHistory(@NonNull P2pReceivedHistory receivedHistory);

    @Query("DELETE FROM p2p_received_history WHERE sending_device_id = :sendingDeviceId")
    int clearDeviceRecords(@NonNull String sendingDeviceId);

    @Query("SELECT * FROM p2p_received_history WHERE sending_device_id = :sendingDeviceId")
    List<P2pReceivedHistory> getDeviceReceivedHistory(@NonNull String sendingDeviceId);

    @Nullable
    @Query("SELECT * FROM p2p_received_history WHERE sending_device_id = :sendingDeviceId AND entity_type = :entityType LIMIT 1")
    P2pReceivedHistory getHistory(@NonNull String sendingDeviceId, @NonNull String entityType);
}
