"""Require actual native pixels and configuration/state assertions for all eight schemes."""
import hashlib,json,shutil,zipfile
from pathlib import Path
from collect_p02_home_ui import png_size
FAMILIES=['precision','quiet','explorer','neon']
PAGES=['home','recording','heart','history','detail','dialog','settings']
def collect(commit):
    roots=list(Path('build/outputs').glob('**/p0-ui'))
    if len(roots)!=1:raise ValueError('Native screenshot output missing/ambiguous')
    root=roots[0];target=Path('build/reports/p03-theme');target.mkdir(parents=True,exist_ok=True);schemes=[]
    for family in FAMILIES:
        for mode in ['light','dark']:
            filename=f'p03-{family}-{mode}.json';proof=json.loads((root/filename).read_text())
            if proof.get('sourceCommit')!=commit or proof.get('api',0)<35 or proof.get('timezone')!='Asia/Singapore' or proof.get('passed') is not True:raise ValueError('Wrong native source/API/timezone or failed scheme')
            for key in ['systemSwitchPreservesRecording','systemSwitchPreservesGps','systemSwitchPreservesConnection','viewModelRetained','pausedStatePreserved','waitingCreatesNoWorkout','dialogRestored']:
                if proof.get(key) is not True:raise ValueError('Missing state assertion: '+key)
            rows=proof['screenshots'];expected={f'p03-{family}-{mode}-{page}.png' for page in PAGES}
            if len(rows)!=7 or {v['file'] for v in rows}!=expected:raise ValueError('Incomplete native page matrix')
            for row in rows:
                data=(root/row['file']).read_bytes();width,height=png_size(data)
                if min(width,height)<300 or row['nightMode']!=(32 if mode=='dark' else 16) or hashlib.sha256(data).hexdigest()!=row['sha256']:raise ValueError('Invalid native PNG/configuration/hash')
                shutil.copyfile(root/row['file'],target/row['file'])
            shutil.copyfile(root/filename,target/filename);schemes.append(proof)
    result={'passed':True,'commit':commit,'schemes':schemes,'screenshots':56,'policy':'FOLLOW_SYSTEM','physicalBleTested':False}
    (target/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    with zipfile.ZipFile(target/'debug-theme-screenshots.zip','w',zipfile.ZIP_DEFLATED) as archive:
        for p in sorted(target.iterdir()):
            if p.suffix in ('.png','.json'):archive.write(p,p.name)
    print('PASS: eight actual native appearance schemes; 56 screenshots; OS/recording/GPS/connection/page/dialog assertions')
    return result
