# P0.2 complete regression

Status: IN_PROGRESS. Phase4 UI/native acceptance and report/archive remote verification passed before this entry. Signing, Tag and Release remain disabled.

## Required actual gates

Debug build, Lint, complete JVM and API35 suites, original/new GPS state and recording/permission/lifecycle tests, original BLE/HR/parser/storage/history regressions,40 legacy HR images,20 original P0 page/window/theme images,36 new home state/window/theme images, independent two-process disk persistence, all9 offline GPX XSD documents, actual packaged Debug/merged Release offline audits, exact APK identity and frozen branch/Tag refs.

Actual XML must report Tests/Executed/Failures/Errors/Skipped. The original single TODO Ignore remains; no new Ignore is permitted. Missing/failed/empty/incomplete reports stop the gate. NO-SOURCE is not execution. Test Runner/JVM/system emulator timezone must remain Asia/Singapore; production display remains device local.

The archive receipt guard additionally requires every PASSED receipt member to exist, match its original size/hash, stay inside the evidence directory and be tracked by Git. Added fail-closed tests cover the exact ignored-file failure, corrupted/missing files, unsafe paths and incomplete receipts. Existing raw XML remains at documented original archive paths. No application production changes in this phase.

## Hardware boundary

Deterministic GPS/SIG driver doubles, emulator background/screen-off and disk force-stop recovery are automated. Physical H303/RF/GNSS accuracy/real1Hz/OEM lock-screen and power remain NEEDS_DEVICE_VALIDATION, not replaced by random/generated heart rates.

Actual run IDs, raw report counts, Artifact identity/hash and acceptance will be appended only after cloud completion and independent downloads.
