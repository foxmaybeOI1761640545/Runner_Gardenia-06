"""Build the reviewed source once; preserve distinct test, build and publication provenance."""
import hashlib,io,json,os,shutil,subprocess,sys,zipfile
from pathlib import Path
import p04_pipeline as pipeline
import verify_p0_release as original
from publish_release import GitHub,verify_apk
from p04_version import VERSION,TAG,CERT,APK,check

def context():
 values=pipeline.scope()
 if values['mode'] not in ('sign','publish'):raise ValueError('Explicit visual approval activation required')
 record={'mode':values['mode'],'workflowCommit':os.environ['GITHUB_SHA'],'approval':pipeline.approval_record()}
 if values['mode']=='publish':record['publication']=json.loads(pipeline.PUBLICATION.read_text())
 Path(os.environ['P04_CONTEXT']).write_text(json.dumps(record,indent=2)+'\n')

def load():
 pipeline.trusted();c=json.loads(Path(os.environ['P04_CONTEXT']).read_text());a=c['approval'];api=GitHub()
 if c['workflowCommit']!=os.environ['GITHUB_SHA'] or pipeline.git('rev-parse','HEAD')!=a['source']:raise ValueError('Build checkout differs from actual visually reviewed source')
 if pipeline.git('rev-parse','HEAD:src/main')!=a['mainTree']:raise ValueError('Reviewed native app tree differs')
 pipeline.accepted_jobs(api,a);pipeline.protected_refs()
 if c['mode']=='sign':pipeline.activation_paths(pipeline.git('diff','--name-only',a['source'],c['workflowCommit']).splitlines())
 elif c['mode']=='publish':
  p=c['publication'];pipeline.accepted_build(api,p);pipeline.publication_paths(pipeline.git('diff','--name-only',p['buildWorkflowCommit'],c['workflowCommit']).splitlines())
 else:raise ValueError('Wrong acceptance operation')
 return c,api

def unpack(api,identifier,name,run,workflow,digest,folder):
 meta=pipeline.artifact(api,identifier,name,run,workflow,digest);raw=api.request(meta['archive_download_url'],binary=True)
 if hashlib.sha256(raw).hexdigest()!=digest:raise ValueError('Immutable artifact archive changed')
 with zipfile.ZipFile(io.BytesIO(raw)) as z:
  if any(Path(n).is_absolute() or '..' in Path(n).parts for n in z.namelist()):raise ValueError('Unsafe artifact path')
  z.extractall(folder)

def inputs():
 c,api=load();a=c['approval']
 unpack(api,a['reportsArtifactId'],'P04-reports-'+a['testRunId'],a['testRunId'],a['source'],a['reportsArtifactSha256'],'gate')
 shutil.copyfile('gate/release-plan.json','release-plan.json')
 if c['mode']=='publish':
  p=c['publication'];unpack(api,p['formalArtifactId'],'RunnerGardenia-P04-Release-'+p['buildRunId'],p['buildRunId'],p['buildWorkflowCommit'],p['formalArtifactSha256'],'dist')
 gate()

def gate():
 c,api=load();a=c['approval'];plan=json.loads(Path('release-plan.json').read_text());report=json.loads(Path('gate/build/reports/p0-validation/tests.json').read_text())
 if plan!=json.loads(Path('gate/release-plan.json').read_text()) or plan.get('testTag')!=TAG or plan.get('refinement') is not True or str(plan['runId'])!=a['testRunId'] or plan['versionName']!=VERSION or plan['versionCode']!=6775:raise ValueError('Wrong exact-source full regression plan')
 original.validate_gate(plan,report,pipeline.configuration(),a['source'])
 import collect_p02_home_ui
 collect_p02_home_ui.inspect(Path('gate/build/reports/p0-ui'),a['source'])
 theme=json.loads(Path('gate/build/reports/p03-theme/result.json').read_text())
 if theme.get('passed') is not True or theme['commit']!=a['source'] or len(theme['schemes'])!=8:raise ValueError('Missing original full native appearance regression')
 preview=json.loads(Path('gate/build/reports/p04-visual/p04-visual-matrix.json').read_text())
 if preview.get('passed') is not True or preview.get('native') is not True or preview['sourceCommit']!=a['source'] or preview['api']!=35 or len(preview['screenshots'])!=192:raise ValueError('Reviewed native fixture evidence absent')
 for r in preview['screenshots']:
  if hashlib.sha256(Path('gate/build/reports/p04-visual',r['file']).read_bytes()).hexdigest()!=r['sha256']:raise ValueError('Reviewed fixture PNG changed')
 check(a['source']);return plan,report

def existing():
 gate();c,api=load();run=os.environ['GITHUB_RUN_ID'];rows=api.request(api.base+f'/actions/runs/{run}/artifacts?per_page=100')['artifacts']
 values={}
 for kind,name in [('formal','RunnerGardenia-P04-Release-'+run),('candidate','RunnerGardenia-P04-Candidate-'+run)]:
  hits=[r for r in rows if r['name']==name]
  if len(hits)>1:raise ValueError('Ambiguous immutable '+kind)
  if hits:pipeline.artifact(api,hits[0]['id'],name,run,c['workflowCommit'],hits[0]['digest'].removeprefix('sha256:'))
  values[kind+'-id']=str(hits[0]['id']) if hits else ''
 with open(os.environ['GITHUB_OUTPUT'],'a') as out:
  for k,v in values.items():out.write(k+'='+v+'\n')
 print(json.dumps(values))

def verify():
 if os.environ.get('ANDROID_SIGNING_CERT_SHA256','').replace(':','').strip().upper()!=CERT:raise ValueError('Fixed signer differs')
 original.gate=gate;original.verify();c,_=load();info=json.loads(Path('dist/build-info.json').read_text())
 out=Path('build/reports/p04-release');out.mkdir(parents=True,exist_ok=True)
 shutil.copyfile('build/outputs/apk/reproducible/release/output-metadata.json',out/'output-metadata.json')
 (out/'manifest.xml').write_text(subprocess.check_output(['apkanalyzer','manifest','print','dist/'+APK],text=True))
 (out/'signature.txt').write_text(subprocess.check_output([str(Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0/apksigner'),'verify','--verbose','--print-certs','dist/'+APK],text=True))
 log=Path(os.environ['RUNNER_TEMP'])/'p04-release-gradle.log';lines=[l for l in log.read_text().splitlines() if l.startswith('> Task :') or l.startswith('BUILD SUCCESSFUL')]
 if not any(':minifyReproducibleReleaseWithR8' in l for l in lines) or not any(':optimizeReproducibleReleaseResources' in l for l in lines):raise ValueError('Actual optimized Release tasks absent')
 (out/'optimization-tasks.txt').write_text('\n'.join(lines)+'\n')
 info.update(r8TaskExecuted=True,resourceOptimizationTaskExecuted=True,themePolicy='FOLLOW_SYSTEM',themeFamilies=['precision','quiet','explorer','neon'],themeSchemes=8,testRunId=c['approval']['testRunId'],buildRunId=os.environ['GITHUB_RUN_ID'],buildWorkflowCommit=c['workflowCommit'],mainTree=c['approval']['mainTree'],nativeVisualReview=c['approval'])
 Path('dist/build-info.json').write_text(json.dumps(info,indent=2)+'\n')

def bundle(folder=Path('dist')):
 plan,report=gate();c,_=load();info=json.loads((folder/'build-info.json').read_text());runtime=json.loads((folder/'runtime-smoke.json').read_text());native=json.loads((folder/'native-theme-smoke.json').read_text())
 if info.get('plan')!=plan or info.get('commit')!=plan['commit'] or info.get('versionName')!=VERSION or info.get('versionCode')!=6775 or info.get('certificateSha256')!=CERT or info.get('applicationId')!='com.fox.runner' or info.get('variant')!='reproducibleRelease' or info.get('debuggable') is not False or info.get('mainTree')!=c['approval']['mainTree']:raise ValueError('Wrong formal APK identity/source/tree')
 if any(info.get(k) is not True for k in ['signatureVerified','alignmentVerified','runtimeSmokeVerified','r8TaskExecuted','resourceOptimizationTaskExecuted']):raise ValueError('Missing actual signing/optimization/runtime proof')
 expected={k:{f:report[k][f] for f in ['tests','executed','failures','errors','skipped']} for k in ['android','unit']}
 if info['tests']!=expected or info['testRunId']!=c['approval']['testRunId']:raise ValueError('Test evidence differs from exact-source full gate')
 if runtime.get('passed') is not True or runtime['commit']!=info['commit'] or runtime['apkSha256']!=info['apkSha256'] or runtime['upgradeFromVersionCode']!=6774 or any(runtime.get(k) is not True for k in ['deviceConfigurationRetained','preparationCreatesNoWorkout','liveGpsFieldsVerified','releaseGpxSchemaVerified','forceStopRecovery']):raise ValueError('Actual P0.3 upgrade/GPS/GPX proof absent')
 if native.get('passed') is not True or native['commit']!=info['commit'] or native['apkSha256']!=info['apkSha256'] or native['api']!=35 or native['policy']!='FOLLOW_SYSTEM' or native['processRestartPersistence'] is not True or native.get('nativePageChecks')!=32 or native.get('evaluationCommit')!=info['commit'] or len(native['schemes'])!=8 or len(native['screenshots'])!=32:raise ValueError('Actual signed native appearance matrix absent')
 if any(s.get('renderedPaletteVerified') is not True for s in native['schemes']) or sum(r.get('renderedBackgroundVerified') is True for r in native['screenshots'])!=16:raise ValueError('Actual signed native rendered palette checks absent')
 rawzip=(folder/'theme-screenshots.zip').read_bytes()
 if hashlib.sha256(rawzip).hexdigest()!=native['screenshotsZipSha256']:raise ValueError('Signed native screenshot ZIP changed')
 import xml.etree.ElementTree as ET
 from p04_release_theme_ui import PAGE_IDS,PAGE_ACTIVITIES,page_matches
 if {(r['family'],r['appearance'],r['page']) for r in native['screenshots']}!={(f,m,p) for f in ['precision','quiet','explorer','neon'] for m in ['light','dark'] for p in PAGE_IDS}:raise ValueError('Native signed page coverage incomplete')
 with zipfile.ZipFile(io.BytesIO(rawzip)) as z:
  for row in native['screenshots']:
   xml=z.read(row['nativeWindowFile']);raw=z.read(row['file'])
   if row.get('nativePageVerified') is not True or hashlib.sha256(xml).hexdigest()!=row['nativeWindowSha256'] or not page_matches(ET.fromstring(xml),row['page']) or PAGE_ACTIVITIES[row['page']] not in row['focusedWindow'] or raw[:8]!=b'\x89PNG\r\n\x1a\n' or hashlib.sha256(raw).hexdigest()!=row['sha256']:raise ValueError('Signed native focused page XML/PNG changed')
 raw=(folder/APK).read_bytes()
 if len(list(folder.glob('*.apk')))!=1 or len(raw)!=info['apkBytes'] or hashlib.sha256(raw).hexdigest()!=info['apkSha256'] or (folder/'SHA256SUMS').read_text()!=f"{info['apkSha256']}  {APK}\n":raise ValueError('Accepted APK bytes changed')
 if c['mode']=='publish':
  p=c['publication']
  if info['apkSha256']!=p['apkSha256'] or info['buildRunId']!=p['buildRunId'] or info['buildWorkflowCommit']!=p['buildWorkflowCommit']:raise ValueError('Visually reviewed signed bundle differs')
 verify_apk(raw,info);print('PASS: visually reviewed source, complete regression, fixed-key APK, actual upgrade/GPX and 32 signed native pages')
 return info

if __name__=='__main__':{'context':context,'inputs':inputs,'gate':gate,'existing':existing,'verify':verify,'bundle':bundle}[sys.argv[1]]()
