package de.dennisguse.opentracks.p0.gps;

/** Monotonic, transport-independent start gate. It never creates or persists a workout. */
public final class GpsReadiness {
    public record Policy(double accuracyMeters, long maxAgeNanos, int consecutiveFixes,
                         long stableSpanNanos, double maxSpeedMetersPerSecond, long timeoutNanos) {
        public Policy {
            if (!Double.isFinite(accuracyMeters) || accuracyMeters <= 0 || maxAgeNanos <= 0
                    || consecutiveFixes < 2 || stableSpanNanos <= 0 || timeoutNanos <= 0
                    || !Double.isFinite(maxSpeedMetersPerSecond) || maxSpeedMetersPerSecond <= 0)
                throw new IllegalArgumentException("Invalid GPS readiness policy");
        }
    }
    public static final Policy DEFAULT = new Policy(30, 5_000_000_000L, 3,
            1_000_000_000L, 20, 60_000_000_000L);
    public record Fix(double latitude, double longitude, boolean hasAccuracy,
                      double accuracyMeters, long elapsedNanos) {}
    private final Policy policy;
    private final long startedNanos;
    private Fix previous;
    private long firstNanos;
    private int consecutive;
    private boolean ready;

    public GpsReadiness(long startedNanos) { this(startedNanos, DEFAULT); }
    public GpsReadiness(long startedNanos, Policy policy) {
        this.startedNanos = startedNanos;
        this.policy = policy;
    }
    public static boolean usable(Fix fix, long nowNanos, long afterNanos, Policy policy) {
        return fix != null && Double.isFinite(fix.latitude()) && Math.abs(fix.latitude()) <= 90
                && Double.isFinite(fix.longitude()) && Math.abs(fix.longitude()) <= 180
                && fix.hasAccuracy() && Double.isFinite(fix.accuracyMeters())
                && fix.accuracyMeters() >= 0 && fix.accuracyMeters() <= policy.accuracyMeters()
                && fix.elapsedNanos() > 0 && fix.elapsedNanos() > afterNanos
                && fix.elapsedNanos() <= nowNanos && nowNanos - fix.elapsedNanos() <= policy.maxAgeNanos();
    }
    public boolean add(Fix fix, long nowNanos) {
        if (ready) return false;
        if (!usable(fix, nowNanos, startedNanos, policy)) { reset(); return false; }
        if (previous != null && fix.elapsedNanos() <= previous.elapsedNanos()) return false;
        if (previous != null) {
            long gap = fix.elapsedNanos() - previous.elapsedNanos();
            double accuracyAllowance = Math.min(15, fix.accuracyMeters() + previous.accuracyMeters());
            double allowed = Math.max(8, accuracyAllowance)
                    + policy.maxSpeedMetersPerSecond() * (gap / 1_000_000_000.0);
            if (gap > policy.maxAgeNanos() || distance(previous, fix) > allowed) reset();
        }
        if (consecutive == 0) firstNanos = fix.elapsedNanos();
        previous = fix;
        consecutive++;
        ready = consecutive >= policy.consecutiveFixes() && fix.elapsedNanos() - firstNanos >= policy.stableSpanNanos();
        return ready;
    }
    public boolean timedOut(long nowNanos) {
        return !ready && nowNanos >= startedNanos && nowNanos - startedNanos >= policy.timeoutNanos();
    }
    private void reset() { previous = null; consecutive = 0; }
    private static double distance(Fix a, Fix b) {
        double dLat = Math.toRadians(b.latitude() - a.latitude());
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(a.latitude()))
                * Math.cos(Math.toRadians(b.latitude())) * Math.pow(Math.sin(dLon / 2), 2);
        return 6_371_000 * 2 * Math.asin(Math.sqrt(Math.min(1, h)));
    }
}
