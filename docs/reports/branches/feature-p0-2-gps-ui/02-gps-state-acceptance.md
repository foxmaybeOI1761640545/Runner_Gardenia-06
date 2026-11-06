# GPS preparation acceptance

Status: IN_PROGRESS. No next-stage UI redesign or signing is authorized until the full actual API35/JVM gate passes.

## Implementation

- Reuses the existing TrackRecordingService, SensorManager/GpsHandler and database/recorder. Service owns preparation and timeout across Activity recreation/background; no duplicate GPS/recording service.
- Session states IDLE → ACQUIRING_GPS → RECORDING → PAUSED/FINISHED. Idle preparation has no track ID, timer, distance or saved points. HR preview can remain connected but pre-start samples are invalidated at the existing segment boundary.
- Central GpsReadiness.DEFAULT: accuracy ≤30m, monotonic age ≤5s, capture strictly after preparation start, 3 distinct stable fixes spanning ≥1s. Invalid fixes reset stability, duplicate times cannot count, excessive jumps/gaps require a new stable sequence. Wall-clock timestamps do not decide freshness.
- 60s timeout only displays waiting options; keep waiting / cancel / system location settings are available. No cached fallback. Cancel removes timeout and GPS/idle resources, retaining an explicitly running HR preview where appropriate.
- Latest accepted fix enters the existing recorder after track creation. Preparation time is excluded. During recording, stale/invalid raw GPS fixes do not become points; a missing signal never pauses/ends the workout. UI clears stale coordinates/speed/pace and shows weak/lost GPS; original segment/gap storage rules remain.
- BLE transport/parser/reconnect production code, database schema, identity, signing and toolchain unchanged.

## Local execution

- GpsReadinessTest: 19 actual JUnit tests, 0 failures/errors/skips.
- RecordingSessionTest: 6 actual JUnit tests, 0 failures/errors/skips (includes preparation transitions/no track ID).
- Python pipeline regression: 47 tests passed, including P0.2 branch isolation/mandatory full gates/signing boundary.
- Java17 syntax parse, edited XML, actionlint, git diff checks passed. Gradle help outcome is recorded with the cloud candidate evidence; no local Android SDK/emulator installed.

## Cloud acceptance pending

New branch-only p02-gps-ui workflow reuses the full existing Debug/Lint/JVM/API35/GPX/schema/two-process durability/offline/UI/XML completeness gates. It grants no signing Environment, write permission, Tag or Release access during this phase. Actual reports must be downloaded and independently recounted before PASSED.

New Android tests cover preparation without records, stale/cached/inaccurate/invalid fixes, stable start and point/time boundary, cancellation, timeout/continue, preview exclusion, signal loss/recovery, emulator screen-off service continuity. Existing recording UI assertions are retained and now explicitly supply valid fresh GPS before expecting RECORDING; they also exercise recreation/background while acquiring. Physical H303, outdoor GNSS rate/accuracy, vendor lock-screen/power behavior remain NEEDS_DEVICE_VALIDATION.

## First candidate failure (retained)

Source `a8ee1f87c79bda4d313a540a35277b60959efcaf`, [run 42181627583](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42181627583): compileReproducibleDebugJavaWithJavac failed at P0RecordingViewModel.java:55, `variable store might not have been initialized`. A field-initializer lambda directly captured the constructor-initialized final repository journal. Replaced it with a method-reference observer whose body executes only after construction. No test/gate changes; Android/JVM suites did not execute on this candidate. Complete stage rerun required.

Local Gradle `help`: first attempt failed because AGP tried to initialize `/home/agent/.android` in a read-only directory; with `ANDROID_USER_HOME=/tmp/p02-android-user` and the existing Gradle cache it passed (8s, one actual help task). No SDK/emulator/JDK installed; no project configuration changed to mask the environment.

## Complete-suite infrastructure interruption (retained)

Source `e01a61cd06ad46dab6197c46d666c9d8ef1eec08`, [run 42181895038](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42181895038): Debug/Lint/compilation passed; actual JVM XML 108/108/0 failures/0 errors/0 skipped. All 11 new P0GpsPreparationTest methods completed without failure, including the emulated screen-off check. Full instrumentation did **not** pass: runner expected 391 but received 182; [raw XML](evidence/stage3-run-42181895038/TEST-mediumPhone.xml) contains 184 testcase records including two synthetic failure records (P0ResponsiveUiTest status -1 and incomplete run), 0 errors/0 skips. [Independent counts](evidence/stage3-run-42181895038/counts.json).

Direct evidence: the managed emulator became offline during the existing responsive screenshot test; diagnostics ended with `adb: device offline`, broken logcat pipe and inability to clear-debug-app. No application Java FATAL exception was found in the test's actual logcat. The exact emulator/host cause is not yet established; graphics ColorBuffer errors also occur in earlier successful runs and are not alone proof of a GPU root cause. No production behavior, assertion, Ignore or test gate was changed to compensate. CI monitoring now preserves the last successful guest logs and records host memory/process/disk/kernel/crash-file evidence. The complete suite must be rerun from a normal branch push; stage 4 remains blocked.
