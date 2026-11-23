package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.SystemClock;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceFragmentCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.*;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.p0.data.SessionStore;
import de.dennisguse.opentracks.p0.data.WorkoutRepository;
import de.dennisguse.opentracks.p0.recording.*;
import de.dennisguse.opentracks.sensors.driver.*;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** T01–T18: actual API35 Activities/task/OS Back and existing service. Test-only GPS/BLE inputs. */
public class P071NavigationTest {
    @Rule public final GrantPermissionRule permissions=TestUtil.createScanPermissionRule();
    @Rule public final ServiceTestRule serviceRule=new ServiceTestRule();
    private final Context context=ApplicationProvider.getApplicationContext();
    private final Class<?>[] roots={P0Activity.class,P0HistoryActivity.class,P0SensorsActivity.class,P0SettingsActivity.class};
    private final int[] tabs={R.id.p04_nav_run,R.id.p0_all_history,R.id.p04_nav_heart,R.id.p03_settings};
    private ThemeUtils.Family family;
    private String gps,heart,night,rotation,accelerometer;
    private final String[] animations={"window_animation_scale","transition_animation_scale","animator_duration_scale"};
    private final String[] savedAnimations=new String[3];
    private TrackRecordingService core;
    private ActivityScenario<P0Activity> home;
    private void main(Runnable action){if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())action.run();else InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    private boolean root(Activity a){return Arrays.asList(roots).contains(a.getClass());}
    private void waitUntil(String message,BooleanSupplier condition){
        long deadline=SystemClock.uptimeMillis()+15000;
        while(SystemClock.uptimeMillis()<deadline){if(condition.getAsBoolean())return;SystemClock.sleep(50);}
        fail(message);
    }
    private Activity current(){
        AtomicReference<Activity> value=new AtomicReference<>();
        main(()->{for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(root(a)&&a.hasWindowFocus())value.set(a);});
        return value.get();
    }
    private Activity await(int tab){
        AtomicReference<Activity> found=new AtomicReference<>();
        waitUntil("Expected root "+roots[tab],()->{
            // A theme recreation can happen between two separate current()
            // reads. Check the actual focused root and all selected tabs in
            // one main-thread transaction, retaining every assertion.
            main(()->{Activity a=current();if(a==null||a.getClass()!=roots[tab])return;for(int i=0;i<tabs.length;i++)assertEquals(i==tab,a.findViewById(tabs[i]).isSelected());found.set(a);});
            return found.get()!=null;
        });return found.get();
    }
    private void select(int tab){onView(withId(tabs[tab])).perform(click());await(tab);}
    private void uniqueRoots(){main(()->{Map<Class<?>,Integer> counts=new HashMap<>();for(Stage s:Stage.values())if(s!=Stage.DESTROYED)for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(s))if(root(a))counts.merge(a.getClass(),1,Integer::sum);for(int n:counts.values())assertEquals(1,n);});}
    private void osBack()throws Exception{SystemThemeTestUtils.shell("input keyevent 4");InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
    private void reopen(int tab){
        main(()->context.startActivity(new Intent(context,roots[tab]).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP)));
        await(tab);
    }
    private void background(){waitUntil("Root Back must background the task, never reveal another tab",()->{AtomicReference<Boolean> resumed=new AtomicReference<>(false);main(()->{for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(root(a))resumed.set(true);});return !resumed.get();});}
    private String navigationMode(){try{return SystemThemeTestUtils.shell("settings get secure navigation_mode");}catch(Exception error){throw new AssertionError("Cannot read actual OS navigation mode",error);}}
    private void capture(String name)throws Exception{P0UiCapture.save("p071-"+name);}
    private PreferenceFragmentCompat settings(){return (PreferenceFragmentCompat)((P0SettingsActivity)current()).getSupportFragmentManager().findFragmentById(R.id.gardenia_settings_container);}
    private void settingsCategory(String name){Activity a=current();main(()->{((P0SettingsActivity)a).openCategory(name);((P0SettingsActivity)a).getSupportFragmentManager().executePendingTransactions();});}
    @Before public void before()throws Exception{
        family=PreferencesUtils.getGardeniaTheme();night=SystemThemeTestUtils.savedMode();
        gps=PreferencesUtils.getString(R.string.settings_sensor_gps_key,SensorType.INTERNAL.getPreferenceValue());
        heart=PreferencesUtils.getString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
        rotation=SystemThemeTestUtils.shell("settings get system user_rotation");accelerometer=SystemThemeTestUtils.shell("settings get system accelerometer_rotation");
        for(int i=0;i<animations.length;i++){savedAnimations[i]=SystemThemeTestUtils.shell("settings get global "+animations[i]);SystemThemeTestUtils.shell("settings put global "+animations[i]+" 0");}
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,SensorType.NONE.getPreferenceValue());PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
        core=((TrackRecordingService.Binder)serviceRule.bindService(new Intent(context,TrackRecordingService.class))).getService();
        home=ActivityScenario.launch(P0Activity.class);await(0);
    }
    @After public void after()throws Exception{
        main(()->{for(Stage s:Stage.values())if(s!=Stage.DESTROYED)for(Activity a:new ArrayList<>(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(s)))if(root(a))a.finish();if(core!=null){core.cancelGpsAcquisition();core.endCurrentTrack();core.stopIdleSensors();}});
        home.close();serviceRule.unbindService();TestEnvironmentUtils.stopTrackRecordingService();TestEnvironmentUtils.awaitTrackRecordingServiceDestroyed();
        new SessionStore(context).save(RecordingSession.ready());TestEnvironmentUtils.deleteAllTracks();
        PreferencesUtils.setGardeniaTheme(family);PreferencesUtils.setString(R.string.settings_sensor_gps_key,gps);PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,heart);SystemThemeTestUtils.restore(night);
        for(int i=0;i<animations.length;i++)restore("global",animations[i],savedAnimations[i]);restore("system","user_rotation",rotation);restore("system","accelerometer_rotation",accelerometer);
    }
    private void restore(String table,String key,String value)throws Exception{SystemThemeTestUtils.shell("settings "+(value.equals("null")?"delete "+table+" "+key:"put "+table+" "+key+" "+value));}

    @Test public void T01_runToHistory()throws Exception{select(1);uniqueRoots();capture("T01-history");}
    @Test public void T02_historyToSettings()throws Exception{select(1);select(3);uniqueRoots();capture("T02-settings");}
    @Test public void T03_settingsToHeart()throws Exception{select(3);select(2);uniqueRoots();capture("T03-heart");}
    @Test public void T04_fourRootCyclesKeepInstances()throws Exception{
        Map<Integer,Activity> first=new HashMap<>();first.put(0,current());
        for(int round=0;round<4;round++)for(int tab:new int[]{1,3,2,0}){select(tab);if(first.containsKey(tab))assertSame(first.get(tab),current());else first.put(tab,current());uniqueRoots();}
        capture("T04-cycles");
    }
    @Test public void T05_reselectionDoesNotLaunch()throws Exception{
        for(int tab:new int[]{0,1,2,3}){select(tab);Activity a=current();main(()->{for(int n=0;n<20;n++)a.findViewById(tabs[tab]).performClick();});assertSame(a,await(tab));uniqueRoots();}capture("T05-reselect");
    }
    @Test public void T06_rapidSwitchLaunchesOnce()throws Exception{
        Activity source=current();main(()->{for(int n=0;n<20;n++){source.findViewById(tabs[1]).performClick();source.findViewById(tabs[3]).performClick();}});
        await(1);uniqueRoots();select(3);select(0);uniqueRoots();capture("T06-rapid");
    }
    @Test public void T07_systemBackAtEveryRootBackgroundsTask()throws Exception{
        for(int tab:new int[]{1,3,2,0}){select(tab);osBack();background();reopen(tab);assertFalse(core.isRecording());uniqueRoots();}capture("T07-root-restored");
    }
    @Test public void T08_actualApi35EdgeGestureBackgroundsRoot()throws Exception{
        String overlays=SystemThemeTestUtils.shell("cmd overlay list --user 0");
        String enabled=Arrays.stream(overlays.split("\n")).filter(s->s.trim().startsWith("[x]")&&s.contains("com.android.internal.systemui.navbar.")).map(s->s.trim().substring(4).trim()).findFirst().orElse("com.android.internal.systemui.navbar.threebutton");
        try{
            SystemThemeTestUtils.shell("cmd overlay enable-exclusive --user 0 --category com.android.internal.systemui.navbar.gestural");
            // SystemUI applies the overlay asynchronously after the shell command returns.
            waitUntil("SystemUI did not enable actual gestural navigation",()->"2".equals(navigationMode()));
            assertEquals("2",navigationMode());await(0);select(2);Activity a=current();
            AtomicReference<int[]> size=new AtomicReference<>();main(()->size.set(new int[]{a.getWindow().getDecorView().getWidth(),a.getWindow().getDecorView().getHeight()}));
            int[] wh=size.get();SystemThemeTestUtils.shell("input swipe 1 "+wh[1]/2+" "+wh[0]/2+" "+wh[1]/2+" 400");background();reopen(2);capture("T08-gesture-restored");
        }finally{SystemThemeTestUtils.shell("cmd overlay enable-exclusive --user 0 --category "+enabled);String expected=enabled.endsWith(".gestural")?"2":enabled.endsWith(".twobutton")?"1":"0";waitUntil("Original OS navigation mode not restored",()->expected.equals(navigationMode()));}
    }
    @Test public void T09_settingsInternalBackSurvivesRootReturn()throws Exception{
        select(3);for(int round=0;round<2;round++){
            settingsCategory("motion");main(()->assertEquals(1,((P0SettingsActivity)current()).getSupportFragmentManager().getBackStackEntryCount()));
            Activity a=current();main(()->((P0SettingsActivity)a).onSupportNavigateUp());await(3);main(()->assertEquals(0,((P0SettingsActivity)a).getSupportFragmentManager().getBackStackEntryCount()));
            osBack();background();reopen(3);
        }capture("T09-settings-root");
    }
    @Test public void T10_historyDetailThenListThenTaskRoot()throws Exception{
        var id=P05HistoryFixtures.create(context,"Synthetic P071 retained history",true);select(1);
        waitUntil("History fixture not loaded",()->{AtomicReference<Boolean> v=new AtomicReference<>(false);main(()->v.set(((android.widget.ListView)current().findViewById(R.id.p0_history_list)).getAdapter().getCount()==1));return v.get();});
        // Select the actual visible native row rather than using a private ViewModel command.
        androidx.test.espresso.Espresso.onData(org.hamcrest.Matchers.anything()).inAdapterView(withId(R.id.p0_history_list)).atPosition(0).perform(click());
        waitUntil("History detail not shown",()->{AtomicReference<Boolean> v=new AtomicReference<>(false);main(()->v.set(current().findViewById(R.id.p0_workout_pane).isShown()));return v.get();});
        capture("T10-history-detail");osBack();await(1);main(()->assertTrue(current().findViewById(R.id.p0_history_list).isShown()));assertNotNull(new WorkoutRepository(context).find(id));osBack();background();reopen(1);
        Activity history=current();select(3);settingsCategory("history");main(()->settings().findPreference("p05_manage_history").performClick());await(1);assertSame(history,current());uniqueRoots();
        main(()->assertTrue(current().findViewById(R.id.p05_selected_count).isShown()));osBack();await(1);main(()->assertFalse(current().findViewById(R.id.p05_selected_count).isShown()));assertNotNull(new WorkoutRepository(context).find(id));
        select(3);main(()->assertEquals(1,((P0SettingsActivity)current()).getSupportFragmentManager().getBackStackEntryCount()));osBack();await(3);main(()->assertEquals(0,((P0SettingsActivity)current()).getSupportFragmentManager().getBackStackEntryCount()));
    }
    @Test public void T11_heartDeviceDialogReturnsToHeart()throws Exception{
        select(2);Activity a=current();main(()->{var f=(P0SensorsFragment)((P0SensorsActivity)a).getSupportFragmentManager().findFragmentById(R.id.p0_sensor_container);f.onDisplayPreferenceDialog(f.findPreference(context.getString(R.string.settings_sensor_bluetooth_heart_rate_key)));((P0SensorsActivity)a).getSupportFragmentManager().executePendingTransactions();});
        capture("T11-device-dialog");osBack();await(2);main(()->assertNull(((P0SensorsActivity)a).getSupportFragmentManager().findFragmentByTag("p0-heart-rate-scan")));assertFalse(core.isRecording());
        select(0);onView(withId(R.id.p0_devices)).perform(click());await(2);assertSame(a,current());uniqueRoots();
        select(3);settingsCategory("devices");main(()->settings().findPreference("p05_manage_devices").performClick());await(2);assertSame(a,current());uniqueRoots();osBack();background();reopen(3);
        main(()->assertEquals(1,((P0SettingsActivity)current()).getSupportFragmentManager().getBackStackEntryCount()));osBack();await(3);main(()->assertEquals(0,((P0SettingsActivity)current()).getSupportFragmentManager().getBackStackEntryCount()));
    }
    @Test public void T12_bottomSheetDismissedBeforeTaskBack()throws Exception{
        var id=P05HistoryFixtures.create(context,"Synthetic P071 sheet",false);select(1);Activity a=current();
        main(()->{P05BatchDialog.create(new long[]{id.id()},true).show(((P0HistoryActivity)a).getSupportFragmentManager(),"p071-sheet");((P0HistoryActivity)a).getSupportFragmentManager().executePendingTransactions();});
        capture("T12-delete-sheet");osBack();await(1);main(()->assertNull(((P0HistoryActivity)a).getSupportFragmentManager().findFragmentByTag("p071-sheet")));assertNotNull(new WorkoutRepository(context).find(id));
    }
    @Test public void T13_activityRecreationRetainsInternalBack()throws Exception{
        select(3);settingsCategory("motion");Activity old=current();main(old::recreate);waitUntil("Settings not recreated",()->current()!=null&&current()!=old&&current() instanceof P0SettingsActivity);await(3);
        main(()->assertEquals(1,((P0SettingsActivity)current()).getSupportFragmentManager().getBackStackEntryCount()));osBack();await(3);capture("T13-recreated-settings");osBack();background();reopen(3);
    }
    @Test public void T14_actualRotationRetainsSelectedRoot()throws Exception{
        select(2);AtomicReference<Integer> oldOrientation=new AtomicReference<>();main(()->oldOrientation.set(current().getResources().getConfiguration().orientation));
        SystemThemeTestUtils.shell("settings put system accelerometer_rotation 0");SystemThemeTestUtils.shell("settings put system user_rotation "+(oldOrientation.get()==android.content.res.Configuration.ORIENTATION_PORTRAIT?1:0));
        waitUntil("Configuration did not rotate",()->{Activity a=current();return a!=null&&a.getResources().getConfiguration().orientation!=oldOrientation.get();});await(2);uniqueRoots();capture("T14-rotated-heart");
    }
    @Test public void T15_backgroundAndForegroundKeepDestination()throws Exception{
        select(3);settingsCategory("motion");SystemThemeTestUtils.shell("input keyevent 3");background();reopen(3);main(()->assertEquals(1,((P0SettingsActivity)current()).getSupportFragmentManager().getBackStackEntryCount()));osBack();await(3);capture("T15-resumed-settings");
    }
    private long recording(){main(()->{core.beginGpsAcquisition();P0GpsPreparationTest.fixture(core);});P0GpsPreparationTest.ready(core);assertTrue(core.isRecording());return core.getRecordingStatus().trackId().id();}
    @Test public void T16_recordingContinuesAcrossTabsAndRootBack()throws Exception{
        long id=recording();Object creator=core.getTrackPointCreator();for(int tab:new int[]{1,3,2,0}){select(tab);assertTrue(core.isRecording());assertEquals(id,core.getRecordingStatus().trackId().id());assertSame(creator,core.getTrackPointCreator());}
        osBack();background();assertTrue(core.isRecording());assertSame(creator,core.getTrackPointCreator());reopen(0);capture("T16-recording-restored");
        for(int round=0;round<2;round++){
            onView(withId(R.id.p0_pause)).perform(click());waitUntil("Workout did not pause",()->!core.isRecording());
            for(int tab:new int[]{1,3,2,0}){select(tab);assertFalse(core.isRecording());assertEquals(Long.valueOf(id),new SessionStore(context).recover().trackId());}
            main(()->assertEquals(RecordingSession.Phase.PAUSED,new ViewModelProvider((P0Activity)current()).get(P0RecordingViewModel.class).snapshot().getValue().session().phase()));
            onView(withId(R.id.p0_resume)).perform(click());waitUntil("Original paused workout did not resume",core::isRecording);assertEquals(id,core.getRecordingStatus().trackId().id());
        }
    }
    @Test public void T17_gpsAndBleInputsPersistWhileSwitching()throws Exception{
        long id=recording();Object creator=core.getTrackPointCreator();Object handler=core.getTrackPointCreator().getSensorManager().getGpsHandler();
        main(()->{var h=core.getTrackPointCreator().getSensorManager().getHeartRateHandler();h.onConnected("AA:BB:CC:DD:EE:FF","Synthetic P071 strap");h.onConnectionStateChanged(BleConnectionSession.State.CONNECTED);});
        String connection=core.getHeartRatePreview().status().state().toString();
        for(int tab:new int[]{1,3,2,0}){
            select(tab);main(()->{var previous=core.getGpsPreview();Location point=new Location("gps");point.setLatitude(previous.latitude()+.00003);point.setLongitude(previous.longitude());point.setAccuracy(2);point.setSpeed(3);point.setTime(System.currentTimeMillis());point.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());core.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(point);
                var h=core.getTrackPointCreator().getSensorManager().getHeartRateHandler();var characteristic=new android.bluetooth.BluetoothGattCharacteristic(HeartRateBluetooth.HEARTRATE.measurementUUID(),0,0);h.onDataReceived(new HeartRateBluetooth().parsePayload(HeartRateBluetooth.HEARTRATE,"Synthetic P071 strap",characteristic,new byte[]{6,(byte)(140+tab)}));});
            assertTrue(core.isRecording());assertEquals(id,core.getRecordingStatus().trackId().id());assertSame(creator,core.getTrackPointCreator());assertSame(handler,core.getTrackPointCreator().getSensorManager().getGpsHandler());assertEquals(connection,core.getHeartRatePreview().status().state().toString());assertEquals(140+tab,core.getHeartRatePreview().value().getBPM(),0);
        }capture("T17-synthetic-gps-ble");
    }
    @Test public void T18_eightPalettesKeepNavigationAndBack()throws Exception{
        for(var f:ThemeUtils.Family.values())for(boolean dark:new boolean[]{false,true}){
            PreferencesUtils.setGardeniaTheme(f);SystemThemeTestUtils.night(dark);reopen(0);
            waitUntil("Palette not applied",()->{Activity a=current();return a!=null&&ThemeUtils.isNight(a)==dark;});
            for(int tab:new int[]{1,2,3,0})select(tab);uniqueRoots();capture("T18-"+f.id+(dark?"-dark":"-light"));osBack();background();reopen(0);
        }
    }
}
