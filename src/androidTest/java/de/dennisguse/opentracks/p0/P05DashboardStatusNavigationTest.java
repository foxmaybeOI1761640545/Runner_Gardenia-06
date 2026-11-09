package de.dennisguse.opentracks.p0;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.Settings;

import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.p0.app.P0Activity;
import de.dennisguse.opentracks.p0.app.P0SensorsActivity;
import de.dennisguse.opentracks.p0.data.SessionStore;
import de.dennisguse.opentracks.p0.data.WorkoutRepository;
import de.dennisguse.opentracks.p0.recording.P0RecordingViewModel;
import de.dennisguse.opentracks.p0.recording.RecordingSession;
import de.dennisguse.opentracks.sensors.driver.BleConnectionSession;
import de.dennisguse.opentracks.sensors.driver.HeartRateBluetooth;
import de.dennisguse.opentracks.sensors.driver.SensorType;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** Real Activities/service/database; synthetic GPS/SIG input is explicitly test-only. */
public class P05DashboardStatusNavigationTest {
    @Rule public final GrantPermissionRule permissions = TestUtil.createScanPermissionRule();
    @Rule public final ServiceTestRule serviceRule = new ServiceTestRule();

    private final Context context = ApplicationProvider.getApplicationContext();
    private final String[] animations = {"window_animation_scale", "transition_animation_scale", "animator_duration_scale"};
    private final String[] oldAnimations = new String[animations.length];
    private TrackRecordingService core;
    private ThemeUtils.Family family;
    private String gps, heart, sampling, distance, activityType;

    private void main(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    @Before public void before() throws Exception {
        family = PreferencesUtils.getGardeniaTheme();
        gps = PreferencesUtils.getString(R.string.settings_sensor_gps_key, SensorType.INTERNAL.getPreferenceValue());
        heart = PreferencesUtils.getString(R.string.settings_sensor_bluetooth_heart_rate_key, SensorType.NONE.getPreferenceValue());
        sampling = PreferencesUtils.getString(R.string.min_sampling_interval_key, "0");
        distance = PreferencesUtils.getString(R.string.recording_distance_interval_key, "5");
        activityType = PreferencesUtils.getDefaultActivityTypeLocalized();
        for (int i = 0; i < animations.length; i++) {
            oldAnimations[i] = SystemThemeTestUtils.shell("settings get global " + animations[i]);
            SystemThemeTestUtils.shell("settings put global " + animations[i] + " 0");
        }
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();
        new SessionStore(context).save(RecordingSession.ready());
        PreferencesUtils.setGardeniaTheme(ThemeUtils.Family.PRECISION);
        PreferencesUtils.setString(R.string.settings_sensor_gps_key, SensorType.NONE.getPreferenceValue());
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key, SensorType.NONE.getPreferenceValue());
        core = ((TrackRecordingService.Binder) serviceRule.bindService(new Intent(context, TrackRecordingService.class))).getService();
    }

    @After public void after() throws Exception {
        main(() -> {
            Set<Activity> activities = new HashSet<>();
            for (Stage stage : Stage.values()) if (stage != Stage.DESTROYED)
                activities.addAll(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage));
            for (Activity activity : activities)
                if (activity instanceof P0Activity || activity instanceof P0SensorsActivity) activity.finish();
            if (core != null) {
                core.cancelGpsAcquisition();
                core.endCurrentTrack();
                core.stopIdleSensors();
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        if (core != null) serviceRule.unbindService();
        TestEnvironmentUtils.stopTrackRecordingService();
        TestEnvironmentUtils.awaitTrackRecordingServiceDestroyed();
        new SessionStore(context).save(RecordingSession.ready());
        TestEnvironmentUtils.deleteAllTracks();
        PreferencesUtils.setGardeniaTheme(family);
        PreferencesUtils.setString(R.string.settings_sensor_gps_key, gps);
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key, heart);
        PreferencesUtils.setString(R.string.min_sampling_interval_key, sampling);
        PreferencesUtils.setString(R.string.recording_distance_interval_key, distance);
        PreferencesUtils.setDefaultActivityLocalized(activityType);
        for (int i = 0; i < animations.length; i++)
            SystemThemeTestUtils.shell(oldAnimations[i].equals("null")
                    ? "settings delete global " + animations[i]
                    : "settings put global " + animations[i] + " " + oldAnimations[i]);
    }

    private Activity await(Class<? extends Activity> type, Predicate<Activity> condition) {
        AtomicReference<Activity> found = new AtomicReference<>();
        long deadline = SystemClock.uptimeMillis() + 15000;
        do {
            main(() -> {
                for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
                    if (type.isInstance(activity) && activity.hasWindowFocus() && condition.test(activity)) found.set(activity);
            });
            if (found.get() != null) return found.get();
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Status destination/state did not settle: " + type.getSimpleName());
    }

    private P0RecordingViewModel.Snapshot snapshot(Activity activity) {
        return new ViewModelProvider((P0Activity) activity).get(P0RecordingViewModel.class).snapshot().getValue();
    }

    private Intent clickGpsWithoutLeavingApp() {
        AtomicReference<Intent> sent = new AtomicReference<>();
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                if (!Settings.ACTION_LOCATION_SOURCE_SETTINGS.equals(intent.getAction())) return null;
                sent.set(new Intent(intent));
                return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);
            }
        };
        instrumentation.addMonitor(monitor);
        try {
            onView(withId(R.id.p04_gps_status)).perform(click());
            assertNotNull("GPS status must open Android location settings", sent.get());
            assertEquals(Settings.ACTION_LOCATION_SOURCE_SETTINGS, sent.get().getAction());
            return sent.get();
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    private void visitDeviceManagementAndReturn() {
        onView(withId(R.id.p0_devices)).perform(click());
        await(P0SensorsActivity.class, activity -> activity.findViewById(R.id.p0_sensor_container).isShown());
        onView(withId(R.id.p04_nav_run)).perform(click());
        await(P0Activity.class, activity -> activity.findViewById(R.id.p04_gps_status).isShown());
    }

    @Test public void compactStatusEntriesOpenRealDestinationsWithoutCreatingIdleWorkout() throws Exception {
        try (ActivityScenario<P0Activity> screen = ActivityScenario.launch(P0Activity.class)) {
            await(P0Activity.class, activity -> snapshot(activity).session().phase() == RecordingSession.Phase.IDLE);
            assertTrue(new WorkoutRepository(context).list().isEmpty());
            assertFalse(core.isRecording());
            assertFalse(core.isAcquiringGps());
            clickGpsWithoutLeavingApp();
            visitDeviceManagementAndReturn();
            await(P0Activity.class, activity -> snapshot(activity).session().phase() == RecordingSession.Phase.IDLE);
            assertTrue(new WorkoutRepository(context).list().isEmpty());
            assertNull(TrackRecordingService.activeTrackId(context));
            assertFalse(core.isRecording());
            assertFalse(core.isAcquiringGps());
            P0UiCapture.save("p05-status-navigation-real-idle");
        }
    }

    @Test public void statusNavigationPreservesRecordingConnectionAndPausedJournal() throws Exception {
        try (ActivityScenario<P0Activity> screen = ActivityScenario.launch(P0Activity.class)) {
            onView(withId(R.id.p0_start)).perform(click());
            await(P0Activity.class, activity -> snapshot(activity).session().phase() == RecordingSession.Phase.ACQUIRING_GPS);
            main(() -> P0GpsPreparationTest.fixture(core));
            P0GpsPreparationTest.ready(core);
            await(P0Activity.class, activity -> snapshot(activity).session().phase() == RecordingSession.Phase.RECORDING);
            long id = core.getRecordingStatus().trackId().id();
            Object pointCreator = core.getTrackPointCreator();
            Object gpsHandler = core.getTrackPointCreator().getSensorManager().getGpsHandler();
            main(() -> {
                var handler = core.getTrackPointCreator().getSensorManager().getHeartRateHandler();
                handler.onConnected("AA:BB:CC:DD:EE:FF", "Synthetic status-navigation strap");
                handler.onConnectionStateChanged(BleConnectionSession.State.CONNECTED);
                var characteristic = new android.bluetooth.BluetoothGattCharacteristic(HeartRateBluetooth.HEARTRATE.measurementUUID(), 0, 0);
                handler.onDataReceived(new HeartRateBluetooth().parsePayload(HeartRateBluetooth.HEARTRATE,
                        "Synthetic status-navigation strap", characteristic, new byte[]{6, (byte) 148}));
            });
            String connection = core.getHeartRatePreview().value().sample().connectionId();
            clickGpsWithoutLeavingApp();
            visitDeviceManagementAndReturn();
            await(P0Activity.class, activity -> snapshot(activity).session().phase() == RecordingSession.Phase.RECORDING);
            assertTrue(core.isRecording());
            assertEquals(id, core.getRecordingStatus().trackId().id());
            assertSame(pointCreator, core.getTrackPointCreator());
            assertSame(gpsHandler, core.getTrackPointCreator().getSensorManager().getGpsHandler());
            assertEquals(connection, core.getHeartRatePreview().value().sample().connectionId());
            assertEquals(1, new WorkoutRepository(context).list().size());
            P0UiCapture.save("p05-status-navigation-real-recording-synthetic-input");

            onView(withId(R.id.p0_pause)).perform(click());
            await(P0Activity.class, activity -> snapshot(activity).session().phase() == RecordingSession.Phase.PAUSED);
            assertFalse(core.isRecording());
            assertEquals(Long.valueOf(id), new SessionStore(context).recover().trackId());
            clickGpsWithoutLeavingApp();
            visitDeviceManagementAndReturn();
            await(P0Activity.class, activity -> snapshot(activity).session().phase() == RecordingSession.Phase.PAUSED);
            assertFalse(core.isRecording());
            assertEquals(Long.valueOf(id), new SessionStore(context).recover().trackId());
            assertEquals(1, new WorkoutRepository(context).list().size());
            P0UiCapture.save("p05-status-navigation-real-paused-synthetic-input");
        }
    }
}
