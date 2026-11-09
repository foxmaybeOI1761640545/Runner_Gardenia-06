package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;
import org.junit.*;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.app.P0Activity;
import de.dennisguse.opentracks.p0.ble.HeartRateCommands;
import de.dennisguse.opentracks.p0.data.SessionStore;
import de.dennisguse.opentracks.p0.recording.RecordingSession;
import de.dennisguse.opentracks.sensors.HeartRateHandler;
import de.dennisguse.opentracks.sensors.driver.*;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** Real P0 screen/service/handler path with explicitly synthetic SIG notifications, not RF. */
public class P0HeartRateUiTest {
    @Rule public final ServiceTestRule rule=ServiceTestRule.withTimeout(10,TimeUnit.SECONDS);
    @Rule public final GrantPermissionRule permissions=GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.POST_NOTIFICATIONS);
    private final Context context=ApplicationProvider.getApplicationContext();
    private TrackRecordingService service;
    private String selected;
    private boolean enabled;
    @Before public void before() throws Exception {
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());
        selected=HeartRateCommands.selected();enabled=HeartRateCommands.connectionEnabled(context);
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
        service=((TrackRecordingService.Binder)rule.bindService(new Intent(context,TrackRecordingService.class))).getService();
    }
    @After public void after() {
        if(service!=null)InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{service.endCurrentTrack();service.stopIdleSensors();});
        new SessionStore(context).save(RecordingSession.ready());
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,selected);HeartRateCommands.setConnectionEnabled(context,enabled);
    }
    private void text(ActivityScenario<P0Activity> screen,String expected) {
        long end=SystemClock.uptimeMillis()+10000;AtomicReference<String> actual=new AtomicReference<>();
        do {screen.onActivity(activity->{
                StringBuilder text=new StringBuilder();
                for(int id:new int[]{R.id.p0_heart_rate,R.id.p02_heart_device,R.id.p02_heart_state,R.id.p02_heart_updated,R.id.p02_heart_reconnect})text.append(((TextView)activity.findViewById(id)).getText()).append("\n");
                actual.set(text.toString());
            });
            if(actual.get().contains(expected))return;SystemClock.sleep(50);
        }while(SystemClock.uptimeMillis()<end);
        assertTrue("Expected "+expected+" in "+actual.get(),actual.get().contains(expected));
    }
    @Test public void realPanelClearsStaleContactLostAndDisconnectedDataThenAcceptsNewSession() throws Exception {
        Instant base=Instant.parse("2026-11-04T02:00:00Z");AtomicReference<HeartRateHandler> handler=new AtomicReference<>();
        HeartRateBluetooth parser=new HeartRateBluetooth();
        var characteristic=new android.bluetooth.BluetoothGattCharacteristic(HeartRateBluetooth.HEARTRATE.measurementUUID(),0,0);
        try(ActivityScenario<P0Activity> screen=ActivityScenario.launch(P0Activity.class)) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
                service.getTrackPointCreator().setClock(base.toString());service.startNewTrack();
                HeartRateHandler target=service.getTrackPointCreator().getSensorManager().getHeartRateHandler();handler.set(target);
                target.onConnectionStateChanged(BleConnectionSession.State.CONNECTED);target.onConnected("AA:BB:CC:DD:EE:FF","Synthetic SIG strap");
                target.onDataReceived(parser.parsePayload(HeartRateBluetooth.HEARTRATE,"Synthetic SIG strap",characteristic,new byte[]{6,76}));
            });
            text(screen,context.getString(R.string.hr_value,76f));text(screen,"Synthetic SIG strap");P0UiCapture.save("p0-live-heart-rate-synthetic");
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->service.getTrackPointCreator().setClock(base.plusSeconds(6).toString()));
            text(screen,context.getString(R.string.hr_stale));text(screen,"--");P0UiCapture.save("p0-stale-heart-rate-synthetic");
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->handler.get().onDataReceived(parser.parsePayload(HeartRateBluetooth.HEARTRATE,"Synthetic SIG strap",characteristic,new byte[]{4,76})));
            text(screen,context.getString(R.string.hr_contact_lost));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{handler.get().onConnectionLost();handler.get().onConnectionStateChanged(BleConnectionSession.State.RECONNECT_WAIT);});
            text(screen,context.getString(R.string.hr_retry));text(screen,"--");
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
                handler.get().onConnected("AA:BB:CC:DD:EE:FF","Synthetic SIG strap");handler.get().onConnectionStateChanged(BleConnectionSession.State.CONNECTED);
                assertNull(handler.get().getStatus(base.plusSeconds(6)).lastReceived());assertFalse(handler.get().getStatus(base.plusSeconds(6)).contactLost());
                handler.get().onDataReceived(parser.parsePayload(HeartRateBluetooth.HEARTRATE,"Synthetic SIG strap",characteristic,new byte[]{6,77}));
            });
            text(screen,context.getString(R.string.hr_value,77f));
        }
    }
}
