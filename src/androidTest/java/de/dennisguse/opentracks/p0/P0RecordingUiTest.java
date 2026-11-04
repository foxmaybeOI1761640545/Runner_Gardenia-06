package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.*;
import android.Manifest;
import android.content.Context;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import org.junit.*;
import java.util.concurrent.atomic.AtomicReference;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.app.P0Activity;
import de.dennisguse.opentracks.p0.data.SessionStore;
import de.dennisguse.opentracks.p0.recording.*;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

public class P0RecordingUiTest {
    @Rule public final GrantPermissionRule permissions=GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.POST_NOTIFICATIONS);
    private final Context context=ApplicationProvider.getApplicationContext();
    private String sampling,distance,activityType;
    @Before public void before() {
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());
        sampling=PreferencesUtils.getString(R.string.min_sampling_interval_key,"0");distance=PreferencesUtils.getString(R.string.recording_distance_interval_key,"5");activityType=PreferencesUtils.getDefaultActivityTypeLocalized();
    }
    @After public void after() {
        TestEnvironmentUtils.stopTrackRecordingService();new SessionStore(context).save(RecordingSession.ready());
        PreferencesUtils.setString(R.string.min_sampling_interval_key,sampling);PreferencesUtils.setString(R.string.recording_distance_interval_key,distance);PreferencesUtils.setDefaultActivityLocalized(activityType);
    }
    private void phase(ActivityScenario<P0Activity> scenario,int expected) {
        long until=SystemClock.uptimeMillis()+10000;String label=context.getString(expected);AtomicReference<String> actual=new AtomicReference<>();
        do {scenario.onActivity(a->actual.set(((TextView)a.findViewById(R.id.p0_recording_state)).getText().toString()));if(label.equals(actual.get()))return;SystemClock.sleep(50);}while(SystemClock.uptimeMillis()<until);
        assertEquals(label,actual.get());
    }
    @Test public void cyclingRecordingSurvivesRecreationAndBackgroundThenPausesResumesAndFinishes() {
        try(ActivityScenario<P0Activity> scenario=ActivityScenario.launch(P0Activity.class)) {
            onView(withId(R.id.p0_cycling)).perform(click());onView(withId(R.id.p0_start)).perform(scrollTo(),click());phase(scenario,R.string.p0_recording);
            AtomicReference<Long> id=new AtomicReference<>();scenario.onActivity(a->id.set(new ViewModelProvider(a).get(P0RecordingViewModel.class).snapshot().getValue().session().trackId()));assertNotNull(id.get());
            scenario.recreate();phase(scenario,R.string.p0_recording);
            scenario.moveToState(Lifecycle.State.CREATED);SystemClock.sleep(300);scenario.moveToState(Lifecycle.State.RESUMED);phase(scenario,R.string.p0_recording);
            scenario.onActivity(a->assertEquals(id.get(),new ViewModelProvider(a).get(P0RecordingViewModel.class).snapshot().getValue().session().trackId()));
            onView(withId(R.id.p0_pause)).perform(scrollTo(),click());phase(scenario,R.string.p0_paused);
            onView(withId(R.id.p0_resume)).perform(scrollTo(),click());phase(scenario,R.string.p0_recording);
            onView(withId(R.id.p0_finish)).perform(scrollTo(),click());phase(scenario,R.string.p0_finished);
            assertEquals(ActivityType.CYCLING,new ContentProviderUtils(context).getTrack(new Track.Id(id.get())).activityType());
        }
    }
}
