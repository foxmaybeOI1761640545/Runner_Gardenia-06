package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import org.junit.Test;
import de.dennisguse.opentracks.p0.statistics.HeartRateSummary;

public class HeartRateSummaryTest {
    @Test public void equalValuesCountAsSeparateNotifications() {
        var values=new HeartRateSummary.Accumulator();values.add(72);values.add(72);
        assertEquals(2,values.summary().count());assertEquals(72,values.summary().average(),0);
    }
    @Test public void invalidAndMissingValuesDoNotInventZeroHeartRate() {
        var values=new HeartRateSummary.Accumulator();
        for(double v:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY,65536})values.add(v);
        assertEquals(HeartRateSummary.EMPTY,values.summary());assertTrue(Double.isNaN(values.summary().minimum()));
    }
    @Test public void exactExtremaAndNotificationWeightedMean() {
        var values=new HeartRateSummary.Accumulator();values.add(60);values.add(80);values.add(60);
        assertEquals(60,values.summary().minimum(),0);assertEquals(80,values.summary().maximum(),0);
        assertEquals(200d/3,values.summary().average(),1e-10);
    }
    @Test public void fullSigRangeIsRetained() {
        var values=new HeartRateSummary.Accumulator();values.add(1);values.add(65535);
        assertEquals(32768,values.summary().average(),0);
    }
}
