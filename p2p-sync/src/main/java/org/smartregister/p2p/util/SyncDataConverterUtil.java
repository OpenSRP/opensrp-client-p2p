package org.smartregister.p2p.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.smartregister.p2p.R;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 02/04/2019
 */

public class SyncDataConverterUtil {

    @NonNull
    public static String readInputStreamAsString(InputStream in)
            throws IOException {
        BufferedInputStream bis = new BufferedInputStream(in);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result = bis.read();
        while (result != -1) {
            byte b = (byte) result;
            buf.write(b);
            result = bis.read();
        }
        return buf.toString();
    }

    @NonNull
    public static String generateSummaryReport(@NonNull Context context, boolean sent, @Nullable HashMap<String, Integer> transferItems) {
        String transferSummary = context.getString(R.string.transfer_summary_content);

        int total = 0;
        if (transferItems != null) {
            for (String key : transferItems.keySet()) {
                Integer count = transferItems.get(key);
                if (count != null) {
                    total += count;
                }

            }
        }

        return String.format(transferSummary, total, sent ? "sent" : "received");
    }
}
