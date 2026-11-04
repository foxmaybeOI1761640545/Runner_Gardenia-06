package de.dennisguse.opentracks.p0.export;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Conservative GPX projection; unmatched notifications still remain in the raw extension. */
public final class HeartRateTimeMatcher {
    public record Anchor(long pointId, Instant time, int segment) {}
    private record Segment(List<Anchor> points, Instant start, Instant end) {}
    private final List<Segment> segments = new ArrayList<>();
    private boolean ambiguous;

    public HeartRateTimeMatcher(List<Anchor> anchors) {
        List<Anchor> current = new ArrayList<>();
        for (Anchor anchor : anchors) {
            if (!current.isEmpty() && current.get(0).segment() != anchor.segment()) {
                add(current); current = new ArrayList<>();
            }
            if (!current.isEmpty() && anchor.time().isBefore(current.get(current.size()-1).time())) ambiguous = true;
            current.add(anchor);
        }
        add(current);
        segments.sort(Comparator.comparing(Segment::start));
        for (int i=1; i<segments.size(); i++) {
            if (!segments.get(i).start().isAfter(segments.get(i-1).end())) ambiguous = true;
        }
    }
    private void add(List<Anchor> points) {
        if (!points.isEmpty()) segments.add(new Segment(List.copyOf(points),points.get(0).time(),points.get(points.size()-1).time()));
    }
    public Anchor match(Instant time) {
        if (ambiguous || time == null) return null;
        int low=0, high=segments.size()-1, selected=-1;
        while (low<=high) {
            int middle=(low+high)>>>1;
            if (segments.get(middle).start().isAfter(time)) high=middle-1;
            else {selected=middle;low=middle+1;}
        }
        if (selected<0) return null;
        Segment segment=segments.get(selected);
        if (time.isAfter(segment.end())) return null;
        List<Anchor> points=segment.points(); low=0;high=points.size();
        while (low<high) {
            int middle=(low+high)>>>1;
            if (points.get(middle).time().isBefore(time)) low=middle+1; else high=middle;
        }
        Anchor next=low<points.size()?points.get(low):null;
        Anchor previous=low>0?points.get(low-1):null;
        Anchor closest=previous==null?next:next==null?previous:
                difference(previous.time(),time).compareTo(difference(next.time(),time))<=0?previous:next;
        return closest!=null && difference(closest.time(),time).compareTo(Duration.ofSeconds(1))<=0?closest:null;
    }
    public static Duration difference(Instant first,Instant second) {return Duration.between(first,second).abs();}
}
