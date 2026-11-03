package de.dennisguse.opentracks.chart;

import static org.junit.Assert.*;
import android.graphics.PathMeasure;
import android.view.View;
import android.view.ContextThemeWrapper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.List;
import de.dennisguse.opentracks.R;

public class HeartRatePathTest {
    private ChartView chart() {
        ChartView view=new ChartView(new ContextThemeWrapper(ApplicationProvider.getApplicationContext(),R.style.DayNightColorTheme),null);
        view.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));return view;
    }
    private int segments(ChartView view) {
        PathMeasure measure=new PathMeasure(view.heartRatePath(),false);int count=0;
        do { if(measure.getLength()>0) count++; } while(measure.nextContour());return count;
    }
    @Test public void rawReconnectCreatesTwoSeparatePathContours() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ChartView view=chart();view.setRawHeartRatePoints(List.of(new HeartRatePlot.Point(0,70,true),new HeartRatePlot.Point(1000,75,false),new HeartRatePlot.Point(2000,90,true),new HeartRatePlot.Point(3000,95,false)));
            assertEquals(2,segments(view));
        });
    }
    @Test public void legacyMissingValueBreaksInsteadOfBridging() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ChartView view=chart();view.addChartPoints(List.of(new ChartPoint(0,null,null,null,70d,null,null),new ChartPoint(1000,null,null,null,75d,null,null),new ChartPoint(1500,null,null,null,null,null,null),new ChartPoint(2000,null,null,null,90d,null,null),new ChartPoint(3000,null,null,null,95d,null,null)));
            assertEquals(2,segments(view));
        });
    }
    @Test public void fullUint16ProtocolRangeFitsWithoutAPersonalMaximumAssumption() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ChartView view=chart();view.setRawHeartRatePoints(List.of(new HeartRatePlot.Point(0,5,true),new HeartRatePlot.Point(1000,65535,false)));
            assertTrue(view.heartRateAxisMax()>=65535);assertEquals(1,segments(view));
        });
    }
    @Test public void distanceProjectionKeepsAnExplicitReconnectPathBreak() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ChartView view=chart();view.setChartByDistance(true);
            view.setRawHeartRatePoints(HeartRatePlot.byDistance(List.of(new HeartRatePlot.Point(0,70,true),new HeartRatePlot.Point(1000,75,false),new HeartRatePlot.Point(2000,80,true),new HeartRatePlot.Point(3000,85,false)),
                    List.of(new HeartRatePlot.DistanceAnchor(0,0),new HeartRatePlot.DistanceAnchor(3000,0.3))));
            assertEquals(2,segments(view));
        });
    }
    @Test public void resetRemovesOldPathAndExtrema() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ChartView view=chart();view.setRawHeartRatePoints(List.of(new HeartRatePlot.Point(0,70,true),new HeartRatePlot.Point(1000,75,false)));
            assertEquals(1,segments(view));view.reset();assertEquals(0,segments(view));
        });
    }
}
