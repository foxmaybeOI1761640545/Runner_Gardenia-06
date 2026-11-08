# Signed install acceptance failure — original record retained

Source: `91dbbfc02efe1c74f0c3667388634b452680ca7e`; run 42437343392; signed job 128602485088; diagnostic artifact 13599764651, SHA-256 `c451c7bc52cac19ad91b491fd2a1c361970a445574678822eb94c455a38b2a11`.

All full checks passed: 404 executed Android tests (one existing ignored), 113 JVM tests, 68 helpers, native eight-scheme/56-PNG matrix, GPX schema and separate-process durability. R8, resource optimization, APK version/alignment and fixed certificate verification passed. Signed runtime acceptance failed waiting for Recording, so the formal bundle was not accepted and publication was skipped. No P0.3 Tag/Release was created.

The real service log shows `TrackRecordingService: startNewTrack` at 10:29:55.462, followed by track 991002. Each following system `uiautomator dump` process took about ten seconds; the retained XML remained the earlier Acquiring GPS snapshot. The previous harness did not delete the XML or verify successful dump output, so its global-idle timeout silently reused stale data on the live ticking screen.

P0.3 now compiles a small read-only Java shell helper in Actions, using the already configured SDK/D8. It opens the same framework UiAutomation connection as Android's command and serializes the current actual accessibility window directly, without requiring one second of global inactivity. It adds no app/test APK, dependency, key, GPS shortcut or production hook. Delete the prior XML before every sample, require a fresh success marker, and preserve the last actual screenshot in all outcomes. All real GPS, coordinate, recording, pause/resume, retained-data, GPX and eight-scheme assertions remain mandatory. Re-run the complete cloud gates for the new source.
