package de.dennisguse.opentracks.p0.ble;

import android.content.Context;
import android.util.AttributeSet;
import androidx.preference.PreferenceDialogFragmentCompat;
import java.util.List;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeHeartRatePreference;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeSensorPreference;
import de.dennisguse.opentracks.sensors.driver.HeartRateBluetooth;

/** P0 discovery filters the SIG HR service regardless of legacy multi-sensor preferences. */
public class P0HeartRatePreference extends BluetoothLeHeartRatePreference {
    public P0HeartRatePreference(Context context,AttributeSet attrs) {super(context,attrs);}
    @Override public PreferenceDialogFragmentCompat createInstance() {
        return BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog.newInstance(
                getKey(),List.of(HeartRateBluetooth.HEARTRATE),false,true);
    }
}
