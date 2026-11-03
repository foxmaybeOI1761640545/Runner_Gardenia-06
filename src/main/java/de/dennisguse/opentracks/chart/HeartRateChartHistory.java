package de.dennisguse.opentracks.chart;

import android.content.Context;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import de.dennisguse.opentracks.data.CustomContentProvider;
import de.dennisguse.opentracks.data.HeartRateRepository;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Track;

/** Per-fragment incremental reader; SQLite stays on TrackDataHub's background thread. */
final class HeartRateChartHistory {
    private Track.Id track;
    private long lastId;
    private HeartRate previous;
    private List<HeartRatePlot.Point> points = new ArrayList<>();
    private double sum, min = Double.POSITIVE_INFINITY, max;
    private long count;
    record Result(List<HeartRatePlot.Point> points, long count, double min, double average, double max) {}

    Result load(Context context, Track.Id selected) {
        if (!selected.equals(track)) {
            track = selected; lastId = 0; previous = null; points = new ArrayList<>();
            sum = 0; min = Double.POSITIVE_INFINITY; max = 0; count = 0;
        }
        List<long[]> intervals = intervals(context, selected);
        new HeartRateRepository(context).forEachAfter(selected,lastId,(id,value) -> {
            lastId = Math.max(lastId,id);
            long time = value.sample().capturedAt().toEpochMilli();
            Double x = activeMillis(intervals,time);
            if (x == null) return;
            boolean gap = previous == null || time - previous.sample().capturedAt().toEpochMilli() > 5000
                    || !Objects.equals(value.sample().connectionId(),previous.sample().connectionId());
            points.add(new HeartRatePlot.Point(x,value.value(),gap));
            sum += value.value();min = Math.min(min,value.value());max = Math.max(max,value.value());count++;
            previous = value;
            if (points.size() > 8000) points = new ArrayList<>(HeartRatePlot.reduce(points,4000));
        });
        return new Result(HeartRatePlot.reduce(points,4000),count,min,count == 0 ? 0 : sum/count,max);
    }
    private static List<long[]> intervals(Context context,Track.Id track) {
        List<long[]> result = new ArrayList<>();long start = -1, elapsed = 0;
        try (Cursor c = context.getContentResolver().query(CustomContentProvider.UrlType.TRACKPOINTS.getUri(),
                new String[]{"time","type"},"trackid=? AND type IN (-2,1)",new String[]{Long.toString(track.id())},"_id")) {
            if (c == null) throw new IllegalStateException("Missing recording timeline");
            while (c.moveToNext()) {
                long time = c.getLong(0);
                if (c.getInt(1) == -2) start = time;
                else if (start >= 0) { result.add(new long[]{start,time,elapsed});elapsed += Math.max(0,time-start);start = -1; }
            }
        }
        if (start >= 0) result.add(new long[]{start,Long.MAX_VALUE,elapsed});
        return result;
    }
    static Double activeMillis(List<long[]> intervals,long time) {
        for (long[] range : intervals) if (time >= range[0] && time <= range[1]) return (double)(range[2]+time-range[0]);
        return null;
    }
}
