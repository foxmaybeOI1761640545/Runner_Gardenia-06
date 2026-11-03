package de.dennisguse.opentracks.sensors;

import java.time.Instant;
import de.dennisguse.opentracks.sensors.driver.BleConnectionSession;

/** Immutable UI snapshot. No device timezone is changed by production code. */
public record HeartRateStatus(BleConnectionSession.State state, Instant lastReceived,
                              Instant observedAt, String device, boolean contactLost) {
    public static HeartRateStatus disconnected() {
        return new HeartRateStatus(BleConnectionSession.State.DISCONNECTED,null,null,null,false);
    }
}
