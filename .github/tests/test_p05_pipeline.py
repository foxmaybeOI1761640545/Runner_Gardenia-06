import copy
import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

SCRIPTS = Path(__file__).resolve().parents[1] / 'scripts'
sys.path.insert(0, str(SCRIPTS))
import p05_pipeline as pipeline
from p05_baseline_apk import verify_metadata


class P05PhaseRoutingTests(unittest.TestCase):
    def test_phase_zero_has_no_android_classes_or_signing_authority(self):
        initial = {'stage': 0, 'fullRegression': False, 'classes': [], 'signed': False}
        self.assertEqual(initial, pipeline.configuration(initial))
        for changed in [dict(initial, signed=True), dict(initial, fullRegression=True),
                        dict(initial, classes=['a.Test']), dict(initial, stage=-1),
                        dict(initial, stage=True)]:
            with self.subTest(changed=changed), self.assertRaises(ValueError):
                pipeline.configuration(changed)

    def test_future_phases_cannot_run_zero_tests_or_narrow_final_regression(self):
        for stage in range(1, 6):
            with self.subTest(stage=stage), self.assertRaises(ValueError):
                pipeline.configuration({'stage': stage, 'fullRegression': False,
                                        'classes': [], 'signed': False})
        for classes in [['--stacktrace'], ['a.Test', 'a.Test']]:
            with self.assertRaises(ValueError):
                pipeline.configuration({'stage': 1, 'fullRegression': False,
                                        'classes': classes, 'signed': False})
        with self.assertRaises(ValueError):
            pipeline.configuration({'stage': 5, 'fullRegression': False,
                                    'classes': ['a.Test'], 'signed': False})
        self.assertEqual(5, pipeline.configuration({'stage': 5, 'fullRegression': True,
                                                    'classes': [], 'signed': False})['stage'])

    def test_no_other_branch_or_event_can_activate_validation(self):
        good = {'GITHUB_REPOSITORY': pipeline.REPO,
                'GITHUB_REF': 'refs/heads/' + pipeline.BRANCH, 'GITHUB_EVENT_NAME': 'push'}
        pipeline.trusted(good)
        for key, value in [('GITHUB_REF', 'refs/heads/main'),
                           ('GITHUB_REF', 'refs/heads/feature/theme-system-p0-3-ui-refinement'),
                           ('GITHUB_REPOSITORY', 'someone/fork'), ('GITHUB_EVENT_NAME', 'pull_request')]:
            with self.subTest(key=key, value=value), self.assertRaises(ValueError):
                pipeline.trusted(dict(good, **{key: value}))

    def test_phase_zero_rejects_product_changes_without_ignoring_them(self):
        with patch.object(pipeline, 'git', return_value='src/main/res/layout/gardenia_home_quiet.xml'):
            with self.assertRaises(ValueError):
                pipeline.phase_zero_source()

    def test_historical_branch_tag_or_p04_identity_change_is_detected(self):
        snapshot = json.loads((SCRIPTS.parent / 'p05/protected-refs.json').read_text())
        refs = snapshot['protectedRefs']
        pipeline.compare_refs(snapshot, refs)
        for name in ['refs/heads/main', 'refs/tags/test-p0-v0.1.0-p0.4',
                     'refs/tags/test-p0-v0.1.0-p0.4^{}']:
            altered = dict(refs, **{name: '0' * 40})
            with self.subTest(name=name), self.assertRaises(ValueError):
                pipeline.compare_refs(snapshot, altered)
        altered = copy.deepcopy(snapshot)
        altered['p04']['versionCode'] = 6776
        with self.assertRaises(ValueError):
            pipeline.compare_refs(altered, refs)

    def test_missing_android_xml_cannot_turn_green_after_compilation(self):
        config = {'stage': 1, 'fullRegression': True, 'classes': [], 'signed': False}
        with tempfile.TemporaryDirectory() as directory:
            original = Path.cwd()
            try:
                os.chdir(directory)
                reports = Path('build/reports/p05-validation')
                reports.mkdir(parents=True)
                (reports / 'helpers.json').write_text(json.dumps({'passed': True, 'executed': 3}))
                Path('release-plan.json').write_text(json.dumps({'commit': 'same', 'stage': 1}))
                with patch.object(pipeline, 'configuration', return_value=config), \
                        patch.dict(os.environ, {'GITHUB_SHA': 'same'}), \
                        contextlib.redirect_stdout(io.StringIO()):
                    with self.assertRaises(SystemExit):
                        pipeline.gate()
                proof = json.loads((reports / 'tests.json').read_text())
                self.assertFalse(proof['passed'])
                self.assertIn('reports are missing', proof['diagnostic'])
            finally:
                os.chdir(original)


class P04PublicBaselineTests(unittest.TestCase):
    def test_public_asset_metadata_requires_original_bytes_and_source(self):
        expected = json.loads((SCRIPTS.parent / 'p05/protected-refs.json').read_text())['p04']
        asset = {'name': 'RunnerGardenia-P0-Release.apk', 'state': 'uploaded',
                 'size': expected['apkBytes'], 'digest': 'sha256:' + expected['apkSha256']}
        release = {'tag_name': expected['tag'], 'draft': False, 'prerelease': True,
                   'target_commitish': expected['source'], 'assets': [asset]}
        self.assertEqual(asset, verify_metadata(release, expected))
        for changed in [dict(release, draft=True), dict(release, target_commitish=pipeline.BASE),
                        dict(release, assets=[asset, asset]),
                        dict(release, assets=[dict(asset, digest='sha256:' + '0' * 64)])]:
            with self.assertRaises(ValueError):
                verify_metadata(changed, expected)


if __name__ == '__main__':
    unittest.main()
