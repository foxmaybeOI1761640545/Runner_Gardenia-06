package de.dennisguse.opentracks.p0.map;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.List;
import org.junit.Test;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.P0UiCapture;

/** Full suite logical round trip; dedicated runner additionally force-stops between phases. */
public class P06MapPersistenceTest {
    @Test public void retainedPrivateMapRendersAfterIndependentProcessRestart()throws Exception{
        Context context=ApplicationProvider.getApplicationContext();var repository=new OfflineMapRepository(context);
        String phase=InstrumentationRegistry.getArguments().getString("p06MapPhase","both");assertTrue(List.of("both","write","read").contains(phase));
        var prefs=context.getSharedPreferences("p06-map-process-probe",Context.MODE_PRIVATE);
        String source=InstrumentationRegistry.getArguments().getString("buildCommit");
        if(!phase.equals("read")){
            for(var e:repository.read().entries())repository.delete(e.id());
            try(var input=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("p06/gardenia-synthetic.map")){
                var state=repository.importMap(input,"Synthetic persistent map.map");assertNotNull(state.current());
                assertTrue(prefs.edit().putString("source",source).putString("id",state.selected()).commit());
            }
        }
        if(!phase.equals("write")){
            var state=repository.read();assertEquals(source,prefs.getString("source",null));assertEquals(prefs.getString("id",null),state.selected());assertEquals(751,state.current().bytes());
            try(var screen=ActivityScenario.launch(P06MapActivity.class)){
                long end=SystemClock.uptimeMillis()+20000;boolean[] ready={false};
                do{screen.onActivity(a->{var frame=(android.widget.FrameLayout)a.findViewById(R.id.p06_map_container);ready[0]=((OfflineMapView)frame.getChildAt(0)).renderedTileCount()>0;});if(ready[0])break;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<end);
                assertTrue("New process must actually render the private map without importing again",ready[0]);
                if(!phase.equals("both"))P0UiCapture.save("p06-new-process");
            }
            for(var e:repository.read().entries())repository.delete(e.id());
        }
        if(!phase.equals("both")){
            android.os.Bundle status=new android.os.Bundle();status.putString("p06MapPid",Integer.toString(android.os.Process.myPid()));status.putString("p06MapPhase",phase);InstrumentationRegistry.getInstrumentation().sendStatus(0,status);
        }
    }
}
