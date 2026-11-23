import hashlib,json,re,sys,xml.etree.ElementTree as ET
from pathlib import Path
sys.path.insert(0,'.github/scripts')
from p08_transactions import CASES,validate_case
from p08_predecessor import visual_matches
b=Path('work/run45127118513');a=Path('work/run45123727015');source='dae9de23dfbeaf4e324c4222b70182668830eb2c'
def cases(root):
 result={}
 for f in root.rglob('TEST-*.xml'):
  for c in ET.parse(f).getroot().iter('testcase'):
   key=(c.get('classname'),c.get('name'));assert key not in result;assert all(c.find(k) is None for k in ('failure','error','skipped'));result[key]=c
 return result
jvm=cases(b/'build/test-results/testReproducibleDebugUnitTest');priorjvm=cases(a/'build/test-results/testReproducibleDebugUnitTest');assert len(jvm)==265 and jvm.keys()==priorjvm.keys()
native=cases(b/'build/outputs/androidTest-results');prior=cases(a/'build/outputs/androidTest-results');assert len(native)==56 and len(prior)==24 and prior.keys()<=native.keys()
for cls,file,count in [('P08RouteLibraryUiTest','src/androidTest/java/de/dennisguse/opentracks/p0/routes/P08RouteLibraryUiTest.java',14),('P071NavigationTest','src/androidTest/java/de/dennisguse/opentracks/p0/P071NavigationTest.java',18)]:
 methods=set(re.findall(r'@Test\s+public void (\w+)\(',Path(file).read_text()));actual={n for c,n in native if c.endswith('.'+cls)};assert actual==methods and len(actual)==count
proof=json.load(open(b/'build/reports/p08-transactions/result.json'));assert proof['passed'] and set(proof['cases'])==set(CASES)
for name,checkpoint in CASES.items():
 p=proof['cases'][name];validate_case(p,name,checkpoint,source);folder=b/'build/reports/p08-transactions';producer=(folder/(name+'-producer.txt')).read_text();verify=(folder/(name+'-verify.txt')).read_text();assert 'Process crashed.' in producer and 'INSTRUMENTATION_CODE: 0' in producer and 'p08Verified=true' not in producer;assert 'INSTRUMENTATION_RESULT: p08Verified=true' in verify and 'INSTRUMENTATION_CODE: -1' in verify and 'source='+source in verify
h=json.load(open(b/'build/reports/p08-stage/helpers.json'));assert h['source']==source and h['tests']==181 and h['passed'] and not h['skipped'];gate=json.load(open(b/'build/reports/p08-stage/gate.json'));assert gate['passed'] and gate['plan']['commit']==source and gate['plan']['phase']=='D'
images={f.name:f for f in b.glob('build/outputs/**/p0-ui/*.png')};assert len(images)==49
reviewed=[]
from PIL import Image
for field in ['nativeImportScreenshots','nativeLibraryScreenshots']:
 manifest=gate[field];assert manifest['source']==source
 for row in manifest['captures']:
  assert hashlib.sha256(images[row['file']].read_bytes()).hexdigest()==row['sha256'];assert Image.open(images[row['file']]).size==(1080,2400);reviewed.append(row)
assert len(reviewed)==24
supp=[{'file':name,'sha256':hashlib.sha256(f.read_bytes()).hexdigest(),'native':True,'dimensions':list(Image.open(f).size)} for name,f in sorted(images.items()) if name.startswith('p071-')];assert len(supp)==25
iso=json.load(open(next(b.glob('build/outputs/**/p08-repository/isolation.json'))));assert iso['source']==source and iso['schema']==43 and iso['before']==iso['after']
r={'phase':'D','source':source,'runId':45127118513,'artifactId':14797225165,'artifactName':'P08-phase-D-45127118513-1','artifactSha256':'7855b3aa921732e71b0c3d3e64e9361cb949c4c8efacaee04ade0db3395200cf','artifactBytes':7778139,'stageAccepted':True,'rawJUnitVerified':True,'all265PredecessorJvmIdentitiesUnchanged':True,'all24PredecessorNativeCasesPreserved':True,'all14LibraryAnd18OriginalNavigationMethodIdentitiesChecked':True,'nativeProcessDeathScenarios':8,'tests':{k:{'tests':n,'executed':n,'passed':n,'failures':0,'errors':0,'skipped':0} for k,n in [('python',181),('jvm',265),('androidFocused',56)]},'visualReview':'PASSED_ACTUAL_NATIVE_REVIEW','nativeImagesReviewed':reviewed,'supplementalNavigationImagesReviewed':supp,'nativeImagesReviewedTotal':49,'reviewMethod':'Viewed all24 route and25 navigation images in seven contact sheets, plus originals Library Precision light, Detail Neon dark, Home Explorer dark and T13 recreated Settings. Checked metadata/controls/theme/insets/caller pages/new home route action vs locate/zoom. No blocking issue.','visualScope':'One managed viewport (1080x2400, plus original rotation case), not all final requested sizes/locales/states. These p071-prefix PNGs are fresh P08 regression captures, not replaced P071 evidence.','isolation':iso,'scope':'PhaseD library/details/CRUD/secondary Back and original navigation18. Maps/static preview/foreground following and complete legacy API35/fixed JKS/P071 real upgrade/immutable release/public verification remain later gates.'}
visual_matches(r,gate);Path('work/phase-D-independent-review.json').write_text(json.dumps(r,indent=2)+'\n');print(json.dumps({k:v for k,v in r.items() if k not in ('nativeImagesReviewed','supplementalNavigationImagesReviewed','isolation')},indent=2))
