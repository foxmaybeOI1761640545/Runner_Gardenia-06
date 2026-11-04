package de.dennisguse.opentracks.p0.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.CustomContentProvider;
import de.dennisguse.opentracks.data.HeartRateRepository;
import de.dennisguse.opentracks.data.TrackPointIterator;
import de.dennisguse.opentracks.data.tables.TracksColumns;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.statistics.HeartRateSummary;

/** App/data boundary. Call from a worker, never perform database IO in the Activity. */
public final class WorkoutRepository {
    private final Context context;
    private final ContentResolver resolver;
    private final ContentProviderUtils tracks;
    private final HeartRateRepository heartRate;

    public WorkoutRepository(Context context) {
        this.context=context.getApplicationContext();resolver=this.context.getContentResolver();
        tracks=new ContentProviderUtils(this.context);heartRate=new HeartRateRepository(this.context);
    }
    public WorkoutSummary find(Track.Id id) {
        Track track=tracks.getTrack(id);
        return track==null?null:new WorkoutSummary(track,heartRateStatistics(id));
    }
    public List<WorkoutSummary> list() {
        List<WorkoutSummary> result=new ArrayList<>();
        try(Cursor cursor=resolver.query(CustomContentProvider.UrlType.TRACKS.getUri(),null,null,null,TracksColumns.STARTTIME+" DESC, _id DESC")) {
            if(cursor==null)throw new IllegalStateException("Recording query returned no cursor");
            while(cursor.moveToNext()) {
                Track track=ContentProviderUtils.createTrack(cursor);
                result.add(new WorkoutSummary(track,heartRateStatistics(track.id())));
            }
        }
        return List.copyOf(result);
    }
    public HeartRateSummary heartRateStatistics(Track.Id id) {
        try(Cursor cursor=resolver.query(CustomContentProvider.UrlType.HEART_RATE_SAMPLES.getUri(),
                new String[]{"COUNT(*)","MIN(bpm)","MAX(bpm)","AVG(bpm)"},"trackid=?",new String[]{Long.toString(id.id())},null)) {
            if(cursor==null||!cursor.moveToFirst())throw new IllegalStateException("Missing heart-rate aggregate");
            if(cursor.getLong(0)==0)return HeartRateSummary.EMPTY;
            return new HeartRateSummary(cursor.getLong(0),cursor.getDouble(1),cursor.getDouble(2),cursor.getDouble(3));
        }
    }
    public void forEachPoint(Track.Id id, Consumer<TrackPoint> consumer) {
        try(TrackPointIterator iterator=tracks.getTrackPointIterator(id,null)) {
            while(iterator.hasNext())consumer.accept(iterator.next());
        }
    }
    public void forEachHeartRate(Track.Id id, Consumer<HeartRate> consumer) {
        heartRate.forEach(id,consumer);
    }
    public List<HeartRate> heartRates(Track.Id id) { return heartRate.read(id); }
    public void delete(Track.Id id) { tracks.deleteTrack(context,id); }
}
