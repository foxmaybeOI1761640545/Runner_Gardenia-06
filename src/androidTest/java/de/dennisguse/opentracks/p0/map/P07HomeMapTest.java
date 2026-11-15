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
    private ThemeUtils.Family family;
    private String night;
    @Before public void before()throws Exception{
        repository=new OfflineMapRepository(context);for(var e:repository.read().entries())repository.delete(e.id());
        family=PreferencesUtils.getGardeniaTheme();night=de.dennisguse.opentracks.p0.SystemThemeTestUtils.savedMode();
    }
    @After public void after()throws Exception{
        PreferencesUtils.setGardeniaTheme(family);de.dennisguse.opentracks.p0.SystemThemeTestUtils.restore(night);for(var e:repository.read().entries())repository.delete(e.id());
    }
    private RouteMapView home(P0Activity a){return a.findViewById(R.id.p04_route);}
    private void ready(ActivityScenario<P0Activity> screen,boolean real){
        long end=SystemClock.uptimeMillis()+30000;boolean[] done={false};
        do{screen.onActivity(a->done[0]=home(a).offlineMap().mapView()!=null&&(!real||home(a).offlineMap().renderedTileCount()>0));if(done[0])return;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<end);
        fail("Home native map did not initialize");
    }
    private Position position(double lat,double lon){return new Position(Instant.now(),lat,lon,Distance.of(8),null,null,null,null);}
    @Test public void noMapStillShowsPositionWithoutRecordingAndReleasesOnRecreation()throws Exception{
        try(var screen=ActivityScenario.launch(P0Activity.class)){
            ready(screen,false);
            screen.onActivity(a->{home(a).bind(new RoutePreviewModel.Result(List.of(),false,false),position(23.7,117.43));assertEquals(new LatLong(23.7,117.43),home(a).offlineMap().mapView().getModel().mapViewPosition.getCenter());assertEquals(a.getString(R.string.p07_no_map),home(a).mapStatus());});
            screen.recreate();ready(screen,false);P0UiCapture.save("p07-home-no-map");
        }
    }
    @Test public void realMapPositionFollowPanZoomRecreateReplaceDeleteAndBudget()throws Exception{
        try(var input=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("p061/real.map")){repository.importMap(input,"Actual 105MB map");}
        try(var screen=ActivityScenario.launch(P0Activity.class)){
            ready(screen,true);
            screen.onActivity(a->{var h=home(a);h.bind(new RoutePreviewModel.Result(List.of(),false,false),position(23.7,117.43));var view=h.offlineMap();
                assertTrue(view.isFollowing());assertEquals(new LatLong(23.7,117.43),view.mapView().getModel().mapViewPosition.getCenter());
                var d=view.diagnosticViewport();assertTrue(d.cacheCapacity()<=64);assertTrue(d.cacheCapacity()*(long)d.tileSize()*d.tileSize()*4<=32L*1024*1024);
            });
            // Dispatch genuine touch events to the map, exercising the drag-follow boundary.
            screen.onActivity(a->{var m=home(a).offlineMap().mapView();long t=SystemClock.uptimeMillis();
                var down=android.view.MotionEvent.obtain(t,t,0,120,60,0);var move=android.view.MotionEvent.obtain(t,t+100,2,180,60,0);var up=android.view.MotionEvent.obtain(t,t+200,1,180,60,0);
                try{m.dispatchTouchEvent(down);m.dispatchTouchEvent(move);m.dispatchTouchEvent(up);}finally{down.recycle();move.recycle();up.recycle();}
                assertFalse(home(a).offlineMap().isFollowing());
            });
            SystemClock.sleep(500);
            LatLong[] saved={null};
            screen.onActivity(a->{var h=home(a);saved[0]=h.offlineMap().mapView().getModel().mapViewPosition.getCenter();h.bind(new RoutePreviewModel.Result(List.of(),false,false),position(24,117));assertEquals(saved[0],h.offlineMap().mapView().getModel().mapViewPosition.getCenter());});
            screen.recreate();ready(screen,true);
            screen.onActivity(a->{assertFalse(home(a).offlineMap().isFollowing());assertEquals(saved[0],home(a).offlineMap().mapView().getModel().mapViewPosition.getCenter());
                home(a).bind(new RoutePreviewModel.Result(List.of(),false,false),position(1.3,103.8));a.findViewById(R.id.p07_home_locate).performClick();assertTrue(home(a).offlineMap().isFollowing());assertEquals(new LatLong(1.3,103.8),home(a).offlineMap().mapView().getModel().mapViewPosition.getCenter());});
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
            ready(screen,true);screen.onActivity(a->{var h=home(a);h.bind(new RoutePreviewModel.Result(List.of(),false,false),position(blank.latitude,blank.longitude));h.offlineMap().mapView().setZoomLevel((byte)12);});SystemClock.sleep(1500);
            screen.onActivity(a->assertEquals(a.getString(R.string.p07_no_features),home(a).mapStatus()));P0UiCapture.save("p07-home-inside-bounds-no-features");
        }
        for(ThemeUtils.Family f:ThemeUtils.Family.values())for(boolean dark:new boolean[]{false,true}){
            PreferencesUtils.setGardeniaTheme(f);de.dennisguse.opentracks.p0.SystemThemeTestUtils.night(dark);
            try(var screen=ActivityScenario.launch(P0Activity.class)){ready(screen,true);screen.onActivity(a->home(a).bind(new RoutePreviewModel.Result(List.of(),false,false),position(23.7,117.43)));SystemClock.sleep(800);P0UiCapture.save("p07-home-real-"+f.id+(dark?"-dark":"-light"));}
        }
        try(var input=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("p06/gardenia-synthetic.map")){repository.importMap(input,"Replacement");}
        try(var screen=ActivityScenario.launch(P0Activity.class)){ready(screen,true);}
        for(var e:repository.read().entries())repository.delete(e.id());
        try(var screen=ActivityScenario.launch(P0Activity.class)){ready(screen,false);screen.onActivity(a->assertEquals(a.getString(R.string.p07_no_map),home(a).mapStatus()));}
    }
}
