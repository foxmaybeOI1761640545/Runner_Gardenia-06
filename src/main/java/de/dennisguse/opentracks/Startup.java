package de.dennisguse.opentracks;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import java.lang.reflect.Method;

import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.p0.recording.ForegroundActivityCounter;
import de.dennisguse.opentracks.p0.recording.ForegroundLocationPreview;
import de.dennisguse.opentracks.util.ExceptionHandler;

/**
 * Code that is executed when the application starts.
 * <p>
 * NOTE: How often actual application startup happens depends on the OS.
 * Not every start of an activity will trigger this.
 */
public class Startup extends Application {

    private static final String TAG = Startup.class.getSimpleName();
    private final ForegroundActivityCounter foregroundActivities = new ForegroundActivityCounter();
    private ForegroundLocationPreview locationPreview;

    @Override
    public void onCreate() {
        super.onCreate();

        // Include version information into stack traces.
        Log.i(TAG, BuildConfig.APPLICATION_ID + "; BuildType: " + BuildConfig.BUILD_TYPE + "; VersionName: " + BuildConfig.VERSION_NAME + "/" + BuildConfig.VERSION_NAME_FULL + " VersionCode: " + BuildConfig.VERSION_CODE);
        // In debug builds: show thread and VM warnings.
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Enabling strict mode");
            StrictMode.enableDefaults();
        }

        PreferencesUtils.initPreferences(this);
        // Set default values of preferences on first start.
        PreferencesUtils.resetPreferences(this, false);
        PreferencesUtils.applyDefaultUnit();
        PreferencesUtils.applyNightModeAndDynamicColors(this);

        locationPreview = new ForegroundLocationPreview(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, android.os.Bundle state) {}
            @Override public void onActivityStarted(Activity activity) {
                if (foregroundActivities.onStarted()) locationPreview.setForeground(true);
            }
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {
                if (foregroundActivities.onStopped()) locationPreview.setForeground(false);
            }
            @Override public void onActivitySaveInstanceState(Activity activity, android.os.Bundle state) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    /** Re-evaluate the foreground GPS owner after a runtime permission change. */
    public void onLocationPermissionChanged() {
        if (locationPreview != null) locationPreview.onLocationPermissionChanged();
    }

    @Override
    protected void attachBaseContext(final Context base) {
        super.attachBaseContext(base);

        // handle crashes only outside the crash reporter activity/process
        if (BuildConfig.DEBUG && !isCrashReportingProcess()) {
            Thread.UncaughtExceptionHandler defaultPlatformHandler = Thread.getDefaultUncaughtExceptionHandler();
            ExceptionHandler crashReporter = new ExceptionHandler(this, defaultPlatformHandler);
            Thread.setDefaultUncaughtExceptionHandler(crashReporter);
        }
    }

    private boolean isCrashReportingProcess() {
        String processName = "";
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // Using the same technique as Application.getProcessName() for older devices
            // Using reflection since ActivityThread is an internal API
            try {
                @SuppressLint("PrivateApi")
                Class<?> activityThread = Class.forName("android.app.ActivityThread");
                @SuppressLint("DiscouragedPrivateApi") Method getProcessName = activityThread.getDeclaredMethod("currentProcessName");
                processName = (String) getProcessName.invoke(null);
            } catch (Exception ignored) {
            }
        } else {
            processName = Application.getProcessName();
        }
        return processName != null && processName.endsWith(":crash");
    }

    /**
     * Returns the name of the database used by SQLiteOpenHelper.
     * See {@link android.database.sqlite.SQLiteOpenHelper} for details.
     * @return SQLite database name.
     */
    public String getDatabaseName() {
        return "database.db";
    }
}
