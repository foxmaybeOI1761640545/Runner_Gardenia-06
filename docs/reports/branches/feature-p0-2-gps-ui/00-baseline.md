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
| 4 | Home UI and actual screenshot matrix | PENDING |
| 5 | Full regression | PENDING |
| 6 | Fixed-key signed P0.2, upgrade and test Pre-release | PENDING |
| 7 | Final reports/index/remote verification | PENDING |

No merge/main/P0 branch writes; no old Tag/Release replacement; no new signing key, identity, SDK/toolchain or cloud functionality. Hardware validation remains separate.

## Stage 1 acceptance

PASSED: isolated new branch created from freshly fetched latest remote P0, pushed and independently fetched back. [Full baseline/ref/ledger snapshot](baseline.json). Original branches/Tags unchanged; working tree clean at entry; product/build/version tree exactly matches protected P0. Stage 1 is a Git/document stage: no Android tests or APK build are claimed. The archive cloud verifier checks the frozen refs and every source object; the baseline report commit must also pass that job before stage 2.

## Stage 2 acceptance

[One bounded BLE audit](01-ble-reconnect-audit.md) completed; 23 unchanged state-machine tests passed in one actual local execution. No BLE production changes. H303 root cause remains unconfirmed; no-notification CONNECTED handling is a supported candidate, not a hardware finding. Report/remote/cloud archive validation precede stage 3.

## Stage 3 receipt

[GPS acceptance](02-gps-state-acceptance.md) and [raw verification receipt](stage3-verification.json): source6046e6f/run42187525349, Android393/392 executed/0 failures/0 errors/1 original skip; JVM108 all passed; pipeline47 passed; GPX9, actual two-process durability, Debug/Lint/identity/offline and original screenshot gates passed. Stage 4 may start only after report push and remote byte verification.
