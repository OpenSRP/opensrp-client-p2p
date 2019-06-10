package org.smartregister.p2p.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.dialog.ConnectingDialog;
import org.smartregister.p2p.dialog.SkipQRScanDialog;
import org.smartregister.p2p.dialog.StartDiscoveringModeProgressDialog;
import org.smartregister.p2p.dialog.StartReceiveModeProgressDialog;
import org.smartregister.p2p.fragment.SyncProgressFragment;
import org.smartregister.p2p.fragment.P2PModeSelectFragment;
import org.smartregister.p2p.fragment.QRCodeGeneratorFragment;
import org.smartregister.p2p.fragment.QRCodeScanningFragment;
import org.smartregister.p2p.fragment.SyncCompleteTransferFragment;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.handler.OnActivityResultHandler;
import org.smartregister.p2p.handler.OnResumeHandler;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.presenter.P2PReceiverPresenter;
import org.smartregister.p2p.presenter.P2PSenderPresenter;
import org.smartregister.p2p.tasks.GenericAsyncTask;
import org.smartregister.p2p.util.Constants;
import org.smartregister.p2p.util.DialogUtils;
import org.smartregister.p2p.util.Permissions;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import timber.log.Timber;

public class P2pModeSelectActivity extends AppCompatActivity implements P2pModeSelectContract.View {


    private P2pModeSelectContract.SenderPresenter senderBasePresenter;
    private P2pModeSelectContract.ReceiverPresenter receiverBasePresenter;

    private ArrayList<OnActivityResultHandler> onActivityResultHandlers = new ArrayList<>();
    private ArrayList<OnResumeHandler> onResumeHandlers = new ArrayList<>();
    private ArrayList<OnActivityRequestPermissionHandler> onActivityRequestPermissionHandlers = new ArrayList<>();

    private P2PModeSelectFragment p2PModeSelectFragment;
    private SyncProgressFragment syncProgressFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_p2p_mode_select);

        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setHomeAsUpIndicator(R.drawable.ic_action_close);
        }

        prepareTrackingDetails();
        showP2PModeSelectFragment();
    }

    private void prepareTrackingDetails() {
        P2PLibrary.getInstance()
                .getDeviceMacAddress(this, new GenericAsyncTask.OnFinishedCallback<String>() {
                    @Override
                    public void onSuccess(@Nullable String result) {
                        if (result != null) {
                            P2PLibrary.getInstance()
                                    .setDeviceUniqueIdentifier(result);
                        } else {
                            Timber.e(getString(R.string.log_getting_mac_address_not_successful));
                            showFatalErrorDialog(R.string.an_error_occured, R.string.error_occurred_trying_to_get_mac_address);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        showFatalErrorDialog(R.string.an_error_occured, R.string.error_occurred_trying_to_get_mac_address);
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializePresenters();
    }

    public void showP2PModeSelectFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        p2PModeSelectFragment = new P2PModeSelectFragment();
        fragmentTransaction.replace(R.id.cl_p2pModeSelectActivity_parentLayout, p2PModeSelectFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void enableSendReceiveButtons(boolean enable) {
        if (p2PModeSelectFragment != null && p2PModeSelectFragment.isVisible()) {
            p2PModeSelectFragment.enableSendReceiveButtons(enable);
        } else {
            Timber.e("P2PModeSelectFragment is not available to %s send and receive buttons", (enable ? "enable" : "disable"));
        }
    }

    @Override
    public void showAdvertisingProgressDialog(@NonNull DialogCancelCallback dialogCancelCallback) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        StartReceiveModeProgressDialog newFragment = new StartReceiveModeProgressDialog();
        newFragment.setDialogCancelCallback(dialogCancelCallback);

        newFragment.show(fragmentManager, Constants.Dialog.START_RECEIVE_MODE_PROGRESS);
    }

    @Override
    public void showSyncProgressFragment(@NonNull String title, @NonNull SyncProgressFragment.SyncProgressDialogCallback syncProgressDialogCallback) {
        syncProgressFragment = SyncProgressFragment.create(title);
        syncProgressFragment.setSyncProgressDialogCallback(syncProgressDialogCallback);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.cl_p2pModeSelectActivity_parentLayout, syncProgressFragment);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void updateProgressDialog(@NonNull String progress, @NonNull String summary) {
        if (syncProgressFragment != null) {
            syncProgressFragment.setProgressText(progress);
            syncProgressFragment.setSummaryText(summary);
        } else {
            Timber.e("Could not update progress dialog with %s/%s because sync progress dialog is null", progress, summary);
        }
    }

    @Override
    public void updateProgressDialog(int progress) {
        if (syncProgressFragment != null) {
            syncProgressFragment.setProgress(progress);
        } else {
            Timber.e("Could not update progress dialog to %d because sync progress dialog is null", progress);
        }
    }

    @Override
    public boolean removeSyncProgressDialog() {
        syncProgressFragment = null;
        return removeDialog(Constants.Dialog.SYNC_PROGRESS_DIALOG);
    }

    @Override
    public boolean removeAdvertisingProgressDialog() {
        return removeDialog(Constants.Dialog.START_RECEIVE_MODE_PROGRESS);
    }

    @Override
    public void showSyncCompleteFragment(boolean isSuccess, @Nullable String deviceName, @NonNull SyncCompleteTransferFragment.OnCloseClickListener onCloseClickListener, @NonNull String summaryReport) {
        SyncCompleteTransferFragment syncCompleteTransferFragment = new SyncCompleteTransferFragment();
        syncCompleteTransferFragment.setSuccess(isSuccess);
        syncCompleteTransferFragment.setOnCloseClickListener(onCloseClickListener);
        syncCompleteTransferFragment.setDeviceName(deviceName);
        syncCompleteTransferFragment.setTransferSummaryReport(summaryReport);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.cl_p2pModeSelectActivity_parentLayout, syncCompleteTransferFragment);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void showDiscoveringProgressDialog(@NonNull DialogCancelCallback dialogCancelCallback) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        StartDiscoveringModeProgressDialog newFragment = new StartDiscoveringModeProgressDialog();
        newFragment.setDialogCancelCallback(dialogCancelCallback);

        newFragment.show(fragmentManager, Constants.Dialog.START_SEND_MODE_PROGRESS);
    }

    @Override
    public boolean removeDiscoveringProgressDialog() {
        return removeDialog(Constants.Dialog.START_SEND_MODE_PROGRESS);
    }

    private boolean removeDialog(@NonNull String tag) {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag(tag);

        if (fragment instanceof DialogFragment) {
            ((DialogFragment) fragment)
                    .dismiss();

            return true;
        }

        return false;
    }

    @Override
    public void showQRCodeScanningFragment(@NonNull String deviceName, @NonNull QRCodeScanningFragment.QRCodeScanDialogCallback qrCodeScanDialogCallback) {
        QRCodeScanningFragment newFragment = QRCodeScanningFragment.create(deviceName);
        newFragment.setOnQRRecognisedListener(qrCodeScanDialogCallback);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.cl_p2pModeSelectActivity_parentLayout, newFragment);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void showQRCodeGeneratorFragment(@NonNull String authenticationCode, @NonNull String deviceName
            , @NonNull QRCodeGeneratorFragment.QRCodeGeneratorCallback qrCodeGeneratorCallback) {
        QRCodeGeneratorFragment newFragment = new QRCodeGeneratorFragment();
        newFragment.setAuthenticationCode(authenticationCode);
        newFragment.setDeviceName(deviceName);
        newFragment.setQrCodeGeneratorCallback(qrCodeGeneratorCallback);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.cl_p2pModeSelectActivity_parentLayout, newFragment);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void showConnectionAcceptDialog(@NonNull String receiverDeviceName, @NonNull String authenticationCode
            , @NonNull final DialogInterface.OnClickListener onClickListener) {
        new android.support.v7.app.AlertDialog.Builder(this)
                .setMessage(String.format(getString(R.string.accept_connection_dialog_content), authenticationCode))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onClickListener.onClick(dialog, which);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onClickListener.onClick(dialog, which);
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void requestPermissions(@NonNull List<String> unauthorisedPermissions) {
        Permissions.request(this, unauthorisedPermissions.toArray(new String[]{}), Constants.RqCode.PERMISSIONS);
    }

    @NonNull
    @Override
    public List<String> getUnauthorisedPermissions() {
        TreeSet<DataType> dataTypes = P2PLibrary.getInstance().getReceiverTransferDao().getDataTypes();
        boolean hasMediaDataTypes = false;

        for (DataType dataType: dataTypes) {
            if (dataType.getType() == DataType.Type.MEDIA) {
                hasMediaDataTypes = true;
                break;
            }
        }

        return Permissions.getUnauthorizedCriticalPermissions(
                getContext(),
                hasMediaDataTypes ? Permissions.CRITICAL_PERMISSIONS_WITH_STORAGE
                        : Permissions.CRITICAL_PERMISSIONS
        );
    }

    @Override
    public boolean addOnActivityResultHandler(@NonNull OnActivityResultHandler onActivityResultHandler) {
        return !onActivityResultHandlers.contains(onActivityResultHandler)
                && onActivityResultHandlers.add(onActivityResultHandler);
    }

    @Override
    public boolean removeOnActivityResultHandler(@NonNull OnActivityResultHandler onActivityResultHandler) {
        return onActivityResultHandlers.remove(onActivityResultHandler);
    }

    @Override
    public boolean addOnResumeHandler(@NonNull OnResumeHandler onResumeHandler) {
        return !onResumeHandlers.contains(onResumeHandler) && onResumeHandlers.add(onResumeHandler);
    }

    @Override
    public boolean removeOnResumeHandler(@NonNull OnResumeHandler onResumeHandler) {
        return onResumeHandlers.remove(onResumeHandler);
    }

    @Override
    public boolean isLocationEnabled() {
        LocationManager lm = ((LocationManager) getSystemService(Context.LOCATION_SERVICE));
        return lm != null && (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    @Override
    public void dismissAllDialogs() {
        DialogUtils.dismissAllDialogs(getSupportFragmentManager());
    }

    @Override
    public void requestEnableLocation(@NonNull final OnLocationEnabled onLocationEnabled) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        locationRequest.setInterval(20000);
        locationRequest.setFastestInterval(20000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);

        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                    onLocationEnabled.locationEnabled();
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;

                                addOnActivityResultHandler(new OnActivityResultHandler() {
                                    @Override
                                    public int getRequestCode() {
                                        return Constants.RqCode.LOCATION_SETTINGS;
                                    }

                                    @Override
                                    public void handleActivityResult(int resultCode, Intent data) {
                                        removeOnActivityResultHandler(this);

                                        if (data != null) {
                                            if (resultCode == Activity.RESULT_OK) {
                                                // All required changes were successfully made
                                                onLocationEnabled.locationEnabled();
                                            } else if (resultCode == Activity.RESULT_CANCELED) {
                                                // The user was asked to change settings, but chose not to
                                                showLocationEnableRejectionDialog();
                                            }
                                        }
                                    }
                                });
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        P2pModeSelectActivity.this,
                                        Constants.RqCode.LOCATION_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                                Timber.e(e);
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                                Timber.e(e);
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            Timber.e("Location settings are not satisfied and we have no way to fix the settings");
                            break;


                        default:
                            // This should almost never happen
                            Timber.e(exception, "The location settings API returned an unresolved status code %d", exception.getStatusCode());
                            break;
                    }
                }
            }
        });
    }

    @Override
    public void showToast(@NonNull String text, int length) {
        Toast.makeText(this, text, length)
                .show();
    }

    private void showLocationEnableRejectionDialog() {
        new AlertDialog.Builder(P2pModeSelectActivity.this)
                .setTitle(R.string.location_service_disabled)
                .setMessage(R.string.file_data_sharing_will_not_work_without_location)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void showFatalErrorDialog(@StringRes int title, @StringRes int message) {
        new AlertDialog.Builder(P2pModeSelectActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        P2pModeSelectActivity.this.finish();
                    }
                })
                .show();
    }

    @Override
    public void showConnectingDialog(@NonNull DialogCancelCallback dialogCancelCallback) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ConnectingDialog newFragment = new ConnectingDialog();
        newFragment.setDialogCancelCallback(dialogCancelCallback);

        newFragment.show(fragmentManager, Constants.Dialog.CONNECTING);
    }

    @Override
    public void removeConnectingDialog() {
        removeDialog(Constants.Dialog.CONNECTING);
    }

    @Override
    public void showSkipQRScanDialog(@NonNull String peerDeviceStatus, @NonNull String deviceName, @NonNull SkipQRScanDialog.SkipDialogCallback skipDialogCallback) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        SkipQRScanDialog newFragment = new SkipQRScanDialog();
        newFragment.setPeerDeviceStatus(peerDeviceStatus);
        newFragment.setDeviceName(deviceName);
        newFragment.setSkipDialogCallback(skipDialogCallback);

        newFragment.show(fragmentManager, Constants.Dialog.SKIP_QR_SCAN);
    }

    @Override
    public boolean removeSkipQRScanDialog() {
        return removeDialog(Constants.Dialog.SKIP_QR_SCAN);
    }

    @Override
    protected void onResume() {
        super.onResume();

        for (OnResumeHandler onResumeHandler: onResumeHandlers) {
            onResumeHandler.onResume();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (OnActivityRequestPermissionHandler onActivityRequestPermissionHandler: onActivityRequestPermissionHandlers) {
            if (requestCode == onActivityRequestPermissionHandler.getRequestCode()) {
                onActivityRequestPermissionHandler.handlePermissionResult(permissions, grantResults);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        for (OnActivityResultHandler onActivityResultHandler: onActivityResultHandlers) {
            if (requestCode == onActivityResultHandler.getRequestCode()) {
                onActivityResultHandler.handleActivityResult(resultCode, data); getString(R.string.connected);
            }
        }
    }

    @NonNull
    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void initializePresenters() {
        receiverBasePresenter = new P2PReceiverPresenter(this);
        senderBasePresenter = new P2PSenderPresenter(this);
    }

    @Override
    public boolean addOnActivityRequestPermissionHandler(@NonNull OnActivityRequestPermissionHandler onActivityRequestPermissionHandler) {
        return !onActivityRequestPermissionHandlers.contains(onActivityRequestPermissionHandler)
                && onActivityRequestPermissionHandlers.add(onActivityRequestPermissionHandler);
    }

    @Override
    public boolean removeOnActivityRequestPermissionHandler(@NonNull OnActivityRequestPermissionHandler onActivityRequestPermissionHandler) {
        return onActivityRequestPermissionHandlers.remove(onActivityRequestPermissionHandler);
    }

    @Override
    protected void onStop() {
        super.onStop();

        receiverBasePresenter.onStop();
        senderBasePresenter.onStop();
    }

    public P2pModeSelectContract.SenderPresenter getSenderBasePresenter() {
        return senderBasePresenter;
    }

    public P2pModeSelectContract.ReceiverPresenter getReceiverBasePresenter() {
        return receiverBasePresenter;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
