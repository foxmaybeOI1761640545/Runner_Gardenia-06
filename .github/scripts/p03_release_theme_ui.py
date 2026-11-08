"""Actual signed APK UI: persisted native choices, real OS mode and native screenshot evidence."""
import hashlib,json,re,time,zipfile,struct,zlib,xml.etree.ElementTree as ET
from pathlib import Path
PACKAGE='com.fox.runner'
FAMILIES=[('precision','Precision Pro'),('quiet','Quiet Minimal'),('explorer','Outdoor Explorer'),('neon','Neon Pulse')]
def png_pixel(raw,x,y):
    """Read one real screencap RGB pixel using standard PNG filters; no image dependency."""
    if raw[:8]!=b'\x89PNG\r\n\x1a\n':raise ValueError('Native PNG missing')
    p=8;compressed=bytearray();width=height=channels=0
    while p<len(raw):
        size=struct.unpack_from('>I',raw,p)[0];kind=raw[p+4:p+8];data=raw[p+8:p+8+size];p+=size+12
        if kind==b'IHDR':
            width,height,depth,color,compression,filtering,interlace=struct.unpack('>IIBBBBB',data)
            if depth!=8 or color not in (2,6) or interlace!=0:raise ValueError('Unsupported native PNG layout')
            channels=4 if color==6 else 3
        elif kind==b'IDAT':compressed.extend(data)
    if not (0<=x<width and 0<=y<height):raise ValueError('Pixel outside native screenshot')
    decoded=zlib.decompress(compressed);stride=width*channels;previous=bytearray(stride)
    for row_index in range(y+1):
        at=row_index*(stride+1);kind=decoded[at];row=bytearray(decoded[at+1:at+1+stride])
        if kind not in range(5):raise ValueError('Invalid native PNG filter')
        for i in range(stride):
            left=row[i-channels] if i>=channels else 0;up=previous[i];corner=previous[i-channels] if i>=channels else 0
            if kind==1:row[i]=(row[i]+left)&255
            elif kind==2:row[i]=(row[i]+up)&255
            elif kind==3:row[i]=(row[i]+(left+up)//2)&255
            elif kind==4:
                prediction=left+up-corner;a=abs(prediction-left);b=abs(prediction-up);c=abs(prediction-corner)
                row[i]=(row[i]+(left if a<=b and a<=c else up if b<=c else corner))&255
        previous=row
    return tuple(previous[x*channels:x*channels+3])

PAGE_IDS={'home':'p0_title','heart':'p0_sensor_toolbar','history':'p0_history_list','settings':'gardenia_settings_toolbar'}
PAGE_ACTIVITIES={'home':'P0Activity','heart':'P0SensorsActivity','history':'P0HistoryActivity','settings':'P0SettingsActivity'}

def page_matches(tree,page):
    ids={n.get('resource-id') for n in tree.iter('node')}
    return PACKAGE+':id/'+PAGE_IDS[page] in ids and all(PACKAGE+':id/'+identity not in ids for other,identity in PAGE_IDS.items() if other!=page)

def capture(device,dump,screenshot,tap,start,output):
    info=json.loads(Path('dist/build-info.json').read_text());rows=[];schemes=[]
    palettes={f['id']:f for f in json.loads(Path('src/main/assets/gardenia-theme-tokens.json').read_text())['themes']}
    pref='/data/user/0/'+PACKAGE+'/shared_prefs/'+PACKAGE+'_preferences.xml'
    def select(title):
        for attempt in range(5):
            tree=dump()
            for node in tree.iter('node'):
                if node.get('text')==title:
                    b=list(map(int,re.findall(r'\d+',node.get('bounds',''))));device('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(0.8);return
            device('shell','input','swipe','540','1650','540','600','250')
        raise ValueError('Native family row unavailable: '+title)
    def saved():
        return next((n.text for n in ET.fromstring(device('shell','cat',pref)) if n.get('name')=='gardenia_theme_family'),'precision')
    def await_page(page):
        deadline=time.monotonic()+15
        while time.monotonic()<deadline:
            tree=dump();windows=device('shell','dumpsys','window')
            (output/'last-focus-window.txt').write_text(windows)
            focus=next((line.strip() for line in windows.splitlines() if 'mCurrentFocus=' in line),'')
            if page_matches(tree,page) and PAGE_ACTIVITIES[page] in focus:
                time.sleep(0.4)
                # Re-sample after the ready interval; every capture retains this exact native tree.
                final=dump()
                if page_matches(final,page):return final,focus
            time.sleep(0.2)
        raise ValueError('Actual focused native page never became ready: '+page)
    def photo(family,mode,page):
        name=f'p03-release-{family}-{mode}-{page}';expected=tuple(bytes.fromhex(palettes[family][mode]['background'][1:]));deadline=time.monotonic()+5
        while True:
            tree,focus=await_page(page)
            screenshot(name);raw=(output/(name+'.png')).read_bytes()
            if raw[:8]!=b'\x89PNG\r\n\x1a\n':raise ValueError('Actual native screen missing')
            actual=png_pixel(raw,2,300) if page in ('home','settings') else None
            if actual is None or actual==expected:break
            if time.monotonic()>=deadline:raise ValueError(f'Actual {family}/{mode}/{page} background {actual} differs from native token {expected}')
            time.sleep(0.2)
        xml=ET.tostring(tree,encoding='utf-8');(output/(name+'.xml')).write_bytes(xml)
        rows.append({'nativePageVerified':True,'focusedWindow':focus,'nativeWindowFile':name+'.xml','nativeWindowSha256':hashlib.sha256(xml).hexdigest(),'file':name+'.png','family':family,'appearance':mode,'page':page,'sha256':hashlib.sha256(raw).hexdigest(),'renderedBackgroundVerified':actual is not None,'renderedBackgroundRgb':actual})
    def launch_home():
        device('shell','am','force-stop',PACKAGE)
        # A preceding system share chooser can leave a finishing singleTop launcher task.
        # A process-restart check needs a fresh task, without clearing any application data.
        device('shell','am','start','-W','-f','0x10008000','-n',PACKAGE+'/de.dennisguse.opentracks.introduction.IntroductionActivity')
        deadline=time.monotonic()+10
        while time.monotonic()<deadline:
            if any(n.get('resource-id')==PACKAGE+':id/p0_title' for n in dump().iter('node')):return
            time.sleep(0.2)
        raise ValueError('Fresh process did not reach the actual native home')
    animation_keys=['window_animation_scale','transition_animation_scale','animator_duration_scale']
    original_animations={key:device('shell','settings','get','global',key) for key in animation_keys}
    original_mode=device('shell','cmd','uimode','night').split(':')[-1].strip()
    try:
        for key in animation_keys:device('shell','settings','put','global',key,'0')
        for family,title in FAMILIES:
            launch_home();await_page('home');tap('p03_settings');await_page('settings');select(title);await_page('settings')
            if saved()!=family:raise ValueError('Theme family was not persisted')
            for mode in ['light','dark']:
                # Changes while the settings page is alive; no restart is required for appearance.
                device('shell','cmd','uimode','night','no' if mode=='light' else 'yes');time.sleep(1)
                tree=dump()
                if not any(n.get('text')==title for n in tree.iter('node')):raise ValueError('System mode change lost the current settings page')
                effective=device('shell','cmd','uimode','night')
                expectedMode='no' if mode=='light' else 'yes'
                if not re.search(r'Night mode:\s*'+expectedMode+r'\b',effective):raise ValueError('Effective Android system mode differs: '+effective)
                currentNight='1' if mode=='light' else '2'
                persisted=device('shell','settings','get','secure','ui_night_mode')
                photo(family,mode,'settings')
                device('shell','input','keyevent','4');await_page('home');photo(family,mode,'home')
                tap('p0_devices');await_page('heart');photo(family,mode,'heart');device('shell','input','keyevent','4');await_page('home')
                tap('p0_all_history');await_page('history');photo(family,mode,'history')
                device('shell','input','keyevent','4');await_page('home');tap('p03_settings');await_page('settings')
                schemes.append({'family':family,'appearance':mode,'systemNightMode':currentNight,'effectiveSystemModeQuery':effective,'persistedNightSettingDiagnostic':persisted,'renderedPaletteVerified':True,'passed':True,'settingsPagePreserved':True})
            launch_home()
            if saved()!=family:raise ValueError('Process restart lost the family preference')
        archive=Path('dist/theme-screenshots.zip')
        proof={'passed':True,'commit':info['commit'],'apkSha256':info['apkSha256'],'versionCode':info['versionCode'],'api':int(device('shell','getprop','ro.build.version.sdk')),'policy':'FOLLOW_SYSTEM','processRestartPersistence':True,'schemes':schemes,'screenshots':rows,'physicalBleTested':False,'evaluationCommit':__import__('os').environ['GITHUB_SHA'],'evaluationRunId':__import__('os').environ['GITHUB_RUN_ID'],'animationsDisabledForCapture':True,'nativePageChecks':32}
        if proof['api']<35 or len(schemes)!=8 or len(rows)!=32:raise ValueError('Incomplete actual signed native matrix')
        with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
            for row in rows:
                z.write(output/row['file'],row['file']);z.write(output/row['nativeWindowFile'],row['nativeWindowFile'])
            z.writestr('native-theme-matrix.json',json.dumps(proof,indent=2)+'\n')
        proof['screenshotsZipSha256']=hashlib.sha256(archive.read_bytes()).hexdigest()
        Path('dist/native-theme-smoke.json').write_text(json.dumps(proof,indent=2)+'\n')
        (output/'native-theme-smoke.json').write_text(json.dumps(proof,indent=2)+'\n')
        print('PASS: actual fixed-key signed APK, eight OS-following schemes, 32 native captures and process-restart family persistence')
    finally:
        for key,value in original_animations.items():
            device('shell','settings','delete','global',key) if value=='null' else device('shell','settings','put','global',key,value)
        device('shell','cmd','uimode','night',original_mode if original_mode in ('yes','no','auto','custom') else 'auto')
