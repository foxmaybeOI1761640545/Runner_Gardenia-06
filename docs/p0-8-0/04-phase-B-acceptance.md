# P0.8.0 phase B acceptance: independent persistent Route Repository

Accepted candidate source **52aab2418579bd645a0aa2ac86d5cc5e4b3f3839**. This is a data-stage source identity, not a signed P08 APK or final release identity. Branch `feature/p0-8-0-gpx-route-follow`, Draft PR [#7](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/pull/7). Predecessor phase A source b00bd2538a710368b93daaf2f9f96f99fc0b36a7 / report delivery 04fc6f70d53e9d9f0cdcd674096e8de8dbd9efe7 was verified before phase B started. P071 remains independently delivered and unchanged.

## Exact-source Actions and independent inspection

[Actions 45121619436](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/45121619436) passed all steps: baseline/ref/version preservation; independent download/digest and historical-source rerun of phase A raw gate; Android Debug build, Lint, full JVM and Python; focused API35; dedicated real transaction process termination; source-bound final gate.

Immutable artifact **14794331155**, `P08-phase-B-45121619436-1`, **1,105,303 bytes**, SHA-256 **53504f488b88deb097bc1cdfdb75a95627a086338db1ee6fd6a5e33eea2a1dda**. Independently downloaded ZIP bytes/hash, original JUnit and all eight producer/verify logs were checked. The previous 210 JVM and eight native test identities are present unchanged. All 34 Repository and 21 Codec method identities match source. Raw receipts and independent-review JSON are in `evidence/phase-B/`.

| Test family | Tests | Executed | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|---:|
| Python, complete original/new helpers | 175 | 175 | 175 | 0 | 0 | 0 |
| JVM, complete original/new | 265 | 265 | 265 | 0 | 0 | 0 |
| API35 focused parser + Repository | 14 | 14 | 14 | 0 | 0 | 0 |

Eight additional **process-death scenarios** passed. They are separate from JUnit counts: eight producer processes were intentionally interrupted, and eight different processes verified recovery. Each producer log records process termination/code0; each recovery records source/case/p08Verified=true/code−1. No interrupted producer is counted as a successful test. The host did not uninstall, clear data or reseed between the termination and its verification. The actual persistent directory is `files/route-library`.

| Scenario | Checkpoint | Result | Actual producer → recovery PID |
|---|---|---|---|
| Batch import prepared | PREPARED | old route retained; neither selected route visible | 1863 → 2299 |
| Batch import partially promoted | PROMOTED_0 | entire new batch rolled back | 2372 → 2430 |
| Batch import committed | INDEX_COMMITTED | all three routes retained | 2494 → 2541 |
| Journal atomically retired | JOURNAL_RETIRED | committed routes retained; garbage reclaimed | 2590 → 2645 |
| Delete file retired | RETIRED_0 | old file and both index entries restored | 2684 → 2734 |
| Delete committed | INDEX_COMMITTED | deleted file absent; repair cannot resurrect it | 2773 → 2823 |
| Rename prepared | PREPARED | old alias retained | 2862 → 2909 |
| Rename committed | INDEX_COMMITTED | new local alias retained; original bytes unchanged | 2949 → 2997 |

Every case verified complete route objects/segments/metadata, exact expected UUID set, unchanged retained geometry SHA, index/mirror agreement and empty pending/garbage directories. Local JVM checkpoint matrices additionally cover multiple import promotions, pre/post-commit I/O, corrupt journals, both-index recovery order, adopted orphan ownership, partial garbage deletion and the exact 1024-route capacity.

## Data isolation evidence

Native CRUD/read/rename/repair/delete left all columns and rows of actual provider tables identical before/after: **1 Track, 2 TrackPoints, 3 raw heart-rate samples**. Table snapshot SHA-256 values:

- Tracks: `7bbc1c4f4958c7f8da09e76387d0aa2914f941f5639a09b08a2f732a56f248d7`.
- TrackPoints: `8fa3170066c5771c8ae6fadc72bce63aec92d94cb38cc785ee083876b5d3d149`.
- Raw heart rate: `6fae728ab71d692cbf7dea8bdb96e35c8867833d5639c1101c665bc4fc0bfdb4`.

SQLite version is **43**. Complete preference snapshot SHA `fe62f8c7649ec5f3017104a0763638d129bace1c565ec6824d4651d8ccf3a78c` and private map sentinel SHA `59eaf15d469c7d57e52b01bb372797de460764737af2bfb88101cc13a4199559` were unchanged. This early native test uses TestApplication's in-memory SQLite and a sentinel, explicitly not a real map rendering or file-database upgrade claim. Real historical database/map checks and active-workout GPS/BLE/service regression remain required later.

## Failures preserved and scope

Local first attempt's orphan-adoption journal defect and its regression are retained in `evidence/stage-B-local/`. First Android run 45121146783 / source f1692af598f15b1612cdf49fd8812ac40e1c904f failed one of 14 cases because its evidence writer used unsupported Files.writeString. Original failed XML/gate/artifact identity remain in `evidence/stage-B-attempt1/`. The fix changed only test byte I/O (including the verifier's corresponding readString use); all assertions and production repository were retained. The complete phase gate was rerun at the accepted source.

Repository implementation and bounded binary format are described in `03-repository-design-and-candidate.md`, including atomic journal retirement before unlink and visible recovery warnings. No production Activity/layout changed: native visual review is **not applicable to this data stage**. No GPS/BLE/service/database/historical importer/P071 navigation implementation changed. No new key, signed P08 build, Tag or Release exists yet.

Phase B is accepted. Phase C may start only after this report and its evidence are committed/pushed and the remote HEAD is verified. SAF import/UI, route management, Mapsforge overlays/static preview, route following/progress/deviation, complete legacy API/UI/screens, fixed signing/P071 upgrade and independent publication are still pending. Physical GNSS/BLE/OEM device verification remains NEEDS_DEVICE_VALIDATION.
