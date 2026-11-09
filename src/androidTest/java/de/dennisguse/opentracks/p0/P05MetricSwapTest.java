package de.dennisguse.opentracks.p0;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.*;
import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import org.junit.*;
import java.util.*;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;
/** Native input and persistence contract; displayed sample data is test-only. */
public class P05MetricSwapTest {
 private ThemeUtils.Family family;private final Map<ThemeUtils.Family,DashboardMetricLayout> saved=new EnumMap<>(ThemeUtils.Family.class);
 private final int[] buttons={R.id.p05_secondary_1,R.id.p05_secondary_2,R.id.p05_secondary_3};
 @Before public void before(){family=PreferencesUtils.getGardeniaTheme();for(var f:ThemeUtils.Family.values()){saved.put(f,PreferencesUtils.getDashboardMetricLayout(f));PreferencesUtils.resetDashboardMetricLayout(f);}}
 @After public void after(){for(var f:ThemeUtils.Family.values())PreferencesUtils.setDashboardMetricLayout(f,saved.get(f));PreferencesUtils.setGardeniaTheme(family);}
 @Test public void actualTouchesSwapOnlyClickedPositionAndKeepViewsAcrossRecreation()throws Exception{
  for(var f:ThemeUtils.Family.values()){
   PreferencesUtils.setGardeniaTheme(f);var expected=DashboardMetricLayout.defaults(f==ThemeUtils.Family.NEON);
   try(var screen=ActivityScenario.launch(DashboardPreviewActivity.class)){
    screen.onActivity(a->DashboardFixture.present(a,"recording"));
    final View[] root=new View[1];screen.onActivity(a->{root[0]=a.findViewById(R.id.p03_content);assertFalse(a.findViewById(R.id.p04_accuracy).isClickable());assertFalse(a.findViewById(R.id.p04_accuracy).isFocusable());});
    for(int slot:new int[]{0,1,2,1,0,2}){
     expected=expected.swapSecondary(slot);var check=expected;
     onView(withId(buttons[slot])).perform(scrollTo(),click());
     screen.onActivity(a->{assertSame(root[0],a.findViewById(R.id.p03_content));assertEquals(check,PreferencesUtils.getDashboardMetricLayout(f));assertRendered(a,f,check);View button=a.findViewById(buttons[slot]);assertTrue(button.isFocusable());assertNotNull(button.getContentDescription());assertEquals(android.widget.Button.class.getName(),button.createAccessibilityNodeInfo().getClassName().toString());});
    }
    screen.recreate();var check=expected;screen.onActivity(a->{DashboardFixture.present(a,"recording");assertEquals(check,PreferencesUtils.getDashboardMetricLayout(f));assertRendered(a,f,check);});P0UiCapture.save("p05-swap-"+f.id+"-recreated-synthetic");
   }
   assertEquals(expected,PreferencesUtils.getDashboardMetricLayout(f));
  }
 }
 @Test public void unavailableHeartStillSwapsAndEachThemeKeepsItsOwnOrder()throws Exception{
  for(var f:ThemeUtils.Family.values()){
   PreferencesUtils.setGardeniaTheme(f);var layout=DashboardMetricLayout.defaults(f==ThemeUtils.Family.NEON);
   try(var screen=ActivityScenario.launch(DashboardPreviewActivity.class)){
    screen.onActivity(a->DashboardFixture.present(a,"disconnected"));
    if(layout.primary()!=DashboardMetric.HEART_RATE){onView(withId(buttons[2])).perform(scrollTo(),click());}
    screen.onActivity(a->{TextView value=a.findViewById(f==ThemeUtils.Family.NEON?R.id.p0_heart_rate:R.id.p02_distance_value);assertEquals("--",value.getText().toString());assertEquals(DashboardMetric.HEART_RATE,PreferencesUtils.getDashboardMetricLayout(f).primary());});
   }
  }
  for(var f:ThemeUtils.Family.values())assertEquals(DashboardMetric.HEART_RATE,PreferencesUtils.getDashboardMetricLayout(f).primary());
  PreferencesUtils.resetDashboardMetricLayout(ThemeUtils.Family.PRECISION);
  assertEquals(DashboardMetric.DISTANCE,PreferencesUtils.getDashboardMetricLayout(ThemeUtils.Family.PRECISION).primary());
  for(var f:ThemeUtils.Family.values())if(f!=ThemeUtils.Family.PRECISION)assertEquals(DashboardMetric.HEART_RATE,PreferencesUtils.getDashboardMetricLayout(f).primary());
 }

 @Test public void keyboardActivationKeepsFocusAndLongDurationsFitEveryPalette()throws Exception{
  String oldNight=SystemThemeTestUtils.savedMode(),oldFont=SystemThemeTestUtils.shell("settings get system font_scale"),oldSize=SystemThemeTestUtils.shell("wm size"),oldDensity=SystemThemeTestUtils.shell("wm density");
  try{
   SystemThemeTestUtils.shell("wm size 720x1600");SystemThemeTestUtils.shell("wm density 320");SystemThemeTestUtils.shell("settings put system font_scale 1.3");
   for(var f:ThemeUtils.Family.values())for(boolean dark:new boolean[]{false,true}){
    PreferencesUtils.setGardeniaTheme(f);PreferencesUtils.resetDashboardMetricLayout(f);SystemThemeTestUtils.night(dark);
    try(var screen=ActivityScenario.launch(DashboardPreviewActivity.class)){
     screen.onActivity(a->a.present(new DashboardState(de.dennisguse.opentracks.p0.recording.RecordingSession.Phase.FINISHED,false,false,"1234.56 km","125:25:30","—","—","--","Disconnected","Synthetic","—","Auto","Finished","—","—",null,false,false,null),List.of(),List.of()));
     int slot=f==ThemeUtils.Family.NEON?1:0;
     onView(withId(buttons[slot])).perform(scrollTo());
     androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
     screen.onActivity(a->assertTrue(a.findViewById(buttons[slot]).requestFocus()));
     onView(withId(buttons[slot])).perform(pressKey(android.view.KeyEvent.KEYCODE_ENTER));
     screen.onActivity(a->{assertEquals(DashboardMetric.DURATION,PreferencesUtils.getDashboardMetricLayout(f).primary());assertTrue(a.findViewById(buttons[slot]).hasFocus());});
     androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
     screen.onActivity(a->{TextView value=a.findViewById(f==ThemeUtils.Family.NEON?R.id.p0_heart_rate:R.id.p02_distance_value);assertEquals("125:25:30",value.getText().toString());var text=value.getLayout();assertNotNull(text);assertEquals(value.getText().length(),text.getLineEnd(text.getLineCount()-1));for(int line=0;line<text.getLineCount();line++){assertEquals(0,text.getEllipsisCount(line));assertTrue(text.getLineWidth(line)<=text.getWidth()+1);}((android.widget.ScrollView)a.findViewById(R.id.p02_home_scroll)).scrollTo(0,0);});
     P0UiCapture.save("p05-metric-duration-"+f.id+(dark?"-dark":"-light")+"-360-font130-synthetic");
     var instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
     var announced=instrumentation.getUiAutomation().executeAndWaitForEvent(()->screen.onActivity(a->assertTrue(a.findViewById(buttons[slot]).performClick())),event->event.getEventType()==android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT,5000);
     try{assertFalse("Metric change must announce its new primary",announced.getText().isEmpty());}finally{announced.recycle();}

    }
   }
  }finally{
   SystemThemeTestUtils.restore(oldNight);SystemThemeTestUtils.shell("settings put system font_scale "+(oldFont.equals("null")?"1.0":oldFont));
   for(String[] original:new String[][]{{"size",oldSize},{"density",oldDensity}}){var match=java.util.regex.Pattern.compile("Override (?:size|density): ([^\\r\\n]+)").matcher(original[1]);SystemThemeTestUtils.shell("wm "+original[0]+" "+(match.find()?match.group(1).trim():"reset"));}
  }
 }
 private static void assertRendered(DashboardPreviewActivity a,ThemeUtils.Family f,DashboardMetricLayout order){
  int[] ids=f==ThemeUtils.Family.NEON?new int[]{R.id.p0_heart_rate,R.id.p02_distance_value,R.id.p02_time_value,R.id.p02_pace_value}:new int[]{R.id.p02_distance_value,R.id.p02_time_value,R.id.p02_pace_value,R.id.p0_heart_rate};
  boolean speed=PreferencesUtils.isReportSpeed(PreferencesUtils.getDefaultActivityTypeLocalized());
  for(int i=0;i<4;i++){String expected=switch(order.positions().get(i)){case DISTANCE->"5.25 km";case DURATION->"28:36";case PACE_SPEED->speed?"11.0 km/h":"5:27 /km";case HEART_RATE->a.getString(R.string.hr_value,148f);};assertEquals("Rendered content must follow the actual ordered position",expected,((TextView)a.findViewById(ids[i])).getText().toString());}
 }

}
