package de.dennisguse.opentracks.p0;
import android.content.Context;
import java.time.*;
import java.util.*;
import de.dennisguse.opentracks.data.*;
import de.dennisguse.opentracks.data.models.*;
/** Explicitly synthetic persisted test inputs, never installed in the production APK. */
public final class P05HistoryFixtures {
 public static Track.Id create(Context context,String name,boolean samples){
  var store=new ContentProviderUtils(context);var id=store.insertTrack(new Track(null,UUID.randomUUID(),name,"Synthetic batch fixture","",ActivityType.RUNNING,ZoneOffset.UTC,Statistics.DEFAULT));
  if(samples){Instant time=Instant.parse("2026-11-09T02:00:00Z");for(int i=0;i<2;i++)store.insertTrackPoint(new TrackPoint(null,TrackPoint.Type.TRACKPOINT,new Position(time.plusSeconds(i),1.3+i*.00001,103.8,Distance.of(2),Altitude.WGS84.of(12.5),null,null,Speed.of(3)),null,null,null,null,null,null),id);
   var raw=new HeartRateRepository(context);for(int i=0;i<3;i++)raw.insert(id,new HeartRate(70+i,new HeartRateSample(time.plusSeconds(i),"AA:BB:CC:DD:EE:FF","Synthetic batch strap",true,12,List.of(1024))));}
  return id;
 }
}
