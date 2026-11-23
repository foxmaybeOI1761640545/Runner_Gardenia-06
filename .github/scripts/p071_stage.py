"""Clean branch validation only. No credentials, signing or publication capability."""
import json,os,subprocess,sys,unittest,shutil,tempfile,hashlib,xml.etree.ElementTree as ET
from pathlib import Path
from check_test_results import inspect

OUT=Path('build/reports/p071-stage')
def save(name,value):
 OUT.mkdir(parents=True,exist_ok=True);(OUT/name).write_text(json.dumps(value,indent=2)+'\n')
def prepare():
 expected='fix/p0-7-1-navigation-backstack'
 ref=os.environ['GITHUB_REF'];head=os.environ.get('GITHUB_HEAD_REF')
 if not (ref=='refs/heads/'+expected or (head==expected and os.environ.get('GITHUB_BASE_REF')=='feature/p0-6-map-real-validation')):raise ValueError('Unexpected validation ref')
 actual=dict(row.split('\t')[::-1] for row in subprocess.check_output(['git','ls-remote','--heads','--tags','origin'],text=True).splitlines())
 for row in Path('docs/p0-7-1/protected-refs.txt').read_text().splitlines():
  sha,ref=row.split('\t')
  if actual.get(ref)!=sha:raise ValueError('Historical ref changed: '+ref)
 source=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
 if source!=os.environ['GITHUB_SHA']:raise ValueError('Unexpected source checkout')
 phase=json.loads(Path('.github/p071/phase.json').read_text())
 if phase['stage']!=3 or phase['milestone']!='P0.7.1 navigation maintenance':raise ValueError('Not the maintenance gate')
 baseline=phase['baselineDeliveryHead']
 subprocess.run(['git','merge-base','--is-ancestor',baseline,'HEAD'],check=True)
 # Every pre-existing Java test remains byte-for-byte intact; additions only.
 for path in subprocess.check_output(['git','ls-tree','-r','--name-only',baseline,'--','src/test','src/androidTest'],text=True).splitlines():
  if Path(path).read_bytes()!=subprocess.check_output(['git','show',baseline+':'+path]):raise ValueError('Original test changed: '+path)
 plan={'commit':source,'stage':phase['stage'],'milestone':'P0.7.1 navigation maintenance','fullRegression':True,'branch':expected,'signed':False,'publishing':False,'runId':os.environ['GITHUB_RUN_ID']}
 if phase['stage']==3:
  from p071_version import check,VERSION
  plan.update(versionCode=check(source),versionName=VERSION)
 Path('release-plan.json').write_text(json.dumps(plan,indent=2)+'\n')
 if os.environ.get('GITHUB_OUTPUT'):
  with open(os.environ['GITHUB_OUTPUT'],'a') as output:output.write('stage='+str(phase['stage'])+'\n')
def helpers():
 suite=unittest.defaultTestLoader.discover('.github/tests');r=unittest.TextTestRunner(verbosity=2).run(suite)
 proof={'tests':r.testsRun,'executed':r.testsRun-len(r.skipped),'failures':len(r.failures),'errors':len(r.errors),'skipped':len(r.skipped),'passed':r.wasSuccessful() and r.testsRun>0 and not r.skipped,'source':os.environ['GITHUB_SHA'],'runId':os.environ['GITHUB_RUN_ID']}
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
 target=Path('build/reports/p071-focused');target.mkdir(parents=True,exist_ok=True)
 rows=[]
 for picture in pictures:
  data=picture.read_bytes()
  if not data.startswith(b'\x89PNG\r\n\x1a\n'):raise ValueError('Invalid native capture')
  shutil.copy2(picture,target/picture.name);rows.append({'file':picture.name,'sha256':hashlib.sha256(data).hexdigest()})
 shutil.copytree('build/outputs/androidTest-results',target/'raw-android',dirs_exist_ok=True)
 (target/'focused.json').write_text(json.dumps({'source':plan['commit'],'run':plan['runId'],'scope':'P07HomeMapTest only; full regression still required','tests':report,'screenshots':rows,'visualReviewRequired':True,'stageAccepted':False},indent=2)+'\n')
GPS_FILES=['p0/map/P07ForegroundGpsTest.java','p0/map/P061GpsPreferencesTest.java','p0/map/P06GpsOwnershipTest.java','p0/P0GpsPreparationTest.java','p0/P0RecordingUiTest.java','p0/P0SensorsTest.java','p0/P05DashboardStatusNavigationTest.java']
def gps_files():
 return GPS_FILES+(['services/P07GpsHandoffTest.java','services/P07RecoveryPermissionTest.java'] if json.loads(Path('.github/p071/phase.json').read_text())['stage']==3 else [])
def gps_classes():
 print(','.join('de.dennisguse.opentracks.'+p.removesuffix('.java').replace('/','.') for p in gps_files()))
def gps_focused():
 plan=json.loads(Path('release-plan.json').read_text())
 if plan['commit']!=os.environ['GITHUB_SHA'] or plan['stage'] not in (2,3):raise ValueError('Wrong GPS focused source/stage')
 with tempfile.TemporaryDirectory() as directory:
  source=Path(directory)
  for path in gps_files():shutil.copy2(Path('src/androidTest/java/de/dennisguse/opentracks')/path,source/Path(path).name)
  report=inspect(Path('build/outputs/androidTest-results'),source,False)
 if not report['passed'] or report['tests']!=(40 if plan['stage']==2 else 47) or report['skipped']:raise ValueError('GPS/heart/owner/preferences/paused-journal regression failed or incomplete')
 target=Path('build/reports/p071-gps-focused');target.mkdir(parents=True,exist_ok=True)
 for path in Path('build/outputs/androidTest-results').rglob('TEST-*.xml'):shutil.copy2(path,target/path.name)
 (target/'focused.json').write_text(json.dumps({'source':plan['commit'],'run':plan['runId'],'tests':report,'fullRegressionRequired':True,'stageAccepted':False},indent=2)+'\n')
def gate():
 proof={'passed':False}
 try:
  proof['plan']=json.loads(Path('release-plan.json').read_text())
  proof['helpers']=json.loads((OUT/'helpers.json').read_text())
  proof['jvm']=inspect(Path('build/test-results/testReproducibleDebugUnitTest'),Path('src/test/java'),False)
  proof['android']=inspect(Path('build/outputs/androidTest-results'),Path('src/androidTest/java'))
  skips=[]
  for path in Path('build/outputs/androidTest-results').rglob('TEST-*.xml'):
   for case in ET.parse(path).getroot().iter('testcase'):
    if case.find('skipped') is not None:skips.append((case.get('classname'),case.get('name')))
  proof['historicalSkip']=skips
  proof['passed']=all(proof[k]['passed'] for k in ['helpers','jvm','android']) and proof['android']['skipped']==1 and proof['jvm']['skipped']==0 and skips==[('de.dennisguse.opentracks.services.TrackRecordingServiceStateMachineTest','recording_stopGPS_noop')]
  if proof['plan']['stage'] in (2,3):
   gps=json.loads(Path('build/reports/p071-gps-focused/focused.json').read_text());proof['gpsFocused']=gps
   permissions=json.loads(Path('build/reports/p071-permissions/result.json').read_text());proof['permissions']=permissions
   proof['passed']=proof['passed'] and gps['source']==proof['plan']['commit'] and gps['tests']['passed'] and proof['android']['tests']>=(464 if proof['plan']['stage']==2 else 489) and permissions['passed'] and permissions['source']==proof['plan']['commit']
  if proof['plan']['stage']==3:
   from p071_delivery import gate as delivery
   proof['delivery']=delivery(proof['plan']['commit']);save('delivery.json',proof['delivery'])
   recovery=permissions['phases']['recovery-denied']
   proof['passed']=proof['passed'] and recovery['p0RecoveryRejected'] and recovery['actualGpsPermission']=='denied' and recovery['actualBluetoothPermission']=='granted' and proof['helpers']['source']==proof['plan']['commit']
 except Exception as error:proof['error']=str(error);proof['passed']=False
 save('gate.json',proof)
 print(json.dumps(proof,indent=2))
 if not proof['passed']:raise SystemExit('Stage acceptance blocked')
if __name__=='__main__':{'prepare':prepare,'helpers':helpers,'focused':focused,'gps-classes':gps_classes,'gps-focused':gps_focused,'gate':gate}[sys.argv[1]]()
