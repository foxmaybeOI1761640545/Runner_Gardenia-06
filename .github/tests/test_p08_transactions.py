import copy,hashlib,sys,unittest,uuid
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from p08_predecessor import identity,safe_path
from p08_transactions import CASES,validate_case
from p08_version import BRANCH
class P08PredecessorAndProcessEvidenceTests(unittest.TestCase):
 def receipt(self):return {'phase':'A','source':'a'*40,'runId':12,'artifactId':34,'artifactName':'P08-phase-A-12-1','artifactBytes':567,'artifactSha256':'b'*64,'stageAccepted':True,'rawJUnitVerified':True}
 def run_info(self):return {'id':12,'head_sha':'a'*40,'head_branch':BRANCH,'event':'push','path':'.github/workflows/p08-stage.yml','status':'completed','conclusion':'success'}
 def artifact(self):return {'id':34,'name':'P08-phase-A-12-1','size_in_bytes':567,'digest':'sha256:'+'b'*64,'expired':False,'workflow_run':{'id':12,'head_sha':'a'*40}}
 def test_exact_successful_predecessor_identity(self):identity(self.receipt(),'A',self.run_info(),self.artifact())
 def test_unaccepted_receipt_rejected(self):
  for k,v in [('phase','B'),('stageAccepted',False),('rawJUnitVerified',False)]:
   r=self.receipt();r[k]=v
   with self.subTest(k=k),self.assertRaises(ValueError):identity(r,'A',self.run_info(),self.artifact())
 def test_wrong_source_branch_trigger_workflow_or_conclusion_rejected(self):
  for k,v in [('head_sha','c'*40),('head_branch','main'),('event','pull_request'),('path','.github/workflows/android-build.yml'),('status','in_progress'),('conclusion','failure'),('id',13)]:
   r=self.run_info();r[k]=v
   with self.subTest(k=k),self.assertRaises(ValueError):identity(self.receipt(),'A',r,self.artifact())
 def test_wrong_expired_or_replaced_artifact_rejected(self):
  for k,v in [('id',35),('name','other'),('size_in_bytes',568),('digest','sha256:'+'c'*64),('expired',True),('workflow_run',{'id':13,'head_sha':'a'*40})]:
   a=self.artifact();a[k]=v
   with self.subTest(k=k),self.assertRaises(ValueError):identity(self.receipt(),'A',self.run_info(),a)
 def test_evidence_archive_path_cannot_escape(self):
  for path in ('../outside','/absolute','build/../../outside','build\\outside'):
   with self.subTest(path=path),self.assertRaises(ValueError):safe_path(path)
  self.assertEqual('build/reports/raw.xml',str(safe_path('build/reports/raw.xml')))
 def proof(self,name):
  committed=name.endswith('Committed') or name=='addGarbage';numbers=[1,2,3] if name.startswith('add') and committed else [1,2] if name.startswith('delete') and not committed else [1]
  return {'source':'a'*40,'case':name,'checkpoint':CASES[name],'api':35,'passed':True,'persistentDirectory':'files/route-library','producerPid':111,'recoveryPid':222,'committed':committed,'ids':[str(uuid.UUID(int=(8<<64)+n)) for n in numbers],'oldRouteSha256':'f'*64}
 def test_all_eight_transaction_outcomes_are_atomic(self):
  for name,checkpoint in CASES.items():validate_case(self.proof(name),name,checkpoint,'a'*40)
 def test_process_evidence_rejects_stale_or_non_native_proof(self):
  for k,v in [('source','b'*40),('case','other'),('checkpoint','STAGED'),('api',34),('passed',False),('persistentDirectory','cache/routes')]:
   p=self.proof('addPromoted');p[k]=v
   with self.subTest(k=k),self.assertRaises(ValueError):validate_case(p,'addPromoted','PROMOTED_0','a'*40)
 def test_same_or_missing_process_ids_rejected(self):
  for k,v in [('producerPid',222),('producerPid',0),('recoveryPid',None),('recoveryPid','222')]:
   p=self.proof('addPromoted');p[k]=v
   with self.subTest(k=k,v=v),self.assertRaises(ValueError):validate_case(p,'addPromoted','PROMOTED_0','a'*40)
 def test_partial_import_and_wrong_commit_state_rejected(self):
  for k,v in [('ids',self.proof('addPrepared')['ids']),('committed',False),('oldRouteSha256','invalid')]:
   p=self.proof('addCommitted');p[k]=v
   with self.subTest(k=k),self.assertRaises(ValueError):validate_case(p,'addCommitted','INDEX_COMMITTED','a'*40)
 def test_deleted_route_cannot_resurrect_in_process_receipt(self):
  p=self.proof('deleteCommitted');p['ids']=self.proof('deleteRetired')['ids']
  with self.assertRaises(ValueError):validate_case(p,'deleteCommitted','INDEX_COMMITTED','a'*40)
