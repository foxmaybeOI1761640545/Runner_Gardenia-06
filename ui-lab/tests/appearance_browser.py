"""Execute all 20 appearances at three viewports on local or published UI Lab."""
import contextlib
from datetime import datetime,timezone
from functools import partial
import http.server
import json
import os
from pathlib import Path
import subprocess
import threading
import traceback
from urllib.parse import urlparse
from playwright.sync_api import sync_playwright,expect
from appearance_acceptance import exercise_appearances

ROOT=Path(__file__).resolve().parents[1]
RESULTS=ROOT/'test-results';RESULTS.mkdir(exist_ok=True)
report={'browser_version':None,'errors':[],'requests_failed':[],'assertions':[],'passed':False}
@contextlib.contextmanager
def served():
    if os.getenv('UI_LAB_URL'):yield os.environ['UI_LAB_URL'];return
    class Quiet(http.server.SimpleHTTPRequestHandler):
        def log_message(self,*_):pass
    server=http.server.ThreadingHTTPServer(('127.0.0.1',0),partial(Quiet,directory=str(ROOT)))
    threading.Thread(target=server.serve_forever,daemon=True).start()
    try:yield f'http://127.0.0.1:{server.server_port}/'
    finally:server.shutdown();server.server_close()

def checked(label):report['assertions'].append(label);print('PASS:',label,flush=True)
def choose(page,index):page.locator(f'#pagination [data-theme-index="{index}"]').click()
def nav(page,target):page.locator(f'#app-nav [data-page="{target}"]').click()
def sim(page,kind,value):page.locator(f'#{kind}-control [data-value="{value}"]').click()
def reset(page):
    page.locator('#reset-demo').click();page.clock.fast_forward(3500)
def assert_layout(page,label):
    result=page.evaluate('''()=>{const e=document.querySelector('#phone-content');return {body:document.documentElement.scrollWidth,viewport:innerWidth,content:e.scrollWidth-e.clientWidth}}''')
    assert result['body']<=result['viewport']+1 and result['content']<=1,(label,result)

with served() as url,sync_playwright() as pw:
    browser=pw.chromium.launch(executable_path=os.getenv('CHROMIUM_EXECUTABLE','/usr/bin/chromium'),headless=True)
    context=browser.new_context(viewport={'width':1440,'height':1000},reduced_motion='reduce',accept_downloads=True)
    page=context.new_page();report['browser_version']=browser.version
    page.on('pageerror',lambda error:report['errors'].append(str(error)))
    page.on('console',lambda message:report['errors'].append(message.text) if message.type=='error' else None)
    page.on('response',lambda response:report['requests_failed'].append({'url':response.url,'status':response.status}) if response.status>=400 else None)
    context.tracing.start(screenshots=True,snapshots=True,sources=True)
    try:
        page.clock.install(time=datetime(2026,11,7,1,41,tzinfo=timezone.utc))
        response=page.goto(url,wait_until='networkidle');assert response.status==200
        page.clock.pause_at(page.evaluate('Date.now()')+100)
        exercise_appearances(page,choose=choose,nav=nav,sim=sim,reset=reset,checked=checked,assert_layout=assert_layout,results=RESULTS,report=report)
        assert not report['errors'],report['errors'];assert not report['requests_failed'],report['requests_failed']
        report['sourceCommit']=os.getenv('GITHUB_SHA') or subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
        report['actionsRunId']=os.getenv('GITHUB_RUN_ID');report['url']=url;report['passed']=True
    except Exception:
        report['failure']=traceback.format_exc();page.screenshot(path=str(RESULTS/'appearance-failure.png'),full_page=True);raise
    finally:
        context.tracing.stop(path=str(RESULTS/'appearance-trace.zip'))
        (RESULTS/'appearance-browser.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');browser.close()
