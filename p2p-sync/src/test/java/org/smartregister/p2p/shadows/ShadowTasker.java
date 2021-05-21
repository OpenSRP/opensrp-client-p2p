package org.smartregister.p2p.shadows;

import androidx.annotation.NonNull;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.smartregister.p2p.tasks.GenericAsyncTask;
import org.smartregister.p2p.tasks.Tasker;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 29/03/2019
 */
@Implements(Tasker.class)
public class ShadowTasker {

    @Implementation
    public static <T> void run(@NonNull Callable<T> callable
            , @NonNull GenericAsyncTask.OnFinishedCallback<T> onFinishedCallback) {
        Exception ex = null;
        T result = null;

        try {
            result = callable.call();
        } catch (Exception e) {
            ex = e;
        }

        if (ex != null) {
            ex.printStackTrace();
            onFinishedCallback.onError(ex);
        } else {
            onFinishedCallback.onSuccess(result);
        }
    }

    @Implementation
    public static <T> void run(@NonNull Callable<T> callable
            , @NonNull GenericAsyncTask.OnFinishedCallback<T> onFinishedCallback, @NonNull Executor executor) {
        Exception ex = null;
        T result = null;

        try {
            result = callable.call();
        } catch (Exception e) {
            ex = e;
        }

        if (ex != null) {
            onFinishedCallback.onError(ex);
        } else {
            onFinishedCallback.onSuccess(result);
        }
    }
}
