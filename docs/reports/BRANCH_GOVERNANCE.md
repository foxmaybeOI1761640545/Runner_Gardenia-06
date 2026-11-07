# Branch, evidence and backup governance

User authorization and the branch-specific task take precedence over this policy. This document grants no automatic permission to create/delete branches, merge, publish, modify main or external storage. This implementation changes only existing P0.2 and UI Lab branches; no `project-records` branch is needed.

## Before a new authorized branch

Record its name/type (`main`, Android `feature/*`, independent `design/*`), source branch and full source SHA, objective, expected deliverables, owned report directory and acceptance criteria. Also record whether it will merge, whether it remains a long-lived historical branch, and who can authorize deletion. Git does not store exact branch creation time: cite a first unique commit and call the date an inference when necessary. Check the live remote, preserve user work, and use a unique name only when branch creation is authorized.

## During development

- Work on the correct branch; switch only with a clean workspace. Never merge/copy other branch product code just to obtain reports. Retain historical report paths and use fixed-commit cross-branch links.
- Root `.gitignore` owns common Android/security/cache rules; module ignores own module-only outputs. Differences can be necessary (`!/src/release/`). Before changing rules, check actual and proposed paths with `git check-ignore -v --no-index`; never exclude all docs/reports/evidence/screenshots.
- Keep build caches, APKs, keys/JKS/keystores, Tokens, `.env`, personal databases/activities out of Git. Do not read credential directories while collecting evidence. Use explicit staging paths, never a blanket add.
- Record actual source/build/report SHAs, Actions run and Artifact IDs, original member paths, byte sizes and SHA-256. An Artifact is temporary transport, not the sole permanent location for important evidence. Verify archive digests before extraction and reject unsafe paths.
- Distinguish formal acceptance, Android/JVM tests, local helper receipts, emulator screenshots, browser simulations, historical notes and intermediate receipts. Unknown source commit/time stays explicitly unknown with a reason. Never infer physical BLE/GNSS behavior from a browser/emulator.

## Every stage ends with a gate

1. Inspect actual test reports/results and artifacts; zero execution, missing evidence and parse failures fail closed. Preserve reasonable skips and failed/cancelled runs; do not rewrite them as success.
2. Run the [inventory verifier](../../tools/verify_backup_inventory.py), its tests and relevant historical archive checks. Hash raw evidence; use content comparison for deduplication, not filenames.
3. Execute the [workspace closeout procedure](backup/WORKSPACE_SYNC_CHECKLIST.md), including ignored and explicitly identified outside-checkout project files. Decide retention/exclusion visibly.
4. Review diff and scope; commit/push only authorized refs. Re-read the exact remote Blob bytes and compare SHA-256. Wait for actual related Actions results before advancing. A successful local command or push is not remote acceptance.
5. Update stage/report links and the branch catalog when responsibilities or deliveries change. Retain failure and recovery records.

## End or archive a branch

Update its delivery/status, source/build/report commits, reports, necessary raw evidence, source/checksum inventories and restore instructions. Record merged/not merged/long-term retention explicitly. A completed branch can remain without merging main. Branch deletion requires explicit authorization. Never move a historical Tag or silently replace accepted assets for a documentation update.

Use [BRANCH_CATALOG](BRANCH_CATALOG.md) as a snapshot directory; its own future commit cannot be embedded in itself. Identify source snapshots and later report commits separately. Query the live remote before subsequent work. Preserve original paths/versions across branches rather than synchronizing all documents to a single version.

## Current operational boundary

Backup CI is read-only, stdlib-only and has no signing Secrets/Environment or Android steps. Existing Android/Pages workflows remain unchanged. A tool/workflow push uses `[skip ci]` then a normal docs-only follow-up push to run backup/archive checks without triggering Android. The initial unregistered workflow dispatch404 is retained in the stage report; do not add a PAT or modify main to obtain a manual trigger.

The backup inventory is incremental: 11 recovered originals and six immutable provenance files, alongside the old66-Artifact manifests. UI Lab keeps its JSON/PNG provenance in its own branch; indexes link it. No large copied central archive. Git-held archival alone is not an independent off-site disaster-recovery backup; any second location needs a separately approved plan.
