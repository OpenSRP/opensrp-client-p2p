package org.smartregister.p2p.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */

public class Settings {

    private SharedPreferences sharedPreferences;

    public Settings(@NonNull Context context) {
        sharedPreferences = context
                .getSharedPreferences(Constants.Prefs.NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    public String getHashKey() {
        return sharedPreferences.getString(Constants.Prefs.KEY_HASH, null);

    }

    public void saveHashKey(@NonNull String hashKey) {
        sharedPreferences.edit()
                .putString(Constants.Prefs.KEY_HASH, hashKey)
                .apply();
    }
}
