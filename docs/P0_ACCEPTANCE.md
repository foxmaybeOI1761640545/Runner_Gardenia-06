# P0 acceptance record

## Stage 0 — PASSED

- Repository: foxmaybeOI1761640545/Runner_Gardenia-06.
- Remote main baseline: 9b5d7b06afc1eb8e3315ba73874ac088fa4d5169.
- New branch: feature/p0-core-migration, created directly from that commit with no upstream tracking of main.
- Initial worktree: clean; no user changes moved, overwritten or discarded.
- Existing branches: main and feature/heart-rate-monitoring, recorded with exact original SHAs in P0_BASELINE.json. Neither ref has been modified.
- Checks: git status, fetch/read-only remote branch inventory, baseline ancestry/equality and unique branch creation. Source/application/build files unchanged during stage 0.
- Baseline Android evidence: existing successful main workflow 41848524070 (328 Android cases, 327 executed, 0 failures/errors, 1 original Ignore; 52 JVM cases; 20 helper cases). This is unchanged-baseline evidence, not a claim that new P0 tests have run.
- Local baseline helper regression: 20 tests PASSED before this stage's commit; branch/ref/unchanged-source checks PASSED.
- Failure/fix: none.
- Stage 1 entry: authorized after local branch/ref/unchanged-source checks pass and stage 0 is committed only on the new branch.

## Hardware validation — NEEDS_DEVICE_VALIDATION

No physical GPS handset or H303 is attached to this environment. Real outdoor 1Hz delivery/accuracy/loss recovery, H303 power-off/range/reconnect, Bluetooth cycling, background/screen-off behavior, long-session power use and physical upgrade/install checks must be reported separately. H303 will not be hardcoded as a device name. Emulator/fake transport data is explicitly test data.

## Stage 1 — IN_PROGRESS

- New P0 Activity/layout/permission facade; launcher routes to it without onboarding. App label is Runner Gardenia P0, package/namespace unchanged. This stage is a skeleton: record/device controls are wired only in their later accepted stages.
- Existing GPS/BLE/service/database code and toolchain dependencies are retained for verified reuse; dependency/runtime slimming is audited in stage 8 and optimized in stage 10. Branch version identifies the Artifact as v0.1.0-p0.1 / 6772, above baseline's distributed 6771; it does not allocate a main Release/Tag.
- Branch-only p0-core.yml has contents:read, no signing/release job, no main/PR trigger. All actions use existing pinned SHAs. It checks protected refs, builds Debug/Lint/all JVM tests and the complete API35 suite, requires nonzero complete XML and actual P0/upstream screenshots, and verifies Debug identity.
- Added four pure permission cases and two actual P0 launcher/recreation cases. The prior HR UI scenario still runs all forty original views directly via the legacy TrackList, while P0ActivitySmokeTest now owns the actual launcher check. No upstream test was removed or ignored.
- Local: 4 JUnit cases PASSED with source/target17 and Singapore; 24 Python helper tests PASSED; actionlint, Python/XML syntax and diff checks PASSED.
- Android build/install/start/UI and full regression: PENDING actual branch Actions; no next-stage implementation yet.

- Failed candidate d0f7ac9 / Actions 41870121327: AndroidTest Java compilation rejected an integer timeout passed to ActivityTestUtils.waitForResumedActivity, which requires Duration. No instrumentation tests ran in this attempt; missing XML/screenshots correctly failed the gates. Fixed only the call to Duration.ofSeconds(10). Stage 1 remains IN_PROGRESS pending rerun; stage 2 has not begun.

- Stage 1 accepted: PASSED at 02804a9510f3af894d226a4059b50c6ccb242b9c, https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41870693111 . Downloaded XML: Android 330 / executed 329 / failures 0 / errors 0 / skipped 1; JVM 56 / failures/errors/skips 0. Debug/Lint passed; actual installed launcher and Activity recreation passed. P0 screenshot visually inspected, all forty upstream captures retained. Downloaded Debug SHA e1b3754f774e3300ef5b21908739df451b4b1ecbd7825ea6907653ffef2cfebd matches report. Original refs unchanged. Stage 2 authorized.

## Stage 2 — IN_PROGRESS

- Existing Track, TrackPoint, HeartRateSample and schema 43 are sufficient; no second database or schema bump. Added WorkoutRepository/WorkoutSummary as a worker-only app/data boundary and exact raw-notification aggregate projection. Raw data, device metadata, contact/energy/RR and independent sample identities remain in the existing table.
- Added chronological cursor streaming to HeartRateRepository; its existing row-ID incremental chart API stays unchanged. New repository tests cover reopen/exact summary and RR, deletion cascade/isolation including GPS rows, no-sensor records, chronological equal-BPM notifications and failed foreign-key batch rollback.
- Four pure summary cases cover equal values as separate samples, invalid values, exact min/max/mean and the full SIG range. Stage tests include existing schema v23/v42 migration and raw repository regressions plus mandatory Singapore/service lifecycle gates and P0 install/start smoke. Full upstream suite remains scheduled for stages 9/10; no configured stage test is omitted.
- Documentation-only pushes are excluded from the branch build trigger; accepted stage records can be committed without unnecessarily building the same source again. Main/other branches remain protected.
- Local four new summary tests, 24 Python tests, actionlint, protected-ref and diff checks PASSED; Android/database acceptance PENDING actual stage-2 Actions. Stage 3 is not yet authorized.

- Candidate afadca7 / 41873228190 executed all 24 selected Android and 60 JVM cases with 0 failures/errors/skips, including raw batch rollback. The overall run FAILED correctly because scoped-source copying used a fully-qualified filename and report owner discovery doubled package names. Fixed the copy to preserve source package directories/simple filenames; added a real XML-owner regression with duplicate simple names in different packages. No tests or gate were removed. Stage 2 remains IN_PROGRESS until the complete rerun passes.

- Stage 2 accepted: PASSED at b7e60a01ac00a6aa947cb4c72896c9b567fbf6b9; https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41874166590. Downloaded actual reports Android {'tests': 24, 'executed': 24, 'failures': 0, 'errors': 0, 'skipped': 0}, JVM {'tests': 60, 'executed': 60, 'failures': 0, 'errors': 0, 'skipped': 0}; Debug/Lint/identity and protected-ref checks passed. Downloaded Debug SHA-256 69ee2edf5e05df296a09794c4eaaf8b42a49467eaeee2f90744fe62aabbef00e matches its report. Stage 3 is authorized.

## Stage 3 — IN_PROGRESS

- P0 ViewModel/controller binds the existing TrackRecordingService with application context; UI lifecycle only attaches/detaches observers and preview ticks. Started recordings continue independently of Activity recreation/backgrounding. Database history reads run on an executor through WorkoutRepository.
- Start/run/cycle, pause via existing endCurrentTrack, resume via existing resumeTrack and finish reuse manual track segments/statistics. Session intent is kept atomically in a small private preference journal; all actual data remains in SQLite. A reopened intent is PAUSED until actual service state proves recording.
- P0 start/resume select existing settings: one-second GPS requests and zero distance threshold, retaining each valid GPS fix without replacing location/statistics algorithms. UI shows current state/time/moving time/distance/speed/pace/GPS status/lat/lon/accuracy/fix time and reuses HR freshness rendering.
- Fixed a core permission coupling: foreground service type now reflects granted GPS/connection capabilities; absent Bluetooth does not require CONNECTED_DEVICE. The one-shot service helper unbinds in finally. Missing capabilities cannot create a recording and a sticky restart with no sensor permission stops safely.
- Added 13 pure session/permission-mode/motion-format cases (local PASS), actual GPS request/foreground-type/paused-distance and session journal tests, plus a real P0 cycling UI flow through recreation/background/pause/resume/finish. Existing recording/GPS/lifecycle regressions remain selected. Device-only outdoor/1Hz/H303/power checks are in P0_DEVICE_CHECKLIST.md, all NEEDS_DEVICE_VALIDATION.
- Android build, UI/service and data gates PENDING actual stage-3 Actions. Stage 4 not authorized until passed.
