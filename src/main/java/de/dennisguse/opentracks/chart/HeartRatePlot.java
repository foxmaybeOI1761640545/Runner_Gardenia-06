package de.dennisguse.opentracks.chart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Bounded extrema-preserving plot reduction. Gaps are evaluated before reduction. */
public final class HeartRatePlot {
    public record Point(double x, double bpm, boolean breakBefore) {}
    private HeartRatePlot() {}
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
