package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;
import org.junit.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.p0.recording.RecordingPermissions;
import de.dennisguse.opentracks.sensors.driver.SensorType;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

public class P0OfflinePolicyTest {
    @Rule public final ServiceTestRule rule=ServiceTestRule.withTimeout(10,TimeUnit.SECONDS);
    @Rule public final GrantPermissionRule permissions=GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.POST_NOTIFICATIONS);
    private final Context context=ApplicationProvider.getApplicationContext();
    private final Map<Integer,String> saved=new HashMap<>();
    private final int[] optional={R.string.settings_sensor_bluetooth_pressure_key,R.string.settings_sensor_bluetooth_temperature_key,R.string.settings_sensor_bluetooth_cycling_power_key,R.string.settings_sensor_bluetooth_cycling_cadence_key,R.string.settings_sensor_bluetooth_cycling_speed_key,R.string.settings_sensor_bluetooth_running_speed_and_cadence_key};
    private TrackRecordingService service;
    @Before public void before() throws Exception {
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();
        for(int key:optional){saved.put(key,PreferencesUtils.getString(key,SensorType.NONE.getPreferenceValue()));PreferencesUtils.setString(key,"AA:BB:CC:DD:EE:FF");}
        for(int key:new int[]{R.string.settings_sensor_gps_key,R.string.settings_sensor_bluetooth_heart_rate_key})saved.put(key,PreferencesUtils.getString(key,SensorType.NONE.getPreferenceValue()));
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,SensorType.INTERNAL.getPreferenceValue());PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
        service=((TrackRecordingService.Binder)rule.bindService(new Intent(context,TrackRecordingService.class))).getService();
    }
    @After public void after() {
        if(service!=null)InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{service.endCurrentTrack();service.stopIdleSensors();});
        saved.forEach(PreferencesUtils::setString);
    }
    @Test public void applicationCannotAccessInternetOrParticipateInSystemBackups() throws Exception {
        var info=context.getPackageManager().getPackageInfo(context.getPackageName(),PackageManager.GET_PERMISSIONS);
        assertFalse(Arrays.asList(info.requestedPermissions).contains(Manifest.permission.INTERNET));assertEquals(0,context.getApplicationInfo().flags&ApplicationInfo.FLAG_ALLOW_BACKUP);
        Set<String> domains=Set.of("root","file","database","sharedpref","external","device_root","device_file","device_database","device_sharedpref");
        try(var parser=context.getResources().getXml(R.xml.p0_data_extraction_rules)) {
            Map<String,Set<String>> exclusions=new HashMap<>();String mode=null;
            while(parser.next()!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if(parser.getEventType()!=org.xmlpull.v1.XmlPullParser.START_TAG)continue;
                if(parser.getName().equals("include"))fail("P0 must not opt data into system backup");
                if(parser.getName().equals("cloud-backup")||parser.getName().equals("device-transfer")){mode=parser.getName();exclusions.put(mode,new HashSet<>());}
                if(parser.getName().equals("exclude")){assertEquals(".",parser.getAttributeValue(null,"path"));exclusions.get(mode).add(parser.getAttributeValue(null,"domain"));}
            }
            assertEquals(domains,exclusions.get("cloud-backup"));assertEquals(domains,exclusions.get("device-transfer"));
        }
    }
    @Test public void idlePreviewAndPreferenceChangesNeverStartGpsOrOtherSensors() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            service.tryStartSensors();assertTrue(service.areSensorsStarted());
            var manager=service.getTrackPointCreator().getSensorManager();assertNull(manager.getGpsHandler().driver);assertFalse(manager.hasNonP0Driver());
            manager.onSharedPreferenceChanged(null,null);assertNull(manager.getGpsHandler().driver);assertFalse(manager.hasNonP0Driver());
            assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,RecordingPermissions.foregroundTypes(context,false));
        });
    }
    @Test public void realWorkoutReopensOnlyGpsAndStopsItAfterFinish() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            service.tryStartSensors();var manager=service.getTrackPointCreator().getSensorManager();assertNull(manager.getGpsHandler().driver);
            assertNotNull(service.startNewTrack());assertNotNull(manager.getGpsHandler().driver);assertFalse(manager.hasNonP0Driver());
            service.endCurrentTrack();assertFalse(service.areSensorsStarted());assertFalse(manager.hasNonP0Driver());
        });
    }
    @Test public void deniedBluetoothAndNotificationsDoNotDisableGpsRecording() {
        Context denied=new ContextWrapper(context) {
            private boolean denied(String permission){return permission.contains("BLUETOOTH")||permission.equals(Manifest.permission.POST_NOTIFICATIONS);}
            @Override public int checkSelfPermission(String permission){return denied(permission)?PackageManager.PERMISSION_DENIED:super.checkSelfPermission(permission);}
            @Override public int checkPermission(String permission,int pid,int uid){return denied(permission)?PackageManager.PERMISSION_DENIED:super.checkPermission(permission,pid,uid);}
        };
        assertTrue(RecordingPermissions.hasAny(denied));assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,RecordingPermissions.foregroundTypes(denied,true));assertEquals(0,RecordingPermissions.foregroundTypes(denied,false));
    }
    @Test public void missingRecoveryTrackStopsWithoutAnOrphanForegroundService() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            var id=service.startNewTrack();assertNotNull(id);new ContentProviderUtils(context).deleteTrack(context,id);
            service.onDestroy();service.onCreate();
            assertEquals(android.app.Service.START_NOT_STICKY,service.onStartCommand(null,0,77));assertFalse(service.isRecording());assertNull(TrackRecordingService.activeTrackId(context));
        });
    }
}
