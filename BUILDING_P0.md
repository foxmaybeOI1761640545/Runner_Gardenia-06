# Runner Gardenia P0 build and installation

Development branch: feature/p0-core-migration. Baseline main is
9b5d7b06afc1eb8e3315ba73874ac088fa4d5169. This task does not merge or push main,
modify existing branches, or create tags/GitHub Releases.

Push code changes to this branch. The branch-only P0 workflow runs Debug, Lint,
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

After all checks, download the RunnerGardenia-P0-Release-RUNID Artifact from the
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
