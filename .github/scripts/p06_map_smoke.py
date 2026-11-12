"""Fast real API35 map/GPS feedback; never substitutes for the full regression gate."""
import json,os,sys,tempfile
from pathlib import Path
from check_test_results import inspect
from p0_pipeline import scoped_sources
from p06_map_evidence import collect
CLASSES=['de.dennisguse.opentracks.p0.'+name for name in [
 'P0GpsRecordingTest','P0GpsPreparationTest','P051GpsUiTest',
 'map.P06GpsOwnershipTest','map.P06MapRepositoryTest','map.P06OfflineMapTest',
 'map.P06MapPersistenceTest','map.P06RouteReadTest']]

def run():
 if sys.argv[1:] == ['classes']:print(','.join(CLASSES));return
 proof={'passed':False,'commit':os.environ['GITHUB_SHA'],'fullRegression':False,'subsuiteOnly':True}
 try:
  with tempfile.TemporaryDirectory() as temporary:
   scoped_sources(CLASSES,Path(temporary));result=inspect(Path('build/outputs/androidTest-results'),Path(temporary))
  proof['android']=result
  if not result['passed'] or result['executed']<=0 or result['skipped']!=0:raise ValueError('Fast map/GPS subsuite did not completely pass')
  proof['map']=collect(os.environ['GITHUB_SHA']);proof['passed']=True
 except (ValueError,OSError,KeyError) as error:proof['diagnostic']=str(error)
 output=Path('build/reports/p06-map-smoke');output.mkdir(parents=True,exist_ok=True);(output/'result.json').write_text(json.dumps(proof,indent=2)+'\n');print(json.dumps(proof,indent=2))
 if not proof['passed']:raise SystemExit('Map/GPS subsuite failed; full regression remains independently required')
if __name__=='__main__':run()
