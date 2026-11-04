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
        String originalTheme = PreferencesUtils.getString(R.string.theme_key,context.getString(R.string.theme_default));
        String overrideSize = shell("wm size");String overrideDensity = shell("wm density");
        String headsUp = shell("settings get global heads_up_notifications_enabled").trim();
        String[] scales={"window_animation_scale","transition_animation_scale","animator_duration_scale"};
        java.util.Map<String,String> originalScales=new java.util.HashMap<>();
        for(String scale:scales) originalScales.put(scale,shell("settings get global "+scale).trim());
        android.bluetooth.BluetoothAdapter radio = context.getSystemService(android.bluetooth.BluetoothManager.class).getAdapter();
        boolean radioWasEnabled = radio != null && radio.isEnabled();
        String configured = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
        output = new File(configured != null ? new File(configured) : new File(context.getExternalMediaDirs()[0],"additional_test_output"),"heart-rate-ui");
        assertTrue(output.isDirectory() || output.mkdirs());
        try {
            // UI screenshots exercise the real offline dialog. This emulator has no physical HR radio;
            // its virtual Bluetooth stack aborts on real scan requests. Transport behavior is tested with fakes.
            shell("settings put global heads_up_notifications_enabled 0");
            for(String scale:scales) shell("settings put global "+scale+" 0");
            if (radioWasEnabled) shell("svc bluetooth disable");
            long deadline = android.os.SystemClock.uptimeMillis()+5000;
            while (radio != null && radio.isEnabled() && android.os.SystemClock.uptimeMillis()<deadline) android.os.SystemClock.sleep(50);
            assertTrue("UI radio must be explicitly offline",radio == null || !radio.isEnabled());
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
                    .put("timezone",java.util.TimeZone.getDefault().getID()).put("radioScenario","Offline emulator; scan device rows are test fixtures").put("screenshots",captures);
            try (FileOutputStream stream = new FileOutputStream(new File(output,"manifest.json"))) {
                stream.write(manifest.toString(2).getBytes(StandardCharsets.UTF_8));
            }
            assertEquals(40,captures.length());
        } finally {
            TestEnvironmentUtils.stopTrackRecordingService();
            PreferencesUtils.setString(R.string.theme_key,originalTheme);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(PreferencesUtils::applyNightMode);
            restoreOverride("size",overrideSize);restoreOverride("density",overrideDensity);
            if (radioWasEnabled) shell("svc bluetooth enable");
            shell(headsUp.equals("null") ? "settings delete global heads_up_notifications_enabled" : "settings put global heads_up_notifications_enabled "+headsUp);
            for(String scale:scales) shell(originalScales.get(scale).equals("null") ? "settings delete global "+scale : "settings put global "+scale+" "+originalScales.get(scale));
        }
    }

    private void recording(String profile) throws Exception {
        android.content.SharedPreferences preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        String introductionKey = context.getString(R.string.show_introduction_screen_key);
        boolean hadIntroduction = preferences.contains(introductionKey);
        boolean originalIntroduction = preferences.getBoolean(introductionKey,true);
        // Legacy UI regression; the P0 launcher is exercised by P0ActivitySmokeTest.
        if (profile.equals("phone-light")) preferences.edit().remove(introductionKey).commit();
        else PreferencesUtils.setShowIntroduction(profile.endsWith("light"));
        try (ActivityScenario<TrackListActivity> launcher = ActivityScenario.launch(TrackListActivity.class)) {
            Activity home = ActivityTestUtils.waitForResumedActivity(TrackListActivity.class,WAIT);
            waitUntilDisplayed(withId(R.id.track_list),WAIT); capture(profile+"-home",false);
            startRecordingFromTrackList(WAIT);waitUntilDisplayed(withId(R.id.heart_rate_panel),WAIT);
            if (boundService == null) boundService = ((TrackRecordingService.Binder)serviceRule.bindService(new Intent(context,TrackRecordingService.class))).getService();
            TrackRecordingService service = boundService;
            waitForResumedViewCondition(withId(R.id.heart_rate_panel),view -> {
                RecordingData data = service.getRecordingDataObservable().getValue();
                var status = service.getRecordingStatusObservable().getValue();
                return data != null && data.track() != null && status != null && data.track().id().equals(status.trackId());
            },"current recording data",WAIT);
            Activity ready=ActivityTestUtils.waitForResumedActivity(TrackRecordingActivity.class,WAIT);
            waitForResumedViewCondition(withId(R.id.heart_rate_panel),view -> ready.hasWindowFocus(),"focused recording window",WAIT);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            android.os.SystemClock.sleep(500);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(service::stopUpdateRecordingData);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
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
            InstrumentationRegistry.getInstrumentation().runOnMainSync(home::finish);
        } finally {
            if (hadIntroduction) preferences.edit().putBoolean(introductionKey,originalIntroduction).commit();
            else preferences.edit().remove(introductionKey).commit();
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
            onView(withText(R.string.sensor_scan_again)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
            capture(profile+"-scan",false);
            scenario.onActivity(activity -> {
                BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog dialog=(BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog)activity.getSupportFragmentManager().findFragmentByTag(SensorsSettingsFragment.class.getSimpleName());
                var adapter=dialog.getDeviceAdapter();adapter.add("test-1","Synthetic chest strap",-49,true);adapter.add("test-2","Synthetic optical sensor",-68,false);adapter.notifyDataSetChanged();
            });
            capture(profile+"-scan-fixture",true);
        }
    }

    private void waitForTab(int index) {
        waitForResumedViewCondition(withId(R.id.track_detail_activity_tablayout),v -> withSelectedTab(index).matches(v),"selected chart tab",WAIT);
        waitForResumedViewCondition(withId(R.id.track_detail_activity_view_pager),v -> withViewPagerCurrentItem(index).matches(v) && ((androidx.viewpager2.widget.ViewPager2)v).getScrollState()==androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE,"selected chart page",WAIT);
    }

    private void capture(String name,boolean synthetic) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Class<? extends Activity> expected = name.endsWith("-home") ? TrackListActivity.class
                : name.contains("-recording-") ? TrackRecordingActivity.class
                : name.contains("-settings") || name.contains("-scan") ? SettingsActivity.class : TrackRecordedActivity.class;
        Activity activity=ActivityTestUtils.waitForResumedActivity(expected,WAIT);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        java.util.concurrent.CountDownLatch frames = new java.util.concurrent.CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View decor=activity.getWindow().getDecorView();decor.invalidate();
            decor.postOnAnimation(() -> decor.postOnAnimation(frames::countDown));
        });
        assertTrue("UI did not render two frames",frames.await(5,java.util.concurrent.TimeUnit.SECONDS));
        android.os.SystemClock.sleep(500);
        Bitmap image=null;long deadline=android.os.SystemClock.uptimeMillis()+7000;
        long previousHash=Long.MIN_VALUE;int stable=0;
        do {
            if(image!=null) image.recycle();
            image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);
            long hash=contentHash(image);stable=hash==previousHash ? stable+1 : 0;previousHash=hash;
            boolean painted=!name.contains("-recording-") || recordingTextIsPainted(activity,image);
            if(stable>=2 && painted) break;
            android.os.SystemClock.sleep(200);
        } while(android.os.SystemClock.uptimeMillis()<deadline);
        assertTrue("Screenshot did not stabilize",stable>=2);
        if(name.contains("-recording-")) {
            assertTrue("Screenshot captured an unpainted recording page",recordingTextIsPainted(activity,image));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                TextView bpm=activity.findViewById(R.id.heart_rate_current);
                TextView state=activity.findViewById(R.id.heart_rate_status);
                assertEquals(name.endsWith("-connected") ? context.getString(R.string.hr_value,76f) : context.getString(R.string.hr_empty_value),bpm.getText().toString());
                int expectedState=name.endsWith("-connected") ? R.string.hr_connected : name.endsWith("-stale") ? R.string.hr_stale : R.string.hr_retry;
                assertTrue("Recording fixture changed before capture",state.getText().toString().contains(context.getString(expectedState)));
            });
        }
        assertTrue(image.getWidth()>=600);assertTrue(image.getHeight()>=1000);
        try(FileOutputStream file=new FileOutputStream(new File(output,name+".png"))) {assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,file));}
        int night=activity.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        int widthDp=activity.getResources().getConfiguration().screenWidthDp;
        assertEquals(name.contains("-dark-") ? android.content.res.Configuration.UI_MODE_NIGHT_YES : android.content.res.Configuration.UI_MODE_NIGHT_NO,night);
        captures.put(new JSONObject().put("nightMode",night).put("widthDp",widthDp).put("file",name+".png").put("width",image.getWidth()).put("height",image.getHeight()).put("syntheticHeartRateOrDevices",synthetic));image.recycle();
    }
    private long contentHash(Bitmap image) {
        long hash=1125899906842597L;
        for(int y=image.getHeight()/15;y<image.getHeight()*9/10;y+=7)
            for(int x=0;x<image.getWidth();x+=7) hash=31*hash+image.getPixel(x,y);
        return hash;
    }

    private boolean recordingTextIsPainted(Activity activity,Bitmap image) {
        Rect bounds=new Rect();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> activity.findViewById(R.id.heart_rate_current).getGlobalVisibleRect(bounds));
        java.util.Set<Integer> colors=new java.util.HashSet<>();
        for(int y=Math.max(0,bounds.top);y<Math.min(image.getHeight(),bounds.bottom);y+=3)
            for(int x=Math.max(0,bounds.left);x<Math.min(image.getWidth(),bounds.right);x+=3) colors.add(image.getPixel(x,y));
        return colors.size()>=6;
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
