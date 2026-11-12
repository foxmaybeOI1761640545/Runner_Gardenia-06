package de.dennisguse.opentracks.p0.map;

import android.content.Context;
import android.widget.FrameLayout;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes;
import de.dennisguse.opentracks.p0.app.RouteGeometry;
import de.dennisguse.opentracks.data.models.Position;
import java.util.*;

/** Owns exactly one native offline renderer. No downloader or network tile layer. */
public final class OfflineMapView extends FrameLayout {
    private MapView map;
    private final List<Layer> overlays=new ArrayList<>();
    private List<RouteGeometry.Point> route=List.of();
    private Position position;
    private int segmentCount;
    private final java.util.concurrent.atomic.AtomicInteger renderedTiles=new java.util.concurrent.atomic.AtomicInteger();
    public OfflineMapView(Context context){super(context);setContentDescription("Offline vector map");}
    public void load(MapFile file){
        clear();AndroidGraphicFactory.createInstance(getContext().getApplicationContext());
        map=new MapView(getContext());map.setClickable(true);map.getMapScaleBar().setVisible(true);map.setBuiltInZoomControls(true);map.setZoomLevelMin((byte)2);map.setZoomLevelMax((byte)20);
        addView(map,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        int tileSize=map.getModel().displayModel.getTileSize();
        int capacity=(int)Math.max(8,Math.min(64,32L*1024*1024/(tileSize*(long)tileSize*4)));
        renderedTiles.set(0);
        var cache=new InMemoryTileCache(capacity){
            @Override public synchronized void put(org.mapsforge.map.layer.queue.Job job,org.mapsforge.core.graphics.TileBitmap bitmap){super.put(job,bitmap);renderedTiles.incrementAndGet();}
        };
        var renderer=new TileRendererLayer(cache,file,map.getModel().mapViewPosition,AndroidGraphicFactory.INSTANCE);
        renderer.setXmlRenderTheme(MapsforgeThemes.DEFAULT);map.getLayerManager().getLayers().add(renderer);
        map.setCenter(file.startPosition());map.setZoomLevel(file.startZoomLevel());
        drawOverlays();
    }
    public void bind(List<RouteGeometry.Point> points,Position current){route=List.copyOf(points);position=current;drawOverlays();}
    private void drawOverlays(){
        if(map==null)return;var layers=map.getLayerManager().getLayers();for(Layer layer:overlays)layers.remove(layer);overlays.clear();segmentCount=0;
        Polyline line=null;
        for(var p:route){
            if(!p.valid()){line=null;continue;}
            if(line==null||p.breakBefore()){
                var paint=AndroidGraphicFactory.INSTANCE.createPaint();paint.setColor(0xff007f86);paint.setStrokeWidth(6*getResources().getDisplayMetrics().density);paint.setStyle(Style.STROKE);
                line=new Polyline(paint,AndroidGraphicFactory.INSTANCE);overlays.add(line);layers.add(line);segmentCount++;
            }
            line.getLatLongs().add(new LatLong(p.latitude(),p.longitude()));
        }
        if(position!=null&&position.hasValidLocation()&&Double.isFinite(position.latitude())&&Double.isFinite(position.longitude())){
            var fill=AndroidGraphicFactory.INSTANCE.createPaint();fill.setColor(0xff9b3dc8);fill.setStyle(Style.FILL);
            var stroke=AndroidGraphicFactory.INSTANCE.createPaint();stroke.setColor(0xffffffff);stroke.setStrokeWidth(3);stroke.setStyle(Style.STROKE);
            Circle marker=new Circle(new LatLong(position.latitude(),position.longitude()),8,fill,stroke);overlays.add(marker);layers.add(marker);
        }
        map.getLayerManager().redrawLayers();
    }
    public void fitRoute(){if(map==null||route.isEmpty())return;var v=MapViewport.fit(route,getWidth(),getHeight(),map.getModel().displayModel.getTileSize());map.setCenter(new LatLong(v.latitude(),v.longitude()));map.setZoomLevel(v.zoom());}
    public boolean centerPosition(){if(map==null||position==null||!position.hasValidLocation())return false;map.setCenter(new LatLong(position.latitude(),position.longitude()));return true;}
    public int segmentCount(){return segmentCount;}
    public int renderedTileCount(){return renderedTiles.get();}
    public MapView mapView(){return map;}
    public void clear(){if(map!=null){map.destroyAll();removeView(map);map=null;}overlays.clear();}
}
