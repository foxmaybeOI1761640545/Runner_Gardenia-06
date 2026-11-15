package de.dennisguse.opentracks.p0.recording;

import android.content.Context;
import android.util.Log;

import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.util.PermissionRequester;

/**
 * Owns the app-foreground idle GPS preview. Recording ownership remains in TrackRecordingService;
 * releasing this owner never ends or pauses an active workout.
 */
public final class ForegroundLocationPreview {
    private static final String TAG = "ForegroundLocationPreview";
    private final Context context;
    private final Object owner = new Object();
    private final TrackRecordingServiceConnection connection;
    private boolean foreground;
    private boolean bound;
    private boolean acquired;
    private TrackRecordingService service;

    public ForegroundLocationPreview(Context context) {
        this.context = context.getApplicationContext();
        connection = new TrackRecordingServiceConnection((connected, self) -> onConnected(connected, self));
    }

    public void setForeground(boolean value) {
        if (foreground == value) {
            if (value) ensureAcquired();
            return;
        }
        foreground = value;
        if (foreground) ensureAcquired();
        else release();
    }

    /** Called after permission prompts and when returning from Android Settings. */
    public void onLocationPermissionChanged() {
        if (!hasLocationPermission()) {
            release();
            return;
        }
        if (foreground) ensureAcquired();
    }

    private boolean hasLocationPermission() {
        return PermissionRequester.GPS.hasPermission(context);
    }

    private void ensureAcquired() {
        if (!foreground || !hasLocationPermission()) return;
        if (service != null) {
            acquire();
            return;
        }
        if (bound) return;
        bound = true;
        try {
            connection.bindWithStart(context);
        } catch (RuntimeException failure) {
            bound = false;
            Log.w(TAG, "Unable to bind location preview service", failure);
        }
    }

    private void onConnected(TrackRecordingService connected, TrackRecordingServiceConnection self) {
        service = connected;
        if (!foreground || !hasLocationPermission()) {
            service = null;
            acquired = false;
            bound = false;
            self.unbind(context);
            return;
        }
        acquire();
    }

    private void acquire() {
        if (service == null || acquired || !foreground || !hasLocationPermission()) return;
        try {
            service.acquireMapGps(owner);
            acquired = true;
        } catch (RuntimeException failure) {
            Log.w(TAG, "Unable to acquire foreground location preview", failure);
        }
    }

    private void release() {
        if (service != null && acquired) {
            try {
                service.releaseMapGps(owner);
            } catch (RuntimeException failure) {
                Log.w(TAG, "Unable to release foreground location preview", failure);
            }
        }
        acquired = false;
        service = null;
        if (bound) {
            connection.unbind(context);
            bound = false;
        }
    }
}
