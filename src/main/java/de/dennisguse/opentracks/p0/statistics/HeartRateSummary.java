package de.dennisguse.opentracks.p0.statistics;

/** Notification-weighted statistics. Equal BPM values remain separate observations. */
public record HeartRateSummary(long count, double minimum, double maximum, double average) {
    public static final HeartRateSummary EMPTY=new HeartRateSummary(0,Double.NaN,Double.NaN,Double.NaN);
    public static final class Accumulator {
        private long count;
        private double sum,minimum=Double.POSITIVE_INFINITY,maximum=Double.NEGATIVE_INFINITY;
        public void add(double bpm) {
            if(!Double.isFinite(bpm)||bpm<=0||bpm>65535)return;
            count++;sum+=bpm;minimum=Math.min(minimum,bpm);maximum=Math.max(maximum,bpm);
        }
        public HeartRateSummary summary() {
            return count==0?EMPTY:new HeartRateSummary(count,minimum,maximum,sum/count);
        }
    }
}
