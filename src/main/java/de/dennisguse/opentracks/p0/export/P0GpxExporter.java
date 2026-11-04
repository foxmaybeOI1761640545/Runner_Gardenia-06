package de.dennisguse.opentracks.p0.export;

import android.content.Context;
import android.util.Xml;
import org.xmlpull.v1.XmlSerializer;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.data.WorkoutRepository;

/** GPX 1.1 with real coordinates only, and a lossless local raw-notification extension. */
public final class P0GpxExporter {
    public static final String GPX="http://www.topografix.com/GPX/1/1";
    public static final String RAW="urn:runner-gardenia:gpx:1";
    public static final String GARMIN="http://www.garmin.com/xmlschemas/TrackPointExtension/v2";
    private final WorkoutRepository repository;
    public P0GpxExporter(Context context) {repository=new WorkoutRepository(context);}
    private static boolean boundary(TrackPoint point) {
        return point.isSegmentManualStart()||point.isSegmentManualEnd()||point.isIdleTriggered()||point.type()==TrackPoint.Type.SEGMENT_START_AUTOMATIC;
    }
    private static final class Segments {
        private java.time.Instant previousGps;
        private int number;
        boolean next(TrackPoint point) {
            boolean split=boundary(point);
            if(previousGps!=null&&point.position().hasLocation()) {
                var elapsed=java.time.Duration.between(previousGps,point.getTime());
                // Match the existing 30-second lost-fix horizon, without fabricating a bridge.
                split|=elapsed.isNegative()||elapsed.compareTo(java.time.Duration.ofSeconds(30))>0;
            }
            if(split){number++;previousGps=null;}
            if(point.position().hasLocation())previousGps=point.getTime();
            return split;
        }
    }
    private static void interrupted() throws InterruptedIOException {
        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("GPX export interrupted");
    }
    private static void text(XmlSerializer xml,String namespace,String tag,Object value) throws IOException {
        xml.startTag(namespace,tag).text(String.valueOf(value)).endTag(namespace,tag);
    }
    private static void attribute(XmlSerializer xml,String key,Object value) throws IOException {
        if(value!=null)xml.attribute(null,key,String.valueOf(value));
    }
    private static String decimal(double value) {return java.math.BigDecimal.valueOf(value).toPlainString();}
    public void write(Track.Id id,OutputStream output) throws IOException {
        if(repository.isProtected(id))throw new IOException("Finish the workout before exporting");
        var workout=repository.find(id);
        if(workout==null)throw new IOException("Recording no longer exists");
        List<HeartRateTimeMatcher.Anchor> anchors=new ArrayList<>();Segments index=new Segments();
        repository.forEachPoint(id,point->{
            index.next(point);
            if(point.position().hasLocation())anchors.add(new HeartRateTimeMatcher.Anchor(point.id().id(),point.getTime(),index.number));
        });
        HeartRateTimeMatcher matcher=new HeartRateTimeMatcher(anchors);
        Map<Long,HeartRate> matched=new HashMap<>();
        repository.forEachHeartRate(id,value->{
            var anchor=matcher.match(value.sample().capturedAt());
            if(anchor==null)return;
            HeartRate previous=matched.get(anchor.pointId());
            if(previous==null||HeartRateTimeMatcher.difference(anchor.time(),value.sample().capturedAt()).compareTo(
                    HeartRateTimeMatcher.difference(anchor.time(),previous.sample().capturedAt()))<0)matched.put(anchor.pointId(),value);
        });
        XmlSerializer xml=Xml.newSerializer();xml.setOutput(output,"UTF-8");xml.startDocument("UTF-8",true);
        xml.setPrefix("",GPX);xml.setPrefix("rg",RAW);xml.setPrefix("gpxtpx",GARMIN);
        xml.startTag(GPX,"gpx").attribute(null,"version","1.1").attribute(null,"creator","Runner Gardenia");
        xml.startTag(GPX,"metadata");text(xml,GPX,"name",workout.track().name());text(xml,GPX,"time",workout.track().statistics().startTime());xml.endTag(GPX,"metadata");
        xml.startTag(GPX,"trk");text(xml,GPX,"name",workout.track().name());text(xml,GPX,"desc",workout.track().description());text(xml,GPX,"type",workout.track().activityType().name());
        boolean[] open={false};Segments writing=new Segments();
        try {
            repository.forEachPoint(id,point->{
                try {
                    interrupted();
                    if(writing.next(point)&&open[0]){xml.endTag(GPX,"trkseg");open[0]=false;}
                    if(!point.position().hasLocation())return;
                    Position position=point.position();
                    if(!Double.isFinite(position.latitude())||!Double.isFinite(position.longitude())||Math.abs(position.latitude())>90||Math.abs(position.longitude())>180)
                        throw new IOException("Invalid GPS coordinates");
                    if(!open[0]){xml.startTag(GPX,"trkseg");open[0]=true;}
                    xml.startTag(GPX,"trkpt");attribute(xml,"lat",decimal(position.latitude()));attribute(xml,"lon",decimal(position.longitude()==180?-180:position.longitude()));
                    if(position.hasAltitude()&&Double.isFinite(position.altitude().toM()))text(xml,GPX,"ele",decimal(position.altitude().toM()));
                    text(xml,GPX,"time",point.getTime());xml.startTag(GPX,"extensions");
                    xml.startTag(RAW,"point");attribute(xml,"id",point.id().id());
                    if(position.hasHorizontalAccuracy()&&Double.isFinite(position.horizontalAccuracy().toM()))attribute(xml,"accuracyMeters",decimal(position.horizontalAccuracy().toM()));
                    if(position.hasSpeed()&&Double.isFinite(position.speed().toMPS()))attribute(xml,"speedMetersPerSecond",decimal(position.speed().toMPS()));
                    xml.endTag(RAW,"point");
                    HeartRate heart=matched.get(point.id().id());
                    if(heart==null&&workout.heartRate().count()>0&&workout.heartRateOrigin()==de.dennisguse.opentracks.p0.data.WorkoutSummary.HeartRateOrigin.LEGACY_TRACK_POINTS)heart=point.heartRate();
                    boolean standardHeart=heart!=null&&heart.isValid()&&heart.getBPM()<=255;
                    boolean speed=position.hasSpeed()&&Double.isFinite(position.speed().toMPS())&&position.speed().toMPS()>=0;
                    if(standardHeart||speed){
                        xml.startTag(GARMIN,"TrackPointExtension");
                        if(standardHeart)text(xml,GARMIN,"hr",Math.round(heart.getBPM()));
                        if(speed)text(xml,GARMIN,"speed",decimal(position.speed().toMPS()));
                        xml.endTag(GARMIN,"TrackPointExtension");
                    }
                    xml.endTag(GPX,"extensions");xml.endTag(GPX,"trkpt");
                }catch(IOException failure){throw new UncheckedIOException(failure);}
            });
            if(open[0])xml.endTag(GPX,"trkseg");xml.endTag(GPX,"trk");
            xml.startTag(GPX,"extensions");xml.startTag(RAW,"workout");attribute(xml,"id",id.id());attribute(xml,"uuid",workout.track().uuid());
            attribute(xml,"matching","nearest-within-1000ms-inside-gps-segment;ties-earlier;all-samples-retained");
            repository.forEachHeartRate(id,heart->{
                try {
                    interrupted();var sample=heart.sample();xml.startTag(RAW,"sample");
                    attribute(xml,"id",sample.sampleId());attribute(xml,"time",sample.capturedAt());attribute(xml,"bpm",Math.round(heart.getBPM()));
                    attribute(xml,"source",sample.source());attribute(xml,"deviceName",sample.deviceName());attribute(xml,"deviceAddress",sample.deviceAddress());attribute(xml,"connectionId",sample.connectionId());
                    attribute(xml,"contact",sample.contactDetected());attribute(xml,"energyExpended",sample.energyExpended());
                    var anchor=matcher.match(sample.capturedAt());if(anchor!=null)attribute(xml,"nearestPointId",anchor.pointId());
                    for(int interval:sample.rrIntervals1024())text(xml,RAW,"rr1024",interval);
                    xml.endTag(RAW,"sample");
                }catch(IOException failure){throw new UncheckedIOException(failure);}
            });
            if(workout.heartRateOrigin()==de.dennisguse.opentracks.p0.data.WorkoutSummary.HeartRateOrigin.LEGACY_TRACK_POINTS) {
                repository.forEachPoint(id,point->{
                    if(point.heartRate()==null||!point.heartRate().isValid())return;
                    try {
                        interrupted();xml.startTag(RAW,"legacyHeartRate");
                        attribute(xml,"pointId",point.id().id());attribute(xml,"time",point.getTime());attribute(xml,"bpm",point.heartRate().getBPM());
                        xml.endTag(RAW,"legacyHeartRate");
                    }catch(IOException failure){throw new UncheckedIOException(failure);}
                });
            }
            xml.endTag(RAW,"workout");xml.endTag(GPX,"extensions");xml.endTag(GPX,"gpx");xml.endDocument();xml.flush();
        }catch(UncheckedIOException failure){throw failure.getCause();}
    }
}
