package de.dennisguse.opentracks.services;

import static org.junit.Assert.*;
import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;
import org.junit.Rule;
import org.junit.Test;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.data.WorkoutRepository;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.PermissionRequester;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** Also run with actual GPS denial in a separately restarted externally configured process. */
public class P07RecoveryPermissionTest {
    @Rule public final ServiceTestRule rule=new ServiceTestRule();
    @Rule public final GrantPermissionRule bluetooth=GrantPermissionRule.grant(Manifest.permission.BLUETOOTH_CONNECT);
    @Test public void persistedP0SessionNeedsActualGpsPermissionEvenWhenBluetoothIsGranted() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();
        String old=PreferencesUtils.getString(R.string.settings_sensor_gps_key,"INTERNAL");PreferencesUtils.setString(R.string.settings_sensor_gps_key,"NONE");
        var service=((TrackRecordingService.Binder)rule.bindService(new Intent(context,TrackRecordingService.class))).getService();
        boolean granted=PermissionRequester.GPS.hasPermission(context);
        assertEquals(android.content.pm.PackageManager.PERMISSION_GRANTED,context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT));
        boolean oldEnabled=de.dennisguse.opentracks.p0.ble.HeartRateCommands.connectionEnabled(context);
        String expected=InstrumentationRegistry.getArguments().getString("expectedRecoveryGpsPermission","current");
        assertTrue(expected.equals("current")||expected.equals("denied")||expected.equals("granted"));
        if(!expected.equals("current"))assertEquals(expected.equals("granted"),granted);
        try{
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
                var id=service.startNewTrack();assertNotNull(id);service.endCurrentTrack();
                TrackIdStorage.set(context,id,true); // Existing saved workout + its service recovery journal.
                int result=service.onStartCommand(null,0,1);
                assertEquals(granted?Service.START_STICKY:Service.START_NOT_STICKY,result);
                assertEquals(granted,service.isRecording());assertNotNull(new WorkoutRepository(context).find(id));
                if(granted){assertEquals(id,service.getRecordingStatus().trackId());service.endCurrentTrack();}
                else {assertNull(TrackRecordingService.activeTrackId(context));assertFalse(service.areSensorsStarted());}
                assertEquals("NONE",PreferencesUtils.getString(R.string.settings_sensor_gps_key,""));
                assertEquals(1,new WorkoutRepository(context).list().size());
            });
            Bundle status=new Bundle();status.putString("p07RecoveryActualGpsPermission",granted?"granted":"denied");
            status.putInt("p07RecoveryPid",android.os.Process.myPid());InstrumentationRegistry.getInstrumentation().sendStatus(0,status);
        }finally{
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{service.endCurrentTrack();service.stopIdleSensors();});
            PreferencesUtils.setString(R.string.settings_sensor_gps_key,old);
            de.dennisguse.opentracks.p0.ble.HeartRateCommands.setConnectionEnabled(context,oldEnabled);
        }
    }
}
