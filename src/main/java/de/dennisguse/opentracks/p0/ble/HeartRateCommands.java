package de.dennisguse.opentracks.p0.ble;

import android.content.Context;
import android.content.Intent;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.sensors.driver.BluetoothDriver;
import de.dennisguse.opentracks.sensors.driver.HeartRateBluetooth;
import de.dennisguse.opentracks.sensors.driver.SensorType;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/** App-private commands to the existing selected-device transport; never owns a GATT. */
public final class HeartRateCommands {
    private HeartRateCommands() {}
    public static String selected() {
        return PreferencesUtils.getString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
    }
    public static boolean hasSelection() {
        String address=selected();
        return address!=null&&address.matches("[0-9A-F]{2}(:[0-9A-F]{2}){5}");
    }
    public static Intent intent(Context context,String action) {
        return new Intent(context.getPackageName()+action).setPackage(context.getPackageName())
                .putExtra("address",selected())
                .putExtra("measurement",HeartRateBluetooth.HEARTRATE.measurementUUID().toString());
    }
    public static void reconnect(Context context) {context.sendBroadcast(intent(context,BluetoothDriver.RECONNECT_SUFFIX));}
    public static void disconnect(Context context) {context.sendBroadcast(intent(context,BluetoothDriver.DISCONNECT_SUFFIX));}
    public static boolean autoReconnectEnabled(Context context) {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.p0_hr_auto_reconnect_key),true);
    }
    public static boolean connectionEnabled(Context context) {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.p0_hr_connection_enabled_key),true);
    }
    private static void save(Context context,int key,boolean value) {
        if(!androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(key),value).commit())
            throw new IllegalStateException("Cannot save heart-rate connection preferences");
    }
    public static void setConnectionEnabled(Context context,boolean enabled) {save(context,R.string.p0_hr_connection_enabled_key,enabled);}
    public static void setAutoReconnect(Context context,boolean enabled) {
        try {save(context,R.string.p0_hr_auto_reconnect_key,enabled);}
        finally {context.sendBroadcast(intent(context,BluetoothDriver.RECONNECT_POLICY_SUFFIX).putExtra("enabled",enabled));}
    }
}
