package de.dennisguse.opentracks.sensors.driver;

import static org.junit.Assert.*;
import android.os.Handler;
import android.os.Looper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import de.dennisguse.opentracks.data.models.HeartRate;

public class BluetoothDriverLifecycleTest {
    @Test public void equalPayloadNotificationsHaveIndependentIdentityAndCaptureTimes() {
        HeartRateBluetooth parser=new HeartRateBluetooth();
        var characteristic=new android.bluetooth.BluetoothGattCharacteristic(HeartRateBluetooth.HEARTRATE.measurementUUID(),0,0);
        HeartRate first=parser.parsePayload(HeartRateBluetooth.HEARTRATE,"Synthetic strap",characteristic,new byte[]{0,76});
        HeartRate second=parser.parsePayload(HeartRateBluetooth.HEARTRATE,"Synthetic strap",characteristic,new byte[]{0,76});
        assertEquals(first.value(),second.value(),0);assertNotEquals(first.sample().sampleId(),second.sample().sampleId());
        var one=first.sample().captured(java.time.Instant.EPOCH,"AA:BB:CC:DD:EE:FF","Synthetic strap","connection-1");
        var two=second.sample().captured(java.time.Instant.EPOCH.plusSeconds(1),"AA:BB:CC:DD:EE:FF","Synthetic strap","connection-1");
        assertNotEquals(one.capturedAt(),two.capturedAt());
        assertEquals(one.sampleId(),one.captured(java.time.Instant.EPOCH.plusSeconds(2),one.deviceAddress(),one.deviceName(),one.connectionId()).sampleId());
    }
    @Test public void liveReconnectPolicyIsMeasurementScopedEvenForSharedDeviceAddress() {
        android.content.Context context=ApplicationProvider.getApplicationContext();
        DriverObserver<HeartRate> observer=new DriverObserver<>() {
            public void onConnected(String address,String name){fail("No adapter is supplied");}
            public void onConnectionLost(){}public void onDisconnected(){}
            public void onDataReceived(HeartRate value){fail("No adapter is supplied");}
        };
        BluetoothDriver<HeartRate> heart=new BluetoothDriver<>(null,observer,new HeartRateBluetooth());
        BluetoothDriver<HeartRate> other=new BluetoothDriver<>(null,observer,new BluetoothDriver.BluetoothParser<>() {
            public java.util.List<de.dennisguse.opentracks.sensors.ServiceMeasurementUUID> getServices(){return List.of(new de.dennisguse.opentracks.sensors.ServiceMeasurementUUID(java.util.UUID.fromString("00001816-0000-1000-8000-00805f9b34fb"),java.util.UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb")));}
            public HeartRate parsePayload(de.dennisguse.opentracks.sensors.ServiceMeasurementUUID service,String name,android.bluetooth.BluetoothGattCharacteristic characteristic){return null;}
        });
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
                Handler owner=new Handler(Looper.getMainLooper());heart.connect(context,owner,"AA:BB:CC:DD:EE:FF");other.connect(context,owner,"AA:BB:CC:DD:EE:FF");
                context.sendBroadcast(new android.content.Intent(context.getPackageName()+BluetoothDriver.RECONNECT_POLICY_SUFFIX).setPackage(context.getPackageName())
                        .putExtra("address","AA:BB:CC:DD:EE:FF").putExtra("measurement",HeartRateBluetooth.HEARTRATE.measurementUUID().toString()).putExtra("enabled",false));
            });
            long deadline=android.os.SystemClock.uptimeMillis()+5000;
            while(heart.isAutoReconnectEnabled()&&android.os.SystemClock.uptimeMillis()<deadline)android.os.SystemClock.sleep(20);
            assertFalse(heart.isAutoReconnectEnabled());assertTrue(other.isAutoReconnectEnabled());
        }finally{InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{heart.disconnect();other.disconnect();});}
    }
    @Test public void alreadyGrantedPermissionRunsSuccessWithoutLaunchingAnotherRequest() {
        int[] successes = {0};
        // No permissions are missing; a null launcher deliberately fails if registration is attempted.
        new de.dennisguse.opentracks.util.PermissionRequester(List.of()).requestPermissionsIfNeeded(
                ApplicationProvider.getApplicationContext(), null, () -> successes[0]++,
                denied -> fail("Already granted permissions must not be rejected"));
        assertEquals(1, successes[0]);
    }

    @Test public void runtimeParserRetainsOptionalFieldsAndNotificationIdentity() {
        HeartRateBluetooth parser = new HeartRateBluetooth();
        android.bluetooth.BluetoothGattCharacteristic characteristic = new android.bluetooth.BluetoothGattCharacteristic(
                HeartRateBluetooth.HEARTRATE.measurementUUID(), 0, 0);
        HeartRate value = parser.parsePayload(HeartRateBluetooth.HEARTRATE, "strap", characteristic,
                new byte[]{0x1e, 72, 12, 0, 0, 4, (byte)0xe8, 3});
        assertEquals(72, value.getBPM(), 0); assertTrue(value.sample().contactDetected());
        assertEquals(Integer.valueOf(12), value.sample().energyExpended());
        assertEquals(List.of(1024,1000), value.sample().rrIntervals1024()); assertNull(value.sample().capturedAt());
        var stamped = value.sample().captured(java.time.Instant.EPOCH, "test-address", "strap", "connection-1");
        assertEquals(value.sample().sampleId(), stamped.sampleId()); assertEquals("connection-1", stamped.connectionId());
    }

    @Test public void notificationAndIndicationSubscriptionsPreserveOtherSensorSupport() {
        assertArrayEquals(new byte[]{1,0},BluetoothDriver.subscriptionValue(android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY));
        assertArrayEquals(new byte[]{2,0},BluetoothDriver.subscriptionValue(android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE));
        assertNull(BluetoothDriver.subscriptionValue(android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ));
    }

    @Test public void missingAdapterDoesNotPretendConnectedOrRegisterTwice() {
        List<BleConnectionSession.State> states=new ArrayList<>();
        int[] connected={0};int[] stopped={0};
        DriverObserver<HeartRate> observer=new DriverObserver<>() {
            public void onConnected(String address,String name) { connected[0]++; }
            public void onConnectionLost() {}
            public void onDataReceived(HeartRate value) { fail("No physical adapter is present"); }
            public void onDisconnected() { stopped[0]++; }
            public void onConnectionStateChanged(BleConnectionSession.State state) { states.add(state); }
        };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            BluetoothDriver<HeartRate> driver=new BluetoothDriver<>(null,observer,new HeartRateBluetooth());
            driver.connect(ApplicationProvider.getApplicationContext(),new Handler(Looper.getMainLooper()),"AA:BB:CC:DD:EE:FF");
            driver.connect(ApplicationProvider.getApplicationContext(),new Handler(Looper.getMainLooper()),"AA:BB:CC:DD:EE:FF");
            assertEquals(BleConnectionSession.State.BLUETOOTH_OFF,states.get(states.size()-1));assertFalse(driver.isConnected());
            driver.disconnect();driver.disconnect();assertEquals(1,stopped[0]);assertEquals(0,connected[0]);
        });
    }
    @Test public void optionalMetadataIsBoundedAndResetBetweenConnections() {
        HeartRateBluetooth parser=new HeartRateBluetooth();
        parser.onOptionalRead(HeartRateBluetooth.BODY_LOCATION.measurementUUID(),new byte[]{1});
        parser.onOptionalRead(de.dennisguse.opentracks.sensors.BluetoothUtils.BATTERY.measurementUUID(),new byte[]{80});
        assertEquals(Integer.valueOf(1),parser.getBodySensorLocation());assertEquals(Integer.valueOf(80),parser.getBatteryLevel());
        parser.onOptionalRead(de.dennisguse.opentracks.sensors.BluetoothUtils.BATTERY.measurementUUID(),new byte[]{(byte)255});
        assertEquals(Integer.valueOf(80),parser.getBatteryLevel());parser.resetOptionalMetadata();assertNull(parser.getBatteryLevel());assertNull(parser.getBodySensorLocation());
    }
}
