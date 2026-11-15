# Stage 1 validation log (not accepted)

Candidate source: 7ed7fb0d57d235e6a50ff7cc97b54a40d08d90d6. Draft PR: https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/pull/5 (base feature/p0-6-map-real-validation).

Run https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43735700103 failed correctly at lint. The new Handler.hasCallbacks call requires API29 while minSdk remains API26. This is an implementation defect, not a reason to change minSdk or suppress lint. Fixed in c0275174634c73becf0c454ea67fd0ef768c548b using explicit scheduled state and main-thread dispatch. Native test inputs now enter the actual shared GpsHandler, keeping live ViewModel refresh consistent with map screenshots and asserting idle preview creates no history. The original GPS preference/ownership tests remain unchanged. Renderer-attached caches are destroyed once by Mapsforge destroyAll; the empty-map cache is released separately.

Actual first-run JVM reports: Tests 167, Executed 167, Failures 0, Errors 0, Skipped 0 (raw-report inventory summarized in evidence/stage1-run43735700103-jvm.json). Python helpers were not reached in that failed CI run; the separately executed local helper suite is 141/141 with 0 failures/errors/skips. Android instrumentation did not run; missing reports fail the gate, not a pass.

The old main/RC workflow also ran on PR opening and rejected the P0.x version series (Run 43735729281). It signed nothing and published nothing. Only its prepare-job PR exclusion now additionally recognizes the clean branch; main's trigger, dependency chain, version allocator and signing/publication jobs are unchanged. The new branch's authoritative validation runs on exact-source pushes, avoiding duplicated full-suite jobs on the same PR source. Old failure records are retained, never relabeled.

Fixed-source validation run: https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43735969185. Pending results cannot authorize Stage 2, signing, tag creation or a release.
