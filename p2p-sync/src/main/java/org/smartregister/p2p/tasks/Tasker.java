package org.smartregister.p2p.tasks;

import android.os.AsyncTask;
import androidx.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */

public abstract class Tasker {

    public static <T> void run(@NonNull Callable<T> callable
            , @NonNull GenericAsyncTask.OnFinishedCallback<T> onFinishedCallback, @NonNull Executor executor) {
        GenericAsyncTask<T> genericAsyncTask = new GenericAsyncTask<T>(callable);
        genericAsyncTask.setOnFinishedCallback(onFinishedCallback);
        genericAsyncTask.executeOnExecutor(executor);
    }

    public static <T> void run(@NonNull Callable<T> callable
            , @NonNull GenericAsyncTask.OnFinishedCallback<T> onFinishedCallback) {
        run(callable, onFinishedCallback, AsyncTask.THREAD_POOL_EXECUTOR);
    }

}
