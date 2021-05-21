package org.smartregister.p2p.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.TextView;

import org.smartregister.p2p.R;
import org.smartregister.p2p.util.Constants;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public class SkipQRScanDialog extends DialogFragment {

    private SkipDialogCallback skipDialogCallback;
    private String peerDeviceStatus = Constants.PeerStatus.RECEIVER;
    private String deviceName;

    private DialogInterface dialogInterface = new DialogInterface() {
        @Override
        public void cancel() {
            SkipQRScanDialog.this.dismiss();
        }

        @Override
        public void dismiss() {
            SkipQRScanDialog.this.dismiss();
        }
    };

    public void setSkipDialogCallback(@NonNull SkipDialogCallback skipDialogCallback) {
        this.skipDialogCallback = skipDialogCallback;
    }

    public void setPeerDeviceStatus(@NonNull String peerDeviceStatus) {
        this.peerDeviceStatus = peerDeviceStatus;
    }

    public void setDeviceName(@NonNull String deviceName) {
        this.deviceName = deviceName;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_skip_authentication_dialog, null))
                .setTitle(R.string.skip_qr_scan)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                        if (skipDialogCallback != null) {
                            skipDialogCallback.onCancelClicked(dialogInterface);
                        }
                    }
                })
                .setPositiveButton(R.string.skip, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                        if (skipDialogCallback != null) {
                            skipDialogCallback.onSkipClicked(dialogInterface);
                        }
                    }
                });

        AlertDialog dialog = builder.create();
        setCancelable(false);

        return dialog;
    }


    @Override
    public void onStart() {
        super.onStart();

        TextView instructions = getDialog().findViewById(R.id.tv_skipAuthenticationDialog_instructionsTv);
        String text = "";
        if (peerDeviceStatus.equals(Constants.PeerStatus.RECEIVER)) {
            text = String.format(getString(R.string.skip_qr_scan_instructions_receiver), deviceName);
        } else if (peerDeviceStatus.equals(Constants.PeerStatus.SENDER)) {
            text = String.format(getString(R.string.skip_qr_scan_instructions_sender), deviceName);
        }

        instructions.setText(text);
    }

    public interface SkipDialogCallback {

        void onSkipClicked(@NonNull DialogInterface dialogInterface);

        void onCancelClicked(@NonNull DialogInterface dialogInterface);
    }
}
