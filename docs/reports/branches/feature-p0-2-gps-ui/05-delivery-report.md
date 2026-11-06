# Runner Gardenia P0.2 delivery

Product/test/publication acceptance: **PASSED**. Final report/index remote seal: [final-verification.json](final-verification.json). Hardware remains **NEEDS_DEVICE_VALIDATION**. This closes the requested GPS/home work; no further P0 feature expansion.

## Sources, branch and release identity

| Field | Actual value |
|---|---|
| Repository | foxmaybeOI1761640545/Runner_Gardenia-06 |
| Development branch | feature/p0-2-gps-ui |
| Frozen main | 9b5d7b06afc1eb8e3315ba73874ac088fa4d5169 |
| Latest fetched P0/new branch baseline | 60daeb21aa50bdbceec6e6ad354a431b8c1be434 |
| release_source_sha / actual APK build | 4454d716a5ae010b92d3eefa1ac1c0ebedde0063 |
| Publication fix/recovery code | 4444c0d30925b146ac693ef0fcfa0cceabfa99ad |
| report_commit_sha | Exact last-change commit of this report, recorded after push in final-verification.json; the subsequent seal does not change this report or APK |
| Annotated Tag | test-p0-v0.1.0-p0.2 |
| Tag object | 51c7f7684ef111352f765ebf35d65562bf35fb42 → release_source_sha |
| Application ID | com.fox.runner |
| Variant / debuggable | reproducibleRelease / false |
| Version Name / Code | v0.1.0-p0.2 /6773 |
| APK | RunnerGardenia-P0-Release.apk |
| APK size | 2,753,061 bytes /2.6255236MiB |
| APK SHA-256 | 48c4a855402b7bec0b310794a228f15323ef8f09b2eb81d4f6913ab56ac6d046 |
| Fixed certificate SHA-256 | E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22 |

- [Public test Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.2), [direct APK](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.2/RunnerGardenia-P0-Release.apk), [SHA256SUMS](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.2/SHA256SUMS).
- Release436759328: draft=false, prerelease=true, non-Latest. Latest stable API returns404 because the repository's releases are Pre-releases. Six assets were uploaded and actually downloaded/compared before and after publication: APK, SHA256SUMS, build-info.json, runtime-smoke.json, test-summary.json, RELEASE_NOTES.md. A separate public browser-URL APK download equals the original accepted Artifact bytes. Official apksig9.5 independently verifies the single original fixed certificate across API26–37 (valid v2 signature); aapt2 confirms actual package/version/code.
- [Build/acceptance run42251766485](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42251766485): checks and signed-install jobs SUCCESS; overall run correctly remains FAILED due to its initial publication HTTP415. [Recovery run42255705842 attempt4](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42255705842/attempts/4) SUCCESS. Do not describe the historical failed overall run as green.
- [Formal APK Artifact13522176163](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42251766485/artifacts/13522176163), [raw test/UI reports13521268691](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42251766485/artifacts/13521268691), [signed/R8/installed-device reports13522905114](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42251766485/artifacts/13522905114), [publication proof13523032214](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42255705842/artifacts/13523032214). Critical raw XML/GPX/JSON/screenshots are also committed, so their survival does not depend on90-day Artifacts.

## GPS and home behavior

[GPS acceptance and full failure history](02-gps-state-acceptance.md). Service-owned IDLE→ACQUIRING_GPS→RECORDING→PAUSED/FINISHED reuses the existing GPS/recording service and repository. No track ID, timer, distance, points or workout-associated HR exists during preparation. Preview HR can continue independently. Cancel releases preparation resources;60s timeout permits wait/cancel/system settings rather than cached-coordinate start. Readiness requires three distinct monotonic fixes spanning at least1s, capture after preparation, age≤5s, finite coordinates/accuracy≤30m and the existing stricter recorder filter, with jump checks. Thresholds are centralized. The latest confirming fix initializes the real workout. Activity recreation/background preserve the service state; GPS loss clears live coordinates/speed/pace but neither pauses nor ends recording. Existing GPS-gap/GPX segmentation is retained.

[UI acceptance and rejected attempts](03-ui-acceptance.md). Home only: state/guidance, four prominent motion values, HR/device/state/last-valid/reconnect summary, contextual start/cancel/pause/resume/finish. Small-screen primary actions remain reachable; wider screens group content/actions. History/settings destinations remain, with no new navigation tab or settings/database/BLE rewrite.36 original9-state×2-theme×2-window captures were manually reviewed and retained with producer metadata/hashes; original40 HR and20 P0 matrix gates remain. Six actual non-debuggable installed-Release screenshots were also viewed, including acquisition0m/00:00, real emulator recording and retained history/detail/device configuration. [New matrix/checksums](screenshots/), [installed Release originals](evidence/stage6-run-42251766485/signed/reports/p0-release-smoke/). CI profiles are standard-font English; Chinese resources compile, but exhaustive accessibility-font/locale/OEM visuals are not claimed.

## BLE: audit only, not fixed

[One bounded audit](01-ble-reconnect-audit.md),23 unchanged state-machine JUnit tests passed. Auto and manual reconnect share service discovery/notification/CCCD callbacks, and CONNECTED follows descriptor success; manual preview reconnect explicitly closes/recreates a link, whereas auto retry mainly follows transport failure. No first-valid-notification watchdog after CONNECTED is a supported candidate for the H303 symptom. Contact flags/invalid notifications are other possibilities. Root cause remains unconfirmed without device timing/notifications. **No BLE production/retry changes were made.** Existing H303 connection/no-BPM behavior is not advertised as repaired.

## Actual automated results

| Accepted execution | Tests | Executed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| APK source API35 main XML |395|394|0|0|1|
| APK source JVM12 XML files |110|110|0|0|0|
| APK source helper regression |66|66|0|0|0|
| Auxiliary durability writer |1|1|0|0|0|
| Auxiliary durability reader |1|1|0|0|0|
| Publication repair/recovery helper suite |71|71|0|0|0|

The original recording_stopGPS_noop TODO remains the only Android Ignore. No new Ignore, deleted test, lowered assertion, runner crash, empty suite or NO-SOURCE claim. Recovery's Android/JVM/build jobs are deliberately not executed: the original successful jobs' immutable raw reports and APK were independently revalidated instead of rebuilding an already accepted APK. Helper71 belongs to recovery; helper66 belongs to the APK source. They are not summed into a misleading new APK test count.

Debug/Lint/instrumentation compilation, all configured GPS/lifecycle/permission/BLE/parser/storage/history/UI regressions,9 actual offline GPX schemas, original40+20/new36 UI matrices, two distinct disk-persistence processes, packaged Debug/merged Release and actual formal APK offline gates passed. [Complete regression](04-regression-report.md), [signed/published raw receipt](stage6-verification.json), [recovery explanation](stage6-publication-recovery.md). All test layers use Asia/Singapore; production absolute timestamps/device-local display remain unchanged.

## Signed install, upgrade, optimization and privacy

Original P0.1 APK (code6772/hash94d33223…cc6ab0) installed first; P0.2 installed with `adb install -r`, never uninstall/clear. Isolated synthetic fixture: schema43, one GPS point, two distinct equal-BPM raw HR samples with RR/contact/energy metadata, selected address and connection preference. Actual new APK retained those rows/preferences; history and GPX1point/2rawHR/XSD passed. New recording captured51 synthetic GPS points,22.4835524559021m,2manual segments,58172ms active/69882ms wall, with pause/force-stop/recovery/resume. Preparation00:00/0m/no extra workout was verified with GPS off then enabled for fresh fixes. BL/notification permissions were denied while GPS recording succeeded. These establish emulator/fixture compatibility, not outdoor1Hz accuracy, physical RF or OEM power stability.

Actual minifyReproducibleReleaseWithR8 and optimizeReproducibleReleaseResources tasks executed; nonempty mapping SHA6f13ea5a780dd844164e766e29c73bc0c45de6d607b0e25fdebd37f0848fd960 retained. Compared with P0.1, this APK adds37,068bytes (~1.36%) for GPS/home changes; no false shrinking claim. Actual output metadata, manifest, signature, task evidence and installed screenshots are committed. No Gradle/AGP/JDK/dependency upgrade, namespace/package-path rename or schema change.

Actual Release manifest has no INTERNET/Health Connect/account/cloud permissions, allowBackup=false, original local GPS/BLE/FGS permissions, and only P0 entry/history/sensor activities plus the existing service/providers. Existing offline backup/extraction and private GPX cache provider checks pass. No cloud sync/upload/ads/account/network feature added; no real personal workout/database/APK/key/JKS/password was committed. Only the existing android-release fixed key was restored in RUNNER_TEMP with step-scoped environment values, no CLI passwords/cache, EXIT+always cleanup. Artifact/Release file lists exclude signing material.

## Publication recovery and remaining authorization limit

Normal orchestration is checks→signed optimized APK→actual identity/signature/install/upgrade→Tag→Draft assets→download verification→Pre-release. Default contents:read, only publication contents:write; signing alone uses android-release; no PR/fork key access; Actions pinned to full SHAs; one concurrency group. Version ledger preserves6766–6773 including abandoned allocations, published rc/P0 history and the P0.2 reservation. Future new code must be at least6774 with a new version; deleted Tags cannot erase this high-water. Reservation basis is not claimed as final build source; Tag/build-info identify the real SHA.

Initial publisher failed fetching job logs with octet-stream(HTTP415); the media adapter and regression now preserve API media type for raw job logs. Recovery pins original ZIP digests/IDs/source, re-parses raw XML and re-verifies the original APK/certificate/upgrade proof. Default Actions token then returned403 creating historical-source Tag/Draft metadata despite contents:write. The existing configured Codex Git/GitHub identity created only this new annotated Tag and exact Draft; no PAT, key, reviewers, network credentials or protection settings changed. Actions then uploaded/downloaded/verified/published all six assets. The more specific backend token-policy cause is not asserted. Similar metadata operations may need the same already authorized fallback; unrestricted default-token Tag/Draft creation is not claimed. Original failed attempts remain documented. Existing identical Tag/assets are reused, differing bytes/source are rejected, never silently overwritten.

## Historical archive and Git safety

[Whole-project inventory](../feature-p0-core-migration/INVENTORY.md), [history index](../feature-p0-core-migration/README.md), [unified report entry](../../README.md). Original62 relevant tracked files and12 HR screenshots retain paths;66 historical report zips/1759 members are mapped to1693 exact-byte deduplicated objects, with raw XML, screenshots, metadata and manifests, plus10 recovered Codex-only documents. rc1.4/5/6/P0.1 public APKs/identities/signatures were actually verified. Large redundant logs remain explicitly referenced in original Artifacts. Missing historical names are marked unavailable rather than fabricated; upstream docs/resources are not relabelled as Runner acceptance. Final inventory corrects a header typo: doc/ and .forgejo workflow files do exist and were already enumerated in detailed Git inventory.

Only feature/p0-2-gps-ui received development/report commits. Main/original P0/other pre-existing branches and four old annotated Tags unchanged; one new test Tag added, no new repository, merge, rebase/reset/force push or old Release replacement. Full final remote blobs, indices, PNG checksums, receipt members, public assets and frozen refs are rechecked in the linked final seal. Final report commits do not rebuild/re-sign or move the Tag. Pending physical work is explicit in [06-device-checklist.md](06-device-checklist.md). Stop feature expansion and wait for phone/H303 validation.
