# P0.6 exact-source test and native visual acceptance

Accepted source `af6e90eabee20ada79ba942584ab7eb8c08660e3` on feature/p0-6-offline-map, [PR#3](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/pull/3), based on unchanged P0.5.1 `ba00559af1af3d48bc3ecfde811d3e8d062e2f68`. Main tree `3b7b266985667a8b057c5353535af56929b79a9d`.

Full exact-source [push43235456414](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43235456414) and [PR43235469449](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43235469449) succeeded. Raw XML was independently parsed; no missing required classes/methods or new skips.

| Suite | tests | executed | failures | errors | skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Python helpers | 138 | 138 | 0 | 0 | 0 |
| Complete JVM | 163 | 163 | 0 | 0 | 0 |
| Complete API35 Android | 452 | 451 | 0 | 0 | 1 |
| Additional fast map/GPS/history/sensor subsuite |64|64|0|0|0|

Fast64 overlap full452, not added. The sole inherited Ignore is TrackRecordingServiceStateMachineTest.recording_stopGPS_noop and is not counted passed. Actual Debug, lint, JVM, instrumentation compilation/API35, P0–P0.5.1 UI/function regressions, GPX1.1, offline/dependency/manifest/cross-UID and process gates passed. Ubuntu24.04/JDK21/API37 compile/Build Tools36/API35 toolchain ran through Actions.

## Actual current PNG content verification

All437 current-source PNG contents verified: 149 directly viewed (19 contact sheets/148 matrix PNGs plus separate full-resolution process PNG), and 288 current app bodies exactly equal to the fully directly viewed b8e437 baseline. Only explicitly known32dp system status inset is excluded from equality; every app-body pixel compared, unknown dimensions receive no mask. All differences and GPS12/map16 scenes require direct review regardless of equality. Product/AndroidTest/JVM trees exactly match baseline; only CI/Python harness/critical helper regression/docs changed. [Full per-image hashes, dimensions, equality and actual-view receipt](visual-content-comparison.json) retained; this does not claim all437 were newly directly viewed.

Breakdown: P0 20/P0.2 36/P0.3 56/P0.4 states192/P0.5 layouts48/metrics8/settings16/history32/GPS12/map16/process1. Actual local vector park/roads/pond, four visible map actions, native zoom, current marker, live and separated historical routes, invalid-file map retention, stationary zero speed/dash pace, real slow metric/imperial rates, long duration, heart/state/history/dialogs/eight schemes inspected. No new blocking clipping/overlap. Bundled standard light map render theme is shared across eight app chrome schemes; old home illustrations are clearly labelled and not used as map-render evidence.

Map durability write1/read1 uses distinct PIDs1848/2212, actual flight mode and force-stop: completed private copy retained, partial pending import cleaned, native tiles rendered after restart. Screenshot SHA256 `c467a95420c2d3a391744833186ae08b3042cf586d094d115f963e3e50af9e53`. Original DB/metric/batch share one write1/read1 pair PIDs1742/2231 covering three domains, not six probes.

Synthetic GPX: static255 timestamps/one coordinate/zero speed-distance/255 heart samples; reconnect4 GPS/two segments/one heart sample; relocation+gap6/three segments. Raw/copied duplicates are not extra scenarios. Continuous .5/.7/3/10mps60-second recording regressions assert30/42/180/600m within.15m. No personal user tracks committed.

## Immutable evidence and retained failure repair

- Reports Artifact13953878314, SHA256 `676851c6a38d339ea49efd64ca89d729d5719ed5288c5d0064bb6be0cb25f087`.
- Visual Artifact13953823411, SHA256 `7a9025e036fd4946d669b1224fe2c758d5f4f56cc5ca285e1e4d62d76a3bc894`.
- Review transport 168,544,096 bytes, SHA256 `1d80673e1795855d48917896d50e31c39a74e1fa876daba421a4455103b62014`; all chunk/Git blob/archive/source identities checked. No APK/key data or ref update. Original/failed artifacts retained90 days.

This approval supersedes7c after sign43229996107 failed in Neon-large-font map return. The actual retained failure PNG/XML/focus were independently viewed: focused P0Activity and home ScrollView, title scrolled offscreen, no FATAL/ANR in retained logcat. The four real signed map runtime pictures also showed local import, true separated historical route, current/live route and corrupt import retaining the map; overall failed run remains unaccepted. [Retained failure review](retained-signing-failure-review.json) and [all development failures](01-development-checks.md).

AF restores the actual focused home viewport using a native gesture before retaining the original visible-title assertion; regression rejects foreign/wrong windows. No app/Android/JVM product changes. Fresh full CI/current PNG review above completed before a docs/approval-only signing activation. Signed88+4 actual image review, fixed-key install/upgrades and anonymous exact-byte publication remain separate pending gates at this point.

Ledger version v0.1.0-p0.6/6778, com.fox.runner, tag test-p0-v0.1.0-p0.6. Original certificate E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22. Physical GNSS/BLE, large maps/long sessions/background/battery, TalkBack speech/real WeChat remain NEEDS_DEVICE_VALIDATION.
