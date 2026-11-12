package de.dennisguse.opentracks.p0.map;

import de.dennisguse.opentracks.p0.app.RouteGeometry.Point;
import java.util.*;

/** Map-only grouping. A longitude seam must never create a line around the world. */
public final class MapRouteSegments {
    private MapRouteSegments(){}
    public static List<List<Point>> split(List<Point> points){
        List<List<Point>> result=new ArrayList<>();List<Point> current=null;Point previous=null;
        for(Point point:points){
            if(!point.valid()){current=null;previous=null;continue;}
            if(current==null||point.breakBefore()||Math.abs(point.longitude()-previous.longitude())>180){current=new ArrayList<>();result.add(current);}
            current.add(point);previous=point;
        }
        return result.stream().map(List::copyOf).toList();
    }
}
