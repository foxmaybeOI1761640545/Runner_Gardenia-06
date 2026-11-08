package de.dennisguse.opentracks.p0;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;
import android.app.Activity;
import android.content.*;
import android.location.Location;
import android.os.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceFragmentCompat;
import androidx.test.core.app.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.*;
import androidx.test.runner.lifecycle.*;
import org.junit.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.*;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.p0.recording.*;
import de.dennisguse.opentracks.p0.data.SessionStore;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.sensors.driver.*;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** Real destinations + real existing service/SQLite route; synthetic GPS/SIG inputs only. */
public class DashboardNavigationTest {
 @Rule public final GrantPermissionRule permissions=TestUtil.createScanPermissionRule();@Rule public final ServiceTestRule serviceRule=new ServiceTestRule();
 private final Context context=ApplicationProvider.getApplicationContext();private TrackRecordingService core;private ThemeUtils.Family family;private String night,gps,heart;private final String[] scales={"window_animation_scale","transition_animation_scale","animator_duration_scale"};private final String[] oldScales=new String[3];
 private void main(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
 @Before public void before()throws Exception{
  family=PreferencesUtils.getGardeniaTheme();night=SystemThemeTestUtils.savedMode();gps=PreferencesUtils.getString(R.string.settings_sensor_gps_key,SensorType.INTERNAL.getPreferenceValue());heart=PreferencesUtils.getString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
  for(int i=0;i<scales.length;i++){oldScales[i]=SystemThemeTestUtils.shell("settings get global "+scales[i]);SystemThemeTestUtils.shell("settings put global "+scales[i]+" 0");}
  TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());PreferencesUtils.setGardeniaTheme(ThemeUtils.Family.EXPLORER);PreferencesUtils.setString(R.string.settings_sensor_gps_key,SensorType.NONE.getPreferenceValue());PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
  core=((TrackRecordingService.Binder)serviceRule.bindService(new Intent(context,TrackRecordingService.class))).getService();
 }
 @After public void after()throws Exception{
  main(()->{Set<Activity> all=new HashSet<>();for(Stage stage:Stage.values())if(stage!=Stage.DESTROYED)all.addAll(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage));for(Activity a:all)if(a instanceof P0Activity||a instanceof P0HistoryActivity||a instanceof P0SensorsActivity||a instanceof P0SettingsActivity)a.finish();if(core!=null){core.cancelGpsAcquisition();core.endCurrentTrack();core.stopIdleSensors();}});
  TestEnvironmentUtils.stopTrackRecordingService();new SessionStore(context).save(RecordingSession.ready());PreferencesUtils.setGardeniaTheme(family);PreferencesUtils.setString(R.string.settings_sensor_gps_key,gps);PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,heart);SystemThemeTestUtils.restore(night);
  for(int i=0;i<scales.length;i++)SystemThemeTestUtils.shell(oldScales[i].equals("null")?"settings delete global "+scales[i]:"settings put global "+scales[i]+" "+oldScales[i]);
 }
 private Activity await(Class<? extends Activity> type,java.util.function.Predicate<Activity> condition){AtomicReference<Activity> result=new AtomicReference<>();long deadline=SystemClock.uptimeMillis()+15000;do{main(()->{for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(type.isInstance(a)&&a.hasWindowFocus()&&condition.test(a))result.set(a);});if(result.get()!=null)return result.get();SystemClock.sleep(50);}while(SystemClock.uptimeMillis()<deadline);throw new AssertionError("Destination/state not restored: "+type.getSimpleName());}
 @Test public void bottomNavigationAndSystemChangeKeepRecordingConnectionAndRealRoute()throws Exception{
  try(ActivityScenario<P0Activity> home=ActivityScenario.launch(P0Activity.class)){
   AtomicReference<P0RecordingViewModel> retained=new AtomicReference<>();home.onActivity(a->retained.set(new ViewModelProvider(a).get(P0RecordingViewModel.class)));
   main(()->{core.beginGpsAcquisition();P0GpsPreparationTest.fixture(core);});P0GpsPreparationTest.ready(core);assertTrue(core.isRecording());long id=core.getRecordingStatus().trackId().id();
   Instant routeStart=core.getGpsPreview().time().plusSeconds(1);
   for(int i=0;i<4;i++){int index=i;main(()->{Instant at=routeStart.plusSeconds(index);core.getTrackPointCreator().setClock(at.toString());Location point=new Location("gps");point.setLatitude(1.3+index*.00003);point.setLongitude(103.8+index*.00002);point.setAccuracy(2);point.setSpeed(3);point.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());point.setTime(at.toEpochMilli());core.getTrackPointCreator().getSensorManager().getGpsHandler().onDataReceived(point);});}
   main(()->{var h=core.getTrackPointCreator().getSensorManager().getHeartRateHandler();h.onConnected("AA:BB:CC:DD:EE:FF","Synthetic navigation strap");h.onConnectionStateChanged(BleConnectionSession.State.CONNECTED);var c=new android.bluetooth.BluetoothGattCharacteristic(HeartRateBluetooth.HEARTRATE.measurementUUID(),0,0);h.onDataReceived(new HeartRateBluetooth().parsePayload(HeartRateBluetooth.HEARTRATE,"Synthetic navigation strap",c,new byte[]{6,(byte)148}));});
   String connection=core.getHeartRatePreview().value().sample().connectionId();var gpsBefore=retained.get().snapshot().getValue().gps();
   await(P0Activity.class,a->((RouteMapView)a.findViewById(R.id.p04_route)).recordedPointCount()>1);
   onView(withId(R.id.p0_all_history)).perform(click());await(P0HistoryActivity.class,a->a.findViewById(R.id.p04_navigation).isShown());assertTrue(core.isRecording());assertEquals(id,core.getRecordingStatus().trackId().id());
   onView(withId(R.id.p04_nav_heart)).perform(click());await(P0SensorsActivity.class,a->a.findViewById(R.id.p03_sensor_chart).isShown());
   SystemThemeTestUtils.night(true);await(P0SensorsActivity.class,ThemeUtils::isNight);assertEquals(connection,core.getHeartRatePreview().value().sample().connectionId());assertTrue(core.isRecording());
   onView(withId(R.id.p03_settings)).perform(click());Activity settings=await(P0SettingsActivity.class,a->a.findViewById(R.id.gardenia_settings_container).isShown());main(()->{var fragment=(PreferenceFragmentCompat)((P0SettingsActivity)settings).getSupportFragmentManager().findFragmentById(R.id.gardenia_settings_container);var choice=fragment.findPreference("gardenia_choice_quiet");assertNotNull(choice);choice.getOnPreferenceClickListener().onPreferenceClick(choice);});await(P0SettingsActivity.class,a->PreferencesUtils.getGardeniaTheme()==ThemeUtils.Family.QUIET&&ThemeUtils.color(a,android.R.attr.colorBackground)==a.getColor(R.color.gardenia_quiet_background));
   onView(withId(R.id.p04_nav_run)).perform(click());await(P0Activity.class,a->PreferencesUtils.getGardeniaTheme()==ThemeUtils.Family.QUIET&&ThemeUtils.color(a,android.R.attr.colorBackground)==a.getColor(R.color.gardenia_quiet_background)&&((RouteMapView)a.findViewById(R.id.p04_route)).recordedPointCount()>1);
   home.onActivity(a->assertSame(retained.get(),new ViewModelProvider(a).get(P0RecordingViewModel.class)));assertTrue(core.isRecording());assertEquals(id,core.getRecordingStatus().trackId().id());assertEquals(connection,core.getHeartRatePreview().value().sample().connectionId());assertEquals(gpsBefore,retained.get().snapshot().getValue().gps());
  }
 }
}
