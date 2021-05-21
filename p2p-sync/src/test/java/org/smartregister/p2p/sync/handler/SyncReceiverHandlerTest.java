package org.smartregister.p2p.sync.handler;

import androidx.collection.SimpleArrayMap;

import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.gson.Gson;

import org.json.JSONArray;
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
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.fragment.SyncCompleteTransferFragment;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.dao.ReceiverTransferDao;
import org.smartregister.p2p.model.dao.SenderTransferDao;
import org.smartregister.p2p.shadows.ShadowAppDatabase;
import org.smartregister.p2p.shadows.ShadowTasker;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.sync.data.ProcessedChunk;
import org.smartregister.p2p.sync.data.SyncPackageManifest;
import org.smartregister.p2p.util.Constants;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 04/04/2019
 */

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAppDatabase.class, ShadowTasker.class})
public class SyncReceiverHandlerTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private SyncReceiverHandler syncReceiverHandler;
    @Mock
    private P2pModeSelectContract.ReceiverPresenter receiverPresenter;

    @Mock
    private P2pModeSelectContract.View view;

    @Mock
    private P2PAuthorizationService authorizationService;
    @Mock
    private SenderTransferDao senderTransferDao;
    @Mock
    private ReceiverTransferDao receiverTransferDao;

    private DataType event = new DataType("event", DataType.Type.NON_MEDIA, 1);
    private DataType profilePic = new DataType("profile-pic", DataType.Type.MEDIA, 3);

    @Before
    public void setUp() throws Exception {
        P2PLibrary.init(new P2PLibrary.Options(RuntimeEnvironment.application, "some password", "username"
                , authorizationService, receiverTransferDao, senderTransferDao));

        Mockito.doReturn(view)
                .when(receiverPresenter)
                .getView();

        Mockito.doReturn(RuntimeEnvironment.application)
                .when(view)
                .getContext();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                int resId = invocation.getArgument(0);
                return RuntimeEnvironment.application.getString(resId);
            }
        })
                .when(view)
                .getString(Mockito.anyInt());

        syncReceiverHandler = Mockito.spy(new SyncReceiverHandler(receiverPresenter));
    }

    @Test
    public void processPayloadShouldCallProcessManifestWhenAwaitingManifestReceiptIsTrue() {
        String endpointId = "id";
        long payloadId = 89834l;
        Payload payload = Mockito.mock(Payload.class);

        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(payloadId, "json", event, 20);

        Mockito.doReturn(payloadId)
                .when(payload)
                .getId();

        Mockito.doReturn(new Gson().toJson(syncPackageManifest).getBytes())
                .when(payload)
                .asBytes();

        Mockito.doReturn(Payload.Type.BYTES)
                .when(payload)
                .getType();

        syncReceiverHandler.processPayload(endpointId, payload);

        Mockito.verify(syncReceiverHandler, Mockito.times(1))
                .processManifest(ArgumentMatchers.eq(endpointId), ArgumentMatchers.eq(payload));

        assertFalse((boolean) ReflectionHelpers.getField(syncReceiverHandler, "awaitingManifestReceipt"));
    }

    @Test
    public void processPayloadShouldCallProcessRecordsWhenAwaitingManifestReceiptIsFalse() {
        String endpointId = "id";
        Payload payload = Mockito.mock(Payload.class);

        ReflectionHelpers.setField(syncReceiverHandler, "awaitingManifestReceipt", false);

        syncReceiverHandler.processPayload(endpointId, payload);
        Mockito.verify(syncReceiverHandler, Mockito.times(1))
                .processPayloadChunk(ArgumentMatchers.eq(endpointId), ArgumentMatchers.eq(payload));
    }

    @Test
    public void processManifestShouldRetrieveManifestAndChangeAwaitingItemWhenPayloadIsBytes() {
        String endpointId = "id";
        long payloadId = 829832l;
        Payload payload = Mockito.mock(Payload.class);

        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(payloadId, "json", event, 20);
        String jsonPackageManifest = new Gson().toJson(syncPackageManifest);

        Mockito.doReturn(jsonPackageManifest.getBytes())
                .when(payload)
                .asBytes();

        Mockito.doReturn(Payload.Type.BYTES)
                .when(payload)
                .getType();

        Mockito.doReturn(payloadId)
                .when(payload)
                .getId();

        syncReceiverHandler.processManifest(endpointId, payload);

        assertFalse((boolean) ReflectionHelpers.getField(syncReceiverHandler, "awaitingManifestReceipt"));
        SyncPackageManifest finalSyncPackageManifest = ((HashMap<Long, SyncPackageManifest>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests"))
                .get(payloadId);

        assertEquals(jsonPackageManifest, new Gson().toJson(finalSyncPackageManifest));
    }

    @Test
    public void processManifestShouldDoNothingWhenPayloadTypeIsStream() {
        String endpointId = "id";
        Payload payload = Mockito.mock(Payload.class);

        Mockito.doReturn(Payload.Type.STREAM)
                .when(payload)
                .getType();

        syncReceiverHandler.processManifest(endpointId, payload);

        assertTrue((boolean) ReflectionHelpers.getField(syncReceiverHandler, "awaitingManifestReceipt"));
    }

    @Test
    public void processRecordsShouldCallProcessNonMediaDataWhenPayloadCorrespondsToAwaitingManifestAndDataTypeIsNonMedia() {
        String endpointId = "id";
        long payloadId = 923823l;
        Payload payload = Mockito.mock(Payload.class);

        JSONArray jsonArray = new JSONArray();

        Payload.Stream payloadStream = Mockito.mock(Payload.Stream.class);

        Mockito.doReturn(new ByteArrayInputStream(jsonArray.toString().getBytes()))
                .when(payloadStream)
                .asInputStream();

        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(payloadId, "json", event, 20);
        Mockito.doReturn(payloadStream)
                .when(payload)
                .asStream();

        Mockito.doReturn(Payload.Type.STREAM)
                .when(payload)
                .getType();

        Mockito.doReturn(payloadId)
                .when(payload)
                .getId();

        ((HashMap<Long, SyncPackageManifest>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests"))
                .put(syncPackageManifest.getPayloadId(), syncPackageManifest);
        assertNull(((SimpleArrayMap<Long, ProcessedChunk>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloads")).get(payloadId));
        syncReceiverHandler.processPayloadChunk(endpointId, payload);
        assertNotNull(((SimpleArrayMap<Long, ProcessedChunk>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloads")).get(payloadId));
    }

    @Test
    public void processRecordsShouldCallProcessMediaDataWhenPayloadCorrespondsToAwaitingManifestAndDataTypeIsMedia() {
        String endpointId = "id";
        long payloadId = 923823l;
        Payload payload = Mockito.mock(Payload.class);

        Payload.File payloadFile = Mockito.mock(Payload.File.class);
        File javaFile = Mockito.mock(File.class);

        Mockito.doReturn(javaFile)
                .when(payloadFile)
                .asJavaFile();

        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(payloadId, "png", profilePic, 1);
        Mockito.doReturn(payloadFile)
                .when(payload)
                .asFile();

        Mockito.doReturn(Payload.Type.FILE)
                .when(payload)
                .getType();

        Mockito.doReturn(payloadId)
                .when(payload)
                .getId();

        ((HashMap<Long, SyncPackageManifest>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests"))
                .put(syncPackageManifest.getPayloadId(), syncPackageManifest);

        assertNull(((SimpleArrayMap<Long, ProcessedChunk>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloads")).get(payloadId));
        syncReceiverHandler.processPayloadChunk(endpointId, payload);
        assertNotNull(((SimpleArrayMap<Long, ProcessedChunk>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloads")).get(payloadId));
    }

    @Test
    public void processPayloadShouldCallShowSyncCompleteFragmentWhenSyncCompleteConnectionSignalPayloadIsReceived() {
        long payloadId = 9293;
        String endpointId = "endpointid";
        String deviceName = "SAMSUNG SMT78723";

        Payload syncCompletePayload = Mockito.mock(Payload.class);

        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);
        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        Mockito.doReturn(payloadId)
                .when(syncCompletePayload)
                .getId();

        Mockito.doReturn(Payload.Type.BYTES)
                .when(syncCompletePayload)
                .getType();

        Mockito.doReturn(Constants.Connection.SYNC_COMPLETE.getBytes())
                .when(syncCompletePayload)
                .asBytes();

        Mockito.doReturn(deviceName)
                .when(discoveredEndpointInfo)
                .getEndpointName();

        Mockito.doReturn(discoveredDevice)
                .when(receiverPresenter)
                .getCurrentPeerDevice();

        syncReceiverHandler.processPayload(endpointId, syncCompletePayload);

        Mockito.verify(view, Mockito.times(1))
                .showSyncCompleteFragment(Mockito.eq(true)
                        , Mockito.eq(deviceName)
                        , Mockito.any(SyncCompleteTransferFragment.OnCloseClickListener.class)
                        , Mockito.anyString()
                        , Mockito.eq(false));
    }

    @Test
    public void onPayloadTransferUpdateShouldSendAcknowledgementToSenderDeviceWhenTransferStatusUpdateIsSuccessful() {
        String endpointId = "endpoint-id";
        long payloadId = 923l;
        PayloadTransferUpdate update = Mockito.mock(PayloadTransferUpdate.class);

        Mockito.doReturn(PayloadTransferUpdate.Status.SUCCESS)
                .when(update)
                .getStatus();

        Mockito.doReturn(payloadId)
                .when(update)
                .getPayloadId();

        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(payloadId, ".json", event, 45);
        HashMap<Long, SyncPackageManifest> awaitingPackageManifests = ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests");
        awaitingPackageManifests.put(payloadId, syncPackageManifest);


        syncReceiverHandler.onPayloadTransferUpdate(endpointId, update);

        Mockito.verify(syncReceiverHandler, Mockito.times(1))
                .sendPayloadReceived(Mockito.eq(payloadId));
        Mockito.verify(receiverPresenter, Mockito.times(1))
                .sendTextMessage(Mockito.eq(Constants.Connection.PAYLOAD_RECEIVED + payloadId));
    }

    @Test
    public void onPayloadTransferUpdateShouldCallFinishProcessDataWhenTransferStatusUpdateIsSuccessful() {
        String endpointId = "endpoint-id";
        long payloadId = 923l;
        PayloadTransferUpdate update = Mockito.mock(PayloadTransferUpdate.class);

        Mockito.doReturn(PayloadTransferUpdate.Status.SUCCESS)
                .when(update)
                .getStatus();

        Mockito.doReturn(payloadId)
                .when(update)
                .getPayloadId();

        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(payloadId, ".json", event, 45);
        HashMap<Long, SyncPackageManifest> awaitingPackageManifests = ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests");
        awaitingPackageManifests.put(payloadId, syncPackageManifest);

        syncReceiverHandler.onPayloadTransferUpdate(endpointId, update);

        Mockito.verify(syncReceiverHandler, Mockito.times(1))
                .finishProcessingData(Mockito.eq(endpointId), Mockito.eq(payloadId));
    }

    @Test
    public void onPayloadTransferUpdateShouldCallUpdateProgressFragmentWhenTransferStatusUpdateIsInProgress() {
        String endpointId = "endpoint-id";
        long payloadId = 923l;
        int total = 20;
        int sent = 5;
        int expectedProgress = (5 * 100) / 20;
        PayloadTransferUpdate update = Mockito.mock(PayloadTransferUpdate.class);

        Mockito.doReturn(PayloadTransferUpdate.Status.IN_PROGRESS)
                .when(update)
                .getStatus();
        Mockito.doReturn((long) sent)
                .when(update)
                .getBytesTransferred();
        Mockito.doReturn(payloadId)
                .when(update)
                .getPayloadId();

        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(payloadId, ".json", event, 45);
        syncPackageManifest.setPayloadSize(total);
        HashMap<Long, SyncPackageManifest> awaitingPackageManifests = ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests");
        awaitingPackageManifests.put(payloadId, syncPackageManifest);

        syncReceiverHandler.onPayloadTransferUpdate(endpointId, update);

        Mockito.verify(view, Mockito.times(1))
                .updateProgressFragment(Mockito.eq(expectedProgress));
    }

    @Test
    public void finishProcessingDataShouldCallFinishProcessingNonMediaDataWhenDataTypeIsNonMedia() {
        String endpointId = "endpoint-id";
        long payloadId = 923l;

        // Add the manifest
        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(payloadId, "json", event, 45);
        HashMap<Long, SyncPackageManifest> awaitingPackageManifests = ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests");
        awaitingPackageManifests.put(payloadId, syncPackageManifest);

        // Add the processed chunk
        ProcessedChunk processedChunk = new ProcessedChunk(Payload.Type.STREAM, "[]");
        ((SimpleArrayMap<Long, ProcessedChunk>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloads"))
                .put(payloadId, processedChunk);

        syncReceiverHandler.finishProcessingData(endpointId, payloadId);

        Mockito.verify(syncReceiverHandler, Mockito.times(1))
                .finishProcessingNonMediaData(Mockito.eq(payloadId));
    }

    @Test
    public void finishProcessingDataShouldCallFinishProcessingMediaDataWhenDataTypeIsMedia() {
        String endpointId = "endpoint-id";
        long payloadId = 923l;
        // Add the manifest
        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(payloadId, "jpg", profilePic, 1);

        HashMap<Long, SyncPackageManifest> awaitingPackageManifests = ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests");
        awaitingPackageManifests.put(payloadId, syncPackageManifest);

        Payload payload = Mockito.mock(Payload.class);

        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);
        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        Mockito.doReturn(discoveredDevice)
                .when(receiverPresenter)
                .getCurrentPeerDevice();

        // Add the processed chunk
        ProcessedChunk processedChunk = new ProcessedChunk(Payload.Type.FILE, payload);
        ((SimpleArrayMap<Long, ProcessedChunk>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloads"))
                .put(payloadId, processedChunk);

        syncReceiverHandler.finishProcessingData(endpointId, payloadId);

        Mockito.verify(syncReceiverHandler, Mockito.times(1))
                .finishProcessingMediaData(Mockito.eq(payloadId));
    }

    @Test
    public void finishProcessNonMediaDataShouldCallUpdateLastRecord() {
        String endpointId = "endpoint-id";
        long payloadId = 923l;
        int recordsTransferred = 45;
        long lastRecordId = 567;

        // Add the manifest
        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(payloadId, "json", event, recordsTransferred);
        HashMap<Long, SyncPackageManifest> awaitingPackageManifests = ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests");
        awaitingPackageManifests.put(payloadId, syncPackageManifest);

        // Add the processed chunk
        ProcessedChunk processedChunk = new ProcessedChunk(Payload.Type.STREAM, "[]");
        ((SimpleArrayMap<Long, ProcessedChunk>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloads"))
                .put(payloadId, processedChunk);

        Mockito.doReturn(lastRecordId)
                .when(receiverTransferDao)
                .receiveJson(Mockito.eq(event), Mockito.any(JSONArray.class));

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, Mockito.mock(DiscoveredEndpointInfo.class));
        Mockito.doReturn(discoveredDevice)
                .when(receiverPresenter)
                .getCurrentPeerDevice();

        syncReceiverHandler.finishProcessingNonMediaData(payloadId);

        Mockito.verify(syncReceiverHandler, Mockito.times(1))
                .updateTransferProgress(Mockito.eq(event.getName()), Mockito.anyInt());
        Mockito.verify(syncReceiverHandler, Mockito.times(1))
                .logTransfer(Mockito.eq(false), Mockito.eq(event.getName()), Mockito.any(DiscoveredDevice.class), Mockito.anyInt());
        Mockito.verify(receiverTransferDao, Mockito.times(1))
                .receiveJson(Mockito.eq(event), Mockito.any(JSONArray.class));
        Mockito.verify(syncReceiverHandler, Mockito.times(1))
                .updateLastRecord(Mockito.eq(event.getName()), Mockito.eq(lastRecordId));
        assertNull(((HashMap<Long, SyncPackageManifest>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests")).get(payloadId));
    }

    @Test
    public void finishProcessMediaDataShouldCallUpdateLastRecord() {
        String endpointId = "endpoint-id";
        long payloadId = 923l;
        long recordId = 823823l;

        // Add the manifest
        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(payloadId, "jpg", profilePic, 1);

        HashMap<String, Object> payloadDetails = new HashMap<>();
        payloadDetails.put("fileRecordId", (new Long(recordId)).doubleValue());

        syncPackageManifest.setPayloadDetails(payloadDetails);

        HashMap<Long, SyncPackageManifest> awaitingPackageManifests = ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests");
        awaitingPackageManifests.put(payloadId, syncPackageManifest);

        Payload payload = Mockito.mock(Payload.class);
        Payload.File payloadFile = Mockito.mock(Payload.File.class);

        Mockito.doReturn(payloadFile)
                .when(payload)
                .asFile();

        Mockito.doReturn(payloadId)
                .when(payload)
                .getId();

        File mockedFile = Mockito.mock(File.class);
        Mockito.doReturn(mockedFile)
                .when(payloadFile)
                .asJavaFile();

        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);
        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        Mockito.doReturn(discoveredDevice)
                .when(receiverPresenter)
                .getCurrentPeerDevice();

        // Add the processed chunk
        ProcessedChunk processedChunk = new ProcessedChunk(Payload.Type.FILE, payload);
        ((SimpleArrayMap<Long, ProcessedChunk>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloads"))
                .put(payloadId, processedChunk);

        Mockito.doReturn(recordId)
                .when(receiverTransferDao)
                .receiveMultimedia(Mockito.eq(profilePic)
                        , Mockito.eq(mockedFile)
                        , ArgumentMatchers.<HashMap<String, Object>>any()
                        , Mockito.eq(recordId));

        syncReceiverHandler.finishProcessingMediaData(payloadId);

        Mockito.verify(syncReceiverHandler, Mockito.times(1))
                .updateLastRecord(Mockito.eq(profilePic.getName()), Mockito.eq(recordId));
        Mockito.verify(syncReceiverHandler, Mockito.times(1))
                .logTransfer(Mockito.eq(false), Mockito.eq(profilePic.getName()), Mockito.any(DiscoveredDevice.class), Mockito.eq(1));
        Mockito.verify(receiverTransferDao, Mockito.times(1))
                .receiveMultimedia(Mockito.eq(profilePic), Mockito.eq(mockedFile), ArgumentMatchers.<HashMap<String, Object>>any(), Mockito.eq(recordId));
        assertNull(((HashMap<Long, SyncPackageManifest>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests")).get(payloadId));
    }

}