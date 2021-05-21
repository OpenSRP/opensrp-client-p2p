package org.smartregister.p2p.tasks;

import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.smartregister.p2p.contract.P2pModeSelectContract;

import java.util.concurrent.Callable;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 2019-06-10
 */

public class ConnectionTimeout extends GenericAsyncTask<Void> implements GenericAsyncTask.OnFinishedCallback<Void> {

    private P2pModeSelectContract.BasePresenter.OnConnectionTimeout onConnectionTimeout;
    private long connectionTimeoutSeconds;

    public ConnectionTimeout(final long connectionTimeoutSeconds, @NonNull P2pModeSelectContract.BasePresenter.OnConnectionTimeout onConnectionTimeout) {
        this(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Thread.sleep(connectionTimeoutSeconds * 1000);
                return null;
            }
        });

        this.onConnectionTimeout = onConnectionTimeout;
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;

        setOnFinishedCallback(this);
    }

    public void start() {
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private ConnectionTimeout(@NonNull Callable<Void> toCall) {
        super(toCall);
    }

    public void stop() {
        cancel(true);
    }

    @Override
    public void onSuccess(@Nullable Void result) {
        onConnectionTimeout.connectionTimeout(connectionTimeoutSeconds, null);
    }

    @Override
    public void onError(Exception e) {
        onConnectionTimeout.connectionTimeout(connectionTimeoutSeconds, e);
    }
}
