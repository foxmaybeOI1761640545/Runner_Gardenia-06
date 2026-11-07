"""Generate offline browser resources from the canonical, authored JSON tokens."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def generated():
    data=json.loads((ROOT/'assets/design-tokens.json').read_text())
    css=[]
    mapping={'bg':'background','surface':'surface','surface-variant':'surfaceVariant','fg':'foreground','muted':'foregroundSecondary','accent':'accent','on-accent':'accentForeground','tint':'tint','line':'divider','outline':'outline','success':'success','warning':'warning','danger':'danger'}
    for theme in data['themes']:
        for mode in ['light','dark']:
            token=theme[mode];props={k:token[v] for k,v in mapping.items()}
            props.update({'accent-text':theme['componentTokens'][mode]['accentText'],'chart':token['chartColors']['heartRate'],'chart-grid':token['chartColors']['grid']})
            props.update({''.join('-'+c.lower() if c.isupper() else c for c in k):v for k,v in theme['componentTokens'][mode].items()})
            css.append(f"[data-theme={theme['id']}][data-appearance={mode}] {{\n"+''.join(f'  --{k}:{v};\n' for k,v in props.items())+f'  color-scheme:{mode};\n}}')
    return {'assets/theme-palettes.css':'/* Generated from design-tokens.json by tests/generate_palettes.py. */\n'+'\n'.join(css)+'\n',
            'assets/theme-metadata.js':'/* Generated offline theme metadata; no network API required. */\nwindow.GARDENIA_THEMES = '+json.dumps([{k:t[k] for k in ['id','name','defaultAppearance','light','dark']} for t in data['themes']],ensure_ascii=False)+';\n'}

if __name__=='__main__':
    for name,content in generated().items():(ROOT/name).write_text(content)
