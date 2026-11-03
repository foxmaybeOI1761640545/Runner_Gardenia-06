package de.dennisguse.opentracks.chart;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class HeartRatePlotTest {
    private List<HeartRatePlot.Point> data(int count) {
        List<HeartRatePlot.Point> result = new ArrayList<>();
        for (int i=0;i<count;i++) result.add(new HeartRatePlot.Point(i,70+i%20,false));return result;
    }
    @Test public void smallInputRemainsExactAndImmutable() {
        var values=data(4);var reduced=HeartRatePlot.reduce(values,8);assertEquals(values,reduced);
        try { reduced.clear();fail("Mutable output"); } catch (UnsupportedOperationException expected) {}
    }
    @Test public void longRecordingIsBoundedAndChronological() {
        var reduced=HeartRatePlot.reduce(data(100000),4000);assertTrue(reduced.size()<=4000);
        assertEquals(0,reduced.get(0).x(),0);assertEquals(99999,reduced.get(reduced.size()-1).x(),0);
        for(int i=1;i<reduced.size();i++) assertTrue(reduced.get(i).x()>=reduced.get(i-1).x());
    }
    @Test public void narrowExtremaAreNotLost() {
        var input=data(20000);input.set(1234,new HeartRatePlot.Point(1234,38,false));input.set(1235,new HeartRatePlot.Point(1235,211,false));
        var result=HeartRatePlot.reduce(input,4000);assertTrue(result.stream().anyMatch(p->p.bpm()==38));assertTrue(result.stream().anyMatch(p->p.bpm()==211));
    }
    @Test public void hiddenGapsCannotBecomeConnectedAfterReduction() {
        var input=data(100);input.set(10,new HeartRatePlot.Point(10,75,true));var result=HeartRatePlot.reduce(input,8);
        assertTrue(result.stream().filter(p->p.x()<50).allMatch(HeartRatePlot.Point::breakBefore));
    }
    @Test public void rereReductionKeepsGapFlagsAndExtrema() {
        var input=data(100000);input.set(50000,new HeartRatePlot.Point(50000,230,true));
        var result=HeartRatePlot.reduce(HeartRatePlot.reduce(input,4000),800);
        assertTrue(result.size()<=800);assertTrue(result.stream().anyMatch(p->p.bpm()==230 && p.breakBefore()));
    }
    @Test(expected=IllegalArgumentException.class) public void invalidBudgetFailsExplicitly() { HeartRatePlot.reduce(data(10),0); }
}
