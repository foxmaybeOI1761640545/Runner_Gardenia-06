# P0.5.1 CI and GPS review

PR #2 base was confirmed remotely as de225b664a3f1b16e455068f1d426204f638745d; initial HEAD 18320e1cf5143eee947a3971267a59a36ca1ca5f has three existing commits and no uncommitted work. All are preserved. main and P0.5 are untouched remotely.

Independent `.github/p051` configuration uses full stage-5 regressions. `check_p051_source.py` compares the current PR base/head (push resolves the remote PR target); only reviewed GPS integration, tests and named pipeline files may differ. GPX exporters, BLE, database schema, dependencies, resources and old phase policies remain frozen. P0.5 scripts/workflow are restored; its version reader now accepts later valid ledger reservations, as the existing P0.3/P0.4 readers do. Missing XML and incomplete screenshots remain failures. PR validation cannot sign or publish.

The first runnable CI, 43047881992 at e5c0b9f, passed helper/scope/JDK/SDK setup and built Debug plus instrumented sources. Its 135 JVM tests exposed the existing slow-start fixture failure (4 nominal meters using 111320m/degree is only ~3.996m on the filter's Earth model). The fixture is now 4.1m/5.1m without weakening either state assertion. This failed run is retained. Two earlier workflow syntax failures are also preserved.

## Motion corrections and acceptance

- Stop threshold 0.4m/s, slow-start threshold 0.45m/s plus accuracy-bounded anchor displacement (4–10m), high-speed start 0.9m/s. The true 0.5m/s and 0.7m/s paths remain above the stopping threshold. Display noise floor 0.3m/s preserves their actual pace.
- Continuous 0–0.3m/s noise is pinned to an anchor; timestamps/accuracy are preserved by cloning the input Location. GPS sampling and workout timing remain unchanged.
- Relocation needs three mutually spatially plausible fixes; unrelated jumps and reported speeds above 20m/s cannot establish a segment. Rejected jumps still advance the monotonic timestamp watermark.
- Reconnect retains a pending discontinuity across filter resets. Automatic boundaries are consumed only on a stored GPS point, not a heart-rate-only update.
- Review found that the existing statistics updater counted the coordinate bridge even on automatic segment starts. The narrow fix excludes that one edge, without resetting duration; the recorder stores an automatic boundary regardless of the minimum-distance preference.
- GPX/BLE/schema implementations are unchanged. New integration tests run GPS Handler → SensorManager → AggregatorGPS → TrackPointCreator → recording service → SQLite → GPX. They check 255 stationary points, zero distance, all HR samples, exact times/accuracy, slow/running/cycling distances, reconnect, BLE interleaving, relocation and long gaps.

Local pure-Java tests: 52 executed, 0 failures/errors/skips using JUnit 4.13.2 and Java 21 runtime with ECJ Java-17 compilation; no Android SDK installed. Full Android/JVM acceptance is still pending Actions. Local helper tests: 111 executed, 0 failures/errors/skips. No signing or publication is yet accepted.

Only synthetic coordinates/data enter source and test artifacts. The uploaded 18-GPX archive remains outside the repository. Physical GNSS, BLE hardware and power usage still require phone validation after emulator acceptance.
