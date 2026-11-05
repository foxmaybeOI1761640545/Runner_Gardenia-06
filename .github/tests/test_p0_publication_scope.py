from pathlib import Path
import sys
import unittest
sys.path.insert(0,str(Path(__file__).parents[1]/'scripts'))
from p0_publication_scope import publication_only,MARKER


class PublicationScopeTests(unittest.TestCase):
    def test_explicit_publication_and_only_allowed_files(self):
        self.assertTrue(publication_only(MARKER,['.github/workflows/p0-core.yml','.github/scripts/p0_publish_existing.py','docs/review.md']))
    def test_normal_code_push_keeps_complete_ci(self):
        self.assertFalse(publication_only('fix(gps): adjust logic',['src/main/java/Position.java']))
    def test_product_or_build_or_signing_changes_cannot_use_old_apk(self):
        for path in ('build.gradle','version.properties','src/main/AndroidManifest.xml','proguard-p0.pro','src/main/java/Fix.java'):
            with self.assertRaises(ValueError):publication_only(MARKER,[path])
    def test_android_or_existing_gate_tests_cannot_be_skipped(self):
        for path in ('src/androidTest/java/Test.java','src/test/java/Test.java','.github/scripts/p0_pipeline.py','.github/tests/test_p0_release.py'):
            with self.assertRaises(ValueError):publication_only(MARKER,[path])
    def test_empty_scope_does_not_authorize_publication(self):
        with self.assertRaises(ValueError):publication_only(MARKER,[])
