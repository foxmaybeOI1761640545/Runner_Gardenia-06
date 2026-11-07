"""Public UI-only metric position, formatting, state and accessibility checks.

Uses the same real browser page as the existing acceptance suite. No app state
is exposed or modified by tests; even the >1h case advances the recording clock.
"""
from pathlib import Path
from playwright.sync_api import expect

CORE = {'distance','duration','paceSpeed','heartRate'}
PRIMARY = '[data-metric-role="primary"]'
SECONDARY = '[data-metric-role="secondary"]'


def positions(page):
    return page.locator('[data-metric-role]').evaluate_all('''nodes=>{
      const read=n=>({id:n.dataset.metricId,value:n.querySelector('.metric-value-text').textContent,unit:n.querySelector('.metric-unit').textContent,label:n.querySelector('.metric-label-text').textContent});
      return {primary:read(nodes.find(n=>n.dataset.metricRole==='primary')),secondary:nodes.filter(n=>n.dataset.metricRole==='secondary').map(read)};
    }''')


def assert_positions(page, expected, label):
    state=positions(page)
    assert [state['primary']['id']]+[m['id'] for m in state['secondary']] == expected, (label,state,expected)
    assert len(state['secondary'])==4
    ids=[state['primary']['id']]+[m['id'] for m in state['secondary']]
    assert len(set(ids))==5 and set(ids)==CORE|{'gpsAccuracy'},(label,ids)
    assert ids[-1]=='gpsAccuracy'
    expect(page.locator('.motion-grid button.metric-swap')).to_have_count(3)
    expect(page.locator('.motion-grid [data-metric-id="gpsAccuracy"]')).not_to_have_attribute('data-action','swap-metric')
    return state


def exercise_metric_swaps(page,index,width,height,*,choose,nav,sim,reset,checked,assert_layout,results):
    choose(page,index);reset(page);nav(page,'run')
    page.clock.fast_forward(3500)
    defaults=['heartRate','distance','duration','paceSpeed','gpsAccuracy'] if index==3 else ['distance','duration','paceSpeed','heartRate','gpsAccuracy']
    expected=defaults.copy();history=[]
    assert_positions(page,expected,'initial theme '+str(index+1))
    if index==3:
        expect(page.locator('.pulse-chart-label')).to_have_text('心率曲线 · BPM')
        expect(page.locator('.primary-readout')).to_contain_text('实时心率')

    def swap(slot,key=None):
        before=assert_positions(page,expected,'before swap')
        button=page.locator(f'.motion-grid [data-slot="{slot}"]')
        if key:
            # Traverse using Tab instead of assigning focus to a test-only target.
            page.locator('[data-action="gps-page"]').focus()
            for _ in range(8):
                if button.evaluate('(e)=>e===document.activeElement'):break
                page.keyboard.press('Tab')
            expect(button).to_be_focused()
            style=button.evaluate('(e)=>({outline:getComputedStyle(e).outlineStyle,width:parseFloat(getComputedStyle(e).outlineWidth)})')
            assert style['outline']!='none' and style['width']>0,style
            page.keyboard.press(key)
        else:button.click()
        expected[0],expected[slot+1]=expected[slot+1],expected[0]
        history.append(slot)
        after=assert_positions(page,expected,'after swap')
        assert after['primary']==before['secondary'][slot],(before,after)
        assert after['secondary'][slot]==before['primary'],(before,after)
        for other in range(4):
            if other!=slot:assert after['secondary'][other]==before['secondary'][other],(other,before,after)
        expect(page.locator(f'.motion-grid [data-slot="{slot}"]')).to_be_focused()
        expect(page.locator('#metric-announcement')).to_contain_text(after['primary']['label'])
        assert_layout(page,f'{width}x{height} theme {index+1} primary {expected[0]}')
        if index==3:
            expect(page.locator('.primary-readout .metric-label-text')).to_have_text(after['primary']['label'])
            expect(page.locator('.pulse-chart-label')).to_have_text('心率曲线 · BPM')
        return before,after

    def promote(id,key=None):
        if expected[0]!=id:return swap(expected[1:4].index(id),key)
        return None

    # The sequence exercises all four choices, all three positions and two keys.
    for id,key in [('duration','Enter'),('paceSpeed','Space'),('heartRate',None),('distance',None),('duration',None),('heartRate',None)]:
        promote(id,key)
        if width==1440 and id=='duration' and len(history)==1:
            page.locator('#phone-frame').screenshot(path=str(Path(results)/f'metric-swap-{index+1:02}.png'),animations='disabled')
    checked(f'{width}x{height} theme {index+1}: exact bidirectional slot exchange, unique metrics, Tab/Enter/Space and focus')

    # All four metrics are bound to the same formatter in either position.
    for id in ['distance','duration','paceSpeed','heartRate']:
        promote(id)
        old=positions(page)['primary']
        page.evaluate('window.__metricRunNode=document.querySelector("#phone-content").firstElementChild')
        page.clock.run_for(2000)
        after=positions(page)['primary']
        assert page.evaluate('window.__metricRunNode===document.querySelector("#phone-content").firstElementChild'),'ticker rebuilt page DOM'
        assert after['unit']==old['unit'] and after['label']==old['label']
        if id=='duration':assert after['value']!=old['value'],(old,after)
        if id=='distance':assert float(after['value'])>float(old['value']),(old,after)
        if id=='paceSpeed':assert after['value']=='5′27″' and after['unit']=='/km',after
        if id=='heartRate':assert after['value']=='148' and after['unit']=='BPM',after
        # The exact displayed value/unit pair must follow the metric into a slot.
        before,after_state=swap(0)
        assert after_state['secondary'][0]==before['primary']
    checked(f'{width}x{height} theme {index+1}: live bindings in both positions; stable page DOM')

    for id in ['distance','paceSpeed','heartRate']:
        promote(id)
        if id=='heartRate':
            for heart in ['disconnected','stale']:
                sim(page,'heart',heart);assert positions(page)['primary']['value']=='--';assert positions(page)['primary']['unit']=='BPM'
                before,after=swap(0);assert after['secondary'][0]['value']=='--' and after['secondary'][0]['unit']=='BPM'
                promote(id)
            sim(page,'heart','connected');assert positions(page)['primary']['value']=='148'
        else:
            for gps in ['searching','lost']:
                sim(page,'gps',gps)
                state=positions(page)
                assert state['primary']['value']=='--'
                assert state['secondary'][3]['value']=='--' and state['secondary'][3]['unit']==''
                before,after=swap(0);assert after['secondary'][0]['value']=='--'
                promote(id)
            sim(page,'gps','fixed')
    # Freeze both accumulation metrics; pace/velocity continue to use paused '--'.
    sim(page,'session','paused');frozen=positions(page);page.clock.run_for(3000)
    assert positions(page)==frozen
    promote('paceSpeed');assert positions(page)['primary']['value']=='--'
    sim(page,'session','recording')
    checked(f'{width}x{height} theme {index+1}: searching/lost GPS, disconnected/stale BLE, pause and resume')

    # Preferences are real settings controls, not injected model values.
    promote('distance');nav(page,'settings');page.locator('[data-setting="unit"]').select_option('mi');nav(page,'run')
    assert positions(page)['primary']['unit']=='mi'
    # Reset restores deterministic demo numbers but must retain metric positions.
    reset(page);nav(page,'settings');page.locator('[data-setting="unit"]').select_option('mi');nav(page,'run')
    assert positions(page)['primary']['value']=='3.26' and positions(page)['primary']['unit']=='mi'
    promote('paceSpeed');assert positions(page)['primary']['value']=='8′46″' and positions(page)['primary']['unit']=='/mi'
    nav(page,'settings');page.locator('[data-setting="rate"]').select_option('speed');nav(page,'run')
    assert positions(page)['primary']['value']=='6.8' and positions(page)['primary']['unit']=='mph'
    assert positions(page)['primary']['label']=='当前速度'
    before,after=swap(0);assert after['secondary'][0]['value']=='6.8' and after['secondary'][0]['unit']=='mph'
    nav(page,'settings');page.locator('[data-setting="unit"]').select_option('km');nav(page,'run')
    promote('paceSpeed');assert positions(page)['primary']['value']=='11.0' and positions(page)['primary']['unit']=='km/h'
    nav(page,'settings');page.locator('[data-setting="rate"]').select_option('pace');nav(page,'run')
    assert positions(page)['primary']['value']=='5′27″' and positions(page)['primary']['unit']=='/km'
    checked(f'{width}x{height} theme {index+1}: identical formatter pairs for km/mi, pace/speed, distance/time/heart rate')

    for slot in reversed(history):
        page.locator(f'.motion-grid [data-slot="{slot}"]').click()
        expected[0],expected[slot+1]=expected[slot+1],expected[0]
    assert_positions(page,defaults,'restored full ordered layout')
    reset(page)


def exercise_metric_persistence(page,*,choose,nav,reset,checked):
    choose(page,0);reset(page);nav(page,'run')
    before0=positions(page)
    page.locator('.motion-grid [data-metric-id="duration"]').click();saved0=positions(page)
    choose(page,1);before1=positions(page)
    page.locator('.motion-grid [data-metric-id="paceSpeed"]').click();saved1=positions(page)
    choose(page,0);assert positions(page)==saved0
    for target in ['history','heart','settings']:
        nav(page,target);nav(page,'run');assert positions(page)==saved0
    choose(page,1);assert positions(page)==saved1
    # Reverse both swaps to leave existing regression fixtures unchanged.
    page.locator('.motion-grid [data-slot="1"]').click();assert positions(page)==before1
    choose(page,0);page.locator('.motion-grid [data-slot="0"]').click();assert positions(page)==before0
    checked('per-theme complete ordered layouts survive theme changes and history/heart/settings navigation')


def exercise_long_duration(page,*,choose,nav,reset,sim,checked,assert_layout,results):
    choose(page,0);reset(page);nav(page,'run')
    # Exercise the real ticker for a synthetic 56m56s, not a private state hook.
    # Chunking lets the browser process all callbacks within each API deadline.
    for seconds in [300]*11+[116]:page.clock.run_for(seconds*1000)
    sim(page,'session','paused')
    states=positions(page)
    assert next(m for m in states['secondary'] if m['id']=='duration')['value']=='01:25:30',states
    for width,height in [(1440,1000),(1366,768),(390,844)]:
        page.set_viewport_size({'width':width,'height':height})
        for index in range(10):
            choose(page,index);nav(page,'run')
            before=positions(page);slot=next(i for i,m in enumerate(before['secondary']) if m['id']=='duration')
            page.locator(f'.motion-grid [data-slot="{slot}"]').click()
            state=positions(page)
            assert state['primary']['value']=='01:25:30' and state['primary']['unit']==''
            expect(page.locator('.hero-number')).to_have_attribute('data-value-size','long')
            assert_layout(page,f'{width}x{height} theme {index+1} HH:MM:SS')
            if width==1440:page.locator('#phone-frame').screenshot(path=str(Path(results)/f'metric-swap-long-{index+1:02}.png'),animations='disabled')
            page.locator(f'.motion-grid [data-slot="{slot}"]').click();assert positions(page)==before
    checked('01:25:30 primary and secondary formatting fits all ten themes at all three viewports')
    reset(page)


def exercise_metric_touch(browser,url,*,checked,report,results):
    from urllib.parse import urlparse
    context=browser.new_context(viewport={'width':390,'height':844},has_touch=True,is_mobile=True)
    page=context.new_page()
    page.on('pageerror',lambda error:report['console_errors'].append(str(error)))
    page.on('console',lambda msg:report['console_errors'].append(msg.text) if msg.type=='error' else None)
    page.on('request',lambda request:report['external_requests'].append(request.url) if urlparse(request.url).scheme in ('http','https') and urlparse(request.url).netloc!=urlparse(url).netloc else None)
    try:
        page.goto(url,wait_until='networkidle')
        # Real Chromium touch input, independent of the carousel's synthetic swipe fixture.
        for index in range(10):
            page.locator(f'#pagination [data-theme-index="{index}"]').tap()
            page.locator('#app-nav [data-page="run"]').tap()
            before=positions(page)
            slot=next(i for i,m in enumerate(before['secondary']) if m['id']=='duration')
            page.locator(f'.motion-grid [data-slot="{slot}"]').tap()
            after=positions(page)
            assert after['primary']['id']=='duration'
            assert after['secondary'][slot]['id']==before['primary']['id']
            assert [m['id'] for i,m in enumerate(after['secondary']) if i!=slot]==[m['id'] for i,m in enumerate(before['secondary']) if i!=slot]
            page.locator(f'.motion-grid [data-slot="{slot}"]').tap()
            assert positions(page)['primary']['id']==before['primary']['id']
        checked('all ten themes exchange and restore exact positions via real Chromium touchscreen taps')
    except Exception:
        page.screenshot(path=str(Path(results)/'metric-swap-touch-failure.png'),full_page=True)
        raise
    finally:context.close()
