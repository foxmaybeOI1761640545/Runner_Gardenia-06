package de.dennisguse.opentracks.p0.map;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;
import android.content.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.*;
import org.junit.*;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.p0.data.SessionStore;
import de.dennisguse.opentracks.p0.recording.RecordingSession;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.sensors.driver.GpsInternal;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

public class P061GpsPreferencesTest {
    @Rule public final GrantPermissionRule permissions=TestUtil.createScanPermissionRule();
    @Rule public final ServiceTestRule rule=new ServiceTestRule();
    private final Context context=ApplicationProvider.getApplicationContext();
    private TrackRecordingService service;
    private String gps,interval;
    private void main(Runnable r){InstrumentationRegistry.getInstrumentation().runOnMainSync(r);}
    @Before public void before() throws Exception {
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());
        gps=PreferencesUtils.getString(R.string.settings_sensor_gps_key,"INTERNAL");
        interval=PreferencesUtils.getString(R.string.min_sampling_interval_key,"1");
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,"NONE");PreferencesUtils.setString(R.string.min_sampling_interval_key,"7");
        service=((TrackRecordingService.Binder)rule.bindService(new Intent(context,TrackRecordingService.class))).getService();
    }
    @After public void after(){
        main(()->{service.cancelGpsAcquisition();service.endCurrentTrack();service.stopIdleSensors();});
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,gps);PreferencesUtils.setString(R.string.min_sampling_interval_key,interval);
        new SessionStore(context).save(RecordingSession.ready());
    }
    private void owners(int expected){
        long end=android.os.SystemClock.uptimeMillis()+5000;int[] count={-1};
        do{main(()->count[0]=service.mapGpsOwnerCount());if(count[0]==expected)return;android.os.SystemClock.sleep(50);}while(android.os.SystemClock.uptimeMillis()<end);
        assertEquals(expected,count[0]);
    }
    private void preferences(){assertEquals("NONE",PreferencesUtils.getString(R.string.settings_sensor_gps_key,""));assertEquals("7",PreferencesUtils.getString(R.string.min_sampling_interval_key,""));}
    @Test public void idlePreviewRecreateAndRepeatedEntryNeverWritePreferencesOrLeakOwners(){
        for(int i=0;i<3;i++){
            try(var screen=ActivityScenario.launch(P06MapActivity.class)){
                onView(withId(R.id.p06_my_position)).perform(click());
                screen.onActivity(a->{assertEquals(1,service.mapGpsOwnerCount());preferences();
                    var driver=(GpsInternal)service.getTrackPointCreator().getSensorManager().getGpsHandler().driver;
                    assertNotNull(driver);assertEquals(1000,driver.createLocationRequest().getIntervalMillis());});
                screen.recreate();owners(1);screen.onActivity(a->{assertEquals(1,service.mapGpsOwnerCount());preferences();});
            }
            main(()->assertEquals(0,service.mapGpsOwnerCount()));preferences();
        }
        main(()->assertFalse(service.isRecording()));
    }
    @Test public void recordingUsesOriginalGpsConfigurationAndSurvivesMapClosure(){
        main(service::startNewTrack);
        try(var screen=ActivityScenario.launch(P06MapActivity.class)){
            onView(withId(R.id.p06_my_position)).perform(click());
            screen.onActivity(a->{preferences();assertTrue(service.isRecording());assertEquals(1,service.mapGpsOwnerCount());
                assertNull("A map must not turn a user's GPS-disabled recording into GPS recording",service.getTrackPointCreator().getSensorManager().getGpsHandler().driver);});
            screen.recreate();owners(1);screen.onActivity(a->assertEquals(1,service.mapGpsOwnerCount()));
        }
        main(()->{assertTrue(service.isRecording());assertEquals(0,service.mapGpsOwnerCount());});preferences();
    }
    @Test public void openingAndClosingMapKeepsLiveGpsDriverAndSevenSecondRecordingInterval(){
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,"INTERNAL");
        main(service::startNewTrack);
        GpsInternal[] original={null};
        main(()->{original[0]=(GpsInternal)service.getTrackPointCreator().getSensorManager().getGpsHandler().driver;assertEquals(7000,original[0].createLocationRequest().getIntervalMillis());});
        try(var screen=ActivityScenario.launch(P06MapActivity.class)){
            onView(withId(R.id.p06_my_position)).perform(click());
            screen.onActivity(a->{assertSame(original[0],service.getTrackPointCreator().getSensorManager().getGpsHandler().driver);assertEquals(7000,original[0].createLocationRequest().getIntervalMillis());});
        }
        main(()->{assertEquals(0,service.mapGpsOwnerCount());assertTrue(service.isRecording());assertSame(original[0],service.getTrackPointCreator().getSensorManager().getGpsHandler().driver);});
        assertEquals("INTERNAL",PreferencesUtils.getString(R.string.settings_sensor_gps_key,""));assertEquals("7",PreferencesUtils.getString(R.string.min_sampling_interval_key,""));
    }

}
