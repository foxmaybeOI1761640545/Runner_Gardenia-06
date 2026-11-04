"""Branch-only P0 validation. Never creates tags/releases or writes protected refs."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import tempfile

from check_test_results import inspect

MANDATORY = ['de.dennisguse.opentracks.TimezoneEnvironmentTest',
             'de.dennisguse.opentracks.services.TrackRecordingServicePreferenceLifecycleTest']


def configuration():
    config = json.loads(Path('.github/p0/stage.json').read_text())
    if type(config.get('stage')) is not int or not 1 <= config['stage'] <= 10:
        raise ValueError('Invalid P0 stage')
    if type(config.get('fullRegression')) is not bool or type(config.get('signed')) is not bool:
        raise ValueError('Explicit test/signing modes required')
    if config['signed'] and (config['stage'] != 10 or not config['fullRegression']):
        raise ValueError('Signing requires the final complete regression stage')
    if config['stage'] >= 9 and not config['fullRegression']:
        raise ValueError('Final stages must run all regression classes')
    if not isinstance(config.get('classes'), list) or any(not re.fullmatch(r'[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)+', x) for x in config['classes']):
        raise ValueError('Invalid instrumentation class selector')
    if not config['fullRegression'] and not config['classes']:
        raise ValueError('A scoped stage cannot run zero selected classes')
    return config


def protected_refs():
    baseline = json.loads(Path('docs/P0_BASELINE.json').read_text())
    actual = {line.split('\t')[1]:line.split('\t')[0] for line in subprocess.check_output(
        ['git','ls-remote','--heads','origin'],text=True).splitlines()}
    for ref, expected in baseline['protectedRemoteBranches'].items():
        if actual.get(ref) != expected:
            raise ValueError('Protected branch changed; do not reset or overwrite it: '+ref)
    subprocess.run(['git','merge-base','--is-ancestor',baseline['baselineCommit'],'HEAD'],check=True)
    return baseline


def prepare():
    baseline = protected_refs()
    if (os.environ.get('GITHUB_REF') != 'refs/heads/'+baseline['branch']
            or os.environ.get('GITHUB_EVENT_NAME') != 'push'
            or os.environ.get('GITHUB_REPOSITORY') != baseline['repository']):
        raise ValueError('P0 workflow may run only for this new branch push')
    config = configuration()
    commit = subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
    if commit != os.environ['GITHUB_SHA']:
        raise ValueError('Checkout differs from exact workflow SHA')
    version = dict(line.split('=',1) for line in Path('version.properties').read_text().splitlines() if line)
    version['VERSION_NAME_FULL'] = version['VERSION_NAME']+'-'+commit[:12]
    Path('version.properties').write_text(''.join(k+'='+v+'\n' for k,v in version.items()))
    plan = {'versionName':version['VERSION_NAME'],'versionCode':int(version['VERSION_CODE']),
            'commit':commit,'stage':config['stage'],'branch':baseline['branch'],
            'runId':os.environ['GITHUB_RUN_ID'],'publishing':False}
    Path('release-plan.json').write_text(json.dumps(plan,indent=2)+'\n')
    classes = sorted(set(MANDATORY+config['classes'])) if not config['fullRegression'] else []
    for name in classes:
        if not Path('src/androidTest/java',*name.split('.')).with_suffix('.java').is_file():
            raise ValueError('Selected test source is missing: '+name)
    with open(os.environ['GITHUB_OUTPUT'],'a') as out:
        out.write(f"stage={config['stage']}\nfull={str(config['fullRegression']).lower()}\nsigned={str(config['signed']).lower()}\nclasses={','.join(classes)}\n")
    print(json.dumps(plan,indent=2))


def collect_p0_ui():
    sources = list(Path('build/outputs').glob('**/p0-ui'))
    if len(sources) != 1:
        raise ValueError('P0 UI screenshot directory missing or ambiguous')
    destination=Path('build/reports/p0-ui');destination.mkdir(parents=True,exist_ok=True)
    count=0
    for source in sources[0].glob('*.png'):
        payload=source.read_bytes()
        if payload[:8]!=b'\x89PNG\r\n\x1a\n' or payload[12:16]!=b'IHDR':raise ValueError('Invalid P0 screenshot')
        width,height=struct.unpack('>II',payload[16:24])
        if min(width,height)<300:raise ValueError('Incomplete P0 screenshot')
        shutil.copyfile(source,destination/source.name);count+=1
    if count==0:raise ValueError('No actual P0 screenshot')
    return count


def scoped_sources(names, destination, root=Path('src/androidTest/java')):
    for name in names:
        relative=Path(*name.split('.')).with_suffix('.java')
        target=destination/relative
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(root/relative,target)


def gate():
    config=configuration();plan=json.loads(Path('release-plan.json').read_text())
    try:
        if config['fullRegression']:
            android=inspect(Path('build/outputs/androidTest-results'),Path('src/androidTest/java'))
        else:
            with tempfile.TemporaryDirectory() as temporary:
                selected=Path(temporary)
                scoped_sources(sorted(set(MANDATORY+config['classes'])),selected)
                android=inspect(Path('build/outputs/androidTest-results'),selected)
        unit=inspect(Path('build/test-results/testReproducibleDebugUnitTest'),Path('src/test/java'),require_environment=False)
        # P0 launcher screenshot is required whenever the corresponding smoke test is configured.
        ui=collect_p0_ui() if config['fullRegression'] or 'de.dennisguse.opentracks.p0.P0ActivitySmokeTest' in config['classes'] else None
        report={'plan':plan,'stage':config['stage'],'android':android,'unit':unit,'p0Screenshots':ui,
                'passed':android['passed'] and unit['passed']}
    except Exception as error:
        report={'plan':plan,'stage':config['stage'],'passed':False,'diagnostic':str(error)}
    folder=Path('build/reports/p0-validation');folder.mkdir(parents=True,exist_ok=True)
    (folder/'tests.json').write_text(json.dumps(report,indent=2)+'\n')
    summary={key:({k:value.get(k) for k in ('tests','executed','failures','errors','skipped','passed')} if key in ('android','unit') else value)
             for key,value in report.items()}
    message='## P0 stage validation\n\n```json\n'+json.dumps(summary,indent=2)+'\n```\n'
    print(message)
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'],'a') as out:out.write(message)
    if not report['passed']:raise SystemExit('P0 report gate failed')


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('command',choices=['prepare','gate','refs']);args=parser.parse_args()
    if args.command=='prepare':prepare()
    elif args.command=='gate':gate()
    else:protected_refs();print('All protected original branch refs unchanged')
