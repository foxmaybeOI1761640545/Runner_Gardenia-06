package de.dennisguse.opentracks.p0.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.ui.util.ThemeUtils;

/** Local vector terrain is explicitly illustrative; the route/position are actual recorded values. */
public final class RouteMapView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private List<RouteGeometry.Point> points=List.of();private Position current;private boolean partial,failed;
    public RouteMapView(Context c,AttributeSet a){super(c,a);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    public void bind(RoutePreviewModel.Result result,Position fix){points=result.points();partial=result.partial();failed=result.failed();current=fix;
        int segments=0;for(var p:points)if(p.breakBefore())segments++;
        setContentDescription(points.isEmpty()?getContext().getString(R.string.p04_route_wait):getContext().getString(R.string.p04_route_summary,points.size(),segments));invalidate();}
    public int recordedPointCount(){return points.size();}
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
    @Override protected void onDraw(Canvas c){super.onDraw(c);int accent=ThemeUtils.color(getContext(),R.attr.gardeniaAccentText),fg=ThemeUtils.color(getContext(),com.google.android.material.R.attr.colorOnSurfaceVariant);float w=getWidth(),h=getHeight();
        c.save();Path clip=new Path();clip.addRoundRect(new RectF(0,0,w,h),dp(9),dp(9),Path.Direction.CW);c.clipPath(clip);c.drawColor(ThemeUtils.color(getContext(),R.attr.gardeniaSurfaceVariant));
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(.7f));paint.setColor(fg);paint.setAlpha(32);
        for(int i=-2;i<8;i++){Path contour=new Path();contour.moveTo(-w*.1f,i*h*.15f);contour.cubicTo(w*.25f,h*(i*.15f+.7f),w*.45f,h*(i*.15f-.65f),w*1.1f,h*(i*.15f+.4f));c.drawPath(contour,paint);}
        paint.setStyle(Paint.Style.FILL);paint.setColor(fg);paint.setAlpha(255);paint.setTextSize(dp(8));c.drawText(getContext().getString(partial?R.string.p04_route_partial:R.string.p04_route_local),dp(10),dp(17),paint);c.drawText(getContext().getString(R.string.p04_north),w-dp(32),dp(36),paint);
        List<RouteGeometry.Point> located=new ArrayList<>(points);boolean hasFix=current!=null&&current.hasValidLocation();if(hasFix)located.add(new RouteGeometry.Point(current.latitude(),current.longitude(),true));
        if(located.isEmpty()){
            paint.setTextSize(dp(12));paint.setTextAlign(Paint.Align.CENTER);c.drawText(getContext().getString(R.string.p04_route_empty),w/2,h*.48f,paint);paint.setTextSize(dp(10));c.drawText(getContext().getString(failed?R.string.p0_operation_failed:R.string.p04_route_wait),w/2,h*.48f+dp(20),paint);paint.setTextAlign(Paint.Align.LEFT);
        }else{
            var projected=RouteGeometry.project(located,w,h,dp(34));paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setColor(accent);paint.setStrokeWidth(dp(2.5f));Path route=new Path();
            for(int i=0;i<projected.size()-(hasFix?1:0);i++){var p=projected.get(i);if(i==0||p.breakBefore())route.moveTo(p.x(),p.y());else route.lineTo(p.x(),p.y());}c.drawPath(route,paint);
            if(!points.isEmpty()){var start=projected.get(0);paint.setStyle(Paint.Style.FILL);paint.setColor(ThemeUtils.color(getContext(),com.google.android.material.R.attr.colorSurface));c.drawCircle(start.x(),start.y(),dp(4),paint);paint.setStyle(Paint.Style.STROKE);paint.setColor(accent);c.drawCircle(start.x(),start.y(),dp(4),paint);}
            if(hasFix){var p=projected.get(projected.size()-1);paint.setStyle(Paint.Style.FILL);paint.setColor(accent);paint.setAlpha(40);c.drawCircle(p.x(),p.y(),dp(10),paint);paint.setAlpha(255);c.drawCircle(p.x(),p.y(),dp(4.5f),paint);}
        }
        paint.setStyle(Paint.Style.FILL);paint.setColor(fg);paint.setAlpha(255);paint.setTextSize(dp(8));c.drawText(getContext().getString(R.string.p04_route_note),dp(10),h-dp(9),paint);c.restore();
    }
}
