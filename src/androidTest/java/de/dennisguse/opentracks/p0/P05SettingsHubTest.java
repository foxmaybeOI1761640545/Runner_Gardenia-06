package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static org.hamcrest.Matchers.allOf;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.preference.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.lifecycle.*;
import org.junit.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import de.dennisguse.opentracks.BuildConfig;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;

/** Actual native preference taps, restored pages, existing destinations and API35 pixels. */
public class P05SettingsHubTest {
 @Rule public final GrantPermissionRule permissions=TestUtil.createScanPermissionRule();
 private final Context context=ApplicationProvider.getApplicationContext();
 private ThemeUtils.Family family;private String night,size,density,font,units,rate;
 private final Map<ThemeUtils.Family,DashboardMetricLayout> layouts=new EnumMap<>(ThemeUtils.Family.class);
 private void main(Runnable r){InstrumentationRegistry.getInstrumentation().runOnMainSync(r);}
 @Before public void before()throws Exception{
  family=PreferencesUtils.getGardeniaTheme();night=SystemThemeTestUtils.savedMode();size=SystemThemeTestUtils.shell("wm size");density=SystemThemeTestUtils.shell("wm density");font=SystemThemeTestUtils.shell("settings get system font_scale");
  units=PreferencesUtils.getString(R.string.stats_units_key,context.getString(R.string.stats_units_default));rate=PreferencesUtils.getString(R.string.stats_rate_key,context.getString(R.string.stats_rate_default));
  for(var f:ThemeUtils.Family.values())layouts.put(f,PreferencesUtils.getDashboardMetricLayout(f));
 }
 @After public void after()throws Exception{
  main(()->{for(Stage s:Stage.values())if(s!=Stage.DESTROYED)for(Activity a:new ArrayList<>(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(s)))if(a instanceof P0SettingsActivity||a instanceof P0SensorsActivity||a instanceof P0HistoryActivity)a.finish();});
  PreferencesUtils.setGardeniaTheme(family);PreferencesUtils.setString(R.string.stats_units_key,units);PreferencesUtils.setString(R.string.stats_rate_key,rate);layouts.forEach(PreferencesUtils::setDashboardMetricLayout);
  SystemThemeTestUtils.restore(night);SystemThemeTestUtils.shell("settings put system font_scale "+(font.equals("null")?"1.0":font));restore("size",size);restore("density",density);
 }
 private void restore(String name,String value)throws Exception{var m=Pattern.compile("Override (?:size|density): ([^\\r\\n]+)").matcher(value);SystemThemeTestUtils.shell("wm "+name+" "+(m.find()?m.group(1).trim():"reset"));}
 private PreferenceFragmentCompat page(P0SettingsActivity a){return (PreferenceFragmentCompat)a.getSupportFragmentManager().findFragmentById(R.id.gardenia_settings_container);}
 private void tap(ActivityScenario<P0SettingsActivity> scenario,String key){
  AtomicReference<String> title=new AtomicReference<>();scenario.onActivity(a->{var p=page(a).findPreference(key);assertNotNull(key,p);title.set(p.getTitle().toString());var list=page(a).getListView();int index=((PreferenceGroupAdapter)list.getAdapter()).getPreferenceAdapterPosition(p);assertTrue(index>=0);list.scrollToPosition(index);});
  InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  onView(allOf(withText(title.get()),isDescendantOfA(isAssignableFrom(RecyclerView.class)))).perform(click());
  InstrumentationRegistry.getInstrumentation().waitForIdleSync();
 }
 private void back(ActivityScenario<P0SettingsActivity> scenario){scenario.onActivity(a->a.getOnBackPressedDispatcher().onBackPressed());InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
 @Test public void sixCategoriesRestoreAndReturnToHubWithRealDeviceAndHistoryDestinations()throws Exception{
  try(var settings=ActivityScenario.launch(P0SettingsActivity.class)){
   for(String category:P05SettingsHubFragment.CATEGORIES){
    tap(settings,"p05_category_"+category);
    settings.recreate();settings.onActivity(a->{assertTrue(a.findViewById(R.id.p03_settings).isSelected());if(category.equals("appearance"))assertTrue(page(a) instanceof de.dennisguse.opentracks.settings.GardeniaThemeSettingsFragment);else assertEquals(category,((P05SettingsSectionFragment)page(a)).category());});
    if(category.equals("info"))settings.onActivity(a->{String actualLabel=context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();assertTrue(actualLabel.contains("Runner Gardenia"));assertEquals(actualLabel,page(a).findPreference("p05_app_name").getSummary());assertEquals(BuildConfig.VERSION_NAME,page(a).findPreference("p05_version").getSummary());assertEquals(Integer.toString(BuildConfig.VERSION_CODE),page(a).findPreference("p05_build").getSummary());});
    if(category.equals("devices")){tap(settings,"p05_manage_devices");finishDestination(P0SensorsActivity.class,R.id.p0_sensor_container);}
    if(category.equals("history")){tap(settings,"p05_manage_history");finishDestination(P0HistoryActivity.class,R.id.p0_history_list);}
    back(settings);settings.onActivity(a->{assertTrue(page(a) instanceof P05SettingsHubFragment);assertEquals(6,page(a).getPreferenceScreen().getPreferenceCount());});
   }
   onView(withId(R.id.p04_nav_run)).perform(click());finishDestination(P0Activity.class,R.id.p0_start);
  }
 }
 private void finishDestination(Class<? extends Activity> type,int view){
  AtomicReference<Activity> found=new AtomicReference<>();long deadline=SystemClock.uptimeMillis()+10000;
  do{main(()->{for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(type.isInstance(a)&&a.hasWindowFocus())found.set(a);});if(found.get()!=null)break;SystemClock.sleep(50);}while(SystemClock.uptimeMillis()<deadline);
  assertNotNull(type.getName(),found.get());main(()->{assertNotNull(found.get().findViewById(view));found.get().finish();});InstrumentationRegistry.getInstrumentation().waitForIdleSync();
 }
 @Test public void nativeUnitRateChoicesPersistAndResetOnlyTheCurrentTheme()throws Exception{
  PreferencesUtils.setGardeniaTheme(ThemeUtils.Family.PRECISION);
  for(var f:ThemeUtils.Family.values())PreferencesUtils.setDashboardMetricLayout(f,DashboardMetricLayout.defaults(f==ThemeUtils.Family.NEON).swapSecondary(1));
  Map<ThemeUtils.Family,DashboardMetricLayout> before=new EnumMap<>(ThemeUtils.Family.class);for(var f:ThemeUtils.Family.values())before.put(f,PreferencesUtils.getDashboardMetricLayout(f));
  try(var settings=ActivityScenario.launch(P0SettingsActivity.class)){
   tap(settings,"p05_category_motion");
   for(int choice:new int[]{1,0}){
    tap(settings,context.getString(R.string.stats_units_key));onView(withText(context.getResources().getStringArray(R.array.stats_units_options)[choice])).inRoot(isDialog()).perform(click());settings.recreate();
    settings.onActivity(a->assertEquals(context.getResources().getStringArray(R.array.stats_units_values)[choice],((ListPreference)page(a).findPreference(context.getString(R.string.stats_units_key))).getValue()));
   }
   for(int choice:new int[]{1,2}){
    tap(settings,context.getString(R.string.stats_rate_key));onView(withText(context.getResources().getStringArray(R.array.stats_rate_metric_options)[choice])).inRoot(isDialog()).perform(click());settings.recreate();
    settings.onActivity(a->assertEquals(context.getResources().getStringArray(R.array.stats_rate_values)[choice],((ListPreference)page(a).findPreference(context.getString(R.string.stats_rate_key))).getValue()));
    assertEquals(choice==1,PreferencesUtils.isReportSpeed(PreferencesUtils.getDefaultActivityTypeLocalized()));
   }
   tap(settings,"p05_reset_metrics");assertEquals(DashboardMetricLayout.defaults(false),PreferencesUtils.getDashboardMetricLayout(ThemeUtils.Family.PRECISION));
   for(var f:ThemeUtils.Family.values())if(f!=ThemeUtils.Family.PRECISION)assertEquals(before.get(f),PreferencesUtils.getDashboardMetricLayout(f));
   back(settings);tap(settings,"p05_category_motion");settings.recreate();assertEquals(DashboardMetricLayout.defaults(false),PreferencesUtils.getDashboardMetricLayout(ThemeUtils.Family.PRECISION));
  }
 }
 @Test public void eightNativePalettesAtNarrowWidthAndLargeFont()throws Exception{
  assertEquals(35,android.os.Build.VERSION.SDK_INT);SystemThemeTestUtils.shell("wm size 720x1600");SystemThemeTestUtils.shell("wm density 320");SystemThemeTestUtils.shell("settings put system font_scale 1.3");
  for(var f:ThemeUtils.Family.values())for(boolean dark:new boolean[]{false,true}){
   PreferencesUtils.setGardeniaTheme(f);SystemThemeTestUtils.night(dark);
   try(var settings=ActivityScenario.launch(P0SettingsActivity.class)){
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();settings.onActivity(a->{assertEquals(360,a.getResources().getConfiguration().screenWidthDp);assertTrue(a.getResources().getConfiguration().fontScale>=1.29f);assertEquals(dark,ThemeUtils.isNight(a));checkText(a.findViewById(android.R.id.content));assertEquals(6,page(a).getPreferenceScreen().getPreferenceCount());});
    P0UiCapture.save("p05-settings-hub-"+f.id+(dark?"-dark":"-light")+"-360-font130-native");
    // Scroll to every row and check its complete text and native touch target.
    for(String category:P05SettingsHubFragment.CATEGORIES){
     settings.onActivity(a->{var p=page(a).findPreference("p05_category_"+category);assertNotNull(p.getIcon());page(a).getListView().scrollToPosition(((PreferenceGroupAdapter)page(a).getListView().getAdapter()).getPreferenceAdapterPosition(p));});InstrumentationRegistry.getInstrumentation().waitForIdleSync();
     settings.onActivity(a->{var list=page(a).getListView();for(int i=0;i<list.getChildCount();i++){var row=list.getChildAt(i);assertTrue(row.getHeight()/a.getResources().getDisplayMetrics().density>=48);checkText(row);}});
    }
    tap(settings,"p05_category_motion");settings.onActivity(a->checkText(a.findViewById(android.R.id.content)));P0UiCapture.save("p05-settings-motion-"+f.id+(dark?"-dark":"-light")+"-360-font130-native");
   }
  }
 }
 private void checkText(View v){if(!v.isShown())return;if(v instanceof TextView t&&t.getLayout()!=null)for(int i=0;i<t.getLineCount();i++)assertEquals("Settings text must wrap completely: "+t.getText(),0,t.getLayout().getEllipsisCount(i));if(v instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++)checkText(g.getChildAt(i));}
}
