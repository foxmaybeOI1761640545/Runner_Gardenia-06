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
