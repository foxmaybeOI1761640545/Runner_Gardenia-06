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
import de.dennisguse.opentracks.p0.data.WorkoutRepository;
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
        time(second);Location value=new Location("gps");value.setLatitude(latitude);value.setLongitude(103.8);value.setAccuracy(2);value.setSpeed(3);value.setElapsedRealtimeNanos((second + 1L) * 1_000_000_000L);value.setTime(Instant.parse("2020-02-02T02:02:00Z").plusSeconds(second).toEpochMilli());
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

    private void motionFix(int second, double northMeters, float speed) {
        time(second);
        Location fix = new Location("gps");
        fix.setLatitude(1.3 + northMeters / 110_575.0); fix.setLongitude(103.8);
        fix.setAccuracy(2); fix.setSpeed(speed);
        fix.setTime(Instant.parse("2020-02-02T02:02:00Z").plusSeconds(second).toEpochMilli());
        fix.setElapsedRealtimeNanos((second + 1L) * 1_000_000_000L);
        service.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(fix);
    }

    private ArrayList<TrackPoint> points(Track.Id id) {
        ArrayList<TrackPoint> result = new ArrayList<>();
        try (var iterator = new ContentProviderUtils(context).getTrackPointIterator(id, null)) {
            while (iterator.hasNext()) result.add(iterator.next());
        }
        return result;
    }

    private org.w3c.dom.Document exported(Track.Id id, String name) throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        new de.dennisguse.opentracks.p0.export.P0GpxExporter(context).write(id, bytes);
        String directory = androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
        if (directory != null) {
            var folder = new java.io.File(directory, "p0-gpx"); assertTrue(folder.exists() || folder.mkdirs());
            try (var file = new java.io.FileOutputStream(new java.io.File(folder, name + ".gpx"))) {file.write(bytes.toByteArray());}
        }
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
        var result = factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(bytes.toByteArray()));
        assertEquals("1.1", result.getDocumentElement().getAttribute("version"));
        return result;
    }

    private void connectHeart() {
        var heart = service.getTrackPointCreator().getSensorManager().getHeartRateHandler();
        heart.onConnected("AA:BB:CC:DD:EE:FF", "Synthetic GPS integration strap");
    }

    private void heart(int second) {
        time(second);
        var sample = new HeartRate(80, new HeartRateSample(null, null, null, true, 12, java.util.List.of(1024)));
        service.getTrackPointCreator().getSensorManager().getHeartRateHandler().onDataReceived(sample);
    }

    @Test public void stationaryNoiseRetainsAll255TimestampsAndHeartSamplesInDatabaseAndGpx() throws Exception {
        time(0); Track.Id id = service.startNewTrack(); fixtureGps(); connectHeart();
        for (int second = 1; second <= 255; second++) {
            heart(second);
            motionFix(second, (second % 7 - 3) * 0.1, (second % 4) * 0.1f);
        }
        time(256); service.endCurrentTrack();
        var stored = points(id).stream().filter(p -> p.position().hasLocation()).toList();
        assertEquals(255, stored.size());
        for (int i = 0; i < stored.size(); i++) {
            assertEquals(Instant.parse("2020-02-02T02:02:00Z").plusSeconds(i + 1), stored.get(i).getTime());
            assertEquals(stored.get(0).position().latitude(), stored.get(i).position().latitude(), 0);
            assertEquals(0, stored.get(i).position().speed().toMPS(), 0);
            assertEquals(2, stored.get(i).position().horizontalAccuracy().toM(), 0);
        }
        var statistics = new ContentProviderUtils(context).getTrack(id).statistics();
        assertEquals(0, statistics.totalDistance().toM(), 0);
        assertEquals(Duration.ofSeconds(256), statistics.totalDuration());
        assertEquals(255, new WorkoutRepository(context).heartRates(id).size());
        var xml = exported(id, "p051-stationary-real-chain");
        String gpx = de.dennisguse.opentracks.p0.export.P0GpxExporter.GPX;
        String raw = de.dennisguse.opentracks.p0.export.P0GpxExporter.RAW;
        assertEquals(255, xml.getElementsByTagNameNS(gpx, "trkpt").getLength());
        assertEquals(255, xml.getElementsByTagNameNS(raw, "sample").getLength());
        assertEquals(1, xml.getElementsByTagNameNS(gpx, "trkseg").getLength());
        for (int i = 0; i < 255; i++) {
            var point = (org.w3c.dom.Element) xml.getElementsByTagNameNS(raw, "point").item(i);
            assertEquals(0, Double.parseDouble(point.getAttribute("speedMetersPerSecond")), 0);
            var trackPoint = (org.w3c.dom.Element) xml.getElementsByTagNameNS(gpx, "trkpt").item(i);
            assertEquals(stored.get(i).getTime().toString(), trackPoint.getElementsByTagNameNS(gpx, "time").item(0).getTextContent());
        }
    }

    private void slowRecording(float speed) {
        time(0); Track.Id id = service.startNewTrack(); fixtureGps();
        motionFix(1, 0, 0);
        for (int second = 2; second <= 61; second++) motionFix(second, (second - 1) * speed, speed);
        time(62); service.endCurrentTrack();
        assertEquals(61, points(id).stream().filter(p -> p.position().hasLocation()).count());
        assertEquals(60 * speed, new ContentProviderUtils(context).getTrack(id).statistics().totalDistance().toM(), 0.15);
        var late = points(id).stream().filter(p -> p.position().hasLocation() && p.getTime().isAfter(Instant.parse("2020-02-02T02:02:20Z"))).toList();
        assertFalse(late.isEmpty());
        for (var point : late) assertEquals(speed, point.position().speed().toMPS(), 0.00001);
    }
    @Test public void halfMeterPerSecondIsPreservedThroughRealRecordingChain() {slowRecording(0.5f);}
    @Test public void slowWalkingIsPreservedThroughRealRecordingChain() {slowRecording(0.7f);}
    @Test public void normalRunningIsPreservedThroughRealRecordingChain() {slowRecording(3f);}
    @Test public void normalCyclingIsPreservedThroughRealRecordingChain() {slowRecording(10f);}

    @Test public void gpsReconnectDoesNotBridgeAndKeepsHeartSamplesAndGpxSegments() throws Exception {
        time(0); Track.Id id = service.startNewTrack(); fixtureGps(); connectHeart();
        motionFix(1, 0, 3); motionFix(2, 3, 3);
        var gps = service.getTrackPointCreator().getSensorManager().getGpsHandler();
        gps.onDisconnected(); heart(3); gps.onConnected(null, "Synthetic GPS reconnected");
        motionFix(4, 1000, 3); motionFix(5, 1003, 3);
        time(6); service.endCurrentTrack();
        assertEquals(6, new ContentProviderUtils(context).getTrack(id).statistics().totalDistance().toM(), 0.1);
        assertEquals(Duration.ofSeconds(6), new ContentProviderUtils(context).getTrack(id).statistics().totalDuration());
        assertEquals(1, points(id).stream().filter(p -> p.type() == TrackPoint.Type.SEGMENT_START_AUTOMATIC && p.position().hasLocation()).count());
        assertEquals(1, new WorkoutRepository(context).heartRates(id).size());
        var xml = exported(id, "p051-reconnect-real-chain");
        assertEquals(2, xml.getElementsByTagNameNS(de.dennisguse.opentracks.p0.export.P0GpxExporter.GPX, "trkseg").getLength());
    }

    @Test public void pendingAutomaticSegmentIsNotConsumedByAnInterleavedBleEvent() {
        time(0); Track.Id id = service.startNewTrack(); fixtureGps(); connectHeart();
        motionFix(1, 0, 3); motionFix(2, 3, 3);
        service.getTrackPointCreator().requestAutomaticSegmentStart();
        heart(3);
        assertEquals(0, points(id).stream().filter(p -> p.type() == TrackPoint.Type.SEGMENT_START_AUTOMATIC).count());
        motionFix(4, 6, 3); motionFix(5, 9, 3); time(6); service.endCurrentTrack();
        var boundaries = points(id).stream().filter(p -> p.type() == TrackPoint.Type.SEGMENT_START_AUTOMATIC).toList();
        assertEquals(1, boundaries.size()); assertTrue(boundaries.get(0).position().hasLocation());
        assertEquals(Instant.parse("2020-02-02T02:02:04Z"), boundaries.get(0).getTime());
        assertEquals(1, new WorkoutRepository(context).heartRates(id).size());
    }

    @Test public void longGapAndTrustedRelocationExcludeJumpDistanceAndSplitGpx() throws Exception {
        time(0); Track.Id id = service.startNewTrack(); fixtureGps();
        motionFix(1, 0, 3); motionFix(2, 3, 3);
        motionFix(3, 1000, 3); motionFix(4, 1001, 3); motionFix(5, 1002, 3);
        motionFix(6, 1005, 3); motionFix(25, 2000, 3); motionFix(26, 2003, 3);
        time(27); service.endCurrentTrack();
        assertEquals(9, new ContentProviderUtils(context).getTrack(id).statistics().totalDistance().toM(), 0.15);
        assertEquals(2, points(id).stream().filter(p -> p.type() == TrackPoint.Type.SEGMENT_START_AUTOMATIC).count());
        var xml = exported(id, "p051-relocation-gap-real-chain");
        assertEquals(3, xml.getElementsByTagNameNS(de.dennisguse.opentracks.p0.export.P0GpxExporter.GPX, "trkseg").getLength());
        assertEquals(6, xml.getElementsByTagNameNS(de.dennisguse.opentracks.p0.export.P0GpxExporter.GPX, "trkpt").getLength());
    }

    @Test public void stoppingDriftCannotIncreaseDistanceDuringHysteresisOrAfterward() {
        time(0); Track.Id id = service.startNewTrack(); fixtureGps();
        motionFix(1,0,3); motionFix(2,3,3);
        double distance = new ContentProviderUtils(context).getTrack(id).statistics().totalDistance().toM();
        for (int second = 3; second <= 65; second++) {
            motionFix(second, 3 + (second % 5 - 2) * 0.1, (second % 4) * 0.1f);
            assertEquals(distance, new ContentProviderUtils(context).getTrack(id).statistics().totalDistance().toM(), 0);
        }
        time(66); service.endCurrentTrack();
        assertEquals(65, points(id).stream().filter(point -> point.position().hasLocation()).count());
    }
}
