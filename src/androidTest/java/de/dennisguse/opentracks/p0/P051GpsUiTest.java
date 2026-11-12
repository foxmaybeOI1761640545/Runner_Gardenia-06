package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;
import org.junit.*;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.p0.data.SessionStore;
import de.dennisguse.opentracks.p0.recording.*;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.*;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;

/** Real native dashboard fed through the GPS recording path; no production preview data. */
public class P051GpsUiTest {
    @Rule public final GrantPermissionRule permissions = TestUtil.createScanPermissionRule();
    @Rule public final ServiceTestRule rule = new ServiceTestRule();
    private final Context context = ApplicationProvider.getApplicationContext();
    private TrackRecordingService core;
    private UnitSystem unit;
    private ThemeUtils.Family family;
    private String gps, heart, rate;
    private DashboardMetricLayout layout;
    private final org.json.JSONArray captures = new org.json.JSONArray();
    private void main(Runnable action) {InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    @Before public void before() throws Exception {
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();
        new SessionStore(context).save(RecordingSession.ready());
        unit = PreferencesUtils.getUnitSystem(); family = PreferencesUtils.getGardeniaTheme();
        layout = PreferencesUtils.getDashboardMetricLayout(ThemeUtils.Family.PRECISION);
        gps = PreferencesUtils.getString(R.string.settings_sensor_gps_key, "INTERNAL");
        heart = PreferencesUtils.getString(R.string.settings_sensor_bluetooth_heart_rate_key, "NONE");
        rate = PreferencesUtils.getString(R.string.stats_rate_key, context.getString(R.string.stats_rate_default));
        PreferencesUtils.setString(R.string.settings_sensor_gps_key, "NONE");
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key, "NONE");
        PreferencesUtils.setGardeniaTheme(ThemeUtils.Family.PRECISION);
        PreferencesUtils.resetDashboardMetricLayout(ThemeUtils.Family.PRECISION);
        PreferencesUtils.setString(R.string.stats_rate_key, R.string.stats_rate_pace);
        core = ((TrackRecordingService.Binder) rule.bindService(new Intent(context, TrackRecordingService.class))).getService();
    }
    @After public void after() {
        if (core != null) main(() -> {core.cancelGpsAcquisition(); core.endCurrentTrack(); core.stopIdleSensors();});
        new SessionStore(context).save(RecordingSession.ready());
        PreferencesUtils.setUnit(unit); PreferencesUtils.setGardeniaTheme(family);
        PreferencesUtils.setDashboardMetricLayout(ThemeUtils.Family.PRECISION, layout);
        PreferencesUtils.setString(R.string.stats_rate_key, rate);
        PreferencesUtils.setString(R.string.settings_sensor_gps_key, gps);
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key, heart);
    }
    private void fix(double meters, float speed) {
        main(() -> {
            Location location = new Location("gps");
            location.setLatitude(1.3 + meters / 111_320.0); location.setLongitude(103.8);
            location.setAccuracy(2); location.setSpeed(speed); location.setTime(System.currentTimeMillis());
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            core.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(location);
        });
        SystemClock.sleep(100);
    }
    private DashboardState state(ActivityScenario<P0Activity> screen, boolean stationary) {
        AtomicReference<DashboardState> state = new AtomicReference<>();
        long end = SystemClock.uptimeMillis() + 10000;
        do {
            screen.onActivity(activity -> state.set(DashboardState.from(context,
                    new ViewModelProvider(activity).get(P0RecordingViewModel.class).snapshot().getValue())));
            if (state.get().currentGps() != null && (stationary ? state.get().pace().equals("—")
                    : !state.get().pace().equals("—"))) return state.get();
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < end);
        fail("Dashboard did not render validated GPS motion state: " + state.get()); return null;
    }
    private void capture(ActivityScenario<P0Activity> screen, String name, boolean stationary) throws Exception {
        for (boolean showSpeed : new boolean[]{false, true}) {
            main(() -> PreferencesUtils.setString(R.string.stats_rate_key,
                    showSpeed ? R.string.stats_rate_speed : R.string.stats_rate_pace));
            DashboardState previous = state(screen, stationary);
            fix((previous.currentGps().latitude() - 1.3) * 111_320.0,
                    (float) previous.currentGps().speed().toMPS());
            DashboardState value = state(screen, stationary);
            String expected = showSpeed ? value.speed() : value.pace();
            AtomicReference<String> displayed = new AtomicReference<>();
            long deadline = SystemClock.uptimeMillis() + 10000;
            do {
                screen.onActivity(activity -> displayed.set(((TextView) activity.findViewById(R.id.p02_pace_value)).getText().toString()));
                if (expected.equals(displayed.get())) break;
                SystemClock.sleep(50);
            } while (SystemClock.uptimeMillis() < deadline);
            assertEquals("Native GPS rate readout", expected, displayed.get());
            screen.onActivity(activity -> assertTrue(activity.hasWindowFocus()));
            String mode = showSpeed ? "speed" : "pace";
            String filename = "p051-" + name + "-" + mode + ".png";
            P0UiCapture.save(filename.substring(0, filename.length() - 4));
            String output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
            File directory = new File(output != null ? new File(output) : new File(context.getExternalMediaDirs()[0], "additional_test_output"), "p0-ui");
            byte[] png = Files.readAllBytes(new File(directory, filename).toPath());
            String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(png));
            captures.put(new org.json.JSONObject().put("file", filename).put("sha256", hash)
                    .put("stationary", stationary).put("unit", PreferencesUtils.getUnitSystem().name())
                    .put("mode", mode).put("displayed", displayed.get())
                    .put("speed", value.speed()).put("pace", value.pace()).put("native", true));
        }
        main(() -> PreferencesUtils.setString(R.string.stats_rate_key, R.string.stats_rate_pace));
    }
    @Test public void realGpsStationaryAndSlowMotionRenderConsistentlyInMetricAndImperial() throws Exception {
        for (UnitSystem selected : new UnitSystem[]{UnitSystem.METRIC, UnitSystem.IMPERIAL_FEET}) {
            PreferencesUtils.setUnit(selected);
            new SessionStore(context).save(RecordingSession.ready());
            try (ActivityScenario<P0Activity> screen = ActivityScenario.launch(P0Activity.class)) {
                main(() -> {core.beginGpsAcquisition(); P0GpsPreparationTest.fixture(core);});
                main(() -> {
                    Location invalid = new Location("gps");
                    invalid.setLatitude(1.3); invalid.setLongitude(103.8); invalid.setAccuracy(2); invalid.setSpeed(3);
                    core.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(invalid);
                });
                assertFalse("Live P0 fixes without monotonic metadata must not create a workout", core.isRecording());
                P0GpsPreparationTest.ready(core);
                assertTrue(core.isRecording());
                double confirmedLatitude = core.getGpsPreview().latitude();
                main(() -> {
                    Location invalid = new Location("gps");
                    invalid.setLatitude(4.3); invalid.setLongitude(103.8); invalid.setAccuracy(2); invalid.setSpeed(3);
                    core.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(invalid);
                });
                assertEquals("Active P0 also rejects an untimed fix", confirmedLatitude, core.getGpsPreview().latitude(), 0);
                for (int i = 0; i < 5; i++) fix(i % 2 * 0.2, 0.235f);
                DashboardState stopped = state(screen, true);
                assertTrue(stopped.speed().startsWith("0.0")); assertEquals("—", stopped.pace());
                assertEquals(0, new de.dennisguse.opentracks.data.ContentProviderUtils(context).getTrack(core.getRecordingStatus().trackId()).statistics().totalDistance().toM(), 0);
                capture(screen, selected.name() + "-stationary", true);
                for (int i = 1; i <= 12; i++) fix(i * 0.5, 0.5f);
                DashboardState walking = state(screen, false);
                assertFalse(walking.speed().startsWith("0.0")); assertFalse(walking.pace().contains("71:01"));
                assertEquals(0.5, walking.currentGps().speed().toMPS(), 0.00001);
                capture(screen, selected.name() + "-slow-moving", false);
                for (int i = 0; i < 5; i++) fix(6, 0);
                double distance = new de.dennisguse.opentracks.data.ContentProviderUtils(context).getTrack(core.getRecordingStatus().trackId()).statistics().totalDistance().toM();
                for (int i = 0; i < 5; i++) fix(6 + i % 2 * 0.2, 0.2f);
                assertEquals(distance, new de.dennisguse.opentracks.data.ContentProviderUtils(context).getTrack(core.getRecordingStatus().trackId()).statistics().totalDistance().toM(), 0);
                assertTrue(state(screen, true).speed().startsWith("0.0"));
                capture(screen, selected.name() + "-stopped-again", true);
                screen.onActivity(activity -> new ViewModelProvider(activity).get(P0RecordingViewModel.class).finish());
            }
            main(core::stopIdleSensors);
        }
        String output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
        File directory = new File(output != null ? new File(output) : new File(context.getExternalMediaDirs()[0], "additional_test_output"), "p0-ui");
        String receipt = new org.json.JSONObject()
                .put("sourceCommit", InstrumentationRegistry.getArguments().getString("buildCommit", "local"))
                .put("api", android.os.Build.VERSION.SDK_INT).put("screenshots", captures).put("passed", true).toString(2);
        try (var outputStream = new java.io.FileOutputStream(new File(directory, "p051-gps-ui.json"))) {
            outputStream.write(receipt.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
