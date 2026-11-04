package de.dennisguse.opentracks.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.HeartRateSample;
import de.dennisguse.opentracks.data.models.Track;

/** Local-only raw samples; no second database, exporter, service or upload queue. */
public final class HeartRateRepository {
    private final android.content.ContentResolver resolver;
    public HeartRateRepository(Context context) { resolver = context.getContentResolver(); }

    public void insert(Track.Id track, HeartRate heartRate) {
        if (track == null || heartRate == null || !heartRate.isValid() || heartRate.sample() == null) return;
        HeartRateSample sample = heartRate.sample();
        if (sample.capturedAt() == null || Boolean.FALSE.equals(sample.contactDetected())) return;
        ContentValues values = new ContentValues();
        values.put("trackid", track.id());
        values.put("time", sample.capturedAt().toEpochMilli());
        // Repeated GPS/UI snapshots of the same notification are idempotent; repeated BPM values are valid samples.
        values.put("sample_key", sample.sampleId());
        values.put("bpm", heartRate.value());
        values.put("metadata", encode(sample));
        resolver.insert(CustomContentProvider.UrlType.HEART_RATE_SAMPLES.getUri(), values);
    }

    public List<HeartRate> read(Track.Id track) {
        List<HeartRate> result = new ArrayList<>();
        forEach(track,result::add);
        return result;
    }

    /** Chronological streaming for exports; leaves incremental row-ID chart queries unchanged. */
    public void forEach(Track.Id track, java.util.function.Consumer<HeartRate> consumer) {
        try (Cursor cursor = resolver.query(CustomContentProvider.UrlType.HEART_RATE_SAMPLES.getUri(),
                new String[]{"bpm", "metadata"}, "trackid=?", new String[]{Long.toString(track.id())}, "time, _id")) {
            if (cursor == null) throw new IllegalStateException("Heart-rate query returned no cursor");
            while (cursor.moveToNext()) consumer.accept(new HeartRate(cursor.getFloat(0), decode(cursor.getString(1))));
        }
    }

    public void forEachAfter(Track.Id track, long afterId, java.util.function.BiConsumer<Long,HeartRate> consumer) {
        try (Cursor c = resolver.query(CustomContentProvider.UrlType.HEART_RATE_SAMPLES.getUri(),
                new String[]{"_id","bpm","metadata"},"trackid=? AND _id>?",
                new String[]{Long.toString(track.id()),Long.toString(afterId)},"_id")) {
            if (c == null) throw new IllegalStateException("Missing heart-rate cursor");
            while (c.moveToNext()) consumer.accept(c.getLong(0),new HeartRate(c.getFloat(1),decode(c.getString(2))));
        }
    }

    private static String encode(HeartRateSample sample) {
        try {
            JSONObject json = new JSONObject().put("schema", 1).put("source", sample.source())
                    .put("sampleId", sample.sampleId()).put("connectionId", sample.connectionId())
                    .put("capturedAt", sample.capturedAt().toString()).put("deviceAddress", sample.deviceAddress())
                    .put("deviceName", sample.deviceName()).put("contactDetected", sample.contactDetected())
                    .put("energyExpended", sample.energyExpended()).put("rr1024", new JSONArray(sample.rrIntervals1024()));
            return json.toString();
        } catch (JSONException invalid) { throw new IllegalArgumentException("Invalid heart-rate metadata", invalid); }
    }

    private static HeartRateSample decode(String encoded) {
        try {
            JSONObject json = new JSONObject(encoded);
            if (json.getInt("schema") != 1 || !"ble_sig_heart_rate".equals(json.getString("source"))) {
                throw new IllegalArgumentException("Unsupported heart-rate metadata schema");
            }
            List<Integer> rr = new ArrayList<>();
            JSONArray intervals = json.getJSONArray("rr1024");
            for (int i = 0; i < intervals.length(); i++) rr.add(intervals.getInt(i));
            return new HeartRateSample(Instant.parse(json.getString("capturedAt")),
                    json.optString("deviceAddress", null), json.optString("deviceName", null),
                    json.has("contactDetected") ? json.getBoolean("contactDetected") : null,
                    json.has("energyExpended") ? json.getInt("energyExpended") : null, rr,
                    json.getString("sampleId"), json.optString("connectionId", null));
        } catch (JSONException invalid) { throw new IllegalArgumentException("Invalid stored heart-rate metadata", invalid); }
    }
}
