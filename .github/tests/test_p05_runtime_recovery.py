import sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from p05_runtime_recovery import delta,ALLOWED,APK_SHA
class RuntimeRecoveryScopeTest(unittest.TestCase):
 def test_only_new_runtime_harness_workflow_helpers_and_receipts_can_change(self):
  delta(sorted(ALLOWED)+['docs/p0-5/05-final-delivery.md'])
  for p in ['src/main/Activity.java','src/androidTest/Test.java','version.properties','release-ledger.json','.github/scripts/p05_signing.py','.github/scripts/p05_publish.py','.github/p05/visual-approval.json','.github/workflows/p05-dashboard-settings-history.yml']:
   with self.subTest(path=p),self.assertRaises(ValueError):delta([p])
  with self.assertRaises(ValueError):delta([])
 def test_navigation_requires_each_actual_destination_before_the_next_back(self):
  text=Path('.github/scripts/p05_release_runtime_smoke.py').read_text();start=text.index("tap_text('Pace (min/km)')");end=text.index('validate_live_metrics(metric)',start);block=text[start:end]
  self.assertLess(block.index("wait_text('Preferred units')"),block.index("wait_text('Theme and appearance')"));self.assertLess(block.index("wait_text('Theme and appearance')"),block.index("wait_text('Recording')"))
  self.assertIn("if next((n.text",block);self.assertEqual(64,len(APK_SHA))
