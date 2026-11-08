# Validate effective night mode, not an optional persisted setting

Source 29ada73d330d528f49bc1f232fb9a7efd6973514, run 42442152346, signed job 128616439845. All full checks, helper self-test, fixed-key verification and actual signed GPS/upgrade/GPX passed again. The corrected cold-task launch reached the actual four-choice native settings page; original last-window XML and screenshot confirm it.

The first Light matrix check incorrectly required Settings.Secure.ui_night_mode to already equal the string 1. Android's default effective Light mode can exist before a persisted setting has been written; requesting an unchanged mode does not guarantee a persistence write. The observed screenshot is the proper Precision Light palette. The effective UiModeManager query is the relevant system state, and the app's rendered native palette is the relevant user-visible result.

Replace this assumption with the actual `cmd uimode night` query (no/yes), retain the raw persisted value as diagnostic only, and require the actual signed PNG's background pixel to match each authored native Light/Dark token on both settings and home (16 rendered checks). A bounded frame wait allows the real configuration recreation to finish; a wrong rendered color still fails. The existing page, choice, OS-switch, process-restart, GPS and GPX assertions remain required. Add no/yes effective-query checks to the independent API35 helper job. No production changes.

AOSP reference: https://android.googlesource.com/platform/frameworks/base/+/android-15.0.0_r1/services/core/java/com/android/server/UiModeManagerService.java (setNightModeInternal / persistNightMode).

Original diagnostic artifact 13602801033, SHA-256 50a3196e7da26a63a3c23af30471db54bc9c59b978f56a34b34830bdbd3f80db.
