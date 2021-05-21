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
import org.smartregister.p2p.R;
import org.smartregister.p2p.contract.P2pModeSelectContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 13/03/2019
 */
@RunWith(RobolectricTestRunner.class)
public class StartDiscoveringModeProgressDialogTest {

    private StartDiscoveringModeProgressDialog startDiscoveringModeProgressDialog;

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
        startDiscoveringModeProgressDialog = new StartDiscoveringModeProgressDialog();
        startDiscoveringModeProgressDialog.show(fragmentManager, "dialog_discovering_progress");
    }

    @Test
    public void onCreateDialogShouldMakeFragmentDialogNotCancelable() {
        Assert.assertFalse(startDiscoveringModeProgressDialog.isCancelable());
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

        startDiscoveringModeProgressDialog.setDialogCancelCallback(dialogCancelCallback);
        Dialog dialog = startDiscoveringModeProgressDialog.getDialog();
        Button cancelButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
        cancelButton.callOnClick();

        Assert.assertNull(startDiscoveringModeProgressDialog.getDialog());
        Assert.assertTrue(results.get(0));
    }
}