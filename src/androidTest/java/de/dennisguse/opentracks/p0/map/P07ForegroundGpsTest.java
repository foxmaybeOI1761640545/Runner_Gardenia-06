package de.dennisguse.opentracks.p0.map;

import static org.junit.Assert.*;
import android.app.Activity;
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
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
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
    private <T extends Activity> T resumed(Class<T> type,Activity previous){
        AtomicReference<T> result=new AtomicReference<>();
        await(()->{
            for(Activity activity:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
                if(type.isInstance(activity)&&activity!=previous){result.set(type.cast(activity));return true;}
            return false;
        });
        return result.get();
    }
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
            var opened=new ArrayList<Activity>();
            try{
                home.onActivity(a->assertTrue(a.findViewById(R.id.p03_settings).performClick()));
                var firstSettings=resumed(P0SettingsActivity.class,null);opened.add(firstSettings);
                foreground();main(firstSettings::recreate);
                var settings=resumed(P0SettingsActivity.class,firstSettings);opened.add(settings);foreground();
                main(()->{assertSame(handler[0],service.getTrackPointCreator().getSensorManager().getGpsHandler());assertSame(driver[0],service.getTrackPointCreator().getSensorManager().getGpsHandler().driver);});
                main(()->assertTrue(settings.findViewById(R.id.p0_all_history).performClick()));
                var history=resumed(P0HistoryActivity.class,null);opened.add(history);foreground();
                main(()->history.startActivity(P06MapActivity.intent(history,null)));
                var maps=resumed(P06MapActivity.class,null);opened.add(maps);foreground();
                main(()->{assertSame(driver[0],service.getTrackPointCreator().getSensorManager().getGpsHandler().driver);assertEquals(0,service.mapGpsOwnerCount());});
                main(maps::finish);resumed(P0HistoryActivity.class,null);
                main(history::finish);resumed(P0SettingsActivity.class,null);
                main(settings::finish);resumed(P0Activity.class,null);
            }finally{
                main(()->{for(int i=opened.size()-1;i>=0;i--){var activity=opened.get(i);if(!activity.isDestroyed()&&!activity.isFinishing())activity.finish();}});
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
            settings.onActivity(a->assertTrue(a.findViewById(R.id.p0_all_history).performClick()));
            var history=resumed(P0HistoryActivity.class,null);
            try{
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
            }finally{main(history::finish);}
            resumed(P0SettingsActivity.class,null);
        }
        background();preferences();assertTrue(new WorkoutRepository(context).list().isEmpty());
    }
    @Test public void mapDemandDoesNotDuplicateGlobalDriverAndDefaultHeartPreferenceCannotRetainIdleGps(){
        HeartRateCommands.setConnectionEnabled(context,true);
        de.dennisguse.opentracks.sensors.GpsHandler[] released={null};
        try(var home=ActivityScenario.launch(P0Activity.class)){
            foreground();Object owner=new Object();Object[] original={null};
            main(()->{released[0]=service.getTrackPointCreator().getSensorManager().getGpsHandler();original[0]=released[0].driver;service.acquireMapGps(owner);service.acquireMapGps(owner);
                assertEquals(1,service.mapGpsOwnerCount());assertSame(original[0],service.getTrackPointCreator().getSensorManager().getGpsHandler().driver);
                service.releaseMapGps(owner);assertEquals(0,service.mapGpsOwnerCount());assertSame(original[0],service.getTrackPointCreator().getSensorManager().getGpsHandler().driver);});
        }
        background();lateFix(released[0]);assertFalse(service.hasFreshGpsPreview());
        try(var home=ActivityScenario.launch(P0Activity.class)){
            foreground();Object[] current={null};
            main(()->{current[0]=service.getTrackPointCreator().getSensorManager().getGpsHandler();assertNotSame(released[0],current[0]);});
            lateFix(released[0]);main(()->assertSame(current[0],service.getTrackPointCreator().getSensorManager().getGpsHandler()));
        }
        background();
        assertTrue(new WorkoutRepository(context).list().isEmpty());preferences();
    }
    private void lateFix(de.dennisguse.opentracks.sensors.GpsHandler released){main(()->{
        Location location=new Location("gps");location.setLatitude(1.3);location.setLongitude(103.8);location.setAccuracy(2);
        location.setTime(System.currentTimeMillis());location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        released.onDataReceived(location); // A released aggregator must never be accessed or revived.
    });}
    @Test public void explicitHeartPreviewSurvivesAppBackgroundButItsGpsDoesNot(){
        HeartRateCommands.setConnectionEnabled(context,true);
        de.dennisguse.opentracks.sensors.GpsHandler[] released={null};
        try(var settings=ActivityScenario.launch(P0SettingsActivity.class)){
            foreground();main(()->{service.tryStartSensors();released[0]=service.getTrackPointCreator().getSensorManager().getGpsHandler();});
        }
        await(()->!service.hasForegroundGpsRequest()&&service.areSensorsStarted());
        lateFix(released[0]);assertFalse(service.hasFreshGpsPreview());
        main(()->{assertNull(service.getTrackPointCreator().getSensorManager().getGpsHandler().driver);assertFalse(service.isRecording());service.stopIdleSensors();});
        background();preferences();
    }
}
