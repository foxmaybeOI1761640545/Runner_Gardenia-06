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
import de.dennisguse.opentracks.data.tables.TrackPointsColumns;
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
        return track==null?null:project(track);
    }
    public List<WorkoutSummary> list() {
        return queryNewest(null);
    }
    public List<WorkoutSummary> listNewest(int limit) {
        if(limit<=0)throw new IllegalArgumentException("Positive history limit required");
        return queryNewest(limit);
    }
    private List<WorkoutSummary> queryNewest(Integer limit) {
        List<WorkoutSummary> result=new ArrayList<>();
        String order=TracksColumns.STARTTIME+" DESC, _id DESC"+(limit==null?"":" LIMIT "+limit);
        try(Cursor cursor=resolver.query(CustomContentProvider.UrlType.TRACKS.getUri(),null,null,null,order)) {
            if(cursor==null)throw new IllegalStateException("Recording query returned no cursor");
            while(cursor.moveToNext()) {
                Track track=ContentProviderUtils.createTrack(cursor);
                result.add(project(track));
            }
        }
        return List.copyOf(result);
    }
    private WorkoutSummary project(Track track) {
        HeartRateSummary raw=heartRateStatistics(track.id());
        if(raw.count()>0)return new WorkoutSummary(track,raw,WorkoutSummary.HeartRateOrigin.RAW_NOTIFICATIONS);
        String column=TrackPointsColumns.SENSOR_HEARTRATE;
        try(Cursor cursor=resolver.query(CustomContentProvider.UrlType.TRACKPOINTS.getUri(),
                new String[]{"COUNT(*)","MIN("+column+")","MAX("+column+")","AVG("+column+")"},
                "trackid=? AND "+column+">0 AND "+column+"<=65535",new String[]{Long.toString(track.id().id())},null)) {
            if(cursor==null||!cursor.moveToFirst())throw new IllegalStateException("Missing legacy heart-rate aggregate");
            if(cursor.getLong(0)>0)return new WorkoutSummary(track,new HeartRateSummary(cursor.getLong(0),cursor.getDouble(1),cursor.getDouble(2),cursor.getDouble(3)),WorkoutSummary.HeartRateOrigin.LEGACY_TRACK_POINTS);
        }
        return new WorkoutSummary(track,HeartRateSummary.EMPTY,WorkoutSummary.HeartRateOrigin.NONE);
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
    public boolean isProtected(Track.Id id) {
        Long journal=new SessionStore(context).recover().trackId();
        return (journal!=null&&journal==id.id())||id.equals(de.dennisguse.opentracks.services.TrackRecordingService.activeTrackId(context));
    }
    public void delete(Track.Id id) {
        if(isProtected(id))throw new IllegalStateException("Finish the active or paused workout before deleting it");
        tracks.deleteTrack(context,id);
    }
}
