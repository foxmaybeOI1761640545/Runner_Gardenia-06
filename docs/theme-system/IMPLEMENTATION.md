# P0.3 native theme system

Branch: `feature/theme-system-p0-3`; exact source baseline: peeled `test-p0-v0.1.0-p0.2` → `4454d716a5ae010b92d3eefa1ac1c0ebedde0063`. Phase 1's complete cloud/live gate and independently verified source `61b9c889772bb3a7b36760d58cecedbe8dc524f3` are retained in `phase1-final-gate.json`. No UI Lab branch merge or historical archive task is performed.

The shared `AbstractActivity → PreferencesUtils` path now chooses exactly one native family style. `AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM` is the only appearance policy. Only the family ID is persisted; Android `values` / `values-night` resolve all colors. Wallpaper/dynamic and legacy forced-night preferences cannot override the authored palettes. `ThemeUtils` remains the semantic color utility and owns the four-family metadata; no second appearance manager is introduced.

The shared native preference screen provides four selectable families with actual native Drawable diagrams and selected-state announcements. P0 has a lean settings Activity because its accepted Release manifest excludes the legacy full settings Activity. The P0 sensor settings page also links to appearance; Debug's existing full settings screen uses the same preference fragment. Back-stack Activities recreate on resume only when their saved family differs. Framework configuration recreation retains their ViewModels, saved selected history ID and Fragment dialogs.

| Family | Native home structure | Light | Dark |
|---|---|---|---|
| Precision Pro | Full-width telemetry number, ruled metric grid | Cool white / graphite / lime fill | Graphite / lime |
| Quiet Minimal | Centered open readout, generous space, no card borders | White / forest | Deep forest / soft ivory |
| Outdoor Explorer | Native terrain VectorDrawable, metric card before status, field-note labels | Earth / forest | Forest / stone |
| Neon Pulse | Full-width heart readout and actual native chart before distance | Lavender / deep teal | Violet / cyan |

Java, XML, ViewBinding, native Drawables and existing ChartView are used throughout. The four home XML structures retain the same binding IDs and service actions. Light/dark styles cover window/background, Material surfaces, text, outline, buttons, inputs, dialogs, preferences, status messages, chart lines/grid, and system-bar icon contrast. Semantic colors are compiled into native resources; only four families/eight palettes are included in the APK's token metadata.

Chart integration reads actual preview notifications into a bounded ViewModel projection and uses the existing ChartView / HeartRatePlot gap rendering. Historical curves use the existing worker-side HeartRateChartHistory reader; its visibility is widened for native UI reuse. No recording/GPS/BLE/repository/export algorithm is modified. GPX selection uses a Fragment dialog with the existing save/share actions, so system-mode recreation restores the dialog.

## Validation contract

Source checks require the completed Phase 1 gate, exact P0.2 tag/base, eight complete native resource palettes, four distinct XML structures, system-only mode and unchanged service/GPS/BLE/database/GPX algorithms. Existing helper, JVM, instrumentation, offline, real-process durability, GPX schema and UI regressions remain required. Existing screenshot tests now change the actual system mode instead of the removed manual appearance preference; their assertions are retained.

New API35 tests require eight native scheme matrices (56 PNGs across home, active record, heart, history, detail, GPX dialog, settings); actual system-mode changes while GPS acquisition/recording/connection/paused state are active; retained ViewModel identity; restored dialog/detail; preference selection persistence; narrow-screen 1.3× fonts, native touch targets and accessibility text. Synthetic notifications/GPS fixtures are explicit and do not establish RF/GNSS hardware performance.

The fixed-key signed Release must additionally pass the original P0.2 upgrade/retained-data/actual-GPX suite, eight native OS-mode schemes and 32 screenshots of the signed APK itself. The public APK must match the immutable signed artifact SHA, package, version and fixed certificate. Publication is a distinct same-run gate, never an overwrite of P0.2. CI uses Asia/Singapore; production keeps the user's device timezone.

Current implementation and local helper/source checks are ready for cloud compilation; Android results, signature/hash, Release and final receipts will be appended only after actual execution.
