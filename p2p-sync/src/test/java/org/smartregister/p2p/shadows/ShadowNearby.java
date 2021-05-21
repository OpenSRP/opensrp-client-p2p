package org.smartregister.p2p.shadows;

import android.content.Context;
import androidx.annotation.NonNull;

import com.google.android.gms.internal.nearby.zzbd;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionsClient;

import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 11/03/2019
 */

@Implements(Nearby.class)
@Config(shadows = {Shadowzzbd.class})
public class ShadowNearby {

    @Implementation
    public static final ConnectionsClient getConnectionsClient(@NonNull Context var0) {
        return ReflectionHelpers.newInstance(zzbd.class);
    }

}
