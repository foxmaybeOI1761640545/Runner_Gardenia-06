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
