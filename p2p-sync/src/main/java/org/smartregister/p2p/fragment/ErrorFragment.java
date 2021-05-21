package org.smartregister.p2p.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.smartregister.p2p.R;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 10/06/2019
 */

public class ErrorFragment extends Fragment {

    private OnOkClickCallback onOkClickCallback;

    private String title;
    private String message;

    public static ErrorFragment create(@NonNull String title, @NonNull String message) {
        ErrorFragment errorFragment = new ErrorFragment();
        errorFragment.title = title;
        errorFragment.message = message;

        return errorFragment;
    }

    public void setOnOkClickCallback(@Nullable OnOkClickCallback onOkClickCallback) {
        this.onOkClickCallback = onOkClickCallback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_error, container, false);

        TextView titleTv = view.findViewById(R.id.tv_errorFragment_title);
        TextView messageTv = view.findViewById(R.id.tv_errorFragment_message);
        Button okBtn = view.findViewById(R.id.btn_errorFragment_okBtn);

        titleTv.setText(title);
        messageTv.setText(message);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onOkClickCallback != null) {
                    onOkClickCallback.onOkClicked();
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

    public interface OnOkClickCallback {
        void onOkClicked();
    }
}
