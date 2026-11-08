import os,json,sys,tempfile,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from p04_pipeline import activation_paths,approval_record
from p04_version import allocated_code
class RefinementGateTests(unittest.TestCase):
 def test_visual_approval_never_skips_product_test_or_signing_changes(self):
  for path in ['src/main/App.java','src/debug/Fixture.java','src/androidTest/Fixture.java','version.properties','release-ledger.json','.github/workflows/p04-ui-refinement.yml','.github/scripts/p04_signing.py']:
   with self.subTest(path=path),self.assertRaises(ValueError):activation_paths([path])
  self.assertTrue(activation_paths(['.github/p04/visual-approval.json','docs/theme-system/ui-refinement/DELIVERY.md']))
 def test_no_actual_visual_approval_means_no_signing(self):
  previous=Path.cwd()
  with tempfile.TemporaryDirectory() as folder:
   try:
    os.chdir(folder)
    with self.assertRaises(ValueError):approval_record()
   finally:os.chdir(previous)
 def test_refinement_code_is_next_without_invalidating_historical_p03(self):
  previous=Path.cwd()
  try:
   os.chdir(Path(__file__).resolve().parents[2]);data=json.loads(Path('release-ledger.json').read_text());self.assertEqual(6775,allocated_code(data))
   from p03_version import allocated_code as previous_code
   self.assertEqual(6774,previous_code(data))
  finally:os.chdir(previous)
