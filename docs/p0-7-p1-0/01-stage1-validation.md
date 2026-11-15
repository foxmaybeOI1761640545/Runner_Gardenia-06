# Stage 1 validation log (not accepted)

Candidate source: 7ed7fb0d57d235e6a50ff7cc97b54a40d08d90d6. Draft PR: https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/pull/5 (base feature/p0-6-map-real-validation).

Run https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43735700103 failed correctly at lint. The new Handler.hasCallbacks call requires API29 while minSdk remains API26. This is an implementation defect, not a reason to change minSdk or suppress lint. Fixed in c0275174634c73becf0c454ea67fd0ef768c548b using explicit scheduled state and main-thread dispatch. Native test inputs now enter the actual shared GpsHandler, keeping live ViewModel refresh consistent with map screenshots and asserting idle preview creates no history. The original GPS preference/ownership tests remain unchanged. Renderer-attached caches are destroyed once by Mapsforge destroyAll; the empty-map cache is released separately.

Actual first-run JVM reports: Tests 167, Executed 167, Failures 0, Errors 0, Skipped 0 (raw-report inventory summarized in evidence/stage1-run43735700103-jvm.json). Python helpers were not reached in that failed CI run; the separately executed local helper suite is 141/141 with 0 failures/errors/skips. Android instrumentation did not run; missing reports fail the gate, not a pass.

The old main/RC workflow also ran on PR opening and rejected the P0.x version series (Run 43735729281). It signed nothing and published nothing. Only its prepare-job PR exclusion now additionally recognizes the clean branch; main's trigger, dependency chain, version allocator and signing/publication jobs are unchanged. The new branch's authoritative validation runs on exact-source pushes, avoiding duplicated full-suite jobs on the same PR source. Old failure records are retained, never relabeled.

Fixed-source validation run: https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43735969185. Pending results cannot authorize Stage 2, signing, tag creation or a release.

## Native layout failure and inspection

Run 43736726705 (source c6b17a56b27f2630bbff23ae745672a15a5b0401) executed Python 141/141 and JVM 167/167 with 0 failures/errors/skips. The focused Android run had Tests 2, Executed 2, Failures 1, Errors 0, Skipped 0. It failed the new real native control-rectangle assertion: recenter overlapped Mapsforge zoom controls on compact layouts. The complete suite was correctly not started. No acceptance or release is authorized by this run.

Actually inspected the native p07-home-no-map.png and p07-home-inside-bounds-no-features.png from this run. The map areas are neutral/empty with a position marker and truthful map status, rather than illustrative terrain/roads. Both screenshots demonstrate the control overlap. The recenter control now uses the bottom start corner; the compact home map omits its competing scale bar (the independent full map retains its scale bar). The layout assertion remains intact. Visual review also exposed that idle GPS status and accuracy still said unavailable while the map had a valid preview; DashboardState now derives those preview fields from the same fresh fix, while speed/distance/duration remain subject to the original workout policy. No GPS owner/driver/recording logic was changed.

Runs c027517/43735969185, d836768/43736258541 and 17e332c/43736512644 were superseded and canceled during incomplete full regressions as the candidate changed. They are not full passes. In particular 17e332c's focused step succeeded, but its full run was canceled and its output was cleared by the subsequent full invocation; no missing screenshot/report is counted as acceptance. The final candidate must execute its focused and complete suites again.
