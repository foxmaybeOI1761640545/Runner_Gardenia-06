package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.action.ViewActions.click;
import static org.hamcrest.Matchers.containsString;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;
import org.junit.*;
import java.util.concurrent.TimeUnit;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.p0.ble.*;
import de.dennisguse.opentracks.sensors.driver.*;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeSensorPreference;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

public class P0SensorsTest {
    private static final String ADDRESS="AA:BB:CC:DD:EE:FF";
    @Rule public final ServiceTestRule serviceRule=ServiceTestRule.withTimeout(10,TimeUnit.SECONDS);
    @Rule public final GrantPermissionRule permissions=GrantPermissionRule.grant(
            Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.POST_NOTIFICATIONS);
    private final Context context=ApplicationProvider.getApplicationContext();
    private String selected,gpsSelection;
    private TrackRecordingService service;
    @Before public void before() {
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();
        selected=HeartRateCommands.selected();PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
        gpsSelection=PreferencesUtils.getString(R.string.settings_sensor_gps_key,SensorType.INTERNAL.getPreferenceValue());
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,SensorType.INTERNAL.getPreferenceValue());
    }
    @After public void after() {
        if(service!=null)InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{service.endCurrentTrack();service.stopIdleSensors();});
        TestEnvironmentUtils.stopTrackRecordingService();
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,selected);
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,gpsSelection);
    }
    private P0SensorsFragment fragment(P0SensorsActivity activity) {
        activity.getSupportFragmentManager().executePendingTransactions();
        return (P0SensorsFragment)activity.getSupportFragmentManager().findFragmentById(R.id.p0_sensor_container);
    }
    @Test public void p0SettingsExposeOnlyHeartRateAndForceStandardServiceDiscovery() throws Exception {
        try(ActivityScenario<P0SensorsActivity> scenario=ActivityScenario.launch(P0SensorsActivity.class)) {
            scenario.onActivity(activity->{
                P0SensorsFragment settings=fragment(activity);
                assertNull(settings.findPreference(context.getString(R.string.settings_sensor_gps_key)));
                var preference=(P0HeartRatePreference)settings.findPreference(context.getString(R.string.settings_sensor_bluetooth_heart_rate_key));
                var arguments=preference.createInstance().getArguments();assertNotNull(arguments);
                assertTrue(arguments.getBoolean("forceServiceFilter"));
                var uuids=arguments.<android.os.ParcelUuid>getParcelableArrayList("bluetoothUUID");assertNotNull(uuids);
                assertEquals(1,uuids.size());assertEquals(HeartRateBluetooth.HEARTRATE.serviceUUID(),uuids.get(0).getUuid());
            });
            P0UiCapture.save("p0-heart-rate-settings");
        }
    }
    @Test public void scanDialogRemembersSelectedDeviceWithoutStartingAnUnrequestedConnection() throws Exception {
        try(ActivityScenario<P0SensorsActivity> scenario=ActivityScenario.launch(P0SensorsActivity.class)) {
            scenario.onActivity(activity->{
                P0SensorsFragment settings=fragment(activity);
                settings.onDisplayPreferenceDialog(settings.findPreference(context.getString(R.string.settings_sensor_bluetooth_heart_rate_key)));
                activity.getSupportFragmentManager().executePendingTransactions();
                var dialog=(BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog)activity.getSupportFragmentManager().findFragmentByTag("p0-heart-rate-scan");
                assertNotNull(dialog);dialog.getDeviceAdapter().add(ADDRESS,"Synthetic SIG strap",-45,true);dialog.getDeviceAdapter().notifyDataSetChanged();
            });
            onView(withText(R.string.sensor_scan_again)).check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            P0UiCapture.save("p0-heart-rate-scan-synthetic");
            onView(withText(containsString(ADDRESS))).perform(click());
            assertEquals(ADDRESS,HeartRateCommands.selected());
            scenario.onActivity(activity->{
                var model=new androidx.lifecycle.ViewModelProvider(activity).get(de.dennisguse.opentracks.p0.recording.P0RecordingViewModel.class);
                assertEquals(BleConnectionSession.State.DISCONNECTED,model.snapshot().getValue().preview().status().state());
            });
        }
    }
    @Test public void heartRateCommandsArePackageAddressAndMeasurementScoped() {
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,ADDRESS);
        Intent command=HeartRateCommands.intent(context,BluetoothDriver.DISCONNECT_SUFFIX);
        assertEquals(context.getPackageName(),command.getPackage());assertEquals(ADDRESS,command.getStringExtra("address"));
        assertEquals(HeartRateBluetooth.HEARTRATE.measurementUUID().toString(),command.getStringExtra("measurement"));
        assertTrue(HeartRateCommands.hasSelection());
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,"not-a-device");assertFalse(HeartRateCommands.hasSelection());
    }
    @Test public void previewToRecordingRefreshesGpsAndIdleStopCannotEndAnActiveWorkout() throws Exception {
        service=((TrackRecordingService.Binder)serviceRule.bindService(new Intent(context,TrackRecordingService.class))).getService();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            service.tryStartSensors();assertTrue(service.areSensorsStarted());
            var manager=service.getTrackPointCreator().getSensorManager();var previousGps=manager.getGpsHandler().driver;
            var id=service.startNewTrack();assertNotNull(id);assertSame(manager,service.getTrackPointCreator().getSensorManager());
            assertNotSame(previousGps,manager.getGpsHandler().driver);
            service.stopIdleSensors();assertTrue(service.isRecording());assertTrue(service.areSensorsStarted());
            service.endCurrentTrack();service.stopIdleSensors();assertFalse(service.areSensorsStarted());assertNull(service.getHeartRatePreview().value());
        });
    }
}
