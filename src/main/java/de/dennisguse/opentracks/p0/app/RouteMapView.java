package de.dennisguse.opentracks.p0.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.p0.map.*;

/** Home uses the same private map and renderer as the offline-map screen. */
public final class RouteMapView extends FrameLayout {
    private final OfflineMapView map;
    private final TextView status;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final OfflineMapRepository repository;
    private ExecutorService worker;
    private List<RouteGeometry.Point> points=List.of();
    private Position current;
    private String selected;
    private long generation;
    private boolean active,checking,coverageScheduled;
    private android.os.Bundle viewport;
    public android.os.Bundle saveViewport(){
        var state=new android.os.Bundle();state.putBoolean("follow",map.isFollowing());
        if(map.mapView()!=null){var p=map.mapView().getModel().mapViewPosition;
            state.putDouble("lat",p.getCenter().latitude);state.putDouble("lon",p.getCenter().longitude);state.putByte("zoom",p.getZoomLevel());}
        else if(viewport!=null)state.putAll(viewport);
        return state;
    }
    public void restoreViewport(android.os.Bundle state){viewport=state;if(state!=null)map.setFollowing(state.getBoolean("follow",true));}
    private final Runnable coverage=this::checkCoverage;

    public RouteMapView(Context context,AttributeSet attributes){
        super(context,attributes);
        repository=new OfflineMapRepository(context);
        map=new OfflineMapView(context);addView(map,new LayoutParams(-1,-1));
        status=new TextView(context);status.setText(R.string.p06_map_empty);
        status.setTextColor(0xff202020);status.setBackgroundColor(0xeef5f5f5);
        status.setTextSize(11);status.setPadding(8,2,8,2);
        addView(status,new LayoutParams(-1,-2,Gravity.TOP));
        Button locate=new Button(context);locate.setId(R.id.p07_home_locate);
        locate.setText(R.string.p06_my_position);
        locate.setTextSize(10);locate.setMinHeight(0);locate.setMinimumHeight(0);
        addView(locate,new LayoutParams(-2,(int)(40*getResources().getDisplayMetrics().density),Gravity.BOTTOM|Gravity.END));
        locate.setOnClickListener(v->{map.setFollowing(true);map.centerPosition();scheduleCoverage();});
        map.setOnViewportChanged(this::scheduleCoverage);
    }
    public void bind(RoutePreviewModel.Result result,Position fix){
        points=result.points();current=fix;map.bind(points,current);
        if(map.isFollowing())map.centerPosition();
        scheduleCoverage();
    }
    public int recordedPointCount(){return points.size();}
    public OfflineMapView offlineMap(){return map;}
    public CharSequence mapStatus(){return status.getText();}
    public void start(){
        if(active)return;active=true;long token=++generation;
        worker=Executors.newSingleThreadExecutor();
        worker.execute(()->{
            org.mapsforge.map.reader.MapFile file=null;String id=null;boolean failed=false;
            try{var state=repository.read();if(state.current()!=null){id=state.selected();file=repository.open(state.current());}}
            catch(Exception error){failed=true;}
            var opened=file;var chosen=id;var error=failed;
            main.post(()->{
                if(!active||token!=generation){if(opened!=null)opened.close();return;}
                selected=chosen;
                try{if(opened!=null)map.load(opened);else map.loadEmpty();}
                catch(RuntimeException failure){if(opened!=null)opened.close();map.loadEmpty();selected=null;}
                if(viewport!=null&&viewport.containsKey("zoom")){
                    map.mapView().setCenter(new org.mapsforge.core.model.LatLong(viewport.getDouble("lat"),viewport.getDouble("lon")));
                    map.mapView().setZoomLevel(viewport.getByte("zoom"));
                }
                if(viewport==null||!viewport.containsKey("zoom"))map.mapView().setZoomLevel((byte)15);
                map.bind(points,current);if(map.isFollowing())map.centerPosition();
                status.setText(error?R.string.p06_map_failed:selected==null?R.string.p07_no_map:R.string.p07_map_checking);
                scheduleCoverage();
            });
        });
    }
    public void stop(){
        if(active)viewport=saveViewport();
        active=false;++generation;main.removeCallbacks(coverage);
        if(worker!=null)worker.shutdownNow();worker=null;checking=false;coverageScheduled=false;map.clear();
    }
    private void scheduleCoverage(){
        // Mapsforge observers can run on its animation thread; serialize all UI state.
        main.post(()->{
            if(!active||coverageScheduled)return;
            coverageScheduled=true;main.postDelayed(coverage,300);
        });
    }
    private void checkCoverage(){
        coverageScheduled=false;
        if(!active||selected==null||checking||map.mapView()==null)return;
        checking=true;long token=generation;
        var center=map.mapView().getModel().mapViewPosition.getCenter();
        byte zoom=map.mapView().getModel().mapViewPosition.getZoomLevel();
        String id=selected;
        worker.execute(()->{
            boolean features=false;
            try{
                var entry=repository.read().entries().stream().filter(e->e.id().equals(id)).findFirst().orElse(null);
                if(entry!=null){
                    var file=repository.open(entry);try{
                    var tile=new org.mapsforge.core.model.Tile(
                        org.mapsforge.core.util.MercatorProjection.longitudeToTileX(center.longitude,zoom),
                        org.mapsforge.core.util.MercatorProjection.latitudeToTileY(center.latitude,zoom),zoom,256);
                    var data=file.supportsTile(tile)?file.readMapData(tile):null;
                    features=data!=null&&(!data.ways.isEmpty()||!data.pois.isEmpty());
                    }finally{file.close();}
                }
            }catch(Exception unavailable){/* Missing/corrupt files never imply geographic coverage. */}
            boolean available=features;
            main.post(()->{if(!active||token!=generation)return;checking=false;
                status.setText(available?R.string.p07_map_ready:R.string.p07_no_features);
                if(!center.equals(map.mapView().getModel().mapViewPosition.getCenter())||zoom!=map.mapView().getModel().mapViewPosition.getZoomLevel())scheduleCoverage();
            });
        });
    }
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
}
