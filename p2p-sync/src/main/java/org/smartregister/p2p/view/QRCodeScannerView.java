package org.smartregister.p2p.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.smartregister.p2p.R;
import org.smartregister.p2p.util.Constants;

import java.io.IOException;
import java.util.ArrayList;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 14/03/2019
 */

public class QRCodeScannerView extends LinearLayout implements Detector.Processor<Barcode>, LifecycleObserver {

    private CameraSource cameraSource;
    private CameraSourcePreview cameraSourcePreview;
    private LifecycleOwner lifecycleOwner;

    private ArrayList<OnQRRecognisedListener> onQRRecognisedListeners = new ArrayList<>();

    public QRCodeScannerView(Context context) {
        super(context);
        init();
    }

    public QRCodeScannerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public QRCodeScannerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_barcode_scanner, this);

        cameraSourcePreview = findViewById(R.id.csp_barcodeScannerView_preview);
        createCameraSource();

        // Register to the lifecycle
        if (getContext() instanceof LifecycleOwner) {
            lifecycleOwner = (LifecycleOwner) getContext();
            lifecycleOwner.getLifecycle().addObserver(this);
        }
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     * <p>
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource() {
        Context context = getContext().getApplicationContext();
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        barcodeDetector.setProcessor(this);

        if (!barcodeDetector.isOperational()) {
            Timber.w(getContext().getString(R.string.log_detector_dependencies_are_not_yet_available));
            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = getContext().registerReceiver(null, lowStorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(getContext(), getContext().getString(R.string.barcode_detector_dependencies_cannot_be_downloaded)
                        , Toast.LENGTH_LONG).show();
                Timber.e(getContext().getString(R.string.barcode_detector_dependencies_cannot_be_downloaded));
            }
        }

        CameraSource.Builder builder = new CameraSource.Builder(getContext().getApplicationContext(), barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 1024)
                .setAutoFocusEnabled(true)
                .setRequestedFps(45.0f);

        cameraSource = builder.build();
    }

    @Override
    public void release() {
        // Our processor in #receiveDetections does not hold any resources and therefore we have not resources
        // to release
    }

    @Override
    public void receiveDetections(Detector.Detections<Barcode> detections) {
        final SparseArray<Barcode> barcodeSparseArray = detections.getDetectedItems();
        if (barcodeSparseArray.size() > 0) {
            Vibrator vibrator = (Vibrator) getContext().getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
            assert vibrator != null;
            vibrator.vibrate(100);

            for (OnQRRecognisedListener onQRRecognisedListener : onQRRecognisedListeners) {
                onQRRecognisedListener.onBarcodeRecognised(barcodeSparseArray);
            }
        }
    }


    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    public void startCameraSource() throws SecurityException {
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getContext().getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {

            if (getContext() instanceof Activity) {
                Dialog errorDialog =
                        GoogleApiAvailability.getInstance().getErrorDialog((Activity) getContext()
                                , code, Constants.RqCode.BARCODE_SCANNER_GOOGLE_PLAY_FIX);
                errorDialog.show();
            } else {
                Timber.e(getContext().getString(R.string.log_could_not_show_google_play_services_resolution_dialog));
            }
        }

        if (cameraSource != null) {
            try {
                cameraSourcePreview.start(cameraSource);
            } catch (IOException e) {
                Timber.e(e, getContext().getString(R.string.log_unable_to_start_camera_source));
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    /**
     * Restarts the camera.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        if (cameraSourcePreview != null) {
            cameraSourcePreview.stop();
        }

        // We call onDestroy here because the view is destroyed at this point
        onDestroy();

        if (lifecycleOwner != null) {
            lifecycleOwner.getLifecycle().removeObserver(this);
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    public void onDestroy() {
        if (cameraSourcePreview != null) {
            cameraSourcePreview.release();
        }
    }

    public boolean addOnBarcodeRecognisedListener(@NonNull OnQRRecognisedListener onQRRecognisedListener) {
        return !onQRRecognisedListeners.contains(onQRRecognisedListener) &&
                onQRRecognisedListeners.add(onQRRecognisedListener);
    }

    public boolean removeOnBarcodeRecognisedListener(@NonNull OnQRRecognisedListener onQRRecognisedListener) {
        return onQRRecognisedListeners.remove(onQRRecognisedListener);
    }

    public interface OnQRRecognisedListener {

        void onBarcodeRecognised(SparseArray<Barcode> recognisedItems);
    }
}
