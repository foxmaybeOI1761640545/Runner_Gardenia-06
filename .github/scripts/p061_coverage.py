"""Reproducible geometry-wide reader diagnostics; never equates empty tiles with corruption."""
import csv,hashlib,json,math,os,subprocess,urllib.request
from pathlib import Path
from shapely.geometry import shape,Point,box

def run():
 out=Path('output');geometry=out/'target-region.geojson';manifest=json.loads((out/'geometry-manifest.json').read_text())
 raw=geometry.read_bytes();digest=hashlib.sha256(raw).hexdigest()
 if digest!=manifest['geometrySha256']:raise ValueError('Geometry digest mismatch')
 g=shape(json.loads(raw)['features'][0]['geometry'])
 if not g.is_valid or g.is_empty:raise ValueError('Invalid administrative union')
 if any(not g.covers(Point(row['test_point_lat_lon'][1],row['test_point_lat_lon'][0])) for row in manifest['selected']):raise ValueError('Required target center outside actual geometry')
 z=12;n=2**z
 def ty(lat):return int((1-math.asinh(math.tan(math.radians(lat)))/math.pi)/2*n)
 def lat(y):return math.degrees(math.atan(math.sinh(math.pi*(1-2*y/n))))
 points=[]
 for x in range(int((g.bounds[0]+180)/360*n),int((g.bounds[2]+180)/360*n)+1):
  for y in range(ty(g.bounds[3]),ty(g.bounds[1])+1):
   west,east=x/n*360-180,(x+1)/n*360-180;south,north=lat(y+1),lat(y)
   if g.intersects(box(west,south,east,north)):points.append((lat(y+.5),(west+east)/2))
 coords=out/'intersecting-tiles.csv';coords.write_text('\n'.join(f'{a},{b}' for a,b in points))
 jars=Path('work/reader');jars.mkdir(parents=True,exist_ok=True)
 expected={'core':'72d7782b1d1ccb534060c8be3aee03c2c192f85f78c68e143c056e2b4cb7e383','map':'a070a7beaa5e1b32424f4adc1604e74b7aa7949794cd22e6b8cf2bbc13180714','map-reader':'f821f519f07e0dcbe46fec01ca4d54ea20749b0b1fd0d806084f3ee017543dd9'}
 for name,sha in expected.items():
  artifact='mapsforge-'+name;url=f'https://repo.maven.apache.org/maven2/org/mapsforge/{artifact}/0.25.0/{artifact}-0.25.0.jar';data=urllib.request.urlopen(url,timeout=60).read()
  if hashlib.sha256(data).hexdigest()!=sha:raise ValueError('Reader dependency bytes differ')
  (jars/(name+'.jar')).write_bytes(data)
 cp=os.pathsep.join(str(p) for p in jars.glob('*.jar'));file=Path('work/real-map/RunnerGardenia_GD_PRD_Zhangzhou_Dongshan.map')
 for cls,filename in [('MapCoverageProbe','reader-coverage.csv'),('MapRoadConnectivity','road-connectivity.csv')]:
  result=subprocess.run(['java','-Xmx2g','-cp',cp,'tools/offline-map/'+cls+'.java',str(file),str(coords)],check=True,text=True,capture_output=True,timeout=600)
  (out/filename).write_text(result.stdout);(out/(cls+'.log')).write_text(result.stderr)
 rows=list(csv.DictReader((out/'reader-coverage.csv').open()));roads=list(csv.DictReader((out/'road-connectivity.csv').open()))
 if len(rows)!=len(points) or any(r['readable']!='true' for r in rows):raise ValueError('Actual geometry-intersecting tiles must all be readable')
 proof={'passed':True,'sourceCommit':os.environ['GITHUB_SHA'],'mapSha256':'5412067262dace98d9f95b02621446516e7d98e1debbb6a314d575cae022ee1e','geometrySha256':digest,'geometryValid':True,'geometryConnectedComponents':len(g.geoms) if hasattr(g,'geoms') else 1,'zoom':z,'allIntersectingTiles':len(rows),'unreadable':0,'emptyFeatureTiles':sum(int(r['ways'])==0 and int(r['pois'])==0 for r in rows),'excludedCityCenters':manifest['excludedCityCenters'],'roadGraph':roads,'roadGraphLimit':'Undirected rendered highway geometry at z12, quantized about 1m; filtered/simplified roads and nearest isolated footpaths can split components. This is not turn-by-turn route validation.','emptyTileLimit':'Valid empty data is not automatically a map omission; coastline/land classification and native visual evidence remain separate.','wholeRegionFullyAccepted':False}
 (out/'coverage-result.json').write_text(json.dumps(proof,indent=2)+'\n');print(json.dumps(proof,indent=2))
if __name__=='__main__':run()
