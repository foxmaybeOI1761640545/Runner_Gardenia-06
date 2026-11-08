package de.dennisguse.opentracks.p0;
import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.test.core.app.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;

/** Actual native pixel matrix, not an HTML rendering. Business regressions run separately in the full suite. */
public class DashboardVisualMatrixTest {
 private final Context context=ApplicationProvider.getApplicationContext();private final JSONArray captures=new JSONArray();
 private ThemeUtils.Family family;private String night,size,density,font;private LocaleListCompat locales;private final String[] scales={"window_animation_scale","transition_animation_scale","animator_duration_scale"};private final String[] scaleValues=new String[3];
 private void main(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
 @Before public void before()throws Exception{
  family=PreferencesUtils.getGardeniaTheme();night=SystemThemeTestUtils.savedMode();size=SystemThemeTestUtils.shell("wm size");density=SystemThemeTestUtils.shell("wm density");font=SystemThemeTestUtils.shell("settings get system font_scale");locales=AppCompatDelegate.getApplicationLocales();
  for(int i=0;i<scales.length;i++){scaleValues[i]=SystemThemeTestUtils.shell("settings get global "+scales[i]);SystemThemeTestUtils.shell("settings put global "+scales[i]+" 0");}SystemThemeTestUtils.shell("settings put system font_scale 1.0");main(()->AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN")));
 }
 @After public void after()throws Exception{
  PreferencesUtils.setGardeniaTheme(family);SystemThemeTestUtils.restore(night);main(()->AppCompatDelegate.setApplicationLocales(locales));SystemThemeTestUtils.shell("settings put system font_scale "+(font.equals("null")?"1.0":font));
  for(String[] r:new String[][]{{"size",size},{"density",density}}){var match=java.util.regex.Pattern.compile("Override (?:size|density): ([^\\r\\n]+)").matcher(r[1]);SystemThemeTestUtils.shell("wm "+r[0]+" "+(match.find()?match.group(1).trim():"reset"));}
  for(int i=0;i<scales.length;i++)SystemThemeTestUtils.shell(scaleValues[i].equals("null")?"settings delete global "+scales[i]:"settings put global "+scales[i]+" "+scaleValues[i]);
 }
 private File output(){String configured=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");return new File(configured!=null?new File(configured):new File(context.getExternalMediaDirs()[0],"additional_test_output"),"p0-ui");}
 private static String hash(byte[] raw)throws Exception{var b=new StringBuilder();for(byte v:java.security.MessageDigest.getInstance("SHA-256").digest(raw))b.append(String.format(Locale.ROOT,"%02x",v&255));return b.toString();}
 @Test public void allFamiliesSystemModesViewportsAndEightStates()throws Exception{
  for(int[] viewport:new int[][]{{360,800},{390,844},{412,915}}){
   SystemThemeTestUtils.shell("wm size "+(viewport[0]*2)+"x"+(viewport[1]*2));SystemThemeTestUtils.shell("wm density 320");
   for(ThemeUtils.Family f:ThemeUtils.Family.values())for(boolean dark:new boolean[]{false,true}){
    PreferencesUtils.setGardeniaTheme(f);SystemThemeTestUtils.night(dark);
    try(ActivityScenario<DashboardPreviewActivity> screen=ActivityScenario.launch(DashboardPreviewActivity.class)){
     long modeDeadline=SystemClock.uptimeMillis()+10000;java.util.concurrent.atomic.AtomicBoolean modeReady=new java.util.concurrent.atomic.AtomicBoolean();
     do{screen.onActivity(a->modeReady.set(ThemeUtils.isNight(a)==dark&&a.hasWindowFocus()));if(modeReady.get())break;SystemClock.sleep(50);}while(SystemClock.uptimeMillis()<modeDeadline);assertTrue("Actual system mode/focus never propagated",modeReady.get());
     for(String state:DashboardFixture.STATES){
      screen.onActivity(a->{assertEquals(viewport[0],a.getResources().getConfiguration().screenWidthDp);assertEquals(dark,ThemeUtils.isNight(a));((ScrollView)a.findViewById(R.id.p02_home_scroll)).scrollTo(0,0);DashboardFixture.present(a,state);});
      SystemClock.sleep(180);InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      String name="p04-"+f.id+(dark?"-dark-":"-light-")+viewport[0]+"x"+viewport[1]+"-"+state;
      screen.onActivity(a->{assertTrue(a.hasWindowFocus());View content=a.findViewById(R.id.p03_content);assertTrue(content.getWidth()<=a.findViewById(android.R.id.content).getWidth());checkHorizontal(content);
       for(int id:new int[]{R.id.p04_nav_run,R.id.p0_all_history,R.id.p04_nav_heart,R.id.p03_settings}){View v=a.findViewById(id);Rect b=new Rect();assertTrue(v.getGlobalVisibleRect(b));assertTrue(b.height()/a.getResources().getDisplayMetrics().density>=47.5);}
       View scroll=a.findViewById(R.id.p02_home_scroll),nav=a.findViewById(R.id.p04_navigation);assertTrue(scroll.getBottom()<=nav.getTop());
       var chart=(PulseChartView)a.findViewById(R.id.p03_heart_chart);assertTrue(chart.getHeight()/a.getResources().getDisplayMetrics().density<=90);
       boolean fresh=!Set.of("idle","gps-searching","finished","disconnected","connected-no-sample").contains(state);assertEquals(fresh,chart.sampleCount()>0);
       assertFalse(((TextView)a.findViewById(R.id.p02_heart_device)).getText().toString().matches(".*[0-9A-F]{2}(:[0-9A-F]{2}){5}.*"));
       if(state.equals("recording")){assertEquals("5.25 km",((TextView)a.findViewById(R.id.p02_distance_value)).getText().toString());assertEquals("28:36",((TextView)a.findViewById(R.id.p02_time_value)).getText().toString());assertTrue(((TextView)a.findViewById(R.id.p0_heart_rate)).getText().toString().contains("148"));assertTrue(((RouteMapView)a.findViewById(R.id.p04_route)).recordedPointCount()>1);}
      });
      P0UiCapture.save(name);byte[] png=java.nio.file.Files.readAllBytes(new File(output(),name+".png").toPath());
      captures.put(new JSONObject().put("file",name+".png").put("family",f.id).put("appearance",dark?"dark":"light").put("widthDp",viewport[0]).put("heightDp",viewport[1]).put("state",state).put("sha256",hash(png)).put("syntheticUiFixture",true));
     }
    }
   }
  }
  assertEquals(192,captures.length());
  JSONObject proof=new JSONObject().put("passed",true).put("sourceCommit",InstrumentationRegistry.getArguments().getString("buildCommit","local")).put("api",Build.VERSION.SDK_INT).put("policy","FOLLOW_SYSTEM").put("native",true).put("productionFixtureEntry",false).put("screenshots",captures).put("fixture",new JSONObject().put("distanceKm",5.25).put("duration","28:36").put("pace","5:27 /km").put("bpm",148));
  try(var stream=new FileOutputStream(new File(output(),"p04-visual-matrix.json"))){stream.write(proof.toString(2).getBytes(StandardCharsets.UTF_8));}
 }
 private static void checkHorizontal(View v){if(!v.isShown())return;if(v instanceof TextView text&&text.getLayout()!=null)for(int line=0;line<text.getLineCount();line++)assertEquals("Truncated dashboard text",0,text.getLayout().getEllipsisCount(line));if(v instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View child=group.getChildAt(i);if(child.isShown()){assertTrue("Horizontal dashboard overflow",child.getRight()<=group.getWidth()+1);checkHorizontal(child);}}}
}
