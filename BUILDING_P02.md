# Runner Gardenia P0.2 cloud build and delivery

Work only on feature/p0-2-gps-ui. Do not merge/push main or the original P0 branch. No SDK/Studio/Gradle installation on Windows or Codex is required. Use the existing Wrapper9.8.2/AGP9.5.0, Temurin21/source17, Ubuntu24.04, SDK37.0/BuildTools36 and existing API35 managed device. Runtime remains offline/device-local timezone; all CI layers are explicitly Asia/Singapore.

## Automatic branch-only workflow

Push source/workflow/version changes to the isolated branch. `.github/workflows/p02-gps-ui.yml` runs all Debug/Lint/JVM/API35/original/new UI/GPX/durability/offline/archive/ref checks. Phase6 alone enables the trusted `android-release` signed job. No PR/fork code reaches signing or publication. The final publisher needs successful checks AND signed runtime acceptance; default token is read-only, only publication has contents:write. It creates the annotated test Tag within the same workflow, after the accepted APK exists, without relying on a token-created Tag to trigger another run.

Formal tasks: `clean assembleReproducibleRelease`, with existing R8 and resource optimization. Checks use `clean assembleReproducibleDebug compileReproducibleDebugAndroidTestJavaWithJavac lintReproducibleDebug testReproducibleDebugUnitTest`, `mediumPhoneReproducibleDebugAndroidTest` with swiftshader_indirect, and the existing independent two-process disk suite. No empty/failed/missing report authorizes signing.

## Fixed signing

Use existing Environment android-release and original ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD, plus ANDROID_SIGNING_CERT_SHA256. Values are passed via step environment, never command-line passwords. JKS is restored with0600 permissions under RUNNER_TEMP; shell EXIT trap and always-clean step remove it even on failure. Gradle cache is disabled for signing. Only APK/checksums/public build-info/runtime proof enter the formal Artifact. No new key, PAT or credential configuration.

The actual certificate must match BOTH the Environment fingerprint and preserved P0.1 certificate. apksigner, zipalign and actual output-metadata/packaged manifest verify one reproducibleRelease APK, com.fox.runner, v0.1.0-p0.2, allocated code6773 and debuggable=false. Actual minifyWithR8/optimizeResources tasks and mapping hash are retained as public evidence.

## Version and recovery

The root ledger retains abandoned allocations and published rc/P0 codes. The P0.2 entry explicitly reserves the next code6773 from its accepted development basis; its future source commit is intentionally null until the immutable Tag/build-info identify the real build. Do not invent a self-referential commit SHA, reuse codes, move/delete Tags or overwrite assets. Historical original ledger bytes remain verifiable from frozen P0 commit60daeb2; the operational branch ledger can evolve without altering archived reports.

The tested version plan is identical for Debug and Release in a run. Formal optimized APK installs directly over the original P0.1 APK(hash94d33223…cc6ab0/code6772), without clearing/uninstalling. Isolated emulator fixtures are synthetic, never personal data or physical H303 acceptance.

If publication stops after Tag or partial Draft upload, use Actions → failed run → Re-run failed jobs. It reuses the immutable signed Artifact. A full rerun of the signed job also searches for that same-run bundle before restoring a key/building again; its exact source/hash/signature/runtime proof must pass. Matching existing Tag/assets are reused; any differing source or bytes fail closed. A network interruption after a server upload is recovered by downloading and comparing the existing asset, never replacing it. Draft assets are downloaded/hash-checked before publication, then again after publication. Releases are always prerelease=true, make_latest=false.

Report-only docs pushes run archive checks and do not rebuild, re-sign or move the Release Tag. Final release_source_sha and report_commit_sha are recorded separately. Source files and old branches remain intact.

## Download and hardware boundaries

Find accepted APKs under the run's RunnerGardenia-P02-Release Artifact and the test-p0-v0.1.0-p0.2 Pre-release after actual acceptance. Use the direct APK Release asset, verify SHA256SUMS and install over the existing app; do not uninstall/clear data. View logs/report Artifacts from the run. Never upload a keystore or passwords to an issue/chat/Artifact.

BLE automatic connection is audited only in this task; no transport/retry production change. H303 first BPM/RF reconnect, outdoor GNSS precision/actual1Hz, OEM screen-off/long-run stability and power remain NEEDS_DEVICE_VALIDATION. [Unified reports](docs/reports/README.md).

One-time existing-APK recovery after publication code fixes: an explicitly marked [p02-existing-apk] push is allowed only when the complete diff from accepted source4454d71 contains publication/workflow/media tests and reports. It validates pinned original checks/signed-runtime jobs, immutable ZIP digests, raw XML and the actual original signed APK before creating its test Tag. It never rebuilds/re-signs, never loads Environment Secrets, and keeps build source separate from publication-code source. Normal source/version/signing/test changes cannot take this route.
