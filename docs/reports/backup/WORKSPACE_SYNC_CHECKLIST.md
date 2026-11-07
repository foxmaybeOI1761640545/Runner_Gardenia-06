# Codex Workspace closeout: mandatory repeatable procedure

Run before ending each authorized stage/task, from the correct checkout. These commands are read-only. They never commit/push/fetch, inspect credential directories or automatically upload local files.

```sh
git rev-parse --show-toplevel
git branch --show-current
git rev-parse HEAD
git status --short --branch
git status --porcelain=v2
git worktree list
git branch -avv
git ls-files --others --exclude-standard
git ls-files --others --ignored --exclude-standard
git config --show-origin --get-all core.excludesfile
git ls-remote --heads origin
python3 -B tools/verify_backup_inventory.py
python3 -B tools/check_workspace_sync.py
```

`git config --get-all` returns1 if unset; record that as no configured global rule, not an archive failure. Inspect `.git/info/exclude` and the effective default global ignore only if present. Do not print credential-bearing remote URLs or open secret files. Existing tracked files remain tracked even if they later match ignore rules.

The sync tool checks dirty/untracked state, the exact live remote HEAD, ignored evidence paths and optional explicit local candidates. It exits nonzero for unsynced/unreviewed state, unavailable remote, detached HEAD, ignored evidence, sensitive/out-of-scope candidates or missing files. Generated build/Python caches are counted without reading them. It does **not** recursively inspect all `/tmp`, home or other projects and does not replace the separate integrity validator or a human retention review.

For known Runner Gardenia files outside the checkout, pass only explicitly identified candidates:

```sh
python3 -B tools/check_workspace_sync.py --candidate-file /tmp/p02-phase6-local-tests.txt --candidate-file /tmp/p02-active-state.md
```

This implementation's first file matches archived original bytes; the second has a recorded non-authoritative exclusion. Changed candidate bytes require a new decision. A sensitive-looking name is rejected before reading. Never pass keys, `.env`, credential directories, personal databases or unrelated projects. If the prior `/tmp` originals no longer exist after a fresh clone, omit them: Git originals remain verifiable without those temporary paths.

## Review and synchronization record

- Classify A technical docs, B reports, C original evidence, D caches, E sensitive/private, F uncertain. Record potential omissions with path, bytes, SHA-256, Git state, effective ignore rule, branch ownership and decision.
- Compare against existing manifests/provenance and live Git Blobs. Exact duplicates become source aliases, not another copied file. Retain valuable raw evidence with source IDs/classification; retain unknowns/exclusions explicitly. Do not call old working notes current acceptance.
- Run actual relevant tests, inspect logs/results, review scope/diff and stage named files only. Push to an authorized existing branch, never force. Use the docs-only gate activation procedure from [restore instructions](RESTORE_AND_VERIFY.md) for tool/workflow changes.
- Re-query remote HEAD and changed Blobs. Confirm local bytes/size/SHA-256 match; check Actions at that SHA; check required links and screenshot provenance. Run the two tools again on a clean synchronized checkout.
- Record remaining `LOCAL_ONLY`, `ARTIFACT_ONLY`, `NOT_ARCHIVED_WITH_REASON`, `MISSING` and `NEEDS_DEVICE_VALIDATION` separately. Do not claim all evidence permanently backed up just because status is clean.
- Preserve branch/Tag/Release boundaries. Update the catalog/index and final report; keep previous source snapshots and real failures. No unrelated cleanup or branch deletion.

For UI Lab, use its existing branch in a clean checkout (or a detached checkout pushed explicitly to that existing remote branch, without creating a new branch). Verify its adjacent checksums, original result/source JSON and both PNG provenance lists. P0.2's script is intentionally not copied with the design sources; cross-branch records point to immutable commits.
