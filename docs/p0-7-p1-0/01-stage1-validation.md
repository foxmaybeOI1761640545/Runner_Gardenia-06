# Stage 1 validation log (not accepted)

Candidate source: 7ed7fb0d57d235e6a50ff7cc97b54a40d08d90d6. Draft PR: https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/pull/5 (base feature/p0-6-map-real-validation).

Run https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43735700103 failed correctly at lint. The new Handler.hasCallbacks call requires API29 while minSdk remains API26. This is an implementation defect, not a reason to change minSdk or suppress lint. Fixed in c0275174634c73becf0c454ea67fd0ef768c548b using explicit scheduled state and main-thread dispatch. Native test inputs now enter the actual shared GpsHandler, keeping live ViewModel refresh consistent with map screenshots and asserting idle preview creates no history. The original GPS preference/ownership tests remain unchanged. Renderer-attached caches are destroyed once by Mapsforge destroyAll; the empty-map cache is released separately.

Actual first-run JVM reports: Tests 167, Executed 167, Failures 0, Errors 0, Skipped 0 (raw-report inventory summarized in evidence/stage1-run43735700103-jvm.json). Python helpers were not reached in that failed CI run; the separately executed local helper suite is 141/141 with 0 failures/errors/skips. Android instrumentation did not run; missing reports fail the gate, not a pass.

The old main/RC workflow also ran on PR opening and rejected the P0.x version series (Run 43735729281). It signed nothing and published nothing. Only its prepare-job PR exclusion now additionally recognizes the clean branch; main's trigger, dependency chain, version allocator and signing/publication jobs are unchanged. The new branch's authoritative validation runs on exact-source pushes, avoiding duplicated full-suite jobs on the same PR source. Old failure records are retained, never relabeled.

Fixed-source validation run: https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43735969185. Pending results cannot authorize Stage 2, signing, tag creation or a release.

## Native layout failure and inspection

Run 43736726705 (source c6b17a56b27f2630bbff23ae745672a15a5b0401) executed Python 141/141 and JVM 167/167 with 0 failures/errors/skips. The focused Android run had Tests 2, Executed 2, Failures 1, Errors 0, Skipped 0. It failed the new real native control-rectangle assertion: recenter overlapped Mapsforge zoom controls on compact layouts. The complete suite was correctly not started. No acceptance or release is authorized by this run.

Actually inspected the native p07-home-no-map.png and p07-home-inside-bounds-no-features.png from this run. The map areas are neutral/empty with a position marker and truthful map status, rather than illustrative terrain/roads. Both screenshots demonstrate the control overlap. The recenter control now uses the bottom start corner; the compact home map omits its competing scale bar (the independent full map retains its scale bar). The layout assertion remains intact. Visual review also exposed that idle GPS status and accuracy still said unavailable while the map had a valid preview; DashboardState now derives those preview fields from the same fresh fix, while speed/distance/duration remain subject to the original workout policy. No GPS owner/driver/recording logic was changed.

Runs c027517/43735969185, d836768/43736258541 and 17e332c/43736512644 were superseded and canceled during incomplete full regressions as the candidate changed. They are not full passes. In particular 17e332c's focused step succeeded, but its full run was canceled and its output was cleared by the subsequent full invocation; no missing screenshot/report is counted as acceptance. The final candidate must execute its focused and complete suites again.

## Full tests succeeded; actual visual review rejected the candidate

Run 43737955499 / source dc965a5f47a872dd5066f9c253c56aaa2a76be7f: Python Tests 141 / Executed 141 / Failures 0 / Errors 0 / Skipped 0; JVM 167 / 167 / 0 / 0 / 0; API35 459 / 458 / 0 / 0 / 1. The sole skipped method is exactly TrackRecordingServiceStateMachineTest.recording_stopGPS_noop. Raw XML and the gate/index are retained under evidence/run43737955499.

This is NOT Stage 1 acceptance. Actually viewing the eight Explorer real-map captures exposed the old floating primary-metric card covering the interactive map controls. Several intended real-map captures also showed 5m fixes outside the target instead of the injected 8m fix at 23.7/117.43: the live emulator GPS provider left by other tests competed with the deterministic input. Test success and successful PNG generation did not justify visual acceptance.

Fixes: move Explorer's metric readout below its map with a compact map height that preserves the page budget; add metric/control intersection assertions; replace only the test GPS driver with the existing controlled P0 fixture while retaining the real service/GpsHandler path; require the intended camera position, real feature status and all visible tiles cached before every real-map capture. The real map remains the immutable 105,834,033-byte fixture. No production GPS mock or alternate collector is added. New workflow checkpoint preserves the focused raw XML and 35 screenshots before the full invocation clears those directories, enabling actual review during the long regression. Both focused and full runs must be repeated for the corrected source.

## Single-frame marker defect found during early review

Source 9b3641fd579c5c881c6317e3cfac1e60ead11080 / Run 43741989911 passed the focused two tests and preserved 35 source/hash-verified native captures in artifact 14173396005. Actually inspected all 35. Real streets and place names are stable across all32 real-map captures; Explorer readout no longer covers controls, and the three empty-map states remain neutral with location/status. One Explorer dark / 390dp / font1.4 capture, however, showed only the faint accuracy circle without the opaque position marker. This is a visual rejection, not acceptance.

Mapsforge 0.25.0 LayerManager paints under the Layers monitor. Overlay replacement previously removed/added layers using individually redrawable operations, allowing an intermediate accuracy-only frame. The fix updates the complete overlay set under that same monitor, disables intermediate redraw requests, then requests one redraw. A new check reads the actual native PNG and requires a visible opaque marker around the known current camera center; it tolerates display color conversion rather than matching one exact RGB value. The failed original native frame and all35 per-image review outcomes are committed. Original GPS ownership/preference assertions remain unchanged. The incomplete full run is superseded; corrected source must repeat focused and full validation.

## 阶段1最终验收

修复源码 `e08887d479719699e474e8407d4ac90f98fc128a` / Run 43743383681 完整成功。Python Tests141/Executed141/Failures0/Errors0/Skipped0；JVM167/167/0/0/0；API35 459/458/0/0/1。唯一历史Skip逐项核验，原始XML与摘要已保存至 `evidence/run43743383681-full/`。同源码先行35张原生PNG已全部实际查看通过。完整结论、实际APK哈希、限制及后续门禁见 `02-stage1-acceptance.md`。旧候选的失败/视觉拒绝和取消记录不变，未将其重标为通过。
