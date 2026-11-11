package de.dennisguse.opentracks.p0;
import static org.junit.Assert.*;
import org.junit.Test;
import java.time.Duration;
import de.dennisguse.opentracks.p0.gps.MotionFormat;
public class MotionFormatTest {
    @Test public void distanceChangesUnitsAtOneKilometerWithoutChangingStoredValues() {
        assertEquals("0.0 m",MotionFormat.distance(0));assertEquals("999.9 m",MotionFormat.distance(999.9));assertEquals("1.00 km",MotionFormat.distance(1000));assertEquals("42.20 km",MotionFormat.distance(42195));
    }
    @Test public void invalidDistanceNeverBecomesPlausibleData() {
        assertEquals("—",MotionFormat.distance(-1));assertEquals("—",MotionFormat.distance(Double.NaN));assertEquals("—",MotionFormat.distance(Double.POSITIVE_INFINITY));
    }
    @Test public void zeroOrMissingSpeedDoesNotInventPace() {assertEquals("—",MotionFormat.pace(0));assertEquals("—",MotionFormat.pace(Double.NaN));}
    @Test public void nearZeroSpeedDoesNotProduceAnImplausiblePace() {
        assertEquals("0.0 km/h",MotionFormat.speed(0.2));
        assertEquals("—",MotionFormat.pace(0.2));
        assertEquals("—",MotionFormat.pace(0.5));
    }
    @Test public void speedAndPaceUseConsistentMetricUnits() {assertEquals("9.0 km/h",MotionFormat.speed(2.5));assertEquals("6:40 /km",MotionFormat.pace(2.5));}
    @Test public void durationRetainsHoursBeyondADay() {assertEquals("25:00:01",MotionFormat.duration(Duration.ofHours(25).plusSeconds(1)));}
    @Test public void subHourDurationIsReadable() {assertEquals("01:02",MotionFormat.duration(Duration.ofSeconds(62)));}
}
