package de.dennisguse.opentracks.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.app.P0Activity;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.DistanceFormatter;
import de.dennisguse.opentracks.data.models.SpeedFormatter;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.util.IntentUtils;

/**
 * Manages the content of the notification shown by {@link TrackRecordingService}.
 */
class TrackRecordingServiceNotificationManager implements SharedPreferences.OnSharedPreferenceChangeListener {

    static final int NOTIFICATION_ID = 123;

    // Separate from the upstream high-importance channel: an upgrade must not inherit
    // a heads-up overlay that intercepts P0 recording controls. Users retain channel settings.
    static final String CHANNEL_ID = "runner_gardenia_p0_recording";

    private final NotificationCompat.Builder notificationBuilder;

    private final NotificationManager notificationManager;

    private boolean previousLocationWasAccurate = true;

    private UnitSystem unitSystem = null;

    TrackRecordingServiceNotificationManager(Context context) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.p0_title), NotificationManager.IMPORTANCE_LOW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            notificationChannel.setAllowBubbles(false);
        }

        notificationManager.createNotificationChannel(notificationChannel);

        notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
        notificationBuilder
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle(context.getString(R.string.p0_title))
                .setSmallIcon(R.drawable.ic_logo_color_24dp);
    }

    @VisibleForTesting
    TrackRecordingServiceNotificationManager(NotificationManager notificationManager, NotificationCompat.Builder notificationBuilder) {
        this.notificationManager = notificationManager;
        this.notificationBuilder = notificationBuilder;
    }

    Notification onSensorsStarted(Context context) {
        return onSensorsStarted(context,true);
    }

    Notification onSensorsStarted(Context context,boolean recording) {
        Intent intent = IntentUtils.newIntent(context, P0Activity.class);

        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addParentStack(P0Activity.class)
                .addNextIntent(intent)
                .getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        updateGPSStatus(context.getString(recording?R.string.gps_starting:R.string.p0_hr_preview_notification));

        notificationBuilder.setContentIntent(pendingIntent);
        updateNotification();

        return getNotification();
    }

    void updateGPSStatus(String content) {
        notificationBuilder.setSubText(content);
        updateNotification();
    }

    public void onRecordingStarted(Context context, @NonNull Track.Id trackId) {
        Intent intent = IntentUtils.newIntent(context, P0Activity.class);

        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addParentStack(P0Activity.class)
                .addNextIntent(intent)
                .getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(pendingIntent);
        updateNotification();
    }

    void updateTrackPoint(Context context, RecordingData recordingData, Distance thresholdHorizontalAccuracy) {
        String formattedAccuracy = context.getString(R.string.value_none);

        DistanceFormatter formatter = DistanceFormatter.Builder()
                .setUnit(unitSystem)
                .build(context);

        //TODO The decision if a trackpoint is not accurate enough should happen in the TrackRecordingManager (via RecordingData).
        if (recordingData.latestTrackPoint().position().hasHorizontalAccuracy()) {
            formattedAccuracy = formatter.formatDistance(recordingData.latestTrackPoint().position().horizontalAccuracy());

            boolean currentLocationWasAccurate = recordingData.latestTrackPoint().position().horizontalAccuracy().lessThan(thresholdHorizontalAccuracy);
            boolean shouldAlert = !currentLocationWasAccurate && previousLocationWasAccurate;
            notificationBuilder.setOnlyAlertOnce(!shouldAlert);
            previousLocationWasAccurate = currentLocationWasAccurate;
        }

        notificationBuilder.setContentTitle(context.getString(R.string.track_distance_notification, formatter.formatDistance(recordingData.trackStatistics().totalDistance())));
        String formattedSpeed = SpeedFormatter.Builder().setUnit(unitSystem).setReportSpeedOrPace(true).build(context).formatSpeed(recordingData.latestTrackPoint().position().speed());
        notificationBuilder.setContentText(context.getString(R.string.track_speed_notification, formattedSpeed));
        notificationBuilder.setSubText(context.getString(R.string.track_recording_notification_accuracy, formattedAccuracy));
        updateNotification();

        notificationBuilder.setOnlyAlertOnce(true);
    }

    void stop() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, getNotification());
    }

    private Notification getNotification() {
        return notificationBuilder.build();
    }

    @VisibleForTesting
    void setUnitSystem(UnitSystem unitSystem) {
        this.unitSystem = unitSystem;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PreferencesUtils.isKey(R.string.stats_units_key, key)) {
            setUnitSystem(PreferencesUtils.getUnitSystem());
        }
    }
}
