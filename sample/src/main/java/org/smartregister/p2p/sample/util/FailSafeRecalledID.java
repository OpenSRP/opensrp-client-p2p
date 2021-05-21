package org.smartregister.p2p.sample.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import org.smartregister.p2p.contract.RecalledIdentifier;
import org.smartregister.p2p.util.Constants;
import org.smartregister.p2p.util.Device;

import java.util.UUID;

public class FailSafeRecalledID implements RecalledIdentifier {

    private static final String FAIL_SAFE_ID = "P2P_FAIL_SAFE_ID";

    @NonNull
    @Override
    public String getUniqueID(Context context) {
        String uniqueAddress = Device.getMacAddress();

        if (uniqueAddress == null) {
            // save a uuid in
            SharedPreferences sharedPreferences =
                    context.getSharedPreferences(Constants.Prefs.NAME, Context.MODE_PRIVATE);

            uniqueAddress = sharedPreferences.getString(FAIL_SAFE_ID, null);

            if (uniqueAddress == null) {
                uniqueAddress = UUID.randomUUID().toString();
                sharedPreferences.edit().putString(FAIL_SAFE_ID, uniqueAddress).apply();
            }
        }
        return uniqueAddress;
    }
}
