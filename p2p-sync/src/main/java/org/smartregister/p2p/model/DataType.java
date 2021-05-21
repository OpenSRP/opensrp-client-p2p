package org.smartregister.p2p.model;

import androidx.annotation.NonNull;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public class DataType implements Comparable<DataType> {

    private final String name;
    private final Type type;
    private final int position;

    public DataType(@NonNull String name, @NonNull Type type, int position) {
        this.name = name;
        this.type = type;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public int compareTo(@NonNull DataType o) {
        if (getPosition() == o.getPosition()) {
            return 0;
        } else if (getPosition() < o.getPosition()) {
            return -1;
        }

        return 1;
    }

    public enum Type {
        MEDIA,
        NON_MEDIA
    }
}
