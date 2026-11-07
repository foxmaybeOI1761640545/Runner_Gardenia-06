"""Actual file/CLI regressions for archival integrity; no network or Android."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[2] / "tools/verify_backup_inventory.py"
SPEC = importlib.util.spec_from_file_location("backup", SCRIPT)
backup = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(backup)


class BackupInventoryTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / "evidence.txt").write_bytes(b"original evidence\n")
        self.row = {
            "path": "evidence.txt", "bytes": 18,
            "sha256": hashlib.sha256(b"original evidence\n").hexdigest(),
            "branch": "feature/p0-2-gps-ui", "archived_at_utc": "2026-11-07T00:00:00Z",
            "archive_type": "LOCAL_HELPER_TEST_RECEIPT", "verification": "VERIFIED",
            "source": {"original_name": "original.txt", "original_path": "original.txt",
                       "branch": "feature/p0-2-gps-ui", "origin": "local test receipt",
                       "commit_sha": None, "commit_unavailable_reason": "receipt did not record a commit",
                       "actions_run_id": None, "artifact_id": None},
        }
        self.data = {"schema_version": 1, "repository": "owner/repo",
                     "branch": self.row["branch"], "updated_at_utc": "2026-11-07T00:00:00Z",
                     "files": [self.row]}

    def run_inventory(self):
        (self.root / "inventory.json").write_text(json.dumps(self.data))
        return backup.verify(self.root, "inventory.json")

    def rejected(self, fragment):
        result = self.run_inventory()
        self.assertFalse(result["passed"])
        self.assertIn(fragment, " ".join(result["errors"]))

    def test_original_file_passes(self):
        self.assertTrue(self.run_inventory()["passed"])

    def test_missing_file(self):
        (self.root / "evidence.txt").unlink()
        self.rejected("does not exist")

    def test_size_mismatch(self):
        self.row["bytes"] = 1
        self.rejected("size mismatch")

    def test_sha256_mismatch_with_same_size(self):
        (self.root / "evidence.txt").write_bytes(b"altered! evidence\n")
        self.rejected("SHA-256 mismatch")

    def test_malformed_json(self):
        (self.root / "inventory.json").write_text("{")
        self.assertFalse(backup.verify(self.root, "inventory.json")["passed"])

    def test_duplicate_path(self):
        self.data["files"].append(copy.deepcopy(self.row))
        self.rejected("duplicate file path")

    def test_absolute_path(self):
        self.row["path"] = str(self.root / "evidence.txt")
        self.rejected("relative path")

    def test_parent_traversal(self):
        self.row["path"] = "../evidence.txt"
        self.rejected("relative path")

    def test_nested_parent_traversal(self):
        self.row["path"] = "docs/../../evidence.txt"
        self.rejected("relative path")

    def test_windows_path(self):
        self.row["path"] = "C:\\private\\evidence.txt"
        self.rejected("relative path")

    def test_non_normalized_path(self):
        self.row["path"] = "docs//evidence.txt"
        self.rejected("relative path")

    def test_symlink_escape(self):
        (self.root / "link").symlink_to(self.root.parent, target_is_directory=True)
        self.row["path"] = "link/evidence.txt"
        self.rejected("escapes repository root")

    def test_internal_symlink_rejected(self):
        (self.root / "link.txt").symlink_to(self.root / "evidence.txt")
        self.row["path"] = "link.txt"
        self.rejected("symlink")

    def test_empty_inventory(self):
        self.data["files"] = []
        self.rejected("nonempty list")

    def test_multiple_files_and_all_errors_reported(self):
        a = copy.deepcopy(self.row)
        a["path"] = "second.txt"
        self.data["files"].append(a)
        self.row["sha256"] = "0" * 64
        result = self.run_inventory()
        self.assertEqual(len(result["errors"]), 2)
        self.assertEqual(len(result["files"]), 2)

    def test_multiple_valid_files(self):
        (self.root / "second.txt").write_bytes(b"original evidence\n")
        a = copy.deepcopy(self.row)
        a["path"] = "second.txt"
        self.data["files"].append(a)
        self.assertTrue(self.run_inventory()["passed"])

    def test_missing_source(self):
        del self.row["source"]
        self.rejected("source object")

    def test_unknown_local_commit_requires_reason(self):
        del self.row["source"]["commit_unavailable_reason"]
        self.rejected("commit_unavailable_reason")

    def test_artifact_requires_run(self):
        self.row["source"]["artifact_id"] = 123
        self.rejected("requires its Actions run")

    def test_invalid_schema_version(self):
        self.data["schema_version"] = True
        self.rejected("schema_version")

    def test_invalid_timestamp(self):
        self.row["archived_at_utc"] = "2026-11-07T08:00:00+08:00"
        self.rejected("UTC")

    def test_invalid_hash(self):
        self.row["sha256"] = "not-a-sha"
        self.rejected("64 lowercase")

    def test_boolean_size_rejected(self):
        self.row["bytes"] = True
        self.rejected("integer")

    def test_branch_mismatch(self):
        self.row["branch"] = "main"
        self.rejected("branch differs")

    def test_invalid_entry(self):
        self.data["files"] = ["not an object"]
        self.rejected("entry must be an object")

    def test_duplicate_json_key(self):
        (self.root / "inventory.json").write_text('{"schema_version":1,"schema_version":1}')
        self.assertIn("duplicate JSON key", backup.verify(self.root, "inventory.json")["errors"][0])

    def test_missing_inventory(self):
        self.assertFalse(backup.verify(self.root, "missing.json")["passed"])

    def test_cli_error_returns_nonzero(self):
        self.row["sha256"] = "0" * 64
        self.run_inventory()
        result = subprocess.run([sys.executable, "-B", str(SCRIPT), "--root", str(self.root),
                                 "--inventory", "inventory.json", "--json"], capture_output=True, text=True)
        self.assertEqual(result.returncode, 1)
        self.assertFalse(json.loads(result.stdout)["passed"])

    def test_inventory_path_traversal(self):
        self.assertFalse(backup.verify(self.root, "../inventory.json")["passed"])


if __name__ == "__main__":
    unittest.main()
