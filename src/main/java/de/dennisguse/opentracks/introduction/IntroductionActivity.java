package de.dennisguse.opentracks.introduction;

import android.app.Activity;
import android.os.Bundle;

import androidx.core.splashscreen.SplashScreen;

import de.dennisguse.opentracks.TrackListActivity;
import de.dennisguse.opentracks.util.IntentUtils;

/** Keeps the existing launcher component compatible with desktop icons and shortcuts. */
public class IntroductionActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Route directly to the home screen without ever drawing the old introduction.
        splashScreen.setKeepOnScreenCondition(() -> true);
        startActivity(IntentUtils.newIntent(this, TrackListActivity.class));
        finish();
    }
}
