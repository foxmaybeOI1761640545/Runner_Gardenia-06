"""Local regression tests for version allocation, strict report gates and safe publication."""
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import urllib.request

SCRIPTS = Path(__file__).resolve().parents[1] / 'scripts'
sys.path.insert(0, str(SCRIPTS))
import release_policy as policy
import check_test_results as reports
import publish_release as publication


class VersionTests(unittest.TestCase):
    def test_allocation_gate_immutable_tag_and_rerun(self):
        with tempfile.TemporaryDirectory() as tmp:
            original = Path.cwd()
            os.chdir(tmp)
            try:
                def git(*args):
                    return subprocess.check_output(['git', *args], stderr=subprocess.DEVNULL, text=True).strip()
                git('init', '-b', 'main')
                git('config', 'user.name', 'Pipeline test')
                git('config', 'user.email', 'pipeline@example.invalid')
                Path('version.properties').write_text('VERSION_NAME=v0.1.0-rc1.1\nVERSION_CODE=6766\n')
                git('add', '.'); git('commit', '-m', 'fixture')
                git('tag', '-a', 'v0.1.0-rc1.1', '-m', 'legacy tag')
                remote = Path(tmp) / 'remote.git'
                subprocess.run(['git', 'init', '--bare', str(remote)], check=True, capture_output=True)
                git('remote', 'add', 'origin', str(remote));git('push', 'origin', 'main', '--tags')
                env = {'GITHUB_RUN_ID': '123', 'GITHUB_EVENT_NAME': 'push', 'GITHUB_REF': 'refs/heads/main',
                       'GITHUB_REPOSITORY': 'foxmaybeOI1761640545/Runner_Gardenia-06', 'GITHUB_OUTPUT': '', 'GITHUB_SHA': ''}
                with patch.dict(os.environ, env):
                    policy.prepare('plan.json')
                    plan = json.loads(Path('plan.json').read_text())
                    self.assertEqual(('v0.1.0-rc1.2', 6767), (plan['versionName'], plan['versionCode']))
                    Path('gate.json').write_text(json.dumps({'plan': plan, 'passed': False}))
                    with self.assertRaises(SystemExit): policy.create_tag('plan.json', 'gate.json')
                    self.assertNotIn(plan['versionName'], git('tag', '--list'))
                    Path('gate.json').write_text(json.dumps({'plan': plan, 'passed': True}))
                    policy.create_tag('plan.json', 'gate.json')
                    tag_object = git('rev-parse', plan['versionName'])
                    self.assertEqual(plan['commit'], git('rev-parse', plan['versionName'] + '^{commit}'))
                    policy.prepare('again.json')
                    self.assertEqual(plan, json.loads(Path('again.json').read_text()))
                    policy.apply('plan.json');policy.verify_tag('plan.json')
                    policy.create_tag('plan.json', 'gate.json')
                    self.assertEqual(tag_object, git('rev-parse', plan['versionName']))
                    bad = {**plan, 'runId': 'other'}
                    Path('bad.json').write_text(json.dumps(bad))
                    Path('bad-gate.json').write_text(json.dumps({'plan': bad, 'passed': True}))
                    with self.assertRaises(SystemExit):policy.create_tag('bad.json', 'bad-gate.json')
                    Path('version.properties').write_text('VERSION_NAME=v0.1.0-rc1.1\nVERSION_CODE=6766\n')
                    Path('next.txt').write_text('next');git('add','next.txt');git('commit','-m','next');git('push','origin','main')
                    with patch.dict(os.environ, {'GITHUB_RUN_ID': '124'}):
                        policy.prepare('next.json')
                    following = json.loads(Path('next.json').read_text())
                    self.assertEqual(('v0.1.0-rc1.3',6768),(following['versionName'],following['versionCode']))
            finally:os.chdir(original)


class ReportTests(unittest.TestCase):
    methods = {
        'de.dennisguse.opentracks.TimezoneEnvironmentTest': ['instrumentationDefaultIsSingapore', 'emulatorSystemTimezoneIsSingapore', 'timezoneRuleRestoresPreviousDefaultOnFailure'],
        'de.dennisguse.opentracks.services.TrackRecordingServicePreferenceLifecycleTest': ['callbackAfterDestroyDoesNotAccessReleasedManagers', 'queuedPreferenceSnapshotIsIgnoredAfterListenerUnregister'],
    }

    def fixture(self, tmp, error=False):
        source=Path(tmp)/'source';source.mkdir()
        suites=[]
        for owner, methods in self.methods.items():
            package,name=owner.rsplit('.',1)
            (source/(name+'.java')).write_text(f'package {package};\nclass {name} {{\n@Test\n}}')
            cases=''.join(f'<testcase classname="{owner}" name="{m}">' + ('<error>failure</error>' if error and m==methods[0] else '') + '</testcase>' for m in methods)
            suites.append(f'<testsuite name="{owner}" tests="{len(methods)}" failures="0" errors="{int(error)}" skipped="0">{cases}</testsuite>')
        xml=Path(tmp)/'TEST-device.xml'
        xml.write_text(f'<testsuites tests="5" failures="0" errors="{2 if error else 0}" skipped="0">'+''.join(suites)+'</testsuites>')
        return source,xml

    def test_complete_and_separate_errors(self):
        with tempfile.TemporaryDirectory() as tmp:
            source,xml=self.fixture(tmp)
            self.assertTrue(reports.inspect(Path(tmp),source)['passed'])
        with tempfile.TemporaryDirectory() as tmp:
            source,xml=self.fixture(tmp,error=True)
            report=reports.inspect(Path(tmp),source)
            self.assertFalse(report['passed']);self.assertEqual(2,report['errors']);self.assertEqual(0,report['failures'])

    def test_missing_corrupt_and_miscounted_reports(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(ValueError):reports.inspect(Path(tmp),Path(tmp))
            source,xml=self.fixture(tmp)
            xml.write_text(xml.read_text().replace('tests="5"','tests="6"'))
            with self.assertRaises(ValueError):reports.inspect(Path(tmp),source)
            xml.write_text('<not-xml')
            with self.assertRaises(reports.ET.ParseError):reports.inspect(Path(tmp),source)


class FakeGitHub:
    base='https://api.github.com/repos/owner/repo'
    def __init__(self): self.record=None;self.data={};self.uploads=0;self.fail_upload=False
    def release(self,tag):return self.record
    def request(self,url,method='GET',data=None,binary=False,optional=False):
        if method=='POST' and url==self.base+'/releases':
            self.record={**data,'url':self.base+'/releases/1','assets_url':self.base+'/releases/1/assets',
                         'upload_url':'https://uploads.github.com/assets{?name}', 'html_url':'https://github.com/owner/repo/releases/tag/'+data['tag_name']}
            return self.record
        if method=='POST':
            if self.fail_upload and self.uploads==1:raise RuntimeError('network interrupted')
            name=publication.urllib.parse.parse_qs(publication.urllib.parse.urlsplit(url).query)['name'][0]
            self.data[name]=data;self.uploads+=1;return {}
        if method=='PATCH':self.record.update(data);return self.record
        if binary:return self.data[url.rsplit('/',1)[1]]
        raise AssertionError((method,url))
    def assets(self,release):
        return [{'name':n,'url':'https://api.github.com/assets/'+n,'browser_download_url':'https://github.com/owner/repo/releases/download/tag/'+n} for n in self.data]


class PublicationTests(unittest.TestCase):
    def test_draft_resume_published_rerun_and_identity_rejection(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder=Path(tmp)
            apk=b'fixture APK bytes, not a real signing key'
            plan={'versionName':'v0.1.0-rc1.2','versionCode':6767,'commit':'a'*40}
            info={**plan,'plan':plan,'apkSha256':publication.digest(apk),'applicationId':'com.fox.runner',
                  'certificateSha256':'B'*64,'variant':'reproducibleRelease','run':'https://github.com/owner/repo/actions/runs/1'}
            filename='runner-gardenia-v0.1.0-rc1.2-6767.apk'
            (folder/filename).write_bytes(apk)
            (folder/'build-info.json').write_text(json.dumps(info))
            (folder/'SHA256SUMS').write_text(info['apkSha256']+'  '+filename+'\n')
            api=FakeGitHub();api.fail_upload=True
            def verify(data,expected):
                if publication.digest(data)!=expected['apkSha256']:raise RuntimeError('checksum mismatch')
            with patch.object(publication.subprocess,'check_output',return_value='a'*40),patch.object(publication,'verify_apk',side_effect=verify),patch.dict(os.environ,{'GITHUB_STEP_SUMMARY':''}):
                with self.assertRaises(RuntimeError):publication.publish(api,folder)
                self.assertTrue(api.record['draft']);self.assertEqual({'build-info.json'},set(api.data))
                api.fail_upload=False;publication.publish(api,folder)
                self.assertFalse(api.record['draft']);self.assertTrue(api.record['prerelease']);self.assertEqual('false',api.record['make_latest'])
                uploads=api.uploads;publication.publish(api,folder);self.assertEqual(uploads,api.uploads)
                invalid={**info,'commit':'c'*40};api.data['build-info.json']=json.dumps(invalid).encode()
                with self.assertRaises(RuntimeError):publication.publish(api,folder)
                self.assertEqual(uploads,api.uploads)

    def test_no_credentials_on_cross_host_redirect(self):
        request=urllib.request.Request('https://api.github.com/assets/1',headers={'Authorization':'Bearer fixture'})
        redirected=publication.SafeRedirect().redirect_request(request,None,302,'Found',{},'https://storage.example.invalid/data')
        self.assertIsNone(redirected.get_header('Authorization'))


if __name__=='__main__':unittest.main()
