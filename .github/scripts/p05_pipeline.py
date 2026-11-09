"""P0.5 branch-only staged validation; this module cannot sign or publish."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

from check_test_results import inspect
from p0_pipeline import MANDATORY, scoped_sources

ROOT = Path(__file__).resolve().parents[2]
BASE = 'f994d1ecd0e19a840800b2f946df3656a6b312c6'
BRANCH = 'feature/p0-5-dashboard-settings-history'
REPO = 'foxmaybeOI1761640545/Runner_Gardenia-06'
CONFIG = Path('.github/p05/stage.json')
SNAPSHOT = Path('.github/p05/protected-refs.json')
REPORTS = Path('build/reports/p05-validation')


def git(*args):
    return subprocess.check_output(['git', *args], text=True).strip()


def configuration(value=None):
    value = json.loads(CONFIG.read_text()) if value is None else value
    if set(value) != {'stage', 'fullRegression', 'classes', 'signed'}:
        raise ValueError('Explicit P0.5 phase configuration is required')
    stage = value['stage']
    if type(stage) is not int or stage not in range(6):
        raise ValueError('P0.5 phase must be 0 through 5')
    if type(value['fullRegression']) is not bool or value['signed'] is not False:
        raise ValueError('Validation route has no signing or publication authority')
    classes = value['classes']
    if not isinstance(classes, list) or len(classes) != len(set(classes)) or any(
            not isinstance(name, str) or not re.fullmatch(r'[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)+', name)
            for name in classes):
        raise ValueError('Invalid or duplicate Android test class')
    if stage == 0 and (value['fullRegression'] or classes):
        raise ValueError('Phase 0 is static baseline validation, without Android builds')
    if stage > 0 and not value['fullRegression'] and not classes:
        raise ValueError('A development phase must execute selected Android classes')
    if stage == 5 and (not value['fullRegression'] or classes):
        raise ValueError('Final validation must execute every original and P0.5 test')
    if value['fullRegression'] and classes:
        raise ValueError('Full regression cannot narrow its class selection')
    return value


def trusted(environment=None):
    environment = os.environ if environment is None else environment
    expected = {'GITHUB_REPOSITORY': REPO, 'GITHUB_REF': 'refs/heads/' + BRANCH,
                'GITHUB_EVENT_NAME': 'push'}
    if any(environment.get(key) != value for key, value in expected.items()):
        raise ValueError('Only an exact P0.5 branch push can run this validation')


def compare_refs(snapshot, actual):
    if snapshot.get('baselineCommit') != BASE or snapshot.get('branch') != BRANCH:
        raise ValueError('P0.5 baseline identity differs')
    protected = snapshot.get('protectedRefs', {})
    if not protected or any(not name.startswith(('refs/heads/', 'refs/tags/')) for name in protected):
        raise ValueError('Protected reference snapshot is empty or invalid')
    for name, sha in protected.items():
        if actual.get(name) != sha:
            raise ValueError('Protected historical ref changed; do not overwrite: ' + name)
    p04 = snapshot.get('p04', {})
    if (p04.get('source') != 'ccc59b546b5087a2038d32087cc675a3096852c3'
            or p04.get('apkSha256') != 'aa5ebc21c76cbca492edc66453b91d7fdecaf82bd5a69fcab208c740b4224ebc'
            or p04.get('versionCode') != 6775
            or p04.get('certificateSha256') != 'E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22'
            or protected.get('refs/tags/test-p0-v0.1.0-p0.4^{}') != p04['source']):
        raise ValueError('Immutable P0.4 release identity differs')
    return snapshot


def protected_refs():
    rows = git('ls-remote', '--heads', '--tags', 'origin').splitlines()
    actual = {row.split('\t')[1]: row.split('\t')[0] for row in rows}
    snapshot = compare_refs(json.loads(SNAPSHOT.read_text()), actual)
    subprocess.run(['git', 'merge-base', '--is-ancestor', BASE, 'HEAD'], check=True)
    if git('rev-parse', 'test-p0-v0.1.0-p0.4^{commit}') != snapshot['p04']['source']:
        raise ValueError('Local P0.4 tag differs from the public frozen source')
    return snapshot


def phase_zero_source():
    paths = ['src', 'build.gradle', 'settings.gradle', 'gradle', 'gradlew', 'gradlew.bat',
             'version.properties', 'release-ledger.json', 'proguard-p0.pro']
    if git('diff', '--name-only', BASE, 'HEAD', '--', *paths):
        raise ValueError('Phase 0 must finish before changing product/build/version/test sources')
    for path in Path('src/main/res').rglob('*.xml'):
        ET.parse(path)
    ET.parse('src/main/AndroidManifest.xml')


def write_report(name, value):
    REPORTS.mkdir(parents=True, exist_ok=True)
    (REPORTS / name).write_text(json.dumps(value, indent=2) + '\n')


def prepare():
    trusted()
    config = configuration()
    snapshot = protected_refs()
    source = git('rev-parse', 'HEAD')
    if source != os.environ['GITHUB_SHA']:
        raise ValueError('Checkout differs from the exact triggering commit')
    if config['stage'] == 0:
        phase_zero_source()
    classes = [] if config['fullRegression'] else sorted(set(MANDATORY + config['classes']))
    for name in classes if config['stage'] else []:
        if not Path('src/androidTest/java', *name.split('.')).with_suffix('.java').is_file():
            raise ValueError('Selected test class does not exist: ' + name)
    version = dict(line.split('=', 1) for line in Path('version.properties').read_text().splitlines()
                   if line and not line.startswith('#'))
    plan = {'commit': source, 'branch': BRANCH, 'stage': config['stage'],
            'runId': os.environ['GITHUB_RUN_ID'], 'fullRegression': config['fullRegression'],
            'versionName': version['VERSION_NAME'], 'versionCode': int(version['VERSION_CODE']),
            'classes': classes if config['stage'] else [], 'publishing': False, 'signed': False,
            'protectedRefsSha256': hashlib.sha256(SNAPSHOT.read_bytes()).hexdigest()}
    Path('release-plan.json').write_text(json.dumps(plan, indent=2) + '\n')
    write_report('baseline.json', {'passed': True, 'plan': plan, 'snapshot': snapshot,
                                 'productChanged': config['stage'] != 0,
                                 'androidExecuted': False, 'signingActivated': False})
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a') as target:
            target.write(f"stage={config['stage']}\nfull={str(config['fullRegression']).lower()}\n")
            target.write('classes=' + ','.join(plan['classes']) + '\n')
    print(json.dumps(plan, indent=2))


def helpers():
    suite = unittest.defaultTestLoader.discover('.github/tests')
    result = unittest.TextTestRunner(verbosity=2, buffer=True).run(suite)
    proof = {'tests': result.testsRun, 'executed': result.testsRun - len(result.skipped),
             'failures': len(result.failures), 'errors': len(result.errors),
             'skipped': len(result.skipped),
             'passed': result.wasSuccessful() and result.testsRun > 0 and not result.skipped,
             'sourceCommit': os.environ.get('GITHUB_SHA', git('rev-parse', 'HEAD')),
             'runId': os.environ.get('GITHUB_RUN_ID', 'local')}
    write_report('helpers.json', proof)
    # Gradle clean removes build/reports. Preserve the already executed receipt
    # in this job's private temporary directory, then restore it at the gate.
    backup = Path(os.environ.get('RUNNER_TEMP', 'work')) / 'p05-helper-results.json'
    backup.parent.mkdir(parents=True, exist_ok=True)
    backup.write_text(json.dumps(proof, indent=2) + '\n')
    if not proof['passed']:
        raise SystemExit('Actual pipeline helper regression failed')


def helper_receipt(plan):
    primary = REPORTS / 'helpers.json'
    path = primary if primary.exists() else Path(os.environ.get('RUNNER_TEMP', 'work')) / 'p05-helper-results.json'
    helper = json.loads(path.read_text())
    if (helper.get('passed') is not True or helper.get('executed', 0) <= 0
            or helper.get('sourceCommit') != plan['commit']
            or helper.get('runId') != plan['runId']
            or any(helper.get(key) != 0 for key in ('failures', 'errors', 'skipped'))
            or helper.get('tests') != helper.get('executed')):
        raise ValueError('Exact-source executed helper evidence missing or failed')
    write_report('helpers.json', helper)
    return helper


def gate():
    config = configuration()
    plan = json.loads(Path('release-plan.json').read_text())
    proof = {'passed': False, 'plan': plan}
    try:
        if plan['commit'] != os.environ['GITHUB_SHA'] or plan['stage'] != config['stage']:
            raise ValueError('Validation evidence differs from triggering phase/source')
        helper = helper_receipt(plan)
        proof['helpers'] = helper
        if config['stage'] == 0:
            phase_zero_source()
            previous = json.loads((REPORTS / 'p04-apk.json').read_text())
            expected = json.loads(SNAPSHOT.read_text())['p04']
            if (previous.get('passed') is not True or previous.get('signatureVerifiedBy') != 'apksigner'
                    or previous.get('identityVerifiedBy') != 'apkanalyzer'
                    or previous.get('oldApkRebuilt') is not False
                    or previous.get('oldApkSignedAgain') is not False
                    or any(previous.get(key) != expected[key] for key in (
                        'source', 'tag', 'applicationId', 'versionName', 'versionCode',
                        'apkSha256', 'certificateSha256', 'apkBytes'))):
                raise ValueError('Independent public P0.4 APK verification is missing')
            proof['p04'] = previous
            proof.update(passed=True, androidExecuted=False, android='NOT_RUN_PHASE_0')
        else:
            if config['fullRegression']:
                android = inspect(Path('build/outputs/androidTest-results'), Path('src/androidTest/java'))
            else:
                with tempfile.TemporaryDirectory() as directory:
                    selected = Path(directory)
                    scoped_sources(plan['classes'], selected)
                    android = inspect(Path('build/outputs/androidTest-results'), selected)
            unit = inspect(Path('build/test-results/testReproducibleDebugUnitTest'),
                           Path('src/test/java'), require_environment=False)
            proof.update(android=android, unit=unit, androidExecuted=android['executed'] > 0,
                         passed=android['passed'] and unit['passed'])
            layout = json.loads(Path('build/reports/p05-visual/p05-layout-matrix.json').read_text())
            original_matrix = json.loads(Path('build/reports/p04-visual/p04-visual-matrix.json').read_text())
            for matrix, count in ((layout, 48), (original_matrix, 192)):
                if (matrix.get('passed') is not True or matrix.get('sourceCommit') != plan['commit']
                        or len(matrix.get('screenshots', [])) != count):
                    raise ValueError('Matching complete native UI evidence is required')
            proof['nativeVisualEvidence'] = {'p05Layouts': 48, 'originalStates': 192,
                                           'humanReview': 'REQUIRED_AFTER_DOWNLOAD'}
            if config['stage']>=2:
                metrics=json.loads(Path('build/reports/p05-metrics/metric-matrix.json').read_text())
                if metrics.get('sourceCommit')!=plan['commit'] or metrics.get('passed') is not True or len(metrics.get('screenshots',[]))!=8:raise ValueError('Actual eight-palette metric screenshots missing')
                proof['metricScreenshots']=8
            if config['stage']>=3:
                settings=json.loads(Path('build/reports/p05-settings/settings-matrix.json').read_text())
                if settings.get('sourceCommit')!=plan['commit'] or settings.get('passed') is not True or len(settings.get('screenshots',[]))!=16:raise ValueError('Actual eight-palette settings screenshots missing')
                proof['settingsScreenshots']=16
            if config['stage'] in (2,5):
                process=json.loads(Path('build/reports/p0-durability/result.json').read_text())
                if (not process.get('forceStopped') or process.get('timezone')!='Asia/Singapore' or not process.get('passed') or not process.get('p05MetricProbe') or process.get('commit')!=plan['commit'] or process['phases']['write']['pid']==process['phases']['read']['pid']):
                    raise ValueError('Actual separate-process metric persistence evidence missing')
                for phase in ('write','read'):
                    if any(process['phases'][phase].get(key)!=value for key,value in {'tests':1,'executed':1,'failures':0,'errors':0,'skipped':0}.items()):raise ValueError('Incomplete actual process probe execution')
                proof['metricProcessDurability']=process
            # Final process/GPX/offline gates remain mandatory even if Gradle itself is green.
            if config['stage'] == 5:
                for key, location in [('processDurability', 'p0-durability'),
                                      ('gpxSchema', 'p0-gpx'), ('offline', 'p0-offline')]:
                    row = json.loads(Path('build/reports', location, 'result.json').read_text())
                    if row.get('passed') is not True or row.get('commit') != plan['commit']:
                        raise ValueError('Matching final evidence missing: ' + key)
                    proof[key] = row
    except (ValueError, KeyError, OSError, ET.ParseError) as error:
        proof.update(passed=False, diagnostic=str(error))
    write_report('tests.json', proof)
    print(json.dumps(proof, indent=2))
    if not proof['passed']:
        raise SystemExit('P0.5 actual phase evidence gate failed')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=['prepare', 'helpers', 'gate', 'refs'])
    {'prepare': prepare, 'helpers': helpers, 'gate': gate,
     'refs': protected_refs}[parser.parse_args().command]()
