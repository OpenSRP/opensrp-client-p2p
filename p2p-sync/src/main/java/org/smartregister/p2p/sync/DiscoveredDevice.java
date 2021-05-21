package org.smartregister.p2p.sync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;

import java.util.Map;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 15/03/2019
 */
public class DiscoveredDevice {

    private String endpointId;

    @Nullable
    private DiscoveredEndpointInfo discoveredEndpointInfo;

    @Nullable
    private ConnectionInfo connectionInfo;

    @Nullable
    private Map<String, Object> authorizationDetails;

    @Nullable
    private String username;

    public DiscoveredDevice(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
        this.endpointId = endpointId;
        this.discoveredEndpointInfo = discoveredEndpointInfo;
    }

    public DiscoveredDevice(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
        this.endpointId = endpointId;
        this.connectionInfo = connectionInfo;
    }

    public void setDiscoveredEndpointInfo(@NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
        this.discoveredEndpointInfo = discoveredEndpointInfo;
    }

    public void setConnectionInfo(@NonNull ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    public String getEndpointId() {
        return endpointId;
    }

    @Nullable
    public DiscoveredEndpointInfo getDiscoveredEndpointInfo() {
        return discoveredEndpointInfo;
    }

    @NonNull
    public String getEndpointName() {
        // From the constructor, it will never be possible for both discoveredEndpointInfo and
        // connectionInfo to be null
        return connectionInfo != null ? connectionInfo.getEndpointName()
                : discoveredEndpointInfo != null ? discoveredEndpointInfo.getEndpointName() : "";
    }

    @Nullable
    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    @Nullable
    public Map<String, Object> getAuthorizationDetails() {
        return authorizationDetails;
    }

    public void setAuthorizationDetails(@Nullable Map<String, Object> authorizationDetails) {
        this.authorizationDetails = authorizationDetails;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    public void setUsername(@Nullable String username) {
        this.username = username;
    }
}
