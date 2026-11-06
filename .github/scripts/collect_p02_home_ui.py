"""Fail-closed validation of the actual P0.2 UI state/theme/window screenshot matrix."""
import hashlib
import json
from pathlib import Path
import shutil
import struct
import zlib

STATES = {'idle':'IDLE','acquiring':'ACQUIRING_GPS','gps-ready':'RECORDING',
          'recording':'RECORDING','gps-lost':'RECORDING','paused':'PAUSED',
          'resumed':'RECORDING','heart-connected':'RECORDING','heart-disconnected':'RECORDING'}


def png_size(raw):
    if raw[:8]!=b'\x89PNG\r\n\x1a\n':raise ValueError('Not a PNG')
    offset=8;dimensions=None;saw_data=False
    while offset<len(raw):
        if offset+12>len(raw):raise ValueError('Truncated PNG chunk')
        length=struct.unpack('>I',raw[offset:offset+4])[0];kind=raw[offset+4:offset+8]
        end=offset+12+length
        if end>len(raw):raise ValueError('Incomplete PNG payload')
        payload=raw[offset+8:offset+8+length];crc=struct.unpack('>I',raw[offset+8+length:end])[0]
        if zlib.crc32(kind+payload)&0xffffffff!=crc:raise ValueError('PNG checksum mismatch')
        if dimensions is None:
            if kind!=b'IHDR' or length!=13:raise ValueError('Missing PNG header')
            dimensions=struct.unpack('>II',payload[:8])
        if kind==b'IDAT':saw_data=True
        if kind==b'IEND':
            if length or end!=len(raw) or not saw_data:raise ValueError('Incomplete PNG ending')
            return dimensions
        offset=end
    raise ValueError('Missing PNG end')


def inspect(folder, commit):
    data=json.loads((folder/'p02-home-matrix.json').read_text())
    if data.get('sourceCommit')!=commit or data.get('timezone')!='Asia/Singapore' or data.get('physicalBleTested') is not False or data.get('syntheticGps') is not True:
        raise ValueError('UI evidence source/timezone/hardware boundary differs from this run')
    expected={f'p02-home-{size}-{theme}-{state}.png' for size in ('small','large') for theme in ('light','dark') for state in STATES}
    rows=data.get('screenshots',[])
    if len(rows)!=36 or {r['file'] for r in rows}!=expected:raise ValueError('Missing/duplicate/unexpected home state capture')
    for row in rows:
        name=row['file'];raw=(folder/name).read_bytes()
        dimensions=png_size(raw);small='-small-' in name;dark='-dark-' in name
        if dimensions!=((720,1280) if small else (1600,2560)):raise ValueError('Incorrect actual screenshot window dimensions')
        if row.get('nightMode')!=(32 if dark else 16) or (row.get('widthDp',0)>360 if small else row.get('widthDp',0)<600):raise ValueError('Actual theme/window configuration missing')
        state=row.get('state')
        if state not in STATES or name!=f"p02-home-{'small' if small else 'large'}-{'dark' if dark else 'light'}-{state}.png" or row.get('phase')!=STATES[state]:raise ValueError('Capture state does not match the real session')
        if hashlib.sha256(raw).hexdigest()!=row.get('sha256'):raise ValueError('Original screenshot changed after Android capture')
    return {'passed':True,'commit':commit,'timezone':data['timezone'],'physicalBleTested':False,'syntheticGps':True,'screenshots':rows}


def collect(commit):
    roots=list(Path('build/outputs').glob('**/p0-ui'))
    if len(roots)!=1:raise ValueError('Missing/ambiguous actual UI output folder')
    proof=inspect(roots[0],commit)
    destination=Path('build/reports/p0-ui');destination.mkdir(parents=True,exist_ok=True)
    for row in proof['screenshots']:shutil.copyfile(roots[0]/row['file'],destination/row['file'])
    shutil.copyfile(roots[0]/'p02-home-matrix.json',destination/'p02-home-matrix.json')
    folder=Path('build/reports/p02-home');folder.mkdir(parents=True,exist_ok=True)
    (folder/'result.json').write_text(json.dumps(proof,indent=2)+'\n')
    print('Verified 36 actual P0.2 home captures: nine states × two themes × two window sizes; manual image inspection still required.')
    return proof
