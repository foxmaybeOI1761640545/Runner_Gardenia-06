import hashlib,json,re,sys,xml.etree.ElementTree as ET
from pathlib import Path
sys.path.insert(0,'.github/scripts')
from p08_transactions import CASES,validate_case
from check_test_results import inspect
b=Path('work/run45123727015');a=Path('work/run45121619436');source='a4c6c1803c25d3c18a9e2158554219d3597bf1a1'
def cases(root):
 result={}
 for f in root.rglob('TEST-*.xml'):
  for c in ET.parse(f).getroot().iter('testcase'):
   key=(c.get('classname'),c.get('name'));assert key not in result;assert c.find('failure') is None and c.find('error') is None and c.find('skipped') is None;result[key]=c
 return result
jvm=cases(b/'build/test-results/testReproducibleDebugUnitTest');priorjvm=cases(a/'build/test-results/testReproducibleDebugUnitTest');assert len(jvm)==265 and jvm.keys()==priorjvm.keys()
native=cases(b/'build/outputs/androidTest-results');prior=cases(a/'build/outputs/androidTest-results');assert len(native)==24 and len(prior)==14 and prior.keys()<=native.keys()
expected=set(re.findall(r'@Test\s+public void (\w+)\(',Path('src/androidTest/java/de/dennisguse/opentracks/p0/routes/P08RouteImportUiTest.java').read_text()));actual={n for c,n in native if c=='de.dennisguse.opentracks.p0.routes.P08RouteImportUiTest'};assert len(expected)==10 and actual==expected
proof=json.load(open(b/'build/reports/p08-transactions/result.json'));assert proof['passed'] and set(proof['cases'])==set(CASES)
for name,point in CASES.items():
 validate_case(proof['cases'][name],name,point,source)
 folder=b/'build/reports/p08-transactions';producer=(folder/(name+'-producer.txt')).read_text();verify=(folder/(name+'-verify.txt')).read_text();assert 'Process crashed.' in producer and 'INSTRUMENTATION_CODE: 0' in producer and 'p08Verified=true' not in producer;assert 'INSTRUMENTATION_RESULT: p08Verified=true' in verify and 'INSTRUMENTATION_CODE: -1' in verify and 'source='+source in verify
screens=json.load(open(next(b.glob('build/outputs/**/p08-import/screenshots.json'))));assert screens['source']==source and len(screens['captures'])==8
images={f.name:f for f in b.glob('build/outputs/**/p0-ui/p08-import-*.png')};assert set(images)=={c['file'] for c in screens['captures']}
from PIL import Image
for row in screens['captures']:assert hashlib.sha256(images[row['file']].read_bytes()).hexdigest()==row['sha256'] and Image.open(images[row['file']]).size==(1080,2400)
iso=json.load(open(next(b.glob('build/outputs/**/p08-repository/isolation.json'))));assert iso['passed'] and iso['source']==source and iso['schema']==43 and iso['before']==iso['after']
h=json.load(open(b/'build/reports/p08-stage/helpers.json'));assert h['passed'] and h['source']==source and h['tests']==175 and not h['skipped'];gate=json.load(open(b/'build/reports/p08-stage/gate.json'));assert gate['passed'] and gate['plan']['commit']==source and gate['plan']['phase']=='C'
result={'phase':'C','source':source,'runId':45123727015,'artifactId':14795525179,'artifactName':'P08-phase-C-45123727015-1','artifactBytes':2302724,'artifactSha256':'f3e0bd3faa5281fe3dc09801813c21347847b9bad3a2f2fbb25dd814d3e0b75f','stageAccepted':True,'rawJUnitVerified':True,'all265PredecessorJvmIdentitiesUnchanged':True,'all14PredecessorNativeCasesPreserved':True,'all10NewNativeMethodIdentitiesChecked':True,'nativeProcessDeathScenarios':8,'tests':{k:{'tests':n,'executed':n,'passed':n,'failures':0,'errors':0,'skipped':0} for k,n in [('python',175),('jvm',265),('androidFocused',24)]},'visualReview':'PASSED_ACTUAL_NATIVE_REVIEW','nativeImagesReviewed':screens['captures'],'reviewMethod':'Viewed two contact sheets containing all eight originals, plus original Neon dark and Explorer light at full resolution. Checked all eight themes/modes, selected/invalid/multi-segment labels, controls, system insets, text wrapping and absence of overlaps. No blocking finding.','visualScope':'Eight English mixed-validity previews at actual1080x2400; three requested viewport sizes, other UI states/locales and final release visuals remain phaseF.','isolation':iso,'scope':'PhaseC SAF preview and selected import only. Library/details, maps/following, complete legacy API35, fixed-key Release, actual P071 upgrade and publication remain later gates.'}
Path('work/phase-C-independent-review.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ('isolation','nativeImagesReviewed')},indent=2))
