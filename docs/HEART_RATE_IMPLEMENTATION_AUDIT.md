# Heart-rate implementation audit — stage 0

Baseline: Runner Gardenia `24aa0d7d36d244852e796cca6307936e34d989ed`, release v0.1.0-rc1.4 / Code 6769.
Reference inspected: MiuiBootShutdownLauncher-1777869935 `bc678e48f7be6ef8774733a5a0ef00e8056e1e56`.
Development stays on feature/heart-rate-monitoring and a Draft PR. Intermediate stages must not publish from main.

## Existing functionality and reuse

| Area | Actual implementation | Decision |
|---|---|---|
| Discovery/selection | BluetoothLeHeartRatePreference delegates to the shared BluetoothLeSensorPreference; service UUID filtering, bonded devices, saved address and explicit None already exist | Reuse Material preference/dialog; fix duplicate starts, bound scan lifetime, add RSSI/selected status and explicit rescan |
| Standard HR transport | HeartRateBluetooth advertises 0x180D/0x2A37, plus the existing Mi Band advertised-service compatibility entry; BluetoothUtils defines CCCD 0x2902 | Keep UUID support and generic BluetoothDriver; no parallel BLE manager |
| GATT lifecycle | BluetoothDriver connects, discovers service, enables local notifications and writes CCCD | Fix stale callbacks, subscription acknowledgement, failed discovery, connection timeout, permission/Bluetooth-off handling and bounded reconnect |
| Parsing | UINT8/UINT16 BPM supported, empty/truncated BPM rejected; null characteristic value is not handled | Introduce a pure Java SIG measurement decoder with explicit contact/energy/RR fields and strict bounds checks; keep BPM API compatibility |
| Aggregation | HeartRateHandler feeds AggregatorHeartRate; Aggregator expires sensor values after five seconds | Preserve stale-data semantics; explicitly clear HR on disconnect and contact loss, retain optional measurement metadata |
| Recording | SensorManager → TrackPointCreator → TrackRecordingService → TrackRecordingManager already supports sensor-only TrackPoints and GPS-independent recording | Reuse the recording session/track ID and monotonic recording clock; a BLE disconnect must not end a track |
| Storage/history | TrackPointsColumns.sensor_heartrate, track ID and timestamp are already persisted/read by ContentProviderUtils; existing database version 42 | Retain BPM column and existing database. Add only nullable metadata columns if needed for device/source/contact/energy/RR; additive migration and old-record tests required |
| Live data | TrackDataHub and recording-data observer already publish heart-rate data; custom recording fields include heart-rate values | Integrate connection/freshness information into the existing recording screen, rather than a separate application/service |
| Charts | ChartPoint includes heartRate; ChartView/ChartFragment have HR series, time/distance axes, zoom/scroll and history reload | Reuse renderer and navigation; verify missing-value paths and break HR lines across missing samples/long time gaps |
| Statistics | Existing SQL/statistics cover average/max heart rate; minimum and freshness are not presented as a coherent HR panel | Compute valid-sample minimum/average/maximum with existing history data, without assuming a user's maximum HR |
| Privacy | Manifest does not grant INTERNET for the application; no analytics/upload is needed | Keep all samples local and keep device identifiers out of exported public diagnostics by default |

Reviewed the requested HeartRateBluetooth, HeartRateHandler, AggregatorHeartRate, BluetoothUtils, SensorManager, BluetoothLeHeartRatePreference, README_TESTED_SENSORS, TrackPoint, TrackDataHub, TrackRecordingService, database tables/migrations, ChartFragment, ChartView, ChartValueSeries, ChartPoint and chart/recording/statistics layouts.

README_TESTED_SENSORS lists upstream experience with Polar H7/H10/OH1, Wahoo Tickr, Garmin HRM-Dual and others. This is **not** a claim that this development environment has physically tested these devices.

## Concrete defects and limitations

- BluetoothDriver.isConnected currently means only bluetoothGatt != null, including a pending/failed connection.
- onServicesDiscovered does not reject a failed status and announces connected before the CCCD write is acknowledged.
- Disconnect callbacks immediately call gatt.connect without backoff or a manual-stop/generation guard; an already-closed stale GATT can reconnect.
- Missing service/characteristic/CCCD and failed notification setup are logged but can leave resources and state stuck.
- There is no explicit lifecycle for Bluetooth power changes; permissions can be revoked between checking and invoking a protected API.
- Preference discovery invokes startBluetoothScan both through the permission callback and directly. It lacks a scan timeout and a clear rescan affordance.
- BluetoothLeAdapter.addAll uses stream.anyMatch with a mutating add operation: short-circuiting can omit devices from a batch.
- HeartRateBluetooth ignores sensor contact, energy and RR optional fields, and accepts malformed optional tails because it reads only BPM.
- TrackPoint persistence currently loses optional protocol/device metadata. RR must remain exact 1/1024-second units, not silently rounded milliseconds.
- AggregatorHeartRate keeps its last value until stale and inherits a no-op resetAggregated; a lost connection can briefly show old HR.
- The chart supports HR already, but missing/long-gap continuity, entry points, minimum HR and connection freshness need explicit coverage.

## Reference architecture and boundaries

HeartRateEnvironment owns shared manager/event sinks; HeartRateForegroundService owns its standalone foreground lifetime. TargetDeviceMatcher includes a project-specific target-name fallback, which will not be adopted: Runner Gardenia must connect only to the user-selected address.

The reference is a Kotlin/Capacitor module with a shared HeartRateBleManager, JS event sinks, a standalone foreground service and JSONL session/sample files. It waits for CCCD success, remembers a selected target, carries contact/energy/RR metadata, reads optional body location/battery, and schedules bounded reconnects. HeartRateStorage reads session-linked timestamped samples; HeartRateChunkWriter also contains upload-oriented chunk state.

Runner Gardenia is a native Java application whose TrackRecordingService owns SensorManager, generic BluetoothDriver, aggregators, TrackPoints and SQLite history. Copying the reference manager/storage/service would duplicate lifecycle and data ownership.

Borrow only SIG flag decoding, acknowledged subscription, explicit manual-stop state, stale-callback protection, bounded retry, timestamp/session association and optional device metadata. Do not copy the reference UI, target device name, Capacitor bridge, auto-start policy, GitHub uploader, upload queue, upload chunking or cloud credentials. The reference parser's acceptance of an incomplete trailing RR byte is not adopted: malformed optional fields should be rejected explicitly.

## Release audit findings and planned correction

The only workflow is .github/workflows/android-build.yml. The actual verifier is .github/scripts/verify_apk.py; verify_release_apk.py does not exist. Helpers and .github/tests have been reviewed together with build.gradle, version.properties and BUILDING_RUNNER_GARDENIA.md.

Current order is prepare → checks → create-tag → signed → publish. The checked-in properties define the seed; annotated Tags record allocated versions and Codes. Signed verification currently requires the Tag before signing. Publish already uses recoverable drafts, checks identity/signatures/checksums and avoids replacing published files.

Stage 5 must change the order to prepare → checks → signed/verification → create-tag → publish. Passing signed artifact metadata must be required by the tag job. Before Tag creation, signing must validate the tested plan/commit rather than an existing Tag. PRs stay CI-only; no new token, key or Environment setting is needed.

Stage 6 must inventory actual API assets and successful signed artifacts, preserve rc1.4, persist the complete allocated-version high-water mark before deleting any confirmed orphan, and regression-test allocation after deleting the highest historical Tag. Unknown-purpose Tags are retained. Source archives do not count as APK assets.

## Stage scope, expected files and evidence

1. BLE: pure SIG parser and reconnect/session policy; HeartRateBluetooth/BluetoothDriver/SensorHandler, shared scan dialog/adapter, permission helpers; parser, fake-transport lifecycle and scan-list tests.
2. Recording: optional HR measurement metadata in the existing pipeline, nullable TrackPoints extensions only if required; ContentProviderUtils/CustomSQLiteOpenHelper and recording tests; old database upgrade, timestamps, reconnect gaps and round-trip tests.
3. UI: existing recording layout/observer, chart renderer and HR statistics; Material theme resources only. Add deterministic emulator screenshots for light/dark and multiple window sizes, live/stale/empty/history states. Synthetic test fixtures are not advertised as real BLE hardware.
4. Regression: retain existing tests/Ignore, add parser/state/storage/chart coverage and docs/HEART_RATE_BLE_TEST_CHECKLIST.md. Verify complete CI and screenshot artifacts.
5. Workflow: reorder signed/tag dependencies, adapt release_policy/verify_apk, retain strict XML gates, fixed signing and idempotent publish, and test that signing failure cannot tag.
6. Cleanup: release-ledger.json plus release-policy tests and docs/RELEASE_TAG_AUDIT.md; delete only explicitly authorized, proven automated orphan Tags/empty drafts after ledger persistence.
7. Final: ready PR, normal merge if permitted, then inspect main's complete workflow, actual XML counts, screenshots, signed APK, Tag target, Pre-release assets and downloaded SHA-256.

Each stage has a commit and PR CI run. No later-stage implementation begins before the current stage's required checks pass. No hardware is attached: actual radio compatibility, reconnect behavior under real RF conditions, battery impact and physical installation/update remain manual checklist items and cannot be claimed as completed by emulator tests.

## Stage 0 acceptance

The baseline and reference commit were identified, the requested existing paths were traced, reused components and concrete gaps were recorded, privacy/release boundaries were defined, and per-stage files/checks were listed. Documentation/static review is complete; PR CI evidence is recorded in HEART_RATE_STAGE_ACCEPTANCE.md before stage 1 starts.
