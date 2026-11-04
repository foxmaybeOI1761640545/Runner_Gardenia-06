package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import de.dennisguse.opentracks.data.*;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.data.WorkoutRepository;

public class P0WorkoutRepositoryTest {
    private final Context context=ApplicationProvider.getApplicationContext();
    private ContentProviderUtils store;
    private WorkoutRepository repository;
    @Before public void prepare() {store=new ContentProviderUtils(context);store.deleteAllTracks(context);repository=new WorkoutRepository(context);}
    private Track.Id track(String name) {return store.insertTrack(new Track(null,UUID.randomUUID(),name,"","",ActivityType.RUNNING,ZoneOffset.UTC,Statistics.DEFAULT));}
    private HeartRate sample(float bpm, long time) {return new HeartRate(bpm,new HeartRateSample(Instant.EPOCH.plusSeconds(time),"AA:BB:CC:DD:EE:FF","Test strap",true,5,List.of(1024,1000)));}
    @Test public void snapshotsPreserveRawDataAndStatisticsAcrossRepositoryReopen() {
        Track.Id id=track("P0 local");HeartRate a=sample(70,1),b=sample(70,2),c=sample(100,3);
        HeartRateRepository raw=new HeartRateRepository(context);raw.insert(id,a);raw.insert(id,a);raw.insert(id,b);raw.insert(id,c);
        var reopened=new WorkoutRepository(context);var summary=reopened.find(id);
        assertEquals("P0 local",summary.track().name());assertEquals(3,summary.heartRate().count());
        assertEquals(80,summary.heartRate().average(),0);assertEquals(70,summary.heartRate().minimum(),0);assertEquals(100,summary.heartRate().maximum(),0);
        assertEquals(List.of(a,b,c),reopened.heartRates(id));assertEquals(List.of(1024,1000),reopened.heartRates(id).get(0).sample().rrIntervals1024());
    }
    @Test public void deletionCascadesGpsAndHeartRateOnlyForSelectedWorkout() {
        Track.Id a=track("A"),b=track("B");var raw=new HeartRateRepository(context);
        raw.insert(a,sample(60,1));raw.insert(b,sample(80,2));
        store.insertTrackPoint(new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL,Instant.EPOCH),a);
        repository.delete(a);assertNull(repository.find(a));assertTrue(repository.heartRates(a).isEmpty());assertNotNull(repository.find(b));
        final int[] points={0};repository.forEachPoint(a,p->points[0]++);assertEquals(0,points[0]);assertEquals(1,repository.heartRates(b).size());
    }
    @Test public void noSensorWorkoutsRemainReadableWithoutSyntheticStatistics() {
        var id=track("GPS only");assertEquals(0,repository.find(id).heartRate().count());assertTrue(repository.heartRates(id).isEmpty());assertEquals(1,repository.list().size());
    }
    @Test public void chronologicalStreamingRetainsEqualBpmAndIndependentCaptureTimes() {
        var id=track("Ordered samples");var raw=new HeartRateRepository(context);
        HeartRate late=sample(72,3),early=sample(72,1);raw.insert(id,late);raw.insert(id,early);
        java.util.ArrayList<HeartRate> actual=new java.util.ArrayList<>();repository.forEachHeartRate(id,actual::add);
        assertEquals(List.of(early,late),actual);
    }
    @Test public void failedRawBatchRollsBackEveryRow() {
        var id=track("Atomic batch");ContentValues valid=new ContentValues();valid.put("trackid",id.id());valid.put("time",0);valid.put("sample_key","batch");valid.put("bpm",70);valid.put("metadata","{}");
        ContentValues invalid=new ContentValues(valid);invalid.put("trackid",-999999999L);invalid.put("sample_key","invalid");
        try {context.getContentResolver().bulkInsert(CustomContentProvider.UrlType.HEART_RATE_SAMPLES.getUri(),new ContentValues[]{valid,invalid});fail("Invalid batch must fail");}
        catch(SQLiteException expected) {assertEquals(0,repository.find(id).heartRate().count());}
    }
    @Test public void legacyHeartRateRemainsReadableWithoutInventingRawNotifications() {
        var id=track("Legacy heart rate");
        for(int bpm:new int[]{80,100})store.insertTrackPoint(new TrackPoint(null,TrackPoint.Type.TRACKPOINT,Position.of(Instant.EPOCH.plusSeconds(bpm)),null,HeartRate.of(bpm),null,null,null,null),id);
        var old=repository.find(id);assertEquals(de.dennisguse.opentracks.p0.data.WorkoutSummary.HeartRateOrigin.LEGACY_TRACK_POINTS,old.heartRateOrigin());
        assertEquals(90,old.heartRate().average(),0);assertTrue(repository.heartRates(id).isEmpty());
        new HeartRateRepository(context).insert(id,sample(70,1));
        var raw=repository.find(id);assertEquals(de.dennisguse.opentracks.p0.data.WorkoutSummary.HeartRateOrigin.RAW_NOTIFICATIONS,raw.heartRateOrigin());assertEquals(70,raw.heartRate().average(),0);
    }
    @Test public void boundedHistoryAndPausedWorkoutDeletionProtection() {
        var first=track("First"),second=track("Second"),third=track("Third");
        assertEquals(List.of(third,second),repository.listNewest(2).stream().map(row->row.track().id()).toList());
        var journal=new de.dennisguse.opentracks.p0.data.SessionStore(context);
        journal.save(de.dennisguse.opentracks.p0.recording.RecordingSession.ready().begin(first.id()).pause());
        try {assertTrue(repository.isProtected(first));repository.delete(first);fail("Paused workout must be retained");}
        catch(IllegalStateException expected){assertNotNull(repository.find(first));}
        finally{journal.save(de.dennisguse.opentracks.p0.recording.RecordingSession.ready());}
        repository.delete(first);assertNull(repository.find(first));assertEquals(2,repository.list().size());
    }
}
