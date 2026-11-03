package de.dennisguse.opentracks.ui;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static de.dennisguse.opentracks.util.EspressoUtils.*;
import static de.dennisguse.opentracks.util.TrackUiTestUtils.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.MutableLiveData;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import de.dennisguse.opentracks.*;
import de.dennisguse.opentracks.data.*;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.data.statistics.TrackStatisticsUpdater;
import de.dennisguse.opentracks.sensors.HeartRateStatus;
import de.dennisguse.opentracks.sensors.driver.BleConnectionSession;
import de.dennisguse.opentracks.services.*;
import de.dennisguse.opentracks.settings.*;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeSensorPreference;
import de.dennisguse.opentracks.util.ActivityTestUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** Real pages with explicitly synthetic test data; never a physical BLE acceptance claim. */
public class HeartRateUiScreenshotTest {
    private static final Duration WAIT = Duration.ofSeconds(15);
    @Rule public GrantPermissionRule permissions = TestUtil.createScanPermissionRule();
    @Rule public ServiceTestRule serviceRule = new ServiceTestRule();
    private final Context context = ApplicationProvider.getApplicationContext();
    private final JSONArray captures = new JSONArray();
    private File output;
    private TrackRecordingService boundService;

    @Test public void realPagesInLightDarkAndTwoWindowSizes() throws Exception {
        String originalTheme = PreferencesUtils.getString(R.string.theme_key,R.string.theme_default);
        String overrideSize = shell("wm size");String overrideDensity = shell("wm density");
        String configured = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
        output = new File(configured != null ? new File(configured) : new File(context.getExternalMediaDirs()[0],"additional_test_output"),"heart-rate-ui");
        assertTrue(output.isDirectory() || output.mkdirs());
        try {
            for (boolean narrow : new boolean[]{false,true}) for (boolean dark : new boolean[]{false,true}) {
                shell(narrow ? "wm size 720x1280" : "wm size reset");
                shell(narrow ? "wm density 320" : "wm density reset");
                PreferencesUtils.setString(R.string.theme_key,dark ? R.string.theme_night_value : R.string.theme_day_value);
                InstrumentationRegistry.getInstrumentation().runOnMainSync(PreferencesUtils::applyNightMode);
                String profile = (narrow ? "narrow" : "phone") + (dark ? "-dark" : "-light");
                TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();
                recording(profile);
                history(profile);
                settings(profile);
            }
            JSONObject manifest = new JSONObject().put("physicalBleTested",false)
                    .put("fixtures","Synthetic heart-rate notifications and device rows in test code only")
                    .put("timezone",java.util.TimeZone.getDefault().getID()).put("screenshots",captures);
            try (FileOutputStream stream = new FileOutputStream(new File(output,"manifest.json"))) {
                stream.write(manifest.toString(2).getBytes(StandardCharsets.UTF_8));
            }
            assertEquals(40,captures.length());
        } finally {
            TestEnvironmentUtils.stopTrackRecordingService();
            PreferencesUtils.setString(R.string.theme_key,originalTheme);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(PreferencesUtils::applyNightMode);
            restoreOverride("size",overrideSize);restoreOverride("density",overrideDensity);
        }
    }

    private void recording(String profile) throws Exception {
        try (ActivityScenario<TrackListActivity> home = ActivityScenario.launch(TrackListActivity.class)) {
            waitUntilDisplayed(withId(R.id.track_list),WAIT); capture(profile+"-home",false);
            startRecordingFromTrackList(WAIT);waitUntilDisplayed(withId(R.id.heart_rate_panel),WAIT);
            if (boundService == null) boundService = ((TrackRecordingService.Binder)serviceRule.bindService(new Intent(context,TrackRecordingService.class))).getService();
            TrackRecordingService service = boundService;
            waitForResumedViewCondition(withId(R.id.heart_rate_panel),view -> {
                RecordingData data = service.getRecordingDataObservable().getValue();
                var status = service.getRecordingStatusObservable().getValue();
                return data != null && data.track() != null && status != null && data.track().id().equals(status.trackId());
            },"current recording data",WAIT);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(service::stopUpdateRecordingData);
            RecordingData base = service.getRecordingDataObservable().getValue();assertNotNull(base);assertNotNull(base.latestTrackPoint());
            for (String state : new String[]{"connected","stale","reconnecting"}) {
                Instant received = Instant.parse("2026-11-01T02:00:00Z");
                HeartRateStatus status = new HeartRateStatus(state.equals("reconnecting") ? BleConnectionSession.State.RECONNECT_WAIT : BleConnectionSession.State.CONNECTED,
                        received,received.plusSeconds(state.equals("stale") ? 8 : 1),"Test strap · synthetic",false);
                TrackPoint point = base.latestTrackPoint();
                TrackPoint fixture = new TrackPoint(point.id(),point.type(),point.position(),point.sensorDistance(),
                        HeartRate.of(76),point.temperature(),point.cadence(),point.power(),point.altitudeGainLoss());
                RecordingData data = new RecordingData(base.track(),fixture,base.sensorDataSet(),base.currentSegment(),status);
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> ((MutableLiveData<RecordingData>)service.getRecordingDataObservable()).setValue(data));
                waitForResumedViewCondition(withId(R.id.heart_rate_current),view -> ((TextView)view).getText().toString().equals(context.getString(state.equals("connected") ? R.string.hr_value : R.string.hr_empty_value,76f)),"heart-rate fixture render",WAIT);
                Activity activity = ActivityTestUtils.waitForResumedActivity(TrackRecordingActivity.class,WAIT);
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                    for (int id : new int[]{R.id.heart_rate_current,R.id.heart_rate_status,R.id.heart_rate_updated,R.id.heart_rate_settings}) assertVisibleText(activity.findViewById(id));
                });
                capture(profile+"-recording-"+state,true);
            }
            stopRecordingToTrackStopped(WAIT,R.id.finish_button);finishStoppedTrack(WAIT);
        }
        TestEnvironmentUtils.stopTrackRecordingService();
    }

    private void history(String profile) throws Exception {
        ContentProviderUtils store = new ContentProviderUtils(context);
        Track.Id id = new Track.Id(System.currentTimeMillis());
        Instant start = Instant.parse("2026-11-01T02:00:00Z");List<TrackPoint> points = new ArrayList<>();
        points.add(new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL,start));
        for(int s=1;s<=70;s++) {
            if(s==31) {points.add(new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL,start.plusSeconds(s)));continue;}
            if(s==40) {points.add(new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL,start.plusSeconds(s)));continue;}
            if(s>31 && s<40) continue;
            TrackPoint p = new TrackPoint(null,TrackPoint.Type.TRACKPOINT,
                    new Position(start.plusSeconds(s),1.3+s/100000d,103.8,Distance.of(3),Altitude.WGS84.of(10+s/5d),null,null,Speed.of(2.5f)),
                    null,HeartRate.of(90+s%60),null,null,null,null);points.add(p);
        }
        points.add(new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL,start.plusSeconds(71)));
        Track track = new Track(id,UUID.randomUUID(),"Gardenia · heart-rate test session","Synthetic samples, including a pause and reconnect","",ActivityType.UNKNOWN,ZoneOffset.ofHours(8),new TrackStatisticsUpdater(points).getTrackStatistics());
        store.insertTrack(track);store.bulkInsertTrackPoint(points,id);HeartRateRepository raw=new HeartRateRepository(context);
        for(TrackPoint point:points) if(point.heartRate()!=null) raw.insert(id,new HeartRate(point.heartRate().value(),new HeartRateSample(point.getTime(),"test-device","Synthetic strap",true,null,List.of(1024)).captured(point.getTime(),"test-device","Synthetic strap",point.getTime().isBefore(start.plusSeconds(40)) ? "before" : "after")));
        try(ActivityScenario<TrackRecordedActivity> scenario=ActivityScenario.launch(new Intent(context,TrackRecordedActivity.class).putExtra(TrackRecordedActivity.EXTRA_TRACK_ID,id))) {
            waitUntilDisplayed(allOf(withId(R.id.stats_name_value),withText(track.name())),WAIT);capture(profile+"-history",true);
            onView(withId(R.id.track_detail_activity_tablayout)).perform(selectTabAtIndex(2));waitForTab(2);
            var values = raw.read(id);double min=values.stream().mapToDouble(HeartRate::value).min().orElseThrow();
            double max=values.stream().mapToDouble(HeartRate::value).max().orElseThrow();
            double average=values.stream().mapToDouble(HeartRate::value).average().orElseThrow();
            String expectedSummary=context.getString(R.string.hr_summary,min,average,max);
            waitForResumedViewCondition(allOf(withId(R.id.heart_rate_summary),isDisplayed()),v -> ((TextView)v).getText().toString().equals(expectedSummary),"raw HR statistics",WAIT);
            capture(profile+"-time-chart",true);
            onView(withId(R.id.track_detail_activity_tablayout)).perform(selectTabAtIndex(3));waitForTab(3);
            waitUntilDisplayed(allOf(withId(R.id.chart_view),isDisplayed()),WAIT);capture(profile+"-distance-chart",true);
        }
    }

    private void settings(String profile) throws Exception {
        try(ActivityScenario<SettingsActivity> scenario=ActivityScenario.launch(new Intent(context,SettingsActivity.class).putExtra(SettingsActivity.FRAGMENT_KEY,context.getString(R.string.settings_sensors_key)))) {
            waitUntilDisplayed(withText(R.string.sensor_state_heart_rate),WAIT);capture(profile+"-settings",false);
            scenario.onActivity(activity -> {
                SensorsSettingsFragment fragment=(SensorsSettingsFragment)activity.getSupportFragmentManager().findFragmentById(R.id.settings_fragment);
                fragment.onDisplayPreferenceDialog(fragment.findPreference(context.getString(R.string.settings_sensor_bluetooth_heart_rate_key)));
            });
            waitUntilDisplayed(withText(R.string.sensor_scan_again),WAIT);capture(profile+"-scan",false);
            scenario.onActivity(activity -> {
                BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog dialog=(BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog)activity.getSupportFragmentManager().findFragmentByTag(SensorsSettingsFragment.class.getSimpleName());
                var adapter=dialog.getDeviceAdapter();adapter.add("test-1","Synthetic chest strap",-49,true);adapter.add("test-2","Synthetic optical sensor",-68,false);adapter.notifyDataSetChanged();
            });
            capture(profile+"-scan-fixture",true);
        }
    }

    private void waitForTab(int index) {
        waitForResumedViewCondition(withId(R.id.track_detail_activity_tablayout),v -> withSelectedTab(index).matches(v),"selected chart tab",WAIT);
        waitForResumedViewCondition(withId(R.id.track_detail_activity_view_pager),v -> withViewPagerCurrentItem(index).matches(v),"selected chart page",WAIT);
    }

    private void capture(String name,boolean synthetic) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();android.os.SystemClock.sleep(150);
        Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);
        assertTrue(image.getWidth()>=600);assertTrue(image.getHeight()>=1000);
        try(FileOutputStream file=new FileOutputStream(new File(output,name+".png"))) {assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,file));}
        Activity activity=ActivityTestUtils.waitForResumedActivity(Activity.class,WAIT);
        int night=activity.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        int widthDp=activity.getResources().getConfiguration().screenWidthDp;
        assertEquals(name.contains("-dark-") ? android.content.res.Configuration.UI_MODE_NIGHT_YES : android.content.res.Configuration.UI_MODE_NIGHT_NO,night);
        captures.put(new JSONObject().put("nightMode",night).put("widthDp",widthDp).put("file",name+".png").put("width",image.getWidth()).put("height",image.getHeight()).put("syntheticHeartRateOrDevices",synthetic));image.recycle();
    }
    private void assertVisibleText(View view) {
        assertNotNull(view);Rect bounds=new Rect();assertTrue(view.getGlobalVisibleRect(bounds));assertTrue(bounds.width()>0);assertTrue(bounds.height()>0);
        if(view instanceof TextView text && text.getLayout()!=null) for(int line=0;line<text.getLineCount();line++) assertEquals("Truncated text",0,text.getLayout().getEllipsisCount(line));
    }
    private static String shell(String command) throws Exception {
        try(ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
            java.io.FileInputStream input=new java.io.FileInputStream(fd.getFileDescriptor())) {return new String(input.readAllBytes(),StandardCharsets.UTF_8);}
    }
    private static void restoreOverride(String command,String original) throws Exception {
        var match=java.util.regex.Pattern.compile("Override (?:size|density): ([^\\r\\n]+)").matcher(original);
        shell("wm "+command+" "+(match.find() ? match.group(1).trim() : "reset"));
    }
}
