# Runner Gardenia P0.7–P1.0 Master Development Prompt

## Mission

Complete one continuous development program from the accepted P0.6.1 baseline. The user has authorized the full scope in one instruction; do not stop after each milestone to request another prompt or repeat questions. Work sequentially: finish, test, document, and publish each milestone before beginning the next.

This file is the source-of-truth execution brief. Update its status and append evidence as work progresses.

## 0. Baseline and repository safety

- Accepted application baseline: P0.6.1 source commit `6cee1958a23bfd1a1fe3b6a9b547717c528cbc75`.
- Accepted evidence/documentation head: `ae68319361c1277e97e4fe9bfc9ae52de88146e5`. The four commits after the source commit add acceptance/publication evidence only; the code tree is unchanged.
- P0.6.1 immutable release tag: `test-p0-v0.1.0-p0.6.1`.
- The repository default branch `main` is stale and does not contain the accepted P0.6.1 line. Never use `main` as the development base and never force-reset it. Develop from the accepted evidence head and use a dedicated release branch based on that line.
- Preserve the accepted P0.6.1 source, signing identity, application ID `com.fox.runner`, existing database and workout history, existing map files, BLE heart-rate support, offline-only policy, and all existing user settings.
- Do not rewrite history, move existing tags, delete existing releases, or replace published APK assets.
- Do not claim a build, test, release, or device verification succeeded unless the actual evidence exists.
- The accepted P0.6.1 real-map binary test depends on the external 105,834,033-byte fixture identified by SHA-256 `5412067262dace98d9f95b02621446516e7d98e1debbb6a314d575cae022ee1e`. The fixture is not committed to the repository or present in this Actions run. Keep the test active when `RG_REAL_MAP` is supplied; otherwise report it as skipped and rely only on the immutable P0.6.1 acceptance evidence. Never fabricate a map fixture or claim that binary validation ran.

## 1. Product decisions already confirmed

- The home-screen map should show a real Mapsforge offline map, not the current illustrative Canvas terrain.
- The map is a geographic reference while the user moves. It should show current position and the current workout track when recording.
- Location preview must operate whenever the app is in the foreground, regardless of which Runner Gardenia screen is visible.
- If no offline map covers the current location, continue locating and show a neutral/blank map surface with the current-position marker and a clear no-map state. Absence of map data must never stop GPS.
- Idle location preview stops when the app leaves the foreground or the device locks. Do not request background-location permission for idle preview.
- A formal workout must continue locating and recording while the user switches apps, changes screens, or locks the device. It ends only through the app's explicit finish/pause controls or unavoidable OS termination (for example force-stop or permission revocation).
- Idle preview must not create a workout, persist track points, increment workout duration/distance, or associate idle heart-rate samples with a workout.
- Reuse the existing TrackRecordingService and GPS handler. Do not create a second GPS provider or duplicate location subscriptions.
- A transition from idle preview to recording must reuse the live GPS source without a needless stop/restart or avoidable location gap. When a workout ends while the app remains foreground, return to idle preview.
- The user most often holds the phone in hand, but may also carry it in a pocket, waist pack, or arm band. Map orientation must therefore be north-up; do not assume the phone is mounted on the body or use phone heading as route direction.
- GPX import is for routes the user has planned elsewhere. The first implementation does not need automatic route generation or an embedded BRouter engine.
- Step/gait analysis is design-only in this program. Do not add IMU collection, runtime permissions, sensor persistence, or gait UI in the implementation stages.

## 2. Sequential implementation milestones and release gates

### Milestone A — P0.7.0: home offline map + foreground location lifecycle (requirements 1–3)

1. Replace the illustrative home route panel with a compact real Mapsforge map using the currently selected local map from OfflineMapRepository.
2. Show a distinct current-position marker and a GPS-accuracy indication when accuracy is available.
3. Show the actual current workout path as an overlay while recording; preserve track segment gaps and never join across missing-data segments.
4. Keep the map north-up. Auto-follow the current position while the home map is visible, with sensible zoom and no needless zoom oscillation. Avoid reloading the map file or recreating the renderer on each GPS fix.
5. When no map is selected or the current position lies outside available map coverage, show a neutral blank surface and current-position marker. Keep the location preview active.
6. Add an application/process foreground lifecycle owner (or equivalent robust application-wide mechanism) so location preview remains active across all in-app activities/screens. Do not tie it only to P0Activity or P06MapActivity.
7. Use a stable GPS owner token and reference-counted ownership. Home preview, full map, and workout recording must share the existing service and GPS source without duplicate subscriptions or premature release.
8. On first foreground entry, start preview only if location permission is already granted. Existing permission UI must allow the user to grant it; after permission grant or return from system settings, preview starts without requiring an app restart.
9. On leaving foreground/locking while idle, release only the idle preview owner and stop GPS preview. Do not stop an explicit BLE heart-rate preview that was independently requested.
10. Starting a workout while preview is active must transition to recording without dropping the GPS pipeline. Ending a workout while foreground must restore idle preview. Pausing/resuming must retain the existing workout semantics and never create a distance bridge.
11. Keep the foreground-service notification accurate: idle preview says location preview, active recording says recording. Respect Android foreground-service type/permission requirements. Do not add ACCESS_BACKGROUND_LOCATION for idle preview.
12. Add unit/instrumentation coverage for lifecycle transitions, multiple owners, permission denial/grant, empty map, selected map, renderer stability, workout handoff, lock/background behavior, and no accidental workout writes.
13. Preserve all current P0.6.1 map repository and real-map evidence behavior.

Gate: full existing CI checks, unit tests, lint, API 35 instrumentation suite, signed reproducible APK, APK identity/signature verification, immutable tag, and pre-release publication must pass. Only then begin Milestone B.

Target release: `v0.1.0-p0.7.0`. Derive the next version code from the release ledger/high-water mark; the expected next code is 6780 unless verified repository history proves otherwise.

### Milestone B — P0.8.0: independent GPX planned-route library (requirement 4)

1. Create a route-plan domain model and independent local RouteRepository. Planned routes are not workouts and must never be inserted into the existing Track/TrackPoint database.
2. Add a SAF document picker for GPX import. Import entirely offline and copy validated GPX data into app-private route storage with atomic/interrupt-safe behavior.
3. Parse GPX 1.0/1.1 route data (`rte/rtept`) and track data (`trk/trkseg/trkpt`). If a file contains planned route points, use those; otherwise support track geometry as a route. Preserve segment boundaries. Ignore unrelated waypoint data unless explicitly surfaced as optional route markers.
4. Validate coordinates, malformed XML, file size, point count, empty routes, and unsupported content. A failed import must not corrupt or replace previously imported routes.
5. Persist route metadata (stable ID, display name, source filename, import time, point count, calculated route distance, segment count) and route geometry. Use app-private storage and a recoverable atomic index.
6. Add a route library screen with import, preview, select/follow, rename if safe, export/share GPX, and delete with confirmation. Keep route management separate from workout history and map-file management.
7. Reopen route library after process restart and verify route files survive. Do not silently delete or rewrite source GPX geometry.
8. Add parser/repository tests for GPX rte, trk fallback, multiple segments, malformed/oversized files, invalid coordinates, interrupted writes, duplicate imports, deletion, and process recreation.

Gate: full CI plus real SAF import and process-restart persistence tests. Only then begin Milestone C.

Target release: `v0.1.0-p0.8.0`. Expected next code 6781, subject to release-ledger verification.

### Milestone C — P0.9.0: GPX route following and navigation-style guidance (requirements 5–6)

1. Extend map rendering to display three distinct layers where applicable: offline basemap, planned GPX route, and actual recorded workout track, plus current position. Planned route and recorded track must not overwrite each other.
2. Add a RouteMatcher that projects current GPS fixes onto route segments, computes cumulative progress and remaining distance, handles date-line wrapping, segment breaks, loops/crossings, and poor GPS accuracy.
3. Constrain route matching to a continuity window around the prior progress so a crossing or nearby parallel segment cannot cause large progress jumps. Progress may move backward only within a small hysteresis window; never jump to the end of a loop because it is geographically close.
4. Add route-follow session state independent from recording state. Users can follow a route without recording; record while following; pause/finish recording while continuing to follow; or stop following without ending a workout.
5. Provide navigation-style map-follow behavior: current position, planned line, completed/remaining route distinction, progress percentage, travelled/remaining route distance, and clear GPS-quality state.
6. Detect off-route only after consecutive valid fixes beyond a threshold that accounts for reported horizontal accuracy. Use hysteresis to avoid alert oscillation; suppress off-route claims when GPS quality is inadequate. Provide a clear return-to-route state when the user rejoins.
7. Add an optional, rate-limited vibration alert using the existing VIBRATE permission. It must be user-configurable and must not vibrate repeatedly on every fix. No internet, cloud route matching, or location upload.
8. Keep the map auto-following the user during an active follow session, with a manual recenter action. If the user manually pans, temporarily suspend auto-follow until recentered.
9. Do not claim turn-by-turn instructions where GPX has no turn metadata. Do not implement automatic road routing or BRouter in this milestone. The supported experience is offline GPX route following and deviation guidance.
10. Route follow must survive Activity recreation and screen changes. Define explicitly whether it survives process death; if it does, restore only safe route/session state and require a fresh GPS fix before guidance resumes.
11. Add deterministic unit tests for route projection, cumulative distance, cross-track distance, loops, crossing segments, gaps, poor accuracy, hysteresis, off-route confirmation/recovery, and route progress monotonicity. Add instrumentation tests for route import → follow → simultaneous recording → screen change → stop recording → continue following.
12. Provide a clear user-facing route-follow status and accessibility descriptions. Avoid cluttering the existing recording metrics.

Gate: full CI, instrumentation, route-matcher regression, offline airplane-mode test, process/activity lifecycle tests, and signed reproducible APK with immutable tag and verified publication.

Target release: `v0.1.0-p0.9.0`. Expected next code 6782, subject to release-ledger verification.

### Milestone D — final design-only deliverable: gait/step analysis

Do not implement any runtime gait-analysis functionality. Produce a technical design document only, covering:

- Primary use case is handheld phone; secondary placements include pocket, waist pack, and arm band.
- Which Android sensors are available and what they can/cannot measure.
- Why handheld acceleration/rotation is not a reliable basis for left/right foot asymmetry or clinical gait conclusions.
- Optional future Step Detector/Step Counter and IMU strategy; sampling rates, timestamp handling, sensor availability, permission model, battery cost, and missing-sample handling.
- A placement-aware confidence model and calibration experiment.
- Candidate metrics (step count, cadence, inter-step interval, estimated average step length) and explicitly experimental metrics (vertical movement proxy, impact proxy, left/right alternation proxy).
- Proposed independent sensor-data storage separate from GPS TrackPoints; raw data retention, derived summaries, algorithm versioning, device/placement metadata, export and deletion controls.
- A staged PoC/test matrix for handheld, pocket, waist, and arm placements, with a clear rule that no accuracy claims are made before device-specific validation.
- Privacy-first/offline-only architecture and explicit non-medical positioning.

Add the design document and final project summary to the repository after Milestones A–C have passed. No APK release is required for design-only documentation.

## 3. Architecture constraints

- Reuse Mapsforge 0.25.0, OfflineMapRepository, OfflineMapView, TrackRecordingService, GpsHandler, P0RecordingViewModel, existing GPX conventions, and existing UI theme system where appropriate.
- Keep GPS collection independent from map rendering. Renderer failures, absent map coverage, and route-file failures must never stop GPS or recording.
- Keep idle preview, recording, and route-follow ownership explicit and independent.
- Preserve raw GPS/TrackPoint data. Display simplification must not mutate stored workout points.
- Avoid high-frequency full-map reloads or rebuilding all map layers every second. Update only position/track overlays and viewport as required.
- No internet permission, online tiles, map download service, account, cloud sync, analytics, ads, or location upload.
- Do not add database schema changes unless necessary; justify and test any migration.
- All IO (GPX/map files/index) belongs on a worker executor; all view changes belong on the main thread.
- Respect Android 14+ foreground service location requirements and Android 12+ runtime sensor/location permission rules.
- Use Java 17-compatible code and existing Gradle/AGP/JDK 21 build pipeline.

## 4. Release and repository workflow

- Keep one long-lived development branch and a dedicated release branch rooted at the accepted P0.6.1 evidence head. Do not publish from stale `main`.
- Extend the existing Android Actions pipeline only as needed so a push to the dedicated release branch can run the same full checks, fixed-key signed build, exact-commit tag, and resumable pre-release publication as main. Keep all existing main behavior intact.
- Update release-policy tests for the P0.7/P0.8/P0.9 version series and dedicated release branch. Never bypass the release ledger, test gate, APK digest, certificate verification, immutable tag, or publication receipt.
- Publish only after each milestone's CI and source review pass. Do not publish a debug APK as a release.
- Release checkpoints: after A publish P0.7.0; after B publish P0.8.0; after C publish P0.9.0. The final gait design document is included with the C completion summary and is not a separate code release.
- Each milestone gets a concise delivery report containing commit SHA, PR, workflow run, test totals, APK version/code, signing certificate identity, APK SHA-256, release URL, and any remaining real-device checks.
- The user builds/tests through GitHub Actions and downloads the signed APK on a phone. Do not ask the user to install Android Studio/SDK or build locally.

## 5. Completion definition

The program is complete only when:
- A, B, and C are implemented, reviewed, fully tested, signed, and published in sequence.
- D is delivered as a design document only.
- Existing P0.6.1 upgrade/data-preservation guarantees remain intact.
- No milestone is started before the preceding milestone's gate passes.
- The final summary links every PR, workflow run, release, and design document and clearly distinguishes automated test evidence from real-device checks still requiring the user.
