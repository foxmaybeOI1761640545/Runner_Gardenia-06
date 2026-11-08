"""Reuse the existing APK verifier; add exact P0.3 branch, report and recovery gates."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import collect_p02_home_ui
import p03_pipeline
import verify_p0_release as original
from publish_release import GitHub,verify_apk
from p03_version import VERSION,TAG,CERT,APK,check


def gate():
    if (os.environ.get('GITHUB_REF')!='refs/heads/feature/theme-system-p0-3' or os.environ.get('GITHUB_EVENT_NAME')!='push'
            or os.environ.get('GITHUB_REPOSITORY')!='foxmaybeOI1761640545/Runner_Gardenia-06'):raise ValueError('Untrusted signing/publication context')
    config=p03_pipeline.configuration()
    if config['phase']!=6 or not config['signed']:raise ValueError('Signing is limited to accepted final phase')
    plan=json.loads(Path('release-plan.json').read_text());tested=json.loads(Path('gate/release-plan.json').read_text())
    report=json.loads(Path('gate/build/reports/p0-validation/tests.json').read_text())
    commit=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
    if plan!=tested or plan.get('p03Phase')!=6 or plan.get('testTag')!=TAG or str(plan['runId'])!=os.environ['GITHUB_RUN_ID']:raise ValueError('Wrong tested run/phase/plan')
    original.validate_gate(plan,report,config,commit)
    collect_p02_home_ui.inspect(Path('gate/build/reports/p0-ui'),commit)
    theme=json.loads(Path("gate/build/reports/p03-theme/result.json").read_text())
    if theme.get("passed") is not True or theme.get("commit")!=commit or len(theme.get("schemes",[]))!=8:raise ValueError("Missing exact native theme gate")
    check(commit);p03_pipeline.protected_refs()
    return plan,report


def bundle(folder=Path('dist')):
    plan,report=gate();info=json.loads((folder/'build-info.json').read_text());runtime=json.loads((folder/'runtime-smoke.json').read_text())
    if (info.get('plan')!=plan or info.get('commit')!=plan['commit'] or info.get('versionName')!=VERSION
            or info.get('versionCode')!=plan['versionCode'] or info.get('certificateSha256')!=CERT
            or info.get('applicationId')!='com.fox.runner' or info.get('variant')!='reproducibleRelease'
            or any(info.get(k) is not True for k in ('signatureVerified','alignmentVerified','runtimeSmokeVerified'))
            or info.get('debuggable') is not False):raise ValueError('Unsigned/unverified/wrong-source formal bundle')
    expected={k:{f:report[k][f] for f in ('tests','executed','failures','errors','skipped')} for k in ('android','unit')}
    if info.get('tests')!=expected:raise ValueError('Bundle tests differ from actual same-run gate')
    if (runtime.get('passed') is not True or runtime.get('commit')!=plan['commit'] or runtime.get('apkSha256')!=info['apkSha256']
            or runtime.get('upgradeFromVersionCode')!=6773 or runtime.get('deviceConfigurationRetained') is not True
            or runtime.get('preparationCreatesNoWorkout') is not True):raise ValueError('Missing actual P0.2 upgrade/acquisition acceptance')
    if len(list(folder.glob('*.apk')))!=1 or (folder/APK).stat().st_size!=info['apkBytes']:raise ValueError('Ambiguous APK/size')
    native=json.loads((folder/'native-theme-smoke.json').read_text())
    if native.get('passed') is not True or native.get('commit')!=info['commit'] or native.get('apkSha256')!=info['apkSha256'] or len(native.get('schemes',[]))!=8:raise ValueError('Missing actual signed native appearance matrix')
    if any(s.get('renderedPaletteVerified') is not True for s in native['schemes']) or sum(r.get('renderedBackgroundVerified') is True for r in native.get('screenshots',[]))!=16:raise ValueError('Missing actual signed Light/Dark rendered pixel verification')
    if hashlib.sha256((folder/'theme-screenshots.zip').read_bytes()).hexdigest()!=native['screenshotsZipSha256']:raise ValueError('Native screenshot package changed')
    raw=(folder/APK).read_bytes()
    if hashlib.sha256(raw).hexdigest()!=info['apkSha256'] or (folder/'SHA256SUMS').read_text()!=f"{info['apkSha256']}  {APK}\n":raise ValueError('APK/checksum changed')
    verify_apk(raw,info)
    proof=Path('build/reports/p03-bundle');proof.mkdir(parents=True,exist_ok=True)
    (proof/'result.json').write_text(json.dumps({'passed':True,'commit':info['commit'],'apkSha256':info['apkSha256'],'runId':plan['runId'],'signatureAndRuntimeVerified':True},indent=2)+'\n')
    print('Fixed-key P0.3 APK, actual same-run tests and P0.2 upgrade verified.')
    return info


def existing():
    gate();api=GitHub();run=os.environ['GITHUB_RUN_ID'];name='RunnerGardenia-P03-Release-'+run
    rows=[];page=1
    while True:
        batch=api.request(api.base+f'/actions/runs/{run}/artifacts?per_page=100&page={page}')['artifacts'];rows+=batch
        if len(batch)<100:break
        page+=1
    rows=[a for a in rows if a['name']==name]
    if len(rows)>1 or (rows and (rows[0]['expired'] or rows[0]['workflow_run']['head_sha']!=os.environ['GITHUB_SHA'])):raise ValueError('Existing formal artifact is ambiguous/unavailable/wrong-source')
    with open(os.environ['GITHUB_OUTPUT'],'a') as f:f.write('artifact-id='+str(rows[0]['id'] if rows else '')+'\n')


def verify():
    if os.environ.get('ANDROID_SIGNING_CERT_SHA256','').replace(':','').strip().upper()!=CERT:raise ValueError('Environment certificate differs from preserved P0.2 identity')
    original.gate=gate;original.verify()
    output=Path('build/reports/p03-release');output.mkdir(parents=True,exist_ok=True)
    # Preserve actual merged/packaged manifest and non-secret task/mapping evidence.
    sdk=Path(os.environ['ANDROID_HOME']);apk=Path('dist')/APK
    import shutil
    shutil.copyfile('build/outputs/apk/reproducible/release/output-metadata.json',output/'output-metadata.json')
    (output/'manifest.xml').write_text(subprocess.check_output(['apkanalyzer','manifest','print',str(apk)],text=True))
    (output/'signature.txt').write_text(subprocess.check_output([str(sdk/'build-tools/36.0.0/apksigner'),'verify','--verbose','--print-certs',str(apk)],text=True))
    log=Path(os.environ['RUNNER_TEMP'])/'p03-release-gradle.log'
    lines=[l for l in log.read_text().splitlines() if l.startswith('> Task :') or l.startswith('BUILD SUCCESSFUL')]
    if not any(':minifyReproducibleReleaseWithR8' in l for l in lines) or not any(':optimizeReproducibleReleaseResources' in l for l in lines):raise ValueError('Actual R8/resource optimizer tasks missing')
    (output/'optimization-tasks.txt').write_text('\n'.join(lines)+'\n')
    info=json.loads(Path('dist/build-info.json').read_text());info.update(r8TaskExecuted=True,resourceOptimizationTaskExecuted=True,themePolicy='FOLLOW_SYSTEM',themeFamilies=['precision','quiet','explorer','neon'],themeSchemes=8)
    Path('dist/build-info.json').write_text(json.dumps(info,indent=2)+'\n')


if __name__=='__main__':
    {'gate':gate,'existing':existing,'verify':verify,'bundle':bundle}[sys.argv[1]]()
