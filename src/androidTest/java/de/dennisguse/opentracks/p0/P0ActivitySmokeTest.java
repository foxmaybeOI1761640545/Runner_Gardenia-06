package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import android.app.Activity;
import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.app.P0Activity;
import de.dennisguse.opentracks.util.ActivityTestUtils;

public class P0ActivitySmokeTest {
    @Test public void launcherOpensP0HomeWithoutIntroduction() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        try (ActivityScenario<Activity> launcher=ActivityScenario.launch(context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()))) {
            Activity home=ActivityTestUtils.waitForResumedActivity(P0Activity.class,java.time.Duration.ofSeconds(10));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                assertNotNull(home.findViewById(R.id.p0_title));
                assertNotNull(home.findViewById(R.id.p0_location_permission));
                assertNotNull(home.findViewById(R.id.p0_start));
                assertNotNull(home.findViewById(R.id.p0_all_history));
                for(int id:new int[]{R.id.p02_distance_value,R.id.p02_time_value,R.id.p02_pace_value,R.id.p0_heart_rate})assertNotNull(home.findViewById(id));
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            android.os.SystemClock.sleep(300);
            String configured=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
            File directory=new File(configured!=null?new File(configured):new File(context.getExternalMediaDirs()[0],"additional_test_output"),"p0-ui");
            assertTrue(directory.isDirectory()||directory.mkdirs());
            try(FileOutputStream stream=new FileOutputStream(new File(directory,"p0-home.png"))) {
                android.graphics.Bitmap screen=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                assertNotNull(screen);assertTrue(screen.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream));screen.recycle();
            }
            InstrumentationRegistry.getInstrumentation().runOnMainSync(home::finish);
        }
    }
    @Test public void activityRecreationRetainsCoreControls() {
        try(ActivityScenario<P0Activity> scenario=ActivityScenario.launch(P0Activity.class)) {
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertNotNull(activity.findViewById(R.id.p0_sport));
                assertNotNull(activity.findViewById(R.id.p0_heart_rate));
                assertNotNull(activity.findViewById(R.id.p0_finish));
            });
        }
    }
}
