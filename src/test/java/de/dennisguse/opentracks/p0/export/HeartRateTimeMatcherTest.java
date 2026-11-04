package de.dennisguse.opentracks.p0.export;

import org.junit.Test;
import static org.junit.Assert.*;
import java.time.Instant;
import java.util.List;

public class HeartRateTimeMatcherTest {
    private final Instant base=Instant.parse("2026-11-05T02:00:00Z");
    private HeartRateTimeMatcher.Anchor point(long id,long millis,int segment){return new HeartRateTimeMatcher.Anchor(id,base.plusMillis(millis),segment);}
    @Test public void exactAndNearestWithinOneSecondUseRealPointIds() {
        var matcher=new HeartRateTimeMatcher(List.of(point(10,0,0),point(11,2000,0)));
        assertEquals(10,matcher.match(base).pointId());assertEquals(11,matcher.match(base.plusMillis(1900)).pointId());
        assertEquals(10,matcher.match(base.plusMillis(1000)).pointId());
    }
    @Test public void sparsePointsNeverMatchBeyondOneSecond() {
        var matcher=new HeartRateTimeMatcher(List.of(point(10,0,0),point(11,10000,0)));
        assertNull(matcher.match(base.plusSeconds(5)));
    }
    @Test public void gapAndPausedIntervalsDoNotJoinSegments() {
        var matcher=new HeartRateTimeMatcher(List.of(point(1,0,0),point(2,1000,0),point(3,2000,1),point(4,3000,1)));
        assertNull(matcher.match(base.plusMillis(1500)));assertNull(matcher.match(base.minusMillis(1)));
        assertNull(matcher.match(base.plusMillis(3001)));assertEquals(3,matcher.match(base.plusMillis(2100)).pointId());
    }
    @Test public void emptyAndSinglePointWorkoutsDoNotInventCoordinates() {
        assertNull(new HeartRateTimeMatcher(List.of()).match(base));
        var matcher=new HeartRateTimeMatcher(List.of(point(2,0,0)));
        assertEquals(2,matcher.match(base).pointId());assertNull(matcher.match(base.plusMillis(1)));
    }
    @Test public void overlappingOrBackwardsImportedTimesRemainUnmatched() {
        assertNull(new HeartRateTimeMatcher(List.of(point(1,0,0),point(2,2000,0),point(3,1000,1),point(4,3000,1))).match(base.plusMillis(1000)));
        assertNull(new HeartRateTimeMatcher(List.of(point(1,2000,0),point(2,0,0))).match(base.plusMillis(1000)));
    }
    @Test public void deterministicEarlierPointWinsTiesAndLargeTimesDoNotOverflow() {
        var matcher=new HeartRateTimeMatcher(List.of(point(1,0,0),point(2,2000,0)));
        assertEquals(1,matcher.match(base.plusMillis(1000)).pointId());assertNull(matcher.match(Instant.MAX));assertNull(matcher.match(Instant.MIN));
    }
}
