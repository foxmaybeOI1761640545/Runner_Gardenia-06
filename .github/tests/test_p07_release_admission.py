import copy,os,sys,unittest
from pathlib import Path
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
import p07_release as release
import p07_version as version

class P07AdmissionTests(unittest.TestCase):
 def test_live_reservation_appends_without_rewriting_old_allocations(self):
  row=version.allocation();self.assertEqual(6780,row['versionCode']);self.assertEqual(version.VERSION,row['versionName']);self.assertIsNone(row['commit'])
 def test_only_exact_repository_clean_branch_push_is_eligible(self):
  good={'GITHUB_REPOSITORY':version.REPO,'GITHUB_EVENT_NAME':'push','GITHUB_REF':'refs/heads/'+version.BRANCH}
  with patch.dict(os.environ,good,clear=True):release.trusted()
  for field,value in [('GITHUB_REPOSITORY','other/repo'),('GITHUB_EVENT_NAME','pull_request'),('GITHUB_REF','refs/heads/main'),('GITHUB_REF','refs/heads/feature/p0-7-to-p1-0-offline-navigation')]:
   with self.subTest(field=field,value=value),patch.dict(os.environ,{**good,field:value},clear=True),self.assertRaises(ValueError):release.trusted()
 def test_approval_commits_cannot_change_product_tests_version_or_workflow(self):
  for approval in (release.APPROVAL,release.PUBLICATION):
   release.allowed([str(approval),'docs/p0-7-p1-0/review.md'],approval)
   for path in ('src/main/App.java','src/androidTest/Test.java','version.properties','release-ledger.json','.github/workflows/p07-release.yml',str(release.PUBLICATION if approval==release.APPROVAL else release.APPROVAL)):
    with self.subTest(path=path),self.assertRaises(ValueError):release.allowed([path],approval)
   with self.assertRaises(ValueError):release.allowed([],approval)
 def test_artifact_identity_rejects_expired_stale_wrong_run_and_wrong_digest(self):
  row={'expired':False,'name':'reports','workflow_run':{'id':42,'head_sha':'a'*40},'digest':'sha256:'+'b'*64}
  class API:
   base='https://api.github.com/repos/example/repo'
   def request(self,url):return self.row
  api=API();api.row=row;release.artifact(api,1,'reports',42,'a'*40,'b'*64)
  for key,value in [('expired',True),('name','other'),('workflow_run',{'id':43,'head_sha':'a'*40}),('workflow_run',{'id':42,'head_sha':'c'*40}),('digest','sha256:'+'c'*64)]:
   api.row={**row,key:value}
   with self.subTest(key=key),self.assertRaises(ValueError):release.artifact(api,1,'reports',42,'a'*40,'b'*64)
 def test_full_workflow_must_really_succeed_and_execute_required_job(self):
  class API:
   base='https://api.github.com/repos/example/repo'
   run={'head_sha':'a'*40,'head_branch':version.BRANCH,'event':'push','conclusion':'success','path':'.github/workflows/p07-clean-navigation.yml'}
   jobs=[{'name':'stage-validation','conclusion':'success'}]
   def request(self,url):return {'jobs':self.jobs} if '/jobs?' in url else self.run
  api=API();release.accepted_run(api,42,'a'*40,api.run['path'],'stage-validation')
  for key,value in [('conclusion','failure'),('event','pull_request'),('head_sha','c'*40),('path','.github/workflows/other.yml')]:
   api.run={**API.run,key:value}
   with self.subTest(key=key),self.assertRaises(ValueError):release.accepted_run(api,42,'a'*40,API.run['path'],'stage-validation')
  api.run=API.run;api.jobs=[{'name':'stage-validation','conclusion':'skipped'}]
  with self.assertRaises(ValueError):release.accepted_run(api,42,'a'*40,API.run['path'],'stage-validation')
