package org.smartregister.p2p.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.p2p.R;
import org.smartregister.p2p.TestApplication;
import org.smartregister.p2p.contract.P2pModeSelectContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */
@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class, manifest = Config.NONE)
public class StartReceiveModeProgressDialogTest {

    private StartReceiveModeProgressDialog startReceiveModeProgressDialog;

    @Before
    public void setUp() throws Exception {
        FragmentActivity activity = Robolectric
                .buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        activity.setTheme(R.style.Theme_AppCompat_Light);

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        startReceiveModeProgressDialog = new StartReceiveModeProgressDialog();
        startReceiveModeProgressDialog.show(fragmentManager, "dialog_receive_progress");
    }

    @Test
    public void onCreateDialogShouldMakeFragmentDialogNotCancelable() {
        Assert.assertFalse(startReceiveModeProgressDialog.isCancelable());
    }

    @Test
    public void cancelButtonShouldCancelDialogAndCallDialogCancelCallback() {
        final List<Boolean> results = new ArrayList<>();
        P2pModeSelectContract.View.DialogCancelCallback dialogCancelCallback = new P2pModeSelectContract.View.DialogCancelCallback() {
            @Override
            public void onCancelClicked(DialogInterface dialogInterface) {
                results.add(true);
            }
        };

        startReceiveModeProgressDialog.setDialogCancelCallback(dialogCancelCallback);
        Dialog dialog = startReceiveModeProgressDialog.getDialog();
        Button cancelButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
        cancelButton.callOnClick();

        Assert.assertNull(startReceiveModeProgressDialog.getDialog());
        Assert.assertTrue(results.get(0));
    }
}