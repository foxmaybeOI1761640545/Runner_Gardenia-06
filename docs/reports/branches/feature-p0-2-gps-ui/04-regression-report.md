# P0.2 complete regression

Status: PASSED. Phase4 UI/native acceptance and report/archive remote verification passed before this entry. Signing, Tag and Release remain disabled.

## Required actual gates

Debug build, Lint, complete JVM and API35 suites, original/new GPS state and recording/permission/lifecycle tests, original BLE/HR/parser/storage/history regressions,40 legacy HR images,20 original P0 page/window/theme images,36 new home state/window/theme images, independent two-process disk persistence, all9 offline GPX XSD documents, actual packaged Debug/merged Release offline audits, exact APK identity and frozen branch/Tag refs.

Actual XML must report Tests/Executed/Failures/Errors/Skipped. The original single TODO Ignore remains; no new Ignore is permitted. Missing/failed/empty/incomplete reports stop the gate. NO-SOURCE is not execution. Test Runner/JVM/system emulator timezone must remain Asia/Singapore; production display remains device local.

The archive receipt guard additionally requires every PASSED receipt member to exist, match its original size/hash, stay inside the evidence directory and be tracked by Git. Added fail-closed tests cover the exact ignored-file failure, corrupted/missing files, unsafe paths and incomplete receipts. Existing raw XML remains at documented original archive paths. No application production changes in this phase.

## Hardware boundary

Deterministic GPS/SIG driver doubles, emulator background/screen-off and disk force-stop recovery are automated. Physical H303/RF/GNSS accuracy/real1Hz/OEM lock-screen and power remain NEEDS_DEVICE_VALIDATION, not replaced by random/generated heart rates.

Actual run IDs, raw report counts, Artifact identity/hash and acceptance will be appended only after cloud completion and independent downloads.

## Actual full regression: PASSED

Source `f7beafabc7f960a0fb46f29995c76b02ba8eeb94`, [Actions42202541308](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42202541308) SUCCESS. [Independent receipt](stage5-verification.json), [raw XML/proofs/GPX](evidence/stage5-run-42202541308/).

| Actual report | Tests | Executed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| API35 main instrumentation XML |395|394|0|0|1|
| JVM12 XML files |110|110|0|0|0|
| Independent durability writer |1|1|0|0|0|
| Independent durability reader |1|1|0|0|0|
| Pipeline helper unittest job |56|56|0|0|0|

The single original `recording_stopGPS_noop` Ignore retains its original hard-stop-GPS TODO; no added Ignore, no runner crash and no NO-SOURCE claims. The two disk phases are auxiliary executions, separately counted rather than silently added to the main XML count. Actual per-phase process IDs and force-stop proof are in the receipt.

Debug/Lint/instrumentation compile, archived-report integrity gate, all GPS readiness/recording/permission/lifecycle/BLE/HR/storage/history regressions,40 legacy HR screenshots,20 original P0 page captures,36 home state captures,9 GPX XSD documents and both offline manifest audits passed. All9 freshly downloaded actual GPX were independently revalidated with the pinned offline schema. UI production files match accepted phase4; the new same-run capture gate verifies all36 images/configurations/source hashes again. [Original manually inspected phase4 screenshots](screenshots/) remain retained; the full new matrix is in this run's Artifact.

[Debug Artifact13496380256](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42202541308/artifacts/13496380256): actual downloaded package `com.fox.runner.debug`, variant `reproducibleDebug`, name `v0.1.0-p0.1`, code6772, SHA256 `c301f6ddc92277103c5d50f3bef1ae832a35bbc63d3f4cd6264e08fe98a37b8e`. Formal P0.2 version allocation is deliberately deferred to phase6. [Full report Artifact13496350296](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42202541308/artifacts/13496350296).

No production changes or new failed attempt in phase5. New archive guard tests run and pass on the same cloud source. Before phase6, push this receipt/report, verify every receipt member is Git-tracked and remote-byte-identical, and pass cloud archive verification. Signing and release are not claimed yet. Real hardware remains NEEDS_DEVICE_VALIDATION.
