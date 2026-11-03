# Heart-rate development stage acceptance

Baseline: 24aa0d7d36d244852e796cca6307936e34d989ed; valid release v0.1.0-rc1.4 / Code 6769.

## Stage 0 — architecture audit

- Changes: HEART_RATE_IMPLEMENTATION_AUDIT.md and this acceptance record; no application or workflow code changed.
- Reviewed: existing BLE, recording/database, charts/UI, release scripts and the reference native HR module at bc678e48f7be6ef8774733a5a0ef00e8056e1e56.
- Local validation: requested path checks, documented verifier filename correction, git diff --check; baseline helper regression tests.
- Result: architecture/document review PASS. Draft PR CI PASS: https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41691689014 . Debug/Lint and the full API 35 suite succeeded; signing/tag/publication jobs were skipped.
- Next-stage permission: PASS — stage 1 is authorized to begin after the completed PR CI.

Hardware access: unavailable. Emulator/fake transport evidence and real radio evidence must remain separate.

## Stage 1 — standard parsing and BLE lifecycle

- Reused the existing generic BluetoothDriver/SensorHandler and Material sensor dialog; no duplicate BLE service/manager was added.
- Added strict SIG UINT8/UINT16, contact, energy and exact RR decoding; invalid/zero/contact-lost samples clear stale BPM. Optional body location and battery reads are serialized after CCCD acknowledgement.
- Added transport-independent GATT session and scan generation guards, handshake timeout, bounded reconnect, explicit manual stop, Bluetooth/permission states, stale GATT rejection and teardown. Preserved notification/indication support for other sensors.
- Scan now starts only after permission success, has a 15-second timeout, rescan, RSSI/physical-link display and exhaustive batch handling. Re-selecting a remembered active sensor requests explicit reconnect; selecting None disconnects. No new permission was added to the Manifest; Android <=30 now requests the already-declared fine location permission for discovery.
- Local tests: 38 actual JUnit tests PASS using the available Java compiler module with source/target 17 and Asia/Singapore. These cover parsing, malformed tails, RR precision, reconnect, cancellation, stale callbacks, timeouts, scan states and permission policy. The original javac executable is absent; no Android SDK or JDK installation was performed.
- Added emulator regressions for stale/invalid aggregation, adapter rows, optional metadata, actual driver receiver teardown with no radio and subscription type selection.
- Added test-only JUnit 4.13.2 (no production dependency/toolchain upgrade), and included JVM test XML in the existing PR report Artifact.
- Result: PASS — PR CI https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41699651106 . Actual JVM XML: 38 tests, 0 failures/errors/skips. Instrumentation XML: 304 tests, 303 executed, 0 failures/errors, 1 original Ignore. Debug APK downloaded and SHA-256 independently matched 45580b7b0715c2f0332f8ead30844dd5cca399559ae78e3b642afeda70d36a55; com.fox.runner.debug, reproducibleDebug, planned v0.1.0-rc1.5 / 6770. Singapore device/process tests passed. Signing/tag/publish skipped. Stage 2 authorized.
- Fixed during acceptance: already-granted permission callback (with regression), and typed BluetoothStatusCodes.SUCCESS required by actual Android Lint. Gradle 9.8.2 help passed locally without SDK installation.
- Hardware: no physical strap attached; no RF/GATT hardware compatibility claim.

## Stage 2 — local raw sample recording

- Found a significant existing constraint: TrackRecordingManager intentionally throttles distance-free TrackPoints to >10 seconds. Simply adding metadata to those rows would silently lose notifications; changing GPS point selection would affect unrelated recording behavior.
- Reused the same recording SQLite database and its track lifecycle. Added one normalized raw-notification table (schema 43), FK cascade and track/time index; retained existing BPM TrackPoints and their GPS/distance filters unchanged. This is not a second database or recording service.
- Each notification has a stable sample UUID, monotonic capture time, BPM, track FK, source, optional device/contact/energy/exact RR values and a connection-generation ID. Repeated snapshots are idempotent; distinct identical-BPM notifications, even within the same millisecond, remain distinct.
- HeartRateHandler stamps incoming parsed data with the existing monotonic clock. RecordingManager persists raw data before its existing storage filters; the Service's recording guard excludes paused/stopped samples. Disconnect only invalidates HR; it does not end the track. Original BPM-only history/export remains compatible and no network permission/upload was added.
- Added repository round-trip/reopen, track association, timestamp ordering, duplicate identity, missing optional data, invalid values, cascade isolation, v42 data-preserving upgrade and pause/resume/short-disconnect integration tests. Existing v23 upgrade/downgrade schema checks remain and now validate the new table/indexes.
- Result: PASS — https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41701825550 . Downloaded XML: 313 tests, 312 executed, 0 failures/errors, 1 original Ignore; 38 JVM tests, 0 failures/errors/skips. v42 upgrade/legacy BPM preservation, v23 upgrade/downgrade, all six repository tests and actual pause/resume recording integration passed. Debug/Lint/identity checks passed; downloaded Debug APK SHA-256 b8326fd7452954e88a0340cc36b03a910d8e3956745e165c758fdce8e3fc26fd matched its report. Singapore tests passed. Stage 3 authorized.

## Stage 3 — recording panel and existing chart integration

- Added a Material recording panel for real BPM, acknowledged connection state, contact loss, freshness and the existing sensor settings entry. Production uses immutable status snapshots and the recording monotonic clock, never synthetic BPM or a fixed device timezone.
- Kept the current navigation, configurable statistics grid, themes and ChartView zoom/pan. Time charts consume incremental raw samples; distance charts retain TrackPoint snapshots with a clear full-sample Time hint. Minimum/mean/maximum use all retained raw samples, not the reduced drawing points.
- Raw history reads only new row IDs, caps the drawing cache, preserves extrema and gap flags before reduction, and maps pause/resume intervals to the existing active-time axis. Reconnection/contact boundaries and >5-second missing signals break paths. Legacy missing-HR transitions survive TrackDataHub downsampling; chart reset now resets old series extrema too.
- Fixed a UI-discovered permission issue: the settings fragment no longer blocks the dialog's own permission request. Bluetooth-off dialogs remain usable for selecting None/remembered devices and explicit rescan.
- Local pure plot tests: six PASS, including 100,000 samples, bounded output, chronological order, extrema, hidden gaps and repeated reduction. Added Android history tests for pause mapping, incremental reload, real DB stats and legacy fallback.
- Result: pending complete stage 3 CI, emulator screenshots and visual inspection. Stage 4 is not authorized yet. Physical strap access remains unavailable.

- Stage 3 intermediate CI 41706341684: 325 instrumentation cases, 324 executed, 1 UI-scenario failure, 0 errors, 1 original Ignore; all other instrumentation cases and 44 JVM cases passed. Failure was a test helper that searches only Activity decor while waiting for a separate Dialog window. Eight actual screenshots were recovered and inspected; the first recording capture was too early, so capture now waits two frame callbacks and verifies painted text pixels. The helper now uses Espresso's dialog-root matching.
- The API 35 virtual Bluetooth process also aborted on a real scan request (not the application/instrumentation process). UI captures now explicitly exercise the real offline dialog with the emulator radio temporarily disabled and restored; synthetic rows remain identified as fixtures. Protocol/transport/scan state tests remain enabled. No physical radio success is claimed.
- Added pre-recording HR preview and explicit connect/reconnect/disconnect controls in the existing sensor settings/service, with remembered selection and UUID-scoped private commands. Manual stop cancels retries; only explicit reconnect restarts. Preview never creates a track or writes raw samples; samples captured before the new active segment are excluded. Local JUnit total is now 45 PASS. Full Android/UI acceptance remains pending.

- Stage 3 run 41728310766 passed all automated gates: 326 instrumentation tests (325 executed, 0 failures/errors, 1 original Ignore), 45 actual JVM tests, complete 40 PNGs across 411dp/360dp and light/dark, verified Debug APK hash a073dcc990c8a52bae6161999cdce724381e1bb99201ed6fa80ddb8297b37a7a. However, manual inspection rejected two narrow-screen captures: an Activity transition and an outdated test fixture were visible. Therefore stage 3 remains pending; stage 4 has not started. Capture now suppresses/restores test-device animation scales, waits for the exact Activity/window and idle pager, drains pending LiveData updates, requires stable screen pixels and rechecks the actual displayed fixture before saving. This strengthens UI evidence rather than accepting a green CI alone.

- Stage 3 final acceptance: PASS — https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41730815364 at feature commit db08d13b920e4b668b556983efe5db205c10d000. Actual XML: 326 instrumentation cases / 325 executed / 0 failures / 0 errors / 1 original Ignore; 45 JVM cases / 0 failures/errors/skips. All 40 actual captures validated across 411dp/360dp light/dark. Key recording, stale, time/distance chart, settings/dialog, original home/history captures were inspected with view_image; the rejected transitional/stale captures were replaced by stable correct ones. The native settings/history lists remain scrollable at 360dp; no control overlap or text ellipsis was found in the inspected pages. Twelve unedited captures and hashes are retained in docs/screenshots/heart-rate. Downloaded Debug APK SHA-256 c787d0a6d556d7be191e2d5df9a18cc338bb5e0bcd1aac10c3018e436d3b1cfe matches the identity report. Stage 4 authorized.

## Stage 4 — complete regression and hardware boundaries

- Retained every existing test and the original Ignore. Added HR distance projection regressions so even a short reconnect between stored GPS snapshots breaks the distance curve; raw BPM is projected onto interpolated track-distance anchors without changing stored GPS/distance statistics. Time remains the precise timing view. Added full UINT16 y-axis range and actual Android path regressions.
- Added advertisement-name fallback before Bluetooth's name cache is populated, with pure tests. The existing fixed-clock test seam now respects the process default timezone instead of CET.
- Extended the XML gate to inspect actual JVM reports/classes/counts separately. Missing/NO-SOURCE-equivalent, inconsistent, failed, errored or all-skipped unit evidence cannot pass. Added eight gate regressions covering missing/corrupt UI evidence and JVM counts. Instrumentation test Java is explicitly compiled before managed-device startup so SDK type errors fail earlier.
- Local validation: 51 actual JUnit cases PASS (not NO-SOURCE), 14 Python helper cases PASS, actionlint and edited XML/Python/diff checks PASS.
- Hardware checklist: docs/HEART_RATE_BLE_TEST_CHECKLIST.md, including pre-recording preview, recording/pause/reconnect/manual stop, history/restart/upgrade, optional RR and battery, privacy and long-duration checks. Every physical result remains explicitly unverified.
- Result: pending full stage 4 PR CI and actual reports; stage 5 must wait.

- Stage 4 final acceptance: PASS — https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41733224958 at d34c68481427b0bf0e5ceafc2fe2d8b561c3cb06. Downloaded XML: 328 instrumentation / 327 executed / 0 failures / 0 errors / 1 original Ignore; 52 JVM tests / 0 failures/errors/skips, including the actual Gradle test-worker Singapore identity/offset test. All 40 UI captures passed; both revised distance-chart sizes were visually checked. Debug/Lint/identity passed; downloaded APK SHA-256 2be8a3b8fd6b647fc37859c6522c603c4ad9928e3ff65e9413d5f7283c8ffeb2 matched the report. Fourteen Python helper regressions passed. Physical strap/upgrade/endurance checks remain unverified. Stage 5 authorized.

## Stage 5 — verify signed APK before assigning a tag

- Reordered dependencies to prepare → checks → signed (including APK verification/upload) → create-tag → publish. PR/manual runs remain unsigned checks only.
- The tag job now requires both the passed exact-plan CI gate and the same-run signed bundle with matching identity, signature verification, unique APK and SHA256SUMS. The immutable annotation binds the APK SHA-256 in addition to the source/version/run plan.
- A signing retry restores this run's original signed artifact and independently re-verifies the Environment certificate, APK identity and tag digest. Missing/expired originals for an already-tagged version fail closed; existing signed artifacts are never overwritten. Draft publication resumes missing assets; different APK bytes cannot replace an existing release.
- VERSION_NAME_FULL is rendered from planned version plus exact source SHA, so creating the tag afterwards cannot change this build field on retry. Local builds retain git-describe fallback.
- Added negative tests: passed tests without signed bundle, failed signature, corrupted transferred APK, foreign plan, missing/expired tagged artifacts and certificate mismatch cannot proceed. Existing interrupted-upload/rerun publication tests remain.
- Local validation: 17 Python tests PASS, actionlint/Python syntax/diff checks PASS. Gradle 9.8.2 help also passed locally without installing SDK. Actual stage 5 PR CI pending; stage 6 is not authorized yet. The signed main path is intentionally not exercised from an untrusted PR; final main validation must verify it before publication is claimed.

- Stage 5 acceptance: PASS — https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41735349725 at 8ee51b9430f8df147ac72885a88ce4561189afd9. Downloaded XML confirms 328 instrumentation / 327 executed / 0 failures/errors / 1 original Ignore and 52 JVM / 0 failures/errors/skips. All 40 UI captures exist. Debug/Lint/identity passed; downloaded APK SHA-256 994cb800b05d68fe17e4307adaa90f57929aa9e14e463a6ab879da7cfcf0be5b matches the report. Seventeen pipeline regressions passed. PR correctly skipped signing/tag/publish; actual main signing/publication remains a final-stage requirement. Stage 6 authorized.

## Stage 6 — audited orphan cleanup and allocation ledger

- Queried four remote/local tags, all Releases/assets, full failed/successful release logs and retained artifact inventories. rc1.4 is the only published signed APK; rc1.1–rc1.3 have neither a verified signed Artifact nor a Release and cannot be restored. Full evidence and actual APK checksum are in RELEASE_TAG_AUDIT.md.
- Persisting all original names/Codes/commits/tag objects and high water 4 / 6769 before any deletion. Added allocator/tag gates and three real temporary-Git regression scenarios, including removal of the highest or every tag. Twenty pipeline tests, actionlint, Python syntax and diff checks PASS.
- Cleanup and final stage 6 CI pending. No later stage may begin before actual cleanup and PR CI acceptance.
