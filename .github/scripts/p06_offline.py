"""P06 adds one private map Activity; all original exact offline restrictions still apply."""
import verify_p0_offline as original
import json,os,re,subprocess
from pathlib import Path

MAP_DEPENDENCIES={'org.mapsforge:mapsforge-map-android:0.25.0','org.mapsforge:mapsforge-themes:0.25.0'}

def audit_runtime(build,baseline,sources):
    def dependencies(text):return set(re.findall(r"^\s*implementation ['\"]([^'\"]+)['\"]",text,re.M))
    if dependencies(build)!=dependencies(baseline)|MAP_DEPENDENCIES:raise ValueError('Only the pinned offline map runtime dependencies may be added')
    forbidden=('org.mapsforge.map.layer.download','TileDownloadLayer','TileDownloader','java.net.','okhttp','WebView','Socket(')
    if not sources:raise ValueError('Map implementation is missing')
    for path,source in sources.items():
        if any(token in source for token in forbidden):raise ValueError('Online renderer/network path forbidden: '+path)
    return {'passed':True,'engine':'Mapsforge','version':'0.25.0','addedRuntimeDependencies':sorted(MAP_DEPENDENCIES),'mapSourcesAudited':len(sources),'downloadLayerUsed':False,'networkClientUsed':False}

def enable_map_activity():
    original.COMPONENTS={**original.COMPONENTS,'activity':original.COMPONENTS['activity']|{original.PREFIX+'p0.map.P06MapActivity'}}

def verify():
    enable_map_activity();original.verify()
    baseline=subprocess.check_output(['git','show','ba00559af1af3d48bc3ecfde811d3e8d062e2f68:build.gradle'],text=True)
    sources={str(p):p.read_text() for p in Path('src/main/java/de/dennisguse/opentracks/p0/map').glob('*.java')}
    audit=audit_runtime(Path('build.gradle').read_text(),baseline,sources)
    path=Path('build/reports/p0-offline/result.json');proof=json.loads(path.read_text());proof['offlineMapRuntime']=audit
    if proof['commit']!=os.environ['GITHUB_SHA']:raise ValueError('Offline audit source mismatch')
    path.write_text(json.dumps(proof,indent=2)+'\n');print(json.dumps(audit,indent=2))
if __name__=='__main__':verify()
