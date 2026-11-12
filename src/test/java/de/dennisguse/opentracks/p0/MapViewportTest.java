package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.List;
import de.dennisguse.opentracks.p0.map.MapViewport;
import de.dennisguse.opentracks.p0.app.RouteGeometry.Point;

public class MapViewportTest {
    @Test public void localTrackFitsAroundItsCentre(){var v=MapViewport.fit(List.of(new Point(1.299,103.799,true),new Point(1.301,103.801,false)),1080,1600,256);assertEquals(1.3,v.latitude(),.00001);assertEquals(103.8,v.longitude(),.00001);assertTrue(v.zoom()>=17);}
    @Test public void dateLineUsesShortArc(){var v=MapViewport.fit(List.of(new Point(10,179.99,true),new Point(10,-179.99,false)),1080,1600,256);assertEquals(180,Math.abs(v.longitude()),.000001);assertTrue(v.zoom()>10);}
    @Test public void singlePointAndTinyScreenAreFinite(){var v=MapViewport.fit(List.of(new Point(90,0,true)),0,0,256);assertTrue(Double.isFinite(v.latitude()));assertTrue(v.latitude()<=85.052);assertTrue(v.zoom()<=18);}
    @Test public void invalidPositionsCannotAffectViewport(){var v=MapViewport.fit(List.of(new Point(Double.NaN,5,true),new Point(1,2,true)),1000,1000,256);assertEquals(1,v.latitude(),1E-8);assertEquals(2,v.longitude(),1E-8);}
    @Test(expected=IllegalArgumentException.class)public void emptyRouteIsRejected(){MapViewport.fit(List.of(),1000,1000,256);}
    @Test public void separatedSegmentsFitWithoutChangingTheirMarkers(){var points=List.of(new Point(-30,10,true),new Point(30,20,true));var v=MapViewport.fit(points,1000,1000,256);assertEquals(0,v.latitude(),1E-8);assertEquals(15,v.longitude(),1E-8);assertTrue(points.get(1).breakBefore());}
    @Test public void rendererCannotDrawAnAcrossWorldLineAtLongitudeSeam(){var points=List.of(new Point(10,179.99,true),new Point(10,-179.99,false),new Point(10,-179.98,false));var groups=de.dennisguse.opentracks.p0.map.MapRouteSegments.split(points);assertEquals(List.of(1,2),groups.stream().map(List::size).toList());assertEquals(3,groups.stream().mapToInt(List::size).sum());}
    @Test public void invalidPointAndExplicitBreakKeepSeparatePolylines(){var groups=de.dennisguse.opentracks.p0.map.MapRouteSegments.split(List.of(new Point(1,2,true),new Point(Double.NaN,2,false),new Point(1,3,false),new Point(1,4,true)));assertEquals(3,groups.size());}
}
