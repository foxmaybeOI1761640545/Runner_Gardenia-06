package de.dennisguse.opentracks.sensors.driver;

/** Generation guard shared by the scan dialog and fake-scanner tests. */
public final class BleScanSession {
    public static String displayName(String cached, String advertised) {
        if (cached != null && !cached.isBlank()) return cached;
        return advertised != null && !advertised.isBlank() ? advertised : null;
    }

    public enum State { IDLE, SCANNING, COMPLETE, CANCELLED, FAILED, BLUETOOTH_OFF, PERMISSION_DENIED }
    private int generation;
    private State state = State.IDLE;
    public synchronized int begin(boolean permission, boolean powered) {
        generation++;
        state = !permission ? State.PERMISSION_DENIED : !powered ? State.BLUETOOTH_OFF : State.SCANNING;
        return generation;
    }
    public synchronized boolean accepts(int token) { return token == generation && state == State.SCANNING; }
    public synchronized boolean complete(int token) {
        if (!accepts(token)) return false;
        state = State.COMPLETE;
        return true;
    }
    public synchronized boolean fail(int token) {
        if (!accepts(token)) return false;
        state = State.FAILED;
        return true;
    }
    public synchronized void cancel() { generation++; state = State.CANCELLED; }
    public synchronized State state() { return state; }
}
