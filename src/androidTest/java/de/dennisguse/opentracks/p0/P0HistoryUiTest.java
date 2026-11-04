package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.action.ViewActions.*;
import android.content.Context;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import org.junit.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.*;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.app.P0HistoryActivity;
import de.dennisguse.opentracks.p0.data.*;
import de.dennisguse.opentracks.p0.recording.RecordingSession;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

public class P0HistoryUiTest {
    private final Context context=ApplicationProvider.getApplicationContext();
    private Track.Id id;
    @Before public void before() {
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());
        id=new ContentProviderUtils(context).insertTrack(new Track(null,UUID.randomUUID(),"Synthetic history workout","","",ActivityType.RUNNING,ZoneOffset.UTC,Statistics.DEFAULT));
        HeartRateRepository raw=new HeartRateRepository(context);
        for(int bpm:new int[]{70,90})raw.insert(id,new HeartRate(bpm,new HeartRateSample(Instant.EPOCH.plusSeconds(bpm),"AA:BB:CC:DD:EE:FF","Synthetic strap",true,12,List.of(1024))));
    }
    @After public void after(){new SessionStore(context).save(RecordingSession.ready());}
    private void detail(ActivityScenario<P0HistoryActivity> screen) {
        long end=SystemClock.uptimeMillis()+10000;AtomicReference<String> text=new AtomicReference<>("");
        do{screen.onActivity(activity->text.set(((TextView)activity.findViewById(R.id.p0_workout_details)).getText().toString()));if(text.get().contains("Synthetic history workout"))return;SystemClock.sleep(50);}while(SystemClock.uptimeMillis()<end);
        fail("History detail did not load: "+text.get());
    }
    @Test public void historyOpensExactRawStatisticsAndDeletesOnlyAfterConfirmation() throws Exception {
        try(ActivityScenario<P0HistoryActivity> screen=ActivityScenario.launch(P0HistoryActivity.class)) {
            long end=SystemClock.uptimeMillis()+10000;java.util.concurrent.atomic.AtomicBoolean ready=new java.util.concurrent.atomic.AtomicBoolean();
            do{screen.onActivity(activity->ready.set(((android.widget.ListView)activity.findViewById(R.id.p0_history_list)).getAdapter().getCount()==1));if(ready.get())break;SystemClock.sleep(50);}while(SystemClock.uptimeMillis()<end);
            assertTrue(ready.get());P0UiCapture.save("p0-local-history-synthetic");onView(withText("Synthetic history workout")).perform(click());detail(screen);
            screen.onActivity(activity->{String text=((TextView)activity.findViewById(R.id.p0_workout_details)).getText().toString();assertTrue(text.contains(context.getString(R.string.p0_workout_heart,context.getString(R.string.p0_hr_raw),2L,70d,80d,90d)));});
            P0UiCapture.save("p0-workout-detail-synthetic");
            onView(withId(R.id.p0_delete_workout)).perform(scrollTo(),click());assertNotNull(new WorkoutRepository(context).find(id));
            onView(withId(android.R.id.button1)).perform(click());
            end=SystemClock.uptimeMillis()+10000;
            while(new WorkoutRepository(context).find(id)!=null&&SystemClock.uptimeMillis()<end)SystemClock.sleep(50);
            assertNull(new WorkoutRepository(context).find(id));assertTrue(new HeartRateRepository(context).read(id).isEmpty());
        }
    }
    @Test public void pausedWorkoutDetailCannotDeleteProtectedData() {
        new SessionStore(context).save(RecordingSession.ready().begin(id.id()).pause());
        try(ActivityScenario<P0HistoryActivity> screen=ActivityScenario.launch(P0HistoryActivity.intent(context,id.id()))) {
            detail(screen);screen.onActivity(activity->{assertFalse(activity.findViewById(R.id.p0_delete_workout).isEnabled());assertEquals(android.view.View.VISIBLE,activity.findViewById(R.id.p0_workout_protected).getVisibility());});
            screen.onActivity(activity->{assertFalse(activity.findViewById(R.id.p0_save_gpx).isEnabled());assertFalse(activity.findViewById(R.id.p0_share_gpx).isEnabled());});
            assertNotNull(new WorkoutRepository(context).find(id));
        }
    }
    @Test public void exportActionsUseAndroidDocumentAndReadOnlyShareContracts() throws Exception {
        var instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
        AtomicReference<android.content.Intent> document=new AtomicReference<>();AtomicReference<android.content.Intent> share=new AtomicReference<>();
        var monitor=new android.app.Instrumentation.ActivityMonitor() {
            @Override public android.app.Instrumentation.ActivityResult onStartActivity(android.content.Intent intent) {
                if(android.content.Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction()))document.set(new android.content.Intent(intent));
                else if(android.content.Intent.ACTION_CHOOSER.equals(intent.getAction()))share.set(intent.getParcelableExtra(android.content.Intent.EXTRA_INTENT));
                else return null;
                return new android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED,null);
            }
        };
        instrumentation.addMonitor(monitor);
        try(ActivityScenario<P0HistoryActivity> screen=ActivityScenario.launch(P0HistoryActivity.intent(context,id.id()))) {
            detail(screen);P0UiCapture.save("p0-gpx-export-actions-synthetic");
            onView(withId(R.id.p0_save_gpx)).perform(scrollTo(),click());
            assertNotNull(document.get());assertEquals("application/gpx+xml",document.get().getType());assertTrue(document.get().hasCategory(android.content.Intent.CATEGORY_OPENABLE));
            onView(withId(R.id.p0_share_gpx)).perform(scrollTo(),click());
            long end=SystemClock.uptimeMillis()+10000;
            while(share.get()==null&&SystemClock.uptimeMillis()<end)SystemClock.sleep(50);
            assertNotNull(share.get());assertEquals(android.content.Intent.ACTION_SEND,share.get().getAction());assertEquals(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,share.get().getFlags());
            try(var input=context.getContentResolver().openInputStream(share.get().getClipData().getItemAt(0).getUri())) {
                assertNotNull(input);String xml=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                assertTrue(xml.contains("urn:runner-gardenia:gpx:1"));assertTrue(xml.contains("Synthetic history workout"));
            }
        }finally{instrumentation.removeMonitor(monitor);}
    }
}
