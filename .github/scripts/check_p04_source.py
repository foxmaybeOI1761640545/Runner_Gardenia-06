"""Refinement is a native UI change over completed P0.3, never a business rewrite."""
from pathlib import Path
import json,subprocess,xml.etree.ElementTree as ET
BASE='cebf4682cec672fbde022c54bda3b442ff0663ab'
def check():
 subprocess.run(['git','merge-base','--is-ancestor',BASE,'HEAD'],check=True)
 protected=['services/','sensors/','data/','p0/data/','p0/gps/','p0/ble/','p0/recording/','p0/export/','p0/statistics/']
 for folder in protected:
  if subprocess.check_output(['git','diff',BASE,'--','src/main/java/de/dennisguse/opentracks/'+folder]):raise ValueError('Business layer changed: '+folder)
 for path in ['src/main/assets/gardenia-theme-tokens.json','ui-lab']:
  if subprocess.check_output(['git','diff',BASE,'--',path]):raise ValueError('Frozen design/tokens changed: '+path)
 old=json.loads(subprocess.check_output(['git','show',BASE+':release-ledger.json'],text=True));new=json.loads(Path('release-ledger.json').read_text())
 if new['allocations'][:len(old['allocations'])]!=old['allocations']:raise ValueError('Historical version allocations changed')
 if len(new['allocations'])!=len(old['allocations'])+1 or new['allocations'][-1]['versionCode']!=6775:raise ValueError('Missing unique next refinement allocation')
 main=Path('src/main/java/de/dennisguse/opentracks/p0/app');production='\n'.join(p.read_text() for p in main.glob('*.java'))
 if any(k in production for k in ['WebView','android.webkit','DashboardFixture','DashboardPreviewActivity','Preview H303','28:36','5.25 km']):raise ValueError('Prototype/test data entered production UI')
 if 'DashboardPreviewActivity' in Path('src/main/AndroidManifest.xml').read_text():raise ValueError('Debug fixture entry leaked into production')
 A='{http://schemas.android.com/apk/res/android}';layouts=[]
 for f in ['precision','quiet','explorer','neon']:
  root=ET.parse('src/main/res/layout/gardenia_home_'+f+'.xml').getroot();ids=[e.get(A+'id') for e in root.iter() if e.get(A+'id')]
  if len(ids)!=len(set(ids)):raise ValueError('Duplicate dashboard view id')
  if not any(e.tag.endswith('PulseChartView') and e.get(A+'layout_height')=='84dp' for e in root.iter()):raise ValueError('Missing compact native heart component')
  if not any(e.tag.endswith('RouteMapView') for e in root.iter()):raise ValueError('Real route component missing')
  layouts.append(ET.tostring(root))
 if len(set(layouts))!=4:raise ValueError('Four native organizations collapsed into a common layout')
 print('PASS: isolated P0.3 refinement, unchanged GPS/BLE/recording/database/GPX/design tokens, four native route/pulse hierarchies, debug-only fixtures')
if __name__=='__main__':check()
