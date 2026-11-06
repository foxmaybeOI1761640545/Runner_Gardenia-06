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
