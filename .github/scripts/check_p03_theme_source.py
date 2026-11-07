"""Theme-only scope, native resource/semantic completeness and completed design gate."""
from pathlib import Path
import json,re,subprocess,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]
A='{http://schemas.android.com/apk/res/android}'
base='4454d716a5ae010b92d3eefa1ac1c0ebedde0063'
gate=json.loads((ROOT/'docs/theme-system/phase1-final-gate.json').read_text());assert gate['status']=='PASSED' and gate['nextPhaseAuthorized'] is True and all(gate['gates'].values())
data=json.loads((ROOT/'src/main/assets/gardenia-theme-tokens.json').read_text());assert data['referenceCommit']==base
assert [t['id'] for t in data['themes']]==['precision','quiet','explorer','neon']
res=ROOT/'src/main/res'
for mode,folder in [('light','values'),('dark','values-night')]:
    colors={e.get('name'):e.text for e in ET.parse(res/folder/'gardenia_colors.xml').getroot() if e.tag=='color'}
    for theme in data['themes']:
        for role,color in theme[mode].items():
            if isinstance(color,str):assert colors[f'gardenia_{theme["id"]}_{role.lower()}']==color
shapes=[]
base_ids={e.get(A+'id') for e in ET.parse(res/'layout/p0_home.xml').iter() if e.get(A+'id')}
for theme in data['themes']:
    layout=ET.parse(res/f'layout/gardenia_home_{theme["id"]}.xml');ids=[e.get(A+'id') for e in layout.iter() if e.get(A+'id')]
    assert set(ids)==base_ids and len(ids)==len(set(ids))
    shapes.append([(e.tag,e.get(A+'orientation'),e.get(A+'layout_width'),e.get(A+'layout_height'),e.get(A+'padding'),e.get(A+'gravity')) for e in layout.iter()])
assert len({str(x) for x in shapes})==4
source=(ROOT/'src/main/java/de/dennisguse/opentracks/settings/PreferencesUtils.java').read_text();assert 'MODE_NIGHT_NO' not in source and 'MODE_NIGHT_YES' not in source and 'MODE_NIGHT_FOLLOW_SYSTEM' in source
assert '@string/theme_key' not in (res/'xml/settings_user_interface.xml').read_text()
changed=subprocess.check_output(['git','diff','--name-only',base],cwd=ROOT,text=True).splitlines()
for path in changed:
    assert not any(path.startswith('src/main/java/de/dennisguse/opentracks/'+prefix) for prefix in ['services/','sensors/','p0/data/','p0/gps/','p0/ble/','p0/export/']),path
assert not any(re.search(r'\bWebView\b|android\.webkit',p.read_text()) for p in (ROOT/'src/main/java/de/dennisguse/opentracks/p0/app').glob('*.java'))
assert subprocess.check_output(['git','rev-parse','test-p0-v0.1.0-p0.2^{commit}'],cwd=ROOT,text=True).strip()==base
print('PASS: completed Phase 1; four native structures, eight complete paired resources, system-only appearance, preserved service/GPS/BLE/data/export algorithms')
