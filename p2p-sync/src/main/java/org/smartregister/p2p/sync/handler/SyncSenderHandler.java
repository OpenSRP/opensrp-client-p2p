package org.smartregister.p2p.sync.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import org.json.JSONArray;
import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.P2pReceivedHistory;
import org.smartregister.p2p.sync.data.JsonData;
import org.smartregister.p2p.sync.data.MultiMediaData;
import org.smartregister.p2p.sync.data.SyncPackageManifest;
import org.smartregister.p2p.tasks.GenericAsyncTask;
import org.smartregister.p2p.tasks.Tasker;
import org.smartregister.p2p.util.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public class SyncSenderHandler extends BaseSyncHandler {

    private P2pModeSelectContract.SenderPresenter presenter;

    private TreeSet<DataType> dataSyncOrder;
    private HashMap<String, Long> remainingLastRecordIds = new HashMap<>();
    private List<P2pReceivedHistory> receivedHistory;
    private int batchSize;

    private boolean awaitingPayloadTransfer;
    private Payload awaitingPayload;
    private ParcelFileDescriptor awaitingPayloadPipe;
    private byte[] awaitingBytes;
    private String awaitingDataTypeName;
    private long awaitingDataTypeHighestId;
    private int awaitingDataTypeRecordsBatchSize;

    private boolean awaitingManifestTransfer;
    private long awaitingManifestId;

    private int sendMaxRetries = 3;
    private PayloadRetry payloadRetry;
    private SyncPackageManifest syncPackageManifest;

    private Handler uiHandler;

    public SyncSenderHandler(@NonNull P2pModeSelectContract.SenderPresenter presenter, @NonNull TreeSet<DataType> dataSyncOrder
            , @Nullable List<P2pReceivedHistory> receivedHistory) {
        this.presenter = presenter;
        this.dataSyncOrder = dataSyncOrder;
        this.receivedHistory = receivedHistory;
        this.batchSize = P2PLibrary.getInstance().getBatchSize();
        this.uiHandler = new Handler(Looper.getMainLooper());
    }

    private void generateRecordsToSend() {
        for (DataType dataType : dataSyncOrder) {
            remainingLastRecordIds.put(dataType.getName(), 0L);
        }

        if (receivedHistory != null && receivedHistory.size() > 0) {
            for (P2pReceivedHistory dataTypeHistory : receivedHistory) {
                remainingLastRecordIds.put(dataTypeHistory.getEntityType(), dataTypeHistory.getLastRecordId());
            }
        }
    }

    public void startSyncProcess() {
        generateRecordsToSend();
        sendNextManifest();
    }

    public void sendNextManifest() {
        if (!dataSyncOrder.isEmpty()) {
            final DataType dataType = dataSyncOrder.first();

            if (dataType.getType() == DataType.Type.NON_MEDIA) {
                sendJsonDataManifest(dataType);
            } else if (dataType.getType() == DataType.Type.MEDIA) {
                sendMultimediaDataManifest(dataType);
            }
        } else {
            presenter.sendSyncComplete();
        }
    }

    @VisibleForTesting
    public void sendMultimediaDataManifest(@NonNull final DataType dataType) {
        Tasker.run(new Callable<MultiMediaData>() {
            @Override
            public MultiMediaData call() throws Exception {
                long lastRecordId = remainingLastRecordIds.get(dataType.getName());
                return P2PLibrary.getInstance().getSenderTransferDao()
                        .getMultiMediaData(dataType, lastRecordId);
            }
        }, new GenericAsyncTask.OnFinishedCallback<MultiMediaData>() {
            @Override
            public void onSuccess(@Nullable MultiMediaData multiMediaData) {
                if (multiMediaData != null) {
                    File file = multiMediaData.getFile();
                    awaitingDataTypeName = dataType.getName();
                    awaitingDataTypeHighestId = multiMediaData.getRecordId();
                    awaitingDataTypeRecordsBatchSize = 1;

                    if (file.exists()) {
                        // Create the manifest
                        try {
                            awaitingPayload = Payload.fromFile(file);

                            String filename = file.getName();
                            String extension = "";

                            int lastIndex = filename.lastIndexOf(".");
                            if (lastIndex > -1 && lastIndex < filename.length()) {
                                extension = filename.substring(lastIndex);
                            }

                            syncPackageManifest = new SyncPackageManifest(awaitingPayload.getId()
                                    , extension
                                    , dataType, 1);

                            HashMap<String, String> mediaDetails = multiMediaData.getMediaDetails();
                            HashMap<String, Object> payloadDetails = new HashMap<>();

                            if (mediaDetails != null) {
                                for (String key : mediaDetails.keySet()) {
                                    payloadDetails.put(key, mediaDetails.get(key));
                                }
                            }

                            payloadDetails.put("fileRecordId", multiMediaData.getRecordId());
                            syncPackageManifest.setPayloadDetails(payloadDetails);

                            awaitingManifestTransfer = true;
                            awaitingManifestId = presenter.sendManifest(syncPackageManifest);
                        } catch (FileNotFoundException e) {
                            Timber.e(e);
                            presenter.errorOccurredSync(e);
                        }
                    } else {
                        dataSyncOrder.remove(dataType);
                        sendNextManifest();
                    }
                } else {
                    dataSyncOrder.remove(dataType);
                    sendNextManifest();
                }
            }

            @Override
            public void onError(Exception e) {
                presenter.errorOccurredSync(e);
            }
        });
    }

    @VisibleForTesting
    public void sendJsonDataManifest(@NonNull final DataType dataType) {
        Tasker.run(new Callable<String>() {
            @Override
            public String call() throws Exception {

                Long nullableRecordId = remainingLastRecordIds.get(dataType.getName());
                long lastRecordId = nullableRecordId == null ? 0l : nullableRecordId;
                JsonData jsonData = P2PLibrary.getInstance().getSenderTransferDao()
                        .getJsonData(dataType, lastRecordId, batchSize);

                if (jsonData != null) {
                    JSONArray recordsArray = jsonData.getJsonArray();
                    //TODO: Check if I should remove this
                    remainingLastRecordIds.put(dataType.getName(), jsonData.getHighestRecordId());

                    String jsonString = recordsArray.toString();
                    awaitingDataTypeName = dataType.getName();
                    awaitingDataTypeHighestId = jsonData.getHighestRecordId();
                    awaitingDataTypeRecordsBatchSize = recordsArray.length();

                    return jsonString;
                } else {
                    return null;
                }
            }
        }, new GenericAsyncTask.OnFinishedCallback<String>() {
            @Override
            public void onSuccess(@Nullable String result) {
                if (result != null) {
                    // Create the manifest
                    ParcelFileDescriptor[] payloadPipe = createJsonDataStream();
                    if (payloadPipe != null) {
                        awaitingBytes = result.getBytes();

                        awaitingPayload = Payload.fromStream(payloadPipe[0]);
                        awaitingPayloadPipe = payloadPipe[1];

                        syncPackageManifest = new SyncPackageManifest(awaitingPayload.getId()
                                , "json"
                                , dataType
                                , awaitingDataTypeRecordsBatchSize);
                        syncPackageManifest.setPayloadSize(awaitingBytes.length);

                        awaitingManifestTransfer = true;
                        awaitingManifestId = presenter.sendManifest(syncPackageManifest);
                    } else {
                        presenter.errorOccurredSync(new Exception("Payload pipe from json data is null"));
                    }
                } else {
                    dataSyncOrder.remove(dataType);
                    sendNextManifest();
                }
            }

            @Override
            public void onError(Exception e) {
                presenter.errorOccurredSync(e);
            }
        });
    }

    @Nullable
    private ParcelFileDescriptor[] createJsonDataStream() {
        try {
            return ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            Timber.e(e);
            return null;
        }
    }

    @VisibleForTesting
    public void sendNextPayload() {
        startNewThread(new Runnable() {
            @Override
            public void run() {
                if (awaitingPayload != null) {
                    awaitingPayloadTransfer = true;
                    presenter.sendPayload(awaitingPayload);

                    if (awaitingPayload.getType() == Payload.Type.STREAM) {
                        if (awaitingPayloadPipe != null && awaitingBytes != null) {
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    presenter.getView().updateProgressFragment(-1);
                                    presenter.getView().updateProgressFragment(String.format(presenter.getView().getString(R.string.sending_progress_text), awaitingDataTypeRecordsBatchSize, awaitingDataTypeName), "");
                                }
                            });

                            ParcelFileDescriptor.AutoCloseOutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(awaitingPayloadPipe);
                            try {
                                int totalLen = awaitingBytes.length;
                                Timber.e("Bytes size %s", String.valueOf(totalLen));
                                outputStream.write(awaitingBytes);
                                outputStream.flush();
                                outputStream.close();

                            } catch (final IOException e) {
                                Timber.e(e, "Error occurred trying to read bytes into payload pipe");

                                uiHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        presenter.errorOccurredSync(e);
                                    }
                                });
                            }
                        } else {
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    presenter.errorOccurredSync(new Exception("Could not find the payload pipe!"));
                                }
                            });
                        }
                    } else if (awaitingPayload.getType() == Payload.Type.FILE) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                presenter.getView().updateProgressFragment(-1);
                                presenter.getView().updateProgressFragment(String.format(presenter.getView().getString(R.string.sending_progress_text)
                                        , awaitingDataTypeRecordsBatchSize), "");
                            }
                        });
                    }
                }
            }
        });
    }

    @VisibleForTesting
    public void startNewThread(@NonNull Runnable runnable) {
        new Thread(runnable).start();
    }

    public void processString(@NonNull String message) {
        if (message.startsWith(Constants.Connection.PAYLOAD_RECEIVED)
                && awaitingPayloadTransfer
                && awaitingPayload != null) {
            String payloadIdString = message.replace(Constants.Connection.PAYLOAD_RECEIVED, "");
            if (!TextUtils.isEmpty(payloadIdString) && Long.parseLong(payloadIdString) == awaitingPayload.getId()) {
                updateTransferProgress(awaitingDataTypeName, awaitingDataTypeRecordsBatchSize);

                awaitingDataTypeRecordsBatchSize = 0;
                awaitingPayloadTransfer = false;
                awaitingPayload = null;
                awaitingBytes = null;
                awaitingPayloadPipe = null;

                if (awaitingDataTypeName != null) {
                    remainingLastRecordIds.put(awaitingDataTypeName, awaitingDataTypeHighestId);
                }

                sendNextManifest();
            }
        }
    }

    public void onPayloadTransferUpdate(@NonNull final PayloadTransferUpdate update) {
        Timber.e("Payload transfer update %d with %,d bytes transfer | PayloadId %d | Total Bytes %,d"
                , update.getStatus(), update.getBytesTransferred(), update.getPayloadId()
                , update.getTotalBytes());
        if (awaitingManifestTransfer) {
            if (update.getPayloadId() == awaitingManifestId) {
                if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                    awaitingManifestTransfer = false;
                    awaitingManifestId = 0;
                    payloadRetry = null;
                    syncPackageManifest = null;

                    sendNextPayload();
                } else if (update.getStatus() == PayloadTransferUpdate.Status.FAILURE) {
                    // Try to resend the manifest until the max retries are done
                    if (payloadRetry == null) {
                        payloadRetry = new PayloadRetry(0l, sendMaxRetries);
                    }

                    if (payloadRetry.retries > 0) {
                        payloadRetry.retries--;

                        awaitingManifestTransfer = true;
                        awaitingManifestId = presenter.sendManifest(syncPackageManifest);
                        payloadRetry.payloadId = awaitingManifestId;
                    } else {
                        presenter.errorOccurredSync(new Exception("Manifest Payload send failed up-to " + sendMaxRetries));
                    }
                } else if (update.getStatus() == PayloadTransferUpdate.Status.CANCELED) {
                    presenter.errorOccurredSync(new Exception("Manifest Payload sending has been cancelled"));
                }
            }
        } else if (awaitingPayloadTransfer && awaitingPayload != null && update.getPayloadId() == awaitingPayload.getId()) {
            if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {

                logTransfer(true, awaitingDataTypeName, presenter.getCurrentPeerDevice(), awaitingDataTypeRecordsBatchSize);

            } else if (update.getStatus() == PayloadTransferUpdate.Status.FAILURE) {
                // Try to resend the payload until the max retries are done
                if (payloadRetry == null) {
                    payloadRetry = new PayloadRetry(awaitingPayload.getId(), sendMaxRetries);
                }

                if (payloadRetry.retries > 0) {
                    payloadRetry.retries--;
                    sendNextPayload();
                } else {
                    presenter.errorOccurredSync(new Exception("Payload send failed up-to " + sendMaxRetries));
                }
            } else if (update.getStatus() == PayloadTransferUpdate.Status.CANCELED) {
                presenter.errorOccurredSync(new Exception("Payload sending has been cancelled"));
            } else if (update.getStatus() == PayloadTransferUpdate.Status.IN_PROGRESS) {
                // I should update them here
                if (awaitingBytes != null) {
                    int maxSize = awaitingBytes.length;
                    int transferredSize = (int) update.getBytesTransferred();

                    presenter.getView().updateProgressFragment((transferredSize * 100) / maxSize);
                } else {
                    Timber.e("We are waiting for a payload to finish transferring and the bytes for the payload being sent are null");
                }
            }
        }
    }

    class PayloadRetry {
        protected long payloadId;
        protected int retries;

        PayloadRetry(long payloadId, int retries) {
            this.payloadId = payloadId;
            this.retries = retries;
        }
    }

}
