package de.dennisguse.opentracks.p0.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.DashPathEffect;
import android.util.AttributeSet;
import android.view.View;
import java.util.List;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.chart.HeartRatePlot;
import de.dennisguse.opentracks.ui.util.ThemeUtils;

/** Compact native preview. It draws only real notifications and never fills gaps with a waveform. */
public final class PulseChartView extends View {
    private List<HeartRatePlot.Point> points=List.of();private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private ValueAnimator feedback;private float halo;
    public PulseChartView(Context c,AttributeSet a){super(c,a);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    public void setPoints(List<HeartRatePlot.Point> incoming){
        boolean newer=!incoming.isEmpty()&&(points.isEmpty()||incoming.get(incoming.size()-1).x()!=points.get(points.size()-1).x());points=List.copyOf(incoming);
        setContentDescription(getContext().getString(points.isEmpty()?R.string.gardenia_chart_empty:R.string.gardenia_chart_summary,points.size()));
        if(newer&&isShown()&&ValueAnimator.areAnimatorsEnabled()){
            if(feedback!=null)feedback.cancel();feedback=ValueAnimator.ofFloat(1,0);feedback.setDuration(240);feedback.addUpdateListener(a->{halo=(Float)a.getAnimatedValue();invalidate();});feedback.start();
        }invalidate();
    }
    public int sampleCount(){return points.size();}
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
    @Override protected void onDetachedFromWindow(){if(feedback!=null)feedback.cancel();super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float left=dp(25),right=getWidth()-dp(8),top=dp(9),bottom=getHeight()-dp(17);if(right<=left||bottom<=top)return;
        int accent=ThemeUtils.color(getContext(),R.attr.gardeniaChart),fg=ThemeUtils.color(getContext(),com.google.android.material.R.attr.colorOnSurfaceVariant);double min=100,max=180;
        if(!points.isEmpty()){min=Math.floor(points.stream().mapToDouble(HeartRatePlot.Point::bpm).min().orElse(100)/20)*20;max=Math.ceil(points.stream().mapToDouble(HeartRatePlot.Point::bpm).max().orElse(180)/20)*20;if(max-min<40){min-=20;max+=20;}}
        paint.setTextSize(dp(8));paint.setStrokeWidth(dp(.7f));
        for(int i=0;i<3;i++){float y=top+(bottom-top)*i/2;paint.setColor(ThemeUtils.color(getContext(),R.attr.gardeniaChartGrid));paint.setPathEffect(new DashPathEffect(new float[]{dp(2),dp(3)},0));c.drawLine(left,y,right,y,paint);paint.setPathEffect(null);paint.setColor(fg);c.drawText(Integer.toString((int)(max-(max-min)*i/2)),0,y+dp(3),paint);}
        double seconds=points.size()<2?1:Math.max(1,Math.min(60,Math.ceil((points.get(points.size()-1).x()-points.get(0).x())/1000)));c.drawText("-"+(int)seconds+"s",left,bottom+dp(13),paint);paint.setTextAlign(Paint.Align.RIGHT);c.drawText("0s",right,bottom+dp(13),paint);paint.setTextAlign(Paint.Align.LEFT);
        if(points.isEmpty()){
            paint.setColor(fg);paint.setTextSize(dp(10));paint.setTextAlign(Paint.Align.CENTER);paint.setStyle(Paint.Style.FILL);c.drawText(getContext().getString(R.string.p04_chart_wait),(left+right)/2,(top+bottom)/2-dp(4),paint);paint.setTextSize(dp(8));c.drawText(getContext().getString(R.string.p04_chart_gap),(left+right)/2,(top+bottom)/2+dp(10),paint);paint.setTextAlign(Paint.Align.LEFT);return;
        }
        double end=points.get(points.size()-1).x(),start=Math.max(points.get(0).x(),end-60000),span=Math.max(1000,end-start);start=end-span;Path path=new Path();boolean begin=true;float lastX=left,lastY=bottom;
        for(var p:points){if(p.x()<start){begin=true;continue;}float x=left+(float)((p.x()-start)/span)*(right-left),y=bottom-(float)((p.bpm()-min)/(max-min))*(bottom-top);if(begin||p.breakBefore())path.moveTo(x,y);else path.lineTo(x,y);lastX=x;lastY=y;begin=false;}
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1.8f));paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(accent);c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);paint.setAlpha((int)(halo*70));c.drawCircle(lastX,lastY,dp(4+halo*6),paint);paint.setAlpha(255);c.drawCircle(lastX,lastY,dp(2.4f),paint);
    }
}
