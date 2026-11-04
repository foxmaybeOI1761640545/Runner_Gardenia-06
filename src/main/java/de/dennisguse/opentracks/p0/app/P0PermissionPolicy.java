package de.dennisguse.opentracks.p0.app;

import java.util.List;
import de.dennisguse.opentracks.sensors.driver.BluetoothPermissionPolicy;

/** Separate user actions request only their required system capabilities. */
public final class P0PermissionPolicy {
    private P0PermissionPolicy() {}
    public static List<String> location() {
        return List.of("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION");
    }
    public static List<String> bluetooth(int api) { return BluetoothPermissionPolicy.forApi(api); }
    public static List<String> bluetoothConnection(int api) {
        return api>=31?List.of("android.permission.BLUETOOTH_CONNECT"):List.of();
    }
    public static List<String> notifications(int api) {
        return api >= 33 ? List.of("android.permission.POST_NOTIFICATIONS") : List.of();
    }
}
