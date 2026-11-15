"""P0.7+ releases can publish from the dedicated branch without touching stale main."""
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
import release_policy as policy


class P07ReleasePolicyTests(unittest.TestCase):
    def run_plan(self, branch_ref):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        previous = Path.cwd()
        self.addCleanup(os.chdir, previous)
        os.chdir(temporary.name)
        branch = "release/p0-7-to-p1-0"
        subprocess.run(["git", "init", "-b", branch], check=True, capture_output=True)
        subprocess.run(["git", "config", "user.name", "P0.7 release test"], check=True)
        subprocess.run(["git", "config", "user.email", "p07@example.invalid"], check=True)
        Path("version.properties").write_text(
            "VERSION_NAME=v0.1.0-p0.7.0\n"
            "VERSION_CODE=6780\n"
        )
        subprocess.run(["git", "add", "."], check=True)
        subprocess.run(["git", "commit", "-m", "fixture"], check=True, capture_output=True)
        head = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
        env = {
            "GITHUB_RUN_ID": "p07-test",
            "GITHUB_EVENT_NAME": "push",
            "GITHUB_REF": branch_ref,
            "GITHUB_REPOSITORY": "foxmaybeOI1761640545/Runner_Gardenia-06",
            "GITHUB_OUTPUT": "",
            "GITHUB_SHA": head,
        }
        with patch.dict(os.environ, env):
            policy.prepare("plan.json")
        return json.loads(Path("plan.json").read_text())

    def test_dedicated_release_branch_allocates_p07_and_is_publishable(self):
        plan = self.run_plan("refs/heads/release/p0-7-to-p1-0")
        self.assertEqual("v0.1.0-p0.7.0", plan["versionName"])
        self.assertEqual(6780, plan["versionCode"])
        self.assertTrue(plan["publishing"])

    def test_feature_branch_push_does_not_publish(self):
        plan = self.run_plan("refs/heads/feature/p0-7-to-p1-0-offline-navigation")
        self.assertEqual("v0.1.0-p0.7.0", plan["versionName"])
        self.assertFalse(plan["publishing"])


if __name__ == "__main__":
    unittest.main()
