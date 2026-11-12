# P0.5.1 GPS motion quality — independent acceptance review

## Scope and history

PR #2 remains based on `feature/p0-5-dashboard-settings-history` at `de225b664a3f1b16e455068f1d426204f638745d`. The original three commits through `18320e1cf5143eee947a3971267a59a36ca1ca5f` are preserved. No branch was rebased or force-pushed, and main, historical Releases, tags, assets and signing credentials were not modified.

The independent `.github/p051` workflow compares the actual PR base/head, permits only named GPS integration/test/phase infrastructure changes, and freezes unrelated business modules. The original P0.5 workflow, stage and protected refs remain unchanged; its version reader accepts a later valid ledger reservation. The dedicated workflow routes both push and PR events to actual Android tests; PR events cannot sign or publish. Missing XML, zero execution, extra skips, absent matrix images and invalid GPX/offline/process receipts fail closed.

## Recording behavior

- Continuous 1Hz recording and workout timing remain enabled during stationary intervals. Coordinates are pinned, speed is zero and pace is an em dash. The location copy retains the actual timestamp and accuracy.
- Stop threshold 0.4m/s is below true 0.5/0.7m/s motion. Slow starts require speed >=0.45m/s and accuracy-bounded 4–10m anchor displacement; normal starts use 0.9m/s. Start/stop hysteresis is 2/4 fixes. The UI noise floor is 0.3m/s in both unit systems.
- Stop-confirmation coordinates also remain anchored. Resumed motion recovers the short displacement from the anchor; sustained slow movement does not repeatedly enter stationary state.
- An isolated implausible point is rejected. Three mutually coherent relocation fixes or a gap >15 seconds establish a new segment. Repeated unrelated outliers cannot become plausible just because the last accepted fix grows older.
- Reconnect preserves a pending discontinuity across filter reset. Only a stored GPS point consumes the automatic-boundary marker; BLE-only updates retain their heart-rate data and leave the marker pending.
- An automatic boundary carries zero cross-gap sensor distance when no actual sensor distance exists. The recorder persists that boundary even below its distance preference. The shared statistics updater, GPX exporter, database schema and BLE collection implementation remain byte-identical to the PR base.
- Initial readiness and recording reject invalid live monotonic metadata. Legacy untimed import/test Locations are not given fabricated callback timestamps. Valid subsequent fixes explicitly clear a stale weak-GPS warning.

## Evidence interpretation

The full Android run contains one inherited `@Ignore` case, `recording_stopGPS_noop`, which is explicitly reported as skipped and never counted as passed. The matrix tests use synthetic data; emulator/fake input does not establish physical GNSS reception, a physical BLE strap, battery consumption, TalkBack speech or a real WeChat receiving application.

The real recording integration chain covers GPS Handler → SensorManager → AggregatorGPS → TrackPointCreator → TrackRecordingService → SQLite → GPX export. Synthetic fixtures establish 255 stationary points at one coordinate, all 255 raw heart-rate samples, zero recorded stationary speed/distance, preserved timestamps/accuracy, continuous slow/running/cycling distances, reconnect/BLE interleaving, relocation, gaps and pause/resume. GPX segment boundaries prevent erroneous cross-gap lines.

UI captures include both actual SPEED and PACE TextViews for stationary, slow-moving and stopped-again states in metric and imperial units. Expected observed values: stationary 0.0 and em dash; 0.5m/s motion 1.8km/h and 33:20/km, or 1.1mph and 53:39min/mi. Genuine slow motion remains visible; the fix does not disguise it with a zero pace.

## Failed evidence retained

- The original P0.5 scope checker compared an obsolete base and prevented Android setup. It was replaced for this phase by a separate strict checker; P0.5 protection was restored.
- The first actual full run exposed legacy untimed-location compatibility, a shared-statistics change and synthetic distance/start fixtures. Fixes preserve original regression assertions and restore shared statistics to base.
- A later GPS native test exposed a stale weak-signal warning. The production status fix is covered by actual recording/UI input.
- API35 subsequently exposed a test-only `Files.writeString` call compiled against API37. Replacing the evidence writer with FileOutputStream retained every UI assertion.
- Source `84cd3b7a6968f87d72fd0e4fc703612fafc5bb3c` passed push `43062964605` and PR `43062976089`: JVM154/154/0/0/0; Android435/434/0/0/1; helpers118/118/0/0/0. These are prior-source evidence, not a substitute for the final source's run.
- The later transport-only source excludes APK/AAB/keystore material from Git blob review transport; original immutable Actions Artifacts remain preserved. Its first PR attempt exposed an ADB-root connection closure and a platform null bonded-device set. Failed logs and artifacts remain retained; reruns execute complete affected jobs without weakening assertions.

## Final exact-source automated evidence

Source: `a4b2c7a7521a19a60c1d444449ff82c0560e4d19`; main tree `128e1c85c7d88e05bb1d3ff6db723d3d6e7a6e00`.

Full push run: https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43066910884 (success).

| Suite | Tests | Executed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Python pipeline helpers |120|120|0|0|0|
| Full JVM unit tests |154|154|0|0|0|
| Full API35 instrumentation |435|434|0|0|1|

Debug assembly, lint, instrumentation compilation, actual API35 execution, GPX 1.1 schema, merged/packaged offline audit, all original phase matrices, cross-UID shares and three independent new-process durability checks passed. The original skipped method is listed above. A successful compile is not counted as test execution.

Original reports Artifact: `13878632370`, SHA-256 `2e62ebb3bb066e4bfbc78d6331e49a7a76a7c9a8b2cc4524e249d89366680ad0`.
Original native visual Artifact: `13878622397`, SHA-256 `a033d8bc0f2ccf41ff57b6526af9e603b689aa1d41b4ac3369cff1bf630598df`.

The original Artifacts remain immutable. The checksum-bound Git blob transport updates no Git ref and excludes APK/APKS/AAB/idsig plus signing key material. Its index is `ce771494bb20b02efa943682b67c970f0b1300eb`. Independent raw XML inspection matches every reported count, with no missing required class/test. All 54 contact sheets / 420 actual native PNGs were inspected with view_image; the metric/imperial GPS12, original states192, large-font layouts48, metrics8, settings16, history32, P0 (20), P0.2 (36) and P0.3 (56) matrices were all reviewed. The long explorer duration was checked at full native resolution. No new blocking visual problem was observed. PR run43066926326 attempt2 also succeeded after complete affected-job reruns; the initial failures remain retained. Fixed-key signing, signed install/upgrade and publication remain separate gates.
