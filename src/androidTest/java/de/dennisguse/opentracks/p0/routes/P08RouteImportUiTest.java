package de.dennisguse.opentracks.p0.routes;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.view.View;
import android.widget.CheckBox;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.intent.Intents;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;
import de.dennisguse.opentracks.data.CustomContentProvider;
import de.dennisguse.opentracks.p0.P0UiCapture;
import de.dennisguse.opentracks.p0.routes.ui.*;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class P08RouteImportUiTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private ThemeUtils.Family family; private int night; private String gps, heart;
    private RouteRepository repo() { return new RouteRepository(new File(context.getFilesDir(), "route-library")); }
    private RouteImportViewModel model(RouteImportActivity a) { return new ViewModelProvider(a).get(RouteImportViewModel.class); }
    @Before public void before() throws Exception {
        family = PreferencesUtils.getGardeniaTheme(); night = AppCompatDelegate.getDefaultNightMode(); gps = PreferencesUtils.getString(R.string.settings_sensor_gps_key, null); heart = PreferencesUtils.getString(R.string.settings_sensor_bluetooth_heart_rate_key, null);
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks(); PreferencesUtils.setString(R.string.settings_sensor_gps_key, "NONE"); PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key, ""); clean();
    }
    private void clean() throws Exception {
        for (var row : repo().list().entries()) { assertTrue("Unknown route must not be deleted by test", row.sourceFileName() == null || row.sourceFileName().startsWith("p08-")); repo().delete(row.id()); }
    }
    @After public void after() throws Exception {
        clean(); PreferencesUtils.setGardeniaTheme(family); PreferencesUtils.setString(R.string.settings_sensor_gps_key, gps); PreferencesUtils.setString(R.string.settings_sensor_bluetooth_heart_rate_key, heart);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> AppCompatDelegate.setDefaultNightMode(night));
    }
    private RouteImportViewModel.State state(ActivityScenario<RouteImportActivity> screen) { AtomicReference<RouteImportViewModel.State> value = new AtomicReference<>(); screen.onActivity(a -> value.set(model(a).state().getValue())); return value.get(); }
    private RouteImportViewModel.State waitFor(ActivityScenario<RouteImportActivity> screen, RouteImportViewModel.Phase phase) {
        long deadline = SystemClock.elapsedRealtime() + 15000; RouteImportViewModel.State current;
        do { current = state(screen); if (current.phase() == phase) return current; SystemClock.sleep(50); } while (SystemClock.elapsedRealtime() < deadline);
        fail("Expected " + phase + " got " + current); return null;
    }
    private void choose(String name, int resultCode) {
        Intents.init(); try {
            Intent result = new Intent().setData(Uri.parse("content://com.fox.runner.p08.fixture/" + name)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(new Instrumentation.ActivityResult(resultCode, result)); onView(withId(R.id.p08_import_choose)).perform(click());
            Intents.intended(allOf(hasAction(Intent.ACTION_OPEN_DOCUMENT), androidx.test.espresso.intent.matcher.IntentMatchers.hasType("*/*")));
        } finally { Intents.release(); }
    }
    private void select(ActivityScenario<RouteImportActivity> screen, int ordinal) { onView(withTagValue(is((Object)("p08-candidate-" + ordinal)))).perform(androidx.test.espresso.action.ViewActions.scrollTo(), click()); }
    @Test public void safPipeUnknownMimeMultipleRoutesSelectionPersistsOnlyChosenRoutes() throws Exception {
        try (var screen = ActivityScenario.launch(RouteImportActivity.class)) {
            choose("p08-multi.any", Activity.RESULT_OK); var preview = waitFor(screen, RouteImportViewModel.Phase.PREVIEW); assertEquals(3, preview.preview().validRoutes()); assertTrue(repo().list().entries().isEmpty());
            select(screen, 1); select(screen, 3); onView(withId(R.id.p08_import_commit)).perform(click()); var success = waitFor(screen, RouteImportViewModel.Phase.SUCCESS); assertEquals(2, success.imported().size());
            var saved = repo().list().entries(); assertEquals(2, saved.size()); assertEquals(List.of(1, 3), saved.stream().map(RouteEntry::sourceOrdinal).toList()); assertEquals(2, repo().get(saved.get(1).id()).segments().size());
            try (var cursor = context.getContentResolver().query(CustomContentProvider.UrlType.TRACKS.getUri(), null, null, null, null)) { assertNotNull(cursor); assertEquals(0, cursor.getCount()); }
        }
    }
    @Test public void partialInvalidCandidateIsExplicitDisabledAndNeverImported() throws Exception {
        try (var screen = ActivityScenario.launch(RouteImportActivity.class)) {
            choose("p08-mixed.gpx", Activity.RESULT_OK); var p = waitFor(screen, RouteImportViewModel.Phase.PREVIEW); assertEquals(3, p.preview().candidates().size()); assertEquals(2, p.preview().validRoutes());
            screen.onActivity(a -> { CheckBox invalid = a.findViewById(R.id.p08_import_candidates).findViewWithTag("p08-candidate-2"); assertFalse(invalid.isEnabled()); assertTrue(invalid.getText().toString().contains("Invalid latitude")); model(a).selected(2, true); }); assertTrue(state(screen).selected().isEmpty());
            select(screen, 3); onView(withId(R.id.p08_import_commit)).perform(click()); waitFor(screen, RouteImportViewModel.Phase.SUCCESS); assertEquals(3, repo().list().entries().get(0).sourceOrdinal());
        }
    }
    @Test public void duplicateNeedsExplicitCopyConfirmationAndNeverOverwrites() throws Exception {
        try (var screen = ActivityScenario.launch(RouteImportActivity.class)) {
            choose("p08-multi.any", Activity.RESULT_OK); waitFor(screen, RouteImportViewModel.Phase.PREVIEW); select(screen, 1); onView(withId(R.id.p08_import_commit)).perform(click()); waitFor(screen, RouteImportViewModel.Phase.SUCCESS);
        }
        UUID first = repo().list().entries().get(0).id();
        try (var screen = ActivityScenario.launch(RouteImportActivity.class)) {
            choose("p08-multi.any", Activity.RESULT_OK); var p = waitFor(screen, RouteImportViewModel.Phase.PREVIEW); assertEquals(1, p.duplicates().size()); select(screen, 1); onView(withId(R.id.p08_import_commit)).perform(click());
            onView(withText(R.string.p08_duplicate_title)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed())); onView(withId(android.R.id.button2)).perform(click()); assertEquals(1, repo().list().entries().size());
            onView(withId(R.id.p08_import_commit)).perform(click()); onView(withId(android.R.id.button1)).perform(click()); waitFor(screen, RouteImportViewModel.Phase.SUCCESS); assertEquals(2, repo().list().entries().size()); assertNotNull(repo().get(first)); assertNotEquals(first, repo().list().entries().get(1).id());
        }
    }
    @Test public void rotationRetainsPreviewSelectionAndOriginalProviderNeedNotBeReopened() throws Exception {
        try (var screen = ActivityScenario.launch(RouteImportActivity.class)) {
            choose("p08-multi.any", Activity.RESULT_OK); var before = waitFor(screen, RouteImportViewModel.Phase.PREVIEW); select(screen, 3); screen.recreate(); var after = waitFor(screen, RouteImportViewModel.Phase.PREVIEW);
            assertSame(before.preview(), after.preview()); assertEquals(Set.of(3), after.selected()); onView(withId(R.id.p08_import_commit)).perform(click()); waitFor(screen, RouteImportViewModel.Phase.SUCCESS); assertEquals(1, repo().list().entries().size());
        }
    }
    @Test public void pickerCancelLeavesNoDataAndNoError() throws Exception {
        try (var screen = ActivityScenario.launch(RouteImportActivity.class)) { choose("p08-multi.any", Activity.RESULT_CANCELED); assertEquals(RouteImportViewModel.Phase.EMPTY, state(screen).phase()); assertTrue(repo().list().entries().isEmpty()); }
    }
    @Test public void cancelledStreamingReadNeverCreatesFormalRoutesAndAllowsAnotherFile() throws Exception {
        try (var screen = ActivityScenario.launch(RouteImportActivity.class)) {
            choose("p08-slow.gpx", Activity.RESULT_OK); waitFor(screen, RouteImportViewModel.Phase.READING); onView(withId(R.id.p08_import_choose)).perform(click()); waitFor(screen, RouteImportViewModel.Phase.CANCELLED); assertTrue(repo().list().entries().isEmpty());
            choose("p08-multi.any", Activity.RESULT_OK); waitFor(screen, RouteImportViewModel.Phase.PREVIEW); assertTrue(repo().list().entries().isEmpty());
        }
    }
    @Test public void malformedEmptyInterruptedMissingAndWaypointOnlyFilesCannotCommit() throws Exception {
        for (String name : List.of("p08-malformed.gpx", "p08-empty.gpx", "p08-interrupted.gpx", "p08-missing.gpx", "p08-wpt.gpx")) try (var screen = ActivityScenario.launch(RouteImportActivity.class)) {
            choose(name, Activity.RESULT_OK); var error = waitFor(screen, RouteImportViewModel.Phase.ERROR); assertNotNull(error.error()); screen.onActivity(a -> assertFalse(a.findViewById(R.id.p08_import_commit).isEnabled())); assertTrue(repo().list().entries().isEmpty());
        }
    }
    @Test public void absentDisplayNameAndPickerExceptionAreRecoverable() throws Exception {
        try (var screen = ActivityScenario.launch(RouteImportActivity.class)) {
            screen.onActivity(a -> model(a).pickerFailed(new android.content.ActivityNotFoundException("Synthetic missing picker"))); assertEquals(RouteImportViewModel.Phase.ERROR, state(screen).phase());
            choose("p08-metadata.gpx", Activity.RESULT_OK); var p = waitFor(screen, RouteImportViewModel.Phase.PREVIEW); assertNull(p.preview().fileName()); select(screen, 2); onView(withId(R.id.p08_import_commit)).perform(click()); waitFor(screen, RouteImportViewModel.Phase.SUCCESS); assertNull(repo().list().entries().get(0).sourceFileName());
        }
    }
    @Test public void repeatedCommitClicksCannotImportDuplicateOrReopenCompletedSelection() throws Exception {
        try (var screen = ActivityScenario.launch(RouteImportActivity.class)) {
            choose("p08-multi.any", Activity.RESULT_OK); waitFor(screen, RouteImportViewModel.Phase.PREVIEW); select(screen, 1);
            screen.onActivity(a -> { model(a).commit(false); model(a).commit(false); }); waitFor(screen, RouteImportViewModel.Phase.SUCCESS); screen.onActivity(a -> model(a).commit(false)); assertEquals(1, repo().list().entries().size());
        }
    }
    @Test public void nativeImportScreensAllFourThemesBothModes() throws Exception {
        var captures = new org.json.JSONArray();
        for (var theme : ThemeUtils.Family.values()) for (int mode : new int[]{AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES}) {
            PreferencesUtils.setGardeniaTheme(theme); InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> AppCompatDelegate.setDefaultNightMode(mode));
            try (var screen = ActivityScenario.launch(RouteImportActivity.class)) {
                choose("p08-mixed.gpx", Activity.RESULT_OK); waitFor(screen, RouteImportViewModel.Phase.PREVIEW); select(screen, 1); SystemClock.sleep(200);
                String name = "p08-import-" + theme.id + (mode == AppCompatDelegate.MODE_NIGHT_YES ? "-dark" : "-light"); P0UiCapture.save(name);
                String output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir"); File base = output == null ? new File(context.getExternalMediaDirs()[0], "additional_test_output") : new File(output);
                File image = new File(base, "p0-ui/" + name + ".png"); captures.put(new org.json.JSONObject().put("file", image.getName()).put("sha256", RouteCodec.sha256(Files.readAllBytes(image.toPath()))).put("theme", theme.id).put("mode", mode).put("native", true));
            }
        }
        String output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir"); File base = output == null ? new File(context.getExternalMediaDirs()[0], "additional_test_output") : new File(output); File folder = new File(base, "p08-import"); assertTrue(folder.isDirectory() || folder.mkdirs());
        var proof = new org.json.JSONObject().put("source", InstrumentationRegistry.getArguments().getString("buildCommit")).put("api", android.os.Build.VERSION.SDK_INT).put("captures", captures).put("visualReview", "PENDING_ACTUAL_REVIEW"); Files.write(new File(folder, "screenshots.json").toPath(), proof.toString(2).getBytes(StandardCharsets.UTF_8));
    }
}
