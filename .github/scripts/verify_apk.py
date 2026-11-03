"""Fail closed before exporting a signed APK; only public build information is written."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET


def run(*args):
    return subprocess.check_output([str(arg) for arg in args], text=True).strip()


def require(condition, message):
    if not condition:
        raise SystemExit(message)


version = dict(line.split('=', 1) for line in Path('version.properties').read_text().splitlines()
               if line and not line.startswith('#'))
expected_name = version['VERSION_NAME']
expected_code = int(version['VERSION_CODE'])
plan = json.loads(Path('release-plan.json').read_text())
require(plan['versionName'] == expected_name and plan['versionCode'] == expected_code,
        'APK version must match the tested plan.')
require(run('git', 'rev-parse', expected_name + '^{commit}') == plan['commit'] == run('git', 'rev-parse', 'HEAD'),
        'Tag must point to the exact tested commit.')
fingerprint = os.environ.get('ANDROID_SIGNING_CERT_SHA256', '').strip().replace(':', '').upper()
require(re.fullmatch(r'[0-9A-F]{64}', fingerprint), 'Invalid configured SHA-256 certificate fingerprint.')
folder = Path('build/outputs/apk/reproducible/release')
metadata = json.loads((folder / 'output-metadata.json').read_text())
require(metadata['applicationId'] == 'com.fox.runner', 'Incorrect output metadata application ID.')
elements = metadata['elements']
require(len(elements) == 1, 'Expected exactly one release APK.')
element = elements[0]
require(element['versionCode'] == expected_code and element['versionName'] == expected_name,
        'Incorrect output metadata version.')
apk = (folder / element['outputFile']).resolve()
require(apk.parent == folder.resolve() and apk.suffix == '.apk' and apk.is_file(), 'Invalid APK path.')
require(len(list(folder.glob('*.apk'))) == 1, 'Unexpected additional APK.')
sdk = Path(os.environ['ANDROID_HOME'])
signer = sdk / 'build-tools/36.0.0/apksigner'
analyzer = Path(shutil.which('apkanalyzer') or sdk / 'cmdline-tools/latest/bin/apkanalyzer')
certificate = run(signer, 'verify', '--verbose', '--print-certs', '--min-sdk-version', '26', apk)
actual = re.findall(r'^Signer #\d+ certificate SHA-256 digest:\s*([0-9a-fA-F]+)\s*$', certificate, re.M)
require(len(actual) == 1 and actual[0].upper() == fingerprint, 'Signing certificate mismatch.')
require('CN=Android Debug' not in certificate, 'Debug signing certificate is forbidden.')
require(run(analyzer, 'manifest', 'application-id', apk) == 'com.fox.runner', 'Incorrect APK application ID.')
require(run(analyzer, 'manifest', 'version-name', apk) == expected_name, 'Incorrect APK version name.')
require(int(run(analyzer, 'manifest', 'version-code', apk)) == expected_code, 'Incorrect APK version code.')
require(run(analyzer, 'manifest', 'debuggable', apk).lower() == 'false', 'Release APK must not be debuggable.')
manifest = ET.fromstring(run(analyzer, 'manifest', 'print', apk))
android = '{http://schemas.android.com/apk/res/android}'
for item in manifest.iter():
    for attr in ('authorities', 'targetPackage'):
        value = item.get(android + attr, '')
        require('de.dennisguse.opentracks' not in value, 'An upstream application identity remains in the manifest.')
    if item.tag == 'action':
        require(not item.get(android + 'name', '').startswith('de.dennisguse.opentracks.'),
                'An upstream application action remains in the manifest.')
# Class names and Java namespace intentionally remain upstream-compatible.
# Decode the packaged shortcut XML too: checking only source XML misses flavor overrides.
shortcut_xml = run(analyzer, 'resources', 'xml', '--file', 'res/xml/shortcuts.xml', apk)
require('de.dennisguse.opentracks.playstore' not in shortcut_xml, 'Upstream shortcut package remains.')
require(not re.search(r'targetPackage[^\n]*de\.dennisguse\.opentracks', shortcut_xml),
        'Upstream shortcut package remains.')
require('com.fox.runner' in shortcut_xml or '@string/applicationId' in shortcut_xml
        or re.search(r'targetPackage[^\n]*@(?:0x)?[0-9a-fA-F]+', shortcut_xml),
        'Cannot establish shortcut package binding.')
resolved_package = run(analyzer, 'resources', 'value', '--config', 'default',
                       '--name', 'applicationId', '--type', 'string', apk).strip('"')
require(resolved_package == 'com.fox.runner', 'Incorrect resolved shortcut application ID resource.')
out = Path('dist')
require(not out.exists(), 'Output directory must be fresh.')
out.mkdir()
filename = f'runner-gardenia-{expected_name}-{expected_code}.apk'
shutil.copyfile(apk, out / filename)
checksum = hashlib.sha256((out / filename).read_bytes()).hexdigest()
(out / 'SHA256SUMS').write_text(f'{checksum}  {filename}\n')
info = {
    'applicationId': 'com.fox.runner', 'versionName': expected_name, 'versionCode': expected_code,
    'certificateSha256': fingerprint, 'apkSha256': checksum,
    'commit': run('git', 'rev-parse', 'HEAD'), 'variant': 'reproducibleRelease',
    'gradle': '9.8.2', 'agp': '9.5.0', 'jdk': 'Temurin 21',
    'sdk': '37', 'buildTools': '36.0.0',
    'run': f"https://github.com/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}",
    'runAttempt': os.environ.get('GITHUB_RUN_ATTEMPT', '1'),
    'plan': plan,
}
(out / 'build-info.json').write_text(json.dumps(info, indent=2) + '\n')
(out / 'release-notes.md').write_text(
    f'Runner Gardenia {expected_name} — 测试预发布版本\n\n'
    f'- Application ID: `com.fox.runner`\n- Version Code: `{expected_code}`\n'
    f'- Commit: `{info["commit"]}`\n- Build: {info["run"]}\n'
    f'- Variant: `reproducibleRelease`; JDK 21; Gradle 9.8.2; AGP 9.5.0; SDK 37; Build Tools 36.0.0\n'
    f'- Signing certificate SHA-256: `{fingerprint}`\n- APK SHA-256: `{checksum}`\n\n'
    '下载 APK 后核对 SHA256SUMS，在 Android 8.0 或更新系统上允许对应来源安装。\n'
    '可与官方 OpenTracks 并存；原应用数据不会自动迁移。\n'
    '后续更新应保留应用，不要卸载；包名与证书保持相同，Version Code 必须递增。\n'
)
print(json.dumps(info, indent=2))
