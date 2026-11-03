package de.dennisguse.opentracks.sensors.sensorData;

import static org.junit.Assert.*;
import org.junit.Test;
import java.time.Instant;
import de.dennisguse.opentracks.data.models.HeartRate;

public class AggregatorHeartRateTest {
    private final AggregatorHeartRate aggregator=new AggregatorHeartRate("selected-device","Chest strap");
    private final Instant now=Instant.parse("2026-11-04T00:00:00Z");
    @Test public void currentDataExpiresAfterFiveSeconds() { aggregator.add(now,HeartRate.of(75));assertEquals(HeartRate.of(75),aggregator.getAggregatedValue(now.plusSeconds(5)));assertNull(aggregator.getAggregatedValue(now.plusSeconds(6))); }
    @Test public void invalidContactOrZeroImmediatelyClearsPreviousData() { aggregator.add(now,HeartRate.of(75));aggregator.add(now.plusSeconds(1),HeartRate.INVALID);assertNull(aggregator.getAggregatedValue(now.plusSeconds(1))); }
    @Test public void disconnectInvalidatesImmediately() { aggregator.add(now,HeartRate.of(75));aggregator.invalidate();assertNull(aggregator.getAggregatedValue(now)); }
    @Test public void trackpointAggregationResetPreservesInstantValue() { aggregator.add(now,HeartRate.of(75));aggregator.resetAggregated();assertEquals(HeartRate.of(75),aggregator.getAggregatedValue(now)); }
}
