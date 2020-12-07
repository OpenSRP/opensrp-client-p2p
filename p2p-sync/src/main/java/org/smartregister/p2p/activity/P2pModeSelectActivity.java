package org.smartregister.p2p.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.dialog.ConnectingDialog;
import org.smartregister.p2p.dialog.ReceiverConnectionInfoDialog;
import org.smartregister.p2p.dialog.SenderApprovalDialog;
import org.smartregister.p2p.dialog.SkipQRScanDialog;
import org.smartregister.p2p.dialog.StartDiscoveringModeProgressDialog;
import org.smartregister.p2p.dialog.StartReceiveModeProgressDialog;
import org.smartregister.p2p.fragment.DevicesConnectedFragment;
import org.smartregister.p2p.fragment.ErrorFragment;
import org.smartregister.p2p.fragment.P2PModeSelectFragment;
import org.smartregister.p2p.fragment.QRCodeGeneratorFragment;
import org.smartregister.p2p.fragment.QRCodeScanningFragment;
import org.smartregister.p2p.fragment.SyncCompleteTransferFragment;
import org.smartregister.p2p.fragment.SyncProgressFragment;
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
        showP2PModeSelectFragment(true);
        checkForPlayServices();
    }

    private void checkForPlayServices() {
        GoogleApiAvailability.getInstance()
                .makeGooglePlayServicesAvailable(this)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void ignored) {
                        Timber.v("Play services is available");

                        // TODO Look for a better way to check minimum required version of Google Play Services (hard coded for Nearby v16)
                        if (getPlayServicesVersion() < 12451000) {
                            updatePlayStoreOrDie();
                        }
                    }
                }).addOnFailureListener(this,
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timber.v("Play services is not available");
                        showFatalErrorDialog(R.string.error_play_services_title, R.string.error_play_services);
                    }
                });

    }

    private int getPlayServicesVersion() {
        try {
            return getPackageManager().getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    private AlertDialog updatePlayStoreOrDie() {
        return new AlertDialog.Builder(P2pModeSelectActivity.this)
                .setTitle(R.string.error_play_services_title)
                .setMessage(R.string.error_play_services_out_of_date)
                .setNegativeButton(R.string.maybe_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        P2pModeSelectActivity.this.finish();
                    }
                })
                .setPositiveButton(R.string.update_now, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Constants.PlayStore.PLAY_SERVICES)));
                        } catch (android.content.ActivityNotFoundException ex) {
                            // if play store is not installed
                            Timber.e(ex);
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + Constants.PlayStore.PLAY_SERVICES)));
                        }

                        P2pModeSelectActivity.this.finish();
                    }
                })
                .show();
    }


    private void onMacAddressResolutionFailure() {
        // request user to turn on wifi
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (!wifiManager.isWifiEnabled()) {
                showFatalErrorDialog(R.string.an_error_occured, R.string.error_try_turning_wifi_on);
                return;
            }
        }

        showFatalErrorDialog(R.string.an_error_occured, R.string.error_occurred_trying_to_get_mac_address);
    }

    private void prepareTrackingDetails() {
        P2PLibrary.getInstance()
                .getDeviceAddress(this, new GenericAsyncTask.OnFinishedCallback<String>() {
                    @Override
                    public void onSuccess(@Nullable String result) {
                        if (result != null) {
                            P2PLibrary.getInstance()
                                    .setDeviceUniqueIdentifier(result);
                        } else {
                            Timber.e(getString(R.string.log_getting_mac_address_not_successful));
                            onMacAddressResolutionFailure();
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

    public void showP2PModeSelectFragment(boolean enableButtons) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        p2PModeSelectFragment = new P2PModeSelectFragment();
        p2PModeSelectFragment.setEnableButtons(enableButtons);
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
        transaction.replace(R.id.cl_p2pModeSelectActivity_parentLayout, syncProgressFragment, Constants.Fragment.SYNC_PROGRESS);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void updateProgressFragment(@NonNull String progress, @NonNull String summary) {
        if (syncProgressFragment != null) {
            syncProgressFragment.setProgressText(progress);
            syncProgressFragment.setSummaryText(summary);
        } else {
            Timber.e("Could not update progress dialog with %s/%s because sync progress dialog is null", progress, summary);
        }
    }

    @Override
    public void updateProgressFragment(int progress) {
        if (syncProgressFragment != null) {
            syncProgressFragment.setProgress(progress);
        } else {
            Timber.e("Could not update progress dialog to %d because sync progress dialog is null", progress);
        }
    }

    @Override
    public boolean removeAdvertisingProgressDialog() {
        return removeDialog(Constants.Dialog.START_RECEIVE_MODE_PROGRESS);
    }

    @Override
    public void showSyncCompleteFragment(boolean isSuccess, @Nullable String deviceName
            , @NonNull SyncCompleteTransferFragment.OnCloseClickListener onCloseClickListener
            , @NonNull String summaryReport, boolean isSender) {
        SyncCompleteTransferFragment syncCompleteTransferFragment = new SyncCompleteTransferFragment();
        syncCompleteTransferFragment.setSuccess(isSuccess);
        syncCompleteTransferFragment.setSender(isSender);
        syncCompleteTransferFragment.setOnCloseClickListener(onCloseClickListener);
        syncCompleteTransferFragment.setDeviceName(deviceName);
        syncCompleteTransferFragment.setTransferSummaryReport(summaryReport);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.cl_p2pModeSelectActivity_parentLayout, syncCompleteTransferFragment, Constants.Fragment.SYNC_COMPLETE);

        // Commit the transaction
        transaction.commitAllowingStateLoss();
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

    @Override
    public boolean removeDialog(@NonNull String tag) {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag(tag);

        if (fragment instanceof DialogFragment) {
            ((DialogFragment) fragment)
                    .dismiss();

            return true;
        }

        return false;
    }

    private boolean removeFragment(@NonNull String tag) {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag(tag);

        if (fragment != null) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(fragment);
            fragmentTransaction.commit();

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
        transaction.replace(R.id.cl_p2pModeSelectActivity_parentLayout, newFragment, Constants.Fragment.QR_CODE_SCANNING);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void showQRCodeGeneratorFragment(@NonNull String authenticationCode, @NonNull String deviceName
            , @NonNull QRCodeGeneratorFragment.QRCodeGeneratorCallback qrCodeGeneratorCallback) {
        // here
        QRCodeGeneratorFragment newFragment = new QRCodeGeneratorFragment();
        newFragment.setAuthenticationCode(authenticationCode);
        newFragment.setDeviceName(deviceName);
        newFragment.setQrCodeGeneratorCallback(qrCodeGeneratorCallback);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.cl_p2pModeSelectActivity_parentLayout, newFragment, Constants.Fragment.AUTHENTICATION_QR_CODE_GENERATOR);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void showSenderApprovalDialog(@NonNull String authenticationCode, @NonNull String deviceName, @Nullable DialogCancelCallback dialogCancelCallback, @NonNull DialogApprovedCallback dialogApprovedCallback) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        SenderApprovalDialog newFragment = new SenderApprovalDialog();
        newFragment.setConnectionDeviceName(deviceName);
        newFragment.setConnectionKey(authenticationCode);
        newFragment.setDialogCancelCallback(dialogCancelCallback);
        newFragment.setDialogApprovedCallback(dialogApprovedCallback);

        newFragment.show(fragmentManager, Constants.Dialog.DISPLAY_APPROVAL_KEY);
    }

    @Override
    public void showReceiverApprovalDialog(@NonNull String authenticationCode, @NonNull String deviceName, @Nullable DialogCancelCallback dialogCancelCallback, @NonNull DialogApprovedCallback dialogApprovedCallback) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ReceiverConnectionInfoDialog newFragment = new ReceiverConnectionInfoDialog();
        newFragment.setConnectionDeviceName(deviceName);
        newFragment.setConnectionKey(authenticationCode);
        newFragment.setDialogCancelCallback(dialogCancelCallback);
        newFragment.setDialogApprovedCallback(dialogApprovedCallback);

        newFragment.show(fragmentManager, Constants.Dialog.DISPLAY_APPROVAL_KEY);
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
    public void showErrorFragment(@NonNull String title, @NonNull String message, @Nullable ErrorFragment.OnOkClickCallback onOkClickCallback) {
        ErrorFragment errorFragment = ErrorFragment.create(title, message);
        errorFragment.setOnOkClickCallback(onOkClickCallback);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.cl_p2pModeSelectActivity_parentLayout, errorFragment, Constants.Fragment.ERROR);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void removeQRCodeScanningFragment() {
        removeFragment(Constants.Fragment.QR_CODE_SCANNING);
    }

    @Override
    public void removeQRCodeGeneratorFragment() {
        removeFragment(Constants.Fragment.AUTHENTICATION_QR_CODE_GENERATOR);
    }

    @Override
    public void showDevicesConnectedFragment(@NonNull OnStartTransferClicked onStartTransferClicked) {
        DevicesConnectedFragment errorFragment = DevicesConnectedFragment.create(onStartTransferClicked);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.cl_p2pModeSelectActivity_parentLayout, errorFragment, Constants.Fragment.DEVICES_CONNECTED);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public boolean isSyncProgressFragmentShowing() {
        return getSupportFragmentManager().findFragmentByTag(Constants.Fragment.SYNC_PROGRESS) != null;
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

        for (DataType dataType : dataTypes) {
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

    @VisibleForTesting
    void showFatalErrorDialog(@StringRes int title, @StringRes int message) {
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

        for (OnResumeHandler onResumeHandler : onResumeHandlers) {
            onResumeHandler.onResume();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // prevent concurrent modification error
        for (OnActivityRequestPermissionHandler onActivityRequestPermissionHandler : new ArrayList<>(onActivityRequestPermissionHandlers)) {
            if (requestCode == onActivityRequestPermissionHandler.getRequestCode()) {
                onActivityRequestPermissionHandler.handlePermissionResult(permissions, grantResults);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        for (OnActivityResultHandler onActivityResultHandler : onActivityResultHandlers) {
            if (requestCode == onActivityResultHandler.getRequestCode()) {
                onActivityResultHandler.handleActivityResult(resultCode, data);
                getString(R.string.connected);
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
