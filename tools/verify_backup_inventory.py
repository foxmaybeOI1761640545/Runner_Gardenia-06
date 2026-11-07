"""Validate a versioned evidence inventory offline using only Python's stdlib."""
import argparse
import datetime as dt
import hashlib
import json
from pathlib import Path, PureWindowsPath
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_INVENTORY = "docs/reports/backup/BACKUP_INVENTORY.json"
TYPES = {
    "ANDROID_MERGED_MANIFEST", "LOCAL_HELPER_TEST_RECEIPT",
    "LOCAL_REMOTE_VERIFICATION_RECEIPT", "PROVENANCE_RECORD",
    "FORMAL_ACCEPTANCE_REPORT", "ANDROID_TEST_EVIDENCE", "JVM_TEST_EVIDENCE",
    "EMULATOR_SCREENSHOT", "BROWSER_LAB_EVIDENCE", "HISTORICAL_WORKING_NOTE",
    "INTERMEDIATE_RECORD",
}


def relative_path(value):
    if (not isinstance(value, str) or not value or "\\" in value or ":" in value
            or "\0" in value or PureWindowsPath(value).drive
            or any(p in ("", ".", "..") for p in value.split("/"))):
        raise ValueError("expected a normalized repository-relative path")
    return value


def safe_file(root, value):
    relative_path(value)
    p = root / value
    if not p.resolve().is_relative_to(root.resolve()):
        raise ValueError("path escapes repository root")
    cursor = root
    for component in value.split("/"):
        cursor = cursor / component
        if cursor.is_symlink():
            raise ValueError("symlink paths are not allowed in immutable evidence")
    return p


def text(value, field):
    if not isinstance(value, str) or not value.strip():
        raise ValueError(field + " must be a nonempty string")


def utc(value):
    if not isinstance(value, str) or not value.endswith("Z"):
        raise ValueError("timestamp must use UTC with a Z suffix")
    dt.datetime.fromisoformat(value[:-1] + "+00:00")


def unique_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("duplicate JSON key: " + key)
        result[key] = value
    return result


def metadata(row, branch):
    if not isinstance(row, dict):
        raise ValueError("file entry must be an object")
    relative_path(row.get("path"))
    if type(row.get("bytes")) is not int or row["bytes"] < 0:
        raise ValueError("bytes must be a nonnegative integer")
    if not isinstance(row.get("sha256"), str) or not re.fullmatch(r"[0-9a-f]{64}", row["sha256"]):
        raise ValueError("sha256 must have 64 lowercase hex characters")
    if row.get("branch") != branch:
        raise ValueError("entry branch differs from inventory branch")
    utc(row.get("archived_at_utc"))
    if row.get("archive_type") not in TYPES or row.get("verification") != "VERIFIED":
        raise ValueError("unknown archive type or unverified source")
    source = row.get("source")
    if not isinstance(source, dict):
        raise ValueError("missing source object")
    for field in ("original_name", "branch", "origin"):
        text(source.get(field), "source." + field)
    relative_path(source.get("original_path"))
    if source["original_name"] != source["original_path"].split("/")[-1]:
        raise ValueError("original_name differs from original_path basename")
    commit = source.get("commit_sha")
    if commit is None:
        if row["archive_type"] not in {"LOCAL_HELPER_TEST_RECEIPT", "HISTORICAL_WORKING_NOTE", "INTERMEDIATE_RECORD"}:
            raise ValueError("this evidence type requires a source commit")
        text(source.get("commit_unavailable_reason"), "source.commit_unavailable_reason")
    elif not isinstance(commit, str) or not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError("source commit must be a full Git SHA")
    for field in ("actions_run_id", "artifact_id"):
        if field not in source or (source[field] is not None and (type(source[field]) is not int or source[field] <= 0)):
            raise ValueError("source." + field + " must be null or a positive integer")
    if source["artifact_id"] is not None and source["actions_run_id"] is None:
        raise ValueError("an Artifact requires its Actions run")
    if row["archive_type"] == "ANDROID_MERGED_MANIFEST" and (source["artifact_id"] is None or commit is None):
        raise ValueError("recovered Manifest requires original Artifact provenance")


def verify(root=ROOT, inventory=DEFAULT_INVENTORY):
    """Return per-file results and errors; never change files, Git or network state."""
    root = Path(root).resolve()
    results, errors = [], []
    try:
        manifest = safe_file(root, inventory)
        data = json.loads(manifest.read_text(encoding="utf-8"), object_pairs_hook=unique_keys)
        if not isinstance(data, dict) or type(data.get("schema_version")) is not int or data["schema_version"] != 1:
            raise ValueError("unsupported or missing schema_version")
        text(data.get("repository"), "repository")
        if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", data["repository"]):
            raise ValueError("repository must be owner/name")
        text(data.get("branch"), "branch")
        utc(data.get("updated_at_utc"))
        if not isinstance(data.get("files"), list) or not data["files"]:
            raise ValueError("files must be a nonempty list; empty inventories fail closed")
    except (ValueError, OSError, TypeError, KeyError) as exc:
        return {"passed": False, "files": [], "errors": ["inventory: " + str(exc)]}
    seen = set()
    for i, row in enumerate(data["files"]):
        label = row.get("path", "entry " + str(i)) if isinstance(row, dict) else "entry " + str(i)
        try:
            metadata(row, data["branch"])
            if row["path"] in seen:
                raise ValueError("duplicate file path")
            seen.add(row["path"])
            p = safe_file(root, row["path"])
            if p == manifest:
                raise ValueError("inventory cannot hash itself")
            if not p.is_file():
                raise ValueError("file does not exist or is not a regular file")
            if p.stat().st_size != row["bytes"]:
                raise ValueError("file size mismatch")
            digest = hashlib.sha256()
            with p.open("rb") as stream:
                for block in iter(lambda: stream.read(1024 * 1024), b""):
                    digest.update(block)
            if digest.hexdigest() != row["sha256"]:
                raise ValueError("SHA-256 mismatch")
            results.append({"path": label, "passed": True})
        except (ValueError, OSError, TypeError, KeyError) as exc:
            errors.append(str(label) + ": " + str(exc))
            results.append({"path": label, "passed": False, "error": str(exc)})
    return {"passed": not errors, "files": results, "errors": errors}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT, help="checkout or isolated restoration directory")
    parser.add_argument("--inventory", default=DEFAULT_INVENTORY, help="relative to --root")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args(argv)
    result = verify(args.root, args.inventory)
    if args.json:
        print(json.dumps(result, indent=2))
    else:
        for row in result["files"]:
            print(("PASS " if row["passed"] else "FAIL ") + row["path"])
        for error in result["errors"]:
            print("ERROR " + error, file=sys.stderr)
        print(f"{'PASS' if result['passed'] else 'FAIL'}: {len(result['files'])} entries, {len(result['errors'])} errors")
    return 0 if result["passed"] else 1


if __name__ == "__main__":
    sys.exit(main())
