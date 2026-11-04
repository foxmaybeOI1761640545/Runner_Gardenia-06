# P0 physical device acceptance — NEEDS_DEVICE_VALIDATION

Automated tests use explicit synthetic GPS/BLE fixtures. No outdoor GPS handset or Magene H303 is attached to Codex/Actions. All items below remain unverified until a person executes them; simulator success is not RF compatibility.

## GPS

1. Install the final fixed-key P0 Release over the baseline without clearing data; confirm old recordings remain readable.
2. Disable Wi-Fi/mobile data, retain phone GPS and Bluetooth as needed. Open P0 and grant precise location; denial must show a recoverable state.
3. Leave Bluetooth permission denied, start a running workout outdoors. Confirm GPS state, latitude/longitude, accuracy, timestamps, distance, speed and pace update. A 1000ms request is a target; verify real provider delivery intervals rather than assuming exact hardware 1Hz.
4. Walk/run a known route, pause, move while paused, wait, resume elsewhere. Confirm no paused distance/moving time and no line across the pause in exported segments.
5. Block GPS reception or enter a building, then return outdoors. Confirm loss/recovery and sensible segmentation, no spurious distance jump.
6. Switch apps, turn screen off for at least 30 minutes, then reopen. Confirm recording continued in the foreground service and UI restored the same workout.
7. Finish and reopen history after a process/device restart. Confirm timestamps, distances and saved samples persist.

## H303 and generic SIG HR straps (to be expanded with BLE stages)

- Actual H303 discovery/name/MAC, subscription, live BPM/contact/optional RR/energy.
- Strap power-off/on, out-of-range/re-entry, Bluetooth off/on, denied/revoked permissions, explicit disconnect and disabled automatic reconnect.
- Background/screen-off endurance and real RF retry intervals; no duplicated samples or old-session callbacks.
- Preserve local history, GPX raw samples and fixed-signature install/update behavior.
