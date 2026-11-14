"""Verify the immutable real-map artifact and collect exact-source native evidence."""
import hashlib,json,os,shutil,sys
from pathlib import Path
NAME='RunnerGardenia_GD_PRD_Zhangzhou_Dongshan.map'
SHA='5412067262dace98d9f95b02621446516e7d98e1debbb6a314d575cae022ee1e'
BYTES=105834033

def sha(path):
 h=hashlib.sha256()
 with path.open('rb') as f:
  for b in iter(lambda:f.read(65536),b''):h.update(b)
 return h.hexdigest()

def fixture():
 root=Path('work/real-map');file=root/NAME
 if file.stat().st_size!=BYTES or sha(file)!=SHA:raise ValueError('Actual map size/digest mismatch')
 manifest=json.loads((root/'build-manifest.json').read_text())
 if manifest['map']!={'filename':NAME,'bytes':BYTES,'sha256':SHA}:raise ValueError('Map manifest identity differs')
 if manifest['mapsforge_reader_version']!='0.25.0' or manifest['mapsforge_writer_version']!='0.25.0':raise ValueError('Unpinned map reader/writer')
 text=(root/'ATTRIBUTION.txt').read_text()
 if 'OpenStreetMap contributors' not in text or 'https://www.openstreetmap.org/copyright' not in text:raise ValueError('Missing OSM attribution')
 target=Path('src/androidTest/assets/p061/real.map');target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(file,target)
 if 'GITHUB_ENV' in os.environ:
  with open(os.environ['GITHUB_ENV'],'a') as out:out.write('RG_REAL_MAP='+str(file.resolve())+'\n')
 print(json.dumps({'mapBytes':BYTES,'mapSha256':SHA,'testApkOnly':True,'productionApkContainsMap':False}))

def collect():
 roots=list(Path('build/outputs').glob('**/p0-ui'));hits=[p/'p061-real-map-both.json' for p in roots if (p/'p061-real-map-both.json').is_file()]
 if len(hits)!=1:raise ValueError('Missing unambiguous real-file native receipt')
 path=hits[0];r=json.loads(path.read_text())
 if r.get('sourceCommit')!=os.environ['GITHUB_SHA'] or r.get('passed') is not True or r.get('privateFileShaVerified') is not True or r.get('airplaneMode') is not True or r.get('realMapSwitchAndDeletePassed') is not True or r.get('realMapNativeZoomAndPan') is not True or r.get('mapSha256')!=SHA or r.get('mapBytes')!=BYTES or len(r.get('screenshots',[]))!=51:raise ValueError('Incomplete exact-source actual-map receipt')
 out=Path('build/reports/p061-real-map');out.mkdir(parents=True,exist_ok=True)
 rows=[]
 for row in r['screenshots']+[{'file':'p061-real-route.png'},{'file':'p061-real-recreated.png'}]:
  if 'diagnostic' in row and any(t['readerSupportsTile'] and t['rendererTileState']!='CACHED' for t in row['diagnostic']['visibleTiles']):raise ValueError('Partial viewport cannot pass actual map screenshot acceptance')
  name=row['file']
  if Path(name).name!=name:raise ValueError('Unsafe screenshot path')
  f=path.parent/name
  if not f.read_bytes().startswith(b'\x89PNG\r\n\x1a\n'):raise ValueError('Missing native screenshot')
  shutil.copyfile(f,out/name);rows.append({**row,'sha256':sha(f)})
 r['screenshots']=rows;(out/'result.json').write_text(json.dumps(r,indent=2)+'\n');print(json.dumps({'passed':True,'screenshots':len(rows),'sourceCommit':r['sourceCommit']}))
 return r
if __name__=='__main__':{'fixture':fixture,'collect':collect}[sys.argv[1]]()
