package de.dennisguse.opentracks.p0;

import org.junit.Test;
import static org.junit.Assert.*;
import de.dennisguse.opentracks.p0.gps.GpsReadiness;

public class GpsReadinessTest {
    private static final long SECOND = 1_000_000_000L;
    private final GpsReadiness gate = new GpsReadiness(10 * SECOND);
    private GpsReadiness.Fix fix(double lat, double accuracy, long time) {
        return new GpsReadiness.Fix(lat, 103.8, true, accuracy, time * SECOND);
    }
    @Test public void cachedFixCapturedBeforeStartNeverStarts() { assertFalse(gate.add(fix(1.3, 2, 9), 11 * SECOND)); }
    @Test public void fixAtStartIsNotNewAcquisition() { assertFalse(gate.add(fix(1.3, 2, 10), 11 * SECOND)); }
    @Test public void staleFixNeverStarts() { assertFalse(gate.add(fix(1.3, 2, 11), 17 * SECOND)); }
    @Test public void futureElapsedTimeNeverStarts() { assertFalse(gate.add(fix(1.3, 2, 12), 11 * SECOND)); }
    @Test public void missingMonotonicTimeNeverStarts() { assertFalse(gate.add(fix(1.3, 2, 0), 11 * SECOND)); }
    @Test public void inaccurateFixNeverStarts() { assertFalse(gate.add(fix(1.3, 31, 11), 11 * SECOND)); }
    @Test public void missingAccuracyNeverStarts() { assertFalse(gate.add(new GpsReadiness.Fix(1.3,103.8,false,0,11*SECOND),11*SECOND)); }
    @Test public void nonFiniteOrNegativeAccuracyNeverStarts() {
        for(double v:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1})assertFalse(gate.add(fix(1.3,v,11),11*SECOND));
    }
    @Test public void invalidCoordinatesNeverStart() {
        for(double v:new double[]{Double.NaN,Double.POSITIVE_INFINITY,91,-91})assertFalse(gate.add(fix(v,2,11),11*SECOND));
        assertFalse(gate.add(new GpsReadiness.Fix(1.3,181,true,2,11*SECOND),11*SECOND));
    }
    @Test public void oneValidFixDoesNotStart() { assertFalse(gate.add(fix(1.3,2,11),11*SECOND)); }
    @Test public void threeStableFreshFixesStartExactlyOnce() {
        assertFalse(gate.add(fix(1.3,2,11),11*SECOND));assertFalse(gate.add(fix(1.30001,2,12),12*SECOND));
        assertTrue(gate.add(fix(1.30002,2,13),13*SECOND));assertFalse(gate.add(fix(1.30003,2,14),14*SECOND));
    }
    @Test public void duplicateNotificationTimesCannotSatisfyStability() {
        for(int i=0;i<5;i++)assertFalse(gate.add(fix(1.3,2,11),11*SECOND));
    }
    @Test public void manyFixesWithinTooShortATimeCannotStart() {
        for(int i=1;i<=5;i++)assertFalse(gate.add(new GpsReadiness.Fix(1.3,103.8,true,2,10*SECOND+i*100_000_000L),11*SECOND));
    }
    @Test public void invalidFixBreaksConsecutiveSequence() {
        assertFalse(gate.add(fix(1.3,2,11),11*SECOND));assertFalse(gate.add(fix(1.3,99,12),12*SECOND));
        assertFalse(gate.add(fix(1.3,2,13),13*SECOND));assertFalse(gate.add(fix(1.3,2,14),14*SECOND));assertTrue(gate.add(fix(1.3,2,15),15*SECOND));
    }
    @Test public void hugeJumpCannotStartAndRequiresANewStableSequence() {
        assertFalse(gate.add(fix(1.3,2,11),11*SECOND));assertFalse(gate.add(fix(1.31,2,12),12*SECOND));
        assertFalse(gate.add(fix(1.3,2,13),13*SECOND));assertFalse(gate.add(fix(1.3,2,14),14*SECOND));assertTrue(gate.add(fix(1.3,2,15),15*SECOND));
    }
    @Test public void longGapRestartsTheStableSequence() {
        assertFalse(gate.add(fix(1.3,2,11),11*SECOND));assertFalse(gate.add(fix(1.3,2,17),17*SECOND));assertFalse(gate.add(fix(1.3,2,18),18*SECOND));assertTrue(gate.add(fix(1.3,2,19),19*SECOND));
    }
    @Test public void timeoutIsOnlyAWaitingSignalNeverAStartDecision() {
        assertFalse(gate.timedOut(69*SECOND));assertTrue(gate.timedOut(70*SECOND));
        assertFalse(gate.add(fix(1.3,2,71),71*SECOND));assertFalse(gate.add(fix(1.3,2,72),72*SECOND));assertTrue(gate.add(fix(1.3,2,73),73*SECOND));assertFalse(gate.timedOut(74*SECOND));
    }
    @Test public void newGateAfterCancelDoesNotReuseOldReadiness() {
        gate.add(fix(1.3,2,11),11*SECOND);gate.add(fix(1.3,2,12),12*SECOND);
        var next=new GpsReadiness(12*SECOND);assertFalse(next.add(fix(1.3,2,12),13*SECOND));assertFalse(next.add(fix(1.3,2,13),13*SECOND));
    }
    @Test public void fiveSecondBoundaryIsAllowedButOlderIsNot() {
        var f=fix(1.3,30,11);assertTrue(GpsReadiness.usable(f,16*SECOND,0,GpsReadiness.DEFAULT));assertFalse(GpsReadiness.usable(f,16*SECOND+1,0,GpsReadiness.DEFAULT));
    }
}
