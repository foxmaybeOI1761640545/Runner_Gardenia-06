# P0.6 exact-source test and native visual acceptance

Accepted source: `7c27ab2e304a66761d871aeedeed5409627b4677` on `feature/p0-6-offline-map`, PR [#3](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/pull/3), based on unchanged P0.5.1 `ba00559af1af3d48bc3ecfde811d3e8d062e2f68`. Main tree: `3b7b266985667a8b057c5353535af56929b79a9d`.

Both exact-source full runs succeeded: [push43222588921](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43222588921) and [PR43222800515](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43222800515). Real report counts (tests / executed / failures / errors / skipped):

| Suite | tests | executed | failures | errors | skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Python pipeline helpers |137|137|0|0|0|
| Complete JVM |163|163|0|0|0|
| Complete API35 Android |452|451|0|0|1|
| Additional fast map/GPS/history/sensor subsuite |64|64|0|0|0|

The fast64 overlap the full452 and are not added to them. The sole inherited Ignore is `TrackRecordingServiceStateMachineTest.recording_stopGPS_noop`; it is not counted as passed. Raw XML was independently parsed and source coverage checked: no missing required classes or methods, no newly skipped tests. Debug, lint, complete JVM, instrumentation compilation, actual API35 execution, old UI/function regressions, GPX1.1, source/permission/runtime-dependency audits and real new-process probes passed. Android builds use Ubuntu24.04, JDK21, API37 compile, Build Tools36.0.0 and API35 emulator through Actions.

## Actual images inspected

Current-source 437 PNG contents were verified: 152 native PNGs in 19 contact sheets and the separate full-resolution private-map restart PNG were directly viewed; 284 additional current PNG app bodies exactly match the fully directly reviewed b8e59ac baseline. Only the known 32dp system status inset is excluded from equality; every app body pixel is compared. All content differences and GPS12/map16 core scenes require direct review even if equal. No unseen changed pixels are approved. App, Android instrumentation and JVM trees are identical between those sources; only signed Python harness/regression and documentation changed. [Per-image hashes, dimensions, inset policy, exact pixel comparisons and actual direct-review receipt](visual-content-comparison.json) are retained. This does not claim all437 new PNGs were directly viewed. Breakdown: original P0 20, P0.2 36, P0.3 56, P0.4 states192, P0.5 layouts48, metrics8, settings16, history32, GPS12, offline map16, map restart1. The actual content, not only file existence, was reviewed. Eight app chrome schemes, state controls, long125:25:30 duration, history selection/share/delete dialogs, stationary speed0.0/dash pace, real slow metric/imperial rates and retained distance are readable. Local vector park/roads/pond, all four map actions and native zoom controls are visible. Real zoom and touch pan, stable marker size, live track, separated relocation/history segments and invalid-file retention were inspected. Map uses the bundled standard light vector theme; eight app chrome schemes do not mean eight map render styles. No new blocking layout issue observed. Old route-card illustrations remain clearly labelled and are not evidence of the new map engine.

Map durability independently executed write1/read1 in distinct PIDs1725/2225 with force-stop and actual airplane mode: completed private map retained, interrupted pending import staged then cleaned, real native tiles rendered in the new process. Screenshot SHA256 `9fd38727b841d0019cfbf40c3333abec6b3ebf92faf6429257db6364324aeb3c`. Original database/metric/batch persistence uses one shared write1/read1 pair in PIDs1767/2261 covering three domains; it is not six probes.

GPX fixtures are anonymous synthetic inputs. Static fixture retains255 timestamps, one coordinate, zero speed/distance and255 heart samples. Reconnect fixture has4 GPS points/two segments/one heart sample; relocation+gap has6 points/three segments. Copied/raw duplicate evidence files are not additional scenes. Continuous .5/.7/3/10mps recording regressions retain30/42/180/600m over60 seconds within .15m tolerance.

## Immutable evidence

- Reports artifact13948469065: SHA256 `af839d7dba6b35b4ae7585bb09f1516b05648c4192224a56000e04ebf5b99ff9`.
- Visual artifact13948419101: SHA256 `54628edff445d3602cb13cb7b5be16e8cfa68a2736d7c12e3ada4715084d7080`.
- Runner-generated review-transfer archive:167761187 bytes, SHA256 `677446cfd222fdfb348d989a701bbe48adb1291fe771b6827bb6c061def56708`; chunk hashes, Git blob identities, archive hash and extracted source identities verified. This synthetic report transfer excludes APK and signing secrets. Original artifacts retained90 days; failed development reports remain retained separately.

The visual approval is exact-source and evidence-bound. Its activation commit changes approval/docs only; app, tests, CI, versions and ledger remain frozen. Fixed-key signing, original APK bytes, actual install/upgrade, signed native88 plus runtime-map4 review and anonymous public download are separate pending gates at this point. Version reserved through release-ledger: `v0.1.0-p0.6 / 6778`, application `com.fox.runner`, tag `test-p0-v0.1.0-p0.6`. Existing certificate must remain `E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22`.

Physical GNSS, real BLE strap, large third-party maps, battery/background behaviour, TalkBack speech and actual WeChat remain NEEDS_DEVICE_VALIDATION. No private track or original uploaded personal data was committed.

This acceptance replaces the b8e59ac approval after correcting signed runtime historical-route evidence. The signed harness now enters the actual recorded workout through History, verifies its native detail ID, opens Offline map from its real menu, and requires actual teal multi-point route pixels on the historical image (not just a basemap or single position dot). This was a harness coverage correction, not an app change. Both full CI runs and actual-source visual verification above were repeated on7c27ab2. Failed sign43195155793 and superseded/cancelled sign43219379925 remain retained and unaccepted, without any public P0.6 release; see [development failures](01-development-checks.md). The signing activation changes only approval and documentation.
