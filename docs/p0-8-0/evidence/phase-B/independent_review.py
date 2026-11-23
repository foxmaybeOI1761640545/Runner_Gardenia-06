import hashlib,json,re,sys,xml.etree.ElementTree as ET
from pathlib import Path
sys.path.insert(0,'.github/scripts')
from p08_transactions import CASES,validate_case
from check_test_results import inspect
b=Path('work/run45121619436');a=Path('work/run45116960695');source='52aab2418579bd645a0aa2ac86d5cc5e4b3f3839'
def identities(root):
 result={}
 for f in root.rglob('TEST-*.xml'):
  for case in ET.parse(f).getroot().iter('testcase'):
   key=(case.get('classname'),case.get('name'));assert key not in result;result[key]=case
 return result
jvm=identities(b/'build/test-results/testReproducibleDebugUnitTest');old=identities(a/'build/test-results/testReproducibleDebugUnitTest');assert len(old)==210 and old.keys()<=jvm.keys()
for cls,n in [('RouteRepositoryTest',34),('RouteCodecTest',21)]:
 expected=set(re.findall(r'@Test\s+public void (\w+)\(',Path('src/test/java/de/dennisguse/opentracks/p0/routes/'+cls+'.java').read_text()));actual={name for owner,name in jvm if owner=='de.dennisguse.opentracks.p0.routes.'+cls};assert actual==expected and len(actual)==n
assert len(jvm)==265
native=identities(b/'build/outputs/androidTest-results');prior_native=identities(a/'build/outputs/androidTest-results');assert len(native)==14 and len(prior_native)==8 and prior_native.keys()<=native.keys()
proof=json.load(open(b/'build/reports/p08-transactions/result.json'));assert set(proof['cases'])==set(CASES)
for name,checkpoint in CASES.items():
 p=proof['cases'][name];validate_case(p,name,checkpoint,source)
 producer=(b/'build/reports/p08-transactions'/f'{name}-producer.txt').read_text();verify=(b/'build/reports/p08-transactions'/f'{name}-verify.txt').read_text()
 assert 'Process crashed.' in producer and 'INSTRUMENTATION_CODE: 0' in producer and 'p08Verified=true' not in producer
 assert 'INSTRUMENTATION_RESULT: p08Verified=true' in verify and f'INSTRUMENTATION_RESULT: case={name}' in verify and f'INSTRUMENTATION_RESULT: source={source}' in verify and 'INSTRUMENTATION_CODE: -1' in verify
 ready=json.load(open(b/'build/reports/p08-transactions'/f'{name}-ready.json'));assert ready['producerPid']==p['producerPid'] and ready['oldRouteSha256']==p['oldRouteSha256']
iso=json.load(open(next(b.glob('build/outputs/**/p08-repository/isolation.json'))));assert iso['source']==source and iso['before']==iso['after'] and iso['schema']==43
helpers=json.load(open(b/'build/reports/p08-stage/helpers.json'));assert helpers['tests']==175 and helpers['passed'] and helpers['source']==source and not helpers['skipped']
gate=json.load(open(b/'build/reports/p08-stage/gate.json'));assert gate['passed'] and gate['plan']['commit']==source and gate['plan']['phase']=='B'
result={'source':source,'runId':45121619436,'artifactId':14794331155,'artifactSha256':'53504f488b88deb097bc1cdfdb75a95627a086338db1ee6fd6a5e33eea2a1dda','artifactBytes':1105303,'passed':True,'all210AcceptedPredecessorJvmCasesPreserved':True,'all8AcceptedPredecessorNativeCasesPreserved':True,'new55JvmMethodIdentitiesChecked':True,'nativeTransactions':{'scenarios':8,'externallyTerminatedProducers':8,'distinctProcessRecoveryVerifications':8,'intentionallyInterruptedProducersNotCountedAsPassedJUnit':True},'isolation':iso,'tests':{k:{'tests':n,'executed':n,'passed':n,'failures':0,'errors':0,'skipped':0} for k,n in [('python',175),('jvm',265),('androidFocused',14)]},'visualReview':'NOT_APPLICABLE_DATA_ONLY','fullLegacyApi35AndSignedUpgrade':'Required in phaseF; not claimed by this data gate'}
Path('work/phase-B-independent-review.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k!='isolation'},indent=2))
