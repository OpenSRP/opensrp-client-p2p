package org.smartregister.p2p.shadows;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.smartregister.p2p.model.AppDatabase;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */

@Implements(AppDatabase.class)
public class ShadowAppDatabase {

    private static AppDatabase instance;

    @Implementation
    public static AppDatabase getInstance(@NonNull Context context, @NonNull String passphrase) {
        if (instance == null) {
            instance = Room.inMemoryDatabaseBuilder(context.getApplicationContext(), AppDatabase.class)
                    .allowMainThreadQueries()
                    .build();
        }

        return instance;
    }

    public static void setInstance(@NonNull AppDatabase appDatabase) {
        instance = appDatabase;
    }
}
