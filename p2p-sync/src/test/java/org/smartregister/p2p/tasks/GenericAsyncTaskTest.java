package org.smartregister.p2p.tasks;

import androidx.annotation.Nullable;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.p2p.TestApplication;
import org.smartregister.p2p.exceptions.AsyncTaskCancelledException;

import java.util.concurrent.Callable;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 2019-06-12
 */
@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class)
public class GenericAsyncTaskTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void doInBackgroundShouldCallRunnableMethod() throws Exception {
        Callable callable = Mockito.mock(Callable.class);

        GenericAsyncTask genericAsyncTask = new GenericAsyncTask(callable);
        genericAsyncTask.doInBackground();

        Mockito.verify(callable, Mockito.times(1))
                .call();
    }

    @Test
    public void doInBackgroundShouldCallAsyncTaskCancelWhenExceptionOccurs() {
        Callable callable = new Callable() {
            @Override
            public Object call() throws Exception {
                throw new IllegalArgumentException("This is a sample error");
            }
        };

        GenericAsyncTask genericAsyncTask = Mockito.spy(new GenericAsyncTask(callable));
        genericAsyncTask.doInBackground();

        Tasker.run(callable, new GenericAsyncTask.OnFinishedCallback() {
            @Override
            public void onSuccess(@Nullable Object result) {
                // Do nothing
            }

            @Override
            public void onError(Exception e) {
                // Do nothing
            }
        });

        Mockito.verify(genericAsyncTask, Mockito.times(1))
                .cancel(Mockito.eq(true));
    }

    @Test
    public void onPostExecuteShouldCallCallbackOnSuccessWithResult() {
        final String expectedResult = "someresult";

        Callable callable = Mockito.mock(Callable.class);

        GenericAsyncTask.OnFinishedCallback onFinishedCallback = Mockito.mock(GenericAsyncTask.OnFinishedCallback.class);
        GenericAsyncTask genericAsyncTask = new GenericAsyncTask(callable);
        genericAsyncTask.setOnFinishedCallback(onFinishedCallback);
        genericAsyncTask.onPostExecute(expectedResult);

        Mockito.verify(onFinishedCallback, Mockito.times(1))
                .onSuccess(Mockito.eq(expectedResult));
    }

    @Test
    public void onCancelledShouldCallCallbackOnError() {
        Callable callable = new Callable() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        };

        GenericAsyncTask genericAsyncTask = new GenericAsyncTask(callable);
        GenericAsyncTask.OnFinishedCallback onFinishedCallback = Mockito.mock(GenericAsyncTask.OnFinishedCallback.class);
        genericAsyncTask.setOnFinishedCallback(onFinishedCallback);

        genericAsyncTask.onCancelled();

        Mockito.verify(onFinishedCallback, Mockito.times(1))
                .onError(Mockito.any(AsyncTaskCancelledException.class));
    }
}