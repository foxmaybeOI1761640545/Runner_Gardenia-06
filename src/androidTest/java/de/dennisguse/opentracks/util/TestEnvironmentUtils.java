package de.dennisguse.opentracks.util;

import android.content.Context;
import android.content.Intent;
import android.app.ActivityManager;
import android.os.SystemClock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;

import androidx.test.core.app.ApplicationProvider;

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.services.TrackRecordingService;

public final class TestEnvironmentUtils {

    private TestEnvironmentUtils() {
    }

    public static void stopTrackRecordingService() {
        Context context = ApplicationProvider.getApplicationContext();
        context.stopService(new Intent(context, TrackRecordingService.class));
    }

    public static void deleteAllTracks() {
        Context context = ApplicationProvider.getApplicationContext();
        new ContentProviderUtils(context).deleteAllTracks(context);
    }

    /** stopService is asynchronous, and a bound service may outlive it. Quiesce before deleting rows. */
    public static void resetTrackRecordingServiceAndDeleteTracks() {
        Context context = ApplicationProvider.getApplicationContext();
        // A legacy Activity can have a non-auto-create binding waiting for a future service.
        // Never manufacture and immediately destroy a service just to clean an empty fixture.
        if (!hasRunningServiceInstance(context)) {
            stopTrackRecordingService();
            deleteAllTracks();
            return;
        }
        CountDownLatch quiescent = new CountDownLatch(1);
        TrackRecordingServiceConnection.execute(context, (service, connection) -> {
            try {
                service.cancelGpsAcquisition();
                service.endCurrentTrack();
                service.stopIdleSensors();
            } finally { quiescent.countDown(); }
        });
        try {
            if (!quiescent.await(10, TimeUnit.SECONDS)) throw new AssertionError("Recording service did not quiesce before database cleanup");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while awaiting recording cleanup", interrupted);
        }
        stopTrackRecordingService();
        deleteAllTracks();
    }

    /** For recovery tests only: preserve the active-track journal and await an actual new service lifetime. */
    @SuppressWarnings("deprecation")
    public static void awaitTrackRecordingServiceDestroyed() {
        Context context = ApplicationProvider.getApplicationContext();
        long deadline = SystemClock.uptimeMillis() + 10000;
        do {
            boolean running = hasRunningServiceInstance(context);
            if (!running) return;
            SystemClock.sleep(25);
        } while (SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Recording service still bound/running after stop; recovery needs an actual destroyed instance");
    }
    @SuppressWarnings("deprecation")
    private static boolean hasRunningServiceInstance(Context context) {
        ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        return manager.getRunningServices(Integer.MAX_VALUE).stream().anyMatch(info -> info.pid>0
                && info.service.getClassName().equals(TrackRecordingService.class.getName()));
    }

}
