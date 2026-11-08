package de.dennisguse.opentracks.p0;
import android.content.Context;
import java.time.Instant;
import java.util.*;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.chart.HeartRatePlot;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.recording.RecordingSession;
/** Fixed UI-only examples. This class and every value below are excluded from the production APK. */
public final class DashboardFixture {
 public static final String[] STATES={"idle","gps-searching","gps-fixed","recording","paused","finished","disconnected","connected-no-sample"};
 private DashboardFixture(){}
 public static List<RouteGeometry.Point> route(){
  double[][] values={{76,136},{90,111},{104,90},{139,81},{169,57},{206,65},{226,96},{254,117},{246,143},{218,151},{187,136}};List<RouteGeometry.Point> points=new ArrayList<>();
  for(int i=0;i<values.length;i++)points.add(new RouteGeometry.Point(1.3+(190-values[i][1])*.00002,103.8+values[i][0]*.00002,i==0));return List.copyOf(points);
 }
 public static List<HeartRatePlot.Point> heart(){int[] values={113,121,117,130,127,137,133,145,140,151,146,157,151,160,153,164,156,168,160,171,163,169,157,162,151,157,147,152,142,148};List<HeartRatePlot.Point> points=new ArrayList<>();for(int i=0;i<values.length;i++)points.add(new HeartRatePlot.Point(i*2000,values[i],i==0||i==15));return List.copyOf(points);}
 public static void present(DashboardPreviewActivity activity,String name){
  boolean idle=name.equals("idle"),search=name.equals("gps-searching"),paused=name.equals("paused"),finished=name.equals("finished");boolean connected=!idle&&!search&&!finished&&!name.equals("disconnected");boolean fresh=connected&&!name.equals("connected-no-sample");
  var phase=idle?RecordingSession.Phase.IDLE:search?RecordingSession.Phase.ACQUIRING_GPS:paused?RecordingSession.Phase.PAUSED:finished?RecordingSession.Phase.FINISHED:RecordingSession.Phase.RECORDING;
  boolean fix=!idle&&!search&&!paused&&!finished;boolean zero=idle||search||name.equals("gps-fixed");List<RouteGeometry.Point> route=idle||search?List.of():route();if(name.equals("gps-fixed"))route=List.of(route.get(0));var last=route.isEmpty()?null:route.get(route.size()-1);
  Position gps=fix?new Position(Instant.parse("2026-11-08T01:41:00Z"),last.latitude(),last.longitude(),Distance.of(5),null,null,null,Speed.of(1000d/327)):null;
  int state=idle?R.string.p02_idle_detail:search?R.string.p02_acquiring_detail:paused?R.string.p02_paused_detail:finished?R.string.p02_finished_detail:R.string.p02_gps_ready;
  String heart=fresh?activity.getString(R.string.hr_value,148f):activity.getString(R.string.hr_empty_value);
  var data=new DashboardState(phase,false,false,zero?"0.0 m":"5.25 km",zero?"00:00":"28:36",fix?"11.0 km/h":"—",fix?"5:27 /km":"—",heart,activity.getString(connected?fresh?R.string.hr_connected:R.string.p04_heart_wait:R.string.p04_heart_none),connected?"Preview H303":"",fresh?activity.getString(R.string.p02_last_heart,"09:41:00"):activity.getString(R.string.p02_last_heart,"—"),activity.getString(R.string.p02_reconnect_on),activity.getString(state),fix?"1.301080, 103.803740 · ±5 m\n09:41:00":"—",fix?"±5 m":"—",gps,connected,fresh,null);
  activity.present(data,route,fresh?heart():List.of());
 }
}
