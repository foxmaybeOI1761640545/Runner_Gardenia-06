# P0.5.1 fixed-key signed APK acceptance

Actual APK/source: `a4b2c7a7521a19a60c1d444449ff82c0560e4d19`. Signing activation/workflow: `4ffeb2a0b3971e782b0461ccfc759ad9ea8d9844` (approval/docs only). [Signed build43140735933](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43140735933) succeeded, including API35 install, GPS/recovery, both historical upgrades and all80 native page checks. Full tests remain bound to accepted run43066910884 and that exact source.

- Identity: `com.fox.runner`, `v0.1.0-p0.5.1`, Code6777; optimized aligned non-debuggable Release.
- APK: 2,856,108 bytes; SHA-256 `a99a489ffdc63c746346d9a6521884b4fc21a6698552f10c4963711b2232816d`.
- Original fixed certificate: `E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22`.
- Immutable formal Artifact: `13909864105`, `RunnerGardenia-P051-Release-43140735933`, SHA-256 `fb01b0de04ac06496a944af5f8fe1928514dc2288e98d25cc1c4c9147bccd890`.
- Candidate was frozen before emulator acceptance; formal output and subsequent publication reuse its exact APK bytes. No signing key was generated or changed.

## Actual upgrade and runtime evidence

Both P0.4 Code6775 and primary P0.5 Code6776 upgrade receipts passed for the same new APK hash. Each retained the existing synthetic workout, one original GPS point, two raw heart samples, selected device, theme, unit/rate preferences and unchanged database schema. GPS preparation created no workout. Actual Release recording, rate switching, manual pause/resume, forced-process recovery and GPX schema/export checks passed. A later batch-share/cancelled-delete matrix preserved the original database signature.

The P0.5 runtime receipt contains38 actual newly recorded GPS points and22.4835524559021m on the controlled synthetic route; the P0.4 receipt contains34 points and the same controlled-route distance. These smoke timings/sample counts are actual emulator observations, not an estimate of real-world GNSS accuracy. The full Debug recording-chain tests separately establish the precise 60-second stationary/slow/run/cycle assertions.

## Independent signed native review

Codex inspected all10 contact sheets /80 actual APK screenshots using view_image. All8 theme/appearance schemes cover appearance, settings hub, motion preferences, actual version information, home, heart, history, selection, sharing and delete confirmation. All80 native XML/PNG hashes and focused-page contracts were independently checked; all8 actual information pages contain `v0.1.0-p0.5.1` and `6777`, with application label `Runner Gardenia P0`. No new blocking visual problem was observed.

Every non-APK formal payload was independently checked against ARTIFACT-MANIFEST. The review transport deliberately excludes APK/key files; the immutable original formal Artifact retains the verified APK. Publication is still a separate gate: it must transfer these original bytes, preserve historical assets, create the unique annotated test tag, and verify anonymous public download and fixed certificate before completion can be declared.

Real GNSS/BLE hardware, battery usage, TalkBack speech and a real WeChat receiving application remain NEEDS_DEVICE_VALIDATION.
