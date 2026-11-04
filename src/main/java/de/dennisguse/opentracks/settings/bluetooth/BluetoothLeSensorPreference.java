package de.dennisguse.opentracks.settings.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.sensors.BluetoothUtils;
import de.dennisguse.opentracks.sensors.ServiceMeasurementUUID;
import de.dennisguse.opentracks.sensors.driver.SensorType;
import de.dennisguse.opentracks.sensors.driver.BluetoothDriver;
import de.dennisguse.opentracks.sensors.driver.BleScanSession;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.PermissionRequester;

/**
 * Preference to select a discoverable Bluetooth LE device.
 * Based upon ListPreference.
 */
@SuppressLint("MissingPermission")
public abstract class BluetoothLeSensorPreference extends DialogPreference {

    private static final String TAG = BluetoothLeSensorPreference.class.getSimpleName();

    private static final String ARG_BLE_SERVICE_UUIDS = "bluetoothUUID";
    private static final String ARG_BLE_MEASUREMENTS = "bluetoothMeasurements";
    private static final String ARG_INCLUDE_INTERNAL = "supportsInternal";
    private static final String ARG_FORCE_SERVICE_FILTER = "forceServiceFilter";

    private static final int DEVICE_NONE_RESOURCEID = R.string.value_none;

    private static final int SENSOR_INTERNAL_RESOURCEID = R.string.value_internal_sensor;

    public BluetoothLeSensorPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public BluetoothLeSensorPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BluetoothLeSensorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BluetoothLeSensorPreference(Context context) {
        super(context);
    }

    private String value;
    private boolean valueSet = false;

    private void setValue(String value) {
        final boolean changed = !TextUtils.equals(this.value, value);
        if (changed || !valueSet) {
            this.value = value;
            valueSet = true;
            persistString(value);
            if (changed) {
                notifyChanged();
            }
        }
    }

    @Nullable
    @Override
    protected Object onGetDefaultValue(@NonNull TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        setValue(getPersistedString((String) defaultValue));
    }

    @Override
    public CharSequence getSummary() {
        if (value == null || SensorType.NONE.getPreferenceValue().equals(value)) {
            return getContext().getString(DEVICE_NONE_RESOURCEID);
        }
        if (SensorType.INTERNAL.getPreferenceValue().equals(value)) {
            return getContext().getString(SENSOR_INTERNAL_RESOURCEID);
        }

        BluetoothAdapter bluetoothAdapter = BluetoothUtils.getAdapter(getContext());
        if (bluetoothAdapter == null) {
            Log.w(TAG, "No Bluetooth adapter present");
            return value;
        }

        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(value);
            if (device != null && device.getName() != null) {
                return getContext().getString(R.string.bluetooth_sensor_summary, device.getAddress(), device.getName());
            }
        } catch (SecurityException | IllegalArgumentException unavailable) {
            // Reading a remembered sensor must remain safe after permission revocation.
        }
        return value;
    }

    public abstract PreferenceDialogFragmentCompat createInstance();

    public static class BluetoothLeSensorPreferenceDialog extends PreferenceDialogFragmentCompat {

        private AnimatedVectorDrawableCompat bluetoothIcon;

        private int selectedEntryIndex;
        private final BluetoothLeAdapter listAdapter = new BluetoothLeAdapter();
        @androidx.annotation.VisibleForTesting
        public BluetoothLeAdapter getDeviceAdapter() { return listAdapter; }

        private BluetoothLeScanner scanner = null;
        private final BleScanSession scanSession = new BleScanSession();
        private final Handler scanHandler = new Handler(Looper.getMainLooper());
        private Runnable scanTimeout;
        private ScanCallback scanCallback;

        private void stopBluetoothScan() {
            scanSession.cancel();
            stopScanTransport();
        }

        private void stopScanTransport() {
            if (scanTimeout != null) scanHandler.removeCallbacks(scanTimeout);
            scanTimeout = null;
            if (scanner != null && scanCallback != null) {
                try { scanner.stopScan(scanCallback); }
                catch (SecurityException | IllegalStateException unavailable) { Log.i(TAG, "Scan stopped after Bluetooth/permission change"); }
            }
            scanner = null;
            scanCallback = null;
            if (bluetoothIcon != null) bluetoothIcon.stop();
        }
        private ScanCallback createScanCallback(int token) { return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                onBatchScanResults(List.of(result));
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                if (!scanSession.accepts(token) || getContext() == null) return;
                try {
                    BluetoothManager manager = getContext().getSystemService(BluetoothManager.class);
                    for (ScanResult result : results) {
                        boolean connected = manager != null && manager.getConnectionState(result.getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED;
                        String advertised = result.getScanRecord() != null ? result.getScanRecord().getDeviceName() : null;
                        listAdapter.add(result.getDevice().getAddress(), BleScanSession.displayName(result.getDevice().getName(),advertised), result.getRssi(), connected);
                    }
                    listAdapter.notifyDataSetChanged();
                } catch (SecurityException denied) {
                    stopScanTransport();
                    scanSession.begin(false, true);
                    Toast.makeText(getContext(), R.string.permission_bluetooth_failed, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onScanFailed(int errorCode) {
                if (!scanSession.accepts(token) || getContext() == null) return;
                scanSession.fail(token);
                stopScanTransport();
                super.onScanFailed(errorCode);
                Log.e(TAG, "Bluetooth scan failed with errorCode " + errorCode);
                Toast.makeText(getContext(), R.string.sensor_could_not_scan, Toast.LENGTH_LONG).show();
            }
        }; }

        public static BluetoothLeSensorPreferenceDialog newInstance(String preferenceKey, List<ServiceMeasurementUUID> sensorUUIDs) {
            return newInstance(preferenceKey, sensorUUIDs, false);
        }

        public static BluetoothLeSensorPreferenceDialog newInstance(String preferenceKey, List<ServiceMeasurementUUID> sensorUUIDs, boolean includeInternalSensor) {
            return newInstance(preferenceKey,sensorUUIDs,includeInternalSensor,false);
        }

        public static BluetoothLeSensorPreferenceDialog newInstance(String preferenceKey, List<ServiceMeasurementUUID> sensorUUIDs, boolean includeInternalSensor, boolean forceServiceFilter) {
            final BluetoothLeSensorPreferenceDialog fragment = new BluetoothLeSensorPreferenceDialog();
            final Bundle b = new Bundle(3);
            b.putString(ARG_KEY, preferenceKey);
            b.putParcelableArrayList(ARG_BLE_SERVICE_UUIDS, new ArrayList<>(sensorUUIDs.stream()
                    .map(ServiceMeasurementUUID::serviceUUID)
                    .map(ParcelUuid::new)
                    .toList()));
            b.putStringArrayList(ARG_BLE_MEASUREMENTS,new ArrayList<>(sensorUUIDs.stream().map(it -> it.measurementUUID().toString()).distinct().toList()));
            b.putBoolean(ARG_INCLUDE_INTERNAL, includeInternalSensor);
            b.putBoolean(ARG_FORCE_SERVICE_FILTER,forceServiceFilter);

            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Don't know why: need to load the drawable _twice_, so that animation is actually started.
            bluetoothIcon = AnimatedVectorDrawableCompat.create(getContext(), R.drawable.ic_bluetooth_searching_animated_24dp);
            bluetoothIcon = AnimatedVectorDrawableCompat.create(getContext(), R.drawable.ic_bluetooth_searching_animated_24dp);
            bluetoothIcon.start();

            PermissionRequester.BLUETOOTH.requestPermissionsIfNeeded(getContext(), this,
                    this::startBluetoothScan,
                    (requester) -> {
                        if (requester.shouldShowRequestPermissionRationale(this)) {
                            Toast.makeText(getContext(), R.string.permission_bluetooth_failed_rejected, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), R.string.permission_bluetooth_failed, Toast.LENGTH_SHORT).show();
                        }
                        dismiss();
                    });

        }

        private void startBluetoothScan() {
            if (!isAdded() || getContext() == null) return;
            stopBluetoothScan();
            try {
                scanWithPermission();
            } catch (SecurityException denied) {
                stopScanTransport();
                scanSession.begin(false, true);
                Toast.makeText(getContext(), R.string.permission_bluetooth_failed, Toast.LENGTH_SHORT).show();
            } catch (IllegalStateException poweredOff) {
                stopScanTransport();
                scanSession.begin(true, false);
                Toast.makeText(getContext(), R.string.bluetooth_disabled, Toast.LENGTH_SHORT).show();
            }
        }

        private void scanWithPermission() {
            List<ParcelUuid> serviceUUIDs = getArguments().getParcelableArrayList(ARG_BLE_SERVICE_UUIDS);
            boolean includeInternalSensor = getArguments().getBoolean(ARG_INCLUDE_INTERNAL);

            listAdapter.add(SensorType.NONE.getPreferenceValue(), getContext().getString(DEVICE_NONE_RESOURCEID));
            selectedEntryIndex = 0;

            BluetoothLeSensorPreference preference = (BluetoothLeSensorPreference) getPreference();
            String deviceSelected = preference.value;
            if (includeInternalSensor) {
                listAdapter.add(SensorType.INTERNAL.getPreferenceValue(), getString(SENSOR_INTERNAL_RESOURCEID));
                if (SensorType.INTERNAL.getPreferenceValue().equals(deviceSelected)) {
                    selectedEntryIndex = 1;
                }
            }

            if (deviceSelected != null && SensorType.REMOTE.equals(PreferencesUtils.getSensorType(deviceSelected))) {
                listAdapter.add(preference.value, preference.value);
                selectedEntryIndex = !includeInternalSensor ? 1 : 2;
            }

            BluetoothAdapter bluetoothAdapter = BluetoothUtils.getAdapter(getContext());
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                scanSession.begin(true, false);
                Log.w(TAG, "Bluetooth adapter is absent or not enabled. Skipping BLE scan.");
                Toast.makeText(getContext(), R.string.bluetooth_disabled, Toast.LENGTH_SHORT).show();
                if (bluetoothIcon != null) bluetoothIcon.stop();
                listAdapter.notifyDataSetChanged();
                return;
            }

            if (bluetoothAdapter.isDiscovering()) {
                Log.i(TAG, "Cancelling ongoing Bluetooth discovery.");
                bluetoothAdapter.cancelDiscovery();
            }

            scanner = bluetoothAdapter.getBluetoothLeScanner();
            if (scanner == null) {
                Log.e(TAG, "BluetoothLeScanner is null.");
                dismiss();
                return;
            }

            // Pre-populate with already-bonded BLE devices so that devices that are paired but not actively advertising (e.g. Bosch eBike) are still shown. In case we dont have any cached UUIDs for a device, we just add it to the list and let the user decide
            boolean filterEnabled = getArguments().getBoolean(ARG_FORCE_SERVICE_FILTER,false)||PreferencesUtils.getBluetoothFilterEnabled();
            BluetoothManager manager=getContext().getSystemService(BluetoothManager.class);
            if(deviceSelected!=null&&BluetoothAdapter.checkBluetoothAddress(deviceSelected)) {
                BluetoothDevice remembered=bluetoothAdapter.getRemoteDevice(deviceSelected);
                listAdapter.add(remembered.getAddress(),remembered.getName(),null,
                        manager!=null&&manager.getConnectionState(remembered,BluetoothProfile.GATT)==BluetoothProfile.STATE_CONNECTED);
            }
            bluetoothAdapter.getBondedDevices().stream()
                    .filter(bonded -> bonded.getType() == BluetoothDevice.DEVICE_TYPE_LE || bonded.getType() == BluetoothDevice.DEVICE_TYPE_DUAL)
                    .filter(bonded -> !filterEnabled || bonded.getUuids() == null || java.util.Arrays.stream(bonded.getUuids()).anyMatch(serviceUUIDs::contains))
                    .forEach(bonded -> listAdapter.add(bonded.getAddress(), bonded.getName(),null,
                            manager!=null&&manager.getConnectionState(bonded,BluetoothProfile.GATT)==BluetoothProfile.STATE_CONNECTED));

            List<ScanFilter> scanFilter = null;
            if (filterEnabled) {
                scanFilter = serviceUUIDs.stream()
                        .map(it -> new ScanFilter.Builder().setServiceUuid(it).build())
                        .toList();
            }

            ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
            scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

            int token = scanSession.begin(true, true);
            scanCallback = createScanCallback(token);
            if (bluetoothIcon != null) bluetoothIcon.start();
            scanner.startScan(scanFilter, scanSettingsBuilder.build(), scanCallback);
            scanTimeout = () -> { if (scanSession.complete(token)) stopScanTransport(); };
            scanHandler.postDelayed(scanTimeout, 15000);
            listAdapter.notifyDataSetChanged();
        }

        //Behave like ListPreferenceDialogFragmentCompat, but uses a custom listAdapter.
        @Override
        protected void onPrepareDialogBuilder(@NonNull AlertDialog.Builder builder) {
            super.onPrepareDialogBuilder(builder);

            builder.setSingleChoiceItems(listAdapter, selectedEntryIndex,
                    (dialog, which) -> {
                        selectedEntryIndex = which;

                        BluetoothLeSensorPreferenceDialog.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                        dialog.dismiss();
                    });

            builder.setIcon(bluetoothIcon);

            builder.setPositiveButton(null, null);
            builder.setNeutralButton(R.string.sensor_scan_again, null);
        }

        @Override
        public void onStart() {
            super.onStart();
            if (getDialog() instanceof AlertDialog dialog) {
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(unused -> startBluetoothScan());
            }
        }

        @Override
        public void onStop() {
            stopBluetoothScan();
            super.onStop();
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            stopBluetoothScan();

            if (positiveResult && selectedEntryIndex >= 0) {
                String value = listAdapter.get(selectedEntryIndex).getAddress();
                BluetoothLeSensorPreference preference = (BluetoothLeSensorPreference) getPreference();
                if (preference.callChangeListener(value)) {
                    boolean reconnect = value.equals(preference.value) && SensorType.REMOTE.equals(PreferencesUtils.getSensorType(value));
                    preference.setValue(value);
                    if (reconnect && getContext() != null) {
                        getContext().sendBroadcast(new Intent(getContext().getPackageName() + BluetoothDriver.RECONNECT_SUFFIX)
                                .setPackage(getContext().getPackageName()).putExtra("address", value)
                                .putStringArrayListExtra("measurements",getArguments().getStringArrayList(ARG_BLE_MEASUREMENTS)));
                    }
                }
            }
        }

        @Override
        public void onDestroy() {
            stopBluetoothScan();
            bluetoothIcon = null;
            super.onDestroy();
        }
    }
}
