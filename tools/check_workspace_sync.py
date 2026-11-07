"""Read-only end-of-task check; never fetch, stage, upload or change Git state."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def git(root, *args):
    result = subprocess.run(["git", "--no-optional-locks", *args], cwd=root,
                            capture_output=True, timeout=30)
    if result.returncode:
        raise ValueError("Git query failed: " + args[0])  # never echo credential-bearing URLs
    return result.stdout.decode()


def candidate_metadata(root, filename):
    p = Path(filename).absolute()
    if re.search(r"(?i)(^\.env|\.(jks|keystore|p12|pfx|db|sqlite|sqlite3)$|secret|token|password)", p.name):
        return {"path": str(p), "state": "SENSITIVE_NOT_READ"}
    if p.is_symlink() or any(parent.is_symlink() for parent in p.parents):
        return {"path": str(p), "state": "MISSING_OR_UNSAFE"}
    p = p.resolve()
    if not p.is_relative_to(root) and not (p.is_relative_to(Path("/tmp")) and len(p.parts) > 2 and p.parts[2].startswith(("p02-", "gardenia-"))):
        return {"path": str(p), "state": "OUT_OF_PROJECT_SCOPE_NOT_READ"}
    if p.is_symlink() or not p.is_file():
        return {"path": str(p), "state": "MISSING_OR_UNSAFE"}
    digest = hashlib.sha256(p.read_bytes()).hexdigest()
    known, excluded = {}, {}
    inventory = root / "docs/reports/backup/BACKUP_INVENTORY.json"
    if inventory.exists():
        for row in json.loads(inventory.read_text())["files"]:
            q = root / row["path"]
            if (q.resolve().is_relative_to(root) and not any(parent.is_symlink() for parent in q.parents)
                    and q.is_file() and not q.is_symlink() and hashlib.sha256(q.read_bytes()).hexdigest() == row["sha256"]):
                known[row["sha256"]] = row["path"]
    decisions = root / "docs/reports/backup/RECOVERED_SOURCES.json"
    if decisions.exists():
        for row in json.loads(decisions.read_text()).get("working_note_decisions", []):
            if row["decision"] == "NOT_ARCHIVED":
                excluded[row["sha256"]] = row["reason"]
    state = "ARCHIVED_IDENTICAL" if digest in known else "RECORDED_EXCLUSION" if digest in excluded else "UNARCHIVED_CANDIDATE"
    return {"path": str(p), "bytes": p.stat().st_size, "sha256": digest, "state": state,
            "archive_path_or_reason": known.get(digest, excluded.get(digest))}


def audit(root=ROOT, candidates=()):
    root = Path(root).resolve()
    errors = []
    try:
        if Path(git(root, "rev-parse", "--show-toplevel").strip()).resolve() != root:
            raise ValueError("root must be the repository top level")
        branch = git(root, "branch", "--show-current").strip()
        if not branch:
            raise ValueError("detached HEAD: identify and verify the intended existing branch manually")
        head = git(root, "rev-parse", "HEAD").strip()
        dirty = git(root, "status", "--porcelain=v1").splitlines()
        if dirty:
            errors.append("Uncommitted/untracked files require review; nothing was changed")
        remote = git(root, "ls-remote", "--heads", "origin", "refs/heads/" + branch).splitlines()
        remote_head = remote[0].split()[0] if len(remote) == 1 else None
        if remote_head != head:
            errors.append("HEAD differs from live remote branch (or branch missing); do not overwrite either history")
        ignored = git(root, "ls-files", "--others", "--ignored", "--exclude-standard", "-z").split("\0")[:-1]
        important = [p for p in ignored if p.startswith(("docs/", "reports/", "ui-lab/docs/", "ui-lab/previews/"))
                     and not any(x in p.split("/") for x in ("__pycache__", ".gradle"))]
        if important:
            errors.append("Ignored project evidence requires an explicit retention decision")
        rows = [candidate_metadata(root, p) for p in candidates]
        if any(x["state"] not in ("ARCHIVED_IDENTICAL", "RECORDED_EXCLUSION") for x in rows):
            errors.append("Explicit local candidates need review; sensitive/out-of-scope files were not read")
        return {"passed": not errors, "branch": branch, "head": head, "remote_head": remote_head,
                "dirty": dirty, "ignored_files": len(ignored), "ignored_evidence": important,
                "candidates": rows, "errors": errors,
                "scope": "Only this checkout and explicitly listed candidates; not a scan of all /tmp/home/workspaces. Run the inventory validator separately."}
    except (ValueError, OSError, subprocess.TimeoutExpired, KeyError) as exc:
        return {"passed": False, "errors": [str(exc)]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--candidate-file", action="append", default=[], help="explicit project file, never a directory")
    args = parser.parse_args()
    result = audit(args.root, args.candidate_file)
    print(json.dumps(result, indent=2))
    return 0 if result["passed"] else 1


if __name__ == "__main__":
    sys.exit(main())
