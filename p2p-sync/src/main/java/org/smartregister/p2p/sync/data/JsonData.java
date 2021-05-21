package org.smartregister.p2p.sync.data;

import androidx.annotation.NonNull;
import org.json.JSONArray;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public class JsonData {

    private JSONArray jsonArray;
    private long highestRecordId;

    public JsonData(@NonNull JSONArray jsonArray, long highestRecordId) {
        this.jsonArray = jsonArray;
        this.highestRecordId = highestRecordId;
    }

    @NonNull
    public JSONArray getJsonArray() {
        return jsonArray;
    }

    public long getHighestRecordId() {
        return highestRecordId;
    }
}
