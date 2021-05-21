package org.smartregister.p2p.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.smartregister.p2p.R;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 03/05/2019
 */

public class SyncCompleteTransferFragment extends Fragment {

    private OnCloseClickListener onCloseClickListener;
    private String transferSummary;
    private boolean isSuccess;
    private boolean isSender;

    @Nullable
    private String deviceName;

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public void setSender(boolean sender) {
        isSender = sender;
    }

    public void setTransferSummaryReport(@NonNull String transferSummaryReport) {
        this.transferSummary = transferSummaryReport;
    }

    public void setOnCloseClickListener(@Nullable OnCloseClickListener onCloseClickListener) {
        this.onCloseClickListener = onCloseClickListener;
    }

    public void setDeviceName(@Nullable String deviceName) {
        this.deviceName = deviceName;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_successful_transfer, container, false);
        TextView transferSummaryTv = (TextView) view.findViewById(R.id.tv_successfulTransferFragment_transferSummary);

        String[] summarySplit = this.transferSummary == null ? new String[]{""} : this.transferSummary.split(" "); // TODO: Returns to NPE
        String recordsSent = "0";

        if (summarySplit.length > 0) {
            recordsSent = summarySplit[0].trim();
        }

        if (isSuccess) {
            transferSummaryTv.setText(this.transferSummary);

            if (!isSender && (recordsSent.length() > 1 || !"0".equals(recordsSent))) {
                view.findViewById(R.id.tv_successfulTransferFragment_processingDisclaimer)
                        .setVisibility(View.VISIBLE);
            }
        } else {
            transferSummaryTv.setText(String.format(getString(R.string.connection_lost_transfer_summary), deviceName, recordsSent));
        }

        if (!isSuccess) {
            ImageView imageView = view.findViewById(R.id.iv_successfulTransferFragment_successMark);
            imageView.setImageResource(R.drawable.ic_fail);

            ((TextView) view.findViewById(R.id.tv_successfulTransferFragment_transferSuccessText))
                    .setText(R.string.connection_lost);
        }

        Button closeBtn = view.findViewById(R.id.btn_successfulTransferFragment_closeBtn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onCloseClickListener != null) {
                    onCloseClickListener.onCloseClicked();
                }
            }
        });

        return view;
    }

    public interface OnCloseClickListener {

        void onCloseClicked();

    }
}
