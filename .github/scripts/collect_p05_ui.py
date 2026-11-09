"""Verify exact-source native P0.5 screenshots before human review."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import struct

FAMILIES = ('precision', 'quiet', 'explorer', 'neon')
VIEWPORTS = ((360, 800), (393, 873), (412, 915))


def collect(source, root=None, destination=Path('build/reports/p05-visual')):
    if root is None:
        roots = list(Path('build/outputs').glob('**/p0-ui'))
        if len(roots) != 1:
            raise ValueError('Actual API35 P0.5 output is missing or ambiguous')
        root = roots[0]
    proof = json.loads((root / 'p05-layout-matrix.json').read_text())
    if (proof.get('passed') is not True or proof.get('sourceCommit') != source
            or proof.get('api') != 35 or proof.get('native') is not True
            or proof.get('policy') != 'FOLLOW_SYSTEM'
            or proof.get('productionFixtureEntry') is not False):
        raise ValueError('Native layout evidence source/API/policy differs')
    rows = proof['screenshots']
    expected = {(f, m, w, h, scale) for f in FAMILIES for m in ('light', 'dark')
                for w, h in VIEWPORTS for scale in (1.0, 1.3)}
    identities = [(r['family'], r['appearance'], r['widthDp'], r['heightDp'],
                   float(r['fontScale'])) for r in rows]
    if len(identities) != 48 or len(set(identities)) != 48 or set(identities) != expected:
        raise ValueError('Required four-theme/eight-color/three-width/two-font matrix incomplete')
    destination.mkdir(parents=True, exist_ok=True)
    for row in rows:
        name = row['file']
        path = root / name
        if path.parent != root or path.suffix != '.png' or row.get('syntheticUiFixture') is not True:
            raise ValueError('Screenshot fixture/path provenance differs')
        raw = path.read_bytes()
        if (raw[:8] != b'\x89PNG\r\n\x1a\n' or raw[12:16] != b'IHDR'
                or hashlib.sha256(raw).hexdigest() != row['sha256']):
            raise ValueError('Actual screenshot bytes/hash differ: ' + name)
        if struct.unpack('>II', raw[16:24]) != (row['widthDp'] * 2, row['heightDp'] * 2):
            raise ValueError('Actual screenshot viewport differs: ' + name)
        shutil.copy2(path, destination / name)
    shutil.copy2(root / 'p05-layout-matrix.json', destination / 'p05-layout-matrix.json')
    print('PASS: 48 source-bound API35 native P0.5 layout screenshots')
    return proof


if __name__ == '__main__':
    collect(os.environ['GITHUB_SHA'])


def collect_metrics(source):
    roots=list(Path('build/outputs').glob('**/p0-ui'))
    if len(roots)!=1:raise ValueError('Metric screenshot output missing/ambiguous')
    root=roots[0];out=Path('build/reports/p05-metrics');out.mkdir(parents=True,exist_ok=True)
    rows=[]
    for family in FAMILIES:
        for mode in ('light','dark'):
            name=f'p05-metric-duration-{family}-{mode}-360-font130-synthetic.png'
            raw=(root/name).read_bytes()
            if raw[:8]!=b'\x89PNG\r\n\x1a\n' or struct.unpack('>II',raw[16:24])!=(720,1600):raise ValueError('Wrong actual long-duration native viewport')
            shutil.copy2(root/name,out/name)
            rows.append({'file':name,'family':family,'appearance':mode,'widthDp':360,'fontScale':1.3,'duration':'125:25:30','sha256':hashlib.sha256(raw).hexdigest(),'syntheticUiFixture':True})
    proof={'passed':True,'sourceCommit':source,'api':35,'native':True,'screenshots':rows}
    (out/'metric-matrix.json').write_text(json.dumps(proof,indent=2)+'\n')
    return proof


def collect_settings(source):
    roots=list(Path('build/outputs').glob('**/p0-ui'))
    if len(roots)!=1:raise ValueError('Native settings output missing/ambiguous')
    root=roots[0];out=Path('build/reports/p05-settings');out.mkdir(parents=True,exist_ok=True)
    rows=[]
    for family in FAMILIES:
        for mode in ('light','dark'):
            for page in ('hub','motion'):
                name=f'p05-settings-{page}-{family}-{mode}-360-font130-native.png'
                raw=(root/name).read_bytes()
                if raw[:8]!=b'\x89PNG\r\n\x1a\n' or struct.unpack('>II',raw[16:24])!=(720,1600):raise ValueError('Wrong native settings viewport')
                shutil.copy2(root/name,out/name)
                rows.append({'file':name,'family':family,'appearance':mode,'page':page,'widthDp':360,'fontScale':1.3,'sha256':hashlib.sha256(raw).hexdigest(),'native':True})
    proof={'passed':True,'sourceCommit':source,'api':35,'native':True,'screenshots':rows}
    (out/'settings-matrix.json').write_text(json.dumps(proof,indent=2)+'\n')
    return proof
