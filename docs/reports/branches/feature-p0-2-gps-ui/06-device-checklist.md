# P0.2 real-device acceptance checklist

All physical rows below: **NEEDS_DEVICE_VALIDATION**. No H303/real RF/outdoor GNSS/OEM power result has been supplied. Emulator XML, synthetic notifications and screenshots are not a substitute.

## Preparation and upgrade

- Record phone/model/Android and vendor OS version, H303 firmware, app version6773, timezone, theme/font/display scale. Keep sensitive addresses/track coordinates out of public screenshots/logs.
- Download only [this test APK](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.2/RunnerGardenia-P0-Release.apk); expected2753061bytes/hash48c4a855402b7bec0b310794a228f15323ef8f09b2eb81d4f6913ab56ac6d046. [Checksum file](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.2/SHA256SUMS). Existing OS checksum tools are sufficient; no Windows JDK/SDK/Studio required.
- Before updating P0.1, note historical record/sample counts, selected strap and reconnect setting; export important GPX locally. Install over P0.1 without uninstalling or clearing data. Confirm old records, raw HR counts/statistics and device selection survive; reopen/export an old record.

## GPS preparation / running / cycling

1. Deny Bluetooth/nearby-device permission and leave optional notifications denied; grant precise location only. Confirm GPS-only running/cycling still work, without a compulsory Bluetooth prompt.
2. With location disabled, tap Start. Expect acquiring guidance/settings and Cancel GPS,00:00/0distance/no coordinates/speed/pace. Wait and cancel: no empty history record, GPS/FGS preparation resources stop. Repeat through Activity rotation/background and cancellation.
3. Leave preparation60s without a reliable fix. Expect not-ready guidance, continue-wait/cancel/settings. No cached-coordinate forced start or timed workout. Continue waiting must remain no workout until readiness.
4. Move outdoors, enable location/precise permission, allow stable fixes. Note actual accuracy/readiness delay. Target≤30m, fresh≤5s, three fixes spanning≥1s plus any stricter pre-existing recorder accuracy filter. Verify timer starts only on readiness; waiting contributes no distance/points/time/associated HR. Compare against a known local distance. Real1Hz/precision remain to be measured, not assumed from request settings.
5. Record running and cycling: distance/time/speed/pace update plausibly. Pause, move while paused, resume: paused movement/time must not be counted; GPX segments must avoid bridging the pause. Finish once, reopen, verify saved data.
6. Obstruct GPS/tunnel or turn location off during recording. Recording/time continue, GPS weak appears, speed/pace/coordinates clear, distance/points do not fabricate motion. Restore GPS; recording resumes with existing gap/segment policy. Do not use satellite count alone as readiness proof.
7. Rotate/recreate home in acquiring/recording/paused states, switch apps and lock screen. Verify no duplicate workout or leaked acquisition session. Check foreground-service notification when allowed. Explicit OS force-stop is expected to stop sampling; after reopening, already saved records must remain readable. Distinguish this from ordinary background/lock-screen operation.

## H303 / generic standard BLE

1. Wear/power the H303 with good contact. Grant only the platform-required BLE/nearby permissions; older Android may need scan location. Heart preview must not start GPS or create a workout.
2. Scan standard Heart Rate Service devices, verify name/address privately and select exactly one. Record connection status and time until first actual BPM. Do not treat CONNECTED alone as successful data reception.
3. Reproduce the known automatic-connection/no-BPM symptom. Compare initial automatic route against one explicit reconnect; note status/first valid update times/contact status. This release only audits that issue, it does not claim a fix. Do not publish the device MAC/raw personal workout.
4. Once actual BPM is flowing, start GPS preparation: preview continues, no workout-associated samples before readiness. Then record and confirm timestamped raw samples, including consecutive identical BPM, continue saving.
5. Lose contact, power off/leave range, return/power on. Verify stale BPM clears, history remains, and actual reconnect behavior matches the enabled setting. Record failures rather than assuming success. Disable automatic reconnect; explicitly disconnect; neither should immediately reconnect without user intent.
6. Toggle phone Bluetooth off/on; deny/revoke permission and retry from settings; confirm no crash or old-session BPM. Repeat in background/lock screen. RR/Energy are optional: note whether the actual strap provides them, never manufacture missing fields.
7. Finish, reopen history and restart the app. Verify raw sample counts/times, min/avg/max and GPX raw-sample extensions; no cloud upload/account required.

## UI, offline and long-duration checks

- Inspect idle/acquiring/ready/recording/weak/paused/resumed/connected/disconnected in both themes and small/large screens; increase system font/display scale, rotate, check buttons/text/contrast and preserved history/settings. The CI matrix uses standard-font English, not exhaustive real-device localization/accessibility coverage.
- Disable Internet (keep GPS/Bluetooth enabled as needed); scan/connect/record/history/GPX save/share locally must work. No account/cloud backup/sync should be needed. Check system settings show no cloud backup enabled by the app.
- Run a realistic outdoor duration with screen off. Record elapsed time, samples/gaps, battery/thermal state, OS battery restrictions and vendor kills. Include Xiaomi/MIUI/HyperOS and another Android version when available. Do not relax permissions/retry policy or delete records merely to obtain a pass.

Record each row with PASS/FAIL, device/version, steps, observed state/timestamps and privacy-scrubbed evidence. Keep every unperformed row NEEDS_DEVICE_VALIDATION; report reproducible defects in a new task/version without replacing this APK/Tag or changing the fixed identity.
