package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Looper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.rule.ServiceTestRule;
import androidx.test.rule.GrantPermissionRule;
import org.junit.*;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.*;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.data.SessionStore;
import de.dennisguse.opentracks.p0.recording.*;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.sensors.driver.GpsInternal;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

public class P0GpsRecordingTest {
    @Rule public final ServiceTestRule rule=ServiceTestRule.withTimeout(10,TimeUnit.SECONDS);
    @Rule public final GrantPermissionRule permissions=GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.POST_NOTIFICATIONS);
    private final Context context=ApplicationProvider.getApplicationContext();
    private TrackRecordingService service;
    private String sampling,distance;
    private boolean ownLooper;
    @Before public void before() throws Exception {
        if(Looper.myLooper()==null){Looper.prepare();ownLooper=true;}
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();
        sampling=PreferencesUtils.getString(R.string.min_sampling_interval_key,"0");
        distance=PreferencesUtils.getString(R.string.recording_distance_interval_key,"5");
        PreferencesUtils.setString(R.string.min_sampling_interval_key,"1");PreferencesUtils.setString(R.string.recording_distance_interval_key,"0");
        service=((TrackRecordingService.Binder)rule.bindService(new Intent(context,TrackRecordingService.class))).getService();
    }
    @After public void after() {
        if(service!=null)service.endCurrentTrack();
        new SessionStore(context).save(RecordingSession.ready());
        PreferencesUtils.setString(R.string.min_sampling_interval_key,sampling);PreferencesUtils.setString(R.string.recording_distance_interval_key,distance);
        if(ownLooper)Looper.myLooper().quit();
    }
    private void time(int second) {service.getTrackPointCreator().setClock(Instant.parse("2020-02-02T02:02:00Z").plusSeconds(second).toString());}
    private void fixtureGps() {
        var gps=service.getTrackPointCreator().getSensorManager().getGpsHandler();
        if(gps.driver!=null)gps.driver.disconnect();
        // Keep the normal SensorManager -> Driver.disconnect lifecycle. Clearing the driver
        // here would leave the synthetic GPS status timer running after pause/finish.
        gps.driver=new de.dennisguse.opentracks.sensors.driver.Driver() {
            private boolean connected=true;
            @Override public void connect(Context context,android.os.Handler handler,String address) {}
            @Override public boolean isConnected() {return connected;}
            @Override public void updateDriverParameters(Context context,android.os.Handler handler) {}
            @Override public void disconnect() {connected=false;gps.onDisconnected();}
        };
        gps.onConnected(null,"Synthetic GPS fixture");
    }
    private void fix(int second,double latitude) {
        time(second);Location value=new Location("gps");value.setLatitude(latitude);value.setLongitude(103.8);value.setAccuracy(2);value.setSpeed(3);value.setTime(Instant.parse("2020-02-02T02:02:00Z").plusSeconds(second).toEpochMilli());
        service.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(value);
    }
    @Test public void realDriverRequestTargetsOneHertzWithoutBatching() {
        var request=new GpsInternal(null).createLocationRequest();
        assertEquals(Duration.ofSeconds(1),PreferencesUtils.getMinSamplingInterval());
        assertEquals(1000,request.getIntervalMillis());assertEquals(0,request.getMaxUpdateDelayMillis());
    }
    @Test public void deniedBluetoothDoesNotAddConnectedDeviceForegroundType() {
        Context denied=new ContextWrapper(context) {
            @Override public int checkPermission(String name,int pid,int uid) {return name.contains("BLUETOOTH")?PackageManager.PERMISSION_DENIED:super.checkPermission(name,pid,uid);}
            @Override public int checkSelfPermission(String name) {return name.contains("BLUETOOTH")?PackageManager.PERMISSION_DENIED:super.checkSelfPermission(name);}
        };
        assertTrue(RecordingPermissions.hasAny(denied));assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,RecordingPermissions.foregroundTypes(denied));
    }
    @Test public void pauseResumeDoesNotBridgeFarCoordinatesOrAccumulatePausedTime() {
        time(0);Track.Id id=service.startNewTrack();assertNotNull(id);fixtureGps();fix(1,1.3);fix(2,1.3001);
        time(3);service.endCurrentTrack();
        var store=new ContentProviderUtils(context);double pausedDistance=store.getTrack(id).statistics().totalDistance().toM();
        time(20);assertFalse(service.newTrackPoint(new TrackPoint(TrackPoint.Type.TRACKPOINT,new Position(Instant.parse("2020-02-02T02:02:20Z"),5d,103.8,Distance.of(2),null,null,null,Speed.of(3))),Distance.of(2)));
        assertEquals(pausedDistance,store.getTrack(id).statistics().totalDistance().toM(),0);
        time(33);service.resumeTrack(id);fixtureGps();fix(34,2.3);fix(35,2.3001);time(36);service.endCurrentTrack();
        assertNull(service.getTrackPointCreator().getSensorManager().getGpsHandler());
        var stats=store.getTrack(id).statistics();assertTrue(stats.totalDistance().toM()>10);assertTrue(stats.totalDistance().toM()<100);
        assertEquals(Duration.ofSeconds(6),stats.totalDuration());assertTrue(stats.movingDuration().compareTo(Duration.ofSeconds(6))<=0);
        ArrayList<TrackPoint> points=new ArrayList<>();try(var iterator=store.getTrackPointIterator(id,null)){while(iterator.hasNext())points.add(iterator.next());}
        assertEquals(2,points.stream().filter(p->p.isSegmentManualStart()).count());
        assertTrue(points.stream().filter(p->p.position().hasLocation()).allMatch(p->p.position().horizontalAccuracy()!=null));
    }
    @Test public void sessionIntentReopensPausedWithoutInventingRunningService() {
        var store=new SessionStore(context);store.save(RecordingSession.ready().begin(123));
        assertEquals(RecordingSession.Phase.PAUSED,new SessionStore(context).recover().phase());assertEquals(Long.valueOf(123),new SessionStore(context).recover().trackId());
    }
}
