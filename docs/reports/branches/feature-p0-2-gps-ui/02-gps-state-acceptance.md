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

## Test-lifecycle race identified on rerun (retained)

Source `1afd60b459925a58b59627f087c2730151f399fd`, [run 42183062819](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42183062819): JVM 108/108/0/0/0 passed; Android incomplete, [raw XML](evidence/stage3-run-42183062819/TEST-mediumPhone.xml) 208 records, 3 failures, 0 errors/skips (includes incomplete-run sentinel). Responsive P0 UI and new GPS preparation cases completed on this run. Two concrete failures: `P0SensorsTest.systemServiceRecoveryRespectsDisabledAutomaticReconnect` immediately rebound the same service instance after asynchronous stop; `PublicApiTest.StopAndWait` then crashed on an old recording timer reading a track removed by test cleanup. No production BLE reconnect changes were involved.

Root test-fixture issue: `stopService()` returns before service destruction and cannot destroy an externally bound service. The shared reset helper immediately deleted all track rows without first ending recording/cancelling preparation/stopping its timer. `waitForIdleSync()` alone did not establish a new service lifetime. Host diagnostics show ~9–10GB available memory, 79GB disk free and no OOM/kernel kill evidence; this failure is an application test-process crash, distinct from the preceding VM-offline interruption.

Correction is test-only: reset explicitly quiesces the service on its own thread and awaits completion before row deletion; the recovery test separately waits for actual service destruction before rebinding, preserving its same-track recovery, automatic-reconnect-disabled and `assertNotSame` assertions. A new regression checks preparation cancellation/resource shutdown before fixture database cleanup. BLE transport and business data handling are unchanged; full suite rerun required.

## Accuracy boundary found during stage review

The mature recorder also applies the existing user-configured accuracy threshold (strictly less than its value; default 50m). Readiness's independent ≤30m ceiling alone would be insufficient if that existing setting is stricter, e.g. 1m: it could create a workout whose initial 2m fix is then rejected by the recorder. The GPS callback now passes the exact cached recorder threshold to the preparation gate, so every confirming fix must satisfy both filters. No user setting or mature filter was changed. A new Android regression uses the real 1m recorder setting, proves 2m fixes cannot start, then proves 0.5m fixes start with exactly one GPS point. Test preference presence/value are restored. This correction remains part of stage 3 and requires another complete cloud run.

## Concrete leaked test binding identified

[Run 42184396681](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42184396681), source `4ac7a7cb83b06b079d66d865ae0262c4ecd84879`: [XML](evidence/stage3-run-42184396681/TEST-mediumPhone.xml) contains 291 records, 290 executed, 8 failures, 0 errors, 1 original skip; JVM 108 passed. The stronger destruction wait correctly failed instead of accepting the same service instance. Later legacy service cases inherited its stale data/fixed clock, then the instrumentation process crashed.

The specific binding leak was in the **new GPS UI fixture**: `prepare()` bound ServiceTestRule, then the real-GPS-metrics test bound the same rule again. That overwrote the rule's tracked connection and left the first binding alive. This is a test change introduced in this stage, not a demonstrated BLE transport defect. The earlier asynchronous cleanup observation was valid but incomplete: merely waiting cannot release a leaked binding.

Correction: the UI fixture now shares exactly one bound service per test across preparation/metric checks, explicitly unbinds it in teardown, then verifies actual service destruction. Original metric, lifecycle, new-instance, GPS and BLE policy assertions remain unchanged. Shared quiescent database cleanup is retained to prevent independent stop/delete races. Full cloud rerun required; stage 4 is still blocked.

## Avoid creating an idle service during fixture reset

[Run 42185032274](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42185032274), source `a5ee848b8221158afe835cf1b8fbc7a5c5116cf8`, failed in the first legacy UI case: 2 XML records including process-crash sentinel, both failures; JVM 108 passed. [Original XML](evidence/stage3-run-42185032274/TEST-mediumPhone.xml), [counts](evidence/stage3-run-42185032274/counts.json). Exact guest log sequence: TrackListActivity non-auto-create bind waiting → fixture reset unnecessarily created a service → helper unbound and destroyed it → delayed legacy Activity callback received that destroyed Binder and observed a null LiveData.

The test reset now first checks for an actual running own service instance (`pid > 0`), and simply stops/deletes fixtures if none exists. It never creates a service solely to clear an idle fixture. The destruction wait also distinguishes an actual instance from a pid-zero non-auto-create binding record. Active recordings/preparation still take the explicit quiescence path before database deletion. No production legacy Activity or BLE code was changed to mask this test-environment ordering issue. Full suite rerun required.

## Full suite executed; precision-fixture preference restoration corrected

[Run 42185916176](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42185916176), source `fe83a5a667c8a5876318d0a87d55b821a22a5460`: all 393 Android cases reported, 392 executed, 16 failures, 0 errors, 1 original skip; JVM 108 passed. [Original complete XML](evidence/stage3-run-42185916176/TEST-mediumPhone.xml), [counts](evidence/stage3-run-42185916176/counts.json). The UI service-binding leak is resolved; instrumentation completed and both original screenshot matrices were collected.

Actual first failure was the newly added precision fixture's `SharedPreferences.getInt` snapshot: the existing ListPreference stores a String, while the mature PreferencesUtils deliberately supports both String and Integer. The test setup threw ClassCastException and teardown wrote its uninitialized numeric field back, corrupting later tests' accuracy setting and causing GPS assertion failures. This was test-fixture contamination, not evidence for changing GPS algorithms or old assertions.

Correction: snapshot the raw preference Object, restore its exact String/Integer/absent representation, and assert the restored raw value/type equals the original. The stricter-accuracy production check and all GPS assertions remain. Complete rerun is required before acceptance.
