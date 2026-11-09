package de.dennisguse.opentracks.p0;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.anything;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.test.core.app.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.zip.*;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.*;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.p0.data.*;
import de.dennisguse.opentracks.p0.recording.RecordingSession;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** Actual native ListView/checkbox/actions/dialogs and the original worker/database path. */
public class P05HistoryBatchTest {
 private final Context context=ApplicationProvider.getApplicationContext();private final List<Track.Id> ids=new ArrayList<>();
 private ThemeUtils.Family family;private String night,size,density,font;
 private void main(Runnable r){InstrumentationRegistry.getInstrumentation().runOnMainSync(r);}
 @Before public void before()throws Exception{TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());family=PreferencesUtils.getGardeniaTheme();night=SystemThemeTestUtils.savedMode();size=SystemThemeTestUtils.shell("wm size");density=SystemThemeTestUtils.shell("wm density");font=SystemThemeTestUtils.shell("settings get system font_scale");}
 @After public void after()throws Exception{new SessionStore(context).save(RecordingSession.ready());PreferencesUtils.setGardeniaTheme(family);SystemThemeTestUtils.restore(night);SystemThemeTestUtils.shell("settings put system font_scale "+(font.equals("null")?"1.0":font));restore("size",size);restore("density",density);}
 private void restore(String type,String text)throws Exception{var m=java.util.regex.Pattern.compile("Override (?:size|density): ([^\\r\\n]+)").matcher(text);SystemThemeTestUtils.shell("wm "+type+" "+(m.find()?m.group(1).trim():"reset"));}
 private void fixtures(int count,boolean samples){for(int i=0;i<count;i++)ids.add(P05HistoryFixtures.create(context,"Synthetic batch workout "+i,samples));}
 private P0HistoryViewModel model(P0HistoryActivity a){return new ViewModelProvider(a).get(P0HistoryViewModel.class);}
 private void await(ActivityScenario<P0HistoryActivity> screen,java.util.function.Predicate<P0HistoryViewModel> condition){long until=SystemClock.uptimeMillis()+15000;AtomicBoolean yes=new AtomicBoolean();do{screen.onActivity(a->yes.set(condition.test(model(a))));if(yes.get())return;SystemClock.sleep(30);}while(SystemClock.uptimeMillis()<until);fail("History state/operation did not complete");}
 private void loaded(ActivityScenario<P0HistoryActivity> screen,int count){await(screen,m->!m.state().getValue().loading()&&m.state().getValue().rows().size()==count);}
 private void row(int position,boolean longer){onData(anything()).inAdapterView(withId(R.id.p0_history_list)).atPosition(position).perform(longer?longClick():click());}
 private void count(ActivityScenario<P0HistoryActivity> screen,int n){screen.onActivity(a->{assertEquals(n,model(a).selection().getValue().ids().size());assertEquals(context.getString(R.string.p05_selected_count,n),((TextView)a.findViewById(R.id.p05_selected_count)).getText().toString());});}
 private static List<Intent> shares=new CopyOnWriteArrayList<>();
 private android.app.Instrumentation.ActivityMonitor chooserMonitor(){return new android.app.Instrumentation.ActivityMonitor(){@Override public android.app.Instrumentation.ActivityResult onStartActivity(Intent i){if(!Intent.ACTION_CHOOSER.equals(i.getAction()))return null;shares.add(i.getParcelableExtra(Intent.EXTRA_INTENT));return new android.app.Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null);}};}
 private void awaitShares(int count){long until=SystemClock.uptimeMillis()+15000;while(shares.size()<count&&SystemClock.uptimeMillis()<until)SystemClock.sleep(30);assertEquals(count,shares.size());}
 @Test public void nativeLongPressToggleLoadedOnlySelectAllRecreateAndNormalDetail()throws Exception{
  fixtures(55,false);try(var screen=ActivityScenario.launch(P0HistoryActivity.class)){
   loaded(screen,50);row(0,true);count(screen,1);row(0,false);count(screen,0);row(1,false);row(2,false);count(screen,2);
   screen.recreate();loaded(screen,50);count(screen,2);onView(withId(R.id.p05_batch_select_all)).perform(click());count(screen,50);
   onView(withId(R.id.p0_load_more)).perform(click());loaded(screen,55);count(screen,50);onView(withId(R.id.p05_batch_select_all)).perform(click());count(screen,55);
   onView(withId(R.id.p05_batch_cancel)).perform(click());screen.onActivity(a->assertFalse(model(a).selection().getValue().active()));row(0,false);await(screen,m->m.state().getValue().selected()!=null);
   screen.onActivity(a->{assertNotNull(model(a).selectedId());a.getOnBackPressedDispatcher().onBackPressed();});loaded(screen,55);row(0,true);screen.onActivity(a->a.getOnBackPressedDispatcher().onBackPressed());screen.onActivity(a->assertFalse(model(a).selection().getValue().active()));
  }
 }
 @Test public void actualChooserSupportsSingleZipDefaultAndDirectMultipleWithoutDeletingSources()throws Exception{
  fixtures(3,true);shares.clear();var instrumentation=InstrumentationRegistry.getInstrumentation();var monitor=chooserMonitor();instrumentation.addMonitor(monitor);
  try(var screen=ActivityScenario.launch(P0HistoryActivity.class)){
   loaded(screen,3);row(0,true);onView(withId(R.id.p05_batch_share)).perform(click());awaitShares(1);loaded(screen,3);assertEquals(Intent.ACTION_SEND,shares.get(0).getAction());assertEquals("application/gpx+xml",shares.get(0).getType());
   row(1,false);onView(withId(R.id.p05_batch_share)).perform(click());screen.recreate();onView(withId(android.R.id.button1)).perform(click());awaitShares(2);loaded(screen,3);assertEquals("application/zip",shares.get(1).getType());
   onView(withId(R.id.p05_batch_share)).perform(click());onView(withText(R.string.p05_direct_gpx)).perform(click());onView(withId(android.R.id.button1)).perform(click());awaitShares(3);assertEquals(Intent.ACTION_SEND_MULTIPLE,shares.get(2).getAction());assertEquals(2,shares.get(2).getClipData().getItemCount());
   for(var id:ids){assertNotNull(new WorkoutRepository(context).find(id));assertEquals(3,new HeartRateRepository(context).read(id).size());}count(screen,2);
  }finally{instrumentation.removeMonitor(monitor);}
 }
 @Test public void nativeDeleteConfirmationSurvivesRotationAndCancellationThenDeletesExactCount()throws Exception{
  fixtures(3,true);try(ActivityScenario<P0HistoryActivity> screen=ActivityScenario.launch(P0HistoryActivity.managementIntent(context))){
   loaded(screen,3);onView(withId(R.id.p05_batch_select_all)).perform(click());onView(withId(R.id.p05_batch_delete)).perform(click());screen.recreate();onView(withText(context.getString(R.string.p05_delete_confirm,3))).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));onView(withId(android.R.id.button2)).perform(click());for(var id:ids)assertNotNull(new WorkoutRepository(context).find(id));count(screen,3);
   onView(withId(R.id.p05_batch_delete)).perform(click());onView(withId(android.R.id.button1)).perform(click());loaded(screen,0);
   for(var id:ids){assertNull(new WorkoutRepository(context).find(id));assertTrue(new HeartRateRepository(context).read(id).isEmpty());}
  }
 }
 @Test public void protectedOrMissingMemberRejectsWholeDeleteAndDocumentFailureKeepsSources()throws Exception{
  fixtures(3,true);var store=new ViewModelStore();AtomicReference<P0HistoryViewModel> ref=new AtomicReference<>();main(()->ref.set(new ViewModelProvider(store,ViewModelProvider.AndroidViewModelFactory.getInstance((Application)context)).get(P0HistoryViewModel.class)));var m=ref.get();
  try{
   for(boolean paused:new boolean[]{false,true}){var session=RecordingSession.ready().begin(ids.get(1).id());new SessionStore(context).save(paused?session.pause():session);main(()->m.deleteSnapshot(ids.stream().map(Track.Id::id).toList()));idle(m);AtomicReference<String> message=new AtomicReference<>();main(()->message.set(m.exported().getValue().message()));assertTrue(message.get().contains(Long.toString(ids.get(1).id())));for(var id:ids)assertNotNull(new WorkoutRepository(context).find(id));new SessionStore(context).save(RecordingSession.ready());}
   List<Long> missing=new ArrayList<>(ids.stream().map(Track.Id::id).toList());missing.add(3000000000L);main(()->m.deleteSnapshot(missing));idle(m);for(var id:ids)assertNotNull(new WorkoutRepository(context).find(id));
   main(()->m.exportSnapshot(ids.stream().map(Track.Id::id).toList(),true,android.net.Uri.parse("content://com.fox.runner.nonexistent.documents/denied")));idle(m);main(()->{assertNull(m.exported().getValue().share());assertEquals(context.getString(R.string.p05_document_failed),m.exported().getValue().message());assertEquals(0,m.operation().getValue().done());});for(var id:ids){assertNotNull(new WorkoutRepository(context).find(id));assertEquals(3,new HeartRateRepository(context).read(id).size());}
  }finally{main(store::clear);}
 }
 private void idle(P0HistoryViewModel m){AtomicBoolean busy=new AtomicBoolean(true);long until=SystemClock.uptimeMillis()+15000;do{main(()->busy.set(m.busy()));if(!busy.get())return;SystemClock.sleep(30);}while(SystemClock.uptimeMillis()<until);fail("Worker did not finish");}
 @Test public void exportResultSurvivesRefreshAndActivityRecreationAndSafUsesOriginalSnapshot()throws Exception{
  fixtures(3,true);shares.clear();var instrumentation=InstrumentationRegistry.getInstrumentation();var monitor=chooserMonitor();instrumentation.addMonitor(monitor);CountDownLatch release=new CountDownLatch(1);
  try(var screen=ActivityScenario.launch(P0HistoryActivity.class)){
   loaded(screen,3);AtomicReference<P0HistoryViewModel> ref=new AtomicReference<>();screen.onActivity(a->ref.set(model(a)));var field=P0HistoryViewModel.class.getDeclaredField("worker");field.setAccessible(true);ExecutorService executor=(ExecutorService)field.get(ref.get());CountDownLatch held=new CountDownLatch(1);executor.execute(()->{held.countDown();try{release.await(10,TimeUnit.SECONDS);}catch(InterruptedException stopped){Thread.currentThread().interrupt();}});assertTrue(held.await(3,TimeUnit.SECONDS));
   screen.onActivity(a->model(a).exportSnapshot(ids.stream().map(Track.Id::id).toList(),true,null));screen.recreate();screen.onActivity(a->{assertSame(ref.get(),model(a));assertTrue(model(a).busy());model(a).refresh();});release.countDown();awaitShares(1);assertEquals("application/zip",shares.get(0).getType());loaded(screen,3);
  }finally{release.countDown();instrumentation.removeMonitor(monitor);}
  File destination=new File(context.getCacheDir(),"p0_exports/snapshot-document.zip");assertTrue(destination.isFile()||destination.createNewFile());var uri=androidx.core.content.FileProvider.getUriForFile(context,context.getPackageName()+".fileprovider",destination);
  AtomicReference<P0HistoryViewModel> current=new AtomicReference<>();var document=new android.app.Instrumentation.ActivityMonitor(){@Override public android.app.Instrumentation.ActivityResult onStartActivity(Intent i){if(!Intent.ACTION_CREATE_DOCUMENT.equals(i.getAction()))return null;assertEquals("application/zip",i.getType());assertTrue(i.hasCategory(Intent.CATEGORY_OPENABLE));current.get().exitSelection();current.get().toggle(ids.get(2).id());return new android.app.Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(uri));}};
  instrumentation.addMonitor(document);
  try(var screen=ActivityScenario.launch(P0HistoryActivity.class)){
   loaded(screen,3);screen.onActivity(a->current.set(model(a)));row(0,true);row(1,false);onView(withId(R.id.p05_batch_export)).perform(click());await(screen,m->!m.busy()&&m.operation().getValue().done()==2&&destination.length()>0);
   Set<Long> saved=new HashSet<>();try(ZipInputStream zip=new ZipInputStream(new FileInputStream(destination))){ZipEntry e;while((e=zip.getNextEntry())!=null){saved.add(Long.parseLong(e.getName().replace("Workout-","").replace(".gpx","")));zip.readAllBytes();}}
   assertEquals(Set.of(ids.get(2).id(),ids.get(1).id()),saved); // Newest two rows captured before picker; later UI change cannot replace them.
   for(var id:ids)assertNotNull(new WorkoutRepository(context).find(id));
  }finally{instrumentation.removeMonitor(document);destination.delete();}
 }
 @Test public void actualNativeHistorySelectionAndDialogsAcrossEightPalettesAt360LargeFont()throws Exception{
  fixtures(3,true);SystemThemeTestUtils.shell("wm size 720x1600");SystemThemeTestUtils.shell("wm density 320");SystemThemeTestUtils.shell("settings put system font_scale 1.3");
  for(var theme:ThemeUtils.Family.values())for(boolean dark:new boolean[]{false,true}){PreferencesUtils.setGardeniaTheme(theme);SystemThemeTestUtils.night(dark);String name="p05-history-"+theme.id+(dark?"-dark":"-light");
   try(var screen=ActivityScenario.launch(P0HistoryActivity.class)){
    loaded(screen,3);P0UiCapture.save(name+"-list-native");row(0,true);onView(withId(R.id.p05_batch_select_all)).perform(click());count(screen,3);screen.onActivity(a->{float dp=a.getResources().getDisplayMetrics().density;for(int id:new int[]{R.id.p05_batch_share,R.id.p05_batch_export,R.id.p05_batch_delete,R.id.p05_batch_select_all,R.id.p05_batch_cancel}){View v=a.findViewById(id);assertTrue(v.getWidth()/dp>=48);assertTrue(v.getHeight()/dp>=48);}assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE,a.findViewById(R.id.p05_selected_count).getAccessibilityLiveRegion());checkText(a.findViewById(android.R.id.content));});P0UiCapture.save(name+"-selection-native");
    onView(withId(R.id.p05_batch_share)).perform(click());P0UiCapture.save(name+"-share-native");onView(withId(android.R.id.button2)).perform(click());onView(withId(R.id.p05_batch_delete)).perform(click());P0UiCapture.save(name+"-delete-native");onView(withId(android.R.id.button2)).perform(click());
   }
  }
 }
 private void checkText(View view){if(!view.isShown())return;if(view instanceof TextView t&&t.getLayout()!=null)for(int i=0;i<t.getLineCount();i++)assertEquals("Clipped history control: "+t.getText(),0,t.getLayout().getEllipsisCount(i));if(view instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++)checkText(g.getChildAt(i));}
}
