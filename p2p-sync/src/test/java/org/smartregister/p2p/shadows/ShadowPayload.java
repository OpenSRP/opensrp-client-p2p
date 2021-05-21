package org.smartregister.p2p.shadows;

import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;

import com.google.android.gms.nearby.connection.Payload;

import org.mockito.Mockito;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.FileNotFoundException;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 2019-06-13
 */

@Implements(Payload.class)
public class ShadowPayload {

    private static Payload toReturn = Mockito.mock(Payload.class);

    public static void setPayloadToReturn(@NonNull Payload payloadToReturn) {
        toReturn = payloadToReturn;
    }

    @Implementation
    @NonNull
    public static Payload fromFile(@NonNull java.io.File var0) throws FileNotFoundException {
        return toReturn;
    }

    @Implementation
    @NonNull
    public static Payload fromStream(@NonNull ParcelFileDescriptor var0) {
        return toReturn;
    }

}
