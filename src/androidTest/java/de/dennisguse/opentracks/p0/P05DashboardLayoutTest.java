package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.app.DashboardPreviewActivity;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;

/** Real API35 pixels and geometry. Every displayed workout value is an androidTest-only fixture. */
public class P05DashboardLayoutTest {
    private static final int[][] VIEWPORTS = {{360, 800}, {393, 873}, {412, 915}};
    private static final float[] FONT_SCALES = {1.0f, 1.3f};
    private static final String[] ANIMATIONS = {
            "window_animation_scale", "transition_animation_scale", "animator_duration_scale"};
    private final Context context = ApplicationProvider.getApplicationContext();
    private final JSONArray captures = new JSONArray();
    private final String[] animationValues = new String[ANIMATIONS.length];
    private ThemeUtils.Family previousFamily;
    private String previousNight, previousSize, previousDensity, previousFont;

    @Before public void saveSystemConfiguration() throws Exception {
        assertEquals("This acceptance requires an actual API35 emulator", 35, Build.VERSION.SDK_INT);
        previousFamily = PreferencesUtils.getGardeniaTheme();
        previousNight = SystemThemeTestUtils.savedMode();
        previousSize = SystemThemeTestUtils.shell("wm size");
        previousDensity = SystemThemeTestUtils.shell("wm density");
        previousFont = SystemThemeTestUtils.shell("settings get system font_scale");
        for (int index = 0; index < ANIMATIONS.length; index++) {
            animationValues[index] = SystemThemeTestUtils.shell("settings get global " + ANIMATIONS[index]);
            SystemThemeTestUtils.shell("settings put global " + ANIMATIONS[index] + " 0");
        }
    }

    @After public void restoreSystemConfiguration() throws Exception {
        if (previousFamily != null) PreferencesUtils.setGardeniaTheme(previousFamily);
        if (previousNight != null) SystemThemeTestUtils.restore(previousNight);
        if (previousFont != null) SystemThemeTestUtils.shell("settings put system font_scale "
                + (previousFont.equals("null") ? "1.0" : previousFont));
        restoreOverride("size", previousSize);
        restoreOverride("density", previousDensity);
        for (int index = 0; index < ANIMATIONS.length; index++) {
            if (animationValues[index] != null) SystemThemeTestUtils.shell(
                    animationValues[index].equals("null")
                            ? "settings delete global " + ANIMATIONS[index]
                            : "settings put global " + ANIMATIONS[index] + " " + animationValues[index]);
        }
    }

    private static void restoreOverride(String kind, String saved) throws Exception {
        if (saved == null) return;
        var match = Pattern.compile("Override (?:size|density): ([^\\r\\n]+)").matcher(saved);
        SystemThemeTestUtils.shell("wm " + kind + " " + (match.find() ? match.group(1).trim() : "reset"));
    }

    @Test public void fourNativeFamiliesBothModesThreeWidthsAndTwoFontScales() throws Exception {
        for (int[] viewport : VIEWPORTS) {
            SystemThemeTestUtils.shell("wm size " + (viewport[0] * 2) + "x" + (viewport[1] * 2));
            SystemThemeTestUtils.shell("wm density 320");
            for (float fontScale : FONT_SCALES) {
                SystemThemeTestUtils.shell("settings put system font_scale " + fontScale);
                for (ThemeUtils.Family family : ThemeUtils.Family.values()) {
                    for (boolean dark : new boolean[]{false, true}) {
                        captureProfile(viewport, fontScale, family, dark);
                    }
                }
            }
        }
        assertEquals("Every requested native layout profile must execute", 48, captures.length());
        JSONObject proof = new JSONObject()
                .put("passed", true)
                .put("sourceCommit", InstrumentationRegistry.getArguments().getString("buildCommit", "local"))
                .put("api", Build.VERSION.SDK_INT)
                .put("native", true)
                .put("policy", "FOLLOW_SYSTEM")
                .put("productionFixtureEntry", false)
                .put("physicalBleTested", false)
                .put("screenshots", captures);
        try (FileOutputStream stream = new FileOutputStream(new File(output(), "p05-layout-matrix.json"))) {
            stream.write(proof.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }

    private void captureProfile(int[] viewport, float fontScale, ThemeUtils.Family family, boolean dark)
            throws Exception {
        PreferencesUtils.setGardeniaTheme(family);
        SystemThemeTestUtils.night(dark);
        try (ActivityScenario<DashboardPreviewActivity> screen = ActivityScenario.launch(DashboardPreviewActivity.class)) {
            awaitConfiguration(screen, viewport[0], fontScale, dark);
            screen.onActivity(activity -> {
                DashboardFixture.present(activity, "idle");
                ((ScrollView) activity.findViewById(R.id.p02_home_scroll)).scrollTo(0, 0);
                RadioButton run = activity.findViewById(R.id.p0_running);
                RadioButton cycle = activity.findViewById(R.id.p0_cycling);
                assertTrue(run.isChecked());
                assertFalse(cycle.isChecked());
                assertTrue(cycle.performClick());
                assertTrue(cycle.isChecked());
                assertFalse(run.isChecked());
                assertTrue(run.performClick());
                assertTrue(run.isChecked());
                assertFalse(cycle.isChecked());
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            AtomicReference<JSONObject> bounds = new AtomicReference<>();
            screen.onActivity(activity -> bounds.set(checkCompactLayout(activity)));
            String name = "p05-layout-" + family.id + (dark ? "-dark-" : "-light-")
                    + viewport[0] + "x" + viewport[1] + "-font-" + Math.round(fontScale * 100)
                    + "-idle-synthetic";
            P0UiCapture.save(name);
            byte[] png = Files.readAllBytes(new File(output(), name + ".png").toPath());
            captures.put(new JSONObject().put("file", name + ".png")
                    .put("family", family.id).put("appearance", dark ? "dark" : "light")
                    .put("widthDp", viewport[0]).put("heightDp", viewport[1])
                    .put("fontScale", Math.round(fontScale * 100) / 100d).put("state", "idle")
                    .put("sha256", sha256(png)).put("syntheticUiFixture", true)
                    .put("bounds", bounds.get()));
            screen.onActivity(P05DashboardLayoutTest::checkStatusSemantics);
        }
    }

    private static void awaitConfiguration(ActivityScenario<DashboardPreviewActivity> screen,
                                           int width, float fontScale, boolean dark) {
        AtomicBoolean ready = new AtomicBoolean();
        long deadline = SystemClock.uptimeMillis() + 15_000;
        do {
            screen.onActivity(activity -> ready.set(activity.hasWindowFocus()
                    && ThemeUtils.isNight(activity) == dark
                    && activity.getResources().getConfiguration().screenWidthDp == width
                    && Math.abs(activity.getResources().getConfiguration().fontScale - fontScale) < .01f));
            if (ready.get()) break;
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < deadline);
        assertTrue("Actual OS mode, width and font scale never reached the native Activity", ready.get());
    }

    private static JSONObject checkCompactLayout(DashboardPreviewActivity activity) {
        assertEquals("zh", activity.getResources().getConfiguration().getLocales().get(0).getLanguage());
        float density = activity.getResources().getDisplayMetrics().density;
        TextView title = activity.findViewById(R.id.p04_page_title);
        ViewGroup header = (ViewGroup) title.getParent();
        assertEquals("The removed theme row must leave no empty View or placeholder", 2, header.getChildCount());
        assertSame(activity.findViewById(R.id.p0_title), header.getChildAt(0));
        assertSame(title, header.getChildAt(1));
        int oldId = activity.getResources().getIdentifier("p03_family_label", "id", activity.getPackageName());
        if (oldId != 0) assertNull("Theme label must be removed from the native hierarchy", activity.findViewById(oldId));

        ViewGroup row = activity.findViewById(R.id.p05_compact_controls);
        Rect rowBounds = bounds(row);
        Rect titleBounds = bounds(title);
        int gap = rowBounds.top - titleBounds.bottom;
        assertTrue("The former theme-name region must collapse to the header's 6dp padding", gap >= 0 && gap <= density * 6 + 1);
        assertEquals("Sport and status controls must share one native horizontal row", 3, row.getChildCount());
        int[] ids = {R.id.p0_running, R.id.p0_cycling, R.id.p04_gps_status, R.id.p0_devices};
        JSONArray controls = new JSONArray();
        Rect previous = null;
        for (int id : ids) {
            TextView control = activity.findViewById(id);
            assertTrue("Compact control must remain visible", control.isShown());
            assertTrue("Compact control must be clickable", control.isClickable());
            assertTrue("Compact control must accept keyboard focus", control.isFocusable());
            Rect rectangle = bounds(control);
            assertTrue("Touch target is narrower than 48dp", rectangle.width() / density >= 47.5f);
            assertTrue("Touch target is shorter than 48dp", rectangle.height() / density >= 47.5f);
            assertTrue("Compact control extends outside row", rowBounds.contains(rectangle));
            assertTrue("All four controls must stay on the same horizontal line",
                    Math.abs(rectangle.exactCenterY() - rowBounds.exactCenterY()) <= density + 1);
            if (previous != null) assertTrue("Compact controls overlap", previous.right <= rectangle.left);
            previous = rectangle;
            checkCompleteText(control);
            try {
                controls.put(new JSONObject().put("id", activity.getResources().getResourceEntryName(id))
                        .put("left", rectangle.left).put("top", rectangle.top)
                        .put("width", rectangle.width()).put("height", rectangle.height())
                        .put("text", control.getText()));
            } catch (JSONException error) { throw new AssertionError(error); }
        }
        checkHorizontal(activity.findViewById(R.id.p03_content));
        try {
            return new JSONObject().put("titleBottom", titleBounds.bottom).put("rowTop", rowBounds.top)
                    .put("rowHeight", rowBounds.height()).put("headerChildCount", header.getChildCount())
                    .put("controls", controls);
        } catch (JSONException error) { throw new AssertionError(error); }
    }

    private static Rect bounds(View view) {
        Rect rectangle = new Rect();
        assertTrue("Required native control is not visible", view.getGlobalVisibleRect(rectangle));
        assertEquals("Required native control is vertically clipped", view.getHeight(), rectangle.height());
        assertEquals("Required native control is horizontally clipped", view.getWidth(), rectangle.width());
        return rectangle;
    }

    private static void checkCompleteText(TextView view) {
        var layout = view.getLayout();
        assertNotNull("Text layout must exist", layout);
        assertTrue("Compact labels cannot be empty", view.getText().length() > 0);
        assertTrue("Text must have at least one visible line", layout.getLineCount() > 0);
        assertEquals("The end of a compact label was clipped", view.getText().length(),
                layout.getLineEnd(layout.getLineCount() - 1));
        for (int line = 0; line < layout.getLineCount(); line++) {
            assertEquals("Compact label was ellipsized", 0, layout.getEllipsisCount(line));
            assertTrue("Compact label overflows its text layout", layout.getLineWidth(line) <= layout.getWidth() + 1);
        }
        assertTrue("Compact label is vertically clipped", layout.getHeight()
                <= view.getHeight() - view.getCompoundPaddingTop() - view.getCompoundPaddingBottom() + 1);
    }

    private static void checkHorizontal(View view) {
        if (!view.isShown()) return;
        if (view instanceof ViewGroup group) {
            for (int index = 0; index < group.getChildCount(); index++) {
                View child = group.getChildAt(index);
                if (child.isShown()) {
                    assertTrue("Native dashboard overflows horizontally", child.getLeft() >= -1
                            && child.getRight() <= group.getWidth() + 1);
                    checkHorizontal(child);
                }
            }
        }
    }

    private static void checkStatusSemantics(DashboardPreviewActivity activity) {
        TextView gps = activity.findViewById(R.id.p04_gps_status);
        TextView heart = activity.findViewById(R.id.p0_devices);
        DashboardFixture.present(activity, "idle");
        String unavailable = gps.getText().toString();
        String disconnected = heart.getText().toString();
        String disconnectedDescription = description(heart);
        assertTrue(description(gps).contains(activity.getString(R.string.p04_gps_unavailable)));
        DashboardFixture.present(activity, "gps-searching");
        assertNotEquals("GPS must expose its actual acquisition state in text", unavailable, gps.getText().toString());
        assertTrue(description(gps).contains(activity.getString(R.string.p04_gps_searching)));
        DashboardFixture.present(activity, "recording");
        assertNotEquals("GPS must expose the actual fix state in text", unavailable, gps.getText().toString());
        assertTrue(description(gps).contains(activity.getString(R.string.p04_gps_fixed)));
        assertNotEquals("Heart status cannot depend only on color", disconnected, heart.getText().toString());
        assertNotEquals("Heart accessibility description must follow live connection state",
                disconnectedDescription, description(heart));
        DashboardFixture.present(activity, "disconnected");
        assertEquals(disconnected, heart.getText().toString());
        assertEquals(disconnectedDescription, description(heart));
    }

    private static String description(TextView view) {
        assertNotNull("State action needs a dynamic accessibility description", view.getContentDescription());
        String description = view.getContentDescription().toString();
        assertTrue("Accessibility description must explain more than the short label",
                description.length() > view.getText().length());
        return description;
    }

    private File output() {
        String configured = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
        return new File(configured != null ? new File(configured)
                : new File(context.getExternalMediaDirs()[0], "additional_test_output"), "p0-ui");
    }

    private static String sha256(byte[] raw) throws Exception {
        StringBuilder result = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-256").digest(raw))
            result.append(String.format(Locale.ROOT, "%02x", value & 255));
        return result.toString();
    }
}
