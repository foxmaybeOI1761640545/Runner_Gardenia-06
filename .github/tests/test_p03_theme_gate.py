import json,sys,tempfile,os,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from p03_version import allocated_code,VERSION,TAG
from collect_p03_theme_ui import collect
class P03ThemeGateTests(unittest.TestCase):
    def test_reserved_version_uses_exactly_next_code(self):
        root=Path(__file__).resolve().parents[2];previous=Path.cwd()
        try:
            os.chdir(root);self.assertEqual(6774,allocated_code(json.loads(Path('release-ledger.json').read_text())))
            self.assertEqual('v0.1.0-p0.3',VERSION);self.assertEqual('test-p0-v0.1.0-p0.3',TAG)
        finally:os.chdir(previous)
    def test_absent_native_screenshots_cannot_pass(self):
        previous=Path.cwd()
        with tempfile.TemporaryDirectory() as folder:
            try:
                os.chdir(folder)
                with self.assertRaises(ValueError):collect('a'*40)
            finally:os.chdir(previous)
