package de.dennisguse.opentracks.services;

import static org.junit.Assert.*;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.SystemClock;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.junit.*;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.P0GpsPreparationTest;
import de.dennisguse.opentracks.p0.app.P0Activity;
import de.dennisguse.opentracks.p0.ble.HeartRateCommands;
import de.dennisguse.opentracks.p0.data.*;
import de.dennisguse.opentracks.p0.recording.*;
import de.dennisguse.opentracks.sensors.GpsHandler;
import de.dennisguse.opentracks.sensors.driver.GpsInternal;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** Real P0 UI commands/service lifetimes, deterministic data through the existing sensor handlers. */
public class P07GpsHandoffTest {
    @Rule public final GrantPermissionRule permissions=TestUtil.createScanPermissionRule();
    @Rule public final ServiceTestRule rule=new ServiceTestRule();
    private final Context context=ApplicationProvider.getApplicationContext();
    private final Map<String,Object> original=new HashMap<>();
    private TrackRecordingService service;
    private P0RecordingViewModel model;
    private void main(Runnable r){InstrumentationRegistry.getInstrumentation().runOnMainSync(r);}
    private void await(BooleanSupplier condition){
        long end=SystemClock.uptimeMillis()+7000;boolean[] done={false};
        do{main(()->done[0]=condition.getAsBoolean());if(done[0])return;SystemClock.sleep(25);}while(SystemClock.uptimeMillis()<end);
        fail("P0/service handoff did not reach the required state");
    }
    private void preferences(){
        assertEquals("NONE",PreferencesUtils.getString(R.string.settings_sensor_gps_key,""));
        assertEquals("7",PreferencesUtils.getString(R.string.min_sampling_interval_key,""));
        assertEquals("17",PreferencesUtils.getString(R.string.recording_distance_interval_key,""));
    }
    @Before public void before() throws Exception {
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());
        var prefs=androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        for(int key:new int[]{R.string.settings_sensor_gps_key,R.string.min_sampling_interval_key,R.string.recording_distance_interval_key,
                R.string.settings_sensor_bluetooth_heart_rate_key,R.string.p0_hr_connection_enabled_key})original.put(context.getString(key),prefs.getAll().get(context.getString(key)));
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,"NONE");PreferencesUtils.setString(R.string.min_sampling_interval_key,"7");
        PreferencesUtils.setString(R.string.recording_distance_interval_key,"17");PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,"NONE");
        HeartRateCommands.setConnectionEnabled(context,false);
        service=((TrackRecordingService.Binder)rule.bindService(new Intent(context,TrackRecordingService.class))).getService();
    }
    @After public void after() throws Exception {
        if(service!=null)main(()->{service.cancelGpsAcquisition();service.endCurrentTrack();service.stopIdleSensors();});
        rule.unbindService();TestEnvironmentUtils.stopTrackRecordingService();TestEnvironmentUtils.awaitTrackRecordingServiceDestroyed();
        new SessionStore(context).save(RecordingSession.ready());
        var editor=androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).edit();
        original.forEach((key,value)->{if(value==null)editor.remove(key);else if(value instanceof String v)editor.putString(key,v);
            else if(value instanceof Integer v)editor.putInt(key,v);else if(value instanceof Boolean v)editor.putBoolean(key,v);else throw new AssertionError("Unexpected preference type");});
        assertTrue(editor.commit());
    }
    private void home(ActivityScenario<P0Activity> screen){
        await(()->service.hasForegroundGpsRequest()&&service.areSensorsStarted());
        screen.onActivity(a->model=new ViewModelProvider(a).get(P0RecordingViewModel.class));
        main(()->{
            var actual=(GpsInternal)service.getTrackPointCreator().getSensorManager().getGpsHandler().driver;
            assertEquals(1000,actual.createLocationRequest().getIntervalMillis());
            P0GpsPreparationTest.fixture(service);
        });
        fix(1.3);preferences();
    }
    private GpsHandler gps(){return service.getTrackPointCreator().getSensorManager().getGpsHandler();}
    private void fix(double latitude){
        main(()->{
            Location f=new Location("gps");f.setLatitude(latitude);f.setLongitude(103.8);f.setAccuracy(2);f.setSpeed(2);
            f.setTime(System.currentTimeMillis());f.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());gps().onDataReceived(f);
        });
    }
    private Track.Id start(){
        main(()->model.start(ActivityType.RUNNING));await(service::isAcquiringGps);
        assertTrue(new WorkoutRepository(context).list().isEmpty());
        P0GpsPreparationTest.ready(service);await(service::isRecording);preferences();
        return service.getRecordingStatus().trackId();
    }
    private List<TrackPoint> points(Track.Id id){var rows=new ArrayList<TrackPoint>();new WorkoutRepository(context).forEachPoint(id,rows::add);return rows;}
    private void finish(){main(model::finish);await(()->!service.isRecording()&&model.snapshot().getValue().session().phase()==RecordingSession.Phase.FINISHED);preferences();}

    @Test public void warmPreviewPreparationAndRecordingUseOneHandlerAndNeverWriteGpsPreferences(){
        try(var screen=ActivityScenario.launch(P0Activity.class)){
            home(screen);var handler=gps();var driver=handler.driver;
            Track.Id id=start();assertNotNull(id);assertSame(handler,gps());assertSame(driver,gps().driver);
            main(()->{assertEquals(0,service.mapGpsOwnerCount());assertTrue(service.hasForegroundGpsRequest());
                assertTrue(TrackIdStorage.isP0GpsSession(context));assertEquals(id,TrackRecordingService.activeTrackId(context));});
            assertEquals(1,new WorkoutRepository(context).list().size());finish();
            assertSame(handler,gps());assertSame(driver,gps().driver);assertFalse(TrackIdStorage.isP0GpsSession(context));
        }
    }
    @Test public void onlyPostConfirmationGpsAndHeartSamplesEnterTheWorkout(){
        try(var screen=ActivityScenario.launch(P0Activity.class)){
            home(screen);
            main(()->{var heart=service.getTrackPointCreator().getSensorManager().getHeartRateHandler();
                heart.onConnected("AA:BB:CC:DD:EE:FF","Synthetic strap");heart.onDataReceived(new HeartRate(79,new HeartRateSample(null,null,null,true,12,List.of(1024))));});
            assertTrue(new WorkoutRepository(context).list().isEmpty());
            var before=service.getTrackPointCreator().getNow();Track.Id id=start();
            var workout=new WorkoutRepository(context).find(id).track();assertFalse(workout.statistics().startTime().isBefore(before));
            var locations=points(id).stream().filter(p->p.position().hasLocation()).toList();assertEquals(1,locations.size());
            assertTrue(locations.stream().allMatch(p->!p.getTime().isBefore(workout.statistics().startTime())));
            assertEquals(0,new WorkoutRepository(context).heartRateStatistics(id).count());finish();
        }
    }
    @Test public void finishResetsPreviewAnchorAndNotificationWithoutCreatingOrExtendingAWorkout(){
        try(var screen=ActivityScenario.launch(P0Activity.class)){
            home(screen);Track.Id id=start();var handler=gps();var driver=handler.driver;
            for(int i=0;i<3;i++){SystemClock.sleep(550);fix(1.31);}
            assertEquals(1.31,service.getGpsPreview().latitude(),.000001);finish();
            int count=points(id).size();var saved=new WorkoutRepository(context).find(id).track().statistics();
            fix(1.310001);assertEquals(1.310001,service.getGpsPreview().latitude(),.000001);
            assertSame(handler,gps());assertSame(driver,gps().driver);assertEquals(count,points(id).size());
            assertEquals(1,new WorkoutRepository(context).list().size());assertNull(TrackRecordingService.activeTrackId(context));
            assertEquals(saved.totalDuration(),new WorkoutRepository(context).find(id).track().statistics().totalDuration());
            var notifications=context.getSystemService(NotificationManager.class).getActiveNotifications();
            var note=java.util.Arrays.stream(notifications).filter(n->n.getId()==TrackRecordingServiceNotificationManager.NOTIFICATION_ID).findFirst().orElseThrow().getNotification();
            assertEquals(context.getString(R.string.p0_title),note.extras.getString(Notification.EXTRA_TITLE));
            assertEquals(context.getString(R.string.p07_gps_preview_notification),note.extras.getString(Notification.EXTRA_TEXT));
            assertTrue((note.flags&Notification.FLAG_ONGOING_EVENT)!=0);
        }
    }
    @Test public void pausedRelocationAndP0ResumeKeepTheSameTrackAndCannotBridgeDistance(){
        try(var screen=ActivityScenario.launch(P0Activity.class)){
            home(screen);Track.Id id=start();var driver=gps().driver;
            main(model::pause);await(()->!service.isRecording()&&model.snapshot().getValue().session().phase()==RecordingSession.Phase.PAUSED);
            for(int i=0;i<3;i++){SystemClock.sleep(550);fix(1.31);}preferences();
            double before=new WorkoutRepository(context).find(id).track().statistics().totalDistance().toM();
            main(model::resume);await(service::isRecording);assertEquals(id,service.getRecordingStatus().trackId());assertSame(driver,gps().driver);
            var note=java.util.Arrays.stream(context.getSystemService(NotificationManager.class).getActiveNotifications())
                    .filter(n->n.getId()==TrackRecordingServiceNotificationManager.NOTIFICATION_ID).findFirst().orElseThrow().getNotification();
            assertEquals(context.getString(R.string.p0_recording),note.extras.getString(Notification.EXTRA_TEXT));
            long oldLocations=points(id).stream().filter(p->p.position().hasLocation()).count();
            main(()->{var heart=service.getTrackPointCreator().getSensorManager().getHeartRateHandler();
                heart.onConnected("AA:BB:CC:DD:EE:FF","Synthetic resumed strap");heart.onDataReceived(new HeartRate(81));});
            assertEquals(oldLocations,points(id).stream().filter(p->p.position().hasLocation()).count());
            SystemClock.sleep(550);fix(1.31);finish();
            assertTrue(new WorkoutRepository(context).find(id).track().statistics().totalDistance().toM()-before<10);
            assertTrue(points(id).stream().filter(p->p.type()==TrackPoint.Type.SEGMENT_START_MANUAL).count()>=2);
            assertEquals(1,new WorkoutRepository(context).list().size());
        }
    }
    @Test public void actualP0RecordingSurvivesRotationAppBackgroundAndScreenOff() throws Exception {
        try(var screen=ActivityScenario.launch(P0Activity.class)){
            home(screen);Track.Id id=start();var handler=gps();var driver=handler.driver;
            screen.recreate();await(service::isRecording);assertSame(handler,gps());assertSame(driver,gps().driver);
            screen.moveToState(Lifecycle.State.CREATED);await(()->!service.hasForegroundGpsRequest());assertTrue(service.isRecording());
            var automation=InstrumentationRegistry.getInstrumentation().getUiAutomation();
            try{
                try(var in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("input keyevent KEYCODE_SLEEP"))){while(in.read()!=-1){}}
                SystemClock.sleep(1100);fix(1.30001);assertTrue(service.hasFreshGpsPreview());assertEquals(id,service.getRecordingStatus().trackId());
                assertSame(driver,gps().driver);preferences();
            }finally{
                try(var in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("input keyevent KEYCODE_WAKEUP"))){while(in.read()!=-1){}}
                try(var in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("wm dismiss-keyguard"))){while(in.read()!=-1){}}
            }
            screen.moveToState(Lifecycle.State.RESUMED);await(service::hasForegroundGpsRequest);finish();
        }
    }
    @Test public void newServiceLifetimeRestoresTheP0ModeAndSameWorkoutFromItsJournal() throws Exception {
        Track.Id id;
        try(var screen=ActivityScenario.launch(P0Activity.class)){home(screen);id=start();screen.moveToState(Lifecycle.State.CREATED);await(()->!service.hasForegroundGpsRequest());}
        var previous=service;rule.unbindService();TestEnvironmentUtils.stopTrackRecordingService();TestEnvironmentUtils.awaitTrackRecordingServiceDestroyed();
        assertEquals(id,TrackRecordingService.activeTrackId(context));assertTrue(TrackIdStorage.isP0GpsSession(context));
        service=((TrackRecordingService.Binder)rule.bindService(new Intent(context,TrackRecordingService.class))).getService();assertNotSame(previous,service);
        main(()->{
            assertEquals(android.app.Service.START_STICKY,service.onStartCommand(null,0,1));
            assertTrue(service.isRecording());assertEquals(id,service.getRecordingStatus().trackId());
            var actual=(GpsInternal)gps().driver;assertEquals(1000,actual.createLocationRequest().getIntervalMillis());preferences();
            service.endCurrentTrack();assertNull(TrackRecordingService.activeTrackId(context));assertFalse(TrackIdStorage.isP0GpsSession(context));
        });
        assertEquals(1,new WorkoutRepository(context).list().size());
    }
}
