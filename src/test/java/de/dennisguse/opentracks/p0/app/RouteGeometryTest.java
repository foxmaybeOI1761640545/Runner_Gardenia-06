package de.dennisguse.opentracks.p0.app;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class RouteGeometryTest {
 @Test public void noLocationsProduceNoFabricatedRoute(){assertTrue(RouteGeometry.project(List.of(),360,240,24).isEmpty());}
 @Test public void localProjectionFitsRecordedPositionsAndPreservesBreaks(){
  var input=List.of(new RouteGeometry.Point(1.3,103.8,true),new RouteGeometry.Point(1.301,103.801,false),new RouteGeometry.Point(1.302,103.802,true));var out=RouteGeometry.project(input,350,238,24);assertEquals(3,out.size());assertTrue(out.get(0).breakBefore());assertFalse(out.get(1).breakBefore());assertTrue(out.get(2).breakBefore());for(var p:out){assertTrue(p.x()>=23.9&&p.x()<=326.1);assertTrue(p.y()>=23.9&&p.y()<=214.1);}assertTrue(out.get(2).y()<out.get(0).y());
 }
 @Test public void invalidLocationCannotJoinSeparatedSegments(){var out=RouteGeometry.project(List.of(new RouteGeometry.Point(0,0,true),new RouteGeometry.Point(Double.NaN,0,false),new RouteGeometry.Point(.001,.001,false)),300,200,20);assertEquals(2,out.size());assertTrue(out.get(1).breakBefore());}
 @Test public void crossingDateLineStaysLocal(){var out=RouteGeometry.project(List.of(new RouteGeometry.Point(10,179.999,true),new RouteGeometry.Point(10,-179.999,false)),300,200,20);assertEquals(2,out.size());assertTrue(out.get(1).x()>out.get(0).x());}
 @Test public void reductionNeverJoinsAcrossDiscardedGapsAndIsBounded(){List<RouteGeometry.Point> input=new ArrayList<>();for(int i=0;i<10000;i++)input.add(new RouteGeometry.Point(1.3+i*.000001,103.8,true));var result=RouteGeometry.bounded(input,100);assertTrue(result.size()<=200);assertEquals(input.get(0),result.get(0));assertEquals(input.get(input.size()-1),result.get(result.size()-1));assertTrue(result.stream().allMatch(RouteGeometry.Point::breakBefore));}
}
