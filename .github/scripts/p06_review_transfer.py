"""Transfer immutable synthetic CI evidence through GitHub's repository blob API.

Some review environments cannot reach Actions' Azure artifact storage. Preserve the
original Artifact and add a checksum-bound transport, without updating any Git ref,
committing binary evidence, accessing Secrets, or changing the tested source.
"""
import base64
import hashlib
import io
import json
import os
import sys
from pathlib import Path
import zipfile
from publish_release import GitHub

CHUNK = 512 * 1024


def run():
    if (os.environ['GITHUB_EVENT_NAME'] != 'push'
            or os.environ['GITHUB_REF'] != 'refs/heads/feature/p0-6-offline-map'):
        raise ValueError('Only a trusted source push can transfer evidence')
    root = Path('evidence')
    signed = sys.argv[1:] == ['--signed']
    if signed:
        info = json.loads((root / 'build-info.json').read_text())
        approval = json.loads(Path('.github/p06/visual-approval.json').read_text())
        runtime = json.loads((root / 'runtime-smoke.json').read_text())
        native = json.loads((root / 'native-theme-smoke.json').read_text())
        if (info['buildWorkflowCommit'] != os.environ['GITHUB_SHA']
                or info['commit'] != approval['source'] or info['testRunId'] != approval['testRunId']
                or runtime.get('passed') is not True or native.get('passed') is not True
                or hashlib.sha256((root / 'RunnerGardenia-P0-Release.apk').read_bytes()).hexdigest() != info['apkSha256']):
            raise ValueError('Signed review transport differs from accepted source/APK')
        source, accepted = info['commit'], True
    else:
        plan = json.loads((root / 'release-plan.json').read_text())
        report = json.loads((root / 'build/reports/p06-validation/tests.json').read_text())
        if plan['commit'] != os.environ['GITHUB_SHA'] or report['plan'] != plan:
            raise ValueError('Exact-source diagnostic evidence is required')
        source, accepted = plan['commit'], report['passed'] is True
    raw = io.BytesIO()
    with zipfile.ZipFile(raw, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(root.rglob('*')):
            if path.is_file():
                if path.suffix.lower() in ('.jks', '.keystore'):
                    raise ValueError('Signing key files are forbidden in review evidence')
                if path.suffix.lower() in ('.apk', '.apks', '.aab') or path.name.endswith('.apk.idsig'):
                    continue  # Binaries remain exclusively in original Artifacts/Releases.
                archive.write(path, path.relative_to(root))
    payload = raw.getvalue()
    api = GitHub()
    chunks = []
    for offset in range(0, len(payload), CHUNK):
        part = payload[offset:offset + CHUNK]
        result = api.request(api.base + '/git/blobs', 'POST', {
            'encoding': 'base64', 'content': base64.b64encode(part).decode('ascii')})
        chunks.append({'blob': result['sha'], 'bytes': len(part), 'sha256': hashlib.sha256(part).hexdigest()})
    index = {'source': source, 'workflowCommit': os.environ['GITHUB_SHA'], 'evidencePassed': accepted, 'signedReview': signed, 'signedApkIncluded': False, 'runId': os.environ['GITHUB_RUN_ID'],
             'bytes': len(payload), 'sha256': hashlib.sha256(payload).hexdigest(),
             'chunks': chunks, 'originalArtifactPreserved': True,
             'gitRefUpdated': False, 'signingKeyMaterialIncluded': False}
    result = api.request(api.base + '/git/blobs', 'POST', {
        'encoding': 'utf-8', 'content': json.dumps(index, indent=2) + '\n'})
    index['indexBlob'] = result['sha']
    Path('review-transfer.json').write_text(json.dumps(index, indent=2) + '\n')
    with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
        output.write('index=' + result['sha'] + '\n')
    print(json.dumps(index, indent=2))


if __name__ == '__main__': run()
