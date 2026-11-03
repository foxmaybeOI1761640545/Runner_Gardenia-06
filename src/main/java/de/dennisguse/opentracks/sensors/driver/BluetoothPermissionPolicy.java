package de.dennisguse.opentracks.sensors.driver;

import java.util.List;

/** Runtime permissions already declared by this application; no background/location expansion. */
public final class BluetoothPermissionPolicy {
    private BluetoothPermissionPolicy() {}
    public static List<String> forApi(int sdk) {
        return sdk >= 31 ? List.of("android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT")
                : List.of("android.permission.ACCESS_FINE_LOCATION");
    }
}
