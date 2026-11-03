package de.dennisguse.opentracks.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
public class TrackRecordingServicePreferenceLifecycleTest {
    @Rule
    public final ServiceTestRule serviceRule = ServiceTestRule.withTimeout(5, TimeUnit.SECONDS);

    private final Context context = ApplicationProvider.getApplicationContext();
    private final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

    private TrackRecordingService bind() throws TimeoutException {
        return ((TrackRecordingService.Binder) serviceRule.bindService(
                new Intent(context, TrackRecordingService.class))).getService();
    }

    @Test
    public void callbackAfterDestroyDoesNotAccessReleasedManagers() throws TimeoutException {
        TrackRecordingService service = bind();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            service.onDestroy();
            try {
                assertNull(service.getRecordingStatusObservable());
                // null dispatches all preferences while active, not just one manager's key.
                service.onSharedPreferenceChanged(preferences, null);
            } finally {
                // ServiceTestRule still owns the bound instance and will destroy it at cleanup.
                service.onCreate();
            }
            service.onSharedPreferenceChanged(preferences, null);
            assertFalse(service.isRecording());
        });
    }

    @Test
    public void queuedPreferenceSnapshotIsIgnoredAfterListenerUnregister() throws TimeoutException {
        TrackRecordingService service = bind();
        String key = "test_queued_preference_lifecycle";
        AtomicInteger observed = new AtomicInteger();
        AtomicBoolean committed = new AtomicBoolean();
        AtomicBoolean destroyed = new AtomicBoolean();
        SharedPreferences.OnSharedPreferenceChangeListener observer = (sharedPreferences, changedKey) -> {
            if (key.equals(changedKey)) observed.incrementAndGet();
        };
        preferences.registerOnSharedPreferenceChangeListener(observer);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                // Android takes a listener snapshot and queues notification on the main looper.
                // Hold that looper until the writer has queued the real callback, then destroy.
                Thread writer = new Thread(() -> committed.set(
                        preferences.edit().putLong(key, System.nanoTime()).commit()));
                writer.start();
                try {
                    writer.join(TimeUnit.SECONDS.toMillis(5));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
                assertFalse("Preference writer did not complete", writer.isAlive());
                assertTrue("Preference change was not committed", committed.get());
                assertEquals("Notification should still be queued", 0, observed.get());
                service.onDestroy();
                destroyed.set(true);
            });
            // The queued callback executes here, after onDestroy has cleared all managers.
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals("The captured listener snapshot must actually run", 1, observed.get());
            assertNull(service.getRecordingStatusObservable());
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                if (destroyed.get()) service.onCreate();
                preferences.unregisterOnSharedPreferenceChangeListener(observer);
                preferences.edit().remove(key).apply();
            });
        }
    }
}
