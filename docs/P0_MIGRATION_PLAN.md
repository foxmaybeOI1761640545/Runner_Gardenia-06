# Runner Gardenia P0 migration

Baseline: `9b5d7b06afc1eb8e3315ba73874ac088fa4d5169` from remote main.
Branch: `feature/p0-core-migration`. Protected initial branch refs are recorded in P0_BASELINE.json.

Only this new branch may be pushed. No changes, commits, resets, rebases, pushes or merges on main; no existing branch or tag/Release may be changed. No PR merge is part of this task. Final delivery is a signed Actions Artifact, not a GitHub Release.

## Scope and architecture

Reuse the existing GPS driver/recording state machine, BluetoothDriver/SIG parser and SQLite/HeartRateRepository. Build a small P0 application facade with explicit app, gps, ble, recording, data, export and statistics responsibilities; do not duplicate the mature sensor algorithms or move every legacy class merely to rename packages. The P0 launcher exposes recording, remembered HR device and local history/GPX only. Existing implementation and regression fixtures can remain in source while non-P0 runtime entry points/dependencies are removed and R8/resource shrinking remove unreachable code.

Keep com.fox.runner, Java namespace, Wrapper 9.8.2, AGP 9.5.0, JDK 21/source 17, API 37 and Build Tools 36.0.0. No Android SDK is installed in Codex Cloud. Android tests/builds run on Ubuntu Actions with API35 and Asia/Singapore.

GPX must retain every raw HR sample, exact timestamp and connection identity using a documented application extension. Coordinate-bearing trkpt rows remain the real GPS points; unmatched samples must not invent coordinates or silently disappear. Standard HR extensions may additionally carry a documented nearest-in-time match.

## Stage ledger

| Stage | Work / gate | Status | Commit / evidence |
| --- | --- | --- | --- |
| 0 | Clean worktree, latest main, unique branch, protected-ref snapshot, task plan | PASSED | 26ae496; P0_ACCEPTANCE.md |
| 1 | P0 launcher/facades, branch-only CI, necessary permissions and initial build/install/start | PASSED | 02804a9 / 41870693111 |
| 2 | Reuse models/database, repository facade, transaction/association/delete/reopen/migration tests | PASSED | b7e60a0 / 41874166590 |
| 3 | GPS 1Hz request, status/accuracy/pace, run/cycle, pause/resume segments and recovery | PASSED | 61a41a5 / 41886672130 |
| 4 | Standard HR scan/name/address/selection, GATT/permissions/lifecycle | PASSED | fdfd366 / 41891475431 |
| 5 | BPM/contact/energy/RR, notification identity, timeout/reconnect/manual control/old callback isolation | PASSED | 7d394c2 / 41896259632 |
| 6 | Raw sample/session/track integration, exact statistics, pause/reopen/durability | PASSED | f126648 / 41929851364 |
| 7 | Local GPX save/share, all raw HR samples, no fabricated points, XML/schema/round-trip tests | PENDING | — |
| 8 | Manifest/dependency/code/packaged APK offline audit; no account/upload/cloud/online maps | PENDING | — |
| 9 | Full relevant JVM + API35 regression, all report gates, screenshots | PENDING | — |
| 10 | Existing release variant, fixed key, R8/resource shrink, zipalign/apksigner, Release install/start and Artifact | PENDING | — |

Candidate commits are necessary inputs to GitHub Actions: run available local checks, commit/push the stage candidate only to this branch, inspect actual cloud reports, then record acceptance. A failed candidate never authorizes the next stage. Stage records distinguish code candidates and accepted results; no Android result is inferred from local script checks or NO-SOURCE.

States: PENDING, IN_PROGRESS, PASSED, FAILED, BLOCKED, NEEDS_DEVICE_VALIDATION. Hardware-only GPS/RF/H303/background/power checks remain NEEDS_DEVICE_VALIDATION and do not block independent automated work. No hardware success is claimed from mocks or an emulator.

The branch-only validation workflow must exist early enough to test stages 1–9. Stage 10 enables its signed optimized delivery after all preceding automatic gates pass. Only the trusted signing job may read android-release; no PR may access signing secrets. Signing material never enters Git, logs, caches or artifacts, and is cleaned from RUNNER_TEMP on success/failure. No new credential or key is generated.
