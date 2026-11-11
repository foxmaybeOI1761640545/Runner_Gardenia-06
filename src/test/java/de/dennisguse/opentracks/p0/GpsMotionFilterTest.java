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
        var moving = filter.update(fix(4, 0.5, 5));
        assertTrue(moving.stationary());
        moving = filter.update(fix(5, 0.5, 6));
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
}
