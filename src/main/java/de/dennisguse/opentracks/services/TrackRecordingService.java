/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks.services;

import de.dennisguse.opentracks.R;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.ServiceCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import android.location.Location;
import android.os.SystemClock;
import de.dennisguse.opentracks.p0.gps.GpsReadiness;
import de.dennisguse.opentracks.p0.gps.GpsPreparation;

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.GpsStatusValue;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.settings.PreferencesUtils;

public class TrackRecordingService extends Service implements TrackPointCreator.Callback, SharedPreferences.OnSharedPreferenceChangeListener, TrackRecordingManager.IdleObserver {

    private static final String TAG = TrackRecordingService.class.getSimpleName();

    @VisibleForTesting
    public static final Duration RECORDING_DATA_UPDATE_INTERVAL = Duration.ofSeconds(1);

    public static final RecordingStatus STATUS_DEFAULT = RecordingStatus.notRecording();
    public static final GpsStatusValue STATUS_GPS_DEFAULT = GpsStatusValue.GPS_NONE;

    public TrackPoint getLastStoredTrackPointWithLocation() {
        return trackRecordingManager.getLastStoredTrackPointWithLocation();
    }

    public class Binder extends android.os.Binder {

        private Binder() {
            super();
        }

        public TrackRecordingService getService() {
            return TrackRecordingService.this;
        }
    }

    private final Binder binder = new Binder();

    private final Runnable updateRecordingData = new Runnable() {
        @Override
        public void run() {
            updateRecordingDataWhileRecording();

            TrackRecordingService.this.handler.postDelayed(this, RECORDING_DATA_UPDATE_INTERVAL.toMillis());
        }
    };

    // The following variables are set in onCreate:
    private RecordingStatus recordingStatus;
    private MutableLiveData<RecordingStatus> recordingStatusObservable;
    private MutableLiveData<GpsStatusValue> gpsStatusObservable;
    private MutableLiveData<RecordingData> recordingDataObservable;

    private Handler handler;
    private final MutableLiveData<GpsPreparation> gpsPreparationObservable = new MutableLiveData<>(GpsPreparation.idle());
    private GpsReadiness gpsReadiness;
    private boolean previewBeforePreparation;
    private boolean p0GpsGuard;
    private final java.util.Set<Object> mapGpsOwners=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /** Visible map lifetime only; never begins a workout, clock or associated HR samples. */
    public void acquireMapGps(Object owner) {
        if (!de.dennisguse.opentracks.util.PermissionRequester.GPS.hasPermission(this))
            throw new SecurityException("Fine location permission required");
        mapGpsOwners.add(owner);
        try { startSensors(); } catch (RuntimeException failure) { mapGpsOwners.remove(owner);throw failure; }
    }
    public void releaseMapGps(Object owner) {
        if (!mapGpsOwners.remove(owner)) return;
        if (isRecording() || isAcquiringGps() || !mapGpsOwners.isEmpty()) startSensors();
        else if (de.dennisguse.opentracks.p0.ble.HeartRateCommands.connectionEnabled(this)
                && de.dennisguse.opentracks.p0.recording.RecordingPermissions.hasBluetooth(this)) startSensors();
        else stopIdleSensors();
    }
    private final Runnable gpsPreparationTimeout = () -> {
        GpsPreparation current = gpsPreparationObservable.getValue();
        if (gpsReadiness != null && current != null && current.active())
            gpsPreparationObservable.setValue(new GpsPreparation(GpsPreparation.State.TIMED_OUT, current.startedElapsedNanos()));
    };

    public LiveData<GpsPreparation> getGpsPreparationObservable() { return gpsPreparationObservable; }
    public GpsPreparation getGpsPreparation() { return gpsPreparationObservable.getValue(); }
    public boolean isAcquiringGps() { return gpsReadiness != null; }
    public boolean hasFreshGpsPreview() {
        return areSensorsStarted() && trackPointCreator.getSensorManager().getGpsHandler().hasFreshPreview();
    }
    public void beginGpsAcquisition() {
        if (isRecording() || isAcquiringGps()) return;
        if (!de.dennisguse.opentracks.util.PermissionRequester.GPS.hasPermission(this))
            throw new SecurityException("Fine location permission required for GPS preparation");
        previewBeforePreparation = areSensorsStarted();
        long started = SystemClock.elapsedRealtimeNanos();
        gpsReadiness = new GpsReadiness(started);
        p0GpsGuard = true;
        gpsPreparationObservable.setValue(new GpsPreparation(GpsPreparation.State.ACQUIRING, started));
        try {
            startSensors();
            handler.postDelayed(gpsPreparationTimeout, GpsReadiness.DEFAULT.timeoutNanos() / 1_000_000);
        } catch (RuntimeException failure) {
            cancelGpsAcquisition();
            throw failure;
        }
    }
    public void continueGpsAcquisition() {
        if (!isAcquiringGps()) return;
        GpsPreparation current = getGpsPreparation();
        gpsPreparationObservable.setValue(new GpsPreparation(GpsPreparation.State.ACQUIRING, current.startedElapsedNanos()));
        handler.removeCallbacks(gpsPreparationTimeout);
        handler.postDelayed(gpsPreparationTimeout, GpsReadiness.DEFAULT.timeoutNanos() / 1_000_000);
    }
    public void cancelGpsAcquisition() {
        if (!isAcquiringGps()) return;
        gpsReadiness = null;
        p0GpsGuard = false;
        handler.removeCallbacks(gpsPreparationTimeout);
        gpsPreparationObservable.setValue(GpsPreparation.idle());
        if (previewBeforePreparation && de.dennisguse.opentracks.p0.recording.RecordingPermissions.hasBluetooth(this))
            startSensors(); // Turns GPS off while preserving an existing explicit HR preview.
        else stopIdleSensors();
    }
    @Override public boolean acceptGpsLocation(Location location, Distance requiredAccuracy) {
        if (!preferenceCallbacksEnabled) return false;
        GpsReadiness.Fix fix = new GpsReadiness.Fix(location.getLatitude(), location.getLongitude(),
                location.hasAccuracy() && location.getAccuracy() < requiredAccuracy.toM(),
                location.getAccuracy(), location.getElapsedRealtimeNanos());
        long now = SystemClock.elapsedRealtimeNanos();
        if (gpsReadiness != null) {
            if (!de.dennisguse.opentracks.util.PermissionRequester.GPS.hasPermission(this)) {
                cancelGpsAcquisition();
                gpsPreparationObservable.setValue(new GpsPreparation(GpsPreparation.State.UNAVAILABLE, 0));
                return false;
            }
            if (!gpsReadiness.add(fix, now)) return false;
            // No workout/clock/raw HR association exists before this exact transition.
            gpsReadiness = null;
            handler.removeCallbacks(gpsPreparationTimeout);
            try {
                if (startNewTrack() == null) throw new IllegalStateException("Unable to begin GPS workout");
            } catch (RuntimeException failure) {
                stopIdleSensors();
                gpsPreparationObservable.setValue(new GpsPreparation(GpsPreparation.State.UNAVAILABLE, 0));
                Log.e(TAG, "GPS preparation could not start recording", failure);
                return false;
            }
            gpsPreparationObservable.setValue(GpsPreparation.idle());
            return true; // The newest validated fix initializes the existing recorder.
        }
        if ((p0GpsGuard && isRecording() || !mapGpsOwners.isEmpty()) && !GpsReadiness.usable(fix, now, 0, GpsReadiness.DEFAULT)) {
            newGpsStatus(GpsStatusValue.GPS_SIGNAL_BAD);
            return false;
        }
        return true;
    }


    private TrackPointCreator trackPointCreator;
    private TrackRecordingManager trackRecordingManager;

    // Guarded by this, together with lifecycle initialization/teardown and preference callbacks.
    // SharedPreferences can deliver a queued listener snapshot after unregistering it.
    private boolean preferenceCallbacksEnabled;

    private TrackRecordingServiceNotificationManager notificationManager;

    @Override
    public synchronized void onCreate() {
        super.onCreate();
        Log.d(TAG, "Create");

        handler = new Handler(Looper.getMainLooper());

        recordingStatusObservable = new MutableLiveData<>();
        updateRecordingStatus(STATUS_DEFAULT);
        gpsStatusObservable = new MutableLiveData<>(STATUS_GPS_DEFAULT);
        recordingDataObservable = new MutableLiveData<>(RecordingData.NOT_RECORDING);

        trackPointCreator = new TrackPointCreator(this);
        trackRecordingManager = new TrackRecordingManager(this, trackPointCreator, this, handler);

        notificationManager = new TrackRecordingServiceNotificationManager(this);

        preferenceCallbacksEnabled = true;
        PreferencesUtils.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public synchronized void onDestroy() {
        Log.d(TAG, "Destroying");
        // Stop accepting callbacks before releasing any dependency or stopping sensors.
        preferenceCallbacksEnabled = false;
        gpsReadiness = null;
        gpsPreparationObservable.postValue(GpsPreparation.idle());
        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(this);
        if (trackPointCreator != null && trackPointCreator.isStarted()) {
            stopSensors();
        }

        trackPointCreator = null;

        handler.removeCallbacksAndMessages(null); //Some tests do not finish the recording completely
        handler = null;

        trackRecordingManager = null;

        // Reverse order from onCreate
        notificationManager = null;


        recordingStatusObservable = null;
        gpsStatusObservable = null;
        recordingDataObservable = null;

        Log.d(TAG, "Destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!de.dennisguse.opentracks.p0.recording.RecordingPermissions.hasAny(this)) { stopSelf(startId); return START_NOT_STICKY; }
        if (intent == null) {
            Log.i(TAG, "Service got killed and is restarting");

            //Best guess: do we need to resume a recording?
            Track.Id trackId = TrackIdStorage.get(this);
            if(trackId==null) {stopSelf(startId);return START_NOT_STICKY;}
            if (trackId != null) {
                // System recovery is not an explicit Connect action when automatic retry is off.
                if(!de.dennisguse.opentracks.p0.ble.HeartRateCommands.autoReconnectEnabled(this))
                    de.dennisguse.opentracks.p0.ble.HeartRateCommands.setConnectionEnabled(this,false);
                Log.i(TAG, "After restart: resuming recording for " + trackId);
                resumeTrack(trackId);
                if(!isRecording()){TrackIdStorage.unset(this);stopSelf(startId);return START_NOT_STICKY;}
            }
        }
        return START_STICKY;
    }

    @Override
    public Binder onBind(Intent intent) {
        return binder;
    }

    public Track.Id startNewTrack() {
        if (!de.dennisguse.opentracks.p0.recording.RecordingPermissions.hasAny(this)) return null;
        if (isRecording()) {
            Log.w(TAG, "Ignore startNewTrack. Already recording.");
            return null;
        }
        Log.i(TAG, "startNewTrack");

        // Set recording status
        Track.Id trackId = trackRecordingManager.startNewTrack();
        startRecording(trackId);
        return trackId;
    }

    public static Track.Id activeTrackId(android.content.Context context) {return TrackIdStorage.get(context);}

    public void resumeTrack(Track.Id trackId) {
        if (!de.dennisguse.opentracks.p0.recording.RecordingPermissions.hasAny(this)) return;
        if (!trackRecordingManager.resumeExistingTrack(trackId)) {
            Log.w(TAG, "Cannot resume a non-existing track.");
            return;
        }
        Log.i(TAG, "resumeTrack");

        startRecording(trackId);
    }

    private void startRecording(Track.Id trackId) {
        updateRecordingStatus(RecordingStatus.record(trackId));
        TrackIdStorage.set(this, trackId);

        // A Bluetooth-only preview may have started before GPS permission was granted.
        // Reopen only GPS when beginning a workout; keep the selected HR connection alive.
        if (areSensorsStarted()) {
            trackPointCreator.getSensorManager().getHeartRateHandler().beginRecordingSegment(trackPointCreator.getNow());
        }
        startSensors();


        notificationManager.onRecordingStarted(this, recordingStatus.trackId());

        // Update instance variables
        updateRecordingData.run();
        handler.postDelayed(updateRecordingData, RECORDING_DATA_UPDATE_INTERVAL.toMillis());
    }

    public void tryStartSensors() {
        if (!isRecording()&&!isAcquiringGps()&&!de.dennisguse.opentracks.p0.recording.RecordingPermissions.hasBluetooth(this)) {stopSelf();return;}
        Log.i(TAG, "tryStartSensors");

        startSensors();
    }

    private synchronized void startSensors() {
        boolean gpsNeeded=isRecording() || isAcquiringGps() || !mapGpsOwners.isEmpty();
        trackPointCreator.getSensorManager().setGpsRecording(gpsNeeded);
        if (!trackPointCreator.isStarted()) {
            Log.i(TAG, "startSensors");
            trackPointCreator.start(this, handler);
        }
        // Refresh the foreground capabilities after a newly granted GPS/Bluetooth permission.
        ServiceCompat.startForeground(this, TrackRecordingServiceNotificationManager.NOTIFICATION_ID, notificationManager.onSensorsStarted(this,gpsNeeded), de.dennisguse.opentracks.p0.recording.RecordingPermissions.foregroundTypes(this,gpsNeeded));
    }

    public synchronized void stopIdleSensors() {
        if (isRecording() || isAcquiringGps() || !mapGpsOwners.isEmpty()) return;
        if (areSensorsStarted()) stopSensors();
        stopSelf();
    }

    public void endCurrentTrack() {
        if (!isRecording()) {
            Log.w(TAG, "Ignore endCurrentTrack. Not recording.");
            return;
        }

        // Set recording status
        updateRecordingStatus(STATUS_DEFAULT);

        trackRecordingManager.endCurrentTrack();
        TrackIdStorage.unset(this);

        stopUpdateRecordingData();


        if (mapGpsOwners.isEmpty()) stopSensors(); else startSensors();
    }

    void stopSensors() {
        trackPointCreator.stop();
        stopForeground(true);
        notificationManager.stop();
        gpsStatusObservable.postValue(STATUS_GPS_DEFAULT);
    }

    public Marker.Id createMarker() {
        if (!isRecording()) {
            return null;
        }

        //TODO This contains some duplication to TrackRecodingActivity's Marker creation
        TrackPoint trackPoint = trackRecordingManager.getLastStoredTrackPointWithLocation();
        if (trackPoint == null) {
            return null;
        }
        Marker marker = new Marker(
                null,
                recordingStatus.trackId(),
                "",
                "",
                "",
                trackPoint.position(),
                null
        );
        return new ContentProviderUtils(this).insertMarker(marker);
    }

    @Override
    public boolean newTrackPoint(TrackPoint trackPoint, Distance thresholdHorizontalAccuracy) {
        if (!isRecording()) {
            Log.w(TAG, "Ignore newTrackPoint. Not recording.");
            return false;
        }

        boolean stored = trackRecordingManager.onNewTrackPoint(trackPoint);
        //TODO Should be done during updateRecordingDataWhileRecording()
        notificationManager.updateTrackPoint(this, trackRecordingManager.getDataForUI(), thresholdHorizontalAccuracy);
        return stored;
    }

    @Override
    public void newGpsStatus(GpsStatusValue gpsStatusValue) {
        Log.i(TAG, "newGpsStatus: " + gpsStatusValue.message);

        if (notificationManager == null) {

            StringWriter writer = new StringWriter();
            Exception e = new RuntimeException("TrackRecording.newGpsStatus() called after onDestroy(); objectID: " + this + " with thread: " + Thread.currentThread());
            e.printStackTrace(new PrintWriter(writer));

            Log.e(TAG, e.getMessage() + " " + writer);
            return;
        }
        notificationManager.updateGPSStatus(getString(gpsStatusValue.message));
        gpsStatusObservable.postValue(gpsStatusValue);
    }

    public synchronized boolean areSensorsStarted() {
        return trackPointCreator != null && trackPointCreator.isStarted();
    }

    public synchronized de.dennisguse.opentracks.sensors.HeartRatePreview getHeartRatePreview() {
        return areSensorsStarted() ? trackPointCreator.getHeartRatePreview()
                : new de.dennisguse.opentracks.sensors.HeartRatePreview(null,de.dennisguse.opentracks.sensors.HeartRateStatus.disconnected());
    }

    @Nullable
    public synchronized de.dennisguse.opentracks.data.models.Position getGpsPreview() {
        return areSensorsStarted() ? trackPointCreator.getSensorManager().getGpsHandler().latestPosition() : null;
    }

    @Deprecated
    @VisibleForTesting
    public TrackPointCreator getTrackPointCreator() {
        return trackPointCreator;
    }

    @Deprecated
    @VisibleForTesting
    public TrackRecordingManager getTrackRecordingManager() {
        return trackRecordingManager;
    }

    public LiveData<GpsStatusValue> getGpsStatusObservable() {
        return gpsStatusObservable;
    }

    public LiveData<RecordingData> getRecordingDataObservable() {
        return recordingDataObservable;
    }

    private void updateRecordingDataWhileRecording() {
        if (!isRecording()) {
            Log.w(TAG, "Currently not recording; cannot update data.");
            return;
        }

        // Compute temporary track statistics using sensorData and update time.
        RecordingData data = trackRecordingManager.getDataForUI();


        recordingDataObservable.postValue(data);
    }

    public void onIdle() {
        // P0 keeps recording local and does not invoke an external text-to-speech engine.
    }

    @VisibleForTesting
    public void stopUpdateRecordingData() {
        handler.removeCallbacks(updateRecordingData);
    }

    public synchronized RecordingStatus getRecordingStatus() { return recordingStatus; }

    public LiveData<RecordingStatus> getRecordingStatusObservable() {
        return recordingStatusObservable;
    }

    private void updateRecordingStatus(RecordingStatus status) {
        Log.i(TAG, "new status " + recordingStatus + " -> " + status);
        recordingStatus = status;
        recordingStatusObservable.postValue(recordingStatus);
    }

    @Deprecated //TODO Should be @VisibleForTesting
    public boolean isRecording() {
        return recordingStatus.isRecording();
    }


    @Override
    public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (!preferenceCallbacksEnabled) {
            return;
        }
        trackRecordingManager.onSharedPreferenceChanged(sharedPreferences, key);
        trackPointCreator.onSharedPreferenceChanged(sharedPreferences, key);
        notificationManager.onSharedPreferenceChanged(sharedPreferences, key);
    }
}
