package org.smartregister.p2p.util;

import com.google.android.gms.nearby.connection.Strategy;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 11/03/2019
 */

public interface Constants {

    Strategy STRATEGY = Strategy.P2P_STAR;
    int DEFAULT_SHARE_BATCH_SIZE = 20;
    int DEFAULT_MIN_DEVICE_CONNECTION_RETRY_DURATION = 2 * 60 * 60;
    String NEARBY_DIRECTORY = "Nearby";
    String RECORDS_TRACK_TAG = "ToTrack";
    String RECORDS_TRACK_TAG_HR = "ToTrackHR";

    /**
     * This timeout is for both advertising and discovering
     */
    long CONNECTION_TIMEOUT_SECONDS = 2 * 60;

    interface Dialog {
        String START_SEND_MODE_PROGRESS = "dialog_start_send_mode_progress";
        String START_RECEIVE_MODE_PROGRESS = "dialog_start_receive_mode_progress";
        String SKIP_QR_SCAN = "skip_qr_scan";
        String CONNECTING = "connecting";
        String DISPLAY_APPROVAL_KEY = "display_approval";
    }

    interface Fragment {
        String QR_CODE_SCANNING = "qr_code_scanner";
        String AUTHENTICATION_QR_CODE_GENERATOR = "authentication_qr_code_generator";
        String SYNC_PROGRESS = "sync_progress";
        String ERROR = "error_fragment";
        String SYNC_COMPLETE = "sync_complete";
        String DEVICES_CONNECTED = "devices_connected";
    }

    interface Connection {
        String SYNC_COMPLETE = "SYNC-COMPLETE";
        String PAYLOAD_RECEIVED = "PAYLOAD-RECEIVED";
        String SKIP_QR_CODE_SCAN = "SKIP-AUTHENTICATION";
        String CONNECTION_ACCEPT = "CONNECTION-ACCEPT";
        String START_TRANSFER = "START-TRANSFER";
    }

    interface RqCode {
        int PERMISSIONS = 2;
        int LOCATION_SETTINGS = 3;
        int BARCODE_SCANNER_GOOGLE_PLAY_FIX = 4;
    }

    interface Prefs {
        String NAME = "android-p2p-sync";
        String KEY_HASH = "hash-key";
    }

    interface BasicDeviceDetails {

        String KEY_DEVICE_ID = "device-id";
        String KEY_APP_LIFETIME_KEY = "app-lifetime-key";
    }

    interface AuthorizationKeys {
        String PEER_STATUS = "peer-status";
    }

    interface PeerStatus {
        String SENDER = "sender";
        String RECEIVER = "receiver";
    }

    interface PlayStore {
        String PLAY_SERVICES = "com.google.android.gms";
    }
}
