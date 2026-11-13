#!/usr/bin/env python3
from __future__ import annotations
import hashlib, json, os, subprocess, unicodedata
from datetime import datetime, timezone
from pathlib import Path
from pyproj import CRS, Transformer
from shapely.geometry import shape, mapping
from shapely.ops import transform, unary_union
from shapely.validation import make_valid

ROOT=Path.cwd(); WORK=Path(os.environ.get("RUNNER_TEMP","/tmp"))/"gardenia-map-build"; OUT=ROOT/"output"
WORK.mkdir(parents=True,exist_ok=True); OUT.mkdir(parents=True,exist_ok=True)
CHINA_URL="https://download.geofabrik.de/asia/china-latest.osm.pbf"
WRITER_URL="https://repo.maven.apache.org/maven2/org/mapsforge/mapsforge-map-writer/0.25.0/mapsforge-map-writer-0.25.0-jar-with-dependencies.jar"
MAP_NAME="RunnerGardenia_GD_PRD_Zhangzhou_Dongshan.map"; MAP_PATH=OUT/MAP_NAME
CITIES={
"广州":["广州市","广州","Guangzhou","Guangzhou City"],"深圳":["深圳市","深圳","Shenzhen","Shenzhen City"],
"珠海":["珠海市","珠海","Zhuhai","Zhuhai City"],"佛山":["佛山市","佛山","Foshan","Foshan City"],
"东莞":["东莞市","东莞","Dongguan","Dongguan City"],"中山":["中山市","中山","Zhongshan","Zhongshan City"],
"江门":["江门市","江门","Jiangmen","Jiangmen City"],"惠州":["惠州市","惠州","Huizhou","Huizhou City"],
"肇庆":["肇庆市","肇庆","Zhaoqing","Zhaoqing City"],"汕尾":["汕尾市","汕尾","Shanwei","Shanwei City"],
"揭阳":["揭阳市","揭阳","Jieyang","Jieyang City"],"汕头":["汕头市","汕头","Shantou","Shantou City"],
"潮州":["潮州市","潮州","Chaozhou","Chaozhou City"],"漳州":["漳州市","漳州","Zhangzhou","Zhangzhou City"]}
TEST_POINTS={"Guangzhou":[23.1291,113.2644],"Shenzhen":[22.5431,114.0579],"Zhuhai":[22.2710,113.5767],
"Foshan":[23.0218,113.1219],"Dongguan":[23.0207,113.7518],"Zhongshan":[22.5159,113.3926],
"Jiangmen":[22.5787,113.0815],"Huizhou":[23.1115,114.4152],"Zhaoqing":[23.0469,112.4651],
"Shanwei":[22.7876,115.3753],"Jieyang":[23.5438,116.3557],"Shantou":[23.3535,116.6822],
"Chaozhou":[23.6567,116.6226],"Zhangzhou":[24.5130,117.6471],"Dongshan Island":[23.7010,117.4290]}

def run(args,**kwargs):
 print("+"," ".join(map(str,args)),flush=True)
 return subprocess.run(list(map(str,args)),check=True,**kwargs)
def sha256(path):
 h=hashlib.sha256()
 with open(path,"rb") as f:
  for b in iter(lambda:f.read(8*1024*1024),b""):h.update(b)
 return h.hexdigest()
def download(url,dest):
 run(["curl","--fail","--location","--retry","5","--retry-all-errors","--connect-timeout","30","--max-time","3600",url,"--output",dest])
def norm(value):
 value=unicodedata.normalize("NFKC",value).casefold().strip()
 for suffix in (" city"," county"," district"," shi","市","地区"):
  if value.endswith(suffix):value=value[:-len(suffix)].strip()
 return "".join(ch for ch in value if ch.isalnum() or "\u4e00"<=ch<="\u9fff")
def polygon_parts(g):
 if g.is_empty:return []
 if g.geom_type=="Polygon":return [g]
 if g.geom_type=="MultiPolygon":return list(g.geoms)
 if g.geom_type=="GeometryCollection":
  out=[]
  for x in g.geoms:out.extend(polygon_parts(x))
  return out
 return []

def main():
 china=WORK/"china-latest.osm.pbf"; admin=WORK/"admin-level-5-7.osm.pbf"
 gj=WORK/"admin-level-5-7.geojson"; target_gj=WORK/"target-region.geojson"
 region=WORK/"target-region.osm.pbf"; writer=WORK/"mapsforge-map-writer-0.25.0-jar-with-dependencies.jar"
 print("Downloading current Geofabrik China PBF.",flush=True)
 download(CHINA_URL,china); china_hash=sha256(china)
 china_info=subprocess.run(["osmium","fileinfo","-e",china],check=True,capture_output=True,text=True).stdout
 run(["osmium","tags-filter","--overwrite","-o",admin,china,"r/admin_level=4","r/admin_level=5","r/admin_level=6","r/admin_level=7"])
 run(["osmium","export","--overwrite","--attributes=type,id","--geometry-types=polygon","-o",gj,admin])
 features=json.loads(gj.read_text(encoding="utf-8"))["features"]
 aliases={c:{norm(x) for x in xs} for c,xs in CITIES.items()}; candidates={c:[] for c in CITIES}
 for ft in features:
  p=ft.get("properties") or {}
  typ=p.get("@type")
  if typ and typ!="relation":continue
  try:level=int(p.get("admin_level",-1))
  except (ValueError,TypeError):continue
  if level not in (4,5,6,7):continue
  names=[p.get(k,"") for k in ("name","name:zh","name:zh-Hans","name:en","official_name","alt_name","short_name","int_name")]
  nset={norm(str(x)) for x in names if x}; matched=[c for c,a in aliases.items() if nset&a]
  if not matched:continue
  g=shape(ft["geometry"])
  if not g.is_valid:g=make_valid(g)
  parts=polygon_parts(g)
  if not parts:continue
  g=unary_union(parts)
  for c in matched:candidates[c].append((level,g,p.get("@id","unknown"),names))

 # Zhangzhou's prefecture-level boundary is absent or incomplete in the current OSM extract.
 # Reconstruct the Zhangzhou coverage from its 11 county-level administrative relations
 # only when every county polygon is present; do not substitute Fujian Province or a rectangle.
 if not candidates["漳州"]:
  county_aliases={
   "芗城区":["芗城区","Xiangcheng District"],
   "龙文区":["龙文区","Longwen District"],
   "龙海区":["龙海区","龙海市","Longhai District","Longhai City"],
   "长泰区":["长泰区","长泰县","Changtai District","Changtai County"],
   "云霄县":["云霄县","Yunxiao County"],
   "漳浦县":["漳浦县","Zhangpu County"],
   "诏安县":["诏安县","Zhao'an County","Zhaoan County"],
   "东山县":["东山县","Dongshan County"],
   "南靖县":["南靖县","Nanjing County"],
   "平和县":["平和县","Pinghe County"],
   "华安县":["华安县","Hua'an County","Huaan County"]
  }
  county_rows={name:[] for name in county_aliases}
  for ft in features:
   p=ft.get("properties") or {}
   typ=p.get("@type")
   if typ and typ!="relation":continue
   try:level=int(p.get("admin_level",-1))
   except (ValueError,TypeError):continue
   if level!=6:continue
   names=[p.get(k,"") for k in ("name","name:zh","name:zh-Hans","name:en","official_name","alt_name","short_name","int_name")]
   nset={norm(str(x)) for x in names if x}
   for county,aliases0 in county_aliases.items():
    if nset & {norm(x) for x in aliases0}:
     g=shape(ft["geometry"])
     if not g.is_valid:g=make_valid(g)
     parts=polygon_parts(g)
     if parts:county_rows[county].append((unary_union(parts),p.get("@id","unknown"),names))
  missing_counties=[n for n,rows in county_rows.items() if not rows]
  # Longhai District (formerly Longhai City) is absent from the current Geofabrik
  # China extract. Fetch its complete OSM boundary relation instead of using a
  # rectangle or silently omitting part of Zhangzhou.
  if "龙海区" in missing_counties:
   relation_id="3242114"
   relation_xml=WORK/"longhai-relation.osm"
   relation_pbf=WORK/"longhai-relation.osm.pbf"
   relation_gj=WORK/"longhai-relation.geojson"
   relation_url="https://api.openstreetmap.org/api/0.6/relation/"+relation_id+"/full"
   print("Fetching missing Longhai administrative relation "+relation_id+" from OpenStreetMap API.",flush=True)
   download(relation_url,relation_xml)
   run(["osmium","cat",relation_xml,"--overwrite","-o",relation_pbf])
   run(["osmium","export","--overwrite","--attributes=type,id","--geometry-types=polygon","-o",relation_gj,relation_pbf])
   relation_features=json.loads(relation_gj.read_text(encoding="utf-8"))["features"]
   found=False
   for ft in relation_features:
    p=ft.get("properties") or {}
    if p.get("@type")!="relation" or str(p.get("@id"))!=relation_id or not ft.get("geometry"):
     continue
    g=shape(ft["geometry"])
    if not g.is_valid:g=make_valid(g)
    parts=polygon_parts(g)
    if not parts:continue
    county_rows["龙海区"].append((unary_union(parts),"relation/"+relation_id,["龙海区","Longhai District"]))
    found=True
    break
   if not found:
    raise RuntimeError("OpenStreetMap relation "+relation_id+" did not yield a valid Longhai polygon; refusing to build an incomplete Zhangzhou map.")
   missing_counties=[n for n,rows in county_rows.items() if not rows]
  if not missing_counties:
   county_geoms=[]; county_ids=[]; county_names=[]
   for county,rows in county_rows.items():
    rows.sort(key=lambda x:-x[0].area)
    geom,rid,names=rows[0]
    county_geoms.append(geom);county_ids.append(rid)
    county_names.append(county+": "+", ".join(x for x in names if x))
   candidates["漳州"]=[(6,unary_union(county_geoms),"county-union:"+",".join(county_ids),county_names)]
  else:
   (OUT/"zhangzhou-county-diagnostic.json").write_text(
    json.dumps({"missing_counties":missing_counties,
     "found_counties":[n for n,rows in county_rows.items() if rows],
     "all_admin_names":[{"level":(ft.get("properties") or {}).get("admin_level"),
      "names":[(ft.get("properties") or {}).get(k) for k in ("name","name:zh","name:en") if (ft.get("properties") or {}).get(k)]}
      for ft in features if any("漳" in str((ft.get("properties") or {}).get(k,"")) or "zhang" in str((ft.get("properties") or {}).get(k,"")).lower()
      for k in ("name","name:zh","name:en","official_name"))]},ensure_ascii=False,indent=2),encoding="utf-8")

 missing=[c for c,rows in candidates.items() if not rows]
 if missing:
  sample=[]
  for ft in features:
   p=ft.get("properties") or {}
   if p.get("boundary")=="administrative":
    sample.append({"level":p.get("admin_level"),"names":[p.get(k) for k in ("name","name:zh","name:en") if p.get(k)]})
  (OUT/"boundary-candidates-diagnostic.json").write_text(json.dumps({"missing":missing,"sample":sample},ensure_ascii=False,indent=2),encoding="utf-8")
  raise RuntimeError("Missing exact administrative boundary relations: "+", ".join(missing)+". No rectangle fallback.")
 local=CRS.from_proj4("+proj=aeqd +lat_0=23.2 +lon_0=114.8 +datum=WGS84 +units=m +no_defs")
 to_local=Transformer.from_crs("EPSG:4326",local,always_xy=True).transform
 to_wgs=Transformer.from_crs(local,"EPSG:4326",always_xy=True).transform
 selected=[]; geoms=[]
 for city,rows in candidates.items():
  rows.sort(key=lambda x:(0 if x[0]==6 else 1,-transform(to_local,x[1]).area))
  level,g,rid,names=rows[0]; metric=transform(to_local,g)
  if metric.area<100_000_000:raise RuntimeError(f"Suspiciously small boundary for {city}: {metric.area/1e6:.1f} km2")
  geoms.append(metric); selected.append({"city":city,"admin_level":level,"osm_relation_id":rid,
    "matched_names":[x for x in names if x],"area_km2":round(metric.area/1e6,2)})
 dissolved=unary_union(geoms).buffer(750)
 if not dissolved.is_valid:dissolved=make_valid(dissolved)
 target=transform(to_wgs,dissolved); bounds=target.bounds
 target_gj.write_text(json.dumps({"type":"FeatureCollection","features":[{"type":"Feature",
  "properties":{"name":"Runner Gardenia PRD Eastern Guangdong Zhangzhou","buffer_m":750},
  "geometry":mapping(target)}]},ensure_ascii=False),encoding="utf-8")
 print("Selected boundaries:",json.dumps(selected,ensure_ascii=False),flush=True)
 print("Target bounds:",bounds,flush=True)
 run(["osmium","extract","--overwrite","--polygon",target_gj,"--strategy","smart","-S","types=multipolygon,boundary",
      "--set-bounds","-o",region,china])
 region_info=subprocess.run(["osmium","fileinfo","-e",region],check=True,capture_output=True,text=True).stdout
 download(WRITER_URL,writer)
 plugin=Path.home()/".openstreetmap"/"osmosis"/"plugins";plugin.mkdir(parents=True,exist_ok=True)
 (plugin/writer.name).write_bytes(writer.read_bytes())
 bbox=f"{bounds[1]:.7f},{bounds[0]:.7f},{bounds[3]:.7f},{bounds[2]:.7f}"
 os.environ["JAVACMD_OPTIONS"]="-Xmx5G -XX:+UseG1GC"
 run(["osmosis","--rb",f"file={region}","--mw",f"file={MAP_PATH}","type=hd","threads=2",
      f"bbox={bbox}","map-start-position=23.2,114.8","map-start-zoom=7","preferred-languages=zh,en",
      "polygon-clipping=true","way-clipping=true","simplification-factor=1.0","simplification-max-zoom=12"])
 if not MAP_PATH.is_file() or MAP_PATH.stat().st_size<1024*1024:raise RuntimeError("No plausible map file produced.")
 if MAP_PATH.stat().st_size>=2*1024*1024*1024:raise RuntimeError("Map exceeds P0.6's 2 GiB import limit.")
 check=WORK/"reader-check";check.mkdir(exist_ok=True)
 (check/"pom.xml").write_text("""<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd"><modelVersion>4.0.0</modelVersion><groupId>local.gardenia</groupId><artifactId>map-check</artifactId><version>1</version><properties><maven.compiler.release>17</maven.compiler.release></properties><dependencies><dependency><groupId>org.mapsforge</groupId><artifactId>mapsforge-map-reader</artifactId><version>0.25.0</version></dependency></dependencies></project>""",encoding="utf-8")
 java=r'''import java.io.File;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
public class MapFileCheck {
 public static void main(String[] a)throws Exception{
  File f=new File(a[0]); MapFile m=new MapFile(f);
  try{
   if(m.getMapFileInfo().fileSize!=f.length())throw new IllegalStateException("size mismatch");
   byte z=12;
   String[] n={"Guangzhou","Shenzhen","Zhuhai","Foshan","Dongguan","Zhongshan","Jiangmen","Huizhou","Zhaoqing","Shanwei","Jieyang","Shantou","Chaozhou","Zhangzhou","Dongshan Island"};
   double[][] p={{23.1291,113.2644},{22.5431,114.0579},{22.2710,113.5767},{23.0218,113.1219},{23.0207,113.7518},{22.5159,113.3926},{22.5787,113.0815},{23.1115,114.4152},{23.0469,112.4651},{22.7876,115.3753},{23.5438,116.3557},{23.3535,116.6822},{23.6567,116.6226},{24.5130,117.6471},{23.7010,117.4290}};
   for(int i=0;i<p.length;i++){
    Tile t=new Tile(MercatorProjection.longitudeToTileX(p[i][1],z),MercatorProjection.latitudeToTileY(p[i][0],z),z,256);
    if(m.readMapData(t)==null)throw new IllegalStateException("No readable map tile at "+n[i]);
    System.out.println("MAP_TILE_OK "+n[i]);
   }
   System.out.println("MAP_READER_OK bytes="+f.length()+" bounds="+m.boundingBox());
  }finally{m.close();}
 }
}'''
 (check/"MapFileCheck.java").write_text(java,encoding="utf-8")
 cpfile=check/"classpath.txt"
 run(["mvn","-q","-f",check/"pom.xml","dependency:build-classpath",f"-Dmdep.outputFile={cpfile}"])
 cp=cpfile.read_text().strip();run(["javac","-cp",cp,check/"MapFileCheck.java"])
 result=subprocess.run(["java","-cp",f"{check}:{cp}","MapFileCheck",MAP_PATH],check=True,capture_output=True,text=True)
 print(result.stdout,flush=True)
 (OUT/"ATTRIBUTION.txt").write_text("Map data © OpenStreetMap contributors\nhttps://www.openstreetmap.org/copyright\nOpenStreetMap data is provided under the Open Database License (ODbL).\n",encoding="utf-8")
 manifest={"product":"RunnerGardenia_GD_PRD_Zhangzhou_Dongshan","format":"Mapsforge .map",
  "mapsforge_reader_version":"0.25.0","mapsforge_writer_version":"0.25.0",
  "source":{"url":CHINA_URL,"sha256":china_hash,"fileinfo":china_info},
  "boundary_relations":selected,"buffer_meters":750,
  "bounds_wgs84":{"min_lon":bounds[0],"min_lat":bounds[1],"max_lon":bounds[2],"max_lat":bounds[3]},
  "region_extract_fileinfo":region_info,
  "map":{"filename":MAP_NAME,"bytes":MAP_PATH.stat().st_size,"sha256":sha256(MAP_PATH)},
  "validation":{"reader":"Mapsforge map-reader 0.25.0","test_points":list(TEST_POINTS),"reader_output":result.stdout},
  "built_at_utc":datetime.now(timezone.utc).isoformat()}
 (OUT/"build-manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding="utf-8")
 lines=["# Runner Gardenia Custom Offline Map — Acceptance Report","",
  "- Status: BUILT AND MAPSFORGE-READER VALIDATED",f"- File: {MAP_NAME}",
  f"- Size: {MAP_PATH.stat().st_size:,} bytes",f"- SHA-256: {manifest['map']['sha256']}",
  "- Reader: Mapsforge 0.25.0","- Target boundaries: 14 requested city relations",
  "- Meizhou: full prefecture excluded; no route corridor supplied",
  "- West Guangdong: excluded except complete OSM ways crossing the selected boundary","",
  "## Selected boundaries","","| City | admin_level | OSM relation | area km² |","|---|---:|---:|---:|"]
 for row in selected:lines.append(f"| {row['city']} | {row['admin_level']} | {row['osm_relation_id']} | {row['area_km2']} |")
 lines += ["","## Reader checks","",result.stdout.rstrip(),"",
  "The map is a dissolved union of the requested city boundaries with a 750 m buffer. Complete OSM ways crossing the boundary are retained to avoid severing roads. OSM attribution is required.",
  "","© OpenStreetMap contributors — https://www.openstreetmap.org/copyright","ODbL",""]
 (OUT/"acceptance-report.md").write_text("\n".join(lines),encoding="utf-8")
 print("BUILD_SUCCESS",json.dumps(manifest["map"]),flush=True)

if __name__=="__main__":main()
