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

## H303 and generic SIG HR straps

- Actual H303 discovery/name/MAC, subscription, live BPM/contact/optional RR/energy.
- Strap power-off/on, out-of-range/re-entry, Bluetooth off/on, denied/revoked permissions, explicit disconnect and disabled automatic reconnect.
- Background/screen-off endurance and real RF retry intervals; no duplicated samples or old-session callbacks.
- Preserve local history, GPX raw samples and fixed-signature install/update behavior.

## Reconnection controls — NEEDS_DEVICE_VALIDATION

1. Connect the selected H303 or another SIG strap, verify fresh BPM/name/update age and any RR/energy the device actually provides.
2. With automatic reconnection enabled, turn the strap off/out of range and back on; verify BPM clears during loss and the same selected device reconnects with a new connection identity. Retry delays grow to a 60-second cap; Connect/reconnect requests an immediate attempt.
3. Disable automatic reconnection while connected; the healthy link must remain. Power-cycle strap or phone Bluetooth: no automatic retry until an explicit Connect.
4. Press Disconnect during a workout, then toggle Bluetooth and restart/reopen the app. GPS and saved data must remain intact and the selected strap must remain remembered without reconnecting until Connect.
5. Re-enable automatic retry after an unexpected loss, then verify recovery; enable it after an explicit Disconnect and verify it does not itself reconnect.
6. Repeat screen-off/background endurance for 30 minutes and compare received sample timestamps/counts, especially consecutive equal BPM values. Synthetic emulator cases do not establish physical RF compatibility.

## H303 end-to-end procedure — NEEDS_DEVICE_VALIDATION

1. Wear/wet/activate the Magene H303 according to its instructions. In P0 heart-rate
   settings request the required BLE permissions, scan again, check the device
   name and MAC, and choose the intended strap manually. Do not rely on its name
   alone; another standard 0x180D/0x2A37 strap should follow the same flow.
2. Check Connecting -> subscribed/Connected -> real BPM, device name and update
   age. Compare against another trusted display where available. RR, contact and
   energy are optional: record what this actual firmware provides, without
   expecting fabricated values from devices that omit fields.
3. Start a running/cycling workout outdoors without Internet. Check GPS accuracy,
   speed, pace, distance/time and continuously arriving HR timestamps. Consecutive
   equal BPM readings must remain separate raw samples.
4. Pause, continue moving, then resume. Confirm paused time/distance are excluded;
   preview data from the pause must not be added to the resumed segment. Losing
   the strap must not finish the workout or erase earlier data.
5. Power the strap off, go out of range and disable/re-enable phone Bluetooth in
   separate trials. BPM must clear after loss, state/age must be truthful, and
   automatic reconnection must use bounded backoff and a new connection identity.
   Then verify manual Disconnect and automatic retry Off prevent further retries.
6. Finish; inspect history min/avg/max HR and motion data. Save and share GPX using
   local Android Files. Inspect the versioned raw extension for exact sample
   timestamps, IDs, device/session metadata and optional RR/energy. Samples with
   no GPS match must remain in the extension; no fabricated coordinate is allowed.
7. Force-stop/reopen and restart the phone, inspect the same saved workout. Repeat
   a controlled process interruption during recording and document any recovery
   state. Android force-stop does not promise automatic background restart.
8. Repeat Android 8–11 location-based BLE scan permissions and Android 12+ separate
   scan/connect permissions. Deny/revoke permissions in separate trials and check
   recovery without a crash. Notification permission is optional for recording.
9. Repeat screen-off/background sessions for at least 30 minutes under the actual
   phone's battery policy, including strap power cycles. Record battery use,
   sample counts/intervals, gaps, duplicates and reconnection delay.
10. Keep at least one real pre-P0 workout before an in-place signed APK upgrade.
    Compare its GPS/HR/metadata and exported GPX after upgrade. Do not uninstall
    or clear app data. Delete a separate test workout and verify its DB-linked
    GPS/HR rows disappear; user-exported documents remain user-owned files.

Record phone model/Android version, H303 firmware, permissions, timezone, APK SHA
and certificate, expected/actual result and logs for each trial. These items are
not automatically PASSED by synthetic UI/protocol/storage tests. No real device
has been connected to this execution environment.

## Xiaomi / HyperOS / MIUI and cross-timezone trials — NEEDS_DEVICE_VALIDATION

- Record handset model, Android/HyperOS/MIUI version and the app's GPS/BLE/notification
  grants. First test the default battery policy; then separately document any user-selected
  background-permitted policy. Do not assume autostart or additional permissions are required.
- Run 30-minute and two-hour screen-off workouts, including strap power cycles; compare
  start/end battery, temperature, notification visibility, GPS intervals, raw HR counts and
  reconnection delay. Repeat on a non-Xiaomi phone. Forced-stop is different from OS process
  reclamation: manually reopen after force-stop, without promising automatic background restart.
- Keep a completed workout, change device timezone from Asia/Singapore to UTC/Europe/Berlin,
  reopen history and export GPX. Local display should convert, while epoch timestamps, durations,
  sorting and exported absolute instants remain unchanged. Restore the original timezone afterward.
- Test Android system Save GPX and Share GPX with real local document providers while offline;
  verify the file externally without treating an optional cloud provider as an app dependency.
- Capture actual device/model/firmware/permission/timezone/APK-hash evidence. No item here is
  PASSED by the historical emulator screenshots or host timezone arithmetic.
