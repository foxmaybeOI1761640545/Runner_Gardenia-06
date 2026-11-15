package de.dennisguse.opentracks.p0.app;

import static org.junit.Assert.*;
import java.time.Instant;
import org.junit.Test;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.sensors.GpsStatusValue;

public class DashboardMapPositionTest {
    @Test public void freshIdleFixIsAvailableToTheMap() {
        Position position = new Position(Instant.EPOCH, 23.1, 113.2, null, null, null, null, null);
        assertSame(position, DashboardState.currentMapPosition(true, GpsStatusValue.GPS_SIGNAL_FIX, position));
    }

    @Test public void staleOrUnavailableFixIsNotExposed() {
        Position position = new Position(Instant.EPOCH, 23.1, 113.2, null, null, null, null, null);
        assertNull(DashboardState.currentMapPosition(false, GpsStatusValue.GPS_SIGNAL_FIX, position));
        assertNull(DashboardState.currentMapPosition(true, GpsStatusValue.GPS_NONE, position));
        assertNull(DashboardState.currentMapPosition(true, GpsStatusValue.GPS_SIGNAL_FIX, null));
    }

    @Test public void physicallyInvalidCoordinatesAreNotExposed() {
        Position invalid = new Position(Instant.EPOCH, 120.0, 300.0, null, null, null, null, null);
        assertNull(DashboardState.currentMapPosition(true, GpsStatusValue.GPS_SIGNAL_FIX, invalid));
    }
}
