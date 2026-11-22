package de.dennisguse.opentracks.services;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.dennisguse.opentracks.data.models.Track;

/**
 * For handling restarts of TrackRecordingService: while recording, stores the {@link de.dennisguse.opentracks.data.models.Track.Id}.
 */
class TrackIdStorage {

    private final static String SHARED_PREFERENCE_NAME = "TrackRecordingService_pref";

    private final static String KEY = "trackId_key";
    private final static String P0_GPS = "p0GpsSession";

    private static SharedPreferences sharedPreferences;

    private static void init(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        }
    }

    @Nullable
    public static Track.Id get(Context context) {
        TrackIdStorage.init(context);

        long value = sharedPreferences.getLong(KEY, -1);
        if (value < 0) {
            return null;
        }

        return new Track.Id(value);
    }

    public static void set(Context context, @NonNull Track.Id trackId) {
        set(context, trackId, false);
    }

    /** Session recovery metadata, committed atomically with its active ID; never a user GPS preference. */
    public static void set(Context context, @NonNull Track.Id trackId, boolean p0GpsSession) {
        TrackIdStorage.init(context);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(KEY, trackId.id());
        editor.putBoolean(P0_GPS, p0GpsSession);
        editor.commit();
    }

    public static boolean isP0GpsSession(Context context) {
        return get(context) != null && sharedPreferences.getBoolean(P0_GPS, false);
    }

    public static void unset(Context context) {
        TrackIdStorage.init(context);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY);
        editor.remove(P0_GPS);
        editor.commit();
    }

}
