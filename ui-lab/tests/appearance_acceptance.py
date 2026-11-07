"""Real Chromium appearance matrix and evidence; also runs against deployed Pages."""
import hashlib
import json
from pathlib import Path
from playwright.sync_api import expect

def rendered_contrast(page,selector,minimum=4.5):
    pairs=page.locator(selector).evaluate_all(r'''els=>els.map(el=>{
      const color=s=>{const v=s.match(/[\d.]+/g).map(Number);return [v[0]/255,v[1]/255,v[2]/255,v.length>3?v[3]:1]};
      const over=(fg,bg)=>{const a=fg[3]+bg[3]*(1-fg[3]);return [0,1,2].map(i=>(fg[i]*fg[3]+bg[i]*bg[3]*(1-fg[3]))/a).concat(a)};
      let bg=[1,1,1,1],nodes=[],node=el;
      while(node){nodes.unshift(node);node=node.parentElement;}
      nodes.forEach(n=>bg=over(color(getComputedStyle(n).backgroundColor),bg));
      let opacity=1;for(const n of nodes)opacity*=Number(getComputedStyle(n).opacity);
      let fg=color(getComputedStyle(el).color);fg[3]*=opacity;fg=over(fg,bg);
      const lum=v=>v.slice(0,3).map(x=>x<=.04045?x/12.92:((x+.055)/1.055)**2.4).reduce((s,x,i)=>s+x*[.2126,.7152,.0722][i],0);
      const l=[lum(fg),lum(bg)].sort((a,b)=>a-b);return {text:el.textContent,ratio:(l[1]+.05)/(l[0]+.05),color:getComputedStyle(el).color,background:bg};
    })''')
    assert pairs,selector
    for pair in pairs:assert pair['ratio']>=minimum,(selector,pair)


def exercise_appearances(page, *, choose, nav, sim, reset, checked, assert_layout, results, report):
    root=Path(__file__).resolve().parents[1]
    tokens=json.loads((root/'assets/design-tokens.json').read_text())['themes']
    captures=root/'previews/appearances';captures.mkdir(exist_ok=True)
    evidence={'browser':report['browser_version'],'url':page.url,'matrix':[],'files':[],'passed':False}
    def mode(value):
        page.locator(f'#appearance-control [data-appearance-mode={value}]').click()
        expect(page.locator(f'#appearance-control [data-appearance-mode={value}]')).to_have_attribute('aria-pressed','true')
    def screen_mode(value):expect(page.locator('#phone-screen')).to_have_attribute('data-appearance',value)
    def snapshot(name,selector='#phone-frame'):
        target=captures/name
        page.locator(selector).screenshot(path=str(target),animations='disabled')
        evidence['files'].append({'name':name,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'source':'Playwright Chromium screenshot'})
    for width,height in [(1440,1000),(1366,768),(390,844)]:
        page.set_viewport_size({'width':width,'height':height})
        for i,theme in enumerate(tokens):
            choose(page,i);reset(page)
            for appearance in ['light','dark']:
                mode(appearance);screen_mode(appearance)
                actual=page.locator('#phone-screen').evaluate('(el)=>getComputedStyle(el).getPropertyValue("--bg").trim()')
                assert actual==theme[appearance]['background'],(theme['id'],appearance,actual)
                for target in ['run','heart','history','settings']:
                    nav(page,target);assert_layout(page,f'{theme["id"]} {appearance} {target} {width}x{height}')
                    if target=='history':rendered_contrast(page,'#phone-content .history-summary .value')
                    if target=='heart':rendered_contrast(page,'#phone-content .device-option.selected strong, #phone-content .device-option.selected small, #phone-content .device-option.selected>span')
                    snapshot(f'{i+1:02}-{appearance}-{target}-{width}.png')
                    # Changing palette and theme preserves current navigation and all simulator states.
                    before=page.locator('#app-nav [aria-current=page]').get_attribute('data-page')
                    choose(page,(i+1)%10)
                    assert page.locator('#app-nav [aria-current=page]').get_attribute('data-page')==before
                    choose(page,i)
                nav(page,'run');sim(page,'session','paused');sim(page,'gps','lost');sim(page,'heart','stale')
                metric=page.locator('#phone-content .hero-number').inner_text()
                mode('dark' if appearance=='light' else 'light');mode(appearance)
                assert page.locator('#phone-content .hero-number').inner_text()==metric
                for control,value in [('session','paused'),('gps','lost'),('heart','stale')]:
                    expect(page.locator(f'#{control}-control [data-value={value}]')).to_have_attribute('aria-pressed','true')
                reset(page);nav(page,'history');page.locator('#phone-content .history-row').first.click()
                page.locator('#phone-content [data-action=export]').click()
                snapshot(f'{i+1:02}-{appearance}-dialog-{width}.png')
                mode('dark' if appearance=='light' else 'light')
                expect(page.locator('#modal-root [role=dialog]')).to_be_visible()
                mode(appearance)
                page.locator('#modal-root [data-action=close-export]').first.click()
                reset(page)
                evidence['matrix'].append({'theme':theme['id'],'appearance':appearance,'viewport':[width,height],'pages':['run','heart','history','settings','detail','dialog'],'passed':True})
            nav(page,'run')
            page.locator('#compare-toggle').click()
            expect(page.locator('#comparison .phone-screen')).to_have_count(2)
            assert page.locator('#comparison .phone-screen').evaluate_all('(els)=>els.map(e=>e.dataset.appearance)')==['light','dark']
            assert page.evaluate('document.documentElement.scrollWidth<=innerWidth+1')
            assert page.locator('#comparison .app-scroller').evaluate_all('(els)=>els.every(e=>e.scrollWidth<=e.clientWidth+1)')
            snapshot(f'{i+1:02}-comparison-{width}.png','#comparison')
            page.locator('#compare-toggle').click()
            checked(f'{theme["id"]}: light/dark, four pages, state retention, dialog, comparison at {width}x{height}')
    # OS changes while navigating, manual overrides, and live state retention.
    for i in range(10):
        choose(page,i);reset(page);nav(page,'heart');sim(page,'heart','connected')
        mode('system')
        for system in ['light','dark','light']:
            page.emulate_media(color_scheme=system);page.clock.run_for(20);screen_mode(system)
            expect(page.locator('#app-nav [data-page=heart]')).to_have_attribute('aria-current','page')
            expect(page.locator('#phone-content .bpm-number')).to_contain_text('148')
            expect(page.locator('#session-control [data-value=recording]')).to_have_attribute('aria-pressed','true')
        mode('dark');page.emulate_media(color_scheme='light');page.clock.run_for(20);screen_mode('dark')
        mode('light');page.emulate_media(color_scheme='dark');page.clock.run_for(20);screen_mode('light')
    # Every theme has a direct light/dark entry and selection changes its resolved mode.
    for i,t in enumerate(tokens):
        for appearance in ['light','dark']:
            page.get_by_role('button',name=f'{t["name"]} {appearance.title()}',exact=True).click()
            expect(page.locator('#phone-screen')).to_have_attribute('data-theme',t['id']);screen_mode(appearance)
    evidence['passed']=True;report['appearances']=evidence
    (results/'appearance-results.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n')
    mode('system');page.emulate_media(color_scheme='light');page.clock.run_for(20);reset(page)
    checked('20 palettes; 60 theme/mode/viewport combinations; 300 page/dialog + 30 comparison captures; OS follow and manual override')
