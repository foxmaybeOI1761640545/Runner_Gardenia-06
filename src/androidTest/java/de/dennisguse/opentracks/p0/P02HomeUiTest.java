package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.location.Location;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;
import androidx.lifecycle.ViewModelProvider;
import org.junit.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.p0.ble.HeartRateCommands;
import de.dennisguse.opentracks.p0.data.SessionStore;
import de.dennisguse.opentracks.p0.recording.*;
import de.dennisguse.opentracks.sensors.GpsStatusValue;
import de.dennisguse.opentracks.sensors.driver.*;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.ActivityTestUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** Real UI/service/gate transitions; explicitly synthetic GPS/SIG notifications, never hardware proof. */
public class P02HomeUiTest {
    @Rule public final GrantPermissionRule permissions=TestUtil.createScanPermissionRule();
    @Rule public final ServiceTestRule rule=new ServiceTestRule();
    private final Context context=ApplicationProvider.getApplicationContext();
    private final JSONArray captures=new JSONArray();
    private final Instant base=Instant.parse("2026-11-06T02:00:00Z");
    private TrackRecordingService core;
    private String gps,heart,theme,size,density,activity;
    private boolean enabled,automatic;
    private void main(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    private String shell(String command)throws Exception {
        try(ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
            var stream=new FileInputStream(fd.getFileDescriptor())){return new String(stream.readAllBytes(),StandardCharsets.UTF_8);}
    }
    private void restore(String kind,String original)throws Exception {
        var match=java.util.regex.Pattern.compile("Override (?:size|density): ([^\\r\\n]+)").matcher(original);
        shell("wm "+kind+" "+(match.find()?match.group(1).trim():"reset"));
    }
    @Before public void before()throws Exception {
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());
        gps=PreferencesUtils.getString(R.string.settings_sensor_gps_key,SensorType.INTERNAL.getPreferenceValue());heart=HeartRateCommands.selected();
        theme=PreferencesUtils.getString(R.string.theme_key,context.getString(R.string.theme_default));activity=PreferencesUtils.getDefaultActivityTypeLocalized();
        size=shell("wm size");density=shell("wm density");enabled=HeartRateCommands.connectionEnabled(context);automatic=HeartRateCommands.autoReconnectEnabled(context);
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,SensorType.NONE.getPreferenceValue());
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
        PreferencesUtils.setDefaultActivityLocalized(context.getString(de.dennisguse.opentracks.data.models.ActivityType.RUNNING.getLocalizedStringId()));
        HeartRateCommands.setConnectionEnabled(context,true);HeartRateCommands.setAutoReconnect(context,true);
    }
    @After public void after()throws Exception {
        if(core!=null){main(()->{core.cancelGpsAcquisition();core.endCurrentTrack();core.stopIdleSensors();});rule.unbindService();}
        TestEnvironmentUtils.stopTrackRecordingService();TestEnvironmentUtils.awaitTrackRecordingServiceDestroyed();
        new SessionStore(context).save(RecordingSession.ready());TestEnvironmentUtils.deleteAllTracks();
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,gps);PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,heart);
        HeartRateCommands.setConnectionEnabled(context,enabled);HeartRateCommands.setAutoReconnect(context,automatic);
        PreferencesUtils.setDefaultActivityLocalized(activity);PreferencesUtils.setString(R.string.theme_key,theme);
        main(PreferencesUtils::applyNightMode);restore("size",size);restore("density",density);
    }
    private void waitText(ActivityScenario<P0Activity> screen,int id,String value) {
        long deadline=SystemClock.uptimeMillis()+10000;AtomicBoolean matched=new AtomicBoolean();
        do{screen.onActivity(a->matched.set(((TextView)a.findViewById(id)).getText().toString().contains(value)));if(matched.get())return;SystemClock.sleep(50);}while(SystemClock.uptimeMillis()<deadline);
        fail("Home did not render "+context.getResources().getResourceEntryName(id)+" = "+value);
    }
    private void assertNoTruncation(View view) {
        if(view instanceof TextView text&&view.isShown()&&text.getLayout()!=null)
            for(int i=0;i<text.getLineCount();i++)assertEquals("Truncated home label",0,text.getLayout().getEllipsisCount(i));
        if(view instanceof android.view.ViewGroup group)for(int i=0;i<group.getChildCount();i++)assertNoTruncation(group.getChildAt(i));
    }
    private void capture(ActivityScenario<P0Activity> screen,String profile,String state,int... visibleButtons)throws Exception {
        AtomicBoolean focused=new AtomicBoolean();long deadline=SystemClock.uptimeMillis()+5000;
        do{screen.onActivity(a->focused.set(a.hasWindowFocus()));if(focused.get())break;SystemClock.sleep(50);}while(SystemClock.uptimeMillis()<deadline);
        assertTrue("Actual P0 window must have focus",focused.get());SystemClock.sleep(800);
        screen.onActivity(a->{
            assertNoTruncation(a.findViewById(android.R.id.content));
            // Standard phone/tablet profiles must expose the complete HR summary without scrolling.
            for(int id:new int[]{R.id.p0_heart_rate,R.id.p02_heart_device,R.id.p02_heart_state,R.id.p02_heart_updated,R.id.p02_heart_reconnect}) {
                View item=a.findViewById(id);Rect bounds=new Rect();assertTrue("Heart summary offscreen",item.getGlobalVisibleRect(bounds));
                assertEquals("Heart summary clipped",item.getHeight(),bounds.height());
            }
            String detail=((TextView)a.findViewById(R.id.p02_status_detail)).getText().toString();
            if(Set.of("gps-ready","recording","resumed").contains(state))assertEquals(context.getString(R.string.p02_gps_ready),detail);
            if(state.equals("gps-lost"))assertEquals(context.getString(R.string.p02_gps_weak),detail);
            if(Set.of("idle","acquiring","paused","gps-lost").contains(state)) {
                assertEquals("—",((TextView)a.findViewById(R.id.p02_speed_value)).getText().toString());
                assertEquals("—",((TextView)a.findViewById(R.id.p02_pace_value)).getText().toString());
                assertFalse(a.findViewById(R.id.p0_metrics).isShown());
            }
            Set<Integer> visible=new HashSet<>();for(int id:visibleButtons)visible.add(id);
            for(int id:new int[]{R.id.p0_start,R.id.p0_cancel_gps,R.id.p0_continue_gps,R.id.p0_pause,R.id.p0_resume,R.id.p0_finish}){
                View action=a.findViewById(id);assertEquals("Wrong home action for "+state,visible.contains(id),action.isShown());
                if(visible.contains(id)){Rect bounds=new Rect();assertTrue(action.getGlobalVisibleRect(bounds));assertEquals("Primary action clipped",action.getHeight(),bounds.height());assertTrue(action.isEnabled());}
            }
            var config=a.getResources().getConfiguration();int mode=config.uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            assertEquals(profile.contains("dark")?32:16,mode);assertTrue(profile.contains("small")?config.screenWidthDp<=360:config.screenWidthDp>=600);
            var bars=androidx.core.view.WindowCompat.getInsetsController(a.getWindow(),a.getWindow().getDecorView());
            assertEquals(mode!=32,bars.isAppearanceLightStatusBars());assertEquals(mode!=32,bars.isAppearanceLightNavigationBars());
            try{captures.put(new JSONObject().put("file",profile+state+".png").put("state",state).put("captureEpochMillis",System.currentTimeMillis()).put("nightMode",mode).put("widthDp",config.screenWidthDp)
                    .put("phase",new ViewModelProvider(a).get(P0RecordingViewModel.class).snapshot().getValue().session().phase().name()));}
            catch(JSONException failure){throw new AssertionError(failure);}
        });
        P0UiCapture.save(profile+state);
        byte[] bytes=java.nio.file.Files.readAllBytes(new File(output(),profile+state+".png").toPath());
        byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder hash=new StringBuilder();
        for(byte b:digest)hash.append(String.format(Locale.ROOT,"%02x",b&255));
        captures.getJSONObject(captures.length()-1).put("sha256",hash.toString());
    }
    private File output() {
        String configured=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
        return new File(configured!=null?new File(configured):new File(context.getExternalMediaDirs()[0],"additional_test_output"),"p0-ui");
    }
    private void fix(int second,double latitude) {
        main(()->{
            core.getTrackPointCreator().setClock(base.plusSeconds(second).toString());
            Location f=new Location("gps");f.setLatitude(latitude);f.setLongitude(103.8);f.setAccuracy(2);f.setSpeed(3);
            f.setTime(base.plusSeconds(second).toEpochMilli());f.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            core.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(f);
        });
    }
    @Test public void homeStateMatrixUsesRealRenderingInBothThemesAndScreenSizes()throws Exception {
        core=((TrackRecordingService.Binder)rule.bindService(new Intent(context,TrackRecordingService.class))).getService();
        for(boolean large:new boolean[]{false,true})for(boolean dark:new boolean[]{false,true}) {
            shell(large?"wm size 1600x2560":"wm size 720x1280");shell("wm density 320");
            shell("cmd uimode night "+(dark?"yes":"no"));main(PreferencesUtils::applyNightMode);SystemClock.sleep(500);
            main(()->{core.cancelGpsAcquisition();core.endCurrentTrack();core.stopIdleSensors();core.getTrackPointCreator().setClock(base.toString());});
            new SessionStore(context).save(RecordingSession.ready());String profile="p02-home-"+(large?"large":"small")+(dark?"-dark-":"-light-");
            try(ActivityScenario<P0Activity> screen=ActivityScenario.launch(P0Activity.class)) {
                waitText(screen,R.id.p0_recording_state,context.getString(R.string.p02_idle));
                screen.onActivity(a->{assertEquals("0.0 m",((TextView)a.findViewById(R.id.p02_distance_value)).getText().toString());assertEquals("00:00",((TextView)a.findViewById(R.id.p02_time_value)).getText().toString());});
                capture(screen,profile,"idle",R.id.p0_start);
                main(()->{core.beginGpsAcquisition();P0GpsPreparationTest.fixture(core);});
                waitText(screen,R.id.p0_recording_state,context.getString(R.string.p02_acquiring_gps));
                assertNull(TrackRecordingService.activeTrackId(context));capture(screen,profile,"acquiring",R.id.p0_cancel_gps);
                P0GpsPreparationTest.ready(core);waitText(screen,R.id.p02_status_detail,context.getString(R.string.p02_gps_ready));
                capture(screen,profile,"gps-ready",R.id.p0_pause,R.id.p0_finish);
                fix(12,1.300324);waitText(screen,R.id.p02_speed_value,"10.8 km/h");
                capture(screen,profile,"recording",R.id.p0_pause,R.id.p0_finish);
                main(()->{core.getTrackPointCreator().setClock(base.plusSeconds(20).toString());core.newGpsStatus(GpsStatusValue.GPS_SIGNAL_LOST);});
                waitText(screen,R.id.p02_status_detail,context.getString(R.string.p02_gps_weak));assertTrue(core.isRecording());
                capture(screen,profile,"gps-lost",R.id.p0_pause,R.id.p0_finish);
                screen.onActivity(a->new ViewModelProvider(a).get(P0RecordingViewModel.class).pause());waitText(screen,R.id.p0_recording_state,context.getString(R.string.p0_paused));
                capture(screen,profile,"paused",R.id.p0_resume,R.id.p0_finish);
                var id=new SessionStore(context).recover().trackId();assertNotNull(id);
                main(()->{core.getTrackPointCreator().setClock(base.plusSeconds(30).toString());core.resumeTrack(new de.dennisguse.opentracks.data.models.Track.Id(id));P0GpsPreparationTest.fixture(core);});
                fix(31,1.300351);waitText(screen,R.id.p02_status_detail,context.getString(R.string.p02_gps_ready));
                capture(screen,profile,"resumed",R.id.p0_pause,R.id.p0_finish);
                main(()->{
                    var hr=core.getTrackPointCreator().getSensorManager().getHeartRateHandler();hr.onConnected("AA:BB:CC:DD:EE:FF","Synthetic UI strap");hr.onConnectionStateChanged(BleConnectionSession.State.CONNECTED);
                    var measurement=new android.bluetooth.BluetoothGattCharacteristic(HeartRateBluetooth.HEARTRATE.measurementUUID(),0,0);
                    hr.onDataReceived(new HeartRateBluetooth().parsePayload(HeartRateBluetooth.HEARTRATE,"Synthetic UI strap",measurement,new byte[]{6,76}));
                });
                waitText(screen,R.id.p0_heart_rate,context.getString(R.string.hr_value,76f));
                capture(screen,profile,"heart-connected",R.id.p0_pause,R.id.p0_finish);
                main(()->{var hr=core.getTrackPointCreator().getSensorManager().getHeartRateHandler();hr.onConnectionLost();hr.onConnectionStateChanged(BleConnectionSession.State.RECONNECT_WAIT);});
                waitText(screen,R.id.p02_heart_state,context.getString(R.string.hr_retry));waitText(screen,R.id.p0_heart_rate,context.getString(R.string.hr_empty_value));
                capture(screen,profile,"heart-disconnected",R.id.p0_pause,R.id.p0_finish);
                screen.onActivity(a->new ViewModelProvider(a).get(P0RecordingViewModel.class).finish());waitText(screen,R.id.p0_recording_state,context.getString(R.string.p0_finished));
            }
        }
        assertEquals(36,captures.length());
        try(var stream=new FileOutputStream(new File(output(),"p02-home-matrix.json"))){stream.write(new JSONObject().put("physicalBleTested",false).put("syntheticGps",true)
                .put("sourceCommit",InstrumentationRegistry.getArguments().getString("buildCommit","local")).put("fixtureClockStartUtc",base.toString())
                .put("timezone",TimeZone.getDefault().getID()).put("screenshots",captures).toString(2).getBytes(StandardCharsets.UTF_8));}
    }
    private void waitForHomeInput(ActivityScenario<P0Activity> screen,android.app.Activity outgoing) {
        long deadline=SystemClock.uptimeMillis()+10000;AtomicBoolean ready=new AtomicBoolean();
        do {
            screen.onActivity(a->ready.set(outgoing.isDestroyed()&&a.hasWindowFocus()));
            if(ready.get()) {InstrumentationRegistry.getInstrumentation().waitForIdleSync();return;}
            SystemClock.sleep(50);
        } while(SystemClock.uptimeMillis()<deadline);
        fail("Outgoing page must finish and home must own input before navigation");
    }
    @Test public void homeHistoryAndSensorEntriesNavigateWithoutCreatingAWorkout()throws Exception {
        try(ActivityScenario<P0Activity> screen=ActivityScenario.launch(P0Activity.class)) {
            onView(withId(R.id.p0_all_history)).perform(click());
            var history=ActivityTestUtils.waitForResumedActivity(P0HistoryActivity.class,Duration.ofSeconds(10));assertNotNull(history);main(history::finish);
            ActivityTestUtils.waitForResumedActivity(P0Activity.class,Duration.ofSeconds(10));waitForHomeInput(screen,history);
            onView(withId(R.id.p0_devices)).perform(scrollTo(),click());
            var sensors=ActivityTestUtils.waitForResumedActivity(P0SensorsActivity.class,Duration.ofSeconds(10));assertNotNull(sensors);main(sensors::finish);
            ActivityTestUtils.waitForResumedActivity(P0Activity.class,Duration.ofSeconds(10));waitForHomeInput(screen,sensors);assertNull(TrackRecordingService.activeTrackId(context));
        }
    }
}
