package de.dennisguse.opentracks.p0.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.p0.map.OfflineMapView;
import de.dennisguse.opentracks.ui.util.ThemeUtils;
import org.mapsforge.map.reader.MapFile;

/** Compact home map: real offline basemap when available, live position even without map data. */
public final class RouteMapView extends FrameLayout {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final OfflineMapView offlineMap;
    private List<RouteGeometry.Point> points = List.of();
    private Position current;
    private boolean partial, failed, hasMap, follow = true;

    public RouteMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        offlineMap = new OfflineMapView(context);
        offlineMap.setVisibility(GONE);
        addView(offlineMap, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setBackgroundColor(ThemeUtils.color(context, R.attr.gardeniaSurfaceVariant));
        setContentDescription(context.getString(R.string.p06_map_empty));
    }

    /** The supplied MapFile is owned by OfflineMapView after this call. */
    public void load(MapFile file) {
        offlineMap.load(file);
        offlineMap.setVisibility(VISIBLE);
        hasMap = true;
        failed = false;
        var nativeMap = offlineMap.mapView();
        if (nativeMap != null) {
            nativeMap.setClickable(false);
            nativeMap.setOnTouchListener((view, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) performClick();
                return true;
            });
        }
        offlineMap.bind(points, current);
        if (follow && current != null) offlineMap.followPosition(current, (byte) 16);
        updateDescription();
        invalidate();
    }

    /** Clears only the renderer; the latest fix and route remain available for the next resume. */
    public void clearMap() {
        offlineMap.clear();
        offlineMap.setVisibility(GONE);
        hasMap = false;
        invalidate();
    }

    public void showNoMap() {
        clearMap();
        failed = false;
        updateDescription();
    }

    public void showMapError() {
        showNoMap();
        failed = true;
        updateDescription();
    }

    public void setFollowEnabled(boolean enabled) {
        follow = enabled;
    }

    public void bind(RoutePreviewModel.Result result, Position fix) {
        points = result.points();
        partial = result.partial();
        failed = result.failed();
        current = fix;
        if (hasMap) {
            offlineMap.bind(points, current);
            if (follow && current != null && current.hasValidLocation())
                offlineMap.followPosition(current, (byte) 16);
        }
        updateDescription();
        invalidate();
    }

    public int recordedPointCount() {
        return points.size();
    }

    private void updateDescription() {
        if (hasMap) {
            int segments = 0;
            for (var point : points) if (point.breakBefore()) segments++;
            setContentDescription(points.isEmpty()
                    ? getContext().getString(R.string.p04_route_wait)
                    : getContext().getString(R.string.p04_route_summary, points.size(), segments));
        } else if (current != null && current.hasValidLocation()) {
            setContentDescription(getContext().getString(R.string.p07_home_map_position_no_map));
        } else {
            setContentDescription(getContext().getString(failed ? R.string.p06_map_failed : R.string.p06_map_empty));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (hasMap) return;
        int foreground = ThemeUtils.color(getContext(),
                com.google.android.material.R.attr.colorOnSurfaceVariant);
        canvas.drawColor(ThemeUtils.color(getContext(), R.attr.gardeniaSurfaceVariant));
        float width = getWidth(), height = getHeight(), density = getResources().getDisplayMetrics().density;
        paint.setColor(foreground);
        paint.setAlpha(150);
        paint.setTextSize(11 * density);
        paint.setTextAlign(Paint.Align.CENTER);
        String label = getContext().getString(current != null && current.hasValidLocation()
                ? R.string.p07_home_map_no_map_short : R.string.p06_map_empty);
        canvas.drawText(label, width / 2, height / 2 + 4 * density, paint);
        if (current != null && current.hasValidLocation()) {
            float x = width / 2, y = height / 2 - 18 * density;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xff9b3dc8);
            paint.setAlpha(48);
            canvas.drawCircle(x, y, 14 * density, paint);
            paint.setAlpha(255);
            canvas.drawCircle(x, y, 5 * density, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xffffffff);
            paint.setStrokeWidth(2 * density);
            canvas.drawCircle(x, y, 5 * density, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setAlpha(255);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
