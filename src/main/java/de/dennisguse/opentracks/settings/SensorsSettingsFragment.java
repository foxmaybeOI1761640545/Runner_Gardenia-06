package de.dennisguse.opentracks.settings;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.DistanceFormatter;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeSensorPreference;
import de.dennisguse.opentracks.util.PermissionRequester;

public class SensorsSettingsFragment extends PreferenceFragmentCompat {

    private final static String TAG = SensorsSettingsFragment.class.getSimpleName();

    private final android.os.Handler previewHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final de.dennisguse.opentracks.services.TrackRecordingServiceConnection previewConnection =
            new de.dennisguse.opentracks.services.TrackRecordingServiceConnection((service,connection) -> {});
    private androidx.activity.result.ActivityResultLauncher<String[]> heartRatePermissions;
    private final Runnable updateHeartRatePreview = new Runnable() {
        @Override public void run() {
            if (!isResumed()) return;
            var service = previewConnection.getTrackRecordingService();
            var preview = service != null ? service.getHeartRatePreview() : new de.dennisguse.opentracks.sensors.HeartRatePreview(null,de.dennisguse.opentracks.sensors.HeartRateStatus.disconnected());
            var text = de.dennisguse.opentracks.ui.HeartRatePanel.describe(requireContext(),preview.value(),preview.status());
            Preference preference = findPreference("runner_heart_rate_preview");
            preference.setTitle(getString(R.string.hr_preview_title,text.bpm()));
            preference.setSummary(getString(R.string.hr_preview_detail,text.state(),text.age()));
            previewHandler.postDelayed(this,1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        heartRatePermissions = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),result -> {
            if (PermissionRequester.RECORDING.hasPermission(requireContext())) connectHeartRate();
            else Toast.makeText(requireContext(),R.string.permission_bluetooth_failed,Toast.LENGTH_LONG).show();
        });
    }

    private String selectedHeartRateAddress() {
        return PreferencesUtils.getString(R.string.settings_sensor_bluetooth_heart_rate_key,de.dennisguse.opentracks.sensors.driver.SensorType.NONE.getPreferenceValue());
    }
    private void heartRateCommand(String suffix) {
        if(suffix.equals(de.dennisguse.opentracks.sensors.driver.BluetoothDriver.DISCONNECT_SUFFIX))
            de.dennisguse.opentracks.p0.ble.HeartRateCommands.setConnectionEnabled(requireContext(),false);
        requireContext().sendBroadcast(new android.content.Intent(requireContext().getPackageName()+suffix)
                .setPackage(requireContext().getPackageName()).putExtra("address",selectedHeartRateAddress())
                .putExtra("measurement",de.dennisguse.opentracks.sensors.driver.HeartRateBluetooth.HEARTRATE.measurementUUID().toString()));
    }
    private void connectHeartRate() {
        if (PreferencesUtils.getSensorType(selectedHeartRateAddress()) != de.dennisguse.opentracks.sensors.driver.SensorType.REMOTE) {
            Toast.makeText(requireContext(),R.string.hr_choose_first,Toast.LENGTH_SHORT).show();return;
        }
        if (!PermissionRequester.RECORDING.hasPermission(requireContext())) {
            java.util.List<String> permissions = new java.util.ArrayList<>(de.dennisguse.opentracks.sensors.driver.BluetoothPermissionPolicy.forApi(android.os.Build.VERSION.SDK_INT));
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            heartRatePermissions.launch(permissions.stream().distinct().toArray(String[]::new));return;
        }
        android.content.Context owner = requireContext().getApplicationContext();
        String selectedAddress = selectedHeartRateAddress();
        de.dennisguse.opentracks.services.TrackRecordingServiceConnection.executeForeground(owner,(service,connection) -> {
            boolean wasEnabled=de.dennisguse.opentracks.p0.ble.HeartRateCommands.connectionEnabled(owner);
            de.dennisguse.opentracks.p0.ble.HeartRateCommands.setConnectionEnabled(owner,true);
            boolean alreadyStarted = service.areSensorsStarted();
            service.tryStartSensors();
            if (alreadyStarted&&wasEnabled) owner.sendBroadcast(new android.content.Intent(owner.getPackageName()+de.dennisguse.opentracks.sensors.driver.BluetoothDriver.RECONNECT_SUFFIX)
                    .setPackage(owner.getPackageName()).putExtra("address",selectedAddress)
                    .putExtra("measurement",de.dennisguse.opentracks.sensors.driver.HeartRateBluetooth.HEARTRATE.measurementUUID().toString()));
            if (isResumed()) previewConnection.bind(requireContext());
        });
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_sensors);
        setWheelCircumferenceInputFilter();
        findPreference("runner_heart_rate_connect").setOnPreferenceClickListener(preference -> { connectHeartRate();return true; });
        findPreference("runner_heart_rate_disconnect").setOnPreferenceClickListener(preference -> { heartRateCommand(de.dennisguse.opentracks.sensors.driver.BluetoothDriver.DISCONNECT_SUFFIX);return true; });

        UnitSystem unitSystem = PreferencesUtils.getUnitSystem();

        final DistanceFormatter formatter = DistanceFormatter.Builder()
                .setDecimalPlaces(0)
                .setUnit(unitSystem)
                .build(getContext());

        findPreference(getString(R.string.recording_gps_accuracy_key))
                .setSummaryProvider(
                        preference -> {
                            Distance distance = PreferencesUtils.getThresholdHorizontalAccuracy();
                            return getString(R.string.settings_recording_min_required_accuracy_summary, formatter.formatDistance(distance));
                        }
                );

        findPreference(getString(R.string.min_sampling_interval_key))
                .setSummaryProvider(
                        preference -> {
                            Duration interval = PreferencesUtils.getMinSamplingInterval();
                            return getString(R.string.settings_recording_location_frequency_summary, getString(R.string.value_integer_second, interval.getSeconds()));
                        }
                );
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_sensors_title);
    }


    @Override
    public void onResume() {
        super.onResume();

        ListPreference minSamplingInterval = findPreference(getString(R.string.min_sampling_interval_key));
        minSamplingInterval.setEntries(PreferencesUtils.getMinSamplingIntervalEntries());
        previewConnection.bind(requireContext());
        previewHandler.removeCallbacks(updateHeartRatePreview);
        previewHandler.post(updateHeartRatePreview);

        ListPreference recordingGpsAccuracy = findPreference(getString(R.string.recording_gps_accuracy_key));
        recordingGpsAccuracy.setEntries(PreferencesUtils.getThresholdHorizontalAccuracyEntries());
    }

    @Override public void onPause() {
        previewHandler.removeCallbacks(updateHeartRatePreview);
        previewConnection.unbind(requireContext());
        super.onPause();
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof BluetoothLeSensorPreference) {
            DialogFragment dialogFragment = ((BluetoothLeSensorPreference) preference).createInstance();

            // Deprecation not fixed upstream: https://issuetracker.google.com/issues/181793702
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getParentFragmentManager(), getClass().getSimpleName());
            return;
        }

        super.onDisplayPreferenceDialog(preference);
    }

    private void setWheelCircumferenceInputFilter() {
        EditTextPreference wheelPreference = findPreference(getString(R.string.settings_sensor_bluetooth_cycling_speed_wheel_circumference_key));
        wheelPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue instanceof String newValueString) {
                try {
                    int newValueInt = Integer.parseInt(newValueString);
                    return newValueInt >= 100 && newValueInt < 4000;
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Entered string is no number.");
                }
            }
            return false;
        });
    }
}
