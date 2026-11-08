import importlib.util,os,sys,unittest
from pathlib import Path
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from p03_publication_recovery import publication_only,MARKER
from publish_release import GitHub

class RecoveryScopeTests(unittest.TestCase):
    def test_normal_product_push_uses_full_build_gates(self):
        self.assertFalse(publication_only('ordinary development',['src/main/App.java']))

    def test_marker_cannot_skip_any_product_version_signing_or_original_test_change(self):
        for path in ['src/main/App.java','src/main/res/values/colors.xml','version.properties','release-ledger.json','.github/scripts/p03_signing.py','.github/p03/stage.json','src/androidTest/Test.java','.github/tests/test_pipeline.py']:
            with self.subTest(path=path),self.assertRaises(ValueError):publication_only(MARKER,[path])
        with self.assertRaises(ValueError):publication_only(MARKER,[])

    def test_only_publication_repair_and_reports_can_reuse_original_apk(self):
        self.assertTrue(publication_only(MARKER,['.github/scripts/p03_publication_recovery.py','.github/scripts/publish_release.py','.github/workflows/p03-theme-system.yml','docs/theme-system/DELIVERY.md']))

class GitHubMediaTypeTests(unittest.TestCase):
    def test_actions_binary_endpoints_keep_github_media_type_and_release_assets_use_octet(self):
        class Response:
            def __enter__(self):return self
            def __exit__(self,*args):pass
            def read(self):return b'actual binary response'
        class Opener:
            def open(self,request,timeout):
                self.accept=request.get_header('Accept');return Response()
        with patch.dict(os.environ,{'GH_TOKEN':'fixture-not-a-credential','GITHUB_REPOSITORY':'test/fixture'}):api=GitHub()
        api.opener=Opener()
        for path,expected in [('actions/jobs/123/logs','application/vnd.github+json'),('actions/artifacts/123/zip','application/vnd.github+json'),('releases/assets/123','application/octet-stream')]:
            self.assertEqual(b'actual binary response',api.request(api.base+'/'+path,binary=True))
            self.assertEqual(expected,api.opener.accept)


class NativePageEvidenceTests(unittest.TestCase):
    def test_capture_rejects_stale_home_for_other_pages_and_transition_mixed_tree(self):
        import xml.etree.ElementTree as ET
        from p03_release_theme_ui import PAGE_IDS,page_matches
        def tree(*ids):
            root=ET.Element('hierarchy')
            for identity in ids:ET.SubElement(root,'node',{'resource-id':'com.fox.runner:id/'+identity})
            return root
        for page,identity in PAGE_IDS.items():
            self.assertTrue(page_matches(tree(identity),page))
            for other,other_id in PAGE_IDS.items():
                if other!=page:
                    self.assertFalse(page_matches(tree(other_id),page))
                    self.assertFalse(page_matches(tree(identity,other_id),page))

class PublicationFlowTests(unittest.TestCase):
    def test_all_eight_assets_roundtrip_before_publication_and_public_apk_recheck(self):
        import contextlib,hashlib,io,json,tempfile,urllib.parse
        import p03_publish
        raw_apk=b'publication transport fixture, not an accepted APK'
        info={'commit':'a'*40,'versionName':p03_publish.VERSION,'versionCode':6774,'apkSha256':hashlib.sha256(raw_apk).hexdigest(),'apkBytes':len(raw_apk),'certificateSha256':p03_publish.CERT,'run':'https://test.invalid/build','tests':{},'plan':{'runId':'123'}}
        class API:
            repo='test/fixture';base='https://api.github.com/repos/test/fixture'
            def __init__(self):self.release=None;self.files={};self.downloads={};self.uploaded=[];self.published=False
            def assets(self,release):return [self.metadata(n) for n in self.files]
            def metadata(self,name):return {'name':name,'url':self.base+'/releases/assets/'+name,'state':'uploaded','size':len(self.files[name]),'digest':'sha256:'+hashlib.sha256(self.files[name]).hexdigest(),'browser_download_url':'https://github.com/'+self.repo+'/releases/download/'+p03_publish.TAG+'/'+name}
            def request(self,url,method='GET',data=None,binary=False,optional=False):
                if '/releases/assets/' in url:
                    name=url.split('/releases/assets/')[1];self.downloads[name]=self.downloads.get(name,0)+1;return self.files[name]
                if url.startswith('https://uploads.github.com/'):
                    name=urllib.parse.parse_qs(urllib.parse.urlsplit(url).query)['name'][0]
                    assert self.release['draft'] and name not in self.files
                    self.files[name]=data;self.uploaded.append(name);return self.metadata(name)
                if url==self.base+'/releases' and method=='POST':
                    assert data['draft'] and data['prerelease'] and data['make_latest']=='false'
                    self.release={**data,'id':1,'url':self.base+'/releases/1','html_url':'https://github.com/test/fixture/releases/tag/'+p03_publish.TAG,'upload_url':'https://uploads.github.com/repos/test/fixture/releases/1/assets{?name,label}'};return self.release
                if '/releases?' in url:return [self.release] if self.release else []
                if url==self.base+'/releases/latest':return {'id':0}
                if self.release and url==self.release['url']:
                    if method=='PATCH':
                        assert data['draft'] is False and data['prerelease'] and data['make_latest']=='false'
                        assert len(self.files)==8 and all(self.downloads.get(n,0)>=1 for n in self.files)
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
            for name in ['build-info.json',p03_publish.APK,'SHA256SUMS','runtime-smoke.json','native-theme-smoke.json','theme-screenshots.zip']:(root/name).write_bytes(raw_apk if name==p03_publish.APK else name.encode())
            api=API()
            with patch.object(p03_publish,'remote_history'),patch.object(p03_publish,'helper_counts',return_value={'tests':68}),patch.object(p03_publish,'verify_tag',return_value={'valid':True}),patch('publish_release.verify_apk') as verify,patch('urllib.request.urlopen',return_value=PublicResponse()),contextlib.redirect_stdout(io.StringIO()):
                p03_publish.publish(api,root,verify_bundle=lambda _:info)
            proof=json.loads((root/'publication.json').read_text())
            self.assertTrue(api.published);self.assertEqual(8,len(api.uploaded));self.assertTrue(all(v>=2 for v in api.downloads.values()))
            self.assertTrue(proof['publicDownloadVerified']);self.assertEqual(info['apkSha256'],proof['apkSha256']);verify.assert_called_once_with(raw_apk,info)

if __name__=='__main__':unittest.main()
