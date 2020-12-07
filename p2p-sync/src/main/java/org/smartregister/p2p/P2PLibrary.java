package org.smartregister.p2p;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.callback.SyncFinishedCallback;
import org.smartregister.p2p.contract.RecalledIdentifier;
import org.smartregister.p2p.model.AppDatabase;
import org.smartregister.p2p.model.dao.ReceiverTransferDao;
import org.smartregister.p2p.model.dao.SenderTransferDao;
import org.smartregister.p2p.tasks.GenericAsyncTask;
import org.smartregister.p2p.util.Constants;
import org.smartregister.p2p.util.Device;
import org.smartregister.p2p.util.Settings;

import java.util.UUID;
import java.util.concurrent.Callable;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public final class P2PLibrary {

    private static P2PLibrary instance;
    private Options options;
    private String hashKey;
    private String deviceUniqueIdentifier;

    @NonNull
    public static P2PLibrary getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Instance does not exist!!! Call P2PLibrary.init method"
                    + "in the onCreate method of "
                    + "your Application class ");
        }

        return instance;
    }

    public static void init(@NonNull Options options) {
        instance = new P2PLibrary(options);
    }

    private P2PLibrary(@NonNull Options options) {
        this.options = options;

        // We should not override the host applications Timber trees
        if (Timber.treeCount() == 0) {
            Timber.plant(new Timber.DebugTree());
        }

        hashKey = getHashKey();

        // Start the DB
        AppDatabase.getInstance(getContext(), options.getDbPassphrase());
    }

    @NonNull
    public AppDatabase getDb() {
        return AppDatabase.getInstance(getContext(), options.getDbPassphrase());
    }

    @NonNull
    public String getHashKey() {
        if (hashKey == null) {
            Settings settings = new Settings(getContext());
            hashKey = settings.getHashKey();

            if (hashKey == null) {
                hashKey = generateHashKey();
                settings.saveHashKey(hashKey);
            }
        }

        return hashKey;
    }

    private String generateHashKey() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns a unique device address by using a provided device implementation or by default returns MAC Address
     * <p>
     * This retrieves the device Wifi MAC Address. This might take up-to 5 seconds because it has to turn on wifi
     * if it is not on so that is can access the WLAN interface
     *
     * @param context
     * @param onFinishedCallback
     */
    public void getDeviceAddress(@NonNull final Context context, @NonNull GenericAsyncTask.OnFinishedCallback<String> onFinishedCallback) {
        GenericAsyncTask<String> genericAsyncTask = new GenericAsyncTask<>(new Callable<String>() {
            @Override
            public String call() {
                if (options != null && options.recalledIdentifier != null) {
                    return options.recalledIdentifier.getUniqueID(context);
                }

                return Device.generateUniqueDeviceId(context);
            }
        });

        genericAsyncTask.setOnFinishedCallback(onFinishedCallback);
        genericAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setDeviceUniqueIdentifier(@NonNull String deviceUniqueIdentifier) {
        this.deviceUniqueIdentifier = deviceUniqueIdentifier;
    }

    @Nullable
    public String getDeviceUniqueIdentifier() {
        return deviceUniqueIdentifier;
    }


    @NonNull
    public String getUsername() {
        return options.getUsername();
    }

    @NonNull
    public P2PAuthorizationService getP2PAuthorizationService() {
        return options.getP2PAuthorizationService();
    }

    @NonNull
    public Context getContext() {
        return options.getContext();
    }

    @NonNull
    public ReceiverTransferDao getReceiverTransferDao() {
        return options.getReceiverTransferDao();
    }

    @NonNull
    public SenderTransferDao getSenderTransferDao() {
        return options.getSenderTransferDao();
    }

    public int getBatchSize() {
        return options.getBatchSize();
    }

    public long getDeviceMaxRetryConnectionDuration() {
        return options.getDeviceMaxRetryConnectionDuration();
    }

    @Nullable
    public SyncFinishedCallback getSyncFinishedCallback() {
        return options.getSyncFinishedCallback();
    }

    public Options getOptions() {
        return options;
    }

    public static class Options {

        private Context context;
        private String username;
        private String dbPassphrase;
        private P2PAuthorizationService p2PAuthorizationService;
        private ReceiverTransferDao receiverTransferDao;
        private SenderTransferDao senderTransferDao;
        private SyncFinishedCallback syncFinishedCallback;
        private Boolean skipQrCodeScan = true;

        @Nullable
        private RecalledIdentifier recalledIdentifier;
        private int batchSize = Constants.DEFAULT_SHARE_BATCH_SIZE;

        private long deviceMaxRetryConnectionDuration = Constants.DEFAULT_MIN_DEVICE_CONNECTION_RETRY_DURATION;

        public Options(@NonNull Context context, @NonNull String dbPassphrase, @NonNull String username
                , @NonNull P2PAuthorizationService p2PAuthorizationService, @NonNull ReceiverTransferDao receiverTransferDao
                , @NonNull SenderTransferDao senderTransferDao) {
            this.context = context;
            this.dbPassphrase = dbPassphrase;
            this.username = username;
            this.p2PAuthorizationService = p2PAuthorizationService;
            this.receiverTransferDao = receiverTransferDao;
            this.senderTransferDao = senderTransferDao;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getBatchSize() {
            return batchSize;
        }

        @NonNull
        public String getUsername() {
            return this.username;
        }

        @NonNull
        public P2PAuthorizationService getP2PAuthorizationService() {
            return p2PAuthorizationService;
        }

        @NonNull
        public ReceiverTransferDao getReceiverTransferDao() {
            return receiverTransferDao;
        }

        @NonNull
        public SenderTransferDao getSenderTransferDao() {
            return senderTransferDao;
        }

        @NonNull
        public Context getContext() {
            return context;
        }

        @NonNull
        public String getDbPassphrase() {
            return dbPassphrase;
        }

        public long getDeviceMaxRetryConnectionDuration() {
            return deviceMaxRetryConnectionDuration;
        }

        public void setDeviceMaxRetryConnectionDuration(long deviceMaxRetryConnectionDuration) {
            this.deviceMaxRetryConnectionDuration = deviceMaxRetryConnectionDuration;
        }

        @Nullable
        public SyncFinishedCallback getSyncFinishedCallback() {
            return syncFinishedCallback;
        }

        public void setSyncFinishedCallback(SyncFinishedCallback syncFinishedCallback) {
            this.syncFinishedCallback = syncFinishedCallback;
        }

        @Nullable
        public RecalledIdentifier getRecalledIdentifier() {
            return recalledIdentifier;
        }

        public void setRecalledIdentifier(@Nullable RecalledIdentifier recalledIdentifier) {
            this.recalledIdentifier = recalledIdentifier;
        }

        public boolean allowSkipQrCodeScan() {
            return skipQrCodeScan != null && skipQrCodeScan;
        }

        public void setSkipQrCodeScan(boolean skipQrCodeScan) {
            this.skipQrCodeScan = skipQrCodeScan;
        }
    }
}
