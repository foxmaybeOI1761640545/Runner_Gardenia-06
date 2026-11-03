"""Record the identity of the actual Debug APK before uploading it."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess

version = dict(line.split('=', 1) for line in Path('version.properties').read_text().splitlines() if line)
folder = Path('build/outputs/apk/reproducible/debug')
metadata = json.loads((folder / 'output-metadata.json').read_text())
assert len(metadata['elements']) == 1
entry = metadata['elements'][0]
apk = (folder / entry['outputFile']).resolve()
assert apk.parent == folder.resolve() and apk.is_file()
analyzer = shutil.which('apkanalyzer')
assert analyzer, 'apkanalyzer must be installed by SDK setup'
def manifest(field):
    return subprocess.check_output([analyzer, 'manifest', field, str(apk)], text=True).strip()
info = {'applicationId': manifest('application-id'), 'versionName': manifest('version-name'),
        'versionCode': int(manifest('version-code')), 'variant': metadata['variantName'],
        'debuggable': manifest('debuggable'), 'sha256': hashlib.sha256(apk.read_bytes()).hexdigest()}
assert info['applicationId'] == metadata['applicationId'] == 'com.fox.runner.debug'
assert info['versionName'] == entry['versionName'] == version['VERSION_NAME']
assert info['versionCode'] == entry['versionCode'] == int(version['VERSION_CODE'])
assert info['variant'] == 'reproducibleDebug' and info['debuggable'] == 'true'
subprocess.run([os.environ['ANDROID_HOME'] + '/build-tools/36.0.0/apksigner', 'verify', str(apk)], check=True)
folder = Path('build/reports/ci-validation')
folder.mkdir(parents=True, exist_ok=True)
(folder / 'debug-apk.json').write_text(json.dumps(info, indent=2) + '\n')
summary = '## Verified Debug APK\n\n```json\n' + json.dumps(info, indent=2) + '\n```\n'
print(summary)
if os.environ.get('GITHUB_STEP_SUMMARY'):
    with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as target:
        target.write(summary)
