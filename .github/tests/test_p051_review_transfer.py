"""Original APK bytes stay in Artifacts/Releases; never enter Git evidence blobs."""
import base64
import contextlib
import hashlib
import io
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import p051_review_transfer as transfer


class P051ReviewTransportTests(unittest.TestCase):
    def fixture(self):
        Path('evidence').mkdir()
        Path('.github/p051').mkdir(parents=True)
        apk = b'synthetic APK sentinel, never transported through Git'
        Path('evidence/RunnerGardenia-P0-Release.apk').write_bytes(apk)
        Path('evidence/build-info.json').write_text(json.dumps(dict(
            buildWorkflowCommit='b'*40, commit='a'*40, testRunId='tested',
            apkSha256=hashlib.sha256(apk).hexdigest())))
        Path('.github/p051/visual-approval.json').write_text(json.dumps(dict(source='a'*40, testRunId='tested')))
        for name in ('runtime-smoke.json', 'native-theme-smoke.json'):
            Path('evidence', name).write_text(json.dumps(dict(passed=True)))
        return dict(GITHUB_EVENT_NAME='push', GITHUB_REF='refs/heads/feature/p0-5-1-gps-motion-quality',
                    GITHUB_SHA='b'*40, GITHUB_RUN_ID='123', GITHUB_OUTPUT=str(Path('output.txt').resolve()))

    def execute(self, secret=False):
        class API:
            base='https://test.invalid'
            def __init__(self): self.payloads=[]
            def request(self, url, method, data):
                self.payloads.append(data)
                return {'sha': 'c'*40}
        api = API()
        with tempfile.TemporaryDirectory() as directory:
            current = Path.cwd()
            try:
                os.chdir(directory)
                env = self.fixture()
                if secret: Path('evidence/forbidden.jks').write_bytes(b'not a real key')
                with patch.dict(os.environ, env), patch.object(sys, 'argv', ['transfer','--signed']), \
                        patch.object(transfer, 'GitHub', return_value=api), contextlib.redirect_stdout(io.StringIO()):
                    if secret:
                        with self.assertRaisesRegex(ValueError, 'Signing key'): transfer.run()
                        self.assertEqual([], api.payloads)
                        return
                    transfer.run()
                index = json.loads(Path('review-transfer.json').read_text())
                self.assertTrue(index['signedReview'])
                self.assertFalse(index['signedApkIncluded'])
                self.assertFalse(index['gitRefUpdated'])
                raw = b''.join(base64.b64decode(d['content']) for d in api.payloads if d['encoding']=='base64')
                self.assertEqual(index['sha256'], hashlib.sha256(raw).hexdigest())
                with zipfile.ZipFile(io.BytesIO(raw)) as archive:
                    self.assertFalse(any(n.endswith(('.apk','.apks','.aab','.jks','.keystore')) for n in archive.namelist()))
                    self.assertIn('native-theme-smoke.json', archive.namelist())
                    self.assertIn('build-info.json', archive.namelist())
            finally: os.chdir(current)

    def test_signed_review_excludes_apk_and_preserves_proof(self): self.execute()
    def test_signing_key_file_aborts_before_any_upload(self): self.execute(secret=True)


if __name__ == '__main__': unittest.main()
