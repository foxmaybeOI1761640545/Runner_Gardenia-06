package de.dennisguse.opentracks.p0.app;

import android.content.Context;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.chart.ChartView;
import de.dennisguse.opentracks.chart.HeartRatePlot;
import de.dennisguse.opentracks.sensors.HeartRatePreview;
import de.dennisguse.opentracks.ui.HeartRatePanel;

/** Read-only, bounded UI projection of actual preview notifications; survives UI recreation. */
public final class PreviewChartModel extends ViewModel {
    private final List<HeartRatePlot.Point> points=new ArrayList<>();
    private long origin=-1,last=-1;
    private String connection;
    private boolean gap=true;
    public void bind(Context context,ChartView chart,HeartRatePreview preview) {
        var status=preview.status();var value=preview.value();
        boolean fresh=!HeartRatePanel.describe(context,value,status).bpm().equals(context.getString(R.string.hr_empty_value));
        if(!fresh)gap=true;
        else if(status.lastReceived()!=null) {
            long time=status.lastReceived().toEpochMilli();
            String id=value.sample()==null?status.device():value.sample().connectionId();
            if(time!=last) {
                if(origin<0)origin=time;
                points.add(new HeartRatePlot.Point(time-origin,value.getBPM(),gap||last<0||time-last>5000||!Objects.equals(connection,id)));
                last=time;connection=id;gap=false;
                if(points.size()>300)points.remove(0);
            }
        }
        chart.setChartByDistance(false);
        chart.setRawHeartRatePoints(List.copyOf(points));
        chart.setContentDescription(context.getString(points.isEmpty()?R.string.gardenia_chart_empty:R.string.gardenia_chart_summary,points.size()));
    }
}
