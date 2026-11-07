# Restore and verify repository-held archives

This is Git-persisted, attributable, verifiable project archival. It is **not an independent off-site disaster-recovery backup**. A second independent location requires separate authorization. Artifact ZIPs expire; Git evidence and its original provenance must remain usable without Actions access.

## Get the correct branch and snapshot

In a new directory (never overwrite an existing working checkout):

```sh
git clone --single-branch --branch feature/p0-2-gps-ui https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06.git gardenia-backup
cd gardenia-backup
git status --short --branch
git rev-parse HEAD
python3 -B tools/verify_backup_inventory.py
python3 -B -m unittest discover -s .github/tests -p 'test_backup_inventory.py' -v
```

The validator is offline, stdlib-only and needs no Android SDK or credentials. It prints every PASS/FAIL and exits nonzero on missing/corrupt/unsafe/duplicate records or an empty inventory. Use `--json` for a machine-readable result. Local helper receipts with no recorded source SHA explicitly retain `null` plus an explanation; never infer a test commit from an unrelated successful run.

## Recover one original without overwriting another file

From the clone above, this restores the Manifest from the immutable recovery commit into a fresh temporary directory. The archive did not regenerate the original XML.

```sh
python3 -B - <<'PY'
from pathlib import Path
import hashlib, subprocess, tempfile
target = Path(tempfile.mkdtemp(prefix='gardenia-one-')) / 'release-manifest.xml'
data = subprocess.check_output(['git', 'show', '31bdff76d90722cf59142fff95fd7349f3235897:docs/reports/evidence/p0-1-manifests/release-manifest.xml'])
assert hashlib.sha256(data).hexdigest() == 'c3b0fa84a4eb36aafc82dcf23b2b2be58cd84d82f0b5dbeda70d5443d0936d45'
with target.open('xb') as stream:
    stream.write(data)
print(target)
PY
```

For the two files in their normal checkout location:

```sh
(cd docs/reports/evidence/p0-1-manifests && sha256sum -c SHA256SUMS)
```

## Batch recovery into a separate directory

Run in Bash from the verified clone. `set -o pipefail` ensures a failed Git export cannot be hidden by tar. The destination is new and contains only this inventory's evidence and provenance. The verifier runs from the original clone against the separate recovery root.

```sh
set -o pipefail
backup_snapshot=$(git rev-parse HEAD)
backup_restore_dir=$(mktemp -d /tmp/gardenia-restored.XXXXXX)
git archive "$backup_snapshot" docs/reports/backup docs/reports/evidence/p0-1-manifests docs/reports/evidence/p02-local-receipts | tar -x -C "$backup_restore_dir"
python3 -B tools/verify_backup_inventory.py --root "$backup_restore_dir"
```

For the pre-existing historical content-addressed archive, each [Artifact manifest](../evidence/artifacts/) maps `originalPath` to `object`, byte count and SHA-256. Retrieve the named object from the recorded Git commit, verify its hash, then write it to a new destination. Do not change the canonical object or treat a deduplicated object as a newly executed test. The old archive verifier requires reachable historical Git objects and a read-only remote-ref query:

```sh
python3 -B .github/scripts/verify_report_archive.py
```

UI Lab records remain on `design/p0-ui-lab`; use a **separate clone of that existing branch** for restoration. Do not copy its sources, Tokens or screenshots into the P0.2 archive. The branch catalog links fixed snapshots and distinguishes browser simulations from Android/device evidence.

## Add new evidence safely

1. Confirm the branch/workspace is correct and user work is preserved. Inspect ignored and explicitly identified project-local files, not credential directories.
2. Recover original bytes from the original Artifact/Git snapshot. Verify ZIP digest against GitHub metadata before member extraction; reject unsafe ZIP paths. If only a documented local original survives, verify the recorded hash and state that fallback. Never synthesize missing historical evidence.
3. Review sensitivity and independent evidence value. Never retain keys, `.env`, personal databases, APKs, build caches or redundant copies. Compare content hashes, not names. Preserve original filename and member path when a `.log` becomes `.txt`.
4. Add one entry per newly managed file to [BACKUP_INVENTORY.json](BACKUP_INVENTORY.json). Required fields: relative archive path, bytes, lowercase SHA-256, branch, UTC archive time, archive type, verification, original name/path/branch, full source SHA, Actions/Artifact IDs (explicit null where inapplicable), origin and any justified unknown-commit reason. Do not hash the inventory itself.
5. Keep receipts immutable; a new revision gets a new file and attribution. Mutable indexes/final reports stay outside this fixed-payload inventory. Extend the existing historical manifests instead of enumerating their entire contents again.
6. Run the validator, its real tests and relevant old archive gates; review `git diff --check`, explicitly stage paths, commit/push only an authorized branch, and compare the new remote Blob bytes with local SHA-256.
7. Check the actual backup Actions result and update the branch/report index. A clean worktree alone says nothing about unarchived files outside the repository.

## Mismatch, loss and limitations

Stop treating an entry as verified if its bytes fail. Preserve the suspect copy separately, identify the immutable commit and provenance, restore into a new directory, and rerun the checks. Do not edit the expected hash just to make a failure disappear. Record unrecoverable evidence as missing with its last known origin; do not create a substitute. An Artifact URL or a green job alone is not permanent evidence.

The workflow has read-only permissions, no signing Environment/Secrets, and no Android steps. Documentation pushes use the existing archive gate. A tools/workflow change currently needs `[skip ci]` on its push to avoid the unmodified Android push workflow, followed by a normal **documentation-only** commit/push on the same branch (for example the stage verification record). That push loads the newly installed backup workflow while the unchanged Android workflow ignores the docs-only delta. Verify the backup run head SHA and absence of Android runs. A first dispatch of an unregistered non-default-branch workflow returned HTTP 404 during this implementation; do not rely on dispatch registration or change main to work around it. `workflow_dispatch` is optional only where GitHub actually supports the registered workflow. Never use a publication-recovery marker to bypass this distinction.
