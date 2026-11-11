package de.dennisguse.opentracks.p0.gps;

/**
 * Stateful, transport-independent GPS quality and motion filter.
 *
 * It preserves one-Hz samples while stationary, but pins their coordinates to a stable
 * anchor so GNSS drift cannot inflate distance. Motion transitions use hysteresis; isolated
 * jumps are rejected and a persistent relocation starts a new automatic track segment.
 */
public final class GpsMotionFilter {
    public static final double STOP_SPEED_MPS = 0.4;
    public static final double START_SPEED_MPS = 0.9;
    public static final double MIN_DISPLAY_SPEED_MPS = 0.3;

    private static final double SLOW_START_SPEED_MPS = 0.45;
    private static final double STOP_RADIUS_METERS = 3.0;
    private static final double START_RADIUS_METERS = 4.0;
    private static final double START_WITHOUT_SPEED_METERS = 10.0;
    private static final long START_WITHOUT_SPEED_NANOS = 5_000_000_000L;
    private static final long MAX_FIX_GAP_NANOS = 15_000_000_000L;
    private static final double MAX_SPEED_MPS = 20.0;
    private static final double BASE_JUMP_ALLOWANCE_METERS = 8.0;
    private static final double MAX_ACCURACY_ALLOWANCE_METERS = 15.0;
    private static final int STOP_CONFIRMATION_FIXES = 4;
    private static final int START_CONFIRMATION_FIXES = 2;
    private static final int MAX_JUMP_REJECTIONS = 3;

    private enum State { UNKNOWN, STATIONARY, MOVING }

    public record Fix(double latitude, double longitude, boolean hasAccuracy, double accuracyMeters,
                      boolean hasSpeed, double speedMetersPerSecond, long elapsedRealtimeNanos) {}

    public record Result(boolean accepted, boolean stationary, double latitude, double longitude,
                         boolean hasSpeed, double speedMetersPerSecond, boolean segmentStart) {
        private static Result rejected() {
            return new Result(false, false, Double.NaN, Double.NaN, false, Double.NaN, false);
        }
    }

    private State state = State.UNKNOWN;
    private Fix previous;
    private Fix stationaryAnchor;
    private Fix stopCandidateStart;
    private int lowSpeedFixes;
    private int startFixes;
    private int jumpRejections;
    private Fix relocationCandidate;
    private long lastSeenNanos;

    public synchronized Result update(Fix fix) {
        if (!valid(fix) || fix.elapsedRealtimeNanos() <= lastSeenNanos) return Result.rejected();
        long receivedGap = lastSeenNanos == 0 ? 0 : fix.elapsedRealtimeNanos() - lastSeenNanos;
        lastSeenNanos = fix.elapsedRealtimeNanos();

        boolean segmentStart = false;
        double distanceFromPrevious = 0;
        double secondsFromPrevious = 0;
        if (previous != null) {
            long gapNanos = fix.elapsedRealtimeNanos() - previous.elapsedRealtimeNanos();
            if (gapNanos <= 0) return Result.rejected();
            if (receivedGap > MAX_FIX_GAP_NANOS) {
                resetState();
                segmentStart = true;
            } else {
                secondsFromPrevious = gapNanos / 1_000_000_000.0;
                distanceFromPrevious = distanceMeters(previous, fix);
                double accuracyAllowance = Math.min(MAX_ACCURACY_ALLOWANCE_METERS,
                        previous.accuracyMeters() + fix.accuracyMeters());
                // A run of rejected updates is not a period of GPS silence.
                // Do not let its growing age eventually legitimize an arbitrary jump.
                double plausibilitySeconds = jumpRejections > 0
                        ? receivedGap / 1_000_000_000.0 : secondsFromPrevious;
                double allowed = Math.max(BASE_JUMP_ALLOWANCE_METERS, accuracyAllowance)
                        + MAX_SPEED_MPS * plausibilitySeconds;
                boolean impossibleSpeed = fix.hasSpeed() && fix.speedMetersPerSecond() > MAX_SPEED_MPS;
                if (distanceFromPrevious > allowed || impossibleSpeed) {
                    // Count a relocation only when the rejected fixes themselves form
                    // a credible local sequence. Three unrelated jumps are not a new track.
                    if (relocationCandidate == null || !consistent(relocationCandidate, fix)) {
                        jumpRejections = 1;
                    } else {
                        jumpRejections++;
                    }
                    relocationCandidate = fix;
                    if (jumpRejections < MAX_JUMP_REJECTIONS) return Result.rejected();
                    // Three consecutive, spatially consistent fixes at a new location are
                    // treated as a real relocation. The recorder marks a segment boundary.
                    resetState();
                    segmentStart = true;
                } else {
                    jumpRejections = 0;
                    relocationCandidate = null;
                }
            }
        }

        lastSeenNanos = fix.elapsedRealtimeNanos();
        double observedSpeed = fix.hasSpeed() ? fix.speedMetersPerSecond()
                : previous != null && secondsFromPrevious > 0
                ? distanceFromPrevious / secondsFromPrevious : Double.NaN;

        if (state == State.UNKNOWN) {
            state = Double.isFinite(observedSpeed) && observedSpeed >= START_SPEED_MPS
                    ? State.MOVING : State.STATIONARY;
            stationaryAnchor = state == State.STATIONARY ? fix : null;
            previous = fix;
            return state == State.STATIONARY ? stationary(fix, segmentStart)
                    : moving(fix, observedSpeed, segmentStart);
        }

        if (state == State.STATIONARY) {
            double fromAnchor = distanceMeters(stationaryAnchor, fix);
            boolean movementEvidence;
            if (fix.hasSpeed()) {
                movementEvidence = observedSpeed >= START_SPEED_MPS
                        || (observedSpeed >= SLOW_START_SPEED_MPS
                        && fromAnchor >= Math.max(START_RADIUS_METERS,
                        Math.min(10, stationaryAnchor.accuracyMeters() + fix.accuracyMeters())));
            } else {
                movementEvidence = fromAnchor >= START_WITHOUT_SPEED_METERS
                        && fix.elapsedRealtimeNanos() - stationaryAnchor.elapsedRealtimeNanos()
                        >= START_WITHOUT_SPEED_NANOS;
            }
            startFixes = movementEvidence ? startFixes + 1 : 0;
            previous = fix;
            if (startFixes >= START_CONFIRMATION_FIXES) {
                state = State.MOVING;
                stationaryAnchor = null;
                startFixes = 0;
                return moving(fix, observedSpeed, segmentStart);
            }
            return stationary(stationaryAnchor, segmentStart);
        }

        boolean slow = Double.isFinite(observedSpeed) && observedSpeed <= STOP_SPEED_MPS;
        if (slow) {
            if (stopCandidateStart == null) stopCandidateStart = previous;
            // Hold coordinates during stop confirmation as well. If motion
            // resumes, the next trusted point recovers the short displacement;
            // a stopped runner never accumulates the confirmation jitter.
            lowSpeedFixes++;
            if (distanceMeters(stopCandidateStart, fix) > STOP_RADIUS_METERS) {
                stopCandidateStart = fix;
                lowSpeedFixes = 1;
            }
            previous = fix;
            if (lowSpeedFixes >= STOP_CONFIRMATION_FIXES) {
                state = State.STATIONARY;
                stationaryAnchor = stopCandidateStart;
                stopCandidateStart = null;
                lowSpeedFixes = 0;
                return stationary(stationaryAnchor, segmentStart);
            }
            return new Result(true, false, stopCandidateStart.latitude(), stopCandidateStart.longitude(),
                    true, 0, segmentStart);
        }

        lowSpeedFixes = 0;
        stopCandidateStart = null;
        previous = fix;
        return moving(fix, observedSpeed, segmentStart);
    }

    public synchronized void reset() {
        resetState();
    }

    private void resetState() {
        state = State.UNKNOWN;
        previous = null;
        stationaryAnchor = null;
        stopCandidateStart = null;
        lowSpeedFixes = 0;
        startFixes = 0;
        jumpRejections = 0;
        relocationCandidate = null;
        lastSeenNanos = 0;
    }

    private static boolean consistent(Fix first, Fix next) {
        long gap = next.elapsedRealtimeNanos() - first.elapsedRealtimeNanos();
        double allowance = Math.max(BASE_JUMP_ALLOWANCE_METERS,
                Math.min(MAX_ACCURACY_ALLOWANCE_METERS, first.accuracyMeters() + next.accuracyMeters()));
        return gap > 0 && gap <= MAX_FIX_GAP_NANOS
                && distanceMeters(first, next) <= allowance + MAX_SPEED_MPS * gap / 1_000_000_000.0;
    }

    private static boolean valid(Fix fix) {
        return fix != null && Double.isFinite(fix.latitude()) && Math.abs(fix.latitude()) <= 90
                && Double.isFinite(fix.longitude()) && Math.abs(fix.longitude()) <= 180
                && fix.hasAccuracy() && Double.isFinite(fix.accuracyMeters())
                && fix.accuracyMeters() >= 0
                && (!fix.hasSpeed() || (Double.isFinite(fix.speedMetersPerSecond())
                && fix.speedMetersPerSecond() >= 0 && fix.speedMetersPerSecond() <= MAX_SPEED_MPS))
                && fix.elapsedRealtimeNanos() > 0;
    }

    private static Result stationary(Fix anchor, boolean segmentStart) {
        return new Result(true, true, anchor.latitude(), anchor.longitude(), true, 0, segmentStart);
    }

    private static Result moving(Fix fix, double speed, boolean segmentStart) {
        if (!Double.isFinite(speed)) {
            return new Result(true, false, fix.latitude(), fix.longitude(),
                    false, Double.NaN, segmentStart);
        }
        double displaySpeed = speed <= MIN_DISPLAY_SPEED_MPS ? 0 : speed;
        return new Result(true, false, fix.latitude(), fix.longitude(),
                true, displaySpeed, segmentStart);
    }

    private static double distanceMeters(Fix a, Fix b) {
        if (a == null || b == null) return 0;
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
        return 6_371_000 * 2 * Math.asin(Math.sqrt(Math.min(1, h)));
    }
}
