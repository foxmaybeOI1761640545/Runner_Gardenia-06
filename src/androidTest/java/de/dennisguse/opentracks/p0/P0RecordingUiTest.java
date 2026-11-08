package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.*;
import android.Manifest;
import android.content.Intent;
import android.location.Location;
import androidx.test.rule.ServiceTestRule;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.sensors.driver.Driver;
import android.content.Context;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import org.junit.*;
import java.util.concurrent.atomic.AtomicReference;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.app.P0Activity;
import de.dennisguse.opentracks.p0.data.SessionStore;
import de.dennisguse.opentracks.p0.recording.*;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

public class P0RecordingUiTest {
    @Rule public final GrantPermissionRule permissions=GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.POST_NOTIFICATIONS);
    @Rule public final ServiceTestRule serviceRule=new ServiceTestRule();
    private final Context context=ApplicationProvider.getApplicationContext();
    private String sampling,distance,activityType;
    private final String[] animationNames={"window_animation_scale","transition_animation_scale","animator_duration_scale"};
    private final String[] animationValues=new String[3];
    private TrackRecordingService boundService;
    private TrackRecordingService service() throws Exception {
        if(boundService==null)boundService=((TrackRecordingService.Binder)serviceRule.bindService(new Intent(context,TrackRecordingService.class))).getService();
        return boundService;
    }
    @Before public void before() throws Exception {
        // Espresso coordinates must not race the platform window animation after recreate/background.
        for(int i=0;i<animationNames.length;i++){animationValues[i]=SystemThemeTestUtils.shell("settings get global "+animationNames[i]);SystemThemeTestUtils.shell("settings put global "+animationNames[i]+" 0");}
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());
        sampling=PreferencesUtils.getString(R.string.min_sampling_interval_key,"0");distance=PreferencesUtils.getString(R.string.recording_distance_interval_key,"5");activityType=PreferencesUtils.getDefaultActivityTypeLocalized();
    }
    @After public void after() throws Exception {
        if(boundService!=null)serviceRule.unbindService();
        TestEnvironmentUtils.stopTrackRecordingService();TestEnvironmentUtils.awaitTrackRecordingServiceDestroyed();new SessionStore(context).save(RecordingSession.ready());
        PreferencesUtils.setString(R.string.min_sampling_interval_key,sampling);PreferencesUtils.setString(R.string.recording_distance_interval_key,distance);PreferencesUtils.setDefaultActivityLocalized(activityType);
        for(int i=0;i<animationNames.length;i++)SystemThemeTestUtils.shell(animationValues[i].equals("null")?"settings delete global "+animationNames[i]:"settings put global "+animationNames[i]+" "+animationValues[i]);
    }
    private void phase(ActivityScenario<P0Activity> scenario,int expected) {
        long until=SystemClock.uptimeMillis()+10000;String label=context.getString(expected);AtomicReference<String> actual=new AtomicReference<>();
        do {scenario.onActivity(a->actual.set(((TextView)a.findViewById(R.id.p0_recording_state)).getText().toString()));if(label.equals(actual.get()))return;SystemClock.sleep(50);}while(SystemClock.uptimeMillis()<until);
        assertEquals(label,actual.get());
    }
    @Test public void cyclingRecordingSurvivesRecreationAndBackgroundThenPausesResumesAndFinishes() throws Exception {
        try(ActivityScenario<P0Activity> scenario=ActivityScenario.launch(P0Activity.class)) {
            onView(withId(R.id.p0_cycling)).perform(click());onView(withId(R.id.p0_start)).perform(click());prepare(scenario);phase(scenario,R.string.p0_recording);
            AtomicReference<Long> id=new AtomicReference<>();scenario.onActivity(a->id.set(new ViewModelProvider(a).get(P0RecordingViewModel.class).snapshot().getValue().session().trackId()));assertNotNull(id.get());
            scenario.recreate();phase(scenario,R.string.p0_recording);
            scenario.moveToState(Lifecycle.State.CREATED);SystemClock.sleep(300);scenario.moveToState(Lifecycle.State.RESUMED);phase(scenario,R.string.p0_recording);
            scenario.onActivity(a->assertEquals(id.get(),new ViewModelProvider(a).get(P0RecordingViewModel.class).snapshot().getValue().session().trackId()));
            onView(withId(R.id.p0_pause)).perform(click());phase(scenario,R.string.p0_paused);
            onView(withId(R.id.p0_resume)).perform(click());phase(scenario,R.string.p0_recording);
            onView(withId(R.id.p0_finish)).perform(click());phase(scenario,R.string.p0_finished);
            assertEquals(ActivityType.CYCLING,new ContentProviderUtils(context).getTrack(new Track.Id(id.get())).activityType());
        }
    }
    private void prepare(ActivityScenario<P0Activity> screen) throws Exception {
        phase(screen,R.string.p02_acquiring_gps);
        TrackRecordingService core=service();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->P0GpsPreparationTest.fixture(core));
        screen.recreate();phase(screen,R.string.p02_acquiring_gps);
        screen.moveToState(Lifecycle.State.CREATED);SystemClock.sleep(300);screen.moveToState(Lifecycle.State.RESUMED);phase(screen,R.string.p02_acquiring_gps);
        assertFalse(core.isRecording());assertTrue(core.isAcquiringGps());
        P0GpsPreparationTest.ready(core);
    }
    @Test public void waitingUiHasNoCoordinatesAndCancelLeavesNoWorkout() throws Exception {
        try(ActivityScenario<P0Activity> screen=ActivityScenario.launch(P0Activity.class)) {
            onView(withId(R.id.p0_start)).perform(click());phase(screen,R.string.p02_acquiring_gps);
            TrackRecordingService core=service();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->P0GpsPreparationTest.fixture(core));
            assertNull(TrackRecordingService.activeTrackId(context));assertFalse(metrics(screen).contains("1.300000"));
            assertTrue(new de.dennisguse.opentracks.p0.data.WorkoutRepository(context).list().isEmpty());
            onView(withId(R.id.p0_cancel_gps)).perform(click());phase(screen,R.string.p02_idle);
            assertFalse(core.isAcquiringGps());assertFalse(core.areSensorsStarted());assertNull(TrackRecordingService.activeTrackId(context));
        }
    }
    private String metrics(ActivityScenario<P0Activity> screen) {
        AtomicReference<String> value=new AtomicReference<>();
        screen.onActivity(a->{
            StringBuilder text=new StringBuilder();
            for(int id:new int[]{R.id.p0_metrics,R.id.p02_distance_value,R.id.p02_time_value,R.id.p02_speed_value,R.id.p02_pace_value,R.id.p02_status_detail})text.append(((TextView)a.findViewById(id)).getText()).append("\n");
            value.set(text.toString());
        });
        return value.get();
    }
    @Test public void realGpsPreviewSurvivesConsumedRecordingSnapshotsAndClearsOnDisabledProvider() throws Exception {
        try(ActivityScenario<P0Activity> screen=ActivityScenario.launch(P0Activity.class)) {
            onView(withId(R.id.p0_start)).perform(click());prepare(screen);phase(screen,R.string.p0_recording);
            TrackRecordingService core=service();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
                var gps=core.getTrackPointCreator().getSensorManager().getGpsHandler();
                if(gps.driver!=null)gps.driver.disconnect();
                gps.driver=new Driver() {
                    private boolean connected=true;
                    @Override public void connect(Context c,android.os.Handler h,String address) {}
                    @Override public boolean isConnected(){return connected;}
                    @Override public void updateDriverParameters(Context c,android.os.Handler h) {}
                    @Override public void disconnect(){connected=false;gps.onDisconnected();}
                };
                gps.onConnected(null,"Synthetic UI GPS");
                Location fix=new Location("gps");fix.setLatitude(1.3);fix.setLongitude(103.8);fix.setAccuracy(2);fix.setSpeed(3);fix.setTime(System.currentTimeMillis());fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                gps.onDataReceived(fix);
            });
            long deadline=SystemClock.uptimeMillis()+10000;
            while(!metrics(screen).contains("1.300000")&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(50);
            SystemClock.sleep(2200); // Multiple UI ticks after the recorder has consumed the one GPS event.
            String displayed=metrics(screen);assertTrue(displayed,displayed.contains("1.300000, 103.800000"));
            assertTrue(displayed,displayed.contains("±2.0 m"));assertTrue(displayed,displayed.contains("10.8 km/h"));assertTrue(displayed,displayed.contains("5:33 /km"));
            assertNotNull(core.getGpsPreview());
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->core.getTrackPointCreator().getSensorManager().getGpsHandler().onSensorDeactivated());
            deadline=SystemClock.uptimeMillis()+5000;
            while(metrics(screen).contains("1.300000")&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(50);
            assertFalse(metrics(screen).contains("1.300000"));assertFalse(metrics(screen).contains("10.8 km/h"));
            onView(withId(R.id.p0_finish)).perform(click());phase(screen,R.string.p0_finished);
        }
    }

}
