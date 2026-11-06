package de.dennisguse.opentracks.p0.gps;

/** Service-owned preparation survives screen recreation; no track ID exists at this stage. */
public record GpsPreparation(State state, long startedElapsedNanos) {
    public enum State { IDLE, ACQUIRING, TIMED_OUT, UNAVAILABLE }
    public static GpsPreparation idle() { return new GpsPreparation(State.IDLE, 0); }
    public boolean active() { return state == State.ACQUIRING || state == State.TIMED_OUT; }
}
