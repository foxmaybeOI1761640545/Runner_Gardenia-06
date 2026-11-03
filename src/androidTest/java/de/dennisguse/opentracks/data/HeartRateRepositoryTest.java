package de.dennisguse.opentracks.data;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import java.time.Instant;
import java.util.List;
import de.dennisguse.opentracks.data.models.*;

public class HeartRateRepositoryTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private ContentProviderUtils tracks;
    private HeartRateRepository samples;
    @Before public void prepare() {
        tracks = new ContentProviderUtils(context);
        tracks.deleteAllTracks(context);
        samples = new HeartRateRepository(context);
    }
    private Track.Id track() { return tracks.insertTrack(new Track(null, java.util.UUID.randomUUID(), "Raw HR test", "", "", ActivityType.UNKNOWN, java.time.ZoneOffset.UTC, Statistics.DEFAULT)); }
    private HeartRate sample(int bpm, long second) {
        return new HeartRate(bpm, new HeartRateSample(Instant.EPOCH.plusSeconds(second),
                "AA:BB:CC:DD:EE:FF", "Test strap", true, 12, List.of(1024, 1000)));
    }
    @Test public void exactMetadataSurvivesRepositoryReopening() {
        Track.Id id = track(); HeartRate original = sample(72, 1);
        samples.insert(id, original);
        assertEquals(List.of(original), new HeartRateRepository(context).read(id));
        assertEquals(List.of(1024, 1000), samples.read(id).get(0).sample().rrIntervals1024());
    }
    @Test public void snapshotDuplicatesDoNotDiscardNewIdenticalBpmNotifications() {
        Track.Id id = track(); HeartRate original = sample(72, 1);
        samples.insert(id, original); samples.insert(id, original);
        samples.insert(id, sample(72, 1)); // same clock millisecond, distinct notification identity
        assertEquals(2, samples.read(id).size());
    }
    @Test public void samplesAreOrderedByCaptureTimeAndIsolatedByTrack() {
        Track.Id first = track(); Track.Id second = track();
        HeartRate early = sample(65, 1); HeartRate late = sample(80, 3);
        samples.insert(first, late); samples.insert(first, early); samples.insert(second, sample(90, 2));
        assertEquals(List.of(early, late), samples.read(first));assertEquals(1, samples.read(second).size());
    }
    @Test public void missingOptionalDataAndNoStrapTracksRemainReadable() {
        Track.Id id = track(); assertTrue(samples.read(id).isEmpty());
        HeartRate noOptional = new HeartRate(70, new HeartRateSample(Instant.EPOCH, null, null, null, null, List.of()));
        samples.insert(id, noOptional); assertEquals(List.of(noOptional), samples.read(id));
    }
    @Test public void invalidSamplesAreRejectedWithoutInventingMissingValues() {
        Track.Id id = track();
        for (float value : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY, 65536}) {
            HeartRate invalid = new HeartRate(value, sample(70, 1).sample());
            assertFalse(invalid.isValid()); samples.insert(id, invalid);
        }
        samples.insert(id, new HeartRate(70, new HeartRateSample(Instant.EPOCH, null, null, false, null, List.of())));
        samples.insert(id, HeartRate.of(70)); // legacy BPM-only rows use the original TrackPoint storage
        assertTrue(samples.read(id).isEmpty());
    }
    @Test public void deletingTrackCascadesOnlyItsOwnRawSamples() {
        Track.Id first = track(); Track.Id second = track();
        samples.insert(first, sample(70, 1)); samples.insert(second, sample(80, 1));
        tracks.deleteTrack(context, first); assertTrue(samples.read(first).isEmpty());assertEquals(1, samples.read(second).size());
    }
}
