# API 35 UI synchronization failure — preserved

Source: `d825480cd3d8a78491c76022ef1415584320ba8f`  
Run: https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42435827563  
Artifact: `P03-reports-42435827563` (13599995928).

405 tests, 2 failures, 0 errors, 1 existing skip. All ten new theme tests passed; short-screen home visibility passed. Signing and publication were correctly skipped.

The recreation/background recording test's Espresso pause tap invoked the finish listener during a platform window transition (logcat `09:57:17.655` through `09:57:18.145`, including `P0RecordingViewModel.lambda$finish$6`). Disable/restore only test window/transition/animator scales in this class, as the screenshot tests already do. The pause/resume/finish assertions and physical Espresso taps remain.

The scan test selected the unfocused base window while its real dialog owned focus. Wait for the laid-out dialog window, and explicitly use `RootMatchers.isDialog()` for both rescan and device selection, following the existing upstream screenshot helper. Keep the selected-device and no-unrequested-connection assertions.

All screenshot regressions now restore the real system night preference in cleanup, matching their switch from legacy app overrides to FOLLOW_SYSTEM. No recording, GPS, BLE or storage production code changed for these fixes. Full suite must pass before signing.
