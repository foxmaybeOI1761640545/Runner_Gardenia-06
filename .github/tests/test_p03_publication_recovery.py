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

if __name__=='__main__':unittest.main()

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
