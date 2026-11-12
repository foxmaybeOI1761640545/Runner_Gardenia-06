package de.dennisguse.opentracks.p0.map;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.lifecycle.ViewModelProvider;
import org.junit.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

public class P06RouteReadTest {
    private final Context context=ApplicationProvider.getApplicationContext();
    private ContentProviderUtils database;
    private Track.Id id;
    @Before public void before(){TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();database=new ContentProviderUtils(context);id=database.insertTrack(new Track(null,UUID.randomUUID(),"Synthetic map route","","",ActivityType.RUNNING,ZoneOffset.UTC,Statistics.DEFAULT));}
    private void point(int second,Double latitude,TrackPoint.Type type){Position p=new Position(Instant.EPOCH.plusSeconds(second),latitude,latitude==null?null:103.8,latitude==null?null:Distance.of(2),null,null,null,null);database.insertTrackPoint(new TrackPoint(null,type,p,null,latitude==null?HeartRate.of(90):null,null,null,null,null),id);}
    @Test public void automaticStartCoordinatesRemainAndBleSamplesCannotSplitGpsLine(){
        point(1,1.3,TrackPoint.Type.TRACKPOINT);point(2,null,TrackPoint.Type.TRACKPOINT);point(3,1.30001,TrackPoint.Type.TRACKPOINT);
        point(4,1.301,TrackPoint.Type.SEGMENT_START_AUTOMATIC);point(5,1.30101,TrackPoint.Type.TRACKPOINT);point(6,null,TrackPoint.Type.SEGMENT_END_MANUAL);
        point(7,null,TrackPoint.Type.SEGMENT_START_MANUAL);point(8,1.302,TrackPoint.Type.TRACKPOINT);
        AtomicReference<RoutePreviewModel.Result> result=new AtomicReference<>();
        try(var screen=ActivityScenario.launch(P06MapActivity.intent(context,id.id()))){
            long end=android.os.SystemClock.uptimeMillis()+10000;
            do{screen.onActivity(a->result.set(new ViewModelProvider(a).get(RoutePreviewModel.class).route().getValue()));if(result.get().points().size()==5)break;android.os.SystemClock.sleep(100);}while(android.os.SystemClock.uptimeMillis()<end);
            var points=result.get().points();assertEquals(5,points.size());assertEquals(List.of(true,false,true,false,true),points.stream().map(RouteGeometry.Point::breakBefore).toList());assertEquals(1.301,points.get(2).latitude(),.000001);assertFalse(result.get().failed());
        }
    }
    @Test public void historyMenuOpensTheSelectedWorkoutMap(){
        point(1,1.3,TrackPoint.Type.TRACKPOINT);
        androidx.test.espresso.intent.Intents.init();
        try(var screen=ActivityScenario.launch(P0HistoryActivity.intent(context,id.id()))){
            android.os.SystemClock.sleep(600);
            androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu(context);
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(de.dennisguse.opentracks.R.string.p06_offline_map)).perform(androidx.test.espresso.action.ViewActions.click());
            androidx.test.espresso.intent.Intents.intended(org.hamcrest.Matchers.allOf(androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent(P06MapActivity.class.getName()),androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra("p06.track",id.id())));
            androidx.test.espresso.Espresso.pressBack();
        }finally{androidx.test.espresso.intent.Intents.release();}
    }
}
