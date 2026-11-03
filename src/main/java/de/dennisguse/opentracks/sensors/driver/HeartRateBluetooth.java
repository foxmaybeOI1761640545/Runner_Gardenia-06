package de.dennisguse.opentracks.sensors.driver;

import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.HeartRateSample;
import de.dennisguse.opentracks.sensors.ServiceMeasurementUUID;

public class HeartRateBluetooth implements BluetoothDriver.BluetoothParser<HeartRate> {

    public static final ServiceMeasurementUUID HEARTRATE = new ServiceMeasurementUUID(
            new UUID(0x180D00001000L, 0x800000805f9b34fbL),
            new UUID(0x2A3700001000L, 0x800000805f9b34fbL)
    );

    // Used for device discovery in preferences
    public static final List<ServiceMeasurementUUID> HEART_RATE_SUPPORTING_DEVICES = List.of(
            HEARTRATE,
            //Devices that support HEART_RATE_SERVICE_UUID, but do not announce HEART_RATE_SERVICE_UUID in there BLE announcement messages (during device discovery).
            new ServiceMeasurementUUID(
                    UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb"), //Miband3
                    HEARTRATE.measurementUUID()
            )
    );

    @Override
    public List<ServiceMeasurementUUID> getServices() {
        return HEART_RATE_SUPPORTING_DEVICES;
    }

    @Override
    public HeartRate parsePayload(@NonNull ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, BluetoothGattCharacteristic characteristic) {
        return parseHeartRate(characteristic);
    }

    @VisibleForTesting
    public static HeartRate parseHeartRate(BluetoothGattCharacteristic characteristic) {
        return parseHeartRate(characteristic.getValue());
    }

    public static HeartRate parseHeartRate(byte[] bytes) {
        HeartRateMeasurement measurement = HeartRateMeasurement.parse(bytes);
        if (measurement == null) return null;
        return measurement.hasUsableHeartRate() ? HeartRate.of(measurement.bpm()) : HeartRate.INVALID;
    }

    @Override
    public HeartRate parsePayload(ServiceMeasurementUUID service, String name,
                                  BluetoothGattCharacteristic characteristic, byte[] bytes) {
        HeartRateMeasurement decoded = HeartRateMeasurement.parse(bytes);
        if (decoded == null) return null;
        return new HeartRate(decoded.hasUsableHeartRate() ? decoded.bpm() : 0, new HeartRateSample(null, null, name,
                decoded.contactDetected(), decoded.energyExpended(), decoded.rrIntervals1024()));
    }

    public static final ServiceMeasurementUUID BODY_LOCATION = new ServiceMeasurementUUID(
            HEARTRATE.serviceUUID(), UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb"));
    private Integer bodySensorLocation;
    private Integer batteryLevel;

    @Override
    public List<ServiceMeasurementUUID> getOptionalReads() {
        return List.of(BODY_LOCATION, de.dennisguse.opentracks.sensors.BluetoothUtils.BATTERY);
    }

    @Override
    public void onOptionalRead(UUID characteristic, byte[] bytes) {
        if (bytes == null || bytes.length != 1) return;
        int value = bytes[0] & 0xff;
        if (BODY_LOCATION.measurementUUID().equals(characteristic) && value <= 6) bodySensorLocation = value;
        if (de.dennisguse.opentracks.sensors.BluetoothUtils.BATTERY.measurementUUID().equals(characteristic)
                && value <= 100) batteryLevel = value;
    }

    @Override public void resetOptionalMetadata() { bodySensorLocation = null; batteryLevel = null; }

    public Integer getBodySensorLocation() { return bodySensorLocation; }
    public Integer getBatteryLevel() { return batteryLevel; }
}
