package org.smartregister.p2p.authenticator;

import android.content.DialogInterface;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.vision.barcode.Barcode;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.fragment.QRCodeScanningFragment;
import org.smartregister.p2p.sync.DiscoveredDevice;

import java.util.ArrayList;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 19/03/2019
 */

@RunWith(RobolectricTestRunner.class)
public class SenderConnectionAuthenticatorTest {

    @Rule
    public MockitoRule mockitoJUnit = MockitoJUnit.rule();

    @Mock
    private P2pModeSelectContract.View view;
    @Mock
    private P2pModeSelectContract.SenderPresenter senderPresenter;

    private SenderConnectionAuthenticator senderConnectionAuthenticator;

    @Before
    public void setUp() {
        Mockito.doReturn(view)
                .when(senderPresenter)
                .getView();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                int resId = invocation.getArgument(0);
                return RuntimeEnvironment.application.getString(resId);
            }
        })
                .when(view)
                .getString(Mockito.anyInt());

        senderConnectionAuthenticator = Mockito.spy(new SenderConnectionAuthenticator(senderPresenter));
        Mockito.doReturn(false).when(senderConnectionAuthenticator).allowSkipQrCodeScan();
    }

    @Test
    public void authenticateShouldCallAuthenticationFailedCallbackWhenConnectionInfoIsNull() {
        DiscoveredDevice discoveredDevice = new DiscoveredDevice("id", Mockito.mock(DiscoveredEndpointInfo.class));
        BaseSyncConnectionAuthenticator.AuthenticationCallback authenticationCallback = Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class);
        final ArrayList<Object> innerClassResults = new ArrayList<>();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Exception e = invocation.getArgument(1);
                innerClassResults.add(e);

                return null;
            }
        }).when(authenticationCallback)
                .onAuthenticationFailed(Mockito.anyString(), Mockito.any(Exception.class));

        senderConnectionAuthenticator.authenticate(discoveredDevice, authenticationCallback);

        Mockito.verify(authenticationCallback, Mockito.times(1))
                .onAuthenticationFailed(Mockito.anyString(), Mockito.any(Exception.class));
        Assert.assertEquals("DiscoveredDevice information passed is invalid", ((Exception) innerClassResults.get(0)).getMessage());
    }

    @Test
    public void authenticateShouldCallAuthenticationFailedCallbackWhenConnectionInfoIsValidAndConnectionIsIncoming() {
        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(true)
                .when(connectionInfo)
                .isIncomingConnection();

        DiscoveredDevice discoveredDevice = new DiscoveredDevice("id", connectionInfo);
        BaseSyncConnectionAuthenticator.AuthenticationCallback authenticationCallback = Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class);
        final ArrayList<Object> innerClassResults = new ArrayList<>();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Exception e = invocation.getArgument(1);
                innerClassResults.add(e);

                return null;
            }
        }).when(authenticationCallback)
                .onAuthenticationFailed(Mockito.anyString(), Mockito.any(Exception.class));

        senderConnectionAuthenticator.authenticate(discoveredDevice, authenticationCallback);

        Mockito.verify(authenticationCallback, Mockito.times(1))
                .onAuthenticationFailed(Mockito.anyString(), Mockito.any(Exception.class));
        Assert.assertEquals("DiscoveredDevice information passed is invalid", ((Exception) innerClassResults.get(0)).getMessage());
    }


    @Test
    public void authenticateShouldShowQrScanningDialogWhenConnectionInfoIsValidAndConnectionIsNotIncoming() {
        String authenticationCode = "iowejncCJD";
        String deviceName = "SAMSUNG SM78";

        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(false)
                .when(connectionInfo)
                .isIncomingConnection();

        Mockito.doReturn(deviceName)
                .when(connectionInfo)
                .getEndpointName();

        Mockito.doReturn(authenticationCode)
                .when(connectionInfo)
                .getAuthenticationToken();

        DiscoveredDevice discoveredDevice = new DiscoveredDevice("id", connectionInfo);
        BaseSyncConnectionAuthenticator.AuthenticationCallback authenticationCallback
                = Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class);

        senderConnectionAuthenticator.authenticate(discoveredDevice, authenticationCallback);

        Mockito.verify(view, Mockito.times(1))
                .showQRCodeScanningFragment(Mockito.anyString()
                        , Mockito.any(QRCodeScanningFragment.QRCodeScanDialogCallback.class));
    }

    @Test
    public void authenticationShouldCallAuthenticationSuccessfulCallbackWhenQRCodeIsScannedAndCodeIsSimilar() {
        final String authenticationCode = "iowejncCJD";
        String deviceName = "SAMSUNG SM78";

        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(false)
                .when(connectionInfo)
                .isIncomingConnection();

        Mockito.doReturn(deviceName)
                .when(connectionInfo)
                .getEndpointName();

        Mockito.doReturn(authenticationCode)
                .when(connectionInfo)
                .getAuthenticationToken();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                QRCodeScanningFragment.QRCodeScanDialogCallback qrCodeScanDialogCallback = invocation.getArgument(1);
                Barcode barcode = new Barcode();
                barcode.displayValue = authenticationCode;
                barcode.rawValue = authenticationCode;

                SparseArray<Barcode> sparseArray = new SparseArray<>();
                sparseArray.append(0, barcode);

                qrCodeScanDialogCallback.qrCodeScanned(sparseArray);
                return null;
            }
        })
                .when(view)
                .showQRCodeScanningFragment(Mockito.anyString()
                        , Mockito.any(QRCodeScanningFragment.QRCodeScanDialogCallback.class));

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArgument(0))
                        .run();
                return null;
            }
        })
                .when(view)
                .runOnUiThread(Mockito.any(Runnable.class));

        DiscoveredDevice discoveredDevice = new DiscoveredDevice("id", connectionInfo);
        BaseSyncConnectionAuthenticator.AuthenticationCallback authenticationCallback = Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class);

        senderConnectionAuthenticator.authenticate(discoveredDevice, authenticationCallback);

        Mockito.verify(authenticationCallback, Mockito.times(1))
                .onAuthenticationSuccessful();
    }

    @Test
    public void authenticationShouldCallAuthenticationFailedCallbackWhenQRCodeIsScannedAndCodeIsDifferent() {
        final String authenticationCode = "iowejncCJD";
        String deviceName = "SAMSUNG SM78";

        final ArrayList<Object> innerClassResults = new ArrayList<>();

        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(false)
                .when(connectionInfo)
                .isIncomingConnection();

        Mockito.doReturn(deviceName)
                .when(connectionInfo)
                .getEndpointName();

        Mockito.doReturn(authenticationCode)
                .when(connectionInfo)
                .getAuthenticationToken();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                QRCodeScanningFragment.QRCodeScanDialogCallback qrCodeScanDialogCallback = invocation.getArgument(1);
                String scannedCode = "different-code";
                Barcode barcode = new Barcode();
                barcode.displayValue = scannedCode;
                barcode.rawValue = scannedCode;

                SparseArray<Barcode> sparseArray = new SparseArray<>();
                sparseArray.append(0, barcode);

                qrCodeScanDialogCallback.qrCodeScanned(sparseArray);
                return null;
            }
        })
                .when(view)
                .showQRCodeScanningFragment(Mockito.anyString()
                        , Mockito.any(QRCodeScanningFragment.QRCodeScanDialogCallback.class));

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArgument(0))
                        .run();
                return null;
            }
        })
                .when(view)
                .runOnUiThread(Mockito.any(Runnable.class));

        DiscoveredDevice discoveredDevice = new DiscoveredDevice("id", connectionInfo);
        BaseSyncConnectionAuthenticator.AuthenticationCallback authenticationCallback = Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class);

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                innerClassResults.add((Exception) invocation.getArgument(1));
                return null;
            }
        })
                .when(authenticationCallback)
                .onAuthenticationFailed(Mockito.anyString(), Mockito.any(Exception.class));

        senderConnectionAuthenticator.authenticate(discoveredDevice, authenticationCallback);

        Mockito.verify(authenticationCallback, Mockito.times(1))
                .onAuthenticationFailed(Mockito.anyString(), Mockito.any(Exception.class));

        Assert.assertEquals("Authentication tokens do not match", ((Exception) innerClassResults.get(0)).getMessage());
    }

    @Test
    public void authenticationShouldShowConnectionAcceptDialogWhenQRCodeScanningIsSkipped() {
        String authenticationCode = "iowejncCJD";
        String deviceName = "SAMSUNG SM78";

        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(false)
                .when(connectionInfo)
                .isIncomingConnection();

        Mockito.doReturn(deviceName)
                .when(connectionInfo)
                .getEndpointName();

        Mockito.doReturn(authenticationCode)
                .when(connectionInfo)
                .getAuthenticationToken();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                QRCodeScanningFragment.QRCodeScanDialogCallback qrCodeScanDialogCallback = invocation.getArgument(1);
                qrCodeScanDialogCallback.onSkipClicked();
                return null;
            }
        })
                .when(view)
                .showQRCodeScanningFragment(Mockito.eq(deviceName), Mockito.any(QRCodeScanningFragment.QRCodeScanDialogCallback.class));

        senderConnectionAuthenticator.authenticate(new DiscoveredDevice("id", connectionInfo)
                , Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class));

        Mockito.verify(view, Mockito.times(1))
                .showConnectingDialog(Mockito.any(P2pModeSelectContract.View.DialogCancelCallback.class));
    }

    /*@Test
    public void authenticationShouldCallAuthenticationFailedWhenQRCodeScanningIsCancelledAndConnectionRejectedByUser() {
        String authenticationCode = "iowejncCJD";
        String deviceName = "SAMSUNG SM78";

        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(false)
                .when(connectionInfo)
                .isIncomingConnection();

        Mockito.doReturn(deviceName)
                .when(connectionInfo)
                .getEndpointName();

        Mockito.doReturn(authenticationCode)
                .when(connectionInfo)
                .getAuthenticationToken();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                QRCodeScanningFragment.QRCodeScanDialogCallback qrCodeScanDialogCallback = invocation.getArgument(1);
                qrCodeScanDialogCallback.onSkipClicked();
                return null;
            }
        })
                .when(view)
                .showQRCodeScanningFragment(Mockito.eq(deviceName), Mockito.any(QRCodeScanningFragment.QRCodeScanDialogCallback.class));

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((DialogInterface.OnClickListener) invocation.getArgument(2))
                        .onClick(Mockito.mock(DialogInterface.class), DialogInterface.BUTTON_NEGATIVE);

                return null;
            }
        })
                .when(view)
                .showConnectionAcceptDialog(ArgumentMatchers.eq(deviceName)
                        , ArgumentMatchers.eq(authenticationCode)
                        , Mockito.any(DialogInterface.OnClickListener.class));

        BaseSyncConnectionAuthenticator.AuthenticationCallback authenticationCallback
                = Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class);

        final ArrayList<Object> innerClassResults = new ArrayList<>();
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                innerClassResults.add((Exception) invocation.getArgument(0));
                return null;
            }
        })
                .when(authenticationCallback)
                .onAuthenticationFailed(Mockito.anyString(), Mockito.any(Exception.class));

        senderConnectionAuthenticator.authenticate(new DiscoveredDevice("id", connectionInfo)
                , authenticationCallback);

        Mockito.verify(authenticationCallback, Mockito.times(1))
                .onAuthenticationFailed(Mockito.anyString(), Mockito.any(Exception.class));

        Assert.assertEquals("User rejected the connection", ((Exception) innerClassResults.get(0)).getMessage());
    }*/
}