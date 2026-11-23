import copy,json,sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from p08_predecessor import identity,visual_matches
from p08_version import BRANCH
class P08UiPixelAdmissionTests(unittest.TestCase):
 def receipt(self):return json.loads(Path('docs/p0-8-0/evidence/phase-C/acceptance.json').read_text())
 def raw(self):return json.loads(Path('docs/p0-8-0/evidence/phase-C/build/reports/p08-stage/gate.json').read_text())
 def metadata(self,r):
  return ({'id':r['runId'],'head_sha':r['source'],'head_branch':BRANCH,'event':'push','path':'.github/workflows/p08-stage.yml','status':'completed','conclusion':'success'}, {'id':r['artifactId'],'name':r['artifactName'],'size_in_bytes':r['artifactBytes'],'digest':'sha256:'+r['artifactSha256'],'expired':False,'workflow_run':{'id':r['runId'],'head_sha':r['source']}})
 def test_actual_eight_native_reviews_match_immutable_source_gate(self):
  r=self.receipt();identity(r,'C',*self.metadata(r));visual_matches(r,self.raw());self.assertEqual(8,len(r['nativeImagesReviewed']))
 def test_missing_or_pending_actual_review_blocks_next_ui_phase(self):
  for value in (None,'PENDING_ACTUAL_REVIEW','NOT_APPLICABLE_DATA_ONLY'):
   r=self.receipt();r['visualReview']=value
   with self.subTest(value=value),self.assertRaises(ValueError):identity(r,'C',*self.metadata(r))
 def test_invalid_native_flag_or_hash_blocks_admission(self):
  for key,value in [('native',False),('sha256','not-a-hash')]:
   r=self.receipt();r['nativeImagesReviewed'][0][key]=value
   with self.subTest(key=key),self.assertRaises(ValueError):identity(r,'C',*self.metadata(r))
 def test_replaced_pixels_cannot_reuse_old_review(self):
  r=self.receipt();r['nativeImagesReviewed'][0]['sha256']='f'*64
  with self.assertRaises(ValueError):visual_matches(r,self.raw())
 def test_missing_duplicated_or_extra_reviewed_images_fail(self):
  for change in ('missing','duplicate','extra'):
   r=self.receipt();rows=r['nativeImagesReviewed']
   if change=='missing':rows.pop()
   elif change=='duplicate':rows.append(copy.deepcopy(rows[0]))
   else:rows.append({**rows[0],'file':'unrelated.png'})
   with self.subTest(change=change),self.assertRaises(ValueError):visual_matches(r,self.raw())
 def test_zero_native_capture_evidence_cannot_authorize_next_phase(self):
  with self.assertRaises(ValueError):visual_matches(self.receipt(),{})
