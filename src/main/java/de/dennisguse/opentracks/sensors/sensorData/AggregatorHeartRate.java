package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.HeartRate;

public class AggregatorHeartRate extends Aggregator<HeartRate, HeartRate> {

    public AggregatorHeartRate(String name, String address) {
        super(name, address);
    }

    public void invalidate() { output = null; previous = null; }

    @Override
    protected void computeValue(@NonNull Raw<HeartRate> current) {
        this.output = current.value().isValid() ? current.value() : null;
    }
}
