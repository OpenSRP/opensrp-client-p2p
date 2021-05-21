package org.smartregister.p2p.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.smartregister.p2p.R;
import org.smartregister.p2p.contract.P2pModeSelectContract;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public class ConnectingDialog extends DialogFragment {

    private P2pModeSelectContract.View.DialogCancelCallback dialogCancelCallback;

    public void setDialogCancelCallback(@NonNull P2pModeSelectContract.View.DialogCancelCallback dialogCancelCallback) {
        this.dialogCancelCallback = dialogCancelCallback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_connecting, null))
                .setTitle(R.string.please_wait)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialogCancelCallback != null) {
                            dialogCancelCallback.onCancelClicked(new DialogInterface() {
                                @Override
                                public void cancel() {
                                    ConnectingDialog.this.dismiss();
                                }

                                @Override
                                public void dismiss() {
                                    ConnectingDialog.this.dismiss();
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
