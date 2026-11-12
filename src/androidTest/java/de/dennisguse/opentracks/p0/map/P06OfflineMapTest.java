package de.dennisguse.opentracks.p0.map;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.*;
import android.location.Location;
import android.net.Uri;
import android.os.*;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.*;
import org.junit.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.p0.*;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.p0.data.*;
import de.dennisguse.opentracks.p0.recording.*;
import de.dennisguse.opentracks.services.*;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

public class P06OfflineMapTest {
    @Rule public final GrantPermissionRule permissions=TestUtil.createScanPermissionRule();
    @Rule public final ServiceTestRule serviceRule=new ServiceTestRule();
    private final Context context=ApplicationProvider.getApplicationContext();
    private final org.json.JSONArray captures=new org.json.JSONArray();
    private TrackRecordingService service;
    private OfflineMapRepository repository;
    private ThemeUtils.Family family;
    private String gps,heart;
    private int oldNight;
    private void main(Runnable r){InstrumentationRegistry.getInstrumentation().runOnMainSync(r);}
    @Before public void before()throws Exception{
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());
        repository=new OfflineMapRepository(context);for(var e:repository.read().entries())repository.delete(e.id());
        family=PreferencesUtils.getGardeniaTheme();oldNight=androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode();
        gps=PreferencesUtils.getString(R.string.settings_sensor_gps_key,"INTERNAL");heart=PreferencesUtils.getString(R.string.settings_sensor_bluetooth_heart_rate_key,"NONE");
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,"NONE");PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,"NONE");
        service=((TrackRecordingService.Binder)serviceRule.bindService(new Intent(context,TrackRecordingService.class))).getService();
    }
    @After public void after()throws Exception{
        main(()->{service.cancelGpsAcquisition();service.endCurrentTrack();service.stopIdleSensors();});
        new SessionStore(context).save(RecordingSession.ready());for(var e:repository.read().entries())repository.delete(e.id());
        PreferencesUtils.setGardeniaTheme(family);PreferencesUtils.setString(R.string.settings_sensor_gps_key,gps);PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,heart);
        main(()->androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(oldNight));
    }
    private OfflineMapView view(P06MapActivity a){return (OfflineMapView)((FrameLayout)a.findViewById(R.id.p06_map_container)).getChildAt(0);}
    private void waitMap(ActivityScenario<P06MapActivity> screen){
        long end=SystemClock.uptimeMillis()+20000;boolean[] ready={false};
        do{screen.onActivity(a->ready[0]=view(a).mapView()!=null&&view(a).renderedTileCount()>0);if(ready[0])return;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<end);
        fail("Native offline renderer did not produce actual vector tiles");
    }
    private void fixtureMap()throws Exception{try(var input=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("p06/gardenia-synthetic.map")){repository.importMap(input,"Synthetic park.map");}}
    private void fix(int meters){
        main(()->{Location f=new Location("gps");f.setLatitude(1.3+meters/110575.0);f.setLongitude(103.8);f.setAccuracy(2);f.setSpeed(2);f.setTime(System.currentTimeMillis());f.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());service.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(f);});SystemClock.sleep(150);
    }
    private void capture(ActivityScenario<P06MapActivity> screen,String name)throws Exception{
        waitMap(screen);screen.onActivity(a->assertTrue(a.hasWindowFocus()));SystemClock.sleep(400);P0UiCapture.save("p06-"+name);
        String output=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");File dir=new File(output!=null?new File(output):new File(context.getExternalMediaDirs()[0],"additional_test_output"),"p0-ui");
        File image=new File(dir,"p06-"+name+".png");String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(image.toPath())));
        int[] count={0};screen.onActivity(a->count[0]=view(a).renderedTileCount());captures.put(new org.json.JSONObject().put("file",image.getName()).put("sha256",hash).put("native",true).put("renderedTiles",count[0]));
    }
    private String shell(String command)throws Exception{
        try(var descriptor=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);var input=new FileInputStream(descriptor.getFileDescriptor());var output=new ByteArrayOutputStream()){byte[] data=new byte[4096];int n;while((n=input.read(data))!=-1)output.write(data,0,n);return output.toString(java.nio.charset.StandardCharsets.UTF_8);}
    }
    @Test public void safImportAirplaneRenderGesturesCurrentLiveHistoricalTrackAndAllThemes()throws Exception{
        String airplane=shell("settings get global airplane_mode_on").strip();
        try{
            shell("cmd connectivity airplane-mode enable");assertEquals("1",shell("settings get global airplane_mode_on").strip());
            try(ActivityScenario<P06MapActivity> screen=ActivityScenario.launch(P06MapActivity.class)){
                screen.onActivity(a->assertNull(view(a).mapView()));
                Intents.init();try{
                    intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(Uri.parse("content://com.fox.runner.p06.fixture/synthetic.map")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)));
                    onView(withId(R.id.p06_import_map)).perform(click());waitMap(screen);
                }finally{Intents.release();}
                assertNotNull(repository.read().current());capture(screen,"imported-airplane");
                screen.onActivity(a->{var m=view(a).mapView();byte old=m.getModel().mapViewPosition.getZoomLevel();m.setZoomLevel((byte)(old+1));assertEquals(old+1,m.getModel().mapViewPosition.getZoomLevel());var center=m.getModel().mapViewPosition.getCenter();m.getModel().mapViewPosition.moveCenter(80,40,false);assertNotEquals(center,m.getModel().mapViewPosition.getCenter());});
                AtomicReference<org.mapsforge.core.model.LatLong> beforePan=new AtomicReference<>();
                screen.onActivity(a->beforePan.set(view(a).mapView().getModel().mapViewPosition.getCenter()));
                onView(androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(org.mapsforge.map.android.view.MapView.class)).perform(androidx.test.espresso.action.ViewActions.swipeLeft());
                SystemClock.sleep(300);screen.onActivity(a->assertNotEquals("Real touch drag moves the map",beforePan.get(),view(a).mapView().getModel().mapViewPosition.getCenter()));
                capture(screen,"zoom-pan");screen.recreate();waitMap(screen);capture(screen,"recreated-airplane");
                onView(withId(R.id.p06_my_position)).perform(click());SystemClock.sleep(250);main(()->P0GpsPreparationTest.fixture(service));fix(0);fix(2);
                assertFalse("Map GPS preview must never create a workout",service.isRecording());assertTrue(new WorkoutRepository(context).list().isEmpty());
                SystemClock.sleep(1200);capture(screen,"current-position");
                main(()->service.startNewTrack());for(int i=2;i<=12;i++)fix(i*2);
                var id=service.getRecordingStatus().trackId();new SessionStore(context).save(RecordingSession.recover(id.id(),true));
                SystemClock.sleep(1300);onView(withId(R.id.p06_fit_track)).perform(click());capture(screen,"live-track");
                screen.onActivity(a->assertTrue(view(a).segmentCount()>0));
                main(()->{service.getTrackPointCreator().getSensorManager().getGpsHandler().onDisconnected();P0GpsPreparationTest.fixture(service);});
                for(int i=0;i<4;i++)fix(200+i*2);SystemClock.sleep(1300);
                screen.onActivity(a->assertTrue("Relocation segments must not be joined",view(a).segmentCount()>=2));capture(screen,"segmented-track");
                main(service::endCurrentTrack);assertTrue("Ending a workout keeps the visible map preview alive",service.areSensorsStarted());
                new SessionStore(context).save(RecordingSession.ready());
                try(ActivityScenario<P06MapActivity> history=ActivityScenario.launch(P06MapActivity.intent(context,id.id()))){waitMap(history);SystemClock.sleep(1000);onView(withId(R.id.p06_fit_track)).perform(click());capture(history,"historical-track");history.onActivity(a->assertTrue(view(a).segmentCount()>=2));}
            }
            // Both visible owners are gone; no map-owned GPS may survive.
            assertFalse(service.isRecording());
            for(ThemeUtils.Family theme:ThemeUtils.Family.values()){
                PreferencesUtils.setGardeniaTheme(theme);
                for(boolean dark:new boolean[]{false,true}){
                    main(()->androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(dark?androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES:androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO));
                    try(ActivityScenario<P06MapActivity> screen=ActivityScenario.launch(P06MapActivity.class)){capture(screen,"theme-"+theme.name()+"-"+(dark?"dark":"light"));}
                }
            }
            var old=repository.read();
            try(ActivityScenario<P06MapActivity> screen=ActivityScenario.launch(P06MapActivity.class)){
                waitMap(screen);Intents.init();try{
                    intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(Uri.parse("content://com.fox.runner.p06.fixture/broken.map"))));
                    onView(withId(R.id.p06_import_map)).perform(click());SystemClock.sleep(600);
                    screen.onActivity(a->assertEquals(a.getString(R.string.p06_map_failed),((TextView)a.findViewById(R.id.p06_map_status)).getText().toString()));
                    assertEquals(old,repository.read());capture(screen,"invalid-file-retains-map");
                }finally{Intents.release();}
            }
            org.json.JSONObject proof=new org.json.JSONObject().put("passed",true).put("sourceCommit",InstrumentationRegistry.getArguments().getString("buildCommit")).put("airplaneMode",true).put("safImport",true).put("zoomAndPan",true).put("currentPositionWithoutWorkout",true).put("liveTrack",true).put("historicalTrack",true).put("segmentBreaks",true).put("recreation",true).put("invalidFileRetainsMap",true).put("screenshots",captures);
            String output=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");File dir=new File(output!=null?new File(output):new File(context.getExternalMediaDirs()[0],"additional_test_output"),"p0-ui");
            try(var out=new FileOutputStream(new File(dir,"p06-offline-map.json"))){out.write(proof.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        }finally{shell("cmd connectivity airplane-mode "+("1".equals(airplane)?"enable":"disable"));}
    }
}
