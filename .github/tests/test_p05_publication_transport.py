import importlib.util,os,sys,unittest
from pathlib import Path
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from publish_release import GitHub

class PublicationFlowTests(unittest.TestCase):
    def test_immutable_payloads_roundtrip_and_version_evidence_upload_first(self):
        import contextlib,hashlib,io,json,tempfile,urllib.parse
        import p05_publish
        raw_apk=b'publication transport fixture, not an accepted APK'
        info={'commit':'a'*40,'versionName':p05_publish.VERSION,'versionCode':6776,'apkSha256':hashlib.sha256(raw_apk).hexdigest(),'apkBytes':len(raw_apk),'certificateSha256':p05_publish.CERT,'run':'https://test.invalid/build','tests':{},'plan':{'runId':'123'}}
        class API:
            repo='test/fixture';base='https://api.github.com/repos/test/fixture'
            def __init__(self):self.release=None;self.files={};self.downloads={};self.uploaded=[];self.published=False
            def assets(self,release):return [self.metadata(n) for n in self.files]
            def metadata(self,name):return {'name':name,'url':self.base+'/releases/assets/'+name,'state':'uploaded','size':len(self.files[name]),'digest':'sha256:'+hashlib.sha256(self.files[name]).hexdigest(),'browser_download_url':'https://github.com/'+self.repo+'/releases/download/'+p05_publish.TAG+'/'+name}
            def request(self,url,method='GET',data=None,binary=False,optional=False):
                if '/releases/assets/' in url:
                    name=url.split('/releases/assets/')[1];self.downloads[name]=self.downloads.get(name,0)+1;return self.files[name]
                if url.startswith('https://uploads.github.com/'):
                    name=urllib.parse.parse_qs(urllib.parse.urlsplit(url).query)['name'][0]
                    assert self.release['draft'] and name not in self.files
                    self.files[name]=data;self.uploaded.append(name);return self.metadata(name)
                if url==self.base+'/releases' and method=='POST':
                    assert data['draft'] and data['prerelease'] and data['make_latest']=='false'
                    self.release={**data,'id':1,'url':self.base+'/releases/1','html_url':'https://github.com/test/fixture/releases/tag/'+p05_publish.TAG,'upload_url':'https://uploads.github.com/repos/test/fixture/releases/1/assets{?name,label}'};return self.release
                if '/releases?' in url:return [self.release] if self.release else []
                if url==self.base+'/releases/latest':return {'id':0}
                if self.release and url==self.release['url']:
                    if method=='PATCH':
                        assert data['draft'] is False and data['prerelease'] and data['make_latest']=='false'
                        assert len(self.files)==11 and all(self.downloads.get(n,0)>=1 for n in self.files)
                        self.release.update(data);self.published=True
                    return self.release
                raise AssertionError((url,method))
        class PublicResponse:
            status=200
            def __enter__(self):return self
            def __exit__(self,*args):pass
            def read(self):return raw_apk
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder)
            for name in ['build-info.json',p05_publish.APK,'SHA256SUMS','runtime-smoke.json','native-theme-smoke.json','theme-screenshots.zip','test-summary.json','RELEASE_NOTES.md','DEVELOPMENT_COMMITS.txt','key-test-receipts.zip']:(root/name).write_bytes(raw_apk if name==p05_publish.APK else name.encode())
            manifest={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in root.iterdir()}
            (root/'ARTIFACT-MANIFEST.json').write_text(json.dumps(manifest))
            originals={p.name:p.read_bytes() for p in root.iterdir()}
            api=API()
            with patch.object(p05_publish,'remote_history'),patch.object(p05_publish,'verify_tag',return_value={'valid':True}),patch('publish_release.verify_apk') as verify,patch('urllib.request.urlopen',return_value=PublicResponse()) as public_get,contextlib.redirect_stdout(io.StringIO()):
                p05_publish.publish(api,root,verify_bundle=lambda _:info)
            proof=json.loads((root/'publication.json').read_text())
            self.assertTrue(api.published);self.assertEqual(11,len(api.uploaded));self.assertEqual('build-info.json',api.uploaded[0]);self.assertEqual(originals,api.files);self.assertTrue(all(v>=2 for v in api.downloads.values()))
            self.assertTrue(proof['publicDownloadVerified']);self.assertEqual(info['apkSha256'],proof['apkSha256']);verify.assert_called_once_with(raw_apk,info)
            request=public_get.call_args.args[0];self.assertTrue(request.full_url.startswith(api.base+'/releases/assets/'));self.assertEqual('application/octet-stream',request.get_header('Accept'));self.assertIsNone(request.get_header('Authorization'));self.assertFalse(proof['publicDownloadAuthenticated'])

if __name__=='__main__':unittest.main()
