package de.dennisguse.opentracks.sensors;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.sensors.driver.BluetoothDriver;
import de.dennisguse.opentracks.sensors.driver.Driver;
import de.dennisguse.opentracks.sensors.driver.HeartRateBluetooth;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorHeartRate;

public class HeartRateHandler extends SensorHandler<HeartRate, HeartRate> {

    private String selectedAddress;
    private String selectedName;
    private String connectionId;
    private de.dennisguse.opentracks.sensors.driver.BleConnectionSession.State connectionState = de.dennisguse.opentracks.sensors.driver.BleConnectionSession.State.DISCONNECTED;
    private java.time.Instant lastReceived;
    private boolean contactLost;

    @Override public void onConnectionStateChanged(de.dennisguse.opentracks.sensors.driver.BleConnectionSession.State state) { connectionState = state; }
    public HeartRateStatus getStatus(java.time.Instant now) {
        return new HeartRateStatus(connectionState,lastReceived,now,selectedName != null ? selectedName : selectedAddress,contactLost);
    }

    protected HeartRateHandler(SensorManager sensorManager) {
        super(sensorManager);
    }

    @NonNull
    @Override
    protected Driver createDriverBluetooth(String address) {
        return new BluetoothDriver<>(
                BluetoothUtils.getAdapter(getContext()),
                this,
                new HeartRateBluetooth()
        );
    }

    @NonNull
    @Override
    protected AggregatorHeartRate createAggregator(String sensorAddress, String sensorName) {
        return new AggregatorHeartRate(sensorAddress, sensorName);
    }

    @Override
    public void onConnected(String address, String name) {
        connectionId = java.util.UUID.randomUUID().toString();
        selectedAddress = address;
        selectedName = name;
        super.onConnected(address, name);
    }

    @Override
    public void onDataReceived(HeartRate value) {
        contactLost = value.sample() != null && Boolean.FALSE.equals(value.sample().contactDetected());
        if (value.isValid()) lastReceived = sensorManager.getNow();
        else connectionId = java.util.UUID.randomUUID().toString(); // break continuity after contact loss
        if (value.sample() != null) {
            value = new HeartRate(value.value(), value.sample().captured(sensorManager.getNow(), selectedAddress, selectedName, connectionId));
        }
        super.onDataReceived(value);
    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();
        selectedAddress = null; selectedName = null; lastReceived = null; contactLost = false;
        connectionState = de.dennisguse.opentracks.sensors.driver.BleConnectionSession.State.DISCONNECTED;
    }

    @Override
    public void onConnectionLost() {
        super.onConnectionLost();
        if (aggregator instanceof AggregatorHeartRate heartRate) heartRate.invalidate();
    }

    @Override
    protected int getSensorPreferenceKey() {
        return R.string.settings_sensor_bluetooth_heart_rate_key;
    }
}
