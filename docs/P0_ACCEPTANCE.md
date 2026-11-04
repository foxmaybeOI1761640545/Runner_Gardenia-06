# P0 acceptance record

## Stage 0 — PASSED

- Repository: foxmaybeOI1761640545/Runner_Gardenia-06.
- Remote main baseline: 9b5d7b06afc1eb8e3315ba73874ac088fa4d5169.
- New branch: feature/p0-core-migration, created directly from that commit with no upstream tracking of main.
- Initial worktree: clean; no user changes moved, overwritten or discarded.
- Existing branches: main and feature/heart-rate-monitoring, recorded with exact original SHAs in P0_BASELINE.json. Neither ref has been modified.
- Checks: git status, fetch/read-only remote branch inventory, baseline ancestry/equality and unique branch creation. Source/application/build files unchanged during stage 0.
- Baseline Android evidence: existing successful main workflow 41848524070 (328 Android cases, 327 executed, 0 failures/errors, 1 original Ignore; 52 JVM cases; 20 helper cases). This is unchanged-baseline evidence, not a claim that new P0 tests have run.
- Local baseline helper regression: 20 tests PASSED before this stage's commit; branch/ref/unchanged-source checks PASSED.
- Failure/fix: none.
- Stage 1 entry: authorized after local branch/ref/unchanged-source checks pass and stage 0 is committed only on the new branch.

## Hardware validation — NEEDS_DEVICE_VALIDATION

No physical GPS handset or H303 is attached to this environment. Real outdoor 1Hz delivery/accuracy/loss recovery, H303 power-off/range/reconnect, Bluetooth cycling, background/screen-off behavior, long-session power use and physical upgrade/install checks must be reported separately. H303 will not be hardcoded as a device name. Emulator/fake transport data is explicitly test data.
