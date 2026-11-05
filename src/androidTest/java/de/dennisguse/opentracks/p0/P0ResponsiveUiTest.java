package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;
import androidx.test.rule.GrantPermissionRule;
import org.junit.Rule;
import org.junit.Test;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.data.*;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.data.statistics.TrackStatisticsUpdater;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.p0.data.SessionStore;
import de.dennisguse.opentracks.p0.recording.RecordingSession;
import de.dennisguse.opentracks.sensors.driver.*;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** Actual emulator pixels with synthetic local workout/HR fixtures; no RF acceptance claim. */
public class P0ResponsiveUiTest {
    @Rule public final GrantPermissionRule permissions=TestUtil.createScanPermissionRule();
    @Rule public final ServiceTestRule rule=new ServiceTestRule();
    private final Context context=ApplicationProvider.getApplicationContext();
    private final JSONArray captures=new JSONArray();
    private final Instant base=Instant.parse("2026-11-05T02:00:00Z");
    private static String shell(String command) throws Exception {
        try(ParcelFileDescriptor descriptor=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
            var input=new FileInputStream(descriptor.getFileDescriptor())){return new String(input.readAllBytes(),StandardCharsets.UTF_8);}
    }
    private static void restore(String kind,String original) throws Exception {
        var match=java.util.regex.Pattern.compile("Override (?:size|density): ([^\\r\\n]+)").matcher(original);
        shell("wm "+kind+" "+(match.find()?match.group(1).trim():"reset"));
    }
    private <T extends Activity> void waitText(ActivityScenario<T> screen,int id,String expected) {
        long end=SystemClock.uptimeMillis()+10000;AtomicBoolean ready=new AtomicBoolean();
        do{screen.onActivity(activity->ready.set(((TextView)activity.findViewById(id)).getText().toString().contains(expected)));if(ready.get())return;SystemClock.sleep(50);}while(SystemClock.uptimeMillis()<end);
        fail("P0 page did not display "+expected);
    }
    private <T extends Activity> void capture(ActivityScenario<T> screen,String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();P0UiCapture.save(name);
        screen.onActivity(activity->{
            var config=activity.getResources().getConfiguration();int night=config.uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            assertEquals(name.contains("-dark-")?32:16,night);
            var bars=androidx.core.view.WindowCompat.getInsetsController(activity.getWindow(),activity.getWindow().getDecorView());
            assertEquals(night!=32,bars.isAppearanceLightStatusBars());assertEquals(night!=32,bars.isAppearanceLightNavigationBars());
            assertTrue(name.contains("-small-")?config.screenWidthDp<=360:config.screenWidthDp>=600);
            assertNoEllipsis(activity.findViewById(android.R.id.content));
            try{captures.put(new JSONObject().put("file",name+".png").put("nightMode",night).put("widthDp",config.screenWidthDp));}catch(Exception failure){throw new AssertionError(failure);}
        });
    }
    private void assertNoEllipsis(android.view.View view) {
        if(view instanceof TextView text&&text.getLayout()!=null&&view.isShown())for(int i=0;i<text.getLineCount();i++)assertEquals("Truncated P0 text",0,text.getLayout().getEllipsisCount(i));
        if(view instanceof android.view.ViewGroup group)for(int i=0;i<group.getChildCount();i++)assertNoEllipsis(group.getChildAt(i));
    }
    private Track.Id history() {
        var store=new ContentProviderUtils(context);List<TrackPoint> points=new ArrayList<>();
        points.add(new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL,base));
        for(int i=1;i<=20;i++)points.add(new TrackPoint(null,TrackPoint.Type.TRACKPOINT,new Position(base.plusSeconds(i),1.3+i*0.000018,103.8,Distance.of(2),Altitude.WGS84.of(12),null,null,Speed.of(2)),null,new HeartRate(70+i%3*5),null,null,null,null));
        points.add(new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL,base.plusSeconds(21)));
        Track.Id id=store.insertTrack(new Track(null,UUID.randomUUID(),"Synthetic P0 run","Local UI fixture","",ActivityType.RUNNING,ZoneOffset.ofHours(8),new TrackStatisticsUpdater(points).getTrackStatistics()));
        store.bulkInsertTrackPoint(points,id);
        for(TrackPoint point:points)if(point.heartRate()!=null)new HeartRateRepository(context).insert(id,new HeartRate(point.heartRate().value(),new HeartRateSample(point.getTime(),"AA:BB:CC:DD:EE:FF","Synthetic UI strap",true,12,List.of(1024)).captured(point.getTime(),"AA:BB:CC:DD:EE:FF","Synthetic UI strap","ui-session")));
        return id;
    }
    @Test public void actualP0PagesInBothThemesAndSmallAndLargeWindows() throws Exception {
        String theme=PreferencesUtils.getString(R.string.theme_key,context.getString(R.string.theme_default));
        String selected=PreferencesUtils.getString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
        String size=shell("wm size"),density=shell("wm density");TrackRecordingService service=null;
        try {
            PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
            TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();
            service=((TrackRecordingService.Binder)rule.bindService(new Intent(context,TrackRecordingService.class))).getService();
            final TrackRecordingService core=service;
            for(boolean large:new boolean[]{false,true})for(boolean dark:new boolean[]{false,true}) {
                shell(large?"wm size 1600x2560":"wm size 720x1280");shell("wm density 320");
                PreferencesUtils.setString(R.string.theme_key,dark?R.string.theme_night_value:R.string.theme_day_value);
                InstrumentationRegistry.getInstrumentation().runOnMainSync(PreferencesUtils::applyNightMode);SystemClock.sleep(500);
                TestEnvironmentUtils.deleteAllTracks();new SessionStore(context).save(RecordingSession.ready());Track.Id id=history();
                String profile="p0-matrix-"+(large?"large":"small")+(dark?"-dark-":"-light-");
                try(ActivityScenario<P0Activity> screen=ActivityScenario.launch(P0Activity.class)) {
                    waitText(screen,R.id.p0_recording_state,context.getString(R.string.p0_ready));capture(screen,profile+"home");
                    InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
                        core.getTrackPointCreator().setClock(base.toString());core.startNewTrack();core.getTrackPointCreator().setClock(base.plusSeconds(2).toString());
                        var heart=core.getTrackPointCreator().getSensorManager().getHeartRateHandler();heart.onConnectionStateChanged(BleConnectionSession.State.CONNECTED);heart.onConnected("AA:BB:CC:DD:EE:FF","Synthetic UI strap");
                        var characteristic=new android.bluetooth.BluetoothGattCharacteristic(HeartRateBluetooth.HEARTRATE.measurementUUID(),0,0);
                        heart.onDataReceived(new HeartRateBluetooth().parsePayload(HeartRateBluetooth.HEARTRATE,"Synthetic UI strap",characteristic,new byte[]{6,76}));
                    });
                    waitText(screen,R.id.p0_recording_state,context.getString(R.string.p0_recording));waitText(screen,R.id.p0_heart_rate,"Synthetic UI strap");capture(screen,profile+"recording");
                    InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{core.getTrackPointCreator().setClock(base.plusSeconds(5).toString());core.endCurrentTrack();core.stopIdleSensors();});
                }
                new SessionStore(context).save(RecordingSession.ready());
                try(ActivityScenario<P0SensorsActivity> screen=ActivityScenario.launch(P0SensorsActivity.class)){SystemClock.sleep(500);capture(screen,profile+"settings");}
                try(ActivityScenario<P0HistoryActivity> screen=ActivityScenario.launch(P0HistoryActivity.class)){SystemClock.sleep(700);capture(screen,profile+"history");}
                try(ActivityScenario<P0HistoryActivity> screen=ActivityScenario.launch(P0HistoryActivity.intent(context,id.id()))){waitText(screen,R.id.p0_workout_details,"Synthetic P0 run");capture(screen,profile+"detail");}
            }
            String configured=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
            File output=new File(configured!=null?new File(configured):new File(context.getExternalMediaDirs()[0],"additional_test_output"),"p0-ui");
            try(var stream=new FileOutputStream(new File(output,"matrix.json"))){stream.write(new JSONObject().put("physicalBleTested",false).put("timezone",TimeZone.getDefault().getID()).put("screenshots",captures).toString(2).getBytes(StandardCharsets.UTF_8));}
            assertEquals(20,captures.length());
        }finally {
            if(service!=null){final TrackRecordingService core=service;InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{core.endCurrentTrack();core.stopIdleSensors();});}
            new SessionStore(context).save(RecordingSession.ready());PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,selected);PreferencesUtils.setString(R.string.theme_key,theme);
            TestEnvironmentUtils.deleteAllTracks();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(PreferencesUtils::applyNightMode);restore("size",size);restore("density",density);
        }
    }
}
