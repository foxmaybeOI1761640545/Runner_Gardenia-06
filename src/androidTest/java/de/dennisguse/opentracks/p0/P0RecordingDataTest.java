package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;
import androidx.test.rule.GrantPermissionRule;
import org.junit.*;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.*;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.ble.HeartRateCommands;
import de.dennisguse.opentracks.p0.data.*;
import de.dennisguse.opentracks.p0.recording.RecordingSession;
import de.dennisguse.opentracks.sensors.*;
import de.dennisguse.opentracks.sensors.driver.*;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

public class P0RecordingDataTest {
    @Rule public final ServiceTestRule rule=ServiceTestRule.withTimeout(10,TimeUnit.SECONDS);
    @Rule public final GrantPermissionRule permissions=GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.POST_NOTIFICATIONS);
    private final Context context=ApplicationProvider.getApplicationContext();
    private final Instant base=Instant.parse("2026-11-04T02:00:00Z");
    private TrackRecordingService service;
    private String selected,sampling,distance,gps;
    @Before public void before() throws Exception {
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());
        selected=HeartRateCommands.selected();sampling=PreferencesUtils.getString(R.string.min_sampling_interval_key,"0");distance=PreferencesUtils.getString(R.string.recording_distance_interval_key,"5");gps=PreferencesUtils.getString(R.string.settings_sensor_gps_key,SensorType.INTERNAL.getPreferenceValue());
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,SensorType.INTERNAL.getPreferenceValue());
        PreferencesUtils.setString(R.string.min_sampling_interval_key,"1");PreferencesUtils.setString(R.string.recording_distance_interval_key,"0");
        service=((TrackRecordingService.Binder)rule.bindService(new Intent(context,TrackRecordingService.class))).getService();
    }
    @After public void after() {
        if(service!=null)InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{service.endCurrentTrack();service.stopIdleSensors();});
        new SessionStore(context).save(RecordingSession.ready());
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,selected);PreferencesUtils.setString(R.string.settings_sensor_gps_key,gps);
        PreferencesUtils.setString(R.string.min_sampling_interval_key,sampling);PreferencesUtils.setString(R.string.recording_distance_interval_key,distance);
    }
    private void time(int second){service.getTrackPointCreator().setClock(base.plusSeconds(second).toString());}
    private void fixtureGps() {
        var target=service.getTrackPointCreator().getSensorManager().getGpsHandler();if(target.driver!=null)target.driver.disconnect();
        target.driver=new Driver() {
            private boolean connected=true;
            public void connect(Context context,android.os.Handler owner,String address){}
            public void updateDriverParameters(Context context,android.os.Handler owner){}
            public boolean isConnected(){return connected;}
            public void disconnect(){connected=false;target.onDisconnected();}
        };
        target.onConnected(null,"Synthetic GPS");
    }
    private void fix(int second,double latitude) {
        time(second);Location point=new Location("gps");point.setLatitude(latitude);point.setLongitude(103.8);point.setAccuracy(2);point.setSpeed(3);point.setTime(base.plusSeconds(second).toEpochMilli());
        service.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(point);
    }
    private HeartRateHandler heart() {
        var target=service.getTrackPointCreator().getSensorManager().getHeartRateHandler();
        target.onConnectionStateChanged(BleConnectionSession.State.CONNECTED);target.onConnected("AA:BB:CC:DD:EE:FF","Synthetic integration strap");return target;
    }
    private HeartRate notification(int bpm) {
        return new HeartRateBluetooth().parsePayload(HeartRateBluetooth.HEARTRATE,"Synthetic integration strap",
                new android.bluetooth.BluetoothGattCharacteristic(HeartRateBluetooth.HEARTRATE.measurementUUID(),0,0),new byte[]{30,(byte)bpm,12,0,0,4,(byte)232,3});
    }
    @Test public void gpsAndEveryRawNotificationStayAlignedAcrossPausePreviewAndResume() {
        var id=new java.util.concurrent.atomic.AtomicReference<Track.Id>();var first=new java.util.concurrent.atomic.AtomicReference<HeartRate>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            time(0);id.set(service.startNewTrack());fixtureGps();var handler=heart();
            time(1);handler.onDataReceived(notification(76));first.set(service.getHeartRatePreview().value());fix(1,1.3);
            time(2);handler.onDataReceived(notification(76));fix(2,1.3001);
            service.newTrackPoint(new TrackPoint(null,TrackPoint.Type.TRACKPOINT,Position.of(base.plusSeconds(2)),null,first.get(),null,null,null,null),Distance.of(25));
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertEquals(GpsStatusValue.GPS_SIGNAL_FIX,service.getGpsStatusObservable().getValue());
        var repository=new WorkoutRepository(context);assertEquals(2,repository.heartRates(id.get()).size());
        try{repository.delete(id.get());fail("Active service track must be protected even without a P0 journal");}catch(IllegalStateException expected){}
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            time(3);service.endCurrentTrack();time(19);service.tryStartSensors();heart().onDataReceived(notification(80));
        });
        assertEquals(2,repository.heartRates(id.get()).size());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            time(20);service.resumeTrack(id.get());fixtureGps();fix(20,2.3);
        });
        ArrayList<TrackPoint> pausedBoundary=new ArrayList<>();repository.forEachPoint(id.get(),pausedBoundary::add);
        var resumedPoints=pausedBoundary.stream().filter(point->point.position().hasLocation()&&point.getTime().equals(base.plusSeconds(20))).toList();
        assertEquals(1,resumedPoints.size());assertNull(resumedPoints.get(0).heartRate());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            time(21);service.getTrackPointCreator().getSensorManager().getHeartRateHandler().onDataReceived(notification(90));fix(21,2.3001);time(23);service.endCurrentTrack();
        });
        var rows=new WorkoutRepository(context).heartRates(id.get());assertEquals(3,rows.size());
        assertEquals(List.of(base.plusSeconds(1),base.plusSeconds(2),base.plusSeconds(21)),rows.stream().map(value->value.sample().capturedAt()).toList());
        assertNotEquals(rows.get(0).sample().sampleId(),rows.get(1).sample().sampleId());assertNotEquals(rows.get(0).sample().connectionId(),rows.get(2).sample().connectionId());
        assertEquals(List.of(1024,1000),rows.get(2).sample().rrIntervals1024());assertEquals(Integer.valueOf(12),rows.get(2).sample().energyExpended());
        var summary=repository.find(id.get());assertEquals(3,summary.heartRate().count());assertEquals((76d+76+90)/3,summary.heartRate().average(),0.00001);
        assertEquals(Duration.ofSeconds(6),summary.track().statistics().totalDuration());
    }
}
