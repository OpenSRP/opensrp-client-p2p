package org.smartregister.p2p.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.smartregister.p2p.R;
import org.smartregister.p2p.contract.P2pModeSelectContract;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 10/06/2019
 */

public class DevicesConnectedFragment extends Fragment {

    private P2pModeSelectContract.View.OnStartTransferClicked onStartTransferClicked;

    public static DevicesConnectedFragment create(@NonNull P2pModeSelectContract.View.OnStartTransferClicked onStartTransferClicked) {
        DevicesConnectedFragment devicesConnectedFragment = new DevicesConnectedFragment();
        devicesConnectedFragment.onStartTransferClicked = onStartTransferClicked;

        return devicesConnectedFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_devices_connected, container, false);
        Button okBtn = view.findViewById(R.id.btn_devicesConnectedFragment_okBtn);

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFragment();
                if (onStartTransferClicked != null) {
                    onStartTransferClicked.startTransferClicked();
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
}
