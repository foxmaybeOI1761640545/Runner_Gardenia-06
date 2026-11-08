"""Publish only the immutable, already accepted P0.3 APK; never build or sign."""
import hashlib,io,json,os,subprocess,sys,zipfile
from pathlib import Path
from publish_release import GitHub,verify_apk
from verify_p0_release import validate_gate
import collect_p02_home_ui
import p03_pipeline
from p03_version import VERSION,TAG,CERT,APK

SOURCE='77ef053b76fd5e7c6c0c9ce1023e365fa3d7c471'
BUILD_RUN='42443873771'
APK_SHA='24225e5a28827eda1fa5799e80230d03464416a3aaa72c0cf34ccb4be090a3ff'
MARKER='[p03-existing-apk]'
ALLOWED={'.github/workflows/p03-theme-system.yml','.github/scripts/p03_publication_recovery.py',
         '.github/scripts/p03_publish.py','.github/scripts/publish_release.py',
         '.github/tests/test_p03_publication_recovery.py'}
ARTIFACTS={
    'dist':(13603541459,'RunnerGardenia-P03-Release-'+BUILD_RUN,'d11c2d2385ad915f1e6120f396c19d520d89fab9fae81d8d31ada9485f7b9cd8'),
    'gate':(13603117684,'P03-reports-'+BUILD_RUN,'d2b7f2f31f4b1e64c56a5cf5b35ebb5baad34f14d5a2dc4621f190a18e5e8854')}

def publication_only(message,paths):
    if MARKER not in message:return False
    if not paths or any(p not in ALLOWED and not p.startswith('docs/') for p in paths):
        raise ValueError('Existing APK publication cannot change product/version/signing/original tests')
    return True

def scope():
    if (os.environ.get('GITHUB_REPOSITORY')!='foxmaybeOI1761640545/Runner_Gardenia-06'
            or os.environ.get('GITHUB_REF')!='refs/heads/feature/theme-system-p0-3'
            or os.environ.get('GITHUB_EVENT_NAME')!='push'):raise ValueError('Wrong recovery context')
    head=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
    if head!=os.environ['GITHUB_SHA']:raise ValueError('Wrong recovery checkout')
    subprocess.run(['git','merge-base','--is-ancestor',SOURCE,'HEAD'],check=True)
    message=json.loads(Path(os.environ['GITHUB_EVENT_PATH']).read_text())['head_commit']['message']
    paths=subprocess.check_output(['git','diff','--name-only',SOURCE,'HEAD'],text=True).splitlines()
    selected=publication_only(message,paths)
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'],'a') as out:out.write('publication='+str(selected).lower()+'\n')
    if selected:p03_pipeline.protected_refs()
    return selected,paths

def provenance(api):
    run=api.request(api.base+'/actions/runs/'+BUILD_RUN)
    if run['head_sha']!=SOURCE or run['head_branch']!='feature/theme-system-p0-3' or run['event']!='push':raise ValueError('Wrong original build run')
    jobs=api.request(api.base+'/actions/runs/'+BUILD_RUN+'/jobs?per_page=100&filter=all')['jobs']
    required={'checks','native-window-helper','Optimized fixed-key P0.3 Release and P0.2 upgrade'}
    for name in required:
        if not any(j['name']==name and j['conclusion']=='success' for j in jobs):raise ValueError('Original accepted job missing: '+name)
    return jobs

def download(api):
    selected,paths=scope()
    if not selected:raise ValueError('Explicit unchanged-APK recovery marker required')
    provenance(api)
    receipts=[]
    for folder,(identifier,name,expected) in ARTIFACTS.items():
        meta=api.request(api.base+'/actions/artifacts/'+str(identifier))
        if meta['expired'] or meta['name']!=name or meta['workflow_run']['id']!=int(BUILD_RUN) or meta['workflow_run']['head_sha']!=SOURCE or meta.get('digest')!='sha256:'+expected:raise ValueError('Immutable original artifact identity differs')
        raw=api.request(meta['archive_download_url'],binary=True)
        if hashlib.sha256(raw).hexdigest()!=expected:raise ValueError('Original artifact ZIP checksum differs')
        with zipfile.ZipFile(io.BytesIO(raw)) as archive:
            if any(Path(n).is_absolute() or '..' in Path(n).parts for n in archive.namelist()):raise ValueError('Unsafe artifact path')
            archive.extractall(folder)
        receipts.append({'artifactId':identifier,'name':name,'sha256':expected,'bytes':len(raw)})
    Path('recovery-inputs.json').write_text(json.dumps({'buildSource':SOURCE,'buildRunId':BUILD_RUN,'recoverySource':os.environ['GITHUB_SHA'],'recoveryRunId':os.environ['GITHUB_RUN_ID'],'changedPaths':paths,'artifacts':receipts,'rebuilt':False,'resigned':False},indent=2)+'\n')

def accepted_bundle(folder=Path('dist')):
    selected,_=scope()
    if not selected:raise ValueError('Not a verified unchanged-APK recovery')
    api=GitHub();provenance(api)
    plan=json.loads(Path('gate/release-plan.json').read_text());report=json.loads(Path('gate/build/reports/p0-validation/tests.json').read_text())
    config=json.loads(Path('.github/p03/stage.json').read_text())
    validate_gate(plan,report,config,SOURCE)
    if plan['runId']!=BUILD_RUN or plan['testTag']!=TAG or plan['p03Phase']!=6 or plan['versionCode']!=6774 or plan['versionName']!=VERSION:raise ValueError('Wrong original tested allocation')
    collect_p02_home_ui.inspect(Path('gate/build/reports/p0-ui'),SOURCE)
    theme=json.loads(Path('gate/build/reports/p03-theme/result.json').read_text())
    if theme.get('passed') is not True or theme['commit']!=SOURCE or len(theme['schemes'])!=8 or theme['screenshots']!=56:raise ValueError('Original native theme gate incomplete')
    info=json.loads((folder/'build-info.json').read_text());runtime=json.loads((folder/'runtime-smoke.json').read_text());native=json.loads((folder/'native-theme-smoke.json').read_text())
    if info['plan']!=plan or info['commit']!=SOURCE or info['apkSha256']!=APK_SHA or info['certificateSha256']!=CERT or info['versionCode']!=6774 or info['versionName']!=VERSION or info['applicationId']!='com.fox.runner' or info['variant']!='reproducibleRelease' or info['debuggable'] is not False:raise ValueError('Original formal APK identity differs')
    if any(info.get(k) is not True for k in ['signatureVerified','alignmentVerified','runtimeSmokeVerified','r8TaskExecuted','resourceOptimizationTaskExecuted']):raise ValueError('Original optimization/signature/runtime proof absent')
    expected={k:{f:report[k][f] for f in ['tests','executed','failures','errors','skipped']} for k in ['android','unit']}
    if info['tests']!=expected:raise ValueError('Tests differ from original exact-source reports')
    if runtime.get('passed') is not True or runtime['commit']!=SOURCE or runtime['apkSha256']!=APK_SHA or runtime['upgradeFromVersionCode']!=6773 or any(runtime.get(k) is not True for k in ['deviceConfigurationRetained','preparationCreatesNoWorkout','liveGpsFieldsVerified','releaseGpxSchemaVerified','forceStopRecovery']):raise ValueError('Original actual P0.2 upgrade/GPS/GPX evidence absent')
    if native.get('passed') is not True or native['commit']!=SOURCE or native['apkSha256']!=APK_SHA or native['api']!=35 or native['policy']!='FOLLOW_SYSTEM' or native['processRestartPersistence'] is not True or len(native['schemes'])!=8 or len(native['screenshots'])!=32:raise ValueError('Original signed native matrix absent')
    if any(s.get('renderedPaletteVerified') is not True for s in native['schemes']) or sum(r.get('renderedBackgroundVerified') is True for r in native['screenshots'])!=16:raise ValueError('Original rendered native palette evidence absent')
    screenshot_zip=(folder/'theme-screenshots.zip').read_bytes()
    if hashlib.sha256(screenshot_zip).hexdigest()!=native['screenshotsZipSha256']:raise ValueError('Original native screenshots changed')
    with zipfile.ZipFile(io.BytesIO(screenshot_zip)) as archive:
        for row in native['screenshots']:
            raw=archive.read(row['file'])
            if raw[:8]!=b'\x89PNG\r\n\x1a\n' or hashlib.sha256(raw).hexdigest()!=row['sha256']:raise ValueError('Original native PNG changed')
    raw=(folder/APK).read_bytes()
    if len(list(folder.glob('*.apk')))!=1 or len(raw)!=2799792 or hashlib.sha256(raw).hexdigest()!=APK_SHA or info['apkBytes']!=len(raw) or (folder/'SHA256SUMS').read_text()!=f'{APK_SHA}  {APK}\n':raise ValueError('Original accepted APK bytes changed')
    verify_apk(raw,info)
    subprocess.run([str(Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0/zipalign'),'-c','-P','16','4',str(folder/APK)],check=True)
    return info

def publish():
    api=GitHub();download(api)
    from p03_publish import publish as publish_original
    publish_original(api,verify_bundle=accepted_bundle)
    original=json.loads(Path('dist/publication.json').read_text());original.update(publicationRecovery=json.loads(Path('recovery-inputs.json').read_text()))
    Path('dist/publication.json').write_text(json.dumps(original,indent=2)+'\n')
    print('PASS: original accepted APK published and publicly reverified; no build or signing performed')

if __name__=='__main__':
    if sys.argv[1:] == ['scope']:print('Existing APK recovery:',scope()[0])
    else:publish()
