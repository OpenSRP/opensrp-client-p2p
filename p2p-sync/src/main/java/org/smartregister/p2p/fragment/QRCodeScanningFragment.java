package org.smartregister.p2p.fragment;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.google.android.gms.vision.barcode.Barcode;

import org.smartregister.p2p.R;
import org.smartregister.p2p.view.QRCodeScannerView;

import java.util.HashMap;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public class QRCodeScanningFragment extends Fragment {

    private QRCodeScanDialogCallback qrCodeScanDialogCallback;
    private QRCodeScannerView qrCodeScannerView;
    private String deviceName;

    private static final long CODES_EXPIRE_TIME = 5 * 1000;
    private HashMap<String, Long> alreadyReadCodes = new HashMap<>();

    public static QRCodeScanningFragment create(@NonNull String deviceName) {
        QRCodeScanningFragment qrCodeScanningFragment = new QRCodeScanningFragment();
        qrCodeScanningFragment.deviceName = deviceName;
        return qrCodeScanningFragment;
    }

    public void setOnQRRecognisedListener(@NonNull QRCodeScanDialogCallback qrCodeScanDialogCallback) {
        this.qrCodeScanDialogCallback = qrCodeScanDialogCallback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_code_scanning, container, false);

        Button skipBtn = view.findViewById(R.id.btn_qrCodeScanningDialog_skipBtn);
        skipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFragment();
                if (qrCodeScanDialogCallback != null) {
                    qrCodeScanDialogCallback.onSkipClicked();
                }

                closeFragment();
            }
        });

        qrCodeScannerView = view.findViewById(R.id.bsv_qrCodeScanningDialog_barcodeScanner);
        qrCodeScannerView.addOnBarcodeRecognisedListener(new QRCodeScannerView.OnQRRecognisedListener() {
            @Override
            public void onBarcodeRecognised(SparseArray<Barcode> recognisedItems) {
                if (areCodesDuplicate(recognisedItems)) {
                    return;
                }

                if (qrCodeScanDialogCallback != null) {
                    qrCodeScanDialogCallback.qrCodeScanned(recognisedItems);
                }

                closeFragment();
            }
        });

        TextView scanningInstructions = view.findViewById(R.id.tv_qrCodeScanningDialog_tv);
        scanningInstructions.setText(String.format(getString(R.string.qr_code_scanning_dialog_message), deviceName));

        return view;
    }

    @VisibleForTesting
    protected boolean areCodesDuplicate(@NonNull SparseArray<Barcode> recognisedItems) {
        long currentTime = System.currentTimeMillis();

        for (int i = 0; i < recognisedItems.size(); i++) {
            String scannedCode = recognisedItems.valueAt(i).rawValue;
            Long lastTimeRecorded = alreadyReadCodes.get(scannedCode);

            // Ignore duplicate codes recorded within
            if (lastTimeRecorded != null) {
                if ((currentTime - lastTimeRecorded) <= CODES_EXPIRE_TIME) {
                    return true;
                } else {
                    alreadyReadCodes.put(scannedCode, currentTime);
                }
            } else {
                alreadyReadCodes.put(scannedCode, currentTime);
            }
        }

        return false;
    }

    private void closeFragment() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (qrCodeScannerView != null) {
            qrCodeScannerView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (qrCodeScannerView != null) {
            qrCodeScannerView.onPause();
            qrCodeScannerView.onDestroy();
        }
        super.onPause();
    }

    public interface QRCodeScanDialogCallback {

        void onSkipClicked();

        void qrCodeScanned(@NonNull SparseArray<Barcode> qrCodeResult);

        void onErrorOccurred(@NonNull Exception e);
    }
}
