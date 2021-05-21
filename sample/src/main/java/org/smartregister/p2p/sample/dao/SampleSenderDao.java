package org.smartregister.p2p.sample.dao;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.dao.SenderTransferDao;
import org.smartregister.p2p.sample.util.Constants;
import org.smartregister.p2p.sync.data.JsonData;
import org.smartregister.p2p.sync.data.MultiMediaData;
import org.smartregister.p2p.tasks.GenericAsyncTask;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.Callable;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public class SampleSenderDao implements SenderTransferDao {

    private List<String> nameRecords = new ArrayList<>();
    private List<String> personalDetailsRecords = new ArrayList<>();

    private char[] symbols = "ABCEFGHJKLMNPQRUVWXYabcdefhijkprstuvwx".toCharArray();

    public SampleSenderDao() {
        nameRecords.add("John Doe");
        nameRecords.add("Jane Doe");
        nameRecords.add("Sarah Platlin");
        nameRecords.add("Rose Wambui");
        nameRecords.add("Leo Atieno");
        nameRecords.add("Chris Wamaitha");

        GenericAsyncTask<Void> genericAsyncTask = new GenericAsyncTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (int i = 0; i < 200; i++) {
                    personalDetailsRecords.add(generateRandomString(100000));
                }

                return null;
            }

        });

        genericAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    @Nullable
    @Override
    public TreeSet<DataType> getDataTypes() {
        TreeSet<DataType> dataTypes = new TreeSet<>();
        dataTypes.add(new DataType(Constants.NAMES, DataType.Type.NON_MEDIA, 0));
        dataTypes.add(new DataType(Constants.PERSONAL_DETAIL, DataType.Type.NON_MEDIA, 1));
        dataTypes.add(new DataType(Constants.PROFILE_PIC, DataType.Type.MEDIA, 2));

        return dataTypes;
    }

    @Nullable
    @Override
    public JsonData getJsonData(@NonNull DataType dataType, long lastRecordId, int batchSize) {
        if (dataType.getName().equals(Constants.NAMES)) {
            JSONArray jsonArray = new JSONArray();

            if (lastRecordId >= nameRecords.size()) {
                return null;
            } else {
                int recordsAdded = 0;
                for (int i = 0; i < batchSize; i++) {
                    if ((lastRecordId + i) >= nameRecords.size()) {
                        break;
                    }

                    String nameRecord = nameRecords.get((int) (lastRecordId + i));
                    jsonArray.put(nameRecord);
                    recordsAdded++;
                }

                return new JsonData(jsonArray, lastRecordId + recordsAdded);
            }
        } else if (dataType.getName().equals(Constants.PERSONAL_DETAIL)) {
            JSONArray jsonArray = new JSONArray();

            if (lastRecordId >= personalDetailsRecords.size()) {
                return null;
            } else {
                int recordsAdded = 0;
                for (int i = 0; i < batchSize; i++) {
                    if ((lastRecordId + i) >= personalDetailsRecords.size()) {
                        break;
                    }

                    String personDetails = personalDetailsRecords.get((int) (lastRecordId + i));
                    jsonArray.put(personDetails);
                    recordsAdded++;
                }

                return new JsonData(jsonArray, lastRecordId + recordsAdded);
            }
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public MultiMediaData getMultiMediaData(@NonNull DataType dataType, long lastRecordId) {
        if (lastRecordId < 4) {
            HashMap<String, String> imageDetails = new HashMap<>();
            imageDetails.put("name", "Picture in root folder of my phone");

            File inputFile = new File("/sdcard/1545669737880.png");
            if (inputFile.exists()) {
                MultiMediaData multiMediaData = new MultiMediaData(
                        inputFile,
                        4
                );
                multiMediaData.setMediaDetails(imageDetails);

                return multiMediaData;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Generate a random string.
     */
    private String generateRandomString(int len) {
        char[] buf = new char[len];
        Random random = new SecureRandom();

        for (int idx = 0; idx < buf.length; ++idx) {
            buf[idx] = symbols[random.nextInt(symbols.length)];
        }
        return new String(buf);
    }
}
