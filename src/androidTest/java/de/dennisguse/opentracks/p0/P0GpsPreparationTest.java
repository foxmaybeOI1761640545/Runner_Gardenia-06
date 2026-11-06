package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;
import androidx.test.rule.GrantPermissionRule;
import org.junit.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.data.*;
import de.dennisguse.opentracks.p0.gps.GpsPreparation;
import de.dennisguse.opentracks.p0.recording.RecordingSession;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.sensors.driver.*;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

public class P0GpsPreparationTest {
    @Rule public final ServiceTestRule rule=ServiceTestRule.withTimeout(10,TimeUnit.SECONDS);
    @Rule public final GrantPermissionRule permissions=GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.POST_NOTIFICATIONS);
    private final Context context=ApplicationProvider.getApplicationContext();
    private TrackRecordingService service;
    private String gps,heart;
    private long begin;
    private Object originalAccuracy;
    private void main(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    @Before public void before() throws Exception {
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());
        var prefs=androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        String accuracyKey=context.getString(R.string.recording_gps_accuracy_key);
        originalAccuracy=prefs.getAll().get(accuracyKey);
        gps=PreferencesUtils.getString(R.string.settings_sensor_gps_key,SensorType.INTERNAL.getPreferenceValue());
        heart=PreferencesUtils.getString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,SensorType.NONE.getPreferenceValue());
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
        service=((TrackRecordingService.Binder)rule.bindService(new Intent(context,TrackRecordingService.class))).getService();
        main(()->{service.beginGpsAcquisition();fixture(service);});begin=SystemClock.elapsedRealtimeNanos();
    }
    @After public void after() {
        if(service!=null)main(()->{service.cancelGpsAcquisition();service.endCurrentTrack();service.stopIdleSensors();});
        TestEnvironmentUtils.stopTrackRecordingService();new SessionStore(context).save(RecordingSession.ready());
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,gps);PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,heart);
        var editor=androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).edit();
        String accuracyKey=context.getString(R.string.recording_gps_accuracy_key);
        if(originalAccuracy instanceof String value)editor.putString(accuracyKey,value);
        else if(originalAccuracy instanceof Integer value)editor.putInt(accuracyKey,value);
        else if(originalAccuracy==null)editor.remove(accuracyKey);
        else throw new AssertionError("Unexpected original accuracy preference type: "+originalAccuracy.getClass());
        assertTrue(editor.commit());
        assertEquals(originalAccuracy,androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).getAll().get(accuracyKey));
    }
    public static void fixture(TrackRecordingService core) {
        var handler=core.getTrackPointCreator().getSensorManager().getGpsHandler();
        if(handler.driver!=null)handler.driver.disconnect();
        handler.driver=new Driver(){
            private boolean connected=true;
            public void connect(Context c,android.os.Handler h,String a){}
            public boolean isConnected(){return connected;}
            public void updateDriverParameters(Context c,android.os.Handler h){}
            public void disconnect(){connected=false;handler.onDisconnected();}
        };
        handler.onConnected(null,"Synthetic fresh acquisition GPS");
    }
    private void fix(double latitude,float accuracy,long elapsed) {
        main(()->{
            Location location=new Location("gps");location.setLatitude(latitude);location.setLongitude(103.8);
            location.setAccuracy(accuracy);location.setTime(System.currentTimeMillis());location.setElapsedRealtimeNanos(elapsed);
            service.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(location);
        });
    }
    public static void ready(TrackRecordingService core) {
        for(int i=0;i<3;i++){
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
                Location f=new Location("gps");f.setLatitude(1.3);f.setLongitude(103.8);f.setAccuracy(2);f.setTime(System.currentTimeMillis());f.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                core.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(f);
            });
            if(i<2)SystemClock.sleep(550);
        }
    }
    private void noWorkout(){assertFalse(service.isRecording());assertNull(TrackRecordingService.activeTrackId(context));assertTrue(new WorkoutRepository(context).list().isEmpty());}
    @Test public void waitingCreatesNoTrackTimerDistanceOrPoints(){SystemClock.sleep(1100);noWorkout();assertTrue(service.isAcquiringGps());}
    @Test public void cachedPositionCannotStart(){for(int i=0;i<3;i++)fix(1.3,2,begin-1_000_000_000L);noWorkout();}
    @Test public void expiredPositionCannotStart(){for(int i=0;i<3;i++)fix(1.3,2,SystemClock.elapsedRealtimeNanos()-6_000_000_000L);noWorkout();}
    @Test public void inaccuratePositionsCannotStart(){for(int i=0;i<3;i++)fix(1.3,31,SystemClock.elapsedRealtimeNanos());noWorkout();}
    @Test public void invalidCoordinatesAndSingleFixCannotStart(){fix(91,2,SystemClock.elapsedRealtimeNanos());fix(1.3,2,SystemClock.elapsedRealtimeNanos());noWorkout();}
    @Test public void validStableFixesStartAtConfirmationAndSeedOnlyLatestPoint(){
        SystemClock.sleep(1100);noWorkout();Instant beforeConfirm=service.getTrackPointCreator().getNow();ready(service);
        assertTrue(service.isRecording());assertFalse(service.isAcquiringGps());
        var id=service.getRecordingStatus().trackId();var repository=new WorkoutRepository(context);var track=repository.find(id).track();
        assertFalse(track.statistics().startTime().isBefore(beforeConfirm));assertEquals(0,track.statistics().totalDistance().toM(),0);
        ArrayList<TrackPoint> points=new ArrayList<>();repository.forEachPoint(id,points::add);
        assertEquals(1,points.stream().filter(p->p.position().hasLocation()).count());
        assertTrue(points.stream().filter(p->p.position().hasLocation()).allMatch(p->!p.getTime().isBefore(track.statistics().startTime())));
    }
    @Test public void cancellationReleasesIdleResourcesWithoutEmptyWorkout(){main(service::cancelGpsAcquisition);assertFalse(service.isAcquiringGps());assertFalse(service.areSensorsStarted());noWorkout();assertEquals(GpsPreparation.State.IDLE,service.getGpsPreparation().state());}
    @Test public void timeoutNeverStartsAndContinueKeepsWaiting() throws Exception {
        var field=TrackRecordingService.class.getDeclaredField("gpsPreparationTimeout");field.setAccessible(true);Runnable timeout=(Runnable)field.get(service);main(timeout);
        assertEquals(GpsPreparation.State.TIMED_OUT,service.getGpsPreparation().state());noWorkout();
        main(service::continueGpsAcquisition);assertEquals(GpsPreparation.State.ACQUIRING,service.getGpsPreparation().state());noWorkout();ready(service);assertTrue(service.isRecording());
    }
    @Test public void heartPreviewBeforeConfirmationIsNeverAssociatedWithWorkout(){
        main(()->{
            var handler=service.getTrackPointCreator().getSensorManager().getHeartRateHandler();handler.onConnected("AA:BB:CC:DD:EE:FF","Synthetic strap");
            handler.onConnectionStateChanged(BleConnectionSession.State.CONNECTED);
            handler.onDataReceived(new HeartRate(78,new HeartRateSample(null,null,null,true,12,List.of(1024))));
        });
        noWorkout();assertNotNull(service.getHeartRatePreview().value());ready(service);
        assertEquals(0,new WorkoutRepository(context).heartRateStatistics(service.getRecordingStatus().trackId()).count());
    }
    @Test public void lostGpsDoesNotPauseOrEndAndNewFixResumesWithoutInventedPoints(){
        ready(service);Track.Id id=service.getRecordingStatus().trackId();var repository=new WorkoutRepository(context);
        ArrayList<TrackPoint> before=new ArrayList<>();repository.forEachPoint(id,before::add);
        long count=before.stream().filter(p->p.position().hasLocation()).count();
        SystemClock.sleep(5400);assertTrue(service.isRecording());assertFalse(service.hasFreshGpsPreview());
        ArrayList<TrackPoint> lost=new ArrayList<>();repository.forEachPoint(id,lost::add);assertEquals(count,lost.stream().filter(p->p.position().hasLocation()).count());
        fix(1.30005,2,SystemClock.elapsedRealtimeNanos());assertTrue(service.isRecording());assertEquals(id,service.getRecordingStatus().trackId());assertTrue(service.hasFreshGpsPreview());
        main(service::endCurrentTrack);assertTrue(repository.find(id).track().statistics().totalDuration().toMillis()>=5000);
    }
    @Test public void foregroundRecordingKeepsSessionWhileEmulatedScreenIsOff() throws Exception {
        ready(service);Track.Id id=service.getRecordingStatus().trackId();
        var automation=InstrumentationRegistry.getInstrumentation().getUiAutomation();
        try {
            try(var stream=new android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("input keyevent KEYCODE_SLEEP"))) {while(stream.read()!=-1){}}
            SystemClock.sleep(1200);assertTrue(service.isRecording());assertEquals(id,service.getRecordingStatus().trackId());
            fix(1.30005,2,SystemClock.elapsedRealtimeNanos());assertTrue(service.hasFreshGpsPreview());
        } finally {
            try(var stream=new android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("input keyevent KEYCODE_WAKEUP"))) {while(stream.read()!=-1){}}
            try(var stream=new android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("wm dismiss-keyguard"))) {while(stream.read()!=-1){}}
        }
    }

    @Test public void fixtureDatabaseCleanupCancelsPreparationBeforeDeletingRecords() {
        assertTrue(service.isAcquiringGps());
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();
        assertFalse(service.isAcquiringGps());assertFalse(service.isRecording());assertFalse(service.areSensorsStarted());
        SystemClock.sleep(1200);noWorkout();
    }

    @Test public void preparationAlsoHonorsTheActualRecordersStricterAccuracyFilter() {
        main(service::cancelGpsAcquisition);
        assertTrue(androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(context.getString(R.string.recording_gps_accuracy_key),1).commit());
        main(()->{service.beginGpsAcquisition();fixture(service);});
        ready(service);noWorkout(); // 2m would satisfy readiness's 30m ceiling, but not this recorder's 1m filter.
        for(int i=0;i<3;i++){fix(1.3,0.5f,SystemClock.elapsedRealtimeNanos());if(i<2)SystemClock.sleep(550);}
        assertTrue(service.isRecording());
        ArrayList<TrackPoint> points=new ArrayList<>();new WorkoutRepository(context).forEachPoint(service.getRecordingStatus().trackId(),points::add);
        assertEquals(1,points.stream().filter(point->point.position().hasLocation()).count());
    }

}
