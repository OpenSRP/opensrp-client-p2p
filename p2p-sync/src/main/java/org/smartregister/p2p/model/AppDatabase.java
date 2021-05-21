package org.smartregister.p2p.model;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import androidx.annotation.NonNull;
import android.text.SpannableStringBuilder;

import com.commonsware.cwac.saferoom.SafeHelperFactory;

import org.smartregister.p2p.model.dao.P2pReceivedHistoryDao;
import org.smartregister.p2p.model.dao.SendingDeviceDao;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */

@Database(entities = {SendingDevice.class, P2pReceivedHistory.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;
    public static final String DB_NAME = "p2p";

    public static AppDatabase getInstance(@NonNull Context context, @NonNull String passphrase) {
        if (instance == null) {
            SafeHelperFactory safeHelperFactory = SafeHelperFactory.fromUser(new SpannableStringBuilder(passphrase));

            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, DB_NAME)
                    .openHelperFactory(safeHelperFactory)
                    .build();
        }

        return instance;
    }

    public abstract SendingDeviceDao sendingDeviceDao();

    public abstract P2pReceivedHistoryDao p2pReceivedHistoryDao();

}
