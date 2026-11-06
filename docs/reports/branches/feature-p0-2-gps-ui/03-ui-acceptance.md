# P0.2 home UI acceptance

Status: PASSED — final actual report and original image review below. Phase5 may start only after this report push and remote/cloud archive verification.

## Scope and implementation

- Home only: status panel, four distinct distance/active-time/speed/pace values, compact HR panel, persistent contextual bottom actions. Removed the inline history preview in favor of the existing history destination via a labelled header button; settings/history screens and repository/database/BLE implementation remain unchanged.
- IDLE and ACQUIRING show no prior workout metrics. Stored metrics must match the current session track ID. Unreliable/missing GPS hides coordinates/speed/pace; recording time and saved distance continue.
- Start/cancel/pause/resume/finish are shown by actual session state; primary actions stay in the viewport while data can scroll on a small screen. Permission and timeout guidance remain available. No new navigation tab.
- Material theme colors, contrast-aware tonal controls and surface text; shared layout with wider margins on large windows. Numeric values autosize rather than ellipsize. The four metric fields have a clear hierarchy; device, connection, last valid time and reconnect preference remain separate.
- HR uses the existing real preview/parser/status. Last valid timestamps use the device timezone. Reconnect text reflects existing user settings/manual-stop intent; it does not claim the audited BLE no-notification issue is fixed.
- Existing UI assertions retained against their new visible widgets; fixed buttons are clicked directly rather than scrolled into view. Launcher/history/settings entry assertions and old 40+20 screenshot gates remain.

## Local checks

- MotionFormatTest: six actual JUnit cases passed, including km boundary/invalid-distance formatting; no database values changed.
- Pipeline regression: 51 cases passed, including source/hash/theme/window/state/complete-matrix and hardware-boundary rejection checks.
- Java17 syntax parse, resource XML, actionlint and Git diff checks passed. Android resource/type compilation still requires the actual Actions run.

## Required actual screenshot matrix

Nine states × light/dark × small/large = 36 original API35 screenshots: IDLE, acquiring, GPS ready, recording, signal lost, paused, resumed, HR connected, HR disconnected/retry. Real P0 Activity/service/readiness/parser transitions are exercised with explicitly synthetic GPS/SIG notifications and a controlled clock. This is not H303/RF/GNSS hardware proof.

The Android producer records actual configuration, session phase, source SHA, capture epoch and each PNG SHA-256. A mandatory gate checks the exact set, complete PNG chunk CRCs, original hashes, actual window dimensions and theme metadata. Native assertions check contextual controls are visible/unclipped, labels do not ellipsize, stale data stays hidden and entry navigation creates no workout. Manual inspection of downloaded original images is still required; a green build alone cannot accept the UI.

Screenshots will be retained under `screenshots/` here after successful retrieval, with manifest/checksums; full matrices remain in the same-run Actions Artifact. All hardware items remain NEEDS_DEVICE_VALIDATION. Signing/Tag/Release are still disabled in this phase.

## First cloud run: automated PASS, visual acceptance FAILED

Run [42194157975](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42194157975), source 9b8642ac4886bc366809e9d7d116f551dda45601: actual Android XML 395/394 executed/0 failures/0 errors/1 skipped; JVM 110/110/0/0/0; 51 helper cases. Debug and all report gates passed. Downloaded original small-light idle/acquisition images showed the bottom HR reconnect label clipped by the scroll viewport; large-dark connected left excessive space between content and actions. This is not UI acceptance despite green CI.

Correction: tighter home spacing and shorter equivalent guidance; a centered content/action cluster for wide windows; a small gap between actions. Added strict full-height visibility checks for all five HR summary fields in every standard screenshot profile. Scroll remains available for larger accessibility fonts and permission/error guidance. No GPS/BLE/service changes. Full cloud regression and new original screenshot review are required before phase 5.

## Second cloud run: FAILED navigation synchronization

[42196726849](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42196726849), source7251966c18998dd018cbedcb84119b4281131fdb: Android395/394 executed/1 failure/0 errors/1 original skip; JVM110 all passed. The 36-state matrix and new HR full-visibility assertions passed. The only failed case was homeHistoryAndSensorEntriesNavigateWithoutCreatingAWorkout. Its native per-test log records `InputDispatcher: Dropping event because there is no touchable window` at17:45:38.930 while the finishing history window was still animating; history was destroyed only17:45:39.388. A RESUMED callback alone was insufficient input readiness.

Correction is test-only: wait for outgoing activity destruction AND home window focus, then drain the main thread before the next Espresso click. Preserve both actual destination/resume and no-workout assertions. No production navigation workaround or longer blind sleep, no skipped checks. Full current-stage rerun remains required.

## Third cloud run: FAILED legacy modal synchronization

[42198290386](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42198290386), source3f608902f7f280c07f8caa02a683818cfb0d2e5f: Android395/394/1 failure/0 errors/1 original skip; JVM110 passed. Both new home cases, including navigation and all36 captures, passed. The sole failure was original HeartRateUiScreenshotTest.realPagesInLightDarkAndTwoWindowSizes: its final Scan again check searched the nonfocused background SettingsActivity root after an asynchronous DialogFragment.show(). The native hierarchy explicitly reports background has-window-focus=false; the first three profiles passed the same assertion. Gate correctly rejected the incomplete40-image legacy set.

Correction is limited to that existing UI test: execute the requested fragment transaction, wait up to the existing15-second timeout for the actual modal window focus and painted neutral/rescan button, then keep the original displayed assertion with explicit isDialog root. All40 captures and original fixtures/visibility checks remain mandatory. BLE production code remains unchanged. Full stage rerun required.

## Final actual acceptance: PASSED

- Source: `48693c4021945fa3e1a94b2b53b94edacace766c`; [Actions42200094093](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42200094093) SUCCESS. [Independent XML/proof/file receipt](stage4-verification.json); [original native/JVM XML and auxiliary proofs](evidence/stage4-run-42200094093/).
- Main instrumentation: **395 tests /394 executed /0 failures /0 errors /1 original skip**. JVM: **110/110/0/0/0** across12 original XML reports. Helper regression:51 actual cases; [original job lines](evidence/stage4-run-42200094093/helper-tests.txt). No runner crash. The original recording_stopGPS_noop TODO remains skipped; no new Ignore.
- Debug build/Lint/instrumentation compile, old40 HR images, old20 P0 matrix,9 GPX schema documents, offline packaged Debug/merged Release audits all passed. Independently revalidated all9 downloaded GPX against pinned offline XSD. Durability phases each ran1 test,0 skipped/failed/errors, actual PIDs1807 and2282, force-stop verified.
- [Debug Artifact13494238058](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42200094093/artifacts/13494238058); downloaded actual packagecom.fox.runner.debug, reproducibleDebug, versionv0.1.0-p0.1/code6772, hash679648c56b2639a2cd55e109ed62a5fd7290cf403e87937cf9321d4c05125ea4. This intermediate Debug version is intentionally unchanged; formal P0.2 allocation/signing is phase6, not claimed here.
- [Full report/screenshot Artifact13495455094](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42200094093/artifacts/13495455094). The full36 new original PNGs, actual capture metadata and checksums are retained in [screenshots](screenshots/), [capture manifest](screenshots/p02-home-matrix.json), [SHA256SUMS](screenshots/SHA256SUMS).

### Actual visual review

All36 downloaded images were viewed in temporary review sheets, with critical small-window originals checked at full size. Review sheets were not substituted for original evidence or committed as screenshots. All9 states were examined in both themes at360dp and800dp width. No visible text truncation, overlapping controls, offscreen primary action or clipped HR summary in these standard-font profiles. Metrics, state text and contrast remain readable. Small windows use fixed contextual actions; wide windows group the content and actions centrally rather than separating them across an empty screen. Larger accessibility font scales can scroll; they are not claimed exhaustively screenshot-tested.

- IDLE:0 distance/00:00 and no coordinates, speed or pace; previous workout data is hidden. [Small light](screenshots/p02-home-small-light-idle.png).
- Acquisition: explicit GPS guidance/settings and Cancel GPS,0 distance/00:00, no fake metrics or track ID. [Small light](screenshots/p02-home-small-light-acquiring.png).
- GPS ready/recording/resumed: distinct status and correct12s/21s fixture active time,35.8m,10.8km/h and5:33/km. [Recording dark](screenshots/p02-home-small-dark-recording.png).
- GPS loss: recording remains active, stored distance remains35.8m, time continues20s, speed/pace/coordinates disappear. [Loss dark](screenshots/p02-home-small-dark-gps-lost.png).
- Pause: Resume/Finish visible; no live GPS speed. HR connected shows76bpm and explicit synthetic device; disconnect/retry clears BPM while preserving last valid10:00:31. GPS can independently become weak after its5s freshness window in the last large captures; this is correct independent status, not invented coordinate continuity. [Disconnected dark](screenshots/p02-home-small-dark-heart-disconnected.png).

All GPS/SIG data and the fixed10:00 fixture clock are explicitly synthetic. OS capture time, configuration and source SHA are recorded separately. Physical H303/RF/real GNSS/OEM background/power remain NEEDS_DEVICE_VALIDATION. BLE no-first-notification behavior remains audited only, not fixed. No signing, Tag or Release occurred in phase4.
