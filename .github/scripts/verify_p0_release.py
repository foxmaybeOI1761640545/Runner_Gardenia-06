"""Verify the exact, fully tested P0 APK. No tags, releases, secret output or key access."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET
from verify_p0_offline import inspect as offline
from apk_resources import resolve_packaged_xml


def validate_gate(plan,report,config,commit):
    if (config.get('stage')!=10 or config.get('fullRegression') is not True or config.get('signed') is not True
            or plan.get('publishing') is not False or plan.get('stage')!=10 or plan.get('commit')!=commit
            or report.get('plan')!=plan or report.get('passed') is not True):
        raise ValueError('Signing requires this exact run commit and complete P0 gate')
    for scope in ('android','unit'):
        result=report.get(scope,{})
        if result.get('passed') is not True or result.get('executed',0)<=0 or result.get('failures')!=0 or result.get('errors')!=0:
            raise ValueError('Missing actual successful Android/JVM execution')
    for scope in ('processDurability','gpxSchema','offline'):
        if report.get(scope,{}).get('passed') is not True or report[scope].get('commit')!=commit:
            raise ValueError('Missing exact process/GPX/offline evidence')


def command(*args):return subprocess.check_output(list(map(str,args)),text=True).strip()


def gate():
    plan=json.loads(Path('release-plan.json').read_text())
    transferred=json.loads(Path('gate/release-plan.json').read_text())
    if transferred!=plan:raise ValueError('Downloaded tested plan differs from signing checkout')
    report=json.loads(Path('gate/build/reports/p0-validation/tests.json').read_text())
    config=json.loads(Path('.github/p0/stage.json').read_text())
    validate_gate(plan,report,config,command('git','rev-parse','HEAD'))
    if str(plan['runId'])!=os.environ['GITHUB_RUN_ID']:raise ValueError('Different Actions run')
    print('Exact same-run full P0 test gate verified; publication remains disabled.')
    return plan,report


def verify():
    plan,report=gate();sdk=Path(os.environ['ANDROID_HOME']);tools=sdk/'build-tools/36.0.0'
    expected=os.environ.get('ANDROID_SIGNING_CERT_SHA256','').replace(':','').strip().upper()
    if not re.fullmatch('[0-9A-F]{64}',expected):raise ValueError('Invalid independent certificate fingerprint')
    folder=Path('build/outputs/apk/reproducible/release');metadata=json.loads((folder/'output-metadata.json').read_text())
    if metadata['applicationId']!='com.fox.runner' or metadata['variantName']!='reproducibleRelease' or len(metadata['elements'])!=1:raise ValueError('Unexpected release variant/outputs')
    item=metadata['elements'][0];apk=(folder/item['outputFile']).resolve()
    if apk.parent!=folder.resolve() or not apk.is_file() or len(list(folder.glob('*.apk')))!=1:raise ValueError('Ambiguous actual release APK')
    if item['versionName']!=plan['versionName'] or item['versionCode']!=plan['versionCode']:raise ValueError('Release version differs from tested plan')
    cert=command(tools/'apksigner','verify','--verbose','--print-certs','--min-sdk-version','26',apk)
    actual=re.findall(r'^Signer #\d+ certificate SHA-256 digest:\s*([0-9a-fA-F]+)\s*$',cert,re.M)
    if len(actual)!=1 or actual[0].upper()!=expected or 'CN=Android Debug' in cert:raise ValueError('Fixed signing certificate mismatch or Debug signer')
    command(tools/'zipalign','-c','-P','16','4',apk)
    analyzer=shutil.which('apkanalyzer')
    if not analyzer:raise ValueError('Missing APK analyzer')
    for field,value in [('application-id','com.fox.runner'),('version-name',plan['versionName']),('version-code',str(plan['versionCode'])),('debuggable','false')]:
        if command(analyzer,'manifest',field,apk)!=value:raise ValueError('APK identity mismatch: '+field)
    security=offline(ET.fromstring(command(analyzer,'manifest','print',apk)),True)
    entry=resolve_packaged_xml(analyzer,apk,'file_paths')
    paths=ET.fromstring(command(analyzer,'resources','xml','--file',entry,apk))
    if len(paths)!=1 or paths[0].tag!='cache-path' or paths[0].get('path')!='p0_exports/':raise ValueError('FileProvider exposes more than the private GPX cache')
    mapping=Path('build/outputs/mapping/reproducibleRelease/mapping.txt')
    if not mapping.is_file() or mapping.stat().st_size==0:raise ValueError('R8 mapping evidence missing')
    out=Path('dist');out.mkdir(exist_ok=False);name='RunnerGardenia-P0-Release.apk';shutil.copyfile(apk,out/name)
    checksum=hashlib.sha256((out/name).read_bytes()).hexdigest()
    info={'applicationId':'com.fox.runner','versionName':plan['versionName'],'versionCode':plan['versionCode'],'commit':plan['commit'],
          'variant':'reproducibleRelease','certificateSha256':expected,'signatureVerified':True,'alignmentVerified':True,'debuggable':False,
          'apkSha256':checksum,'apkBytes':apk.stat().st_size,'r8MappingSha256':hashlib.sha256(mapping.read_bytes()).hexdigest(),
          'offline':security,'plan':plan,'tests':{name:{field:report[name][field] for field in ('tests','executed','failures','errors','skipped')} for name in ('android','unit')},
          'run':f"https://github.com/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}"}
    (out/'SHA256SUMS').write_text(f'{checksum}  {name}\n');(out/'build-info.json').write_text(json.dumps(info,indent=2)+'\n')
    print(json.dumps({key:value for key,value in info.items() if key!='tests'},indent=2))


if __name__=='__main__':
    import sys
    gate() if sys.argv[1:] == ['gate'] else verify()
