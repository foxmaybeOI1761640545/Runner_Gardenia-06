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
    @Test public void alreadyGrantedPermissionRunsSuccessWithoutLaunchingAnotherRequest() {
        int[] successes = {0};
        // No permissions are missing; a null launcher deliberately fails if registration is attempted.
        new de.dennisguse.opentracks.util.PermissionRequester(List.of()).requestPermissionsIfNeeded(
                ApplicationProvider.getApplicationContext(), null, () -> successes[0]++,
                denied -> fail("Already granted permissions must not be rejected"));
        assertEquals(1, successes[0]);
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
