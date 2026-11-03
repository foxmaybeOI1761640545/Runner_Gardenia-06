package de.dennisguse.opentracks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

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
