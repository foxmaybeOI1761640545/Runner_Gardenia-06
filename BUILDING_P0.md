# Runner Gardenia P0 build and installation

Development branch: feature/p0-core-migration. Baseline main is
9b5d7b06afc1eb8e3315ba73874ac088fa4d5169. This task does not merge or push main,
or modify other existing branches. The accepted P0 APK is now published at
https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.1
under an annotated test tag pinned to build3116e5fa7a56ce6f299ea45d5a45745f1aed4466.
It remains a non-Latest Pre-release; no rebuild/re-signing was performed for publication.

Normal product-code pushes to this branch retain the complete build/test gates.
The explicit [p0-existing-apk] route permits only publication-helper/workflow/scope-test
and documentation differences from that accepted build. It rejects product, version,
signing and existing-test changes, verifies the original run/tag/Artifact, then uses
a separate contents:write/actions:read job to transfer files to the one existing Draft.
It has no Environment/signing Secrets and runs no Gradle. This one-shot Draft transfer
is not a new APK build or a generic automatic version publisher.

For ordinary code pushes, The branch-only P0 workflow runs Debug, Lint,
all 88 JVM tests and the full API35 Android suite, original/P0 UI evidence,
real file-database process recovery, GPX schema and offline/identity checks.
Missing, failed or zero-execution reports fail closed. NO-SOURCE is not a test.
All CI/JVM/device test environments use Asia/Singapore. Production uses the
Android device timezone. No Windows development dependency is required.

Only stage 10 + fullRegression + signed mode enables the trusted signing job.
It receives the same run/commit's passed gate and uses android-release:
ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS,
ANDROID_KEY_PASSWORD; independent ANDROID_SIGNING_CERT_SHA256.
No key generation/change is part of this branch. JKS is restored only under
RUNNER_TEMP, passwords are environment values, Gradle configuration/build caches
are disabled, and traps plus always cleanup remove temporary signing files.

The formal task is assembleReproducibleRelease with R8 and resource shrinking;
Debug tasks are assembleReproducibleDebug, lintReproducibleDebug,
testReproducibleDebugUnitTest and mediumPhoneReproducibleDebugAndroidTest.
The latter uses swiftshader_indirect and the existing API35 managed device.
The toolchain stays JDK21/source17, Wrapper9.8.2/AGP9.5.0/API37/BuildTools36.0.0.

APK identity is com.fox.runner, version v0.1.0-p0.1, code 6772. Keep identity,
certificate and increasing version codes for later updates. The verifier locates
one actual APK through output-metadata.json, checks apksigner against the
independent Environment certificate, rejects Debug signing/debuggable APKs,
checks zip alignment, offline permissions/components/private FileProvider and
writes SHA256SUMS/public build-info. R8 mapping is retained for diagnosis.

The actual fixed-key APK must install over baseline v0.1.0-rc1.6/code6771 without
clearing data on a disposable API35 AOSP device. Synthetic old GPS/raw-HR rows
are seeded only in that test device. Acceptance covers installed Release UI,
GPS recording without Bluetooth/notification permission, pause/resume, process
recovery, unchanged schema43/history and actual Release GPX export/schema.
Physical H303/RF/outdoor GPS/background/battery remain separate device checks.

The permanent test download is
https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.1/RunnerGardenia-P0-Release.apk
with SHA256SUMS.txt and the original acceptance-document snapshots on that Release.
The independent closing review is docs/P0_FINAL_TECHNICAL_REVIEW.md.

The original RunnerGardenia-P0-Release-RUNID Artifact is also available from the
Actions run page. It contains RunnerGardenia-P0-Release.apk, SHA256SUMS,
build-info.json and runtime-smoke.json. GitHub's normal Artifact download is a
platform archive; the APK is not wrapped in an additional archive by this build.
Artifacts are retained for 90 days. No APK/JKS is committed to Git.

Install on Android 8.0 or newer using the APK and allow the selected installation
source. Finish a pre-P0 workout before updating. Do not uninstall or clear app
data for an upgrade. Verify the downloaded SHA-256 against SHA256SUMS. Keep the
original fixed keystore and passwords in independent encrypted backups; nothing
in these artifacts can recover a lost signing private key.

User flow: grant precise GPS, optionally allow recording notifications, select
Running/Cycling and Start/Pause/Resume/Finish. Choose a SIG HR strap in Heart-rate
strap settings and Connect; retry can be disabled and Disconnect is remembered.
Open Local recordings for motion/raw-HR statistics, confirmed deletion, Save GPX
or Share GPX. The core works offline, has no INTERNET permission and disables
system cloud/device-transfer backups. See docs/P0_GPX_FORMAT.md and
docs/P0_DEVICE_CHECKLIST.md for export details and pending real-device acceptance.

The final APK is 2,715,993bytes,75.326% smaller than the actual rc1.6/6771 APK
(11,007,368bytes), with DEX class definitions14838->1519 and resource IDs8280->5050.
This combines P0 feature/component removal with R8/resources; it is not an isolated
R8-only benchmark.42 Health-related DEX classes and some dormant old exporters/drivers
remain through shared preferences/provider/handler roots; they are not exposed or
started by the P0 UI and do not carry Health permissions. Do not claim zero residual
legacy code. No production functional defect requiring a new APK was found.

Do not overwrite the published p0.1 tag/assets. Any later production fix needs a new
version name/code, full regression, signed build, upgrade verification and new test
Tag/Pre-release (for example p0.2/code6773), after a new authorized task.
