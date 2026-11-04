package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import org.junit.Test;
import de.dennisguse.opentracks.p0.app.P0PermissionPolicy;

public class P0PermissionPolicyTest {
    @Test public void connectingRememberedStrapDoesNotRequireScanOrGps() {
        assertEquals(java.util.List.of("android.permission.BLUETOOTH_CONNECT"),P0PermissionPolicy.bluetoothConnection(31));
        assertTrue(P0PermissionPolicy.bluetoothConnection(30).isEmpty());
    }
    @Test public void gpsDoesNotRequireBluetooth() {
        assertEquals(2,P0PermissionPolicy.location().size());
        assertTrue(P0PermissionPolicy.location().contains("android.permission.ACCESS_FINE_LOCATION"));
        assertTrue(P0PermissionPolicy.location().stream().noneMatch(p -> p.contains("BLUETOOTH")));
    }
    @Test public void modernBluetoothRequiresBothCapabilities() {
        assertEquals(java.util.List.of("android.permission.BLUETOOTH_SCAN","android.permission.BLUETOOTH_CONNECT"),P0PermissionPolicy.bluetooth(31));
    }
    @Test public void olderScanningUsesLocationPermission() {
        assertEquals(java.util.List.of("android.permission.ACCESS_FINE_LOCATION"),P0PermissionPolicy.bluetooth(30));
    }
    @Test public void notificationsAreRequestedOnlyOnSupportedApi() {
        assertTrue(P0PermissionPolicy.notifications(32).isEmpty());
        assertEquals(java.util.List.of("android.permission.POST_NOTIFICATIONS"),P0PermissionPolicy.notifications(33));
    }
}
