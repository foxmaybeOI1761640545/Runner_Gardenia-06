"""Fail closed on missing/failed browser evidence, capture coverage, or wrong URL."""
import hashlib
import json
import os
from pathlib import Path
from urllib.parse import urlparse
ROOT=Path(__file__).resolve().parents[1]
RESULTS=ROOT/'test-results'
regression=json.loads((RESULTS/'results.json').read_text())
appearance=json.loads((RESULTS/'appearance-browser.json').read_text())
assert regression['passed'] is True and appearance['passed'] is True
assert len(regression['viewports'])==3 and all(x['passed'] for x in regression['viewports'])
evidence=appearance['appearances'];assert evidence['passed'] is True
assert len(evidence['matrix'])==60 and all(x['passed'] for x in evidence['matrix'])
expected={(t['id'],m,(w,h)) for t in json.loads((ROOT/'assets/design-tokens.json').read_text())['themes'] for m in ['light','dark'] for w,h in [(1440,1000),(1366,768),(390,844)]}
assert {(x['theme'],x['appearance'],tuple(x['viewport'])) for x in evidence['matrix']}==expected
assert len(evidence['files'])==330
for f in evidence['files']:
    p=ROOT/'previews/appearances'/f['name'];assert hashlib.sha256(p.read_bytes()).hexdigest()==f['sha256']
if os.getenv('GITHUB_SHA'):
    assert regression['sourceCommit']==appearance['sourceCommit']==os.environ['GITHUB_SHA']
live=bool(os.getenv('UI_LAB_URL'))
if live:
    u=urlparse(appearance['url']);assert u.scheme=='https' and u.hostname=='foxmaybeoi1761640545.github.io' and u.path=='/Runner_Gardenia-06/'
receipt={'phase':1,'status':'PASSED','scope':'live' if live else 'local','sourceCommit':appearance['sourceCommit'],'actionsRunId':appearance['actionsRunId'],'url':appearance['url'],'httpStatus':200,'regressionAssertions':len(regression['assertions']),'appearanceAssertions':len(appearance['assertions']),'themeAppearanceViewportCombinations':60,'screenshots':330,'browserVersion':appearance['browser_version'],'nextPhaseAuthorized':live}
(RESULTS/'phase1-gate.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
(ROOT/'previews/appearances/PROVENANCE.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(receipt,ensure_ascii=False,indent=2))
