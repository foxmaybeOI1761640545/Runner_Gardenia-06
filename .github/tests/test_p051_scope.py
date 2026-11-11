"""Fail-closed patch scope and routing regressions, independent of historical P0.5."""
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import check_p051_source as scope
import p051_pipeline as pipeline
from p051_version import allocated_code


class P051ScopeTests(unittest.TestCase):
    def test_gps_integration_is_allowed_but_unrelated_business_is_frozen(self):
        scope.verify_changes([('M', path) for path in scope.ALLOWED_PRODUCT | scope.ALLOWED_TESTS])
        for name in ('p0/export/P0GpxFiles.java', 'p0/ble/HeartRate.java',
                     'data/models/Track.java', 'services/TrackRecordingService.java',
                     'sensors/SensorManager.java'):
            with self.subTest(name=name), self.assertRaises(ValueError):
                scope.verify_changes([('M', scope.JAVA + name)])

    def test_deletions_and_old_phase_policy_changes_fail_closed(self):
        for status, path in [('D', next(iter(scope.ALLOWED_PRODUCT))),
                             ('M', '.github/p05/stage.json'),
                             ('M', '.github/scripts/check_p05_source.py'),
                             ('M', 'build.gradle'), ('M', 'src/main/AndroidManifest.xml')]:
            with self.subTest(path=path), self.assertRaises(ValueError):
                scope.verify_changes([(status, path)])
        with self.assertRaises(ValueError): scope.verify_changes([])

    def test_push_uses_current_remote_base_not_old_p04(self):
        with patch.object(scope, 'git', return_value='latest-base') as git:
            self.assertEqual('latest-base', scope.resolve_base({'GITHUB_EVENT_NAME': 'push'}))
            git.assert_called_once_with('rev-parse', scope.BASE_REF)

    def test_pr_uses_payload_base_and_checks_source(self):
        with tempfile.TemporaryDirectory() as temporary:
            event = Path(temporary) / 'event.json'
            event.write_text(json.dumps({'pull_request': {
                'base': {'ref': 'feature/p0-5-dashboard-settings-history', 'sha': 'actual-pr-base'},
                'head': {'ref': pipeline.BRANCH, 'sha': 'actual-head'}}}))
            env = dict(GITHUB_EVENT_NAME='pull_request', GITHUB_EVENT_PATH=str(event), GITHUB_SHA='actual-head')
            self.assertEqual('actual-pr-base', scope.resolve_base(env))
            with self.assertRaises(ValueError): scope.resolve_base(dict(env, GITHUB_SHA='different'))

    def test_only_expected_branch_push_or_pr_can_validate(self):
        common = dict(GITHUB_REPOSITORY=pipeline.REPO, GITHUB_EVENT_NAME='push',
                      GITHUB_REF='refs/heads/' + pipeline.BRANCH)
        pipeline.trusted(common)
        pipeline.trusted(dict(common, GITHUB_EVENT_NAME='pull_request',
                              GITHUB_HEAD_REF=pipeline.BRANCH,
                              GITHUB_BASE_REF='feature/p0-5-dashboard-settings-history'))
        for changed in (dict(GITHUB_REF='refs/heads/main'), dict(GITHUB_EVENT_NAME='workflow_dispatch'),
                        dict(GITHUB_REPOSITORY='other/fork')):
            with self.assertRaises(ValueError): pipeline.trusted(dict(common, **changed))

    def test_reserved_version_and_prior_allocations_are_still_readable(self):
        from p05_version import allocated_code as p05
        data = json.loads(Path('release-ledger.json').read_text())
        self.assertEqual(6777, allocated_code(data))
        self.assertEqual(6776, p05(data))

    def test_entire_regression_and_no_signing_are_required(self):
        config = pipeline.configuration(json.loads(Path('.github/p051/stage.json').read_text()))
        self.assertEqual(dict(stage=5, fullRegression=True, classes=[], signed=False), config)


if __name__ == '__main__': unittest.main()
