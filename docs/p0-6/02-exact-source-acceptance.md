# P0.6 exact-source test and native visual acceptance

Accepted source: `6156f2cbfa2c5fa3fdca4ddd6372032ab79ad74d` on `feature/p0-6-offline-map`, PR [#3](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/pull/3), based on unchanged P0.5.1 `ba00559af1af3d48bc3ecfde811d3e8d062e2f68`. Main tree: `3b7b266985667a8b057c5353535af56929b79a9d`.

Both exact-source full runs succeeded: [push43188522568](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43188522568) and [PR43188710832](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43188710832). Real report counts (tests / executed / failures / errors / skipped):

| Suite | tests | executed | failures | errors | skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Python pipeline helpers |136|136|0|0|0|
| Complete JVM |163|163|0|0|0|
| Complete API35 Android |452|451|0|0|1|
| Additional fast map/GPS/history/sensor subsuite |64|64|0|0|0|

The fast64 overlap the full452 and are not added to them. The sole inherited Ignore is `TrackRecordingServiceStateMachineTest.recording_stopGPS_noop`; it is not counted as passed. Raw XML was independently parsed and source coverage checked: no missing required classes or methods, no newly skipped tests. Debug, lint, complete JVM, instrumentation compilation, actual API35 execution, old UI/function regressions, GPX1.1, source/permission/runtime-dependency audits and real new-process probes passed. Android builds use Ubuntu24.04, JDK21, API37 compile, Build Tools36.0.0 and API35 emulator through Actions.

## Actual images inspected

All56 contact sheets /436 native PNGs were actually viewed, plus the separate full-resolution private-map restart PNG (437 total). Breakdown: original P0 20, P0.2 36, P0.3 56, P0.4 states192, P0.5 layouts48, metrics8, settings16, history32, GPS12, offline map16, map restart1. The actual content, not only file existence, was reviewed. Eight app chrome schemes, state controls, long125:25:30 duration, history selection/share/delete dialogs, stationary speed0.0/dash pace, real slow metric/imperial rates and retained distance are readable. Local vector park/roads/pond, all four map actions and native zoom controls are visible. Real zoom and touch pan, stable marker size, live track, separated relocation/history segments and invalid-file retention were inspected. Map uses the bundled standard light vector theme; eight app chrome schemes do not mean eight map render styles. No new blocking layout issue observed. Old route-card illustrations remain clearly labelled and are not evidence of the new map engine.

Map durability independently executed write1/read1 in distinct PIDs1733/2138 with force-stop and actual airplane mode: completed private map retained, interrupted pending import staged then cleaned, real native tiles rendered in the new process. Screenshot SHA256 `21630ba6e544abf68c22b37ee9352927f37d3f3466e61a0a717c54e63965f751`. Original database/metric/batch persistence uses one shared write1/read1 pair in PIDs1755/2222 covering three domains; it is not six probes.

GPX fixtures are anonymous synthetic inputs. Static fixture retains255 timestamps, one coordinate, zero speed/distance and255 heart samples. Reconnect fixture has4 GPS points/two segments/one heart sample; relocation+gap has6 points/three segments. Copied/raw duplicate evidence files are not additional scenes. Continuous .5/.7/3/10mps recording regressions retain30/42/180/600m over60 seconds within .15m tolerance.

## Immutable evidence

- Reports artifact13930583661: SHA256 `dc49ef7949d0397f4cc267e4883070f7f03f80bec30c8b921eef89848fc8fa14`.
- Visual artifact13930568791: SHA256 `56cf05cb08b2ed1d7e620e004529f2341c3f319386d12cd9ae0d640674c1fd02`.
- Runner-generated review-transfer archive:167838757 bytes, SHA256 `c6442d17284331f44558e4269e19194b9b613c7a2cdf4020cff89001a5b96ad9`; chunk hashes, Git blob identities, archive hash and extracted source identities verified. This synthetic report transfer excludes APK and signing secrets. Original artifacts retained90 days; failed development reports remain retained separately.

The visual approval is exact-source and evidence-bound. Its activation commit changes approval/docs only; app, tests, CI, versions and ledger remain frozen. Fixed-key signing, original APK bytes, actual install/upgrade, signed native88 plus runtime-map4 review and anonymous public download are separate pending gates at this point. Version reserved through release-ledger: `v0.1.0-p0.6 / 6778`, application `com.fox.runner`, tag `test-p0-v0.1.0-p0.6`. Existing certificate must remain `E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22`.

Physical GNSS, real BLE strap, large third-party maps, battery/background behaviour, TalkBack speech and actual WeChat remain NEEDS_DEVICE_VALIDATION. No private track or original uploaded personal data was committed.
