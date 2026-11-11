# P0.5.1 — GPS motion quality and stationary-distance correction

## Why this phase exists

The 2026-11-11 export comparison shows that iGPSPORT omits many stationary intervals while Runner Gardenia continues writing one-second points. This is consistent with bike-computer auto-pause, not proof of its setting: the iGPSPORT GPX contains timestamps/coordinates but no explicit speed extension, so its pre/post speeds below are derived from adjacent coordinate fixes. Runner Gardenia GPX includes speedMetersPerSecond.

Strong matched examples (Beijing time):

| iGPSPORT point gap | Phone points in the same interval | Phone speed evidence | Phone position drift |
|---|---:|---|---:|
| Nov 7 19:33:58–19:48:51 (893 s) | 893 | 98.3% ≤ 0.3 m/s; deceleration immediately before the gap about 1.09 → 0.83 → 0.64 → 0.46 m/s | max ~8 m |
| Nov 7 19:51:24–19:59:46 (502 s) | 502 | 98.8% ≤ 0.3 m/s; deceleration about 1.08 → 0.93 → 0.79 → 0.70 → 0.59 m/s | max ~2.8 m |
| Nov 7 19:10:29–19:14:07 (218 s) | 218 | 100% reported 0 m/s | ~0 m |
| Nov 9 18:59:04–19:03:10 (246 s) | 246 | 97.6% ≤ 0.3 m/s; phone speed falls 1.47 → 1.12 m/s immediately before and rises 0.54 → 3.65 m/s after | max ~3.7 m |
| Nov 11 18:56:22–19:00:36 (254 s) | 254 | 100% reported 0 m/s | max ~0.6 m |

These are strong stationary/auto-pause signatures. They do not prove the iGPSPORT auto-pause preference was enabled; a manual stop or device-side recording interruption can also create a gap. In the iGPSPORT files, normal sampling is approximately 2 s; Gardenia is approximately 1 s. Point-count comparisons must therefore account for both the different sampling rates and omitted stationary points. GPX first-to-last timestamps are wall-clock spans; an omitted stationary interval remains a timestamp gap and should not be described as elapsed time removed unless the device's moving-time field is separately compared.

## P0.5.1 implementation scope

1. Add a pure-Java GpsMotionFilter with hysteresis:
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
