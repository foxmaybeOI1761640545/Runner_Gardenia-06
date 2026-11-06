"""Network-free recovery/security scenarios; fixture bytes are never APK acceptance evidence."""
import copy
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
from urllib.parse import urlsplit,parse_qs
sys.path.insert(0,str(Path(__file__).parents[1]/'scripts'))
import p02_publish as pub
from p02_version import TAG,VERSION,CERT,APK

class API:
    base='https://api.github.com/repos/test/fixture'
    def __init__(self):self.ref=None;self.tag=None;self.release=None;self.content={};self.downloads=set();self.fail=None;self.tag_creations=0;self.patch_count=0
    def assets(self,r):return list(self.release['assets'])
    def request(self,url,method='GET',data=None,binary=False,optional=False,api_binary=False):
        path=urlsplit(url).path
        if '/actions/runs/' in path:return {'jobs':[{'name':'checks','conclusion':'success','id':123}]}
        if '/actions/jobs/' in path:return b'2026-11-07T01:00:00Z Ran 66 tests in 0.4s\n2026-11-07T01:00:00Z \n2026-11-07T01:00:00Z OK\n'
        if '/git/ref/tags/' in path:return self.ref
        if path.endswith('/git/tags') and method=='POST':
            self.tag_creations+=1;self.tag={'sha':'a'*40,'object':{'type':'commit','sha':data['object']},'message':data['message']};return self.tag
        if '/git/tags/' in path:return self.tag
        if path.endswith('/git/refs'):
            if self.ref:raise AssertionError('Never replace ref')
            self.ref={'object':{'type':'tag','sha':data['sha']}};return self.ref
        if path.endswith('/releases/latest'):return None
        if path.endswith('/releases') and method=='GET':return [self.release] if self.release else []
        if path.endswith('/releases') and method=='POST':
            assert data['draft'] is True and data['prerelease'] is True and data['make_latest']=='false'
            self.release={**data,'id':1,'url':self.base+'/release/1','upload_url':'https://uploads.github.com/fixture/upload{?name}','html_url':'https://github.com/test/fixture/releases/tag/'+TAG,'assets':[]};return self.release
        if method=='POST' and path.endswith('/upload'):
            name=parse_qs(urlsplit(url).query)['name'][0];assert name not in self.content
            self.content[name]=data;asset={'name':name,'state':'uploaded','size':len(data),'digest':'sha256:'+pub.digest(data),'url':self.base+'/asset/'+name,'browser_download_url':'https://github.com/test/'+name};self.release['assets'].append(asset)
            if self.fail==name:self.fail=None;raise RuntimeError('Simulated connection lost after server upload')
            return asset
        if '/asset/' in path:
            name=path.split('/asset/')[1];self.downloads.add(name);return self.content[name]
        if path.endswith('/release/1'):
            if method=='PATCH':
                assert set(self.content)<=self.downloads,'Every Draft asset must be downloaded before publishing'
                assert data['make_latest']=='false' and data['prerelease'] is True and data['draft'] is False
                self.patch_count+=1;self.release.update(data)
            return self.release
        raise AssertionError((url,method))

class PublicationTests(unittest.TestCase):
    def scenario(self,operation):
        with tempfile.TemporaryDirectory() as t:
            folder=Path(t);raw=b'unit fixture bytes, not an APK';info={'plan':{'runId':'1'},'commit':'b'*40,'versionName':VERSION,'versionCode':6773,'certificateSha256':CERT,'apkSha256':pub.digest(raw),'apkBytes':len(raw),'run':'https://github.com/test/actions/runs/1','tests':{'android':{'tests':1,'executed':1,'failures':0,'errors':0,'skipped':0}}}
            files={'build-info.json':json.dumps(info).encode(),APK:raw,'SHA256SUMS':(pub.digest(raw)+'  '+APK+'\n').encode(),'runtime-smoke.json':b'unit fixture'}
            for n,content in files.items():(folder/n).write_bytes(content)
            api=API()
            with patch.object(pub,'bundle',return_value=info),patch.object(pub,'remote_history'):
                operation(api,info,folder)
    def test_tag_only_after_bundle_gate_and_no_tag_on_failed_gate(self):
        api=API()
        with patch.object(pub,'bundle',side_effect=ValueError('Actual gate failed')):
            with self.assertRaises(ValueError):pub.publish(api)
        self.assertEqual(0,api.tag_creations);self.assertIsNone(api.release)
    def test_success_verifies_draft_downloads_then_publishes_non_latest(self):
        def run(api,info,folder):
            pub.publish(api,folder);self.assertFalse(api.release['draft']);self.assertEqual(1,api.patch_count);self.assertEqual(6,len(api.content));self.assertEqual(info['commit'],api.tag['object']['sha'])
        self.scenario(run)
    def test_identical_retry_reuses_tag_and_assets_without_overwrite(self):
        def run(api,info,folder):
            pub.publish(api,folder);before=dict(api.content);pub.publish(api,folder);self.assertEqual(before,api.content);self.assertEqual(1,api.tag_creations);self.assertEqual(1,api.patch_count)
        self.scenario(run)
    def test_network_interruption_after_upload_resumes_partial_draft(self):
        def run(api,info,folder):
            api.fail=APK
            with self.assertRaises(RuntimeError):pub.publish(api,folder)
            self.assertTrue(api.release['draft']);self.assertEqual(0,api.patch_count)
            pub.publish(api,folder);self.assertFalse(api.release['draft']);self.assertEqual(1,api.tag_creations)
        self.scenario(run)
    def test_different_commit_tag_is_never_moved(self):
        def run(api,info,folder):
            pub.publish(api,folder);api.tag['object']['sha']='c'*40
            with self.assertRaises(ValueError):pub.publish(api,folder)
            self.assertEqual(1,api.tag_creations)
        self.scenario(run)
    def test_different_existing_apk_is_never_replaced(self):
        def run(api,info,folder):
            pub.publish(api,folder);api.content[APK]=b'different bytes'
            with self.assertRaises(ValueError):pub.publish(api,folder)
            self.assertEqual(b'different bytes',api.content[APK])
        self.scenario(run)
