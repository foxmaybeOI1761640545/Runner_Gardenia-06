"""Independent P08 phase gates. Never signs or publishes before complete phase F acceptance."""
import hashlib,json,os,re,shutil,subprocess,sys,tempfile,unittest
from pathlib import Path
from check_test_results import inspect
from p08_version import BASELINE,BRANCH,VERSION,allocation,check
from p08_predecessor import verify as verify_predecessor
from p08_transactions import CASES,validate_case
OUT=Path('build/reports/p08-stage')
PARSER='de.dennisguse.opentracks.p0.routes.P08GpxParserPlatformTest'
CORE='src/main/java/de/dennisguse/opentracks/p0/routes/'
def git(*args):return subprocess.check_output(['git',*args],text=True).strip()
def save(name,value):OUT.mkdir(parents=True,exist_ok=True);(OUT/name).write_text(json.dumps(value,indent=2)+'\n')
def trusted():
 if os.environ.get('GITHUB_REPOSITORY')!='foxmaybeOI1761640545/Runner_Gardenia-06' or os.environ.get('GITHUB_EVENT_NAME')!='push' or os.environ.get('GITHUB_REF')!='refs/heads/'+BRANCH:raise ValueError('Exact P08 branch push required')
def preserve():
 remote=dict(row.split('\t')[::-1] for row in git('ls-remote','--heads','--tags','origin').splitlines())
 for line in Path('docs/p0-8-0/protected-refs.txt').read_text().splitlines():
  sha,ref=line.split('\t')
  if remote.get(ref)!=sha:raise ValueError('Protected historical ref changed: '+ref)
 paths=git('ls-tree','-r','--name-only',BASELINE,'--','src/test','src/androidTest','.github/p07','.github/p071').splitlines()
 for path in paths:
  if Path(path).read_bytes()!=subprocess.check_output(['git','show',BASELINE+':'+path]):raise ValueError('Original test/phase/receipt modified: '+path)
 locked=['src/main/java/de/dennisguse/opentracks/'+p for p in ('services/','sensors/','data/','io/file/','p0/gps/','p0/ble/','p0/recording/','p0/data/','p0/export/')]
 for path in git('diff','--name-only',BASELINE,'HEAD','--','src/main').splitlines():
  if any(path.startswith(prefix) for prefix in locked) or path.endswith('/P0Navigation.java'):raise ValueError('Protected business/GPS/BLE/navigation code changed: '+path)
 phase=json.loads(Path('.github/p08/phase.json').read_text())
 if phase['phase'] in ('A','B'):
  for path in git('diff','--name-only',BASELINE,'HEAD','--','src/main').splitlines():
   if not path.startswith(CORE):raise ValueError('Data phase changed existing production/UI: '+path)
  for path in Path(CORE).rglob('*.java'):
   if re.search(r'^import (android\.|androidx\.|de\.dennisguse\.opentracks\.(data|services|sensors|p0\.(gps|ble|recording|data))\.)',path.read_text(),re.M):raise ValueError('Route data depends on Android/history/GPS business: '+str(path))
def prepare():
 trusted();source=git('rev-parse','HEAD')
 if source!=os.environ['GITHUB_SHA']:raise ValueError('Wrong source checkout')
 subprocess.run(['git','merge-base','--is-ancestor',BASELINE,'HEAD'],check=True);preserve()
 phase=json.loads(Path('.github/p08/phase.json').read_text())
 if phase['stage']!=4 or phase['phase'] not in 'ABCDEF' or len(phase['phase'])!=1 or phase['baselineDeliveryHead']!=BASELINE:raise ValueError('Wrong independent phase4 configuration')
 predecessor=verify_predecessor(phase['phase'])
 plan={'predecessor':predecessor,'commit':source,**phase,'branch':BRANCH,'runId':os.environ['GITHUB_RUN_ID'],'versionName':VERSION,'versionCode':check(source)}
 Path('release-plan.json').write_text(json.dumps(plan,indent=2)+'\n')
 with open(os.environ['GITHUB_OUTPUT'],'a') as f:f.write('phase='+phase['phase']+'\n')
def helpers():
 suite=unittest.defaultTestLoader.discover('.github/tests');result=unittest.TextTestRunner(verbosity=2).run(suite)
 value={'source':os.environ['GITHUB_SHA'],'runId':os.environ['GITHUB_RUN_ID'],'tests':result.testsRun,'executed':result.testsRun-len(result.skipped),'passed':result.testsRun>0 and result.wasSuccessful() and not result.skipped,'failures':len(result.failures),'errors':len(result.errors),'skipped':len(result.skipped)}
 save('helpers.json',value)
 if not value['passed']:raise SystemExit('Python helper regressions failed')
def gate():
 result={'passed':False}
 try:
  plan=json.loads(Path('release-plan.json').read_text());assert plan['commit']==os.environ['GITHUB_SHA'];result['plan']=plan
  result['helpers']=json.loads((OUT/'helpers.json').read_text());result['jvm']=inspect(Path('build/test-results/testReproducibleDebugUnitTest'),Path('src/test/java'),False)
  with tempfile.TemporaryDirectory() as temp:
   subset=Path(temp);shutil.copy2('src/androidTest/java/de/dennisguse/opentracks/p0/routes/P08GpxParserPlatformTest.java',subset/'P08GpxParserPlatformTest.java')
   if plan['phase']!='A':shutil.copy2('src/androidTest/java/de/dennisguse/opentracks/p0/routes/P08RouteRepositoryPlatformTest.java',subset/'P08RouteRepositoryPlatformTest.java')
   result['androidFocused']=inspect(Path('build/outputs/androidTest-results'),subset,False)
  assert result['helpers']['passed'] and result['helpers']['source']==plan['commit'] and result['helpers']['tests']>=165 and not result['helpers']['skipped']
  assert result['jvm']['passed'] and result['jvm']['tests']>=(210 if plan['phase']=='A' else 265) and not result['jvm']['skipped']
  assert result['androidFocused']['passed'] and result['androidFocused']['tests']==(8 if plan['phase']=='A' else 14) and not result['androidFocused']['skipped']
  import xml.etree.ElementTree as ET
  parser=[]
  for file in Path('build/test-results/testReproducibleDebugUnitTest').glob('TEST-*.xml'):
   parser.extend(c.get('name') for c in ET.parse(file).getroot().iter('testcase') if c.get('classname')=='de.dennisguse.opentracks.p0.routes.GpxRouteImporterTest')
  expected=set(re.findall(r'@Test\s+public void (\w+)\(',Path('src/test/java/de/dennisguse/opentracks/p0/routes/GpxRouteImporterTest.java').read_text()))
  assert len(parser)==43 and set(parser)==expected;result['parserJvmMethods']=parser
  roots=list(Path('build/outputs').glob('**/p08-parser'));assert len(roots)==1
  profiles={p.stem:json.loads(p.read_text()) for p in roots[0].glob('*.json')};assert set(profiles)=={'points20000','bytes16MiB'}
  assert all(p['source']==plan['commit'] and p['api']==35 and p['elapsedMillis']>0 for p in profiles.values());result['androidBudgetMeasurements']=profiles
  if plan['phase']!='A':
   proof=json.loads(Path('build/reports/p08-transactions/result.json').read_text())
   assert proof['passed'] and proof['source']==plan['commit'] and proof['api']==35 and proof['timezone']=='Asia/Singapore' and set(proof['cases'])==set(CASES)
   for name,checkpoint in CASES.items():
    validate_case(proof['cases'][name],name,checkpoint,plan['commit']);assert proof['cases'][name]['forceStopped'] and proof['cases'][name]['producerOutcome']=='INTENTIONALLY_INTERRUPTED_NOT_A_PASSED_JUNIT_TEST'
   result['actualProcessDeath']=proof
   folders=list(Path('build/outputs').glob('**/p08-repository'));assert len(folders)==1
   isolation=json.loads((folders[0]/'isolation.json').read_text());assert isolation['passed'] and isolation['source']==plan['commit'] and isolation['api']==35 and isolation['schema']==43 and isolation['before']==isolation['after']
   for table,minimum in [('TRACKS',1),('TRACKPOINTS',2),('HEART_RATE_SAMPLES',3)]:assert isolation['before'][table]['rows']>=minimum and re.fullmatch('[0-9a-f]{64}',isolation['before'][table]['sha256'])
   assert all(re.fullmatch('[0-9a-f]{64}',isolation[k]) for k in ('preferencesSha256','offlineMapSentinelSha256'));result['isolation']=isolation
   assert plan['predecessor']['phase']=='A' and plan['predecessor']['historicalRawGateRerun']
  result['scope']='Data/GPX and private route repository only: complete JVM/Python, focused actual API35; complete legacy API/UI/signed upgrade/publication still required in phaseF'
  result['passed']=True
 except Exception as error:result['error']=repr(error)
 save('gate.json',result);print(json.dumps(result,indent=2))
 if not result['passed']:raise SystemExit('P08 phase acceptance blocked')
if __name__=='__main__':{'prepare':prepare,'helpers':helpers,'gate':gate}[sys.argv[1]]()
