import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[2] / "tools/check_workspace_sync.py"
SPEC = importlib.util.spec_from_file_location("sync_check", SCRIPT)
sync = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(sync)


class WorkspaceSyncTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="gardenia-sync-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / "repo"
        self.root.mkdir()
        self.git("init", "-q")
        self.git("config", "user.name", "Fixture")
        self.git("config", "user.email", "fixture@example.invalid")
        (self.root / ".gitignore").write_text("build/\ndocs/*.log\n")
        (self.root / "evidence.txt").write_text("original")
        self.git("add", "--", ".gitignore", "evidence.txt")
        self.git("commit", "-qm", "fixture original")
        remote = Path(self.temp.name) / "remote.git"
        self.git("clone", "--bare", "-q", ".", str(remote))
        self.git("remote", "add", "origin", str(remote))

    def git(self, *args):
        return subprocess.check_output(["git", *args], cwd=self.root, stderr=subprocess.DEVNULL)

    def test_clean_synced_checkout(self):
        self.assertTrue(sync.audit(self.root)["passed"])

    def test_dirty_and_untracked_are_not_silently_synchronized(self):
        (self.root / "new.md").write_text("unsaved report")
        self.assertFalse(sync.audit(self.root)["passed"])

    def test_unpushed_commit(self):
        self.git("commit", "--allow-empty", "-qm", "local only")
        self.assertIn("HEAD differs", " ".join(sync.audit(self.root)["errors"]))

    def test_ignored_evidence(self):
        (self.root / "docs").mkdir()
        (self.root / "docs/result.log").write_text("local receipt")
        result = sync.audit(self.root)
        self.assertFalse(result["passed"])
        self.assertEqual(result["ignored_evidence"], ["docs/result.log"])

    def test_generated_build_cache_is_not_evidence(self):
        (self.root / "build").mkdir()
        (self.root / "build/cache.bin").write_bytes(b"cache")
        self.assertTrue(sync.audit(self.root)["passed"])

    def test_explicit_unarchived_project_candidate(self):
        p = Path(self.temp.name) / "new-report.md"
        p.write_text("local-only receipt")
        result = sync.audit(self.root, [p])
        self.assertFalse(result["passed"])
        self.assertEqual(result["candidates"][0]["state"], "UNARCHIVED_CANDIDATE")

    def test_sensitive_candidate_is_never_read(self):
        p = Path(self.temp.name) / ".env"
        result = sync.audit(self.root, [p])
        self.assertEqual(result["candidates"][0]["state"], "SENSITIVE_NOT_READ")

    def test_candidate_symlink_not_read(self):
        p = Path(self.temp.name) / "link.md"
        p.symlink_to(self.root / "evidence.txt")
        result = sync.audit(self.root, [p])
        self.assertEqual(result["candidates"][0]["state"], "MISSING_OR_UNSAFE")

    def test_exact_archived_candidate(self):
        p = Path(self.temp.name) / "receipt.txt"
        p.write_text("original")
        folder = self.root / "docs/reports/backup"
        folder.mkdir(parents=True)
        (folder / "BACKUP_INVENTORY.json").write_text(json.dumps({"files": [{"path": "evidence.txt", "sha256": hashlib.sha256(b"original").hexdigest()}]}))
        # The new metadata is intentionally uncommitted: candidate classification
        # must be correct while the overall checkout still fails its dirty gate.
        result = sync.audit(self.root, [p])
        self.assertEqual(result["candidates"][0]["state"], "ARCHIVED_IDENTICAL")
        self.assertFalse(result["passed"])

    def test_remote_query_failure_is_not_success(self):
        self.git("remote", "remove", "origin")
        self.assertFalse(sync.audit(self.root)["passed"])

    def test_detached_head_requires_explicit_manual_check(self):
        self.git("checkout", "--detach", "-q")
        self.assertIn("detached HEAD", " ".join(sync.audit(self.root)["errors"]))


if __name__ == "__main__":
    unittest.main()
