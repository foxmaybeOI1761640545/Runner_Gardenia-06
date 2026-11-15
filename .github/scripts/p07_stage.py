"""Clean branch validation only. No credentials, signing or publication capability."""
import json,os,subprocess,sys,unittest,shutil,tempfile,hashlib
from pathlib import Path
from check_test_results import inspect

OUT=Path('build/reports/p07-stage')
def save(name,value):
 OUT.mkdir(parents=True,exist_ok=True);(OUT/name).write_text(json.dumps(value,indent=2)+'\n')
def prepare():
 expected='feature/p0-7-to-p1-0-offline-navigation-clean'
 ref=os.environ['GITHUB_REF'];head=os.environ.get('GITHUB_HEAD_REF')
 if not (ref=='refs/heads/'+expected or (head==expected and os.environ.get('GITHUB_BASE_REF')=='feature/p0-6-map-real-validation')):raise ValueError('Unexpected validation ref')
 actual=dict(row.split('\t')[::-1] for row in subprocess.check_output(['git','ls-remote','--heads','--tags','origin'],text=True).splitlines())
 for row in Path('docs/p0-7-p1-0/protected-refs.txt').read_text().splitlines():
  sha,ref=row.split('\t')
  if actual.get(ref)!=sha:raise ValueError('Historical ref changed: '+ref)
 source=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
 Path('release-plan.json').write_text(json.dumps({'commit':source,'stage':1,'fullRegression':True,'branch':expected,'signed':False,'publishing':False,'runId':os.environ['GITHUB_RUN_ID']},indent=2)+'\n')
def helpers():
 suite=unittest.defaultTestLoader.discover('.github/tests');r=unittest.TextTestRunner(verbosity=2).run(suite)
 proof={'tests':r.testsRun,'executed':r.testsRun-len(r.skipped),'failures':len(r.failures),'errors':len(r.errors),'skipped':len(r.skipped),'passed':r.wasSuccessful() and r.testsRun>0 and not r.skipped}
 save('helpers.json',proof)
 if not proof['passed']:raise SystemExit('Helper regression failed')
def focused():
 plan=json.loads(Path('release-plan.json').read_text())
 if plan['commit']!=os.environ['GITHUB_SHA']:raise ValueError('Wrong focused source')
 with tempfile.TemporaryDirectory() as directory:
  source=Path(directory);shutil.copy2('src/androidTest/java/de/dennisguse/opentracks/p0/map/P07HomeMapTest.java',source/'P07HomeMapTest.java')
  report=inspect(Path('build/outputs/androidTest-results'),source,False)
 if not report['passed'] or report['tests']!=2 or report['skipped']!=0:raise ValueError('Focused home tests incomplete or failed')
 roots=list(Path('build/outputs').glob('**/p0-ui'))
 if len(roots)!=1:raise ValueError('Missing unambiguous native output')
 pictures=sorted(roots[0].glob('p07-home-*.png'))
 if len(pictures)!=35:raise ValueError('Missing actual home matrix')
 target=Path('build/reports/p07-focused');target.mkdir(parents=True,exist_ok=True)
 rows=[]
 for picture in pictures:
  data=picture.read_bytes()
  if not data.startswith(b'\x89PNG\r\n\x1a\n'):raise ValueError('Invalid native capture')
  shutil.copy2(picture,target/picture.name);rows.append({'file':picture.name,'sha256':hashlib.sha256(data).hexdigest()})
 shutil.copytree('build/outputs/androidTest-results',target/'raw-android',dirs_exist_ok=True)
 (target/'focused.json').write_text(json.dumps({'source':plan['commit'],'run':plan['runId'],'scope':'P07HomeMapTest only; full regression still required','tests':report,'screenshots':rows,'visualReviewRequired':True,'stageAccepted':False},indent=2)+'\n')
def gate():
 proof={'passed':False}
 try:
  proof['plan']=json.loads(Path('release-plan.json').read_text())
  proof['helpers']=json.loads((OUT/'helpers.json').read_text())
  proof['jvm']=inspect(Path('build/test-results/testReproducibleDebugUnitTest'),Path('src/test/java'),False)
  proof['android']=inspect(Path('build/outputs/androidTest-results'),Path('src/androidTest/java'))
  proof['passed']=all(proof[k]['passed'] for k in ['helpers','jvm','android']) and proof['android']['skipped']==1 and proof['jvm']['skipped']==0
 except Exception as error:proof['error']=str(error)
 save('gate.json',proof)
 print(json.dumps(proof,indent=2))
 if not proof['passed']:raise SystemExit('Stage acceptance blocked')
if __name__=='__main__':{'prepare':prepare,'helpers':helpers,'focused':focused,'gate':gate}[sys.argv[1]]()
