package org.smartregister.p2p.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.smartregister.p2p.R;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public class SyncProgressFragment extends Fragment {

    private SyncProgressDialogCallback syncProgressDialogCallback;

    private ProgressBar progressBar;
    private TextView progressTextView;
    private TextView summaryTextView;
    private String title;

    public static SyncProgressFragment create(@NonNull String title) {
        SyncProgressFragment syncProgressFragment = new SyncProgressFragment();
        syncProgressFragment.title = title;

        return syncProgressFragment;
    }

    public void setSummaryText(@NonNull String summaryText) {
        if (summaryTextView != null) {
            summaryTextView.setText(summaryText);
        }
    }

    public void setProgressText(@NonNull String progressText) {
        if (progressTextView != null) {
            progressTextView.setText(progressText);
        }
    }

    public void setProgress(int progress) {
        if (progressBar != null) {
            if (progress > -1) {
                progressBar.setIndeterminate(false);
                progressBar.setProgress(progress);
            } else {
                progressBar.setIndeterminate(true);
            }
        }
    }

    public void setSyncProgressDialogCallback(@NonNull SyncProgressDialogCallback syncProgressDialogCallback) {
        this.syncProgressDialogCallback = syncProgressDialogCallback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sync_progress, container, false);

        progressBar = view.findViewById(R.id.pb_syncProgressDialog_progressBar);
        progressTextView = view.findViewById(R.id.tv_syncProgressDialog_progressText);
        summaryTextView = view.findViewById(R.id.tv_syncProgressDialog_summaryText);
        TextView progressTitleTextView = view.findViewById(R.id.tv_syncProgressDialog_startingText);

        if (progressTitleTextView != null) {
            progressTitleTextView.setText(title);
        }

        return view;
    }

    @Override
    public void onDestroy() {
        if (syncProgressDialogCallback != null) {
            syncProgressDialogCallback.onCancelClicked();
        }

        super.onDestroy();
    }

    public void closeFragment() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
        }
    }

    public interface SyncProgressDialogCallback {

        void onCancelClicked();
    }
}
