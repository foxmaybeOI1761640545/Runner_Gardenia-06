package de.dennisguse.opentracks.p0.map;

import java.util.*;
import de.dennisguse.opentracks.p0.app.RouteGeometry;

/** Web Mercator viewport, including tracks that cross the date line. No recording mutation. */
public final class MapViewport {
    private MapViewport(){}
    public record View(double latitude,double longitude,byte zoom){}
    public static View fit(List<RouteGeometry.Point> input,int width,int height,int tileSize){
        List<RouteGeometry.Point> points=input.stream().filter(RouteGeometry.Point::valid).toList();
        if(points.isEmpty())throw new IllegalArgumentException("No valid positions");
        double[] longitude=points.stream().mapToDouble(p->(p.longitude()+360)%360).sorted().toArray();
        double largest=-1;int after=0;
        for(int i=0;i<longitude.length;i++){double gap=(i+1==longitude.length?longitude[0]+360:longitude[i+1])-longitude[i];if(gap>largest){largest=gap;after=(i+1)%longitude.length;}}
        double span=360-largest,center=(longitude[after]+span/2+540)%360-180;
        double min=1,max=0;for(var p:points){double y=mercatorY(p.latitude());min=Math.min(min,y);max=Math.max(max,y);}
        double scaleX=Math.max(1,width-96)/(tileSize*Math.max(span/360,1E-12));
        double scaleY=Math.max(1,height-96)/(tileSize*Math.max(max-min,1E-12));
        int zoom=(int)Math.floor(Math.log(Math.min(scaleX,scaleY))/Math.log(2));zoom=Math.max(2,Math.min(18,zoom));
        return new View(Math.toDegrees(Math.atan(Math.sinh(Math.PI*(1-2*((min+max)/2))))),center,(byte)zoom);
    }
    private static double mercatorY(double latitude){double lat=Math.toRadians(Math.max(-85.05112878,Math.min(85.05112878,latitude)));return (1-Math.log(Math.tan(lat)+1/Math.cos(lat))/Math.PI)/2;}
}
