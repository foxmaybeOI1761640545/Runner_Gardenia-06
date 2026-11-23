package de.dennisguse.opentracks.p0.routes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.*;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceFragmentCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.P0UiCapture;
import de.dennisguse.opentracks.p0.SystemThemeTestUtils;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.p0.routes.ui.*;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;
import org.junit.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/** Actual secondary Activities, native controls, normal system Back, file isolation and screenshot pixels. */
public class P08RouteLibraryUiTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private ThemeUtils.Family family; private String gps,heart;private int night;
    private RouteRepository repo(){return new RouteRepository(new File(context.getFilesDir(),"route-library"));}
    private void main(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    @Before public void before()throws Exception{
        family=PreferencesUtils.getGardeniaTheme();night=AppCompatDelegate.getDefaultNightMode();gps=PreferencesUtils.getString(R.string.settings_sensor_gps_key,null);heart=PreferencesUtils.getString(R.string.settings_sensor_bluetooth_heart_rate_key,null);
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();PreferencesUtils.setString(R.string.settings_sensor_gps_key,"NONE");PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,"NONE");clean();
    }
    @After public void after()throws Exception{
        main(()->{for(Stage stage:Stage.values())if(stage!=Stage.DESTROYED)for(Activity a:new ArrayList<>(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage)))if(a instanceof RouteLibraryActivity||a instanceof RouteDetailActivity||a instanceof RouteImportActivity||a instanceof P0Activity||a instanceof P0SettingsActivity)a.finish();});
        clean();PreferencesUtils.setGardeniaTheme(family);PreferencesUtils.setString(R.string.settings_sensor_gps_key,gps);PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key,heart);main(()->AppCompatDelegate.setDefaultNightMode(night));
    }
    private void clean()throws Exception{for(var entry:repo().list().entries()){assertEquals("Only known fixture routes may be removed",8,entry.id().getMostSignificantBits());repo().delete(entry.id());}}
    private void seed(int count)throws Exception{List<Route> routes=new ArrayList<>();for(int i=1;i<=count;i++)routes.add(P08RouteRepositoryPlatformTest.fixture(i));repo().add(routes,false);}
    private <T extends Activity>T current(Class<T> type){AtomicReference<T> result=new AtomicReference<>();main(()->{for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(type.isInstance(a)&&a.hasWindowFocus())result.set(type.cast(a));});return result.get();}
    private <T extends Activity>T await(Class<T> type){long end=SystemClock.elapsedRealtime()+15000;do{T result=current(type);if(result!=null)return result;SystemClock.sleep(50);}while(SystemClock.elapsedRealtime()<end);throw new AssertionError("No focused "+type.getSimpleName());}
    private RouteLibraryViewModel.State loaded(Activity activity){AtomicReference<RouteLibraryViewModel.State> value=new AtomicReference<>();long end=SystemClock.elapsedRealtime()+15000;do{main(()->value.set(new ViewModelProvider((androidx.fragment.app.FragmentActivity)activity).get(RouteLibraryViewModel.class).state().getValue()));if(!value.get().loading()&&(value.get().requested()!=null||activity instanceof RouteLibraryActivity))return value.get();SystemClock.sleep(50);}while(SystemClock.elapsedRealtime()<end);throw new AssertionError("Route UI did not load");}
    private void row(int index){onData(anything()).inAdapterView(withId(R.id.p08_route_list)).atPosition(index).perform(click());loaded(await(RouteDetailActivity.class));}
    private void back()throws Exception{SystemThemeTestUtils.shell("input keyevent 4");InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
    @Test public void emptyLibraryShowsNativeImportEntryAndNormalChildBack()throws Exception{
        try(var screen=ActivityScenario.launch(RouteLibraryActivity.class)){
            RouteLibraryActivity a=await(RouteLibraryActivity.class);loaded(a);onView(withId(R.id.p08_library_empty)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
            onView(withId(R.id.p08_library_import)).perform(click());await(RouteImportActivity.class);back();assertSame(a,await(RouteLibraryActivity.class));assertTrue(repo().list().entries().isEmpty());
        }
    }
    @Test public void persistedRowsDetailsMetadataAndReopenRemainIndependent()throws Exception{
        seed(2);try(var screen=ActivityScenario.launch(RouteLibraryActivity.class)){
            loaded(await(RouteLibraryActivity.class));screen.onActivity(a->assertEquals(2,((ListView)a.findViewById(R.id.p08_route_list)).getCount()));row(0);
            var detail=await(RouteDetailActivity.class);var value=loaded(detail);assertEquals(3,value.route().pointCount());assertEquals(2,value.route().segments().size());assertEquals("Original 1",value.route().originalName());back();assertNotNull(await(RouteLibraryActivity.class));
        }
        try(var screen=ActivityScenario.launch(RouteLibraryActivity.class)){loaded(await(RouteLibraryActivity.class));row(1);assertEquals(P08RouteRepositoryPlatformTest.fixture(2).id(),loaded(await(RouteDetailActivity.class)).route().id());}
    }
    @Test public void renameValidatesEmptyNameAndOnlyChangesLocalAlias()throws Exception{
        seed(1);var entry=repo().list().entries().get(0);File file=new File(context.getFilesDir(),"route-library/routes/"+entry.id()+".route");byte[] original=Files.readAllBytes(file.toPath());
        try(var screen=ActivityScenario.launch(RouteDetailActivity.intent(context,entry.id()))){
            loaded(await(RouteDetailActivity.class));onView(withId(R.id.p08_detail_rename)).perform(click());onView(withId(R.id.p08_rename_input)).perform(replaceText(""),closeSoftKeyboard());onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());
            onView(withId(R.id.p08_rename_input)).check((view,error)->{assertNull(error);assertNotNull(((EditText)view).getError());});onView(withId(R.id.p08_rename_input)).perform(replaceText("海边计划 — local"),closeSoftKeyboard());onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());
            loaded(await(RouteDetailActivity.class));assertEquals("海边计划 — local",repo().get(entry.id()).displayName());assertEquals("Original 1",repo().get(entry.id()).originalName());assertArrayEquals(original,Files.readAllBytes(file.toPath()));screen.recreate();assertEquals("海边计划 — local",loaded(await(RouteDetailActivity.class)).entry().displayName());
        }
    }
    @Test public void deleteConfirmationRetainsOnCancelAndDeletesOnlySelectedRoute()throws Exception{
        seed(2);UUID first=P08RouteRepositoryPlatformTest.fixture(1).id(),second=P08RouteRepositoryPlatformTest.fixture(2).id();String otherHash=repo().list().entries().get(1).fileSha256();
        try(var screen=ActivityScenario.launch(RouteLibraryActivity.class)){
            loaded(await(RouteLibraryActivity.class));row(0);onView(withId(R.id.p08_detail_delete)).perform(click());onView(withId(android.R.id.button2)).inRoot(isDialog()).perform(click());assertNotNull(repo().get(first));
            onView(withId(R.id.p08_detail_delete)).perform(click());onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());loaded(await(RouteLibraryActivity.class));assertNull(repo().get(first));assertNotNull(repo().get(second));assertEquals(otherHash,repo().list().entries().get(0).fileSha256());
        }
    }
    @Test public void corruptRouteShowsErrorAndCanBeExplicitlyDeleted()throws Exception{
        seed(1);var id=P08RouteRepositoryPlatformTest.fixture(1).id();Files.write(new File(context.getFilesDir(),"route-library/routes/"+id+".route").toPath(),new byte[]{1,2,3});
        try(var screen=ActivityScenario.launch(RouteDetailActivity.intent(context,id))){
            var a=await(RouteDetailActivity.class);var value=loaded(a);assertNotNull(value.error());assertNull(value.route());screen.onActivity(d->{assertFalse(d.findViewById(R.id.p08_detail_rename).isEnabled());assertTrue(d.findViewById(R.id.p08_detail_delete).isEnabled());});onView(withId(R.id.p08_detail_delete)).perform(click());onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());
            long end=SystemClock.elapsedRealtime()+10000;while(!repo().list().entries().isEmpty()&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(50);assertTrue(repo().list().entries().isEmpty());
        }
    }
    @Test public void unknownOrDeletedUuidIsExplicitAndCannotMutateOtherRoutes()throws Exception{
        seed(1);try(var screen=ActivityScenario.launch(RouteDetailActivity.intent(context,UUID.randomUUID()))){var value=loaded(await(RouteDetailActivity.class));assertNull(value.entry());screen.onActivity(a->{assertFalse(a.findViewById(R.id.p08_detail_rename).isEnabled());assertFalse(a.findViewById(R.id.p08_detail_delete).isEnabled());});assertEquals(1,repo().list().entries().size());}
    }
    @Test public void listRecreationRetainsScrolledPosition()throws Exception{
        seed(30);try(var screen=ActivityScenario.launch(RouteLibraryActivity.class)){
            loaded(await(RouteLibraryActivity.class));screen.onActivity(a->((ListView)a.findViewById(R.id.p08_route_list)).setSelectionFromTop(20,0));SystemClock.sleep(250);int[] before={0};screen.onActivity(a->before[0]=((ListView)a.findViewById(R.id.p08_route_list)).getFirstVisiblePosition());assertTrue(before[0]>=18);
            screen.recreate();loaded(await(RouteLibraryActivity.class));SystemClock.sleep(250);screen.onActivity(a->assertEquals(before[0],((ListView)a.findViewById(R.id.p08_route_list)).getFirstVisiblePosition()));
        }
    }
    @Test public void homeLibraryDetailAndInternalBackReturnToExactCaller()throws Exception{
        seed(1);try(var home=ActivityScenario.launch(P0Activity.class)){
            var root=await(P0Activity.class);onView(withId(R.id.p08_home_routes)).perform(click());var library=await(RouteLibraryActivity.class);loaded(library);row(0);back();assertSame(library,await(RouteLibraryActivity.class));back();assertSame(root,await(P0Activity.class));
            onView(withId(R.id.p08_home_routes)).perform(click());loaded(await(RouteLibraryActivity.class));back();assertSame(root,await(P0Activity.class));
        }
    }
    @Test public void settingsLibraryBackRetainsPrivacyCategoryAndFourRoots()throws Exception{
        try(var settings=ActivityScenario.launch(P0SettingsActivity.class)){
            var root=await(P0SettingsActivity.class);main(()->{root.openCategory("privacy");root.getSupportFragmentManager().executePendingTransactions();});
            onView(withText(R.string.p08_library_title)).perform(click());loaded(await(RouteLibraryActivity.class));back();assertSame(root,await(P0SettingsActivity.class));
            main(()->{var fragment=(P05SettingsSectionFragment)root.getSupportFragmentManager().findFragmentById(R.id.gardenia_settings_container);assertEquals("privacy",fragment.category());assertTrue(root.findViewById(R.id.p03_settings).isSelected());});
        }
    }
    @Test public void rapidLibraryOpenCloseAndRecreateNeverDuplicatesSecondaryInstance()throws Exception{
        try(var home=ActivityScenario.launch(P0Activity.class)){
            for(int i=0;i<4;i++){
                var root=await(P0Activity.class);main(()->{for(int n=0;n<5;n++)root.findViewById(R.id.p08_home_routes).performClick();});var library=await(RouteLibraryActivity.class);loaded(library);
                main(()->{int count=0;for(Stage stage:Stage.values())if(stage!=Stage.DESTROYED)for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage))if(a instanceof RouteLibraryActivity)count++;assertEquals(1,count);library.recreate();});loaded(await(RouteLibraryActivity.class));back();assertSame(root,await(P0Activity.class));
            }
        }
    }
    @Test public void listOverflowRenameUsesConfirmedNativeLocalAlias()throws Exception{
        seed(1);try(var screen=ActivityScenario.launch(RouteLibraryActivity.class)){
            loaded(await(RouteLibraryActivity.class));onView(withId(R.id.p08_route_more)).perform(click());onView(withText(R.string.p08_rename_route)).perform(click());await(RouteDetailActivity.class);onView(withId(R.id.p08_rename_input)).perform(replaceText("From list"),closeSoftKeyboard());onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());loaded(await(RouteDetailActivity.class));assertEquals("From list",repo().get(P08RouteRepositoryPlatformTest.fixture(1).id()).displayName());
        }
    }
    @Test public void detailRecreationRetainsLongDescriptionScrollPosition()throws Exception{
        Route base=P08RouteRepositoryPlatformTest.fixture(1);Route longRoute=new Route(base.id(),base.displayName(),base.sourceFileName(),base.sourceSha256(),base.sourceOrdinal(),base.sourceType(),base.importedAtMillis(),Map.of("name","Original 1","desc","Synthetic description. ".repeat(80)),base.segments());repo().add(List.of(longRoute),false);
        try(var screen=ActivityScenario.launch(RouteDetailActivity.intent(context,base.id()))){
            loaded(await(RouteDetailActivity.class));screen.onActivity(a->((ScrollView)a.findViewById(R.id.p08_detail_scroll)).fullScroll(android.view.View.FOCUS_DOWN));SystemClock.sleep(250);int[] before={0};screen.onActivity(a->before[0]=((ScrollView)a.findViewById(R.id.p08_detail_scroll)).getScrollY());assertTrue(before[0]>100);
            screen.recreate();loaded(await(RouteDetailActivity.class));SystemClock.sleep(250);screen.onActivity(a->assertEquals(before[0],((ScrollView)a.findViewById(R.id.p08_detail_scroll)).getScrollY(),2));
        }
    }
    @Test public void nativeVerifyAndRepairShowCorruptionWithoutBlessingChangedBytes()throws Exception{
        seed(1);UUID id=P08RouteRepositoryPlatformTest.fixture(1).id();File file=new File(context.getFilesDir(),"route-library/routes/"+id+".route");byte[] bytes=Files.readAllBytes(file.toPath());bytes[20]^=1;Files.write(file.toPath(),bytes);
        try(var screen=ActivityScenario.launch(RouteLibraryActivity.class)){
            loaded(await(RouteLibraryActivity.class));openActionBarOverflowOrOptionsMenu(context);onView(withText(R.string.p08_verify_files)).perform(click());assertFalse(loaded(await(RouteLibraryActivity.class)).problems().isEmpty());
            openActionBarOverflowOrOptionsMenu(context);onView(withText(R.string.p08_repair_index)).perform(click());onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());assertFalse(loaded(await(RouteLibraryActivity.class)).problems().isEmpty());assertArrayEquals(bytes,Files.readAllBytes(file.toPath()));
            try{repo().get(id);fail("Corrupt bytes cannot become valid by UI repair");}catch(IOException expected){}
        }
    }
    @Test public void nativeLibraryAndDetailScreensAllFourThemesBothModes()throws Exception{
        seed(2);var captures=new org.json.JSONArray();
        for(var theme:ThemeUtils.Family.values())for(int mode:new int[]{AppCompatDelegate.MODE_NIGHT_NO,AppCompatDelegate.MODE_NIGHT_YES}){
            PreferencesUtils.setGardeniaTheme(theme);main(()->AppCompatDelegate.setDefaultNightMode(mode));String suffix=theme.id+(mode==AppCompatDelegate.MODE_NIGHT_YES?"-dark":"-light");
            try(var screen=ActivityScenario.launch(RouteLibraryActivity.class)){loaded(await(RouteLibraryActivity.class));capture("p08-library-"+suffix,captures,theme.id,mode);row(0);capture("p08-detail-"+suffix,captures,theme.id,mode);back();await(RouteLibraryActivity.class);}
        }
        String output=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");File base=output==null?new File(context.getExternalMediaDirs()[0],"additional_test_output"):new File(output);File folder=new File(base,"p08-library");assertTrue(folder.isDirectory()||folder.mkdirs());var proof=new org.json.JSONObject().put("source",InstrumentationRegistry.getArguments().getString("buildCommit")).put("api",android.os.Build.VERSION.SDK_INT).put("captures",captures).put("visualReview","PENDING_ACTUAL_REVIEW");Files.write(new File(folder,"screenshots.json").toPath(),proof.toString(2).getBytes(StandardCharsets.UTF_8));
    }
    private void capture(String name,org.json.JSONArray captures,String theme,int mode)throws Exception{
        P0UiCapture.save(name);String output=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");File base=output==null?new File(context.getExternalMediaDirs()[0],"additional_test_output"):new File(output);File image=new File(base,"p0-ui/"+name+".png");captures.put(new org.json.JSONObject().put("file",image.getName()).put("sha256",RouteCodec.sha256(Files.readAllBytes(image.toPath()))).put("theme",theme).put("mode",mode).put("native",true));
    }
}
