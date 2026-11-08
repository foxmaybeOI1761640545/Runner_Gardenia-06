"""Validate every native fixture PNG before it can be used for human visual approval."""
import hashlib,json,os,shutil,struct
from pathlib import Path
FAMILIES=['precision','quiet','explorer','neon']
STATES=['idle','gps-searching','gps-fixed','recording','paused','finished','disconnected','connected-no-sample']
VIEWPORTS=[(360,800),(390,844),(412,915)]
def collect(source):
 roots=list(Path('build/outputs').glob('**/p0-ui'))
 if len(roots)!=1:raise ValueError('Actual native screenshot output missing/ambiguous')
 root=roots[0];proof=json.loads((root/'p04-visual-matrix.json').read_text())
 if proof.get('passed') is not True or proof['sourceCommit']!=source or proof['api']!=35 or proof['policy']!='FOLLOW_SYSTEM' or proof.get('native') is not True or proof.get('productionFixtureEntry') is not False:raise ValueError('Wrong source/API/policy/native fixture evidence')
 expected={(f,m,w,h,s) for f in FAMILIES for m in ['light','dark'] for w,h in VIEWPORTS for s in STATES}
 rows=proof['screenshots'];actual={(r['family'],r['appearance'],r['widthDp'],r['heightDp'],r['state']) for r in rows}
 if len(rows)!=192 or actual!=expected:raise ValueError('Native fixture matrix incomplete')
 out=Path('build/reports/p04-visual');out.mkdir(parents=True,exist_ok=True)
 for r in rows:
  p=root/r['file'];raw=p.read_bytes()
  if p.parent!=root or raw[:8]!=b'\x89PNG\r\n\x1a\n' or hashlib.sha256(raw).hexdigest()!=r['sha256'] or r.get('syntheticUiFixture') is not True:raise ValueError('Invalid native PNG/source provenance')
  width,height=struct.unpack('>II',raw[16:24])
  if (width,height)!=(r['widthDp']*2,r['heightDp']*2):raise ValueError('Requested native viewport was not actually applied')
  shutil.copy2(p,out/p.name)
 shutil.copy2(root/'p04-visual-matrix.json',out/'p04-visual-matrix.json')
 print('PASS: 192 actual API35 fixture PNGs, eight schemes, three real viewport sizes, eight independent states')
 return proof
if __name__=='__main__':collect(os.environ['GITHUB_SHA'])
