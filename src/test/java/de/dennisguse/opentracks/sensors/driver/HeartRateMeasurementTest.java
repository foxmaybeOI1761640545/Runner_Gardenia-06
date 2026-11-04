package de.dennisguse.opentracks.sensors.driver;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.List;

public class HeartRateMeasurementTest {
    @Test public void wideBpmAndAllOptionalFieldsUseTheirOwnOffsets() {
        HeartRateMeasurement sample=parse(31,44,1,16,0,0,4,0,2);
        assertEquals(300,sample.bpm());assertEquals(Boolean.TRUE,sample.contactDetected());
        assertEquals(Integer.valueOf(16),sample.energyExpended());assertEquals(List.of(1024,512),sample.rrIntervals1024());
    }
    private HeartRateMeasurement parse(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i=0;i<values.length;i++) bytes[i]=(byte)values[i];
        return HeartRateMeasurement.parse(bytes);
    }
    @Test public void uint8() { assertEquals(255, parse(0,255).bpm()); }
    @Test public void uint16LittleEndian() { assertEquals(300, parse(1,44,1).bpm()); }
    @Test public void unsigned16Boundary() { assertEquals(65535, parse(1,255,255).bpm()); }
    @Test public void contactUnsupported() { assertNull(parse(2,60).contactDetected()); }
    @Test public void contactSupportedDetected() { assertEquals(Boolean.TRUE,parse(6,60).contactDetected()); }
    @Test public void contactSupportedNotDetected() { assertFalse(parse(4,60).hasUsableHeartRate()); }
    @Test public void energyAndMultipleRr() {
        HeartRateMeasurement sample=parse(24,75,16,0,0,4,0,2);
        assertEquals(Integer.valueOf(16),sample.energyExpended());
        assertEquals(List.of(1024,512),sample.rrIntervals1024());
    }
    @Test public void rrRetainsFractionalMillisecondPrecision() { assertEquals(List.of(1,65535),parse(16,75,1,0,255,255).rrIntervals1024()); }
    @Test public void emptyAndNull() { assertNull(HeartRateMeasurement.parse(null));assertNull(parse());assertNull(parse(0)); }
    @Test public void truncatedWideBpm() { assertNull(parse(1,60)); }
    @Test public void truncatedEnergy() { assertNull(parse(8,75,16)); }
    @Test public void truncatedRr() { assertNull(parse(16,75,0,4,1)); }
    @Test public void emptyOrZeroRr() { assertNull(parse(16,75));assertNull(parse(16,75,0,0)); }
    @Test public void extraOrReservedBytes() { assertNull(parse(0,75,1));assertNull(parse(32,75)); }
    @Test public void zeroBpmIsNotUsable() { assertFalse(parse(0,0).hasUsableHeartRate()); }
    @Test(expected=UnsupportedOperationException.class) public void rrListIsImmutable() { parse(16,75,0,4).rrIntervals1024().add(2); }
}
