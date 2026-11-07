"""Validate complete schemas, generated resources and text/semantic contrast."""
import json
import re
from pathlib import Path
from generate_palettes import generated
ROOT=Path(__file__).resolve().parents[1]
data=json.loads((ROOT/'assets/design-tokens.json').read_text())
required={'background','surface','surfaceVariant','foreground','foregroundSecondary','accent','accentForeground','tint','divider','outline','success','warning','danger','chartColors','statusColors'}
assert len(data['themes'])==10
assert len({x['id'] for x in data['themes']})==10

def contrast(a,b):
    def lum(h):
        values=[int(h[i:i+2],16)/255 for i in (1,3,5)]
        return sum((x/12.92 if x<=.04045 else ((x+.055)/1.055)**2.4)*w for x,w in zip(values,[.2126,.7152,.0722]))
    x,y=sorted([lum(a),lum(b)]);return (y+.05)/(x+.05)

def colors(item):
    if isinstance(item,dict):
        for value in item.values():colors(value)
    else: assert isinstance(item,str) and re.fullmatch(r'#[0-9a-fA-F]{6}',item),item
for i,theme in enumerate(data['themes']):
    assert theme['nativeImplementation']['preloaded']==(i<4)
    for key in ['shapeTokens','typographyTokens','chartTokens','componentTokens','nativeImplementation']:assert key in theme
    for mode in ['light','dark']:
        t=theme[mode];assert set(t)==required;colors(t)
        for surface in ['background','surface','surfaceVariant']:
            for text in ['foreground','foregroundSecondary','success','warning','danger']:
                value=contrast(t[text],t[surface]);assert value>=4.5,(theme['id'],mode,text,surface,value)
        assert contrast(t['accentForeground'],t['accent'])>=4.5,(theme['id'],mode,'button')
        for surface in ['background','surface','surfaceVariant','tint']:
            if theme['id']=='brutal' and surface=='tint':continue  # Active yellow navigation uses accentForeground.
            assert contrast(theme['componentTokens'][mode]['accentText'],t[surface])>=4.5,(theme['id'],mode,'accentText',surface)
for name,content in generated().items():assert (ROOT/name).read_text()==content,f'Regenerate {name}'
assert not re.search(r'filter\s*:\s*invert|hue-rotate', ''.join(p.read_text() for p in ROOT.glob('*.css')))
print('PASS: 20 complete authored palettes, consistent schemas, legal colors, AA body/status/button contrast, deterministic browser resources')
