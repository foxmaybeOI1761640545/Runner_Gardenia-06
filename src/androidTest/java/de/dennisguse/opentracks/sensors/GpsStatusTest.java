package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static de.dennisguse.opentracks.sensors.GpsStatusValue.GPS_DISABLED;
import static de.dennisguse.opentracks.sensors.GpsStatusValue.GPS_ENABLED;
import static de.dennisguse.opentracks.sensors.GpsStatusValue.GPS_NONE;
import static de.dennisguse.opentracks.sensors.GpsStatusValue.GPS_SIGNAL_BAD;
import static de.dennisguse.opentracks.sensors.GpsStatusValue.GPS_SIGNAL_FIX;
import static de.dennisguse.opentracks.sensors.GpsStatusValue.GPS_SIGNAL_LOST;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Position;

@RunWith(AndroidJUnit4.class)
public class GpsStatusTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    private HandlerThread timerThread;
    private Handler timerHandler;

    @Before
    public void prepareTimer() {
        timerThread = new HandlerThread("GpsStatusTest timer");
        timerThread.start();
        timerHandler = new Handler(timerThread.getLooper());
    }

    @After
    public void stopTimer() throws InterruptedException {
        timerHandler.removeCallbacksAndMessages(null);
        timerThread.quitSafely();
        timerThread.join(1000);
        assertFalse("GPS test timer must be released", timerThread.isAlive());
    }

    private final static Location badFix = new Location("gps");
    private final static Location ok = new Location("gps");

    static {

        badFix.setAccuracy(50);

        ok.setAccuracy(10);
    }

    @Test
    public void testStartDisabledEnabledStop() {
        List<GpsStatusValue> statusList = new CopyOnWriteArrayList<>();

        // given
        GpsStatusManager subject = new GpsStatusManager(context, statusList::add, timerHandler);

        // when / then
        subject.start();
        assertEquals(List.of(GPS_ENABLED), statusList);

        subject.onGpsDisabled();
        assertEquals(List.of(GPS_ENABLED, GPS_DISABLED), statusList);

        subject.onGpsEnabled();
        assertEquals(List.of(GPS_ENABLED, GPS_DISABLED, GPS_ENABLED), statusList);

        subject.stop();
        assertEquals(List.of(GPS_ENABLED, GPS_DISABLED, GPS_ENABLED, GPS_NONE), statusList);
    }

    @Test
    public void testStartBadfixOk() {
        List<GpsStatusValue> statusList = new CopyOnWriteArrayList<>();

        // given
        GpsStatusManager subject = new GpsStatusManager(context, statusList::add, timerHandler);
        subject.onRecordingDistanceChanged(Distance.of(10));

        // when / then
        subject.start();
        subject.onNewTrackPoint(Position.of(badFix, Instant.now()));
        assertEquals(List.of(GPS_ENABLED, GPS_SIGNAL_BAD), statusList);

        subject.onNewTrackPoint(Position.of(ok, Instant.now()));
        assertEquals(List.of(GPS_ENABLED, GPS_SIGNAL_BAD, GPS_SIGNAL_FIX), statusList);

        subject.onNewTrackPoint(Position.of(ok, Instant.now()));
        assertEquals(List.of(GPS_ENABLED, GPS_SIGNAL_BAD, GPS_SIGNAL_FIX), statusList);

        subject.onNewTrackPoint(Position.of(badFix, Instant.now()));
        assertEquals(List.of(GPS_ENABLED, GPS_SIGNAL_BAD, GPS_SIGNAL_FIX, GPS_SIGNAL_BAD), statusList);
    }

    @Test
    public void testStartSignalLost() {
        List<GpsStatusValue> statusList = new CopyOnWriteArrayList<>();

        // given
        GpsStatusManager subject = new GpsStatusManager(context, statusList::add, timerHandler);
        subject.onRecordingDistanceChanged(Distance.of(10));
        // This case advances time explicitly; keep the live timer from racing the assertions.
        subject.onMinSamplingIntervalChanged(GpsStatusManager.SIGNAL_LOST_THRESHOLD.negated().plusSeconds(10));

        // when / then
        subject.start();
        subject.onNewTrackPoint(Position.of(ok, Instant.now().minusSeconds(20)));
        assertEquals(List.of(GPS_ENABLED, GPS_SIGNAL_FIX), statusList);

        subject.determineGpsStatusByTime(Instant.now());
        assertEquals(List.of(GPS_ENABLED, GPS_SIGNAL_FIX, GPS_SIGNAL_LOST), statusList);

        subject.onNewTrackPoint(Position.of(ok, Instant.now()));
        assertEquals(List.of(GPS_ENABLED, GPS_SIGNAL_FIX, GPS_SIGNAL_LOST, GPS_SIGNAL_FIX), statusList);
    }

    @Test
    public void testStartSignalLostByTimer() throws InterruptedException {
        BlockingQueue<GpsStatusValue> events = new LinkedBlockingQueue<>();

        // given
        GpsStatusManager subject = new GpsStatusManager(context, events::add, timerHandler);
        subject.onRecordingDistanceChanged(Distance.of(10));
        subject.onMinSamplingIntervalChanged(GpsStatusManager.SIGNAL_LOST_THRESHOLD.multipliedBy(-1).plus(Duration.ofMillis(10)));

        // when / then
        subject.start();
        assertEquals(GPS_ENABLED, events.poll(2, TimeUnit.SECONDS));
        assertNull(events.poll(100, TimeUnit.MILLISECONDS));

        subject.onNewTrackPoint(Position.of(ok, Instant.now()));
        assertEquals(GPS_SIGNAL_FIX, events.poll(2, TimeUnit.SECONDS));
        assertEquals(GPS_SIGNAL_LOST, events.poll(2, TimeUnit.SECONDS));

        subject.onNewTrackPoint(Position.of(badFix, Instant.now()));
        assertEquals(GPS_SIGNAL_BAD, events.poll(2, TimeUnit.SECONDS));
        assertEquals(GPS_SIGNAL_LOST, events.poll(2, TimeUnit.SECONDS));
        assertTrue(events.isEmpty());
    }
}
