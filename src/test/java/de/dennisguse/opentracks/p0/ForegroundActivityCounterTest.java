package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import org.junit.Test;
import de.dennisguse.opentracks.p0.recording.ForegroundActivityCounter;

public class ForegroundActivityCounterTest {
    @Test public void onlyFirstStartAndLastStopChangeForegroundState() {
        ForegroundActivityCounter counter = new ForegroundActivityCounter();
        assertTrue(counter.onStarted());
        assertFalse(counter.onStarted());
        assertEquals(2, counter.startedCount());
        assertFalse(counter.onStopped());
        assertTrue(counter.onStopped());
        assertEquals(0, counter.startedCount());
    }

    @Test public void duplicateStopCannotMakeCounterNegative() {
        ForegroundActivityCounter counter = new ForegroundActivityCounter();
        assertFalse(counter.onStopped());
        assertEquals(0, counter.startedCount());
        assertTrue(counter.onStarted());
        assertTrue(counter.onStopped());
        assertFalse(counter.onStopped());
        assertEquals(0, counter.startedCount());
    }
}
