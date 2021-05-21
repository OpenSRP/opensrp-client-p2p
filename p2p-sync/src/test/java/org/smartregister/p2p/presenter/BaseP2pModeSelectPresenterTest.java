package org.smartregister.p2p.presenter;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.WindowManager;

import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.model.dao.ReceiverTransferDao;
import org.smartregister.p2p.model.dao.SenderTransferDao;
import org.smartregister.p2p.shadows.ShadowAppDatabase;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.util.Constants;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 22/03/2019
 */

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAppDatabase.class})
public class BaseP2pModeSelectPresenterTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private P2pModeSelectContract.View view;
    @Mock
    private P2pModeSelectContract.Interactor interactor;
    private P2pModeSelectPresenter p2PModeSelectPresenter;

    @Before
    public void setUp() throws Exception {
        p2PModeSelectPresenter = Mockito.spy(new P2pModeSelectPresenter(view, interactor));
    }

    @Test
    public void sendTextMessageShouldCallInteractorSendMessage() {
        String message = "Hello world";
        p2PModeSelectPresenter.setCurrentDevice(new DiscoveredDevice("endpointid", Mockito.mock(DiscoveredEndpointInfo.class)));
        p2PModeSelectPresenter.sendTextMessage(message);

        Mockito.verify(interactor, Mockito.times(1))
                .sendMessage(ArgumentMatchers.eq(message));
    }

    @Test
    public void onStopShouldDismissDialogsAndCallInteractorStoppingAdvertisingAndDiscovering() {
        p2PModeSelectPresenter.onStop();

        Mockito.verify(view, Mockito.times(1))
                .dismissAllDialogs();
        Mockito.verify(view, Mockito.times(1))
                .enableSendReceiveButtons(true);
        Mockito.verify(interactor, Mockito.times(1))
                .stopAdvertising();
        Mockito.verify(interactor, Mockito.times(1))
                .stopDiscovering();
        Mockito.verify(interactor, Mockito.times(0))
                .disconnectFromEndpoint(Mockito.anyString());
        Mockito.verify(interactor, Mockito.times(1))
                .cleanupResources();
    }

    @Test
    public void onStopShouldDisconnectFromConnectedEndpoint() {
        String endpointId = "endpointId";
        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, Mockito.mock(DiscoveredEndpointInfo.class));

        p2PModeSelectPresenter.setCurrentDevice(discoveredDevice);
        p2PModeSelectPresenter.onStop();

        Mockito.verify(interactor, Mockito.times(1))
                .disconnectFromEndpoint(ArgumentMatchers.eq(endpointId));
    }

    @Test
    public void keepScreenOnShouldAddWindowFlagWhenCalledFirstTime() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Mockito.doReturn(activity)
                .when(view)
                .getContext();

        Assert.assertFalse(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        p2PModeSelectPresenter.keepScreenOn(true);
        Assert.assertTrue(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
    }

    @Test
    public void keepScreenOnShouldRemoveWindowFlagWhenCalledWithFalseAfterSingleTrueCall() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Mockito.doReturn(activity)
                .when(view)
                .getContext();

        Assert.assertFalse(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        p2PModeSelectPresenter.keepScreenOn(true);
        Assert.assertTrue(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        p2PModeSelectPresenter.keepScreenOn(false);
        Assert.assertFalse(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
    }

    @Test
    public void keepScreenOnShouldRetainWindowFlagWhenGivenFalseAfterMultipleTrueCalls() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Mockito.doReturn(activity)
                .when(view)
                .getContext();

        Assert.assertFalse(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        p2PModeSelectPresenter.keepScreenOn(true);
        Assert.assertTrue(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        p2PModeSelectPresenter.keepScreenOn(true);
        p2PModeSelectPresenter.keepScreenOn(false);
        Assert.assertTrue(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
    }

    @Test
    public void keepScreenOnShouldRemoveWindowFlagWhenCalledWithFalseSameNumberOfTimesCalledWithTrue() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Mockito.doReturn(activity)
                .when(view)
                .getContext();

        Assert.assertFalse(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        p2PModeSelectPresenter.keepScreenOn(true);
        Assert.assertTrue(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        p2PModeSelectPresenter.keepScreenOn(true);
        p2PModeSelectPresenter.keepScreenOn(true);
        p2PModeSelectPresenter.keepScreenOn(true);
        p2PModeSelectPresenter.keepScreenOn(false);
        p2PModeSelectPresenter.keepScreenOn(false);
        p2PModeSelectPresenter.keepScreenOn(false);
        p2PModeSelectPresenter.keepScreenOn(false);
        Assert.assertFalse(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
    }

    @Test
    public void addDeviceToBlackListShouldReturnFalseWhenDeviceIsAlreadyBlacklisted() {
        String endpointId = "8923898sdfkjsdf";
        p2PModeSelectPresenter.blacklistedDevices.add(endpointId);

        Assert.assertFalse(p2PModeSelectPresenter.addDeviceToBlacklist(endpointId));
    }

    @Test
    public void rejectedDeviceOnAuthenticationShouldAddDeviceToRejectedListWhenRetryIsNotWithinMaxRetryWindow() {
        String endpointId = "i090sdjfklsdjf09";
        P2PLibrary.init(new P2PLibrary.Options(RuntimeEnvironment.application, "pass", "username"
                , Mockito.mock(P2PAuthorizationService.class), Mockito.mock(ReceiverTransferDao.class)
                , Mockito.mock(SenderTransferDao.class)));

        p2PModeSelectPresenter.rejectedDevices.put(endpointId, System.currentTimeMillis() - (3*60*60*1000));
        p2PModeSelectPresenter.rejectDeviceOnAuthentication(endpointId);

        Mockito.verify(p2PModeSelectPresenter, Mockito.times(1))
                .addDeviceToRejectedList(ArgumentMatchers.eq(endpointId));
    }

    @Test
    public void rejectedDeviceOnAuthenticationShouldAddDeviceToBlacklistWhenRetryIsWithinMaxRetryWindow() {
        String endpointId = "i090sdjfklsdjf09";
        P2PLibrary.init(new P2PLibrary.Options(RuntimeEnvironment.application, "pass", "username"
                , Mockito.mock(P2PAuthorizationService.class), Mockito.mock(ReceiverTransferDao.class)
                , Mockito.mock(SenderTransferDao.class)));

        p2PModeSelectPresenter.rejectedDevices.put(endpointId, System.currentTimeMillis() - (1*60*60*1000));
        p2PModeSelectPresenter.rejectDeviceOnAuthentication(endpointId);

        Mockito.verify(p2PModeSelectPresenter, Mockito.times(1))
                .addDeviceToBlacklist(ArgumentMatchers.eq(endpointId));
    }

    @Test
    public void testSendSkipClicked() {
        p2PModeSelectPresenter.setCurrentDevice(new DiscoveredDevice("endpointid", Mockito.mock(DiscoveredEndpointInfo.class)));
        p2PModeSelectPresenter.sendSkipClicked();

        Mockito.verify(interactor, Mockito.times(1))
                .sendMessage(ArgumentMatchers.eq(Constants.Connection.SKIP_QR_CODE_SCAN));
    }

    private class P2pModeSelectPresenter extends BaseP2pModeSelectPresenter {

        private DiscoveredDevice currentDevice;

        protected P2pModeSelectPresenter(@NonNull P2pModeSelectContract.View view, @NonNull P2pModeSelectContract.Interactor p2pModeSelectInteractor) {
            super(view, p2pModeSelectInteractor);
        }

        @Nullable
        @Override
        public DiscoveredDevice getCurrentPeerDevice() {
            return currentDevice;
        }

        @Override
        public void setCurrentDevice(@Nullable DiscoveredDevice discoveredDevice) {
            currentDevice = discoveredDevice;
        }

        @Override
        public void disconnectAndReset(@NonNull String endpointId) {
            // Do nothing in this implementation
        }
    }
}