# P0.5.1 — GPS motion quality and stationary-distance correction

## Why this phase exists

Stationary GNSS noise must not inflate recorded distance or invent a pace. The private user-supplied archive is kept outside the repository. Public tests and evidence use synthetic coordinates and clocks only; no user GPX or personal route is included.

The existing P0.5.1 implementation is continued on its original branch. The acceptance fixture retains 255 one-second stationary points with 0–0.3m/s reported noise and all heart-rate samples, and separately exercises 60-second true movement at 0.5, 0.7, 3 and 10m/s.

## P0.5.1 implementation scope

1. Review and correct the existing pure-Java GpsMotionFilter with hysteresis:
   - pin stationary GPS coordinates to a stable anchor while retaining the time samples;
   - clamp stationary/near-zero speed to 0 and render pace as `—`;
   - require consistent movement evidence before releasing the anchor;
   - reject isolated impossible jumps;
   - after a persistent, consistent relocation or a long fix gap, mark a new automatic track segment rather than drawing a false connecting line.
2. Apply the filter only to recorded GPS fixes. Keep the original accuracy/time fields and the independent BLE heart-rate path.
3. Tighten the initial GPS readiness jump allowance for accurate fixes.
4. Preserve the existing one-second GPS request, GPX schema, database ownership, fixed signing key and existing P0.5 UI/history/export flows.
5. Add deterministic unit tests and Android recording-path tests for stationary drift, stop/start hysteresis, isolated/persistent jumps, and segment boundaries.

## Acceptance gates

- Stationary points remain time-stamped; their coordinates stop wandering and do not increase distance.
- Speed is `0.0` at rest and pace is `—`; low positive GPS noise never produces extreme pace.
- Slow sustained movement can leave stationary state; normal running/cycling speed is not clamped.
- A one-off jump is discarded. Repeated consistent relocation or a >15 s GPS gap creates a route segment boundary.
- Existing GPS readiness, BLE heart-rate capture, pause/resume, history, GPX export and signed upgrade regressions remain green.
- GitHub Actions must run the selected development tests, then the complete P0.5 regression and actual API 35 visual/process evidence before signing.
- The APK must be signed with the existing fixed key, verify against the P0.4 certificate, install/upgrade successfully, and be anonymously downloadable before P0.6 begins.

## Explicit non-goals

- No auto-pause of the Gardenia recording session. Gardenia continues recording elapsed time and retains stationary time points.
- No deletion of stationary samples.
- No ANT+ changes, cloud services, network map tiles or online dependencies.
- No replacement of the existing recording service/database/GPX serializer.
