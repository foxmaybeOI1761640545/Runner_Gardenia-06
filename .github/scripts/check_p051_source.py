"""P0.5.1 exact PR-base diff: explicit GPS integration allowlist, frozen unrelated code."""
import json
import os
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
BASE_REF = 'refs/remotes/origin/feature/p0-5-dashboard-settings-history'
JAVA = 'src/main/java/de/dennisguse/opentracks/'
ALLOWED_PRODUCT = {
    JAVA + name for name in (
        'p0/gps/GpsMotionFilter.java', 'p0/gps/GpsReadiness.java', 'p0/gps/MotionFormat.java',
        'p0/app/DashboardState.java', 'sensors/GpsHandler.java',
        'services/handlers/TrackPointCreator.java', 'services/TrackRecordingManager.java',
        'data/statistics/TrackStatisticsUpdater.java')
}
ALLOWED_TESTS = {
    'src/' + tree + '/java/de/dennisguse/opentracks/' + name
    for tree, names in (
        ('test', ('p0/GpsMotionFilterTest.java', 'p0/GpsReadinessTest.java', 'p0/MotionFormatTest.java')),
        ('androidTest', ('p0/P0GpsRecordingTest.java', 'p0/P051GpsUiTest.java')))
    for name in names
}
ALLOWED_INFRA = {
    '.github/workflows/android-build.yml', '.github/workflows/p051-gps-motion-quality.yml',
    '.github/scripts/check_p051_source.py', '.github/scripts/release_policy.py',
    '.github/scripts/p05_version.py', 'release-ledger.json', 'version.properties',
    *('.github/scripts/p051_' + name + '.py' for name in (
        'pipeline', 'release', 'version', 'signing', 'publish', 'publication',
        'release_smoke', 'release_theme_ui')),
}


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()


def allowed(path):
    return (path in ALLOWED_PRODUCT | ALLOWED_TESTS | ALLOWED_INFRA
            or path.startswith(('.github/p051/', 'docs/p0-5-1/'))
            or (path.startswith('.github/tests/test_p051_') and path.endswith('.py')))


def verify_changes(rows):
    if not rows:
        raise ValueError('Empty P0.5.1 diff')
    for status, path in rows:
        if status not in ('A', 'M') or not allowed(path):
            raise ValueError('Unreviewed P0.5.1 change: ' + status + ' ' + path)


def resolve_base(environment=None):
    environment = os.environ if environment is None else environment
    if environment.get('GITHUB_EVENT_NAME') == 'pull_request':
        event = json.loads(Path(environment['GITHUB_EVENT_PATH']).read_text())['pull_request']
        if (event['base']['ref'] != 'feature/p0-5-dashboard-settings-history'
                or event['head']['ref'] != 'feature/p0-5-1-gps-motion-quality'
                or event['head']['sha'] != environment['GITHUB_SHA']):
            raise ValueError('Unexpected PR source or base')
        return event['base']['sha']
    return git('rev-parse', BASE_REF)


def check():
    base = resolve_base()
    head = os.environ.get('GITHUB_SHA', git('rev-parse', 'HEAD'))
    if head != git('rev-parse', 'HEAD'):
        raise ValueError('Source checkout differs from the tested HEAD')
    subprocess.run(['git', 'merge-base', '--is-ancestor', base, head], cwd=ROOT, check=True)
    rows = [line.split('\t') for line in git('diff', '--no-renames', '--name-status', base, head).splitlines()]
    verify_changes(rows)
    # A patch cannot silently alter theme layouts, palettes, GPX/export, BLE,
    # database schemas, dependencies, or old phase workflows.
    for path in ROOT.joinpath('src/main/res/layout').glob('*home*.xml'):
        ET.parse(path)
    for mode in ('values', 'values-night'):
        path = f'src/main/res/{mode}/gardenia_colors.xml'
        original = subprocess.check_output(['git', 'show', base + ':' + path], cwd=ROOT)
        if original != ROOT.joinpath(path).read_bytes():
            raise ValueError('Existing paired theme colors changed')
    report = {'passed': True, 'base': base, 'head': head,
              'changes': [dict(status=status, path=path) for status, path in rows]}
    target = ROOT / 'build/reports/p051-validation/source-scope.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    check()
