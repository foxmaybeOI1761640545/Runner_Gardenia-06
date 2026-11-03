package de.dennisguse.opentracks.data.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** One local BLE notification. Capture time comes from the recording monotonic clock. */
public record HeartRateSample(Instant capturedAt, String deviceAddress, String deviceName,
                              Boolean contactDetected, Integer energyExpended,
                              List<Integer> rrIntervals1024, String sampleId, String connectionId) {
    public HeartRateSample(Instant capturedAt, String deviceAddress, String deviceName,
                           Boolean contactDetected, Integer energyExpended, List<Integer> rr) {
        this(capturedAt, deviceAddress, deviceName, contactDetected, energyExpended, rr, UUID.randomUUID().toString(), null);
    }
    public HeartRateSample {
        rrIntervals1024 = List.copyOf(rrIntervals1024);
    }
    public String source() { return "ble_sig_heart_rate"; }
    public HeartRateSample captured(Instant time, String address, String name, String connection) {
        return new HeartRateSample(time, address, name, contactDetected, energyExpended, rrIntervals1024, sampleId, connection);
    }
}
