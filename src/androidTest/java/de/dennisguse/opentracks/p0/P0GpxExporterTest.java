package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;
import de.dennisguse.opentracks.data.*;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.data.SessionStore;
import de.dennisguse.opentracks.p0.export.P0GpxExporter;
import de.dennisguse.opentracks.p0.export.P0GpxFiles;
import de.dennisguse.opentracks.p0.recording.RecordingSession;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

public class P0GpxExporterTest {
    private final Context context=ApplicationProvider.getApplicationContext();
    private final Instant base=Instant.parse("2026-11-05T02:00:00Z");
    private final ContentProviderUtils store=new ContentProviderUtils(context);
    private Track.Id id;
    @Before public void before() {
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());
        id=store.insertTrack(new Track(null,UUID.randomUUID(),"Synthetic <run> & heart rate","Local & offline","",ActivityType.RUNNING,ZoneOffset.ofHours(8),Statistics.DEFAULT));
    }
    private void point(int second,double lat,TrackPoint.Type type,HeartRate heart) {
        store.insertTrackPoint(new TrackPoint(null,type,new Position(base.plusSeconds(second),lat,103.8,Distance.of(2),Altitude.WGS84.of(12.5),null,null,Speed.of(3)),null,heart,null,null,null,null),id);
    }
    private void raw(Instant time,int bpm) {
        new HeartRateRepository(context).insert(id,new HeartRate(bpm,new HeartRateSample(time,"AA:BB:CC:DD:EE:FF","Synthetic & strap",true,12,List.of(1024,1000)).captured(time,"AA:BB:CC:DD:EE:FF","Synthetic & strap","session-one")));
    }
    private Document export(String name) throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();new P0GpxExporter(context).write(id,bytes);
        var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
        Document xml=factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(P0GpxExporter.GPX,xml.getDocumentElement().getNamespaceURI());assertEquals("1.1",xml.getDocumentElement().getAttribute("version"));
        String configured=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
        File directory=new File(configured!=null?new File(configured):new File(context.getExternalMediaDirs()[0],"additional_test_output"),"p0-gpx");
        assertTrue(directory.isDirectory()||directory.mkdirs());
        try(var file=new FileOutputStream(new File(directory,name+".gpx"))){bytes.writeTo(file);}
        return xml;
    }
    private int count(Document xml,String namespace,String tag){return xml.getElementsByTagNameNS(namespace,tag).getLength();}
    @Test public void everyRawSampleAndExactMetadataSurviveWithOnlyRealGpsPoints() throws Exception {
        point(0,1.3,TrackPoint.Type.TRACKPOINT,null);point(2,1.3002,TrackPoint.Type.TRACKPOINT,null);
        raw(base,70);raw(base.plusMillis(1500),76);raw(base.plusSeconds(2),90);raw(base.plusSeconds(9).plusNanos(123456789),300);
        Document xml=export("raw-complete");assertEquals(2,count(xml,P0GpxExporter.GPX,"trkpt"));assertEquals(4,count(xml,P0GpxExporter.RAW,"sample"));
        Element sample=(Element)xml.getElementsByTagNameNS(P0GpxExporter.RAW,"sample").item(3);
        assertEquals(base.plusSeconds(9).plusNanos(123456789).toString(),sample.getAttribute("time"));assertEquals("300",sample.getAttribute("bpm"));
        assertEquals("Synthetic & strap",sample.getAttribute("deviceName"));assertEquals("AA:BB:CC:DD:EE:FF",sample.getAttribute("deviceAddress"));
        assertEquals("session-one",sample.getAttribute("connectionId"));assertEquals("ble_sig_heart_rate",sample.getAttribute("source"));assertEquals("true",sample.getAttribute("contact"));assertEquals("12",sample.getAttribute("energyExpended"));
        assertEquals("1024",sample.getElementsByTagNameNS(P0GpxExporter.RAW,"rr1024").item(0).getTextContent());assertEquals("1000",sample.getElementsByTagNameNS(P0GpxExporter.RAW,"rr1024").item(1).getTextContent());assertFalse(sample.hasAttribute("nearestPointId"));
        assertEquals("70",xml.getElementsByTagNameNS(P0GpxExporter.GARMIN,"hr").item(0).getTextContent());assertEquals("90",xml.getElementsByTagNameNS(P0GpxExporter.GARMIN,"hr").item(1).getTextContent());
        assertEquals(base.toString(),xml.getElementsByTagNameNS(P0GpxExporter.GPX,"time").item(1).getTextContent());assertEquals("12.5",xml.getElementsByTagNameNS(P0GpxExporter.GPX,"ele").item(0).getTextContent());
    }
    @Test public void heartRateOnlyWorkoutHasNoInventedGpsPoints() throws Exception {
        raw(base,76);raw(base.plusSeconds(1),76);Document xml=export("heart-only");
        assertEquals(0,count(xml,P0GpxExporter.GPX,"trkpt"));assertEquals(0,count(xml,P0GpxExporter.GPX,"trkseg"));assertEquals(2,count(xml,P0GpxExporter.RAW,"sample"));
    }
    @Test public void gpsOnlyWorkoutExportsWithoutHeartRate() throws Exception {
        point(0,1.3,TrackPoint.Type.TRACKPOINT,null);Document xml=export("gps-only");
        assertEquals(1,count(xml,P0GpxExporter.GPX,"trkpt"));assertEquals(0,count(xml,P0GpxExporter.RAW,"sample"));assertEquals(0,count(xml,P0GpxExporter.GARMIN,"hr"));assertEquals(1,count(xml,P0GpxExporter.GARMIN,"speed"));
    }
    @Test public void pausesAutomaticGapsAndIdleCreateSeparateSegments() throws Exception {
        point(0,1.3,TrackPoint.Type.TRACKPOINT,null);point(1,1.3001,TrackPoint.Type.TRACKPOINT,null);
        point(10,2.3,TrackPoint.Type.SEGMENT_START_AUTOMATIC,null);point(11,2.3001,TrackPoint.Type.TRACKPOINT,null);
        store.insertTrackPoint(new TrackPoint(TrackPoint.Type.IDLE,base.plusSeconds(12)),id);
        point(20,3.3,TrackPoint.Type.TRACKPOINT,null);store.insertTrackPoint(new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL,base.plusSeconds(21)),id);
        store.insertTrackPoint(new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL,base.plusSeconds(30)),id);point(30,4.3,TrackPoint.Type.TRACKPOINT,null);
        raw(base.plusMillis(1500),80);raw(base.plusSeconds(30),90);Document xml=export("segments");
        assertEquals(4,count(xml,P0GpxExporter.GPX,"trkseg"));assertEquals(6,count(xml,P0GpxExporter.GPX,"trkpt"));
        Element unmatched=(Element)xml.getElementsByTagNameNS(P0GpxExporter.RAW,"sample").item(0);assertFalse(unmatched.hasAttribute("nearestPointId"));
    }
    @Test public void legacyPointHeartRateIsExportedWithoutInventingRawNotifications() throws Exception {
        point(0,1.3,TrackPoint.Type.TRACKPOINT,new HeartRate(80));
        store.insertTrackPoint(new TrackPoint(null,TrackPoint.Type.TRACKPOINT,Position.of(base.plusSeconds(1)),null,new HeartRate(85),null,null,null,null),id);
        Document xml=export("legacy");
        assertEquals("80",xml.getElementsByTagNameNS(P0GpxExporter.GARMIN,"hr").item(0).getTextContent());assertEquals(0,count(xml,P0GpxExporter.RAW,"sample"));
        assertEquals(2,count(xml,P0GpxExporter.RAW,"legacyHeartRate"));assertEquals(1,count(xml,P0GpxExporter.GPX,"trkpt"));
    }
    @Test public void longGpsSilenceDoesNotDrawABridgeEvenWithoutAnOldSegmentMarker() throws Exception {
        point(0,1.3,TrackPoint.Type.TRACKPOINT,null);point(1,1.3001,TrackPoint.Type.TRACKPOINT,null);
        point(40,1.301,TrackPoint.Type.TRACKPOINT,null);point(41,1.3011,TrackPoint.Type.TRACKPOINT,null);
        raw(base.plusSeconds(20),76);Document xml=export("gps-gap");
        assertEquals(2,count(xml,P0GpxExporter.GPX,"trkseg"));assertEquals(4,count(xml,P0GpxExporter.GPX,"trkpt"));assertEquals(1,count(xml,P0GpxExporter.RAW,"sample"));
        assertFalse(((Element)xml.getElementsByTagNameNS(P0GpxExporter.RAW,"sample").item(0)).hasAttribute("nearestPointId"));
    }
    @Test public void xmlEscapingDecimalCoordinatesAndLocaleRemainValid() throws Exception {
        Locale original=Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);point(0,0.000001,TrackPoint.Type.TRACKPOINT,null);
            store.insertTrackPoint(new TrackPoint(TrackPoint.Type.TRACKPOINT,new Position(base.plusSeconds(1),1.3,180d,null,null,null,null,null)),id);
            Document xml=export("decimal-escaping");assertEquals("Synthetic <run> & heart rate",xml.getElementsByTagNameNS(P0GpxExporter.GPX,"name").item(0).getTextContent());
            Element first=(Element)xml.getElementsByTagNameNS(P0GpxExporter.GPX,"trkpt").item(0);assertEquals(0,new java.math.BigDecimal("0.000001").compareTo(new java.math.BigDecimal(first.getAttribute("lat"))));
            Element second=(Element)xml.getElementsByTagNameNS(P0GpxExporter.GPX,"trkpt").item(1);assertEquals("-180.0",second.getAttribute("lon"));
        }finally{Locale.setDefault(original);}
    }
    @Test public void controlEventsWithCoordinatesNeverBecomeExtraGpsSamples() throws Exception {
        point(0,1.3,TrackPoint.Type.TRACKPOINT,null);point(1,1.3,TrackPoint.Type.SEGMENT_END_MANUAL,null);
        point(10,2.3,TrackPoint.Type.SEGMENT_START_MANUAL,null);point(11,2.3,TrackPoint.Type.TRACKPOINT,null);point(12,2.3,TrackPoint.Type.IDLE,null);
        Document xml=export("event-boundaries");assertEquals(2,count(xml,P0GpxExporter.GPX,"trkpt"));assertEquals(2,count(xml,P0GpxExporter.GPX,"trkseg"));
    }
    @Test public void outputFailureIsPropagatedAndProtectedWorkoutCannotExport() throws Exception {
        try {
            new P0GpxExporter(context).write(id,new OutputStream(){@Override public void write(int value) throws IOException{throw new IOException("Synthetic disk failure");}});fail("Must report write failure");
        }catch(IOException expected){}
        new SessionStore(context).save(RecordingSession.ready().begin(id.id()).pause());
        try {new P0GpxExporter(context).write(id,new ByteArrayOutputStream());fail("Protected workout must not export");}
        catch(IOException expected){}finally{new SessionStore(context).save(RecordingSession.ready());}
    }
    @Test public void invalidCoordinatesFailAndIncompletePrivateFileIsRemoved() throws Exception {
        point(0,91,TrackPoint.Type.TRACKPOINT,null);
        File directory=new File(context.getCacheDir(),"p0_exports");
        File[] before=directory.listFiles((folder,name)->name.startsWith("RunnerGardenia-"+id.id()+"-"));
        try {new P0GpxFiles(context).prepare(id);fail("Invalid coordinates must not become a valid-looking export");}
        catch(IOException expected){}
        File[] after=directory.listFiles((folder,name)->name.startsWith("RunnerGardenia-"+id.id()+"-"));
        assertEquals(before==null?0:before.length,after==null?0:after.length);
    }
    @Test public void localDocumentStreamAndReadOnlyShareUriContainTheActualGpx() throws Exception {
        point(0,1.3,TrackPoint.Type.TRACKPOINT,null);raw(base,76);
        P0GpxFiles files=new P0GpxFiles(context);File source=files.prepare(id);File target=new File(source.getParentFile(),"document-test.gpx");
        try {
            var uri=androidx.core.content.FileProvider.getUriForFile(context,context.getPackageName()+".fileprovider",target);files.save(source,uri);
            assertArrayEquals(java.nio.file.Files.readAllBytes(source.toPath()),java.nio.file.Files.readAllBytes(target.toPath()));
            android.content.Intent share=files.share(source);assertEquals(android.content.Intent.ACTION_SEND,share.getAction());assertEquals("application/gpx+xml",share.getType());
            assertEquals(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,share.getFlags());assertEquals(share.getParcelableExtra(android.content.Intent.EXTRA_STREAM),share.getClipData().getItemAt(0).getUri());
            try(InputStream input=context.getContentResolver().openInputStream(share.getClipData().getItemAt(0).getUri())){assertNotNull(input);assertArrayEquals(java.nio.file.Files.readAllBytes(source.toPath()),input.readAllBytes());}
            export("local-file");
        }finally{source.delete();target.delete();}
    }
}
