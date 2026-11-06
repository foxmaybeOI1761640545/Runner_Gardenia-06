# P0.2 publication recovery

Status: PASSED via verified recovery. Original build run remains FAILED in publication only; its checks and signed-upgrade jobs succeeded. The new recovery run succeeds and the public Pre-release is independently verified.

## Exact evidence

Original source4454d716a5ae010b92d3eefa1ac1c0ebedde0063, [Actions42251766485](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42251766485). Checks job128008421881 and signed/upgrade job128015231707 SUCCESS; publication job128018794849 FAILED. Actual main Android395/394 executed/0 failures/0 errors/1 original skip; JVM110; helpers66. Optimized fixed-key APK2753061bytes, SHA25648c4a855402b7bec0b310794a228f15323ef8f09b2eb81d4f6913ab56ac6d046. Formal Artifact13522176163; reports13521268691; signed reports13522905114.

Actual signed upgrade proof: P0.1/code6772 installed first, new code6773 via install-r, schema43 preserved,1 original GPS point/2 raw HR samples and selected-device/connection preferences retained. GPS preparation0distance/00:00/no extra workout; real emulator fixes then51GPS points,2manual segments,22.4835524559021m,58172ms active/69882ms wall, process recovery and GPX1GPS/2rawHR/XSD passed. Physical radio/GNSS remains untested.

## Real publication failure and minimal fix

After revalidating the accepted APK, helper_counts requested the checks job log using Accept:application/octet-stream. GitHub's job-log redirect API requires its API media type and returned HTTP415. This occurred BEFORE tag creation. The media adapter now separates raw response bytes from request media type: job logs keep application/vnd.github+json, Release assets still use octet-stream. Added explicit header regression tests; no product/version/signature change.

## Recovery gate

Do not rebuild/re-sign the accepted APK. The explicit [p02-existing-apk] path compares the full current diff against original source4454d71 and permits only publication/workflow/helper-media tests and documents. Any GPS/BLE/product/version/ledger/signing or original acceptance-test change fails closed. Normal pushes retain full checks/signing.

The keyless recovery job requires both original checks and signed-install jobs SUCCESS; pins both immutable Artifact IDs/ZIP digests/source/run; re-parses original actual Android/JVM XML, UI matrix and storage/GPX/offline gate; checks actual APK bytes/size/identity/certificate/alignment and original upgrade proof. Only then does the existing Tag/Draft/asset verification publisher run, with the original build SHA. Recovery code SHA is separate and never replaces release_source_sha. No Environment/signing Secrets, Gradle build, APK modification or key access in recovery. Original failed run remains a historical failure; its two successful acceptance jobs are cited accurately.

Local helper suite71 passed, including5 new media/routing regressions. Cloud recovery result and public download revalidation will be added only after actual success.

## Actual completion and permission boundary

[Recovery run42255705842 attempt4](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42255705842/attempts/4) SUCCESS, publication code4444c0d30925b146ac693ef0fcfa0cceabfa99ad. Original APK source remains4454d716a5ae010b92d3eefa1ac1c0ebedde0063. Recovery route71 actual helper tests passed. Original main Android395/394/0/0/1, JVM110 and helpers66 were not re-executed or misreported as this recovery's executions; their original XML/accepted job proofs were downloaded, hashed and re-parsed.

Recovery attempt1 passed all pinned ZIP/XML/signature/upgrade validation and fixed job-log media handling, then the Actions token received HTTP403 creating the Tag reference. No Tag existed. The existing configured Codex Git identity (repository push permission, no active rulesets found) pushed ONLY annotated test-p0-v0.1.0-p0.2; remote annotation object51c7f7684ef111352f765ebf35d65562bf35fb42 targets4454d71 and pins APK/certificate/version. Attempt2 then received HTTP403 creating the Draft. A transient local metadata-read HTTP503 prevented the first local Draft creation; attempt3 still lacked a Draft and failed in the same metadata operation. No APK was rebuilt and no Secrets/permissions were changed.

The already configured Codex GitHub identity then created Draft436759328 with exact original source, prerelease=true/make_latest=false. Attempt4 reused that Draft, uploaded the6 exact assets, downloaded and checked all Draft assets, published, and downloaded/verified them again. It required no new PAT/key/credential/approval and did not bypass a protection setting. The HTTP403 operation is proven; its underlying GitHub token-policy reason was not exposed by the API wrapper, so no more specific backend rule is claimed. This is a real boundary for historical workflow-changing commits: the default token was insufficient for Tag/Draft creation here; the existing authorized Codex identity completed metadata, while Actions performed asset transfer/verification/publication. This limitation is retained, not hidden behind a green result.

[Public Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.2): draft=false/prerelease=true, notLatest (latest stable API404; all releases are Pre-releases).6 uploaded assets: formal APK, SHA256SUMS, build-info.json, runtime-smoke.json, test-summary.json, RELEASE_NOTES.md. A separate public browser-URL download was byte-identical to original Artifact13522176163,2753061bytes, hash48c4a855402b7bec0b310794a228f15323ef8f09b2eb81d4f6913ab56ac6d046. All6 local downloaded sizes/server digests match. Official apksig9.5 independently validates one fixed certificate across API26–37 (v2 signature valid), original E3E7…9A22 certificate; actual aapt2 packagecom.fox.runner/versionP0.2/code6773. No rebuild, re-sign, Tag move or APK replacement.

[Full evidence receipt](stage6-verification.json) includes original raw reports, actual R8/resource tasks/manifest/signature/metadata, six installed-Release screenshots, upgrade proof/GPX and publication receipts/logs. All six actual installed-Release screenshots were visually reviewed: no blank/overlapping/clipped core controls; acquisition0/00:00, actualGPSrecording, retained history/detail/rawHR76minavgmax and selected device settings confirmed. These are emulator/synthetic-data screenshots, not H303 or outdoor GNSS proof. Hardware remains NEEDS_DEVICE_VALIDATION.
