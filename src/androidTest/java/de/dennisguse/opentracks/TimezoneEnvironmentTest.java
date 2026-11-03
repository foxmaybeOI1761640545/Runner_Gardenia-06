package de.dennisguse.opentracks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;

@RunWith(AndroidJUnit4.class)
public class TimezoneEnvironmentTest {
    @Test
    public void instrumentationDefaultIsSingapore() {
        assertEquals("Asia/Singapore", TimeZone.getDefault().getID());
        assertEquals(ZoneOffset.ofHours(8), ZoneId.systemDefault().getRules()
                .getOffset(Instant.parse("2020-02-02T02:02:02Z")));
        assertEquals("2020-02-02T10:02:02+08:00", Instant.parse("2020-02-02T02:02:02Z")
                .atZone(ZoneId.systemDefault()).toOffsetDateTime().toString());
    }

    @Test
    public void emulatorSystemTimezoneIsSingapore() throws IOException {
        try (ParcelFileDescriptor.AutoCloseInputStream input = new ParcelFileDescriptor.AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().getUiAutomation()
                        .executeShellCommand("getprop persist.sys.timezone"))) {
            String systemTimezone = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
            assertEquals("Asia/Singapore", systemTimezone);
            assertEquals("Asia/Singapore", TimeZone.getDefault().getID());
            Log.i("TimezoneEnvironmentTest", "Device=" + systemTimezone
                    + "; process=" + TimeZone.getDefault().getID() + "; offset=UTC+08:00");
        }
    }

    @Test
    public void timezoneRuleRestoresPreviousDefaultOnFailure() throws Throwable {
        TimeZone original = TimeZone.getDefault();
        AssertionError expected = new AssertionError("fixture failure");
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            Statement statement = new TimezoneRule(TimeZone.getTimeZone("Asia/Singapore"))
                    .apply(new Statement() {
                        @Override
                        public void evaluate() {
                            assertEquals("Asia/Singapore", TimeZone.getDefault().getID());
                            throw expected;
                        }
                    }, Description.EMPTY);
            try {
                statement.evaluate();
                fail("Expected fixture failure");
            } catch (AssertionError actual) {
                if (actual != expected) throw actual;
            }
            assertEquals("UTC", TimeZone.getDefault().getID());
        } finally {
            TimeZone.setDefault(original);
        }
        assertEquals("Asia/Singapore", TimeZone.getDefault().getID());
    }
}
