# Phase C accepted: SAF route preview and selected transactional import

Accepted product-stage source **a4c6c1803c25d3c18a9e2158554219d3597bf1a1** on `feature/p0-8-0-gpx-route-follow`. This is not the final P08 Release APK source. Predecessor B report ac7782909da086a38aa9d0d3ab6eb8522de2588a was pushed and remotely verified before C began. Subsequent 8319bb8c8f5a9e46078a10b48ea55673b4a71943 contains failure evidence only; it does not replace the tested source identity.

## Exact-source result

[Actions45123727015](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/45123727015) passed the predecessor artifact/historical raw gate, Debug/native test compilation, Lint, complete JVM/Python, focused API35, actual process-termination recovery and final source-bound evidence gate. Immutable artifact **14795525179**, `P08-phase-C-45123727015-1`, **2,302,724 bytes**, SHA-256 **f3e0bd3faa5281fe3dc09801813c21347847b9bad3a2f2fbb25dd814d3e0b75f** was downloaded independently and its raw XML, logs and images inspected.

| Family | Tests | Executed | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|---:|
| Python complete helper suite | 175 | 175 | 175 | 0 | 0 | 0 |
| JVM complete original/new | 265 | 265 | 265 | 0 | 0 | 0 |
| API35 parser8 + repository6 + import10 | 24 | 24 | 24 | 0 | 0 | 0 |

All 265 predecessor JVM identities and 14 predecessor native identities are retained. All ten new native method identities match source. The unchanged eight actual transaction-kill scenarios were rerun and independently checked: eight intentionally interrupted producer processes plus eight distinct successful recovery processes, separate from JUnit totals. No zero-report/NO-SOURCE task is counted as tests passed.

New native cases cover:

1. SAF contract with nonstandard MIME and nonseekable ContentResolver pipe; three candidates, import only selected rte/trk, retain two track segments and no history record.
2. Partially invalid file: show all three candidates, disable invalid coordinates, import only an explicitly selected valid candidate.
3. Exact source duplicate: cancel confirmation leaves the existing UUID; explicit confirmation creates an additional UUID without overwrite.
4. Activity recreation retains the exact parsed preview and selected ordinal without reopening the provider.
5. Picker cancellation creates no routes/error.
6. Cancel a slow streaming read, retain no formal route and successfully choose another file.
7. Malformed, empty, interrupted, missing and waypoint-only documents cannot commit.
8. Missing filename metadata and a simulated unavailable picker are recoverable.
9. Repeated commit calls create one route, never replay the completed selection.
10. Actual native preview screenshots for all four themes in light/dark modes.

The test provider is present only in the test APK through a new flavor-test manifest overlay; the old test manifest and all accepted Java/Python tests remain unchanged. Espresso returns a SAF result for these tests and the real ContentResolver/provider stream path executes. This does not claim a human interaction with the real system DocumentsUI; the final signed profile must additionally exercise the actual picker.

## Native visual review completed

Eight original **1080×2400** native PNGs are preserved with source/SHA in `evidence/phase-C/.../p08-import/screenshots.json` and the checked acceptance receipt. All eight were actually viewed using two contact sheets; original **Neon dark** and **Explorer light** were additionally viewed at full resolution. Checked the visible source filename, all valid/invalid candidates and reasons, rte/trk multi-segment summaries, selected count, both action buttons, navigation/system insets, text wrapping, contrast and theme/night distinction. No blocking clipping, overlap or hidden action was found.

- [Review contact sheet: Explorer/Neon](evidence/phase-C/C-import-contact-1.jpg).
- [Review contact sheet: Precision/Quiet](evidence/phase-C/C-import-contact-2.jpg).

These are review aids assembled from the original screenshots; original native PNGs remain in the evidence tree. The CI manifest deliberately says PENDING_ACTUAL_REVIEW; the independently created acceptance receipt records **PASSED_ACTUAL_NATIVE_REVIEW** after inspection. File existence/hash alone was not treated as visual acceptance. This stage covers mixed-validity English previews at one actual managed viewport; additional states, three requested viewport sizes, large fonts/locales, final library/map/following screenshots and fixed-key Release visuals remain later gates.

## Isolation, retained failures and remaining scope

The unchanged native repository isolation check again retained schema43, all Track/TrackPoint/rawHR row values, all preferences and the private map sentinel before/after route operations. It remains an early in-memory SQLite/sentinel proof, not the final real historical database/map upgrade test. C production imports use no service/GPS/BLE/history/database commands or recording model; only the independent repository is written. P071 root navigation is untouched.

Run45123156474 attempt1 stopped on Maven Central429 for the unchanged Mapsforge0.25.0 dependencies, before Android compilation/tests. Retrying the exact source resolved the network issue; attempt2 then found two missing default arguments in new test setup PreferencesUtils.getString. Correcting those calls to preserve nullable old values changed no production behavior or assertion. Both failed artifacts were independently downloaded/hash-verified; raw logs, original gate/plan and precise failure metadata are retained in `evidence/stage-C-attempt1/` and `stage-C-attempt2/`. Neither failed attempt is reported as native test success.

Implementation/limits and cancellation details are in `05-saf-import-candidate.md`. Current stage C is accepted, with evidence below. After this report/evidence commit is pushed and the remote HEAD verified, proceed to D: Home/Settings secondary entry, persistent list/details, alias rename, confirmed deletion and caller-preserving Back. Mapsforge static overlay, foreground GPS following/progress/deviation, full legacy API35 and service regressions, fixed original JKS/P071 upgrade, immutable final APK/Tag/Pre-release and anonymous public verification remain pending. No P08 Release exists yet. P071 published assets remain unchanged. Physical-device GNSS/BLE/OEM gesture checks remain NEEDS_DEVICE_VALIDATION.
