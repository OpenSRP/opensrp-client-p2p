package org.smartregister.p2p.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.smartregister.p2p.R;
import org.smartregister.p2p.contract.P2pModeSelectContract;

public class ReceiverConnectionInfoDialog extends DialogFragment {

    private P2pModeSelectContract.View.DialogCancelCallback dialogCancelCallback;
    private String deviceName;
    private String connectionKey;
    private P2pModeSelectContract.View.DialogApprovedCallback dialogApprovedCallback;

    public void setDialogCancelCallback(@NonNull P2pModeSelectContract.View.DialogCancelCallback dialogCancelCallback) {
        this.dialogCancelCallback = dialogCancelCallback;
    }

    public void setDialogApprovedCallback(@NonNull P2pModeSelectContract.View.DialogApprovedCallback dialogApprovedCallback) {
        this.dialogApprovedCallback = dialogApprovedCallback;
    }

    public void setConnectionDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setConnectionKey(String connectionKey) {
        this.connectionKey = connectionKey;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View rootView = inflater.inflate(R.layout.dialog_pair_receiver_approval, null);

        TextView tvCode = rootView.findViewById(R.id.tv_connection_id);
        tvCode.setText(connectionKey);

        TextView tvMessage = rootView.findViewById(R.id.tv_message);
        tvMessage.setText(getString(R.string.pair_request_sender_prompt, deviceName));

        builder.setView(rootView)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialogCancelCallback != null) {
                            dialogCancelCallback.onCancelClicked(new DialogInterface() {
                                @Override
                                public void cancel() {
                                    ReceiverConnectionInfoDialog.this.dismiss();
                                }

                                @Override
                                public void dismiss() {
                                    ReceiverConnectionInfoDialog.this.dismiss();
                                }
                            });
                        }
                    }
                })
                .setPositiveButton(R.string.approve, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialogApprovedCallback != null) {
                            dialogApprovedCallback.onApprovedClicked(new DialogInterface() {
                                @Override
                                public void cancel() {
                                    ReceiverConnectionInfoDialog.this.dismiss();
                                }

                                @Override
                                public void dismiss() {
                                    ReceiverConnectionInfoDialog.this.dismiss();
                                }
                            });
                        }
                    }
                });


        AlertDialog dialog = builder.create();
        setCancelable(false);

        return dialog;
    }
}
