from pathlib import Path
import sys
import unittest
sys.path.insert(0,str(Path(__file__).parents[1]/'scripts'))
from p02_publication_scope import publication_only,MARKER
class ScopeTests(unittest.TestCase):
    def test_normal_push_keeps_full_gates(self):
        self.assertFalse(publication_only('normal source change',['src/main/a.java']))
    def test_explicit_recovery_accepts_only_publication_and_reports(self):
        self.assertTrue(publication_only(MARKER,['.github/scripts/publish_release.py','docs/reports/proof.json']))
    def test_marker_never_bypasses_product_version_signing_or_tests(self):
        for name in ('src/main/a.java','version.properties','release-ledger.json','.github/p02/stage.json','.github/scripts/p02_signing.py','src/androidTest/a.java','src/test/a.java'):
            with self.assertRaises(ValueError):publication_only(MARKER,[name])
