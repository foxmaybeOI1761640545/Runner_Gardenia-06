package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import org.junit.Test;
import de.dennisguse.opentracks.p0.gps.GpsMotionFilter;

public class GpsMotionFilterTest {
    private static final long SECOND = 1_000_000_000L;
    private static final double METERS_PER_LATITUDE_DEGREE = 111_320.0;

    private GpsMotionFilter.Fix fix(double northMeters, double speed, int second) {
        return new GpsMotionFilter.Fix(1.3 + northMeters / METERS_PER_LATITUDE_DEGREE,
                103.8, true, 2, true, speed, second * SECOND);
    }

    @Test public void stationaryJitterKeepsEveryFixAtOneAnchorAndZeroSpeed() {
        GpsMotionFilter filter = new GpsMotionFilter();
        var first = filter.update(fix(0, 0, 1));
        assertTrue(first.accepted());
        assertTrue(first.stationary());
        for (int second = 2; second <= 60; second++) {
            double jitter = (second % 8 - 4) * 1.5;
            var result = filter.update(fix(jitter, 0, second));
            assertTrue(result.accepted());
            assertTrue(result.stationary());
            assertEquals(first.latitude(), result.latitude(), 0);
            assertEquals(first.longitude(), result.longitude(), 0);
            assertTrue(result.hasSpeed());
            assertEquals(0, result.speedMetersPerSecond(), 0);
        }
    }

    @Test public void twoConsistentMovingFixesReleaseStationaryAnchor() {
        GpsMotionFilter filter = new GpsMotionFilter();
        filter.update(fix(0, 0, 1));
        var first = filter.update(fix(2, 1.2, 2));
        assertTrue(first.stationary());
        var second = filter.update(fix(4, 1.3, 3));
        assertTrue(second.accepted());
        assertFalse(second.stationary());
        assertEquals(4, (second.latitude() - 1.3) * METERS_PER_LATITUDE_DEGREE, 0.01);
    }

    @Test public void decelerationConfirmsStopAndClampsSpeed() {
        GpsMotionFilter filter = new GpsMotionFilter();
        assertFalse(filter.update(fix(0, 3, 1)).stationary());
        filter.update(fix(2, 3, 2));
        filter.update(fix(3, 0.8, 3));
        filter.update(fix(3.5, 0.6, 4));
        filter.update(fix(3.7, 0.4, 5));
        filter.update(fix(3.8, 0.2, 6));
        filter.update(fix(3.8, 0, 7));
        var stopped = filter.update(fix(3.8, 0, 8));
        assertTrue(stopped.accepted());
        assertTrue(stopped.stationary());
        assertEquals(0, stopped.speedMetersPerSecond(), 0);
    }

    @Test public void slowButSustainedMovementCanLeaveStationaryState() {
        GpsMotionFilter filter = new GpsMotionFilter();
        filter.update(fix(0, 0, 1));
        filter.update(fix(1, 0.5, 2));
        filter.update(fix(2, 0.5, 3));
        filter.update(fix(3, 0.5, 4));
        var moving = filter.update(fix(4.1, 0.5, 5));
        assertTrue(moving.stationary());
        moving = filter.update(fix(5.1, 0.5, 6));
        assertFalse(moving.stationary());
    }

    @Test public void isolatedJumpIsRejectedWithoutLosingTheNextGoodFix() {
        GpsMotionFilter filter = new GpsMotionFilter();
        filter.update(fix(0, 3, 1));
        var jump = filter.update(fix(1_000, 3, 2));
        assertFalse(jump.accepted());
        var recovered = filter.update(fix(2, 3, 3));
        assertTrue(recovered.accepted());
        assertFalse(recovered.segmentStart());
    }

    @Test public void repeatedRelocationStartsAnExplicitNewSegment() {
        GpsMotionFilter filter = new GpsMotionFilter();
        filter.update(fix(0, 3, 1));
        assertFalse(filter.update(fix(1_000, 3, 2)).accepted());
        assertFalse(filter.update(fix(1_001, 3, 3)).accepted());
        var rebased = filter.update(fix(1_002, 3, 4));
        assertTrue(rebased.accepted());
        assertTrue(rebased.segmentStart());
    }

    @Test public void longFixGapStartsASeparateSegment() {
        GpsMotionFilter filter = new GpsMotionFilter();
        filter.update(fix(0, 3, 1));
        var next = filter.update(fix(30, 3, 20));
        assertTrue(next.accepted());
        assertTrue(next.segmentStart());
    }

    @Test public void invalidOrNonMonotonicFixDoesNotChangeTheState() {
        GpsMotionFilter filter = new GpsMotionFilter();
        filter.update(fix(0, 3, 1));
        var invalid = new GpsMotionFilter.Fix(91, 103.8, true, 2, true, 3, 2 * SECOND);
        assertFalse(filter.update(invalid).accepted());
        assertFalse(filter.update(fix(1, 3, 1)).accepted());
        assertTrue(filter.update(fix(2, 3, 2)).accepted());
    }

    @Test public void resetClearsPreviousMotionAndAnchor() {
        GpsMotionFilter filter = new GpsMotionFilter();
        filter.update(fix(0, 3, 1));
        filter.reset();
        var next = filter.update(fix(100, 0, 2));
        assertTrue(next.accepted());
        assertTrue(next.stationary());
        assertFalse(next.segmentStart());
    }

    private void sustained(double speed) {
        GpsMotionFilter filter = new GpsMotionFilter();
        var previous = filter.update(fix(0, 0, 1));
        double distance = 0;
        for (int second = 2; second <= 61; second++) {
            var next = filter.update(fix((second - 1) * speed, speed, second));
            assertTrue(next.accepted());
            assertFalse(next.segmentStart());
            distance += Math.abs(next.latitude() - previous.latitude()) * METERS_PER_LATITUDE_DEGREE;
            if (second >= 15) {
                assertFalse("Continuous movement must not oscillate to stopped at " + second, next.stationary());
                assertEquals(speed, next.speedMetersPerSecond(), 0.00001);
            }
            previous = next;
        }
        assertEquals(60 * speed, distance, 0.01);
    }

    @Test public void halfMeterPerSecondForSixtySecondsKeepsAllThirtyMeters() { sustained(0.5); }
    @Test public void slowWalkingForSixtySecondsKeepsAllFortyTwoMeters() { sustained(0.7); }
    @Test public void runningForSixtySecondsKeepsAllDistance() { sustained(3); }
    @Test public void cyclingForSixtySecondsKeepsAllDistance() { sustained(10); }

    @Test public void zeroToPointThreeSpeedNoiseNeverReleasesAStationaryAnchor() {
        var filter = new GpsMotionFilter();
        var anchor = filter.update(fix(0, 0, 1));
        for (int second = 2; second <= 255; second++) {
            var point = filter.update(fix((second % 7 - 3) * 0.1, (second % 4) * 0.1, second));
            assertTrue(point.accepted()); assertTrue(point.stationary());
            assertEquals(anchor.latitude(), point.latitude(), 0);
            assertEquals(0, point.speedMetersPerSecond(), 0);
        }
    }

    @Test public void stoppedRunnerCanResumeSlowWalkingWithoutRepeatedStops() {
        var filter = new GpsMotionFilter();
        filter.update(fix(0, 3, 1));
        for (int second = 2; second <= 8; second++) filter.update(fix(3, 0, second));
        for (int second = 9; second <= 70; second++) {
            var point = filter.update(fix(3 + (second - 8) * 0.5, 0.5, second));
            if (second >= 20) { assertFalse(point.stationary()); assertEquals(0.5, point.speedMetersPerSecond(), 0); }
        }
    }

    @Test public void shortDecelerationDoesNotDeclareAStop() {
        var filter = new GpsMotionFilter();
        filter.update(fix(0, 3, 1));
        assertFalse(filter.update(fix(1, 0.2, 2)).stationary());
        assertFalse(filter.update(fix(1, 0, 3)).stationary());
        assertFalse(filter.update(fix(3, 3, 4)).stationary());
    }

    @Test public void unrelatedConsecutiveJumpsNeverEstablishARelocation() {
        var filter = new GpsMotionFilter();
        filter.update(fix(0, 3, 1));
        for (int second = 2; second <= 10; second++)
            assertFalse(filter.update(fix(second * 1000, 3, second)).accepted());
        assertTrue(filter.update(fix(3, 3, 11)).accepted());
    }

    @Test public void impossibleReportedSpeedCannotBecomeANewSegmentEvenAfterThreeFixes() {
        var filter = new GpsMotionFilter();
        filter.update(fix(0, 3, 1));
        for (int second = 2; second <= 5; second++) assertFalse(filter.update(fix(0, 100, second)).accepted());
        assertFalse(filter.update(fix(3, 3, 6)).segmentStart());
    }

    @Test public void relocationWithoutReportedSpeedDoesNotInheritTheJumpVelocity() {
        var filter = new GpsMotionFilter(); filter.update(fix(0, 3, 1));
        GpsMotionFilter.Result result = null;
        for (int second = 2; second <= 4; second++) {
            var raw = fix(1000 + second, 0, second);
            result = filter.update(new GpsMotionFilter.Fix(raw.latitude(), raw.longitude(), true, 2,
                    false, Double.NaN, raw.elapsedRealtimeNanos()));
        }
        assertTrue(result.accepted()); assertTrue(result.segmentStart());
        assertEquals(0, result.speedMetersPerSecond(), 0);
    }

    @Test public void missingSpeedCanDetectSustainedMotionWithoutAmplifyingStationaryNoise() {
        var filter = new GpsMotionFilter();
        for (int second = 1; second <= 60; second++) {
            var raw = fix((second - 1) * 0.7, 0, second);
            var result = filter.update(new GpsMotionFilter.Fix(raw.latitude(), raw.longitude(), true, 2,
                    false, Double.NaN, raw.elapsedRealtimeNanos()));
            assertTrue(result.accepted());
            if (second > 20) { assertFalse(result.stationary()); assertEquals(0.7, result.speedMetersPerSecond(), 0.002); }
        }
    }

    @Test public void invalidAccuracyAndSpeedNeverMutateTheAnchor() {
        var filter = new GpsMotionFilter(); var anchor = filter.update(fix(0, 0, 1));
        for (double accuracy : new double[]{Double.NaN, -1, Double.POSITIVE_INFINITY})
            assertFalse(filter.update(new GpsMotionFilter.Fix(1.3,103.8,true,accuracy,true,0,2*SECOND)).accepted());
        assertFalse(filter.update(new GpsMotionFilter.Fix(1.3,103.8,false,0,true,0,2*SECOND)).accepted());
        for (double speed : new double[]{Double.NaN, -1, Double.POSITIVE_INFINITY})
            assertFalse(filter.update(new GpsMotionFilter.Fix(1.3,103.8,true,2,true,speed,2*SECOND)).accepted());
        assertEquals(anchor.latitude(), filter.update(fix(0.3,0,3)).latitude(), 0);
    }

    @Test public void rejectedJumpStillAdvancesMonotonicTimestampWatermark() {
        var filter = new GpsMotionFilter(); filter.update(fix(0,3,1));
        assertFalse(filter.update(fix(1000,3,5)).accepted());
        assertFalse(filter.update(fix(3,3,4)).accepted());
        assertTrue(filter.update(fix(4,3,6)).accepted());
    }

    @Test public void stoppingJitterAddsNoDistanceEvenDuringStopConfirmation() {
        var filter = new GpsMotionFilter();
        filter.update(fix(0,3,1));
        var lastMoving = filter.update(fix(3,3,2));
        for (int second = 3; second <= 65; second++) {
            var point = filter.update(fix(3 + (second % 5 - 2) * 0.1, (second % 4) * 0.1, second));
            assertTrue(point.accepted());
            assertEquals(lastMoving.latitude(), point.latitude(), 0);
            assertEquals(0, point.speedMetersPerSecond(), 0);
            if (second >= 6) assertTrue(point.stationary());
        }
    }

    @Test public void longRejectedStreamCannotLegitimizeAnUnrelatedJumpThroughAgeAlone() {
        var filter = new GpsMotionFilter(); filter.update(fix(0,3,1));
        for (int second = 2; second <= 80; second++)
            assertFalse("Unrelated jump at " + second, filter.update(fix(1000 + second % 2 * 1000,3,second)).accepted());
        var recovered = filter.update(fix(3,3,81));
        assertTrue(recovered.accepted()); assertFalse(recovered.segmentStart());
    }
}
