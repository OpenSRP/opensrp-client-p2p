package org.smartregister.p2p.authenticator;

import android.content.DialogInterface;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;

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
import org.robolectric.annotation.Config;
import org.smartregister.p2p.TestApplication;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.fragment.QRCodeGeneratorFragment;
import org.smartregister.p2p.sync.DiscoveredDevice;

import java.util.ArrayList;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 19/03/2019
 */
@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class)
public class ReceiverConnectionAuthenticatorTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private P2pModeSelectContract.View view;
    @Mock
    private P2pModeSelectContract.SenderPresenter senderPresenter;

    private ReceiverConnectionAuthenticator receiverConnectionAuthenticator;

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

        receiverConnectionAuthenticator = Mockito.spy(new ReceiverConnectionAuthenticator(senderPresenter));
        Mockito.doReturn(false).when(receiverConnectionAuthenticator).allowSkipQrCodeScan();
    }

    @Test
    public void authenticateShouldCallAuthenticationFailedCallbackWhenConnectionInfoIsNull() {
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);
        DiscoveredDevice discoveredDevice = new DiscoveredDevice("id", discoveredEndpointInfo);
        String deviceName = "Samsung SMT7834";
        BaseSyncConnectionAuthenticator.AuthenticationCallback authenticationCallback = Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class);
        final ArrayList<Object> innerClassResults = new ArrayList<>();

        Mockito.doReturn(deviceName)
                .when(discoveredEndpointInfo)
                .getEndpointName();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Exception e = invocation.getArgument(1);
                innerClassResults.add(e);

                return null;
            }
        }).when(authenticationCallback)
                .onAuthenticationFailed(Mockito.anyString(), Mockito.any(Exception.class));

        receiverConnectionAuthenticator.authenticate(discoveredDevice, authenticationCallback);

        Mockito.verify(authenticationCallback, Mockito.times(1))
                .onAuthenticationFailed(Mockito.anyString(), Mockito.any(Exception.class));
        Assert.assertEquals("DiscoveredDevice information passed is invalid", ((Exception) innerClassResults.get(0)).getMessage());
    }

    @Test
    public void authenticateShouldCallAuthenticationFailedCallbackWhenConnectionInfoIsValidAndConnectionIsNotIncoming() {
        String deviceName = "Samsung SMT7834";
        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(false)
                .when(connectionInfo)
                .isIncomingConnection();

        Mockito.doReturn(deviceName)
                .when(connectionInfo)
                .getEndpointName();

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

        receiverConnectionAuthenticator.authenticate(discoveredDevice, authenticationCallback);

        Mockito.verify(authenticationCallback, Mockito.times(1))
                .onAuthenticationFailed(Mockito.anyString(), Mockito.any(Exception.class));
        Assert.assertEquals("DiscoveredDevice information passed is invalid", ((Exception) innerClassResults.get(0)).getMessage());
    }

    @Test
    public void authenticateShouldShowQrCodeDialogWhenConnectionInfoIsValidAndConnectionIsIncoming() {
        String authenticationCode = "iowejncCJD";
        String deviceName = "SAMSUNG SM78";

        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(true)
                .when(connectionInfo)
                .isIncomingConnection();

        Mockito.doReturn(deviceName)
                .when(connectionInfo)
                .getEndpointName();

        Mockito.doReturn(authenticationCode)
                .when(connectionInfo)
                .getAuthenticationToken();

        DiscoveredDevice discoveredDevice = new DiscoveredDevice("id", connectionInfo);
        BaseSyncConnectionAuthenticator.AuthenticationCallback authenticationCallback = Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class);

        receiverConnectionAuthenticator.authenticate(discoveredDevice, authenticationCallback);

        Mockito.verify(view, Mockito.times(1))
                .showQRCodeGeneratorFragment(ArgumentMatchers.eq(authenticationCode)
                        , ArgumentMatchers.eq(deviceName)
                        , Mockito.any(QRCodeGeneratorFragment.QRCodeGeneratorCallback.class));
    }

    @Test
    public void authenticateShouldShowConnectingDialogWhenSkipButtonIsClicked() {
        String authenticationCode = "iowejncCJD";
        String deviceName = "SAMSUNG SM78";

        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(true)
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
                QRCodeGeneratorFragment.QRCodeGeneratorCallback qrCodeGeneratorCallback = invocation.getArgument(2);
                qrCodeGeneratorCallback.onSkipped();
                return null;
            }
        })
                .when(view)
                .showQRCodeGeneratorFragment(Mockito.anyString()
                        , Mockito.anyString()
                        , Mockito.any(QRCodeGeneratorFragment.QRCodeGeneratorCallback.class));

        DiscoveredDevice discoveredDevice = new DiscoveredDevice("id", connectionInfo);
        BaseSyncConnectionAuthenticator.AuthenticationCallback authenticationCallback = Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class);

        receiverConnectionAuthenticator.authenticate(discoveredDevice, authenticationCallback);

        Mockito.verify(view, Mockito.times(1))
                .showConnectingDialog(Mockito.any(P2pModeSelectContract.View.DialogCancelCallback.class));
    }

    @Test
    public void authenticateShouldCallAuthenticationCalledWhenCancelBtnOnConnectionDialogIsClicked() {
        String authenticationCode = "iowejncCJD";
        String deviceName = "SAMSUNG SM78";

        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(true)
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
                QRCodeGeneratorFragment.QRCodeGeneratorCallback qrCodeGeneratorCallback = invocation.getArgument(2);
                qrCodeGeneratorCallback.onSkipped();
                return null;
            }
        })
                .when(view)
                .showQRCodeGeneratorFragment(Mockito.anyString()
                        , Mockito.anyString()
                        , Mockito.any(QRCodeGeneratorFragment.QRCodeGeneratorCallback.class));

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                P2pModeSelectContract.View.DialogCancelCallback dialogCancelCallback = invocation.getArgument(0);
                dialogCancelCallback.onCancelClicked(Mockito.mock(DialogInterface.class));
                return null;
            }
        })
                .when(view)
                .showConnectingDialog(Mockito.any(P2pModeSelectContract.View.DialogCancelCallback.class));

        DiscoveredDevice discoveredDevice = new DiscoveredDevice("id", connectionInfo);
        BaseSyncConnectionAuthenticator.AuthenticationCallback authenticationCallback = Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class);

        receiverConnectionAuthenticator.authenticate(discoveredDevice, authenticationCallback);

        Mockito.verify(authenticationCallback, Mockito.times(1))
                .onAuthenticationCancelled(Mockito.anyString());
        Mockito.verify(view, Mockito.times(1))
                .removeConnectingDialog();
    }


}