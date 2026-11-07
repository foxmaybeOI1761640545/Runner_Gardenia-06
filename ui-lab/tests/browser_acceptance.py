"""Actual Chromium acceptance. Test dependencies only; the website is buildless.

CI: pip install -r ui-lab/tests/requirements.txt
    playwright install --with-deps chromium
    python ui-lab/tests/browser_acceptance.py
Optional: UI_LAB_URL uses an already served deployment instead of the test server.
Artifacts include screenshots, JSON results, browser errors, and a Playwright trace.
"""
import contextlib
from datetime import datetime, timezone
from functools import partial
import http.server
import hashlib
import struct
import subprocess
import json
import os
from pathlib import Path
import threading
import traceback
from urllib.parse import urlparse
from playwright.sync_api import sync_playwright, expect
from metric_swap_acceptance import exercise_metric_swaps, exercise_metric_persistence, exercise_long_duration, exercise_metric_touch

ROOT = Path(__file__).resolve().parents[1]
RESULTS = ROOT / 'test-results'
PREVIEWS = ROOT / 'previews'
RESULTS.mkdir(exist_ok=True)
PREVIEWS.mkdir(exist_ok=True)
report = {'viewports': [], 'assertions': [], 'console_errors': [], 'external_requests': [], 'passed': False}
THEMES = ['precision','quiet','explorer','neon','swiss','industrial','wellness','aurora','racing','brutal']

class QuietHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, *_):
        pass

@contextlib.contextmanager
def site_url():
    if os.getenv('UI_LAB_URL'):
        yield os.environ['UI_LAB_URL']
        return
    server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), partial(QuietHandler, directory=str(ROOT)))
    worker = threading.Thread(target=server.serve_forever, daemon=True)
    worker.start()
    try:
        yield f'http://127.0.0.1:{server.server_port}/'
    finally:
        server.shutdown()
        server.server_close()

def checked(label):
    report['assertions'].append(label)
    print('PASS:', label, flush=True)

def click(page, selector):
    page.locator(selector).click()

def nav(page, target):
    click(page, f'#app-nav [data-page="{target}"]')

def choose(page, index):
    click(page, f'#pagination [data-theme-index="{index}"]')
    expect(page.locator('#phone-screen')).to_have_attribute('data-theme', THEMES[index])

def sim(page, kind, value):
    click(page, f'#{kind}-control [data-value="{value}"]')
    expect(page.locator(f'#{kind}-control [data-value="{value}"]')).to_have_attribute('aria-pressed', 'true')

def reset(page):
    click(page, '#reset-demo')

def assert_layout(page, label):
    geometry = page.evaluate('''() => {
      const box = s => {const r=document.querySelector(s).getBoundingClientRect();return {x:r.x,y:r.y,width:r.width,height:r.height,bottom:r.bottom}};
      const content=document.querySelector('#phone-content');
      const overflow=[...content.querySelectorAll('*')].filter(e=>!e.closest('svg') && e.scrollWidth>e.clientWidth+2 && e.clientWidth>0 && getComputedStyle(e).overflowX==='visible').map(e=>({tag:e.tagName,cls:e.className,text:e.textContent.slice(0,50)}));
      return {screen:box('#phone-screen'),phone:box('#phone-size'),carousel:box('#carousel-section'),body:document.documentElement.scrollWidth,viewport:innerWidth,contentOverflow:content.scrollWidth-content.clientWidth,overflow};
    }''')
    assert abs(geometry['screen']['width']/geometry['screen']['height'] - 393/873) < 0.001, geometry
    assert geometry['body'] <= geometry['viewport'] + 1, geometry
    assert geometry['contentOverflow'] <= 1, geometry
    assert geometry['phone']['bottom'] <= geometry['carousel']['y'], geometry
    assert not geometry['overflow'], f'{label}: clipped text/element {geometry["overflow"]}'
    checked('layout ' + label)

def exercise_theme(page, index, width, height):
    choose(page, index)
    reset(page)
    nav(page, 'run')
    dom = cdp.send('DOM.getDocument')
    node = cdp.send('DOM.querySelector', {'nodeId':dom['root']['nodeId'], 'selector':'#phone-content .app-header h2'})
    fonts = cdp.send('CSS.getPlatformFontsForNode', {'nodeId':node['nodeId']})['fonts']
    assert any(f['glyphCount']>0 and any(n in f['familyName'] for n in ['Gardenia UI Lab CJK','Noto Sans CJK','WenQuanYi']) for f in fonts), f'Chinese glyph fallback missing: {fonts}'
    assert_layout(page, f'{width}x{height} theme {index+1} run')
    if width == 1440:
        page.clock.fast_forward(3500)
        page.locator('#phone-frame').screenshot(path=str(PREVIEWS / f'theme-{index+1:02}.png'), animations='disabled')
    click(page, '#phone-content [data-action="pause"]')
    expect(page.locator('.record-state')).to_contain_text('用户主动暂停')
    before = page.locator('.hero-number').inner_text()
    page.clock.fast_forward(3000)
    assert page.locator('.hero-number').inner_text() == before
    click(page, '#phone-content [data-action="resume"]')
    expect(page.locator('.record-state')).to_contain_text('正在记录')
    sim(page, 'gps', 'searching')
    expect(page.locator('#phone-content')).to_contain_text('定位精度尚未确定')
    expect(page.locator('#phone-content')).not_to_contain_text('121.470')
    if index != 3:
        expect(page.locator('.hero-number')).to_contain_text('--')
    sim(page, 'session', 'idle')
    expect(page.locator('[data-action="start"]')).to_be_disabled()
    sim(page, 'gps', 'fixed')
    expect(page.locator('[data-action="start"]')).to_be_enabled()
    click(page, '[data-action="start"]')
    sim(page, 'gps', 'lost')
    expect(page.locator('#phone-content')).to_contain_text('恢复后仅记录新位置')
    expect(page.locator('.record-state')).to_contain_text('正在记录')
    sim(page, 'gps', 'fixed')
    assert page.locator('[data-route-line]').get_attribute('d').count('M') == 2, 'GPS recovery must preserve a route gap'
    if index != 3:
        expect(page.locator('.hero-number')).to_contain_text('0.00')
    click(page, '[data-action="gps-page"]')
    expect(page.locator('#phone-content h2').first).to_have_text('GPS 定位状态')
    click(page, '[aria-label="返回运动"]')
    click(page, '[data-action="finish"]')
    expect(page.locator('.detail-heading h2')).to_have_text('运动详情')
    reset(page)
    nav(page, 'heart')
    for status in ['disconnected','scanning','connecting','reconnecting','stale','connected']:
        sim(page, 'heart', status)
        expected = '--' if status != 'connected' else '148'
        expect(page.locator('.bpm-number')).to_contain_text(expected)
    page.locator('[data-setting="contact"]').uncheck()
    expect(page.locator('.bpm-number')).to_contain_text('--')
    expect(page.locator('.connection-label')).to_have_text('接触不良')
    page.locator('[data-setting="contact"]').check()
    click(page, '[data-action="disconnect"]')
    expect(page.locator('.connection-label')).to_have_text('未连接')
    click(page, '[data-action="scan"]')
    expect(page.locator('#phone-content')).to_contain_text('BLE 模拟扫描中')
    page.clock.fast_forward(1500)
    expect(page.locator('.connection-label')).to_have_text('发现设备')
    expect(page.locator('.device-option')).to_have_count(2)
    expect(page.locator('.device-option').first).to_contain_text('RSSI -54 dBm')
    click(page, '[data-device="1"]')
    expect(page.locator('.connection-label')).to_have_text('连接中')
    page.clock.fast_forward(1100)
    expect(page.locator('.connection-label')).to_have_text('已连接')
    sim(page, 'heart', 'stale')
    click(page, '[data-action="reconnect"]')
    expect(page.locator('.bpm-number')).to_contain_text('--')
    page.clock.fast_forward(1100)
    expect(page.locator('.bpm-number')).to_contain_text('148')
    assert_layout(page, f'{width}x{height} theme {index+1} heart')
    click(page, '[data-action="heart-chart"]')
    expect(page.locator('svg.chart')).to_have_count(1)
    choose(page, (index+1)%10)
    expect(page.locator('.detail-heading h2')).to_have_text('心率详情')
    choose(page, index)
    click(page, '[data-action="chart-back"]')
    nav(page, 'history')
    page.locator('#empty-history').check()
    expect(page.locator('.empty-state')).to_contain_text('第一条记录')
    page.locator('#empty-history').uncheck()
    expect(page.locator('.history-row')).to_have_count(3)
    page.locator('.history-row').first.click()
    expect(page.locator('.detail-heading h2')).to_have_text('运动详情')
    expect(page.locator('#phone-content')).to_contain_text('最低心率')
    expect(page.locator('svg.chart')).to_have_count(1)
    assert_layout(page, f'{width}x{height} theme {index+1} detail')
    click(page, '[data-action="heart-chart"]')
    click(page, '[data-action="chart-back"]')
    expect(page.locator('.detail-heading h2')).to_have_text('运动详情')
    click(page, '[data-action="export"]')
    expect(page.get_by_role('dialog')).to_be_visible()
    expect(page.get_by_role('dialog')).to_contain_text('SIMULATION')
    page.locator('[aria-label="关闭导出弹窗"]').focus()
    page.keyboard.press('Shift+Tab')
    expect(page.locator('.sheet-buttons .text-button')).to_be_focused()
    page.keyboard.press('Tab')
    expect(page.locator('[aria-label="关闭导出弹窗"]')).to_be_focused()
    page.keyboard.press('Escape')
    expect(page.get_by_role('dialog')).to_have_count(0)
    expect(page.locator('[data-action="export"]')).to_be_focused()
    page.locator('#export-failure').check()
    click(page, '[data-action="export"]')
    click(page, '[data-action="save-gpx"]')
    expect(page.locator('#export-title')).to_have_text('导出失败')
    page.locator('#export-failure').uncheck()
    click(page, '[data-action="retry-export"]')
    with page.expect_download() as download_info:
        click(page, '[data-action="save-gpx"]')
    download = download_info.value
    assert download.suggested_filename.endswith('.gpx')
    assert 'SIMULATION' in download.suggested_filename
    path = RESULTS / f'sample-{width}-{index}.gpx'
    download.save_as(path)
    from xml.etree import ElementTree as ET
    root = ET.parse(path).getroot()
    assert root.tag == '{http://www.topografix.com/GPX/1/1}gpx'
    assert len(root.findall('.//{http://www.topografix.com/GPX/1/1}trkseg')) == 2
    expect(page.locator('#export-title')).to_have_text('导出成功')
    click(page, '[aria-label="关闭导出弹窗"]')
    click(page, '[data-action="export"]')
    with page.expect_download():
        click(page, '[data-action="share-gpx"]')
    expect(page.locator('#export-title')).to_have_text('导出成功')
    click(page, '[aria-label="关闭导出弹窗"]')
    click(page, '[aria-label="返回历史记录"]')
    nav(page, 'settings')
    page.locator('[data-setting="unit"]').focus()
    page.locator('[data-setting="unit"]').select_option('mi')
    expect(page.locator('[data-setting="unit"]')).to_be_focused()
    page.locator('[data-setting="rate"]').select_option('speed')
    page.locator('[data-setting="autoReconnect"]').uncheck()
    choose(page, (index+1)%10)
    expect(page.locator('[data-setting="unit"]')).to_have_value('mi')
    expect(page.locator('[data-setting="autoReconnect"]')).not_to_be_checked()
    choose(page, index)
    assert_layout(page, f'{width}x{height} theme {index+1} settings')
    nav(page, 'run')
    expect(page.locator('.metric-grid')).to_contain_text('mph')
    checked(f'{width}x{height} theme {index+1}: navigation/GPS/BLE/history/chart/export/settings/preserved state')

with site_url() as url, sync_playwright() as p:
    report['metric_swap_defaults'] = {theme:('heartRate' if theme=='neon' else 'distance') for theme in THEMES}
    report['metric_swap_auxiliary'] = 'gpsAccuracy: fixed secondary slot 3'
    report['url'] = url
    report['font_check'] = 'Chrome CSS.getPlatformFontsForNode verifies CJK title glyphs for every theme and viewport'
    options = {'headless': True}
    if os.getenv('CHROMIUM_EXECUTABLE'):
        options['executable_path'] = os.environ['CHROMIUM_EXECUTABLE']
    browser = p.chromium.launch(**options)
    report['browser_version'] = browser.version
    context = browser.new_context(viewport={'width':1440,'height':1000}, accept_downloads=True)
    context.tracing.start(screenshots=True, snapshots=True, sources=True)
    page = context.new_page()
    cdp = context.new_cdp_session(page)
    cdp.send('DOM.enable')
    cdp.send('CSS.enable')
    page.on('pageerror', lambda error: report['console_errors'].append(str(error)))
    page.on('console', lambda msg: report['console_errors'].append(msg.text) if msg.type == 'error' else None)
    allowed_host = urlparse(url).netloc
    page.on('request', lambda request: report['external_requests'].append(request.url) if urlparse(request.url).scheme in ('http','https') and urlparse(request.url).netloc != allowed_host else None)
    try:
        page.clock.install(time=datetime(2026,11,7,1,41,tzinfo=timezone.utc))
        page.goto(url, wait_until='networkidle')
        if page.locator('html').get_attribute('data-previews') == 'ready':
            expect(page.locator('.thumb img')).to_have_count(10)
            assert page.locator('.thumb img').evaluate_all('(imgs)=>imgs.every(i=>i.complete && i.naturalWidth>0)')
            checked('all ten local browser screenshot thumbnails loaded successfully')
        # Freeze UI timers; acceptance explicitly advances the browser's clock.
        page.clock.pause_at(page.evaluate('Date.now()') + 1000)
        for width,height in [(1440,1000),(1366,768),(390,844)]:
            page.set_viewport_size({'width':width,'height':height})
            reset(page)
            choose(page,0)
            page.clock.fast_forward(3500)
            page.evaluate('scrollTo(0,0)')
            page.screenshot(path=str(PREVIEWS / f'desktop-{width}.png' if width>500 else PREVIEWS / 'mobile-390.png'), full_page=True, animations='disabled')
            for i in range(10):
                exercise_metric_swaps(page,i,width,height,choose=choose,nav=nav,sim=sim,reset=reset,checked=checked,assert_layout=assert_layout,results=RESULTS)
                exercise_theme(page,i,width,height)
            report['viewports'].append({'width':width,'height':height,'themes':10,'passed':True})
        exercise_metric_persistence(page,choose=choose,nav=nav,reset=reset,checked=checked)
        exercise_long_duration(page,choose=choose,nav=nav,reset=reset,sim=sim,checked=checked,assert_layout=assert_layout,results=RESULTS)
        reset(page)
        choose(page,9)
        page.mouse.move(0,0)
        page.locator('#autoplay').focus()
        click(page,'#autoplay')
        page.locator('#concept-title').evaluate('(e)=>{e.tabIndex=-1;e.focus()}')
        # focus remains in the studio: autoplay must remain paused.
        page.clock.fast_forward(9000)
        expect(page.locator('#phone-screen')).to_have_attribute('data-theme','brutal')
        page.locator('footer a').first.focus()
        page.mouse.move(0,0)
        page.clock.run_for(10)  # flush the focusout task before advancing the autoplay deadline
        page.clock.fast_forward(9000)
        expect(page.locator('#phone-screen')).to_have_attribute('data-theme','precision')
        checked('autoplay loops 10 to 01; focus pauses and leaving resumes')
        page.locator('.studio').hover()
        page.clock.fast_forward(17000)
        expect(page.locator('#phone-screen')).to_have_attribute('data-theme','precision')
        checked('hover pauses autoplay')
        page.keyboard.press('ArrowRight')
        expect(page.locator('#phone-screen')).to_have_attribute('data-theme','quiet')
        page.keyboard.press('ArrowLeft')
        expect(page.locator('#phone-screen')).to_have_attribute('data-theme','precision')
        page.keyboard.press('ArrowLeft')
        expect(page.locator('#phone-screen')).to_have_attribute('data-theme','brutal')
        checked('keyboard arrows, manual cycling, wrapping')
        page.clock.fast_forward(17000)
        expect(page.locator('#phone-screen')).to_have_attribute('data-theme','brutal')
        checked('manual interaction pauses autoplay persistently')
        # Actual pointer event swipe and mouse drag; no direct app-state manipulation.
        track=page.locator('#theme-track')
        track.dispatch_event('pointerdown', {'pointerType':'touch','pointerId':7,'clientX':300,'clientY':700})
        track.dispatch_event('pointerup', {'pointerType':'touch','pointerId':7,'clientX':180,'clientY':700})
        expect(page.locator('#phone-screen')).to_have_attribute('data-theme','precision')
        checked('touch swipe selects and wraps themes')
        page.set_viewport_size({'width':1440,'height':1000})
        track.scroll_into_view_if_needed()
        track.evaluate('(e)=>e.scrollLeft=0')
        box=track.bounding_box()
        page.mouse.move(box['x']+300,box['y']+35)
        page.mouse.down()
        page.mouse.move(box['x']+100,box['y']+35, steps=8)
        page.mouse.up()
        assert track.evaluate('(e)=>e.scrollLeft') > 20
        checked('mouse dragging scrolls the carousel')
        page.emulate_media(reduced_motion='reduce')
        page.clock.fast_forward(17000)
        expect(page.locator('#autoplay')).to_have_attribute('aria-pressed','false')
        checked('reduced motion disables automatic playback')
        # Restore screenshot state, and capture all ten complete phone interfaces.
        reset(page)
        page.clock.fast_forward(3500)
        for i in range(10):
            choose(page,i)
            page.locator('#phone-frame').screenshot(path=str(PREVIEWS / f'theme-{i+1:02}.png'),animations='disabled')
        exercise_metric_touch(browser,url,checked=checked,report=report,results=RESULTS)
        # Verify the buildless distribution with actual file URLs and networking off.
        offline_page = context.new_page()
        offline_page.on('pageerror', lambda error: report['console_errors'].append(str(error)))
        offline_page.on('console', lambda msg: report['console_errors'].append(msg.text) if msg.type == 'error' else None)
        offline_page.on('request', lambda request: report['external_requests'].append(request.url) if urlparse(request.url).scheme in ('http','https') else None)
        context.set_offline(True)
        offline_page.goto((ROOT / 'index.html').as_uri(), wait_until='load')
        expect(offline_page.locator('#app-nav button')).to_have_count(4)
        nav(offline_page, 'settings')
        expect(offline_page.locator('#phone-content')).to_contain_text('隐私与离线')
        choose(offline_page, 9)
        expect(offline_page.locator('#phone-screen')).to_have_attribute('data-theme','brutal')
        offline_page.close()
        context.set_offline(False)
        checked('direct file:// opening and navigation work with browser networking disabled')
        assert not report['console_errors'], report['console_errors']
        assert not report['external_requests'], report['external_requests']
        checked('no browser console errors and no external network dependencies')
        report['passed']=True
        source_sha = os.getenv('GITHUB_SHA') or subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
        run_url = None
        if os.getenv('GITHUB_RUN_ID'):
            run_url = f"{os.getenv('GITHUB_SERVER_URL','https://github.com')}/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}"
        report['sourceCommit']=source_sha
        report['actionsRun']=run_url
        provenance = {'sourceCommit':source_sha, 'actionsRun':run_url, 'browserVersion':browser.version,
                      'phoneCaptureViewport':{'width':1440,'height':1000},
                      'pageScreenshots':'Full-page captures of the named viewport; image height may exceed viewport height',
                      'files':[]}
        for file in sorted(PREVIEWS.glob('*.png')):
            data=file.read_bytes()
            assert data[:8] == b'\x89PNG\r\n\x1a\n'
            width,height=struct.unpack('>II',data[16:24])
            provenance['files'].append({'name':file.name,'width':width,'height':height,'sha256':hashlib.sha256(data).hexdigest()})
        (PREVIEWS / 'PROVENANCE.json').write_text(json.dumps(provenance,ensure_ascii=False,indent=2)+'\n')
        (ROOT / 'docs/BROWSER_ACCEPTANCE_RESULTS.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    except Exception:
        report['failure']=traceback.format_exc()
        page.screenshot(path=str(RESULTS / 'failure.png'),full_page=True)
        raise
    finally:
        context.tracing.stop(path=str(RESULTS / 'trace.zip'))
        (RESULTS / 'results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
        browser.close()
print(f'PASS: {len(report["assertions"])} acceptance assertions; 30 theme/viewport combinations')
