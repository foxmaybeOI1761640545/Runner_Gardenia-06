# P0.6 development checks — acceptance pending

P0.5.1 remains publicly released and unchanged. P0.6 is PR#3 on its own branch.
No P0.6 signing/publication approval has been issued.

- Initial source53f6787: helpers126 and full JVM161 passed; Debug/Lint/test compilation passed. Release offline audit correctly rejected a missing private map Activity because the Release manifest replaces the main application. The Activity was added explicitly, with the exact old permission/component restrictions retained.
- Source6ce7945 PR run43147899063: Android448 tests/447 executed/1 failure/0 errors/1 inherited Ignore; JVM161/161/0/0/0; helpers126/126/0/0/0. All original regressions and map repository/GPS ownership/persistence tests passed. The map UI test failed when clicking a partially visible Fit track button. Failed reports were retained, not skipped or converted into successes.
- Four actual source6ce map screenshots were downloaded with a SHA-bound review bundle and inspected. They show genuine synthetic vector roads, park and pond in actual airplane mode. They also expose a clipped action row, a double-scaled current marker, a stale GPS-waiting message, and a pan that leaves the small fixture coverage. These are development observations, not final visual approval.
- The map actions are now two equal-width rows; zoom buttons remain visible and have accessible labels. The location marker scales once. The waiting message clears on a fresh location. The touch-drag test remains inside fixture coverage and zoom uses the actual native zoom button. A single-point historical segment gets a visible point marker.
- Sourceb4183ce caught a Java type-inference error in the additional route-reader test (`ActivityScenario.launch(Intent)` inferred Activity). It was corrected to `ActivityScenario<P06MapActivity>`. Source0848f1f subsequently compiled and entered API35 execution; its full outcome is still pending.

The full gate still requires all original P0–P0.5.1 regressions, exact-source raw XML, all16 map screenshots, actual map-file import and management, fresh GPS without starting a workout, live/history segments, an actual separate-process force-stop/reopen in airplane mode, GPX/offline audit and native image inspection. The inherited Ignore is never counted as passed. Pure tests and compilation do not satisfy these gates.

The fixed-key route also requires P0.5.1 and P0.4 upgrade acceptance, original APK-byte reuse, all88 signed native pages plus4 map runtime images, and anonymous public download verification. It cannot run from PRs or an ordinary development commit.
