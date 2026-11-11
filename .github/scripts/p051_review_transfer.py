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
from pathlib import Path
import zipfile
from publish_release import GitHub

CHUNK = 512 * 1024


def run():
    if (os.environ['GITHUB_EVENT_NAME'] != 'push'
            or os.environ['GITHUB_REF'] != 'refs/heads/feature/p0-5-1-gps-motion-quality'):
        raise ValueError('Only a trusted source push can transfer evidence')
    root = Path('evidence')
    plan = json.loads((root / 'release-plan.json').read_text())
    report = json.loads((root / 'build/reports/p051-validation/tests.json').read_text())
    if plan['commit'] != os.environ['GITHUB_SHA'] or report['passed'] is not True or report['plan'] != plan:
        raise ValueError('Only successful exact-source full evidence may be transferred')
    raw = io.BytesIO()
    with zipfile.ZipFile(raw, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(root.rglob('*')):
            if path.is_file():
                archive.write(path, path.relative_to(root))
    payload = raw.getvalue()
    api = GitHub()
    chunks = []
    for offset in range(0, len(payload), CHUNK):
        part = payload[offset:offset + CHUNK]
        result = api.request(api.base + '/git/blobs', 'POST', {
            'encoding': 'base64', 'content': base64.b64encode(part).decode('ascii')})
        chunks.append({'blob': result['sha'], 'bytes': len(part), 'sha256': hashlib.sha256(part).hexdigest()})
    index = {'source': plan['commit'], 'runId': os.environ['GITHUB_RUN_ID'],
             'bytes': len(payload), 'sha256': hashlib.sha256(payload).hexdigest(),
             'chunks': chunks, 'originalArtifactPreserved': True,
             'gitRefUpdated': False, 'signedMaterialIncluded': False}
    result = api.request(api.base + '/git/blobs', 'POST', {
        'encoding': 'utf-8', 'content': json.dumps(index, indent=2) + '\n'})
    index['indexBlob'] = result['sha']
    Path('review-transfer.json').write_text(json.dumps(index, indent=2) + '\n')
    with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
        output.write('index=' + result['sha'] + '\n')
    print(json.dumps(index, indent=2))


if __name__ == '__main__': run()
