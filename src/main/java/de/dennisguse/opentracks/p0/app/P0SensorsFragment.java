package de.dennisguse.opentracks.p0.app;

import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.ble.HeartRateCommands;
import de.dennisguse.opentracks.p0.recording.P0RecordingViewModel;
import de.dennisguse.opentracks.p0.recording.RecordingPermissions;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeSensorPreference;
import de.dennisguse.opentracks.ui.HeartRatePanel;

public class P0SensorsFragment extends PreferenceFragmentCompat {
    private P0RecordingViewModel model;
    private ActivityResultLauncher<String[]> connectPermission;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        connectPermission=registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),result->{
            if(RecordingPermissions.hasBluetooth(requireContext()))model.previewHeartRate(true);
            else Toast.makeText(requireContext(),R.string.permission_bluetooth_failed,Toast.LENGTH_LONG).show();
        });
    }
    @Override public void onCreatePreferences(Bundle state,String root) {
        setPreferencesFromResource(R.xml.p0_sensors,root);
        findPreference("p0_sensor_error").setVisible(false);
        model=new ViewModelProvider(requireActivity()).get(P0RecordingViewModel.class);
        findPreference("p0_connect").setOnPreferenceClickListener(preference->{
            if(!HeartRateCommands.hasSelection())Toast.makeText(requireContext(),R.string.hr_choose_first,Toast.LENGTH_SHORT).show();
            else if(RecordingPermissions.hasBluetooth(requireContext()))model.previewHeartRate(true);
            else connectPermission.launch(P0PermissionPolicy.bluetoothConnection(Build.VERSION.SDK_INT).toArray(new String[0]));
            return true;
        });
        findPreference("p0_disconnect").setOnPreferenceClickListener(preference->{model.disconnectHeartRate();return true;});
        model.snapshot().observe(this,value->{
            var preview=value.preview();
            var description=HeartRatePanel.describe(requireContext(),preview.value(),preview.status());
            Preference output=findPreference("p0_preview");
            output.setTitle(getString(R.string.hr_preview_title,description.bpm()));
            output.setSummary(getString(R.string.hr_preview_detail,description.state(),description.age()));
            findPreference("p0_sensor_error").setSummary(value.error());
            findPreference("p0_sensor_error").setVisible(value.error()!=null);
        });
    }
    @Override public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if(preference instanceof BluetoothLeSensorPreference sensor) {
            DialogFragment dialog=sensor.createInstance();dialog.setTargetFragment(this,0);
            dialog.show(getParentFragmentManager(),"p0-heart-rate-scan");
        } else super.onDisplayPreferenceDialog(preference);
    }
}
