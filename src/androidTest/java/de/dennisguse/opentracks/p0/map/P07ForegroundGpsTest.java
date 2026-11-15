package de.dennisguse.opentracks.p0.map;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.SystemClock;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;
import java.util.function.BooleanSupplier;
import org.junit.*;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.p0.P0GpsPreparationTest;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.p0.ble.HeartRateCommands;
import de.dennisguse.opentracks.p0.data.*;
import de.dennisguse.opentracks.p0.recording.RecordingSession;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.sensors.GpsStatusValue;
import de.dennisguse.opentracks.sensors.driver.GpsInternal;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** Real Application callbacks and service/driver; synthetic measurements, no alternate GPS system. */
public class P07ForegroundGpsTest {
    @Rule public final GrantPermissionRule permissions=TestUtil.createScanPermissionRule();
    @Rule public final ServiceTestRule rule=new ServiceTestRule();
    private final Context context=ApplicationProvider.getApplicationContext();
    private TrackRecordingService service;
    private String gps,interval,heart;
    private boolean hrEnabled;
    private void main(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    private void await(BooleanSupplier condition){
        long until=SystemClock.uptimeMillis()+5000;boolean[] ready={false};
        do{main(()->ready[0]=condition.getAsBoolean());if(ready[0])return;SystemClock.sleep(25);}while(SystemClock.uptimeMillis()<until);
        fail("Application/service lifecycle did not reach the required state");
    }
    private void foreground(){await(()->service.hasForegroundGpsRequest()&&service.areSensorsStarted());}
    private void background(){await(()->!service.hasForegroundGpsRequest()&&!service.areSensorsStarted());}
    private void preferences(){assertEquals("NONE",PreferencesUtils.getString(R.string.settings_sensor_gps_key,""));assertEquals("7",PreferencesUtils.getString(R.string.min_sampling_interval_key,""));}
    @Before public void before()throws Exception{
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());
        gps=PreferencesUtils.getString(R.string.settings_sensor_gps_key,"INTERNAL");interval=PreferencesUtils.getString(R.string.min_sampling_interval_key,"1");
        heart=PreferencesUtils.getString(R.string.settings_sensor_bluetooth_heart_rate_key,"NONE");hrEnabled=HeartRateCommands.connectionEnabled(context);
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,"NONE");PreferencesUtils.setString(R.string.min_sampling_interval_key,"7");
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,"NONE");HeartRateCommands.setConnectionEnabled(context,false);
        service=((TrackRecordingService.Binder)rule.bindService(new Intent(context,TrackRecordingService.class))).getService();
    }
    @After public void after(){
        main(()->{service.cancelGpsAcquisition();service.endCurrentTrack();service.stopIdleSensors();});
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,gps);PreferencesUtils.setString(R.string.min_sampling_interval_key,interval);
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,heart);HeartRateCommands.setConnectionEnabled(context,hrEnabled);
        new SessionStore(context).save(RecordingSession.ready());
    }
    private void fix(double latitude,long elapsed,float accuracy){main(()->{
        Location value=new Location("gps");value.setLatitude(latitude);value.setLongitude(103.8);value.setAccuracy(accuracy);
        value.setTime(System.currentTimeMillis());value.setElapsedRealtimeNanos(elapsed);
        service.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(value);
    });}
    private void fix(double latitude){fix(latitude,SystemClock.elapsedRealtimeNanos(),2);}
    @Test public void everyPageAndRecreationShareOneDriverAndNeverWriteHistoryOrGpsPreferences(){
        try(var home=ActivityScenario.launch(P0Activity.class)){
            foreground();Object[] handler={null},driver={null};
            main(()->{handler[0]=service.getTrackPointCreator().getSensorManager().getGpsHandler();driver[0]=service.getTrackPointCreator().getSensorManager().getGpsHandler().driver;
                assertTrue(driver[0] instanceof GpsInternal);assertEquals(1000,((GpsInternal)driver[0]).createLocationRequest().getIntervalMillis());assertEquals(0,service.mapGpsOwnerCount());});
            try(var settings=ActivityScenario.launch(P0SettingsActivity.class)){
                foreground();settings.recreate();foreground();
                main(()->{assertSame(handler[0],service.getTrackPointCreator().getSensorManager().getGpsHandler());assertSame(driver[0],service.getTrackPointCreator().getSensorManager().getGpsHandler().driver);});
                try(var history=ActivityScenario.launch(P0HistoryActivity.class)){
                    foreground();try(var maps=ActivityScenario.launch(P06MapActivity.class)){
                        foreground();main(()->{assertSame(driver[0],service.getTrackPointCreator().getSensorManager().getGpsHandler().driver);assertEquals(0,service.mapGpsOwnerCount());});
                    }
                }
            }
            home.recreate();foreground();main(()->assertSame(driver[0],service.getTrackPointCreator().getSensorManager().getGpsHandler().driver));
            preferences();assertTrue(new WorkoutRepository(context).list().isEmpty());assertNull(TrackRecordingService.activeTrackId(context));
            main(()->{assertFalse(service.isRecording());assertFalse(service.isAcquiringGps());});
            home.moveToState(Lifecycle.State.CREATED);background();preferences();
            home.moveToState(Lifecycle.State.RESUMED);foreground();
        }
        background();assertTrue(new WorkoutRepository(context).list().isEmpty());
    }
    @Test public void positionsStayFreshAcrossPagesAndRejectWeakExpiredUntimedAndJumpedFixes(){
        try(var settings=ActivityScenario.launch(P0SettingsActivity.class)){
            foreground();main(()->P0GpsPreparationTest.fixture(service));fix(1.3);assertTrue(service.hasFreshGpsPreview());
            try(var history=ActivityScenario.launch(P0HistoryActivity.class)){
                foreground();assertEquals(1.3,service.getGpsPreview().latitude(),.000001);
                fix(1.31);assertEquals("Single outlier cannot move the preview",1.3,service.getGpsPreview().latitude(),.000001);
                fix(1.31);fix(1.31);assertEquals(1.31,service.getGpsPreview().latitude(),.000001);
                fix(2,0,2);fix(2,SystemClock.elapsedRealtimeNanos()-6_000_000_000L,2);fix(2,SystemClock.elapsedRealtimeNanos(),90);
                assertEquals(1.31,service.getGpsPreview().latitude(),.000001);
                await(()->service.getGpsStatusObservable().getValue()==GpsStatusValue.GPS_SIGNAL_BAD);
                assertEquals(GpsStatusValue.GPS_SIGNAL_BAD,service.getGpsStatusObservable().getValue());
                SystemClock.sleep(5200);assertFalse(service.hasFreshGpsPreview());
                main(()->service.getTrackPointCreator().getSensorManager().getGpsHandler().onSensorDeactivated());
                assertFalse(service.hasFreshGpsPreview());assertNull(service.getGpsPreview());
                main(()->service.getTrackPointCreator().getSensorManager().getGpsHandler().onConnected(null,"Synthetic recovered GPS"));
                fix(1.32);assertTrue(service.hasFreshGpsPreview());assertEquals(1.32,service.getGpsPreview().latitude(),.000001);
            }
        }
        background();preferences();assertTrue(new WorkoutRepository(context).list().isEmpty());
    }
    @Test public void mapDemandDoesNotDuplicateGlobalDriverAndDefaultHeartPreferenceCannotRetainIdleGps(){
        HeartRateCommands.setConnectionEnabled(context,true);
        try(var home=ActivityScenario.launch(P0Activity.class)){
            foreground();Object owner=new Object();Object[] original={null};
            main(()->{original[0]=service.getTrackPointCreator().getSensorManager().getGpsHandler().driver;service.acquireMapGps(owner);service.acquireMapGps(owner);
                assertEquals(1,service.mapGpsOwnerCount());assertSame(original[0],service.getTrackPointCreator().getSensorManager().getGpsHandler().driver);
                service.releaseMapGps(owner);assertEquals(0,service.mapGpsOwnerCount());assertSame(original[0],service.getTrackPointCreator().getSensorManager().getGpsHandler().driver);});
        }
        background();assertTrue(new WorkoutRepository(context).list().isEmpty());preferences();
    }
    @Test public void explicitHeartPreviewSurvivesAppBackgroundButItsGpsDoesNot(){
        HeartRateCommands.setConnectionEnabled(context,true);
        try(var settings=ActivityScenario.launch(P0SettingsActivity.class)){
            foreground();main(service::tryStartSensors);
        }
        await(()->!service.hasForegroundGpsRequest()&&service.areSensorsStarted());
        main(()->{assertNull(service.getTrackPointCreator().getSensorManager().getGpsHandler().driver);assertFalse(service.isRecording());service.stopIdleSensors();});
        background();preferences();
    }
}
