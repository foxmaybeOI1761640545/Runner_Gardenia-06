# P0.8.0 phase B: independent private route repository candidate

Phase A was accepted at product source b00bd2538a710368b93daaf2f9f96f99fc0b36a7, successful run 45116960695 / immutable artifact 14792470939. Report delivery 04fc6f70d53e9d9f0cdcd674096e8de8dbd9efe7 preceded phase B production changes. P071 remains independently published and immutable. This document describes a candidate, not Android acceptance or release.

## Storage and isolation

The caller supplies `files/route-library` (never cache). Phase B contains no UI, Android production dependency, GPS owner, service command, BLE change, database or historical GPX modification. The repository's JVM data types remain independent of Track/TrackPoint. SQLite schema remains 43. Route UUIDs are stable; local rename changes only the checked index alias, leaving original metadata and immutable route bytes intact. Original GPX bytes are not duplicated: parsed standard metadata plus filename/hash/source ordinal preserve useful provenance without storing many copies of a source file.

Storage layout:

```
route-library/
  index.bin
  index.mirror
  repair.notice                  # persistent, explicitly dismissible recovery notice
  routes/<UUID>.route
  pending/<transaction UUID>/    # complete live recovery journal
    before.index
    after.index
    prepared.bin
    staged/<route UUID>.route
    retired/<route UUID>.route
  garbage/<transaction UUID>/    # atomically retired resolved journal; safe to partially delete
```

Binary envelopes have an eight-byte magic, schema 1, bounded payload length and SHA-256. Strict UTF-8, finite coordinates, count/metadata budgets, identity and summary checks reject malformed files. SHA-256 detects accidental damage; it is not an authentication scheme. Unknown future index/route schema fails closed rather than destructively rebuilding. Index capacity is 1024 routes, index 8 MiB, each route 16 MiB, maximum 64 unfinished transactions; route point/segment/metadata limits match the accepted parser. Limits prevent unbounded decoding. Listing reads checked summaries; detail and verifyAll validate full file hashes/geometry. Explicit repair preserves known alias/integrity expectations and reports unavailable files instead of blessing changed geometry.

## Transaction and recovery order

1. Encode and round-trip-check the entire selection; reject an existing UUID and show source-hash/ordinal duplicate matches unless an extra copy is explicitly requested.
2. Write and sync staged route files and before/after index snapshots. Persist and sync the prepared journal, whose added, retired and adopted files have distinct ownership.
3. Atomically promote new files or retire deleted files. Atomically replace the index: this is the commit point. Verify committed index/file agreement before success.
4. Atomically update the committed mirror. Atomically rename the entire resolved pending journal to garbage **before unlinking any recovery records**. Reclaim garbage, immediately or on the next open.
5. On restart, resolve complete live journals before any index reconstruction. Before commit, remove only newly owned files, restore retired files and the old index. After commit, verify and finish the mirror/cleanup. Adopted preexisting orphan files are never deleted by rollback. A corrupt unresolved journal blocks index rebuilding.

Each durable file is explicitly synced and each namespace transition uses same-filesystem atomic moves. Tests cover process termination and I/O interruption; this does not claim physical flash/power-loss certification. One application process serializes repository mutations with a common lock. No second production process or cross-process concurrent writer is introduced.

A cleanup I/O failure after the commit point reports verified committed routes plus a visible pending-cleanup warning. A new mutation waits for unresolved cleanup. A partially removed **garbage** journal needs no recovery metadata; it cannot turn a committed import into a partial rollback. Missing/corrupt primary index falls back to a checked mirror. Rebuilding both corrupt indexes retains valid immutable route files and clearly warns that aliases may revert to import names.

## Candidate tests and retained failures

The first local run had 73 tests and one failure: orphan adoption lacked an explicit journal ownership record. The fix added a separate adopted-file list and interrupted-adoption regression. The original failure is retained. Subsequent runs passed 74, then 96 tests. A further transaction review identified interruption during journal cleanup; atomic journal retirement and two cleanup regressions bring the current local set to **98 passed** (43 parser, 34 repository, 21 codec), no skipped tests. Pure Java runs use a 192 MiB heap; they are not Android results. Python's full original/new helper suite passes **175 tests**.

The phase-B Actions gate will rerun the historical phase-A gate against independently downloaded immutable raw reports, compile/Lint the Android app, run the complete JVM/Python sets, and execute the unchanged eight native parser cases plus six native repository cases. The latter compare every row/value of Track, TrackPoint and raw heart-rate tables, preferences, a private map sentinel and schema 43 before/after CRUD/repair. TestApplication uses in-memory SQLite here; real file database and actual map upgrade checks remain phase F.

A dedicated test-APK runner will be externally force-stopped on API35 while eight real add/delete/rename transaction calls are on the stack. A new process must recover the same persistent private files without reinstall/pm clear, verify entire expected UUID sets and geometry, ensure index/mirror agreement and empty pending/garbage, and reject deletion resurrection. Interrupted producer processes are explicitly not counted as passed JUnit tests. Build provenance and both actual process IDs are mandatory.

No production Activity or layout changes in phase B: visual review is not applicable. Full legacy API35, UI, route-following, fixed-key Release, P071 upgrade and publication remain required in later phases. Phase C may begin only after exact-source phase-B acceptance, its report commit/push and remote verification.
