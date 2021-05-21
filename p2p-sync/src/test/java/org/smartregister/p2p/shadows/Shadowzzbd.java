package org.smartregister.p2p.shadows;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.internal.nearby.zzbd;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.HashMap;
import java.util.concurrent.Executor;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 11/03/2019
 */

@Implements(zzbd.class)
public class Shadowzzbd extends ShadowConnectionsClient {

    public boolean stopAdvertisingCalled;
    public boolean startAdvertisingCalled;
    public static Shadowzzbd instance;
    public HashMap<String, Integer> methodCalls = new HashMap<>();

    public zzbd mockZzbd;

    @Implementation
    public void __constructor__(Context context) {
        // Do nothing as opposed to calling super in the actual implementation
        instance = this;
    }

    public void setMockZzbd(zzbd mockZzbd) {
        this.mockZzbd = mockZzbd;
    }

    @Implementation
    public void stopAdvertising() {
        stopAdvertisingCalled = true;
    }

    @Implementation
    public Task<Void> startAdvertising(@NonNull String var1, @NonNull String var2, @NonNull ConnectionLifecycleCallback var3, @NonNull AdvertisingOptions var4) {
        startAdvertisingCalled = true;
        return new DummyTask() {
            @NonNull
            @Override
            public Task addOnSuccessListener(@NonNull OnSuccessListener onSuccessListener) {
                onSuccessListener.onSuccess(null);
                return super.addOnSuccessListener(onSuccessListener);
            }
        };
    }


    @Implementation
    public void stopDiscovery() {
        addMethodCall("stopDiscovery");
    }

    @Implementation
    public Task<Void> startDiscovery(@NonNull String var1, @NonNull EndpointDiscoveryCallback var2, @NonNull DiscoveryOptions var3) {
        addMethodCall("startDiscovery");
        return new DummyTask() {
            @NonNull
            @Override
            public Task addOnSuccessListener(@NonNull OnSuccessListener onSuccessListener) {
                onSuccessListener.onSuccess(null);
                return super.addOnSuccessListener(onSuccessListener);
            }
        };
    }

    @Implementation
    public Task<Void> requestConnection(String var1, String var2, ConnectionLifecycleCallback var3) {
        if (mockZzbd != null) {
            return mockZzbd.requestConnection(var1, var2, var3);
        }

        return null;
    }

    private void addMethodCall(@NonNull String methodName) {
        int count = methodCalls.containsKey(methodName) ? methodCalls.get(methodName) + 1 : 1;
        methodCalls.put(methodName, count);
    }


    public static class DummyTask extends Task {

        @Override
        public boolean isComplete() {
            return false;
        }

        @Override
        public boolean isSuccessful() {
            return false;
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Nullable
        @Override
        public Object getResult() {
            return null;
        }

        @Nullable
        @Override
        public Exception getException() {
            return null;
        }

        @NonNull
        @Override
        public Task addOnSuccessListener(@NonNull OnSuccessListener onSuccessListener) {
            return DummyTask.this;
        }

        @NonNull
        @Override
        public Task addOnSuccessListener(@NonNull Executor executor, @NonNull OnSuccessListener onSuccessListener) {
            return DummyTask.this;
        }

        @NonNull
        @Override
        public Task addOnSuccessListener(@NonNull Activity activity, @NonNull OnSuccessListener onSuccessListener) {
            return DummyTask.this;
        }

        @NonNull
        @Override
        public Task addOnFailureListener(@NonNull OnFailureListener onFailureListener) {
            return DummyTask.this;
        }

        @NonNull
        @Override
        public Task addOnFailureListener(@NonNull Executor executor, @NonNull OnFailureListener onFailureListener) {
            return DummyTask.this;
        }

        @NonNull
        @Override
        public Task addOnFailureListener(@NonNull Activity activity, @NonNull OnFailureListener onFailureListener) {
            return DummyTask.this;
        }

        @Nullable
        @Override
        public Object getResult(@NonNull Class aClass) throws Throwable {
            return null;
        }
    }

}
