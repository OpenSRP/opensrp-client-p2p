package org.smartregister.p2p.sync.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.HashMap;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public class MultiMediaData {

    private File file;
    private long fileRecordId;
    private HashMap<String, String> mediaDetails;

    public MultiMediaData(@NonNull File file, long fileRecordId) {
        this.file = file;
        this.fileRecordId = fileRecordId;
    }

    @NonNull
    public File getFile() {
        return file;
    }

    public long getRecordId() {
        return fileRecordId;
    }

    @Nullable
    public HashMap<String, String> getMediaDetails() {
        return mediaDetails;
    }

    public void setMediaDetails(HashMap<String, String> mediaDetails) {
        this.mediaDetails = mediaDetails;
    }
}
