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
- Result: local checks PASS; full stage 1 PR CI pending. Stage 2 is not authorized until that CI and its actual reports pass.
- Hardware: no physical strap attached; no RF/GATT hardware compatibility claim.
