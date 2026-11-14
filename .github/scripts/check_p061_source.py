"""P0.6 exact PR-base diff: explicit GPS integration allowlist, frozen unrelated code."""
import json
import os
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
BASE_REF = 'c7112514565c6b10dac7131b97432328ec67ef1d'
JAVA = 'src/main/java/de/dennisguse/opentracks/'
ALLOWED_PRODUCT = {JAVA + name for name in (
    'p0/app/P0Activity.java','p0/app/P0HistoryActivity.java','p0/app/P05SettingsSectionFragment.java',
    'p0/app/RoutePreviewModel.java','services/TrackRecordingService.java',
    'sensors/GpsHandler.java','sensors/SensorManager.java','sensors/SensorHandler.java','sensors/driver/GpsInternal.java')}
HISTORY_TEST = 'src/androidTest/java/de/dennisguse/opentracks/p0/P05HistoryBatchTest.java'
ALLOWED_INFRA = {HISTORY_TEST,'build.gradle','version.properties','release-ledger.json','src/main/AndroidManifest.xml',
    'src/androidTest/AndroidManifest.xml','src/release/AndroidManifest.xml','.github/workflows/android-build.yml',
    '.github/workflows/p06-offline-map.yml','.github/scripts/release_policy.py','.github/scripts/p051_version.py'}


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()


def allowed(path):
    return (path in ALLOWED_PRODUCT | ALLOWED_INFRA
        or path.startswith((JAVA+'p0/map/', 'src/main/assets/p06-licenses/', '.github/p06/', 'docs/p0-6/', 'tools/p06-map-fixture/',
                            'src/androidTest/assets/p06/', 'src/androidTest/java/de/dennisguse/opentracks/p0/map/'))
        or (path.startswith('.github/scripts/p06_') and path.endswith('.py'))
        or path=='.github/scripts/check_p06_source.py'
        or (path.startswith('.github/tests/test_p06_') and path.endswith('.py'))
        or path in {'src/main/res/layout/p06_offline_map.xml','src/main/res/values/p06_map_strings.xml',
                    'src/main/res/values-zh/p06_map_strings.xml',
                    'src/test/java/de/dennisguse/opentracks/p0/MapViewportTest.java',
                    'src/test/java/de/dennisguse/opentracks/p0/OfflineMapFixtureTest.java'})


_original_allowed=allowed
def allowed(path):
    return (path in ALLOWED_PRODUCT or path in {'version.properties','release-ledger.json','.github/scripts/release_policy.py',
        '.github/workflows/p061-real-offline-map.yml','.github/workflows/build-custom-offline-map.yml',
        '.github/workflows/p061-map-geometry.yml','src/androidTest/AndroidManifest.xml'}
        or path.startswith((JAVA+'p0/map/','.github/p061/','docs/p0-6-1/','tools/offline-map/',
                            'src/androidTest/java/de/dennisguse/opentracks/p0/map/'))
        or path.startswith(('.github/scripts/p061_','.github/tests/test_p061_')) and path.endswith('.py')
        or path=='.github/scripts/check_p061_source.py'
        or path in {'src/main/res/values/p06_map_strings.xml','src/main/res/values-zh/p06_map_strings.xml',
                    'src/test/java/de/dennisguse/opentracks/p0/RealMapFileTest.java'})

def verify_changes(rows):
    if not rows:
        raise ValueError('Empty P0.6 diff')
    for status, path in rows:
        if status not in ('A', 'M') or not allowed(path):
            raise ValueError('Unreviewed P0.6 change: ' + status + ' ' + path)


def focused_history_test(source):
    """Only make the original dialog selectors target actual dialog windows."""
    selectors=('onView(withId(android.R.id.button1))','onView(withId(android.R.id.button2))',
        'onView(withText(R.string.p05_direct_gpx))',
        'onView(withText(context.getString(R.string.p05_delete_confirm,3)))')
    for selector in selectors:
        if selector not in source:raise ValueError('Original history dialog selector missing')
        source=source.replace(selector,selector+'.inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())')
    return source


def verify_history_test(base,head):
    if head!=focused_history_test(base):raise ValueError('Original history tests/assertions may only add explicit dialog roots')


def resolve_base(environment=None):
    environment = os.environ if environment is None else environment
    if environment.get('GITHUB_EVENT_NAME') == 'pull_request':
        event = json.loads(Path(environment['GITHUB_EVENT_PATH']).read_text())['pull_request']
        if (event['base']['ref'] != 'feature/p0-5-1-gps-motion-quality'
                or event['head']['ref'] != 'feature/p0-6-offline-map'
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
    if any(path==HISTORY_TEST for _,path in rows):
        original=subprocess.check_output(['git','show',base+':'+HISTORY_TEST],cwd=ROOT,text=True)
        verify_history_test(original,(ROOT/HISTORY_TEST).read_text())
    # A patch cannot silently alter theme layouts, palettes, GPX/export, BLE,
    # database schemas, or old phase workflows. New map dependencies are explicitly audited.
    for path in ROOT.joinpath('src/main/res/layout').glob('*home*.xml'):
        ET.parse(path)
    for mode in ('values', 'values-night'):
        path = f'src/main/res/{mode}/gardenia_colors.xml'
        original = subprocess.check_output(['git', 'show', base + ':' + path], cwd=ROOT)
        if original != ROOT.joinpath(path).read_bytes():
            raise ValueError('Existing paired theme colors changed')
    report = {'passed': True, 'base': base, 'head': head,
              'changes': [dict(status=status, path=path) for status, path in rows]}
    target = ROOT / 'build/reports/p06-validation/source-scope.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    check()
