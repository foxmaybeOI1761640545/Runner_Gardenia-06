package de.dennisguse.opentracks.sensors;

import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.gps.GpsMotionFilter;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.sensors.driver.Driver;
import de.dennisguse.opentracks.sensors.driver.GpsInternal;
import de.dennisguse.opentracks.sensors.driver.SensorType;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorGPS;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.settings.PreferencesUtils;

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class GpsHandler extends SensorHandler<Location, Position> {

    private final String TAG = GpsHandler.class.getSimpleName();

    //TODO Refactor to just pass information via SensorManager.
    private TrackPointCreator trackPointCreator;

    private GpsStatusManager gpsStatusManager;
    private Distance thresholdHorizontalAccuracy;
    private volatile long latestElapsedNanos;
    private final GpsMotionFilter motionFilter = new GpsMotionFilter();

    /** A manual recording boundary can occur while a visible map keeps GPS connected. */
    public void beginRecordingSegment() {
        motionFilter.reset();
        segmentAfterReconnect = false;
        recordingFixSeen = false;
    }
    private volatile Position latestRecordingPosition;
    private boolean recordingFixSeen;
    private boolean segmentAfterReconnect;

    private void resetConnectionMotion() {
        // Keep the discontinuity across reset; the first trusted new GPS point
        // must carry it even when BLE notifications arrive in the meantime.
        if (recordingFixSeen && trackPointCreator.isRecording()) segmentAfterReconnect = true;
        motionFilter.reset();
        latestRecordingPosition = null;
        latestElapsedNanos = 0;
    }

    public boolean hasFreshPreview() {
        long now = android.os.SystemClock.elapsedRealtimeNanos();
        return latestElapsedNanos > 0 && now >= latestElapsedNanos
                && now - latestElapsedNanos <= de.dennisguse.opentracks.p0.gps.GpsReadiness.DEFAULT.maxAgeNanos();
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (PreferencesUtils.isKey(R.string.recording_gps_accuracy_key, key)) {
            thresholdHorizontalAccuracy = PreferencesUtils.getThresholdHorizontalAccuracy();
            if(gpsStatusManager!=null)gpsStatusManager.onRequiredAccuracyChanged(thresholdHorizontalAccuracy);
        }

        if (PreferencesUtils.isKey(R.string.min_sampling_interval_key, key)) {
            if (gpsStatusManager != null) {
                gpsStatusManager.onMinSamplingIntervalChanged(PreferencesUtils.getMinSamplingInterval());
            }

            if (driver != null) {
                driver.updateDriverParameters(getContext(), sensorManager.getHandler());
            }
        }
    };

    public GpsHandler(SensorManager sensorManager, TrackPointCreator trackPointCreator) {
        super(sensorManager);
        this.trackPointCreator = trackPointCreator;
        gpsStatusManager = new GpsStatusManager(sensorManager.getContext(), trackPointCreator::sendGpsStatus, sensorManager.getHandler());

        sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);
    }

    /** Last actual GPS measurement, with its original timestamp/accuracy; never consumes recording data. */
    public Position latestPosition() {
        Position current = latestRecordingPosition;
        return current != null ? current : gpsStatusManager.latestPosition();
    }

    @Override
    public void onConnected(String sensorAddress, String sensorName) {
        resetConnectionMotion();
        super.onConnected(sensorAddress, sensorName);
        gpsStatusManager.onGpsEnabled();
        gpsStatusManager.start();
    }

    @Override
    public void onDisconnected() {
        resetConnectionMotion();
        super.onDisconnected();
        gpsStatusManager.stop();
    }

    @Override
    public void onSensorDeactivated() {
        resetConnectionMotion();
        super.onSensorDeactivated();
        gpsStatusManager.onGpsDisabled();
    }

    @Override
    public void onDataReceived(Location location) {
        if (!trackPointCreator.acceptGpsLocation(location, thresholdHorizontalAccuracy)) return;

        // Keep the status path honest: invalid/weak fixes remain visible as GPS warnings,
        // but they are never sent to the recorder or motion filter.
        Position rawPosition = Position.of(location, trackPointCreator.getNow());
        gpsStatusManager.onNewTrackPoint(rawPosition);
        if (!rawPosition.hasValidLocation()) {
            Log.w(TAG, "Ignore newTrackPoint. Location is invalid.");
            return;
        }
        if (!rawPosition.fulfillsAccuracy(thresholdHorizontalAccuracy)) {
            Log.d(TAG, "Ignore newTrackPoint. Poor accuracy.");
            return;
        }

        Location recordingLocation = location;
        // P0's acquisition guard already rejects live fixes without monotonic
        // metadata. Untimed legacy Locations have no such time axis: never
        // substitute callback arrival time and infer a bogus movement interval.
        if (trackPointCreator.isRecording() && location.getElapsedRealtimeNanos() > 0) {
            long elapsed = location.getElapsedRealtimeNanos();
            GpsMotionFilter.Result filtered = motionFilter.update(new GpsMotionFilter.Fix(
                    location.getLatitude(), location.getLongitude(),
                    location.hasAccuracy(), location.getAccuracy(),
                    location.hasSpeed(), location.hasSpeed() ? location.getSpeed() : Double.NaN,
                    elapsed));
            if (!filtered.accepted()) {
                Log.w(TAG, "Ignore GPS outlier; waiting for a stable fix.");
                return;
            }

            recordingLocation = new Location(location);
            recordingLocation.setLatitude(filtered.latitude());
            recordingLocation.setLongitude(filtered.longitude());
            if (filtered.hasSpeed()) recordingLocation.setSpeed((float) filtered.speedMetersPerSecond());
            if (filtered.segmentStart() || segmentAfterReconnect) trackPointCreator.requestAutomaticSegmentStart();
            segmentAfterReconnect = false;
            recordingFixSeen = true;
        }

        Position position = Position.of(recordingLocation, trackPointCreator.getNow());
        latestRecordingPosition = position;
        long elapsed = recordingLocation.getElapsedRealtimeNanos();
        latestElapsedNanos = elapsed > 0 ? elapsed : android.os.SystemClock.elapsedRealtimeNanos();
        gpsStatusManager.onNewTrackPoint(position);
        // The acquisition guard can publish BAD directly while the status
        // manager still remembers FIX. An accepted fresh measurement must
        // explicitly clear that warning, including after invalid metadata.
        trackPointCreator.sendGpsStatus(GpsStatusValue.GPS_SIGNAL_FIX);
        super.onDataReceived(recordingLocation);
    }

    @NonNull
    @Override
    protected Driver createDriverInternal() {
        return new GpsInternal(this);
    }

    @NonNull
    @Override
    protected AggregatorGPS createAggregator(String sensorAddress, String sensorName) {
        return new AggregatorGPS(sensorAddress, sensorName);
    }

    @Override
    protected int getSensorPreferenceKey() {
        return R.string.settings_sensor_gps_key;
    }

    @Override
    protected String getSensorPreferenceDefaultValue() {
        return SensorType.INTERNAL.getPreferenceValue();
    }
}
