package de.dennisguse.opentracks.p0.map;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;
import org.junit.Rule;
import org.junit.Test;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.P0UiCapture;
import de.dennisguse.opentracks.p0.app.P0Activity;
import de.dennisguse.opentracks.p0.ble.HeartRateCommands;
import de.dennisguse.opentracks.p0.data.WorkoutRepository;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.PermissionRequester;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** Runs normally and in separate externally permission-configured processes; never self-grants GPS. */
public class P07PermissionStateTest {
    @Rule public final ServiceTestRule rule=new ServiceTestRule();
    @Test public void actualPermissionControlsForegroundPreviewWithoutCreatingWorkout()throws Exception{
        Context context=ApplicationProvider.getApplicationContext();
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();
        boolean granted=PermissionRequester.GPS.hasPermission(context);
        String expected=InstrumentationRegistry.getArguments().getString("expectedGpsPermission","current");
        assertTrue(expected.equals("current")||expected.equals("granted")||expected.equals("denied"));
        if(!expected.equals("current"))assertEquals("The external process must really have the requested permission state",expected.equals("granted"),granted);
        String heart=PreferencesUtils.getString(R.string.settings_sensor_bluetooth_heart_rate_key,"NONE");
        boolean enabled=HeartRateCommands.connectionEnabled(context);
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,"NONE");HeartRateCommands.setConnectionEnabled(context,false);
        var service=((TrackRecordingService.Binder)rule.bindService(new Intent(context,TrackRecordingService.class))).getService();
        try{
            try(var screen=ActivityScenario.launch(P0Activity.class)){
                long end=SystemClock.uptimeMillis()+5000;boolean[] ready={false};
                do{screen.onActivity(a->ready[0]=service.hasForegroundGpsRequest()==granted&&service.areSensorsStarted()==granted);if(ready[0])break;SystemClock.sleep(25);}while(SystemClock.uptimeMillis()<end);
                assertTrue("Preview follows the real OS permission state",ready[0]);
                screen.onActivity(a->{assertEquals(0,service.mapGpsOwnerCount());assertFalse(service.isRecording());assertFalse(service.isAcquiringGps());
                    if(!granted){assertFalse(service.hasFreshGpsPreview());assertNull(service.getGpsPreview());}
                });
                assertTrue(new WorkoutRepository(context).list().isEmpty());assertNull(TrackRecordingService.activeTrackId(context));
                P0UiCapture.save("p07-permission-"+(granted?"granted":"denied"));
            }
            assertFalse(service.hasForegroundGpsRequest());assertFalse(service.areSensorsStarted());
            Bundle status=new Bundle();status.putString("p07ActualGpsPermission",granted?"granted":"denied");status.putInt("p07PermissionPid",android.os.Process.myPid());
            InstrumentationRegistry.getInstrumentation().sendStatus(0,status);
        }finally{
            PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,heart);HeartRateCommands.setConnectionEnabled(context,enabled);
        }
    }
}
