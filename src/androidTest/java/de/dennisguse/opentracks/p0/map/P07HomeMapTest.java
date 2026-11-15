package de.dennisguse.opentracks.p0.map;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.List;
import java.time.Instant;
import org.junit.*;
import org.mapsforge.core.model.LatLong;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.P0UiCapture;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;

/** Home acceptance uses the exact real file, including a feature-empty tile inside its bounds. */
public class P07HomeMapTest {
    private final Context context=ApplicationProvider.getApplicationContext();
    private OfflineMapRepository repository;
    @Rule public final androidx.test.rule.GrantPermissionRule permissions=de.dennisguse.opentracks.TestUtil.createScanPermissionRule();
    @Rule public final androidx.test.rule.ServiceTestRule serviceRule=new androidx.test.rule.ServiceTestRule();
    private de.dennisguse.opentracks.services.TrackRecordingService service;
    private ThemeUtils.Family family;
    private String night,flight,font,size,density;
    @Before public void before()throws Exception{
        de.dennisguse.opentracks.util.TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();
        service=((de.dennisguse.opentracks.services.TrackRecordingService.Binder)serviceRule.bindService(new android.content.Intent(context,de.dennisguse.opentracks.services.TrackRecordingService.class))).getService();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->service.acquireMapGps(this));
        repository=new OfflineMapRepository(context);for(var e:repository.read().entries())repository.delete(e.id());
        family=PreferencesUtils.getGardeniaTheme();night=de.dennisguse.opentracks.p0.SystemThemeTestUtils.savedMode();
        flight=de.dennisguse.opentracks.p0.SystemThemeTestUtils.shell("settings get global airplane_mode_on");
        font=de.dennisguse.opentracks.p0.SystemThemeTestUtils.shell("settings get system font_scale");
        size=de.dennisguse.opentracks.p0.SystemThemeTestUtils.shell("wm size");density=de.dennisguse.opentracks.p0.SystemThemeTestUtils.shell("wm density");
        de.dennisguse.opentracks.p0.SystemThemeTestUtils.shell("cmd connectivity airplane-mode enable");
        // Disconnect emulator/provider inputs left by other tests; continue through
        // the same service/GpsHandler with deterministic synthetic measurements.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->de.dennisguse.opentracks.p0.P0GpsPreparationTest.fixture(service));
    }
    @After public void after()throws Exception{
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->service.releaseMapGps(this));
        if(!flight.equals("1"))de.dennisguse.opentracks.p0.SystemThemeTestUtils.shell("cmd connectivity airplane-mode disable");
        de.dennisguse.opentracks.p0.SystemThemeTestUtils.shell("settings put system font_scale "+(font.equals("null")?"1.0":font));
        for(String[] r:new String[][]{{"size",size},{"density",density}}){var m=java.util.regex.Pattern.compile("Override (?:size|density): ([^\r\n]+)").matcher(r[1]);de.dennisguse.opentracks.p0.SystemThemeTestUtils.shell("wm "+r[0]+" "+(m.find()?m.group(1).trim():"reset"));}
        PreferencesUtils.setGardeniaTheme(family);de.dennisguse.opentracks.p0.SystemThemeTestUtils.restore(night);for(var e:repository.read().entries())repository.delete(e.id());
    }
    private RouteMapView home(P0Activity a){return a.findViewById(R.id.p04_route);}
    private void ready(ActivityScenario<P0Activity> screen,boolean real){
        long end=SystemClock.uptimeMillis()+30000;boolean[] done={false};
        do{screen.onActivity(a->done[0]=home(a).offlineMap().mapView()!=null&&(!real||home(a).offlineMap().renderedTileCount()>0));if(done[0])return;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<end);
        fail("Home native map did not initialize");
    }
    private Position position(double lat,double lon){return new Position(Instant.now(),lat,lon,Distance.of(8),null,null,null,null);}
    private void bind(RouteMapView view,Position point){
        android.location.Location fix=point.toLocation();fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        service.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(fix);
        view.bind(new RoutePreviewModel.Result(List.of(),false,false),point);
    }
    private void settledRealMap(ActivityScenario<P0Activity> screen){
        long end=SystemClock.uptimeMillis()+15000;long[] lastFix={0};boolean[] settled={false};
        do{
            screen.onActivity(a->{
                var h=home(a);long now=SystemClock.uptimeMillis();
                if(now-lastFix[0]>=1000){bind(h,position(23.7,117.43));lastFix[0]=now;}
                var d=h.offlineMap().diagnosticViewport();
                settled[0]=d!=null&&d.center().equals(new LatLong(23.7,117.43))&&!d.tiles().isEmpty()
                    &&d.tiles().stream().allMatch(OfflineMapView.TileState::cached)
                    &&a.getString(R.string.p07_map_ready).contentEquals(h.mapStatus());
            });
            if(settled[0]){SystemClock.sleep(300);return;}
            SystemClock.sleep(100);
        }while(SystemClock.uptimeMillis()<end);
        fail("Real home capture requires the intended location, actual features and every visible tile cached");
    }
    @Test public void noMapStillShowsPositionWithoutRecordingAndReleasesOnRecreation()throws Exception{
        int records=new de.dennisguse.opentracks.p0.data.WorkoutRepository(context).list().size();
        try(var screen=ActivityScenario.launch(P0Activity.class)){
            ready(screen,false);
            screen.onActivity(a->{bind(home(a),position(23.7,117.43));assertEquals(new LatLong(23.7,117.43),home(a).offlineMap().mapView().getModel().mapViewPosition.getCenter());assertEquals(a.getString(R.string.p07_no_map),home(a).mapStatus());});
            screen.recreate();ready(screen,false);P0UiCapture.save("p07-home-no-map");
        }
        assertEquals(records,new de.dennisguse.opentracks.p0.data.WorkoutRepository(context).list().size());
        assertFalse(service.isRecording());
    }
    @Test public void realMapPositionFollowPanZoomRecreateReplaceDeleteAndBudget()throws Exception{
        try(var input=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("p061/real.map")){repository.importMap(input,"Actual 105MB map");}
        try(var screen=ActivityScenario.launch(P0Activity.class)){
            ready(screen,true);
            screen.onActivity(a->{var h=home(a);bind(h,position(23.7,117.43));var view=h.offlineMap();
                boolean accuracy=false;
                for(var layer:view.mapView().getLayerManager().getLayers())if(layer.getClass()==org.mapsforge.map.layer.overlay.Circle.class){accuracy=true;assertEquals(8f,((org.mapsforge.map.layer.overlay.Circle)layer).getRadius(),.01f);}
                assertTrue("The GPS horizontal accuracy must be represented in meters",accuracy);
                assertTrue(view.isFollowing());assertEquals(new LatLong(23.7,117.43),view.mapView().getModel().mapViewPosition.getCenter());
                var d=view.diagnosticViewport();assertTrue(d.cacheCapacity()<=64);assertTrue(d.cacheCapacity()*(long)d.tileSize()*d.tileSize()*4<=32L*1024*1024);
            });
            screen.onActivity(a->home(a).offlineMap().mapView().getMapZoomControls().getButtonZoomIn().performClick());SystemClock.sleep(350);
            screen.onActivity(a->{assertEquals(16,home(a).offlineMap().mapView().getModel().mapViewPosition.getZoomLevel());home(a).offlineMap().mapView().getMapZoomControls().getButtonZoomOut().performClick();});SystemClock.sleep(350);
            screen.onActivity(a->assertEquals(15,home(a).offlineMap().mapView().getModel().mapViewPosition.getZoomLevel()));
            // Dispatch genuine touch events to the map, exercising the drag-follow boundary.
            screen.onActivity(a->{var m=home(a).offlineMap().mapView();long t=SystemClock.uptimeMillis();
                var down=android.view.MotionEvent.obtain(t,t,0,120,60,0);var move=android.view.MotionEvent.obtain(t,t+100,2,180,60,0);var up=android.view.MotionEvent.obtain(t,t+200,1,180,60,0);
                try{m.dispatchTouchEvent(down);m.dispatchTouchEvent(move);m.dispatchTouchEvent(up);}finally{down.recycle();move.recycle();up.recycle();}
                assertFalse(home(a).offlineMap().isFollowing());
            });
            SystemClock.sleep(500);
            LatLong[] saved={null};
            screen.onActivity(a->{var h=home(a);saved[0]=h.offlineMap().mapView().getModel().mapViewPosition.getCenter();bind(h,position(24,117));assertEquals(saved[0],h.offlineMap().mapView().getModel().mapViewPosition.getCenter());});
            screen.recreate();ready(screen,true);
            screen.onActivity(a->{assertFalse(home(a).offlineMap().isFollowing());assertEquals(saved[0],home(a).offlineMap().mapView().getModel().mapViewPosition.getCenter());
                bind(home(a),position(1.3,103.8));a.findViewById(R.id.p07_home_locate).performClick();assertTrue(home(a).offlineMap().isFollowing());assertEquals(new LatLong(1.3,103.8),home(a).offlineMap().mapView().getModel().mapViewPosition.getCenter());});
            SystemClock.sleep(1200);screen.onActivity(a->assertEquals(a.getString(R.string.p07_no_features),home(a).mapStatus()));P0UiCapture.save("p07-home-outside-map");
        }
        var actual=repository.read().current();
        // Derive an actually empty tile from this real map instead of treating its bbox as coverage.
        LatLong empty=null;var file=repository.open(actual);
        try{var bounds=file.boundingBox();byte z=12;
            int left=org.mapsforge.core.util.MercatorProjection.longitudeToTileX(bounds.minLongitude,z),right=org.mapsforge.core.util.MercatorProjection.longitudeToTileX(bounds.maxLongitude,z);
            int top=org.mapsforge.core.util.MercatorProjection.latitudeToTileY(bounds.maxLatitude,z),bottom=org.mapsforge.core.util.MercatorProjection.latitudeToTileY(bounds.minLatitude,z);
            outer:for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++){var tile=new org.mapsforge.core.model.Tile(x,y,z,256);var data=file.readMapData(tile);var c=tile.getBoundingBox().getCenterPoint();if(bounds.contains(c)&&data!=null&&data.ways.isEmpty()&&data.pois.isEmpty()){empty=c;break outer;}}
        }finally{file.close();}
        assertNotNull("Real map must include an inside-bbox empty tile for coverage regression",empty);LatLong blank=empty;
        try(var screen=ActivityScenario.launch(P0Activity.class)){
            ready(screen,true);screen.onActivity(a->{var h=home(a);bind(h,position(blank.latitude,blank.longitude));h.offlineMap().mapView().setZoomLevel((byte)12);});SystemClock.sleep(1500);
            screen.onActivity(a->assertEquals(a.getString(R.string.p07_no_features),home(a).mapStatus()));P0UiCapture.save("p07-home-inside-bounds-no-features");
        }
        for(int width:new int[]{360,390})for(float scale:new float[]{1.0f,1.4f}){
            de.dennisguse.opentracks.p0.SystemThemeTestUtils.shell("wm size "+(width*2)+"x1600");
            de.dennisguse.opentracks.p0.SystemThemeTestUtils.shell("wm density 320");
            de.dennisguse.opentracks.p0.SystemThemeTestUtils.shell("settings put system font_scale "+scale);
            for(ThemeUtils.Family f:ThemeUtils.Family.values())for(boolean dark:new boolean[]{false,true}){
                PreferencesUtils.setGardeniaTheme(f);de.dennisguse.opentracks.p0.SystemThemeTestUtils.night(dark);
                try(var screen=ActivityScenario.launch(P0Activity.class)){
                    ready(screen,true);screen.onActivity(a->{bind(home(a),position(23.7,117.43));
                        home(a).requestRectangleOnScreen(new android.graphics.Rect(0,0,home(a).getWidth(),home(a).getHeight()),true);
                        assertEquals(width,a.getResources().getConfiguration().screenWidthDp);
                        assertEquals(dark,ThemeUtils.isNight(a));
                        android.graphics.Rect locate=new android.graphics.Rect(),zoom=new android.graphics.Rect();
                        a.findViewById(R.id.p07_home_locate).getGlobalVisibleRect(locate);
                        home(a).offlineMap().mapView().getMapZoomControls().getGlobalVisibleRect(zoom);
                        assertFalse("Recenter must not cover native zoom controls",android.graphics.Rect.intersects(locate,zoom));
                        android.graphics.Rect metric=new android.graphics.Rect();a.findViewById(R.id.p05_primary_metric).getGlobalVisibleRect(metric);
                        assertFalse("Primary metric must not cover map zoom",android.graphics.Rect.intersects(metric,zoom));
                        assertFalse("Primary metric must not cover recenter",android.graphics.Rect.intersects(metric,locate));
                    });settledRealMap(screen);
                    screen.onActivity(a->assertEquals(a.getString(R.string.p05_gps_fixed),((android.widget.TextView)a.findViewById(R.id.p04_gps_status)).getText().toString()));
                    P0UiCapture.save("p07-home-real-"+f.id+(dark?"-dark":"-light")+"-"+width+"-font"+scale);
                }
            }
        }
        try(var input=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("p06/gardenia-synthetic.map")){repository.importMap(input,"Replacement");}
        try(var screen=ActivityScenario.launch(P0Activity.class)){ready(screen,true);}
        for(var e:repository.read().entries())repository.delete(e.id());
        try(var screen=ActivityScenario.launch(P0Activity.class)){ready(screen,false);screen.onActivity(a->assertEquals(a.getString(R.string.p07_no_map),home(a).mapStatus()));}
    }
}
