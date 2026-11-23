import json,os,sys,unittest
from pathlib import Path
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
import p08_stage as stage
import p08_version as version
import p071_version
class P08AdmissionTests(unittest.TestCase):
 def test_own_allocation_is_unique_and_preserves_p071_reader(self):
  self.assertEqual(6782,version.allocation()['versionCode']);self.assertEqual(6781,p071_version.allocation()['versionCode'])
  self.assertEqual('f2d57b7440dca0350813313ad9e7ef4a12e55fa6',version.allocation()['allocationBasisCommit'])
 def test_only_exact_repo_branch_push_is_allowed(self):
  good={'GITHUB_REPOSITORY':version.REPO,'GITHUB_EVENT_NAME':'push','GITHUB_REF':'refs/heads/'+version.BRANCH}
  with patch.dict(os.environ,good,clear=True):stage.trusted()
  for field,value in [('GITHUB_REPOSITORY','other/repo'),('GITHUB_EVENT_NAME','pull_request'),('GITHUB_REF','refs/heads/main'),('GITHUB_REF','refs/heads/fix/p0-7-1-navigation-backstack')]:
   with patch.dict(os.environ,{**good,field:value},clear=True),self.assertRaises(ValueError):stage.trusted()
 def test_independent_stage_four_does_not_relabel_p07_or_p071(self):
  self.assertEqual(4,json.loads(Path('.github/p08/phase.json').read_text())['stage'])
  self.assertEqual(3,json.loads(Path('.github/p07/phase.json').read_text())['stage'])
  self.assertEqual(3,json.loads(Path('.github/p071/phase.json').read_text())['stage'])
 def test_missing_empty_and_failed_raw_junit_are_rejected(self):
  import tempfile
  from check_test_results import inspect
  with tempfile.TemporaryDirectory() as temp:
   root=Path(temp)
   with self.assertRaises(ValueError):inspect(root,Path('src/test/java'),False)
   (root/'TEST-empty.xml').write_text('<testsuite tests="0" failures="0" errors="0" skipped="0"/>')
   self.assertFalse(inspect(root,Path('src/test/java'),False)['passed'])
   (root/'TEST-empty.xml').write_text('<testsuite tests="1" failures="1" errors="0" skipped="0"><testcase classname="x" name="bad"><failure/></testcase></testsuite>')
   self.assertFalse(inspect(root,Path('src/test/java'),False)['passed'])
 def test_validation_phase_has_no_signing_or_publication_authority(self):
  p=json.loads(Path('.github/p08/phase.json').read_text());self.assertFalse(p['signed']);self.assertFalse(p['publishing']);self.assertEqual(p['phase']=='F',p['fullRegression'])
  self.assertNotIn('ANDROID_KEYSTORE_BASE64',Path('.github/workflows/p08-stage.yml').read_text())
