package de.dennisguse.opentracks.sensors.driver;

/** Transport-independent lifecycle used by the real GATT driver and deterministic tests. */
public final class BleConnectionSession {
    public enum State { DISCONNECTED, CONNECTING, DISCOVERING, SUBSCRIBING, CONNECTED,
        RECONNECT_WAIT, BLUETOOTH_OFF, PERMISSION_DENIED, UNAVAILABLE }

    public interface Transport {
        boolean open(int generation);
        boolean discover();
        boolean subscribe();
        void close();
    }
    public interface Scheduler {
        void schedule(Runnable task, long delayMillis);
        void cancel(Runnable task);
    }
    public interface Listener {
        void stateChanged(State state);
        void ready();
        void lost();
        void stopped();
    }

    private static final long[] RETRY_MILLIS = {1000, 3000, 5000, 10000};
    private final Transport transport;
    private final Scheduler scheduler;
    private final Listener listener;
    private State state = State.DISCONNECTED;
    private boolean desired;
    private int generation;
    private int retries;
    private Runnable timeout;
    private Runnable retry;

    public BleConnectionSession(Transport transport, Scheduler scheduler, Listener listener) {
        this.transport = transport;
        this.scheduler = scheduler;
        this.listener = listener;
    }

    public synchronized void start() {
        if (desired) return;
        desired = true;
        retries = 0;
        attempt();
    }

    private void attempt() {
        if (!desired) return;
        generation++;
        change(State.CONNECTING);
        int token = generation;
        timeout = () -> timeout(token);
        scheduler.schedule(timeout, 30000);
        if (!transport.open(token)) failed(token);
    }

    public synchronized void connected(int token, boolean success) {
        if (!accept(token, State.CONNECTING)) return;
        if (!success) { failed(token); return; }
        change(State.DISCOVERING);
        if (!transport.discover()) failed(token);
    }

    public synchronized void servicesDiscovered(int token, boolean supported) {
        if (!accept(token, State.DISCOVERING)) return;
        if (!supported) { failed(token); return; }
        change(State.SUBSCRIBING);
        if (!transport.subscribe()) failed(token);
    }

    public synchronized void subscribed(int token, boolean success) {
        if (!accept(token, State.SUBSCRIBING)) return;
        if (!success) { failed(token); return; }
        cancelTimeout();
        retries = 0;
        change(State.CONNECTED);
        listener.ready();
    }

    public synchronized void disconnected(int token) {
        if (desired && token == generation && state != State.RECONNECT_WAIT
                && state != State.BLUETOOTH_OFF && state != State.PERMISSION_DENIED) failed(token);
    }

    private synchronized void timeout(int token) {
        if (desired && token == generation && (state == State.CONNECTING
                || state == State.DISCOVERING || state == State.SUBSCRIBING)) failed(token);
    }

    private void failed(int token) {
        if (!desired || token != generation) return;
        cancelTimeout();
        transport.close();
        listener.lost();
        change(State.RECONNECT_WAIT);
        long delay = RETRY_MILLIS[retries];
        retries = Math.min(retries + 1, RETRY_MILLIS.length - 1);
        retry = () -> retry(token);
        scheduler.schedule(retry, delay);
    }

    private synchronized void retry(int token) {
        if (desired && token == generation && state == State.RECONNECT_WAIT) {
            retry = null;
            attempt();
        }
    }

    public synchronized void bluetoothOff() {
        if (!desired) return;
        cancelTasks();
        generation++;
        transport.close();
        listener.lost();
        change(State.BLUETOOTH_OFF);
    }

    public synchronized void bluetoothOn() {
        if (desired && state == State.BLUETOOTH_OFF) attempt();
    }

    public synchronized void permissionDenied() {
        blocked(State.PERMISSION_DENIED);
    }

    public synchronized void unavailable() { blocked(State.UNAVAILABLE); }

    private void blocked(State reason) {
        if (!desired) return;
        cancelTasks();
        generation++;
        transport.close();
        listener.lost();
        change(reason);
    }

    public synchronized void reconnect() {
        if (!desired) { start(); return; }
        cancelTasks();
        transport.close();
        listener.lost();
        retries = 0;
        attempt();
    }

    public synchronized void stop() {
        if (!desired && state == State.DISCONNECTED) return;
        desired = false;
        generation++;
        cancelTasks();
        transport.close();
        change(State.DISCONNECTED);
        listener.stopped();
    }

    private void cancelTimeout() {
        if (timeout != null) scheduler.cancel(timeout);
        timeout = null;
    }
    private void cancelTasks() {
        cancelTimeout();
        if (retry != null) scheduler.cancel(retry);
        retry = null;
    }
    private boolean accept(int token, State expected) { return desired && token == generation && state == expected; }
    private void change(State next) { state = next; listener.stateChanged(next); }
    public synchronized State state() { return state; }
    public synchronized int generation() { return generation; }
}
