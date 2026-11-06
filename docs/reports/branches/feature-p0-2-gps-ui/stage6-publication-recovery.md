# P0.2 publication recovery

Status: IN_PROGRESS. Signed APK/upgrade PASSED; original overall run FAILED in publication; no new Tag/Release created by that failed step.

## Exact evidence

Original source4454d716a5ae010b92d3eefa1ac1c0ebedde0063, [Actions42251766485](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42251766485). Checks job128008421881 and signed/upgrade job128015231707 SUCCESS; publication job128018794849 FAILED. Actual main Android395/394 executed/0 failures/0 errors/1 original skip; JVM110; helpers66. Optimized fixed-key APK2753061bytes, SHA25648c4a855402b7bec0b310794a228f15323ef8f09b2eb81d4f6913ab56ac6d046. Formal Artifact13522176163; reports13521268691; signed reports13522905114.

Actual signed upgrade proof: P0.1/code6772 installed first, new code6773 via install-r, schema43 preserved,1 original GPS point/2 raw HR samples and selected-device/connection preferences retained. GPS preparation0distance/00:00/no extra workout; real emulator fixes then51GPS points,2manual segments,22.4835524559021m,58172ms active/69882ms wall, process recovery and GPX1GPS/2rawHR/XSD passed. Physical radio/GNSS remains untested.

## Real publication failure and minimal fix

After revalidating the accepted APK, helper_counts requested the checks job log using Accept:application/octet-stream. GitHub's job-log redirect API requires its API media type and returned HTTP415. This occurred BEFORE tag creation. The media adapter now separates raw response bytes from request media type: job logs keep application/vnd.github+json, Release assets still use octet-stream. Added explicit header regression tests; no product/version/signature change.

## Recovery gate

Do not rebuild/re-sign the accepted APK. The explicit [p02-existing-apk] path compares the full current diff against original source4454d71 and permits only publication/workflow/helper-media tests and documents. Any GPS/BLE/product/version/ledger/signing or original acceptance-test change fails closed. Normal pushes retain full checks/signing.

The keyless recovery job requires both original checks and signed-install jobs SUCCESS; pins both immutable Artifact IDs/ZIP digests/source/run; re-parses original actual Android/JVM XML, UI matrix and storage/GPX/offline gate; checks actual APK bytes/size/identity/certificate/alignment and original upgrade proof. Only then does the existing Tag/Draft/asset verification publisher run, with the original build SHA. Recovery code SHA is separate and never replaces release_source_sha. No Environment/signing Secrets, Gradle build, APK modification or key access in recovery. Original failed run remains a historical failure; its two successful acceptance jobs are cited accurately.

Local helper suite71 passed, including5 new media/routing regressions. Cloud recovery result and public download revalidation will be added only after actual success.
