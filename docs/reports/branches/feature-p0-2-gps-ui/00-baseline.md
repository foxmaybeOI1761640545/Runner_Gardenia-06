# P0.2 baseline

Stage 0: PASSED — archive commit `eed6c6e239aa9be3e831c83f1bcd6b782fa68f8f`; [cloud verification](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42179918190) passed; 1,821 report/screenshot/checksum paths independently matched remote Git blobs. [Verification receipt](archive-verification.json). No GPS/UI/BLE product changes made.

The isolated branch was created early solely to archive documents without writing the protected P0 branch. Formal stage 1 acceptance follows archive acceptance.

- Branch: `feature/p0-2-gps-ui`
- Remote P0/new branch base: `60daeb21aa50bdbceec6e6ad354a431b8c1be434`
- Protected main: `9b5d7b06afc1eb8e3315ba73874ac088fa4d5169`
- Protected original heart-rate branch: `121ea381fd11111427073fe291dfb5ddabf8b03e`
- Initial working tree clean; no untracked user files; remote new branch did not exist.
- Existing P0.1 Tag source `3116e5fa7a56ce6f299ea45d5a45745f1aed4466`, published code 6772.
- Existing ledger is stale at 6769; published rc1.5/rc1.6/P0.1 consume 6770/6771/6772. Stage 6 must reconcile actual ledger and published build-info before selecting a code; no version changed in archive stage.
- [Protected refs](../../evidence/protected-refs.txt), [historical inventory](../feature-p0-core-migration/INVENTORY.md), [Releases](../../evidence/github-releases.json).

| Stage | Scope | State |
|---|---|---|
| 0 | Historical inventory/archive/remote verification | PASSED |
| 1 | Formal baseline | PASSED |
| 2 | One read-only BLE reconnect audit | PASSED |
| 3 | GPS acquisition state and actual JVM/API35 acceptance | PASSED |
| 4 | Home UI and actual screenshot matrix | PASSED |
| 5 | Full regression | PASSED |
| 6 | Fixed-key signed P0.2, upgrade and test Pre-release | PASSED |
| 7 | Final reports/index/remote verification | IN_PROGRESS |

No merge/main/P0 branch writes; no old Tag/Release replacement; no new signing key, identity, SDK/toolchain or cloud functionality. Hardware validation remains separate.

## Stage 1 acceptance

PASSED: isolated new branch created from freshly fetched latest remote P0, pushed and independently fetched back. [Full baseline/ref/ledger snapshot](baseline.json). Original branches/Tags unchanged; working tree clean at entry; product/build/version tree exactly matches protected P0. Stage 1 is a Git/document stage: no Android tests or APK build are claimed. The archive cloud verifier checks the frozen refs and every source object; the baseline report commit must also pass that job before stage 2.

## Stage 2 acceptance

[One bounded BLE audit](01-ble-reconnect-audit.md) completed; 23 unchanged state-machine tests passed in one actual local execution. No BLE production changes. H303 root cause remains unconfirmed; no-notification CONNECTED handling is a supported candidate, not a hardware finding. Report/remote/cloud archive validation precede stage 3.

## Stage 3 receipt

[GPS acceptance](02-gps-state-acceptance.md) and [raw verification receipt](stage3-verification.json): source6046e6f/run42187525349, Android393/392 executed/0 failures/0 errors/1 original skip; JVM108 all passed; pipeline47 passed; GPX9, actual two-process durability, Debug/Lint/identity/offline and original screenshot gates passed. Stage 4 may start only after report push and remote byte verification.

## Stage 4 entry

Stage 3 acceptance report commit `2a0636f8dae78f76f8c505a6512ca3272dbacb06` passed [cloud archive verification](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42190116476), and all 1,873 remote report/evidence paths matched local Git blobs. Home redesign started only after that gate.

## Stage4 receipt

[UI acceptance](03-ui-acceptance.md): source48693c4/run42200094093, Android395/394/0/0/1, JVM110 all passed, helper51. All36 new original states/themes/windows manually reviewed and retained with producer hashes; original40+20 UI regressions preserved. Phase5 starts only after report push and remote/cloud archive verification.

## Stage5 entry

Stage4 documentation/evidence commit5158380d31e96237b1c0def535fdd641d8b5ab55 passed [archive run42202340679](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42202340679). All1958 tracked report/evidence paths matched remote blobs; all70 phase4 receipt paths were explicitly tracked/hash-checked. Product source remains accepted48693c4. Phase5 enables the receipt guard with regression tests and runs the complete original/new validation suite, without signing.

## Stage5 receipt

[Full regression acceptance](04-regression-report.md): run42202541308/sourcef7beafa, main Android395/394/0/0/1, JVM110 and helpers56 passed; all auxiliary/storage/GPX/offline/UI gates passed. Phase6 follows report push, remote byte verification and archive cloud PASS.

## Stage6 entry

Stage5 report commit `ee6b5abe7cf38b89839749bf241072572229279b` passed [cloud archive42249747455](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42249747455); all1992 report/evidence paths matched remote Git blobs. Fresh remote inspection still found four preserved Releases and their original annotated Tags. Verified historical ledger/public build-info codes6769/6770/6771/6772; next P0.2 code6773 is explicitly reserved. The reservation basis is accepted stage5 sourcef7beafa, not a fabricated future source SHA; the actual source will be recorded by signed build-info and the immutable test Tag.

Signing remains gated by a new complete same-run regression with the updated version. Only afterward: existing-key optimized build → exact certificate/identity/offline/alignment checks → P0.1 `adb install -r` upgrade preserving synthetic schema43 GPS/rawHR/device preferences → actual GPS acquisition/record/pause/recovery/history/GPX → immutable signed Artifact → annotated test Tag → verified Draft assets → non-Latest Pre-release. No Tag exists yet and no successful signed APK is claimed at entry.

## Stage6 receipt

[Signed delivery/recovery acceptance](stage6-publication-recovery.md) and [raw receipt](stage6-verification.json): actual build/check/sign/install source4454d71/run42251766485, recovered publication run42255705842 attempt4 SUCCESS, testTag at4454d71, public verified2753061-byte APK/hash48c4a855…c6d046/code6773. Default-token403 metadata limitations and configured-identity fallback are explicit. Phase7 follows this evidence/report push and remote archive PASS.

## Stage7 entry

Stage6 evidence commitdb1af76e54aae5f853191577e8c381b7f46894f0 passed [archive42257474866](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42257474866). All2053 report/evidence paths matched remote blobs. Public APK/signature and all6 assets independently downloaded/hash-checked; final stage changes only reports/index/checklist and a remote-verification seal, never APK/Tag/build source.
