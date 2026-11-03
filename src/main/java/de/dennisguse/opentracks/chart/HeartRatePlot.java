package de.dennisguse.opentracks.chart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Bounded extrema-preserving plot reduction. Gaps are evaluated before reduction. */
public final class HeartRatePlot {
    public record Point(double x, double bpm, boolean breakBefore) {}
    public record DistanceAnchor(double activeMillis, double distance) {}
    private HeartRatePlot() {}

    /** Projection changes only the HR chart's axis, never stored GPS/distance statistics. */
    public static List<Point> byDistance(List<Point> input, List<DistanceAnchor> anchors) {
        if (anchors.isEmpty()) return List.of();
        List<Point> result = new ArrayList<>();
        for (Point point : input) {
            int low=0,high=anchors.size()-1;
            while (low<high) {
                int middle=(low+high+1)/2;
                if (anchors.get(middle).activeMillis()<=point.x()) low=middle; else high=middle-1;
            }
            DistanceAnchor left=anchors.get(low),right=anchors.get(Math.min(low+1,anchors.size()-1));
            double ratio=right.activeMillis()>left.activeMillis()
                    ? Math.max(0,Math.min(1,(point.x()-left.activeMillis())/(right.activeMillis()-left.activeMillis()))) : 0;
            double distance=left.distance()+ratio*(right.distance()-left.distance());
            result.add(new Point(distance,point.bpm(),point.breakBefore()));
        }
        return List.copyOf(result);
    }
    public static List<Point> reduce(List<Point> input, int budget) {
        if (budget < 8) throw new IllegalArgumentException("Plot budget too small");
        if (input.size() <= budget) return List.copyOf(input);
        int stride = (int)Math.ceil(input.size() / (budget / 4d));
        List<Point> result = new ArrayList<>();
        for (int start = 0; start < input.size(); start += stride) {
            int end = Math.min(input.size(), start + stride);
            int min = start, max = start; boolean gap = false;
            for (int i = start; i < end; i++) {
                if (input.get(i).bpm() < input.get(min).bpm()) min = i;
                if (input.get(i).bpm() > input.get(max).bpm()) max = i;
                gap |= input.get(i).breakBefore();
            }
            List<Integer> chosen = new ArrayList<>(List.of(start, min, max, end - 1));
            chosen.sort(Comparator.naturalOrder()); int previous = -1;
            for (int i : chosen) {
                if (i == previous) continue;
                Point p = input.get(i);
                // At coarse scales, omit uncertain connections rather than bridging a hidden gap.
                result.add(new Point(p.x(),p.bpm(),gap || p.breakBefore()));previous = i;
            }
        }
        return List.copyOf(result);
    }
}
