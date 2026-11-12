"""Require exact-source real offline map tiles, feature assertions and native screenshots."""
import hashlib,json,shutil
from pathlib import Path

def collect(source):
    roots=list(Path('build/outputs').glob('**/p0-ui'))
    matches=[root for root in roots if (root/'p06-offline-map.json').is_file()]
    if len(matches)!=1:raise ValueError('Missing or ambiguous actual offline-map receipt')
    root=matches[0];receipt=json.loads((root/'p06-offline-map.json').read_text())
    validate_summary(receipt,source)
    rows=receipt['screenshots']
    target=Path('build/reports/p06-offline-map');target.mkdir(parents=True,exist_ok=True)
    for row in rows:
        path=root/row['file']
        if row.get('native') is not True or row.get('renderedTiles',0)<=0 or hashlib.sha256(path.read_bytes()).hexdigest()!=row['sha256'] or not path.read_bytes().startswith(b'\x89PNG\r\n\x1a\n'):raise ValueError('Actual native rendered-map image missing or altered')
        shutil.copy2(path,target/path.name)
    shutil.copy2(root/'p06-offline-map.json',target/'p06-offline-map.json')
    return receipt


def validate_summary(receipt,source):
    required=['passed','airplaneMode','safImport','zoomAndPan','currentPositionWithoutWorkout','liveTrack','historicalTrack','segmentBreaks','recreation','invalidFileRetainsMap']
    if receipt.get('sourceCommit')!=source or any(receipt.get(key) is not True for key in required):raise ValueError('Incomplete or stale actual map acceptance')
    names={'p06-'+name+'.png' for name in ['imported-airplane','zoom-pan','recreated-airplane','current-position','live-track','segmented-track','historical-track','invalid-file-retains-map']}
    names|={'p06-theme-'+theme+'-'+mode+'.png' for theme in ['PRECISION','QUIET','EXPLORER','NEON'] for mode in ['dark','light']}
    rows=receipt.get('screenshots',[])
    if len(rows)!=16 or {row['file'] for row in rows}!=names:raise ValueError('Incomplete native map screenshot matrix')
