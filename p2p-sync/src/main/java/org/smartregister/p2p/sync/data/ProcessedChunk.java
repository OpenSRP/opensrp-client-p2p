package org.smartregister.p2p.sync.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.nearby.connection.Payload;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 29/04/2019
 */

public class ProcessedChunk {

    private int payloadType;
    private String jsonData;
    private Payload fileData;


    public ProcessedChunk(int payloadType, @NonNull String jsonData) {
        this.payloadType = payloadType;
        this.jsonData = jsonData;
    }

    public ProcessedChunk(int payloadType, @NonNull Payload fileData) {
        this.payloadType = payloadType;
        this.fileData = fileData;
    }

    public int getPayloadType() {
        return payloadType;
    }

    @Nullable
    public String getJsonData() {
        return jsonData;
    }

    @Nullable
    public Payload getFileData() {
        return fileData;
    }

    public void setJsonData(String jsonData) {
        this.jsonData = jsonData;
    }
}
