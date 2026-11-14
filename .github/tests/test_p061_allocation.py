import copy,json,os,sys,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
import release_policy,p061_version,p06_version

class P061AllocationTests(unittest.TestCase):
 def setUp(self):self.ledger=json.loads(Path('release-ledger.json').read_text())
 def test_p061_reservation_and_both_high_water_records_agree(self):
  self.assertEqual(6779,p061_version.allocated_code(self.ledger))
  self.assertEqual(6779,self.ledger['codeHighWater']);self.assertEqual(1,self.ledger['seriesHighWater']['v0.1.0-p0.6.'])
  self.assertEqual(6778,p06_version.allocated_code(self.ledger))
 def test_bad_code_or_series_high_water_is_rejected(self):
  previous=Path.cwd()
  try:
   with tempfile.TemporaryDirectory() as d:
    os.chdir(d)
    for field,value in [('codeHighWater',6778),('seriesHighWater',{})]:
     ledger=copy.deepcopy(self.ledger);ledger[field]=value;Path('release-ledger.json').write_text(json.dumps(ledger))
     with self.assertRaises(SystemExit):release_policy.ledger_history()
  finally:os.chdir(previous)
 def test_old_p06_source_conflict_remains_protected(self):
  class API:
   base='api'
   def request(self,url,binary=False):
    if binary:return json.dumps({'commit':'af6e90eabee20ada79ba942584ab7eb8c08660e3','versionName':p06_version.VERSION,'versionCode':6778})
    return [{'tag_name':p06_version.TAG,'draft':False}]
   def assets(self,r):return [{'name':'build-info.json','url':'asset'}]
  with self.assertRaisesRegex(ValueError,'another source'):
   p06_version.remote_history(API(),'c7112514565c6b10dac7131b97432328ec67ef1d',6778)
  p061_version.remote_history(API(),'b'*40,6779)
