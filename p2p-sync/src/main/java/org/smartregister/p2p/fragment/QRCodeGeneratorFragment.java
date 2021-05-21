package org.smartregister.p2p.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.smartregister.p2p.R;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public class QRCodeGeneratorFragment extends Fragment {

    private String authenticationCode;
    private QRCodeGeneratorCallback qrCodeGeneratorCallback;
    private String deviceName;

    public void setAuthenticationCode(@NonNull String authenticationCode) {
        this.authenticationCode = authenticationCode;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setQrCodeGeneratorCallback(QRCodeGeneratorCallback qrCodeGeneratorCallback) {
        this.qrCodeGeneratorCallback = qrCodeGeneratorCallback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_code_generation, container, false);

        // Generate the QR Code
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        Bitmap bitmap = null;
        try {
            int barcodeSize = 1200;
            bitmap = barcodeEncoder.encodeBitmap(authenticationCode, BarcodeFormat.QR_CODE, barcodeSize, barcodeSize);

            ImageView imageViewQrCode = view.findViewById(R.id.iv_qrCodeGenDialog_qrCode);
            imageViewQrCode.setImageBitmap(bitmap);

            ((TextView) view.findViewById(R.id.tv_qrCodeGenDialog_authCode))
                    .setText(String.format(getString(R.string.scan_this_qr_code_using_sending_device), deviceName));
        } catch (WriterException e) {
            Timber.e(e);

            // TODO: Show error toast
            if (qrCodeGeneratorCallback != null) {
                qrCodeGeneratorCallback.onErrorOccurred(e);
            }

            closeFragment();
        }

        Button skip = view.findViewById(R.id.btn_qrCodeGenDialog_skipBtn);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (qrCodeGeneratorCallback != null) {
                    closeFragment();
                    qrCodeGeneratorCallback.onSkipped();
                }
            }
        });

        return view;
    }

    public void closeFragment() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public interface QRCodeGeneratorCallback {

        void onSkipped();

        void onErrorOccurred(@NonNull Exception e);
    }
}
