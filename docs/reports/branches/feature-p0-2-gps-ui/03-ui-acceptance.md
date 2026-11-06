# P0.2 home UI acceptance

Status: IN_PROGRESS. No final screenshot/visual acceptance or next-stage permission is claimed yet.

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
