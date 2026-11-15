package de.dennisguse.opentracks.p0.recording;

/** Counts started activities so an in-app screen transition is not mistaken for backgrounding. */
public final class ForegroundActivityCounter {
    private int started;

    /** @return true only for the transition from zero started activities to one. */
    public boolean onStarted() {
        started++;
        return started == 1;
    }

    /** @return true only when the last started activity leaves the foreground. */
    public boolean onStopped() {
        if (started == 0) return false;
        started--;
        return started == 0;
    }

    public int startedCount() {
        return started;
    }
}
