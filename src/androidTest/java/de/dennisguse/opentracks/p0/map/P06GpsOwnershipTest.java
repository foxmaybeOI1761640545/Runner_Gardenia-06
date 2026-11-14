package de.dennisguse.opentracks.p0.map;

import static org.junit.Assert.*;
import android.content.*;
import android.location.Location;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.*;
import org.junit.*;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.p0.P0GpsPreparationTest;
import de.dennisguse.opentracks.p0.data.*;
import de.dennisguse.opentracks.p0.recording.*;
import de.dennisguse.opentracks.services.*;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** Exercise the same service/handler used by recording; a map never owns the workout. */
public class P06GpsOwnershipTest {
    @Rule public final GrantPermissionRule permissions=TestUtil.createScanPermissionRule();
    @Rule public final ServiceTestRule rule=new ServiceTestRule();
    private final Context context=ApplicationProvider.getApplicationContext();
    private TrackRecordingService service;
    private final Object first=new Object(),second=new Object();
    private String gps,heart;private boolean hrEnabled;
    private void main(Runnable r){InstrumentationRegistry.getInstrumentation().runOnMainSync(r);}
    @Before public void before()throws Exception{
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());
        gps=PreferencesUtils.getString(R.string.settings_sensor_gps_key,"INTERNAL");heart=PreferencesUtils.getString(R.string.settings_sensor_bluetooth_heart_rate_key,"NONE");
        hrEnabled=de.dennisguse.opentracks.p0.ble.HeartRateCommands.connectionEnabled(context);
        de.dennisguse.opentracks.p0.ble.HeartRateCommands.setConnectionEnabled(context,false);
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,"NONE");PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,"NONE");
        service=((TrackRecordingService.Binder)rule.bindService(new Intent(context,TrackRecordingService.class))).getService();
    }
    @After public void after(){
        main(()->{service.releaseMapGps(first);service.releaseMapGps(second);service.cancelGpsAcquisition();service.endCurrentTrack();service.stopIdleSensors();});
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,gps);PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,heart);de.dennisguse.opentracks.p0.ble.HeartRateCommands.setConnectionEnabled(context,hrEnabled);new SessionStore(context).save(RecordingSession.ready());
    }
    private void fix(boolean timed){main(()->{Location f=new Location("gps");f.setLatitude(1.3);f.setLongitude(103.8);f.setAccuracy(2);f.setTime(System.currentTimeMillis());if(timed)f.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());service.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(f);});}
    private void relocatedFix(){main(()->{Location f=new Location("gps");f.setLatitude(1.31);f.setLongitude(103.8);f.setAccuracy(2);f.setSpeed(0);f.setTime(System.currentTimeMillis());f.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());service.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(f);});}
    @Test public void multipleVisibleMapsReleaseOnlyTheirOwnGpsAndNeverCreateWorkout(){
        main(()->{service.acquireMapGps(first);service.acquireMapGps(second);P0GpsPreparationTest.fixture(service);});fix(true);
        assertTrue(service.hasFreshGpsPreview());assertFalse(service.isAcquiringGps());assertFalse(service.isRecording());assertTrue(new WorkoutRepository(context).list().isEmpty());
        main(()->service.releaseMapGps(first));assertTrue(service.areSensorsStarted());
        main(()->service.releaseMapGps(second));assertFalse(service.areSensorsStarted());assertTrue(new WorkoutRepository(context).list().isEmpty());
    }
    @Test public void closingMapCannotStopExistingWorkout(){
        main(()->{service.startNewTrack();service.acquireMapGps(first);P0GpsPreparationTest.fixture(service);});fix(true);var id=service.getRecordingStatus().trackId();
        main(()->service.releaseMapGps(first));assertTrue(service.isRecording());assertTrue(service.areSensorsStarted());assertEquals(id,service.getRecordingStatus().trackId());
    }
    @Test public void defaultHeartRatePreferenceCannotKeepMapOnlySensorsAlive(){
        de.dennisguse.opentracks.p0.ble.HeartRateCommands.setConnectionEnabled(context,true);
        main(()->service.acquireMapGps(first));assertTrue(service.areSensorsStarted());
        main(()->service.releaseMapGps(first));assertFalse(service.areSensorsStarted());assertFalse(service.isRecording());
    }
    @Test public void pausedWorkoutKeepsVisibleMapGpsAndResumeRetainsSameTrack(){
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,"INTERNAL");
        main(()->{service.acquireMapGps(first);service.startNewTrack();P0GpsPreparationTest.fixture(service);});fix(true);var id=service.getRecordingStatus().trackId();
        main(service::endCurrentTrack);assertFalse(service.isRecording());assertTrue(service.areSensorsStarted());
        relocatedFix();SystemClock.sleep(50);relocatedFix();
        var old=new de.dennisguse.opentracks.data.ContentProviderUtils(context).getTrack(id).statistics().totalDistance().toM();
        main(()->service.resumeTrack(id));assertEquals(id,service.getRecordingStatus().trackId());
        main(()->assertFalse("Paused GPS preview must not leak into a resumed BLE-only update",service.getTrackPointCreator().getSensorManager().getSensorDataSet(service.getTrackPointCreator().getNow()).position().data().hasLocation()));
        relocatedFix();
        java.util.concurrent.atomic.AtomicReference<de.dennisguse.opentracks.data.models.Position> last=new java.util.concurrent.atomic.AtomicReference<>();
        new WorkoutRepository(context).forEachPoint(id,p->{if(p.position().hasLocation())last.set(p.position());});
        assertNotNull(last.get());assertEquals("First resumed fix must not be rejected against the pre-pause anchor",1.31,last.get().latitude(),.000001);
        assertEquals("A paused relocation must never add a distance bridge",old,new de.dennisguse.opentracks.data.ContentProviderUtils(context).getTrack(id).statistics().totalDistance().toM(),.001);
        main(()->service.releaseMapGps(first));assertTrue(service.isRecording());
    }
    @Test public void mapPreviewRejectsUntimedFixAndKeepsRecordingPreparationIndependent(){
        main(()->{service.acquireMapGps(first);P0GpsPreparationTest.fixture(service);});fix(false);assertFalse(service.hasFreshGpsPreview());assertFalse(service.isRecording());
        main(service::beginGpsAcquisition);assertTrue(service.isAcquiringGps());main(service::cancelGpsAcquisition);assertTrue(service.areSensorsStarted());fix(true);assertTrue(service.hasFreshGpsPreview());assertTrue(new WorkoutRepository(context).list().isEmpty());
    }
}
