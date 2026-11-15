package de.dennisguse.opentracks.p0.gps;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.util.PermissionRequester;

/** One application-wide idle request; the existing recording service owns all GPS IO. */
public final class ForegroundGpsController implements Application.ActivityLifecycleCallbacks {
    private final Application application;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Set<Activity> started = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean binding;
    private final Runnable configurationTimeout = this::reconcile;
    private final TrackRecordingServiceConnection connection;

    public ForegroundGpsController(Application application) {
        this.application = application;
        connection = new TrackRecordingServiceConnection((service, self) -> {
            // A bind may finish after the last Activity stopped or permission changed.
            if (!wanted()) { release(); return; }
            try { service.setForegroundGpsPreview(true); }
            catch (RuntimeException failure) {
                Log.w("ForegroundGps", "Location preview unavailable", failure);
                release();
            }
        });
        application.registerActivityLifecycleCallbacks(this);
    }

    private boolean wanted() {
        return !started.isEmpty() && PermissionRequester.GPS.hasPermission(application);
    }

    private void reconcile() {
        if (!wanted()) { release(); return; }
        if (connection.getTrackRecordingService() != null) {
            try { connection.getTrackRecordingService().setForegroundGpsPreview(true); }
            catch (RuntimeException failure) {
                Log.w("ForegroundGps", "Location preview unavailable", failure);
                release();
            }
        } else if (!binding) {
            binding = true;
            try { connection.bindWithStart(application); }
            catch (RuntimeException failure) {
                Log.w("ForegroundGps", "Recording service unavailable", failure);
                release();
            }
        }
    }

    private void release() {
        var service = connection.getTrackRecordingService();
        try { if (service != null) service.setForegroundGpsPreview(false); }
        catch (RuntimeException failure) { Log.w("ForegroundGps", "Location preview release failed", failure); }
        if (binding) connection.unbind(application);
        binding = false;
    }

    @Override public void onActivityStarted(Activity activity) {
        main.removeCallbacks(configurationTimeout);
        started.add(activity);
        reconcile();
    }
    @Override public void onActivityResumed(Activity activity) { reconcile(); }
    @Override public void onActivityStopped(Activity activity) {
        started.remove(activity);
        if (started.isEmpty() && activity.isChangingConfigurations()) {
            // Keep the same driver across recreation, but never retain an orphaned request.
            main.postDelayed(configurationTimeout, 500);
        } else reconcile();
    }
    @Override public void onActivityCreated(Activity activity, Bundle state) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}
