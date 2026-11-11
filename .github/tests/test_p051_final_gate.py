import copy,json,os,sys,unittest,xml.etree.ElementTree as ET
from pathlib import Path
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from p051_signing import validate_gate
from p051_release import activation_paths,publication_paths,artifact
from p051_version import allocated_code,VERSION
from p051_release_theme_ui import page_matches

class FinalP05GateTests(unittest.TestCase):
 def valid(self):
  source='a'*40
  plan=dict(commit=source,stage=5,fullRegression=True,classes=[],signed=False,publishing=False,versionName=VERSION)
  report=dict(plan=plan,passed=True,shareReceipts=6,historyScreenshots=32,settingsScreenshots=16,metricScreenshots=8,legacySkippedTest=dict(method='recording_stopGPS_noop',countedAsPassed=False,unchangedFromP04=True))
  for name in ('android','unit','helpers'):report[name]=dict(passed=True,tests=5,executed=4 if name=='android' else 5,failures=0,errors=0,skipped=1 if name=='android' else 0)
  for name in ('processDurability','metricProcessDurability','batchProcessDurability','gpxSchema','offline'):report[name]=dict(passed=True,commit=source)
  report['gpsNativeEvidence']=dict(passed=True,sourceCommit=source,screenshots=[{}]*6)
  return plan,report,source
 def test_complete_actual_stage5_gate_has_own_identity(self):
  validate_gate(*self.valid())
  for change in ({'stage':10},{'classes':['narrowed.Test']},{'fullRegression':False},{'signed':True},{'versionName':'v0.1.0-p0.4'}):
   plan,report,source=self.valid();plan.update(change)
   with self.subTest(change=change),self.assertRaises(ValueError):validate_gate(plan,report,source)
 def test_missing_execution_extra_skips_and_foreign_process_proof_fail(self):
  for scope in ('android','unit','helpers'):
   for field,value in [('executed',0),('failures',1),('errors',1),('skipped',2),('tests',99)]:
    plan,report,source=self.valid();report[scope][field]=value
    with self.subTest(scope=scope,field=field),self.assertRaises(ValueError):validate_gate(plan,report,source)
  for scope in ('processDurability','metricProcessDurability','batchProcessDurability','gpxSchema','offline'):
   plan,report,source=self.valid();report[scope]['commit']='b'*40
   with self.subTest(scope=scope),self.assertRaises(ValueError):validate_gate(plan,report,source)
 def test_legacy_ignore_cannot_be_counted_as_passed_or_hidden(self):
  for field,value in [('method','new_skipped_method'),('countedAsPassed',True),('unchangedFromP04',False)]:
   plan,report,source=self.valid();report['legacySkippedTest'][field]=value
   with self.assertRaises(ValueError):validate_gate(plan,report,source)
 def test_sign_and_publish_activations_cannot_modify_code_or_each_other(self):
  for validator,allowed,other in [(activation_paths,'.github/p051/visual-approval.json','.github/p051/publication-approval.json'),(publication_paths,'.github/p051/publication-approval.json','.github/p051/visual-approval.json')]:
   validator([allowed,'docs/p0-5/05-final-delivery.md'])
   for path in ['src/main/Activity.java','src/androidTest/Test.java','version.properties','release-ledger.json','.github/workflows/p05-dashboard-settings-history.yml','.github/scripts/p051_signing.py',other]:
    with self.subTest(path=path),self.assertRaises(ValueError):validator([path])
   with self.assertRaises(ValueError):validator([])
 def test_artifact_must_match_exact_workflow_run_digest_and_source(self):
  meta=dict(expired=False,name='P05-reports-1',workflow_run=dict(id=1,head_sha='a'*40),digest='sha256:'+'b'*64)
  class API:
   base='https://test.invalid'
   def request(self,url):return meta
  artifact(API(),1,'P05-reports-1','1','a'*40,'b'*64)
  for field,value in [('expired',True),('name','foreign'),('digest','sha256:'+'c'*64),('workflow_run',dict(id=2,head_sha='a'*40))]:
   with patch.dict(meta,{field:value}),self.assertRaises(ValueError):artifact(API(),1,'P05-reports-1','1','a'*40,'b'*64)
 def test_new_reservation_keeps_older_allocations_valid(self):
  from p04_version import allocated_code as p04
  from p03_version import allocated_code as p03
  data=json.loads(Path('release-ledger.json').read_text());self.assertEqual(6777,allocated_code(data));self.assertEqual(6775,p04(data));self.assertEqual(6774,p03(data))
  bad=copy.deepcopy(data);bad['codeHighWater']+=1
  with self.assertRaises(ValueError):allocated_code(bad)
 def test_same_settings_activity_and_dialog_require_distinct_native_content(self):
  def tree(ids,texts):
   root=ET.Element('hierarchy')
   for identity in ids:ET.SubElement(root,'node',{'resource-id':'com.fox.runner:id/'+identity})
   for text in texts:ET.SubElement(root,'node',{'text':text})
   return root
  hub=tree(['gardenia_settings_toolbar'],['Theme and appearance','Workout and display'])
  motion=tree(['gardenia_settings_toolbar'],['Preferred units','Preferred rate','Restore default metric layout'])
  self.assertTrue(page_matches(hub,'hub'));self.assertFalse(page_matches(hub,'motion'));self.assertTrue(page_matches(motion,'motion'));self.assertFalse(page_matches(motion,'hub'))
  scrolled=tree(['gardenia_settings_toolbar'],['History and data','Privacy and offline','App information']);self.assertTrue(page_matches(scrolled,'hub'))
  category=tree(['gardenia_settings_toolbar'],['Workout and display','Preferred units','Preferred rate','Restore default metric layout']);self.assertFalse(page_matches(category,'hub'))
  share=tree(['alertTitle'],['Share selected records (2)','Separate GPX files']);delete=tree(['alertTitle'],['Permanently delete selected records (2)?'])
  self.assertTrue(page_matches(share,'share'));self.assertFalse(page_matches(share,'delete'));self.assertTrue(page_matches(delete,'delete'));self.assertFalse(page_matches(delete,'share'))
