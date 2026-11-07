package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.widget.TextView;
import androidx.test.core.app.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceFragmentCompat;
import org.junit.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.*;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.p0.data.SessionStore;
import de.dennisguse.opentracks.p0.recording.*;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.*;
import de.dennisguse.opentracks.ui.util.ThemeUtils;
import de.dennisguse.opentracks.sensors.driver.*;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** Actual API35 native pixels and actual service lifecycle with explicitly synthetic notifications. */
public class GardeniaThemeSystemTest {
    @Rule public final GrantPermissionRule permissions=TestUtil.createScanPermissionRule();
    @Rule public final ServiceTestRule rule=new ServiceTestRule();
    private final Context context=ApplicationProvider.getApplicationContext();
    private final JSONArray captures=new JSONArray();
    private TrackRecordingService core;
    private ThemeUtils.Family original;
    private String gps,heart,night;
    private void main(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    @Before public void before()throws Exception {
        original=PreferencesUtils.getGardeniaTheme();night=SystemThemeTestUtils.savedMode();
        gps=PreferencesUtils.getString(R.string.settings_sensor_gps_key,SensorType.INTERNAL.getPreferenceValue());
        heart=PreferencesUtils.getString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,SensorType.NONE.getPreferenceValue());
        PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,SensorType.NONE.getPreferenceValue());
        core=((TrackRecordingService.Binder)rule.bindService(new Intent(context,TrackRecordingService.class))).getService();
        main(PreferencesUtils::applyNightMode);
    }
    @After public void after()throws Exception {
        if(core!=null)main(()->{core.cancelGpsAcquisition();core.endCurrentTrack();core.stopIdleSensors();});
        TestEnvironmentUtils.stopTrackRecordingService();new SessionStore(context).save(RecordingSession.ready());
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,gps);PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,heart);
        PreferencesUtils.setGardeniaTheme(original);SystemThemeTestUtils.restore(night);
    }
    private <T extends android.app.Activity> void awaitMode(ActivityScenario<T> screen,boolean dark) {
        long end=SystemClock.uptimeMillis()+10000;AtomicBoolean ready=new AtomicBoolean();
        do{screen.onActivity(a->ready.set(ThemeUtils.isNight(a)==dark));if(ready.get())return;SystemClock.sleep(50);}while(SystemClock.uptimeMillis()<end);
        fail("Actual Android system appearance was not propagated");
    }
    private JSONObject tokens(ThemeUtils.Family family,boolean dark)throws Exception {
        try(var input=context.getAssets().open("gardenia-theme-tokens.json")){
            JSONArray rows=new JSONObject(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getJSONArray("themes");assertEquals(4,rows.length());
            for(int i=0;i<rows.length();i++)if(rows.getJSONObject(i).getString("id").equals(family.id))return rows.getJSONObject(i).getJSONObject(dark?"dark":"light");
        }throw new AssertionError("Missing family");
    }
    private void colors(android.app.Activity activity,JSONObject tokens)throws Exception {
        for(Object[] pair:new Object[][]{{android.R.attr.colorBackground,"background"},{com.google.android.material.R.attr.colorSurface,"surface"},{com.google.android.material.R.attr.colorOnSurface,"foreground"},{com.google.android.material.R.attr.colorOnSurfaceVariant,"foregroundSecondary"},{R.attr.gardeniaAccent,"accent"},{R.attr.gardeniaOnAccent,"accentForeground"},{R.attr.gardeniaDanger,"danger"}})
            assertEquals(pair[1].toString(),Color.parseColor(tokens.getString(pair[1].toString())),ThemeUtils.color(activity,(Integer)pair[0]));
        assertEquals(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode());
    }
    private File output(){String configured=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");return new File(configured!=null?new File(configured):new File(context.getExternalMediaDirs()[0],"additional_test_output"),"p0-ui");}
    private <T extends android.app.Activity> void capture(ActivityScenario<T> screen,ThemeUtils.Family family,boolean dark,String page)throws Exception {
        awaitMode(screen,dark);JSONObject expected=tokens(family,dark);
        screen.onActivity(a->{try{colors(a,expected);assertEquals(family,PreferencesUtils.getGardeniaTheme());}catch(Exception e){throw new AssertionError(e);}});
        String name="p03-"+family.id+(dark?"-dark-":"-light-")+page;P0UiCapture.save(name);
        byte[] raw=java.nio.file.Files.readAllBytes(new File(output(),name+".png").toPath());StringBuilder hash=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(raw))hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        captures.put(new JSONObject().put("file",name+".png").put("family",family.id).put("nightMode",dark?32:16).put("page",page).put("sha256",hash.toString()));
    }
    private void verify(ThemeUtils.Family family,boolean dark)throws Exception {
        PreferencesUtils.setGardeniaTheme(family);SystemThemeTestUtils.night(dark);
        long id;
        try(ActivityScenario<P0Activity> home=ActivityScenario.launch(P0Activity.class)) {
            capture(home,family,dark,"home");
            AtomicReference<P0RecordingViewModel> retained=new AtomicReference<>();home.onActivity(a->retained.set(new ViewModelProvider(a).get(P0RecordingViewModel.class)));
            main(()->{core.beginGpsAcquisition();P0GpsPreparationTest.fixture(core);});
            SystemThemeTestUtils.night(!dark);awaitMode(home,!dark);assertTrue(core.isAcquiringGps());assertNull(TrackRecordingService.activeTrackId(context));
            SystemThemeTestUtils.night(dark);awaitMode(home,dark);P0GpsPreparationTest.ready(core);
            assertTrue(core.isRecording());id=core.getRecordingStatus().trackId().id();
            main(()->{var handler=core.getTrackPointCreator().getSensorManager().getHeartRateHandler();handler.onConnected("AA:BB:CC:DD:EE:FF","Synthetic theme strap");handler.onConnectionStateChanged(BleConnectionSession.State.CONNECTED);var measurement=new android.bluetooth.BluetoothGattCharacteristic(HeartRateBluetooth.HEARTRATE.measurementUUID(),0,0);handler.onDataReceived(new HeartRateBluetooth().parsePayload(HeartRateBluetooth.HEARTRATE,"Synthetic theme strap",measurement,new byte[]{6,76}));});
            String connection=core.getHeartRatePreview().value().sample().connectionId();
            var gpsBefore=retained.get().snapshot().getValue().gps();
            SystemThemeTestUtils.night(!dark);awaitMode(home,!dark);
            assertEquals(gpsBefore,retained.get().snapshot().getValue().gps());
            home.onActivity(a->{assertSame(retained.get(),new ViewModelProvider(a).get(P0RecordingViewModel.class));assertNotNull(a.findViewById(R.id.p0_title));});
            assertTrue(core.isRecording());assertEquals(id,core.getRecordingStatus().trackId().id());assertEquals(connection,core.getHeartRatePreview().value().sample().connectionId());
            SystemThemeTestUtils.night(dark);awaitMode(home,dark);capture(home,family,dark,"recording");
            home.onActivity(a->new ViewModelProvider(a).get(P0RecordingViewModel.class).pause());
            long end=SystemClock.uptimeMillis()+10000;while(core.isRecording()&&SystemClock.uptimeMillis()<end)SystemClock.sleep(50);
            assertEquals(Long.valueOf(id),new SessionStore(context).recover().trackId());assertEquals(RecordingSession.Phase.PAUSED,new SessionStore(context).recover().phase());home.recreate();assertEquals(Long.valueOf(id),new SessionStore(context).recover().trackId());assertEquals(RecordingSession.Phase.PAUSED,new SessionStore(context).recover().phase());
            home.onActivity(a->new ViewModelProvider(a).get(P0RecordingViewModel.class).finish());
            long finished=SystemClock.uptimeMillis()+10000;AtomicBoolean done=new AtomicBoolean();
            do{home.onActivity(a->done.set(((TextView)a.findViewById(R.id.p0_recording_state)).getText().toString().equals(context.getString(R.string.p0_finished))));if(done.get())break;SystemClock.sleep(50);}while(SystemClock.uptimeMillis()<finished);assertTrue(done.get());
        }
        try(ActivityScenario<P0SensorsActivity> sensors=ActivityScenario.launch(P0SensorsActivity.class)){capture(sensors,family,dark,"heart");}
        try(ActivityScenario<P0HistoryActivity> history=ActivityScenario.launch(P0HistoryActivity.class)){SystemClock.sleep(300);capture(history,family,dark,"history");}
        try(ActivityScenario<P0HistoryActivity> detail=ActivityScenario.launch(P0HistoryActivity.intent(context,id))) {
            SystemClock.sleep(500);capture(detail,family,dark,"detail");
            detail.onActivity(a->new GpxExportDialog().show(a.getSupportFragmentManager(),"gardenia-gpx-export"));
            SystemClock.sleep(100);SystemThemeTestUtils.night(!dark);awaitMode(detail,!dark);
            detail.onActivity(a->{assertNotNull(a.getSupportFragmentManager().findFragmentByTag("gardenia-gpx-export"));assertEquals(Long.valueOf(id),new ViewModelProvider(a).get(P0HistoryViewModel.class).selectedId());});
            SystemThemeTestUtils.night(dark);awaitMode(detail,dark);capture(detail,family,dark,"dialog");
        }
        Intent settingsIntent=new Intent(context,P0SettingsActivity.class).putExtra(SettingsActivity.FRAGMENT_KEY,context.getString(R.string.gardenia_appearance_key));
        try(ActivityScenario<P0SettingsActivity> settings=ActivityScenario.launch(settingsIntent)) {
            settings.onActivity(a->{var f=(PreferenceFragmentCompat)a.getSupportFragmentManager().findFragmentById(R.id.gardenia_settings_container);for(ThemeUtils.Family choice:ThemeUtils.Family.values())assertNotNull(f.findPreference("gardenia_choice_"+choice.id).getIcon());assertNull(f.findPreference(context.getString(R.string.theme_key)));});
            capture(settings,family,dark,"settings");settings.recreate();assertEquals(family,PreferencesUtils.getGardeniaTheme());
        }
        assertEquals(7,captures.length());
        JSONObject evidence=new JSONObject().put("sourceCommit",InstrumentationRegistry.getArguments().getString("buildCommit","local")).put("api",Build.VERSION.SDK_INT).put("timezone",java.util.TimeZone.getDefault().getID()).put("family",family.id).put("appearance",dark?"dark":"light").put("passed",true).put("physicalBleTested",false).put("syntheticNotifications",true).put("systemSwitchPreservesRecording",true).put("systemSwitchPreservesGps",true).put("systemSwitchPreservesConnection",true).put("viewModelRetained",true).put("pausedStatePreserved",true).put("waitingCreatesNoWorkout",true).put("dialogRestored",true).put("screenshots",captures);
        try(var out=new FileOutputStream(new File(output(),"p03-"+family.id+(dark?"-dark":"-light")+".json"))){out.write(evidence.toString(2).getBytes(StandardCharsets.UTF_8));}
    }
    @Test public void precisionLight()throws Exception{verify(ThemeUtils.Family.PRECISION,false);}
    @Test public void precisionDark()throws Exception{verify(ThemeUtils.Family.PRECISION,true);}
    @Test public void quietLight()throws Exception{verify(ThemeUtils.Family.QUIET,false);}
    @Test public void quietDark()throws Exception{verify(ThemeUtils.Family.QUIET,true);}
    @Test public void explorerLight()throws Exception{verify(ThemeUtils.Family.EXPLORER,false);}
    @Test public void explorerDark()throws Exception{verify(ThemeUtils.Family.EXPLORER,true);}
    @Test public void neonLight()throws Exception{verify(ThemeUtils.Family.NEON,false);}
    @Test public void neonDark()throws Exception{verify(ThemeUtils.Family.NEON,true);}
    @Test public void familyChoicesPersistAndRecreateTheCurrentSettingsPage()throws Exception {
        Intent intent=new Intent(context,P0SettingsActivity.class).putExtra(SettingsActivity.FRAGMENT_KEY,context.getString(R.string.gardenia_appearance_key));
        try(ActivityScenario<P0SettingsActivity> settings=ActivityScenario.launch(intent)) {
            for(ThemeUtils.Family family:ThemeUtils.Family.values()) {
                settings.onActivity(a->{var f=(PreferenceFragmentCompat)a.getSupportFragmentManager().findFragmentById(R.id.gardenia_settings_container);var p=f.findPreference("gardenia_choice_"+family.id);assertNotNull(p);p.getOnPreferenceClickListener().onPreferenceClick(p);});
                SystemClock.sleep(300);assertEquals(family,PreferencesUtils.getGardeniaTheme());settings.recreate();
                settings.onActivity(a->assertTrue(a.getSupportFragmentManager().findFragmentById(R.id.gardenia_settings_container) instanceof GardeniaThemeSettingsFragment));
            }
        }
    }
    @Test public void narrowScreenLargeFontKeepsNativeTouchAndReadingTargets()throws Exception {
        String size=SystemThemeTestUtils.shell("wm size"),density=SystemThemeTestUtils.shell("wm density"),font=SystemThemeTestUtils.shell("settings get system font_scale");
        try {
            SystemThemeTestUtils.shell("wm size 720x1280");SystemThemeTestUtils.shell("wm density 320");SystemThemeTestUtils.shell("settings put system font_scale 1.3");
            for(ThemeUtils.Family family:ThemeUtils.Family.values()) {
                PreferencesUtils.setGardeniaTheme(family);
                try(ActivityScenario<P0Activity> home=ActivityScenario.launch(P0Activity.class)) {
                    home.onActivity(a->{assertTrue(a.getResources().getConfiguration().fontScale>=1.25f);float dp=a.getResources().getDisplayMetrics().density;for(int id:new int[]{R.id.p0_start,R.id.p03_settings,R.id.p0_all_history}){var v=a.findViewById(id);assertTrue(v.getHeight()/dp>=47.5);assertTrue(v.getWidth()/dp>=47.5);}assertNoEllipsis(a.findViewById(android.R.id.content));assertNotNull(a.findViewById(R.id.p03_heart_chart).getContentDescription());});
                    P0UiCapture.save("p03-"+family.id+"-font130-narrow");
                }
            }
        } finally {
            SystemThemeTestUtils.shell("settings put system font_scale "+(font.equals("null")?"1.0":font));
            for(String[] value:new String[][]{{"size",size},{"density",density}}){var match=java.util.regex.Pattern.compile("Override (?:size|density): ([^\\r\\n]+)").matcher(value[1]);SystemThemeTestUtils.shell("wm "+value[0]+" "+(match.find()?match.group(1).trim():"reset"));}
        }
    }
    private void assertNoEllipsis(android.view.View view) {
        if(view instanceof TextView text&&view.isShown()&&text.getLayout()!=null)for(int i=0;i<text.getLineCount();i++)assertEquals("Truncated accessible theme text",0,text.getLayout().getEllipsisCount(i));
        if(view instanceof android.view.ViewGroup group)for(int i=0;i<group.getChildCount();i++)assertNoEllipsis(group.getChildAt(i));
    }

}
