"""Recovery must preserve signed bytes and fail closed if they are unavailable."""
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
import signed_bundle as bundle
import publish_release


class SignedRecoveryTests(unittest.TestCase):
    plan={'versionName':'v0.1.0-rc1.5','versionCode':6770,'commit':'a'*40,'runId':'123','publishing':True}

    def fixture(self,folder):
        filename='runner-gardenia-v0.1.0-rc1.5-6770.apk'
        data=b'test artifact; not a signed Android APK'
        info={**self.plan,'plan':self.plan,'apkSha256':bundle.hashlib.sha256(data).hexdigest(),
              'applicationId':'com.fox.runner','variant':'reproducibleRelease','debuggable':False,
              'signatureVerified':True,'certificateSha256':'B'*64}
        (folder/filename).write_bytes(data)
        (folder/'build-info.json').write_text(json.dumps(info))
        (folder/'SHA256SUMS').write_text(info['apkSha256']+'  '+filename+'\n')
        return info

    def test_restore_missing_expired_or_wrong_run_never_rebuilds_tagged_apk(self):
        env={'GITHUB_RUN_ID':'123','GITHUB_REPOSITORY':'owner/repo','GITHUB_OUTPUT':''}
        with tempfile.TemporaryDirectory() as tmp,patch.dict(os.environ,env),patch.object(bundle,'git',return_value=self.plan['versionName']),patch.object(bundle,'tag_plan',return_value=self.plan):
            for artifacts in [[],[{'name':'signed-apk-123','expired':True}]]:
                with patch.object(bundle.subprocess,'check_output',return_value=json.dumps([{'artifacts':artifacts}])):
                    with self.assertRaises(SystemExit):bundle.restore(self.plan,Path(tmp))
            with self.assertRaises(SystemExit):bundle.restore({**self.plan,'runId':'other'},Path(tmp))

    def test_restore_existing_bytes_and_reverify_independent_certificate(self):
        env={'GITHUB_RUN_ID':'123','GITHUB_REPOSITORY':'owner/repo','GITHUB_OUTPUT':'',
             'ANDROID_SIGNING_CERT_SHA256':'B'*64}
        with tempfile.TemporaryDirectory() as tmp,patch.dict(os.environ,env):
            folder=Path(tmp);info=self.fixture(folder)
            with patch.object(bundle,'git',return_value=self.plan['versionName']),patch.object(bundle,'tag_plan',return_value=self.plan),patch.object(bundle,'verify_tag_digest') as tag_check,patch.object(publish_release,'verify_apk') as signer:
                bundle.reverify(self.plan,folder)
                signer.assert_called_once();tag_check.assert_called_once_with(self.plan['versionName'],info['apkSha256'])
                with patch.dict(os.environ,{'ANDROID_SIGNING_CERT_SHA256':'C'*64}):
                    with self.assertRaises(SystemExit):bundle.reverify(self.plan,folder)
            listing=json.dumps([{'artifacts':[{'name':'signed-apk-123','expired':False}]}])
            with patch.object(bundle,'git',return_value=''),patch.object(bundle.subprocess,'check_output',return_value=listing),patch.object(bundle.subprocess,'run') as download:
                bundle.restore(self.plan,folder)
                download.assert_called_once()
            (folder/'extra.apk').write_bytes(b'extra')
            with self.assertRaises(SystemExit):bundle.validate_bundle(self.plan,folder)

    def test_unsigned_or_foreign_plan_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder=Path(tmp);info=self.fixture(folder)
            for field,value in [('debuggable',True),('signatureVerified',False),('commit','c'*40),('certificateSha256','invalid')]:
                (folder/'build-info.json').write_text(json.dumps({**info,field:value}))
                with self.assertRaises(SystemExit):bundle.validate_bundle(self.plan,folder)


if __name__=='__main__':unittest.main()
