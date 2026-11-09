"""Current P0.5 native UI scope, binding and offline invariants."""
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
BASE = 'f994d1ecd0e19a840800b2f946df3656a6b312c6'
A = '{http://schemas.android.com/apk/res/android}'
FAMILIES = ('precision', 'quiet', 'explorer', 'neon')


def check():
    stage = json.loads((ROOT / '.github/p05/stage.json').read_text())['stage']
    if stage == 0:
        return
    res = ROOT / 'src/main/res'
    tokens = json.loads((ROOT / 'src/main/assets/gardenia-theme-tokens.json').read_text())
    assert tuple(t['id'] for t in tokens['themes']) == FAMILIES
    structures = []
    expected_ids = None
    for name in ('p0_home', *('gardenia_home_' + f for f in FAMILIES)):
        layout = ET.parse(res / 'layout' / (name + '.xml'))
        ids = [e.get(A + 'id') for e in layout.iter() if e.get(A + 'id')]
        assert len(ids) == len(set(ids)), 'Duplicate binding ID: ' + name
        if expected_ids is None:
            expected_ids = set(ids)
        assert set(ids) == expected_ids, 'Shared ViewBinding contract differs: ' + name
        assert '@+id/p03_family_label' not in ids, 'Theme name View must be removed'
        row = next(e for e in layout.iter() if e.get(A + 'id') == '@+id/p05_compact_controls')
        assert row.get(A + 'orientation') == 'horizontal'
        descendants = {e.get(A + 'id') for e in row.iter()}
        assert {'@+id/p0_running', '@+id/p0_cycling', '@+id/p04_gps_status',
                '@+id/p0_devices'}.issubset(descendants)
        if name != 'p0_home':
            structures.append([(e.tag, sorted(e.attrib.items())) for e in layout.iter()])
    assert len({str(s) for s in structures}) == 4, 'Preserve four native hierarchies'
    renderer = (ROOT / 'src/main/java/de/dennisguse/opentracks/p0/app/DashboardRenderer.java').read_text()
    assert 'p03FamilyLabel' not in renderer
    for path in (ROOT / 'src/main/java/de/dennisguse/opentracks/p0/app').glob('*.java'):
        assert not re.search(r'\bWebView\b|android\.webkit', path.read_text()), path
    # Current scope has no authorization to rewrite acquisition, storage or recording algorithms.
    changes = subprocess.check_output(['git', 'diff', '--name-only', BASE, '--', 'src/main'],
                                      cwd=ROOT, text=True).splitlines()
    frozen = ('services/', 'sensors/', 'data/', 'p0/recording/', 'p0/gps/', 'p0/ble/',
              'p0/data/', 'p0/statistics/')
    for path in changes:
        assert not any(path.startswith('src/main/java/de/dennisguse/opentracks/' + prefix)
                       for prefix in frozen), 'Mature business implementation changed: ' + path
        if stage < 4:
            assert not path.startswith('src/main/java/de/dennisguse/opentracks/p0/export/'), path
    for mode in ('values', 'values-night'):
        colors = res / mode / 'gardenia_colors.xml'
        original = subprocess.check_output(['git', 'show', BASE + ':' + str(colors.relative_to(ROOT))],
                                           cwd=ROOT)
        assert original == colors.read_bytes(), 'Existing paired theme colors changed'
    print('PASS: P0.5 native scope, compact controls, four binding hierarchies and frozen business algorithms')


if __name__ == '__main__':
    check()
