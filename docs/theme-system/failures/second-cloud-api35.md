# Second cloud candidate — preserved API35 failure

Source: `12a52ab4fbe013603a525659b8e795fc55ee916e`; Run: https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42390109280 .

Compile, lint, 113 JVM tests and all 10 new theme tests passed. The complete API35 suite reported 405 tests, 404 executed, 1 failure, 0 errors, 1 existing skip. The only failure was the retained P02HomeUiTest narrow-screen invariant: heart-rate summary offscreen. Full original XML is retained beside this report; the cloud artifact contains all original native captures.

Fix: keep the family label inside the native header, compact Precision's secondary metrics into one ruled row, use a short-window hero dimension with a tall-window resource override, and keep Neon status before its heart panel. Original visibility assertions are retained.

Native screenshots also exposed the existing general chart's empty speed axis in heart-only panels. A dedicated display-only mode now selects the existing HR series and an honest no-data state; the existing series/path/gap algorithms are unchanged. Pending additions cover family switches while recording and native selected-state accessibility semantics. All changes require a new complete gate before signing.
