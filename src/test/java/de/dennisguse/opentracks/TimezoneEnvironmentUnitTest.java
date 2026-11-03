package de.dennisguse.opentracks;

import static org.junit.Assert.assertEquals;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.junit.Test;

/** Verifies the actual Gradle test-worker process, not just its launching shell. */
public class TimezoneEnvironmentUnitTest {
    @Test public void gradleTestWorkerActuallyUsesSingapore() {
        assertEquals("Asia/Singapore",TimeZone.getDefault().getID());
        assertEquals("Asia/Singapore",ZoneId.systemDefault().getId());
        assertEquals(ZoneOffset.ofHours(8),ZoneId.systemDefault().getRules().getOffset(Instant.parse("2020-02-02T02:02:02Z")));
    }
}
