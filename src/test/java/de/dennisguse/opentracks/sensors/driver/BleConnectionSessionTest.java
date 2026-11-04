package de.dennisguse.opentracks.sensors.driver;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.LinkedHashMap;
import java.util.Map;

public class BleConnectionSessionTest {
    private final Map<Runnable,Long> tasks=new LinkedHashMap<>();
    private int opens,closes,ready,lost;
    private boolean canOpen=true,canDiscover=true,canSubscribe=true;
    private final BleConnectionSession session=new BleConnectionSession(new BleConnectionSession.Transport() {
        public boolean open(int token) { opens++;return canOpen; }
        public boolean discover() { return canDiscover; }
        public boolean subscribe() { return canSubscribe; }
        public void close() { closes++; }
    },new BleConnectionSession.Scheduler() {
        public void schedule(Runnable task,long delay) { tasks.put(task,delay); }
        public void cancel(Runnable task) { tasks.remove(task); }
    },new BleConnectionSession.Listener() {
        public void stateChanged(BleConnectionSession.State state) {}
        public void ready() { ready++; }
        public void lost() { lost++; }
        public void stopped() {}
    });
    private void establish() { session.start();int token=session.generation();session.connected(token,true);session.servicesDiscovered(token,true);session.subscribed(token,true); }
    private Runnable firstTask() { return tasks.keySet().iterator().next(); }
    private long runFirst() { Runnable task=firstTask();long delay=tasks.remove(task);task.run();return delay; }
    @Test public void subscriptionAcknowledgementIsRequired() {
        session.start();int token=session.generation();session.connected(token,true);session.servicesDiscovered(token,true);
        assertEquals(0,ready);assertEquals(BleConnectionSession.State.SUBSCRIBING,session.state());
        session.subscribed(token,true);assertEquals(1,ready);assertTrue(tasks.isEmpty());
    }
    @Test public void duplicateStartDoesNotOpenTwice() { session.start();session.start();assertEquals(1,opens); }
    @Test public void retriesAreBoundedAndDelayed() {
        canOpen=false;session.start();
        assertEquals(1000,runFirst());assertEquals(3000,runFirst());assertEquals(5000,runFirst());
        assertEquals(10000,runFirst());assertEquals(30000,runFirst());
        assertEquals(60000,runFirst());assertEquals(60000,runFirst());
    }
    @Test public void manualStopCancelsEvenAlreadyQueuedRetry() {
        canOpen=false;session.start();Runnable stale=firstTask();session.stop();stale.run();
        assertTrue(tasks.isEmpty());assertEquals(1,opens);assertEquals(BleConnectionSession.State.DISCONNECTED,session.state());
    }
    @Test public void staleTimeoutAfterReadyIsIgnored() {
        session.start();Runnable timeout=firstTask();int token=session.generation();session.connected(token,true);session.servicesDiscovered(token,true);session.subscribed(token,true);timeout.run();
        assertEquals(BleConnectionSession.State.CONNECTED,session.state());assertEquals(0,lost);
    }
    @Test public void handshakeTimeoutClosesTransport() { session.start();assertEquals(30000,runFirst());assertEquals(1,closes);assertEquals(BleConnectionSession.State.RECONNECT_WAIT,session.state()); }
    @Test public void failedDiscoveryRetries() { session.start();session.connected(session.generation(),true);session.servicesDiscovered(session.generation(),false);assertEquals(BleConnectionSession.State.RECONNECT_WAIT,session.state()); }
    @Test public void descriptorFailureNeverReportsReady() { session.start();int t=session.generation();session.connected(t,true);session.servicesDiscovered(t,true);session.subscribed(t,false);assertEquals(0,ready);assertEquals(BleConnectionSession.State.RECONNECT_WAIT,session.state()); }
    @Test public void rejectedFrameworkOperationsRetry() { canDiscover=false;session.start();session.connected(session.generation(),true);assertEquals(BleConnectionSession.State.RECONNECT_WAIT,session.state()); }
    @Test public void staleCallbacksCannotCompleteNewConnection() { establish();int old=session.generation();session.disconnected(old);runFirst();session.subscribed(old,true);assertEquals(1,ready);assertEquals(BleConnectionSession.State.CONNECTING,session.state()); }
    @Test public void bluetoothPowerCycleWaitsForPowerOn() { establish();session.bluetoothOff();assertTrue(tasks.isEmpty());assertEquals(BleConnectionSession.State.BLUETOOTH_OFF,session.state());session.bluetoothOn();assertEquals(2,opens); }
    @Test public void permissionDeniedDoesNotSpin() { establish();session.permissionDenied();assertEquals(BleConnectionSession.State.PERMISSION_DENIED,session.state());assertTrue(tasks.isEmpty()); }
    @Test public void invalidDeviceDoesNotSpin() { session.start();session.unavailable();assertEquals(BleConnectionSession.State.UNAVAILABLE,session.state());assertTrue(tasks.isEmpty()); }
    @Test public void disconnectDoesNotStopDesiredSession() { establish();session.disconnected(session.generation());assertEquals(BleConnectionSession.State.RECONNECT_WAIT,session.state());assertEquals(1,ready);assertEquals(1,lost);assertFalse(tasks.isEmpty()); }
    @Test public void onlyExplicitReconnectRestartsAfterManualDisconnect() {
        establish();session.stop();session.bluetoothOn();assertEquals(1,opens);assertTrue(tasks.isEmpty());
        session.reconnect();assertEquals(2,opens);assertEquals(BleConnectionSession.State.CONNECTING,session.state());
    }
    @Test public void rejectedSubscriptionRetries() { canSubscribe=false;session.start();int t=session.generation();session.connected(t,true);session.servicesDiscovered(t,true);assertEquals(BleConnectionSession.State.RECONNECT_WAIT,session.state()); }
    @Test public void disabledAutoReconnectStillAllowsOneExplicitConnection() {
        session.setAutoReconnectEnabled(false);establish();assertEquals(1,ready);
        session.disconnected(session.generation());assertEquals(BleConnectionSession.State.DISCONNECTED,session.state());
        assertTrue(tasks.isEmpty());assertEquals(1,opens);
    }
    @Test public void disablingPolicyCancelsEvenADequeuedRetry() {
        canOpen=false;session.start();Runnable stale=firstTask();session.setAutoReconnectEnabled(false);stale.run();
        assertTrue(tasks.isEmpty());assertEquals(1,opens);assertEquals(BleConnectionSession.State.DISCONNECTED,session.state());
        session.setAutoReconnectEnabled(true);assertEquals(2,opens);assertFalse(tasks.isEmpty());
    }
    @Test public void disabledPolicyWaitsForExplicitConnectionAfterBluetoothPowerCycle() {
        session.setAutoReconnectEnabled(false);establish();session.bluetoothOff();session.bluetoothOn();
        assertEquals(1,opens);assertEquals(BleConnectionSession.State.DISCONNECTED,session.state());
        session.reconnect();assertEquals(2,opens);assertEquals(BleConnectionSession.State.CONNECTING,session.state());
    }
    @Test public void changingPolicyDoesNotCloseAHealthyConnection() {
        establish();session.setAutoReconnectEnabled(false);
        assertFalse(session.isAutoReconnectEnabled());assertEquals(0,closes);assertEquals(BleConnectionSession.State.CONNECTED,session.state());
    }
    @Test public void enablingPolicyNeverOverridesManualStop() {
        establish();session.stop();session.setAutoReconnectEnabled(false);session.setAutoReconnectEnabled(true);session.bluetoothOn();
        assertEquals(1,opens);assertTrue(tasks.isEmpty());assertEquals(BleConnectionSession.State.DISCONNECTED,session.state());
    }
    @Test public void disabledPolicyClosesTimedOutHandshakeWithoutRetry() {
        session.setAutoReconnectEnabled(false);session.start();assertEquals(30000,runFirst());
        assertEquals(1,closes);assertEquals(1,lost);assertTrue(tasks.isEmpty());assertEquals(BleConnectionSession.State.DISCONNECTED,session.state());
    }
    @Test public void duplicateDisconnectAfterDisabledRetryIsIgnored() {
        session.setAutoReconnectEnabled(false);establish();int old=session.generation();
        session.disconnected(old);session.disconnected(old);assertEquals(1,closes);assertEquals(1,lost);assertTrue(tasks.isEmpty());
    }
}
