"""Buildless, stdlib-only source and isolation checks. Run from repository root."""
from html.parser import HTMLParser
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parent
BASE = '3116e5fa7a56ce6f299ea45d5a45745f1aed4466'

class Markup(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids, self.links = [], []
    def handle_starttag(self, tag, attrs):
        data = dict(attrs)
        if 'id' in data:
            self.ids.append(data['id'])
        for key in ('src', 'href'):
            if key in data:
                self.links.append(data[key])

parser = Markup()
parser.feed((ROOT / 'index.html').read_text())
assert len(parser.ids) == len(set(parser.ids)), 'Duplicate HTML IDs'
for link in parser.links:
    assert not re.match(r'^(https?:)?//', link), f'External dependency: {link}'
    if link and not link.startswith('#'):
        assert (ROOT / link).exists(), f'Missing resource: {link}'
css = (ROOT / 'styles.css').read_text()
assert '@import' not in css, 'External stylesheet import'
assert not re.search(r'url\([\s\'\"]*(https?:)?//', css), 'External CSS asset'
js = (ROOT / 'app.js').read_text()
assert not re.search(r'\b(fetch|XMLHttpRequest|WebSocket|geolocation|requestDevice)\s*\(', js), 'Unexpected network/device API'
assert len(re.findall(r"\{id:'(?:precision|quiet|explorer|neon|swiss|industrial|wellness|aurora|racing|brutal)'", js)) == 10
for name in ['precision','quiet','explorer','neon','swiss','industrial','wellness','aurora','racing','brutal']:
    assert f'[data-theme={name}]' in css, f'Missing theme: {name}'
for doc in ['ANDROID_TECH_STACK_AUDIT.md','NATIVE_UI_IMPLEMENTATION_GUIDE.md','DESIGN_SPEC.md','PAGE_STRUCTURE.md','TEST_REPORT.md']:
    assert (ROOT / 'docs' / doc).exists(), f'Missing document: {doc}'
subprocess.run(['node', '--check', str(ROOT / 'app.js')], check=True)
changed = subprocess.check_output(['git','diff','--name-only',BASE,'HEAD'], cwd=REPO, text=True).splitlines()
changed += subprocess.check_output(['git','diff','--name-only'], cwd=REPO, text=True).splitlines()
for path in changed:
    assert path.startswith('ui-lab/') or path == '.github/workflows/p0-ui-lab-pages.yml', f'Isolation violation: {path}'
print('PASS: HTML resources, ten themes, no runtime network/device API, documents, JS syntax, branch isolation')
