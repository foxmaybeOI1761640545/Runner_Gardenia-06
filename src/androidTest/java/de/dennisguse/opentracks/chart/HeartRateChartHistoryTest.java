package de.dennisguse.opentracks.chart;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import java.time.Instant;
import java.util.List;
import de.dennisguse.opentracks.data.*;
import de.dennisguse.opentracks.data.models.*;

public class HeartRateChartHistoryTest {
    private final Context context=ApplicationProvider.getApplicationContext();
    private ContentProviderUtils tracks;private HeartRateRepository raw;private Track.Id track;
    @Before public void prepare() {
        tracks=new ContentProviderUtils(context);tracks.deleteAllTracks(context);raw=new HeartRateRepository(context);
        track=tracks.insertTrack(new Track(null,java.util.UUID.randomUUID(),"HR chart","","",ActivityType.UNKNOWN,java.time.ZoneOffset.UTC,Statistics.DEFAULT));
        tracks.insertTrackPoint(new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL,Instant.EPOCH),track);
    }
    private void add(int second,int bpm,String connection) {
        raw.insert(track,new HeartRate(bpm,new HeartRateSample(Instant.EPOCH.plusSeconds(second),"strap","Strap",true,null,List.of())
                .captured(Instant.EPOCH.plusSeconds(second),"strap","Strap",connection)));
    }
    @Test public void incrementalReloadKeepsExactStatsWithoutDuplicateFrames() {
        add(1,60,"a");add(2,90,"a");HeartRateChartHistory history=new HeartRateChartHistory();
        var first=history.load(context,track);assertEquals(2,first.count());assertEquals(75,first.average(),0);
        assertEquals(2,history.load(context,track).count());add(3,72,"a");var updated=history.load(context,track);
        assertEquals(3,updated.count());assertEquals(60,updated.min(),0);assertEquals(90,updated.max(),0);assertEquals(74,updated.average(),0);
    }
    @Test public void connectionChangesAndStaleIntervalsBreakCurves() {
        add(1,70,"a");add(2,71,"a");add(3,72,"b");add(10,73,"b");
        var p=new HeartRateChartHistory().load(context,track).points();
        assertTrue(p.get(0).breakBefore());assertFalse(p.get(1).breakBefore());assertTrue(p.get(2).breakBefore());assertTrue(p.get(3).breakBefore());
    }
    @Test public void pausedWallTimeIsExcludedFromChartXAxis() {
        tracks.insertTrackPoint(new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL,Instant.EPOCH.plusSeconds(2)),track);
        tracks.insertTrackPoint(new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL,Instant.EPOCH.plusSeconds(10)),track);
        add(1,70,"a");add(11,75,"b");var p=new HeartRateChartHistory().load(context,track).points();
        assertEquals(1000,p.get(0).x(),0);assertEquals(3000,p.get(1).x(),0);assertTrue(p.get(1).breakBefore());
        assertNull(HeartRateChartHistory.activeMillis(List.of(new long[]{0,2000,0}),5000));
    }
    @Test public void oldTrackWithoutRawSamplesIsNotInvented() {
        var result=new HeartRateChartHistory().load(context,track);assertEquals(0,result.count());assertTrue(result.points().isEmpty());
    }
}
