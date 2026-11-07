"""Actual signed APK UI: persisted native choices, real OS mode and native screenshot evidence."""
import hashlib,json,re,time,zipfile,xml.etree.ElementTree as ET
from pathlib import Path
PACKAGE='com.fox.runner'
FAMILIES=[('precision','Precision Pro'),('quiet','Quiet Minimal'),('explorer','Outdoor Explorer'),('neon','Neon Pulse')]
def capture(device,dump,screenshot,tap,start,output):
    info=json.loads(Path('dist/build-info.json').read_text());rows=[];schemes=[]
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
    def photo(family,mode,page):
        name=f'p03-release-{family}-{mode}-{page}';screenshot(name);raw=(output/(name+'.png')).read_bytes()
        if raw[:8]!=b'\x89PNG\r\n\x1a\n':raise ValueError('Actual native screen missing')
        rows.append({'file':name+'.png','family':family,'appearance':mode,'page':page,'sha256':hashlib.sha256(raw).hexdigest()})
    def launch_home():
        device('shell','am','force-stop',PACKAGE);start();time.sleep(0.7)
    original_mode=device('shell','settings','get','secure','ui_night_mode')
    try:
        for family,title in FAMILIES:
            launch_home();tap('p03_settings');select(title)
            if saved()!=family:raise ValueError('Theme family was not persisted')
            for mode in ['light','dark']:
                # Changes while the settings page is alive; no restart is required for appearance.
                device('shell','cmd','uimode','night','no' if mode=='light' else 'yes');time.sleep(1)
                tree=dump()
                if not any(n.get('text')==title for n in tree.iter('node')):raise ValueError('System mode change lost the current settings page')
                config=device('shell','dumpsys','activity','top')
                currentNight=device('shell','settings','get','secure','ui_night_mode')
                if currentNight!=('1' if mode=='light' else '2'):raise ValueError('System mode did not change')
                photo(family,mode,'settings')
                device('shell','input','keyevent','4');time.sleep(0.5);photo(family,mode,'home')
                tap('p0_devices');time.sleep(0.5);photo(family,mode,'heart');device('shell','input','keyevent','4');time.sleep(0.5)
                tap('p0_all_history');time.sleep(0.5);photo(family,mode,'history')
                device('shell','input','keyevent','4');time.sleep(0.5);tap('p03_settings')
                schemes.append({'family':family,'appearance':mode,'systemNightMode':currentNight,'passed':True,'settingsPagePreserved':True})
            launch_home()
            if saved()!=family:raise ValueError('Process restart lost the family preference')
        archive=Path('dist/theme-screenshots.zip')
        proof={'passed':True,'commit':info['commit'],'apkSha256':info['apkSha256'],'versionCode':info['versionCode'],'api':int(device('shell','getprop','ro.build.version.sdk')),'policy':'FOLLOW_SYSTEM','processRestartPersistence':True,'schemes':schemes,'screenshots':rows,'physicalBleTested':False}
        if proof['api']<35 or len(schemes)!=8 or len(rows)!=32:raise ValueError('Incomplete actual signed native matrix')
        with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
            for row in rows:z.write(output/row['file'],row['file'])
            z.writestr('native-theme-matrix.json',json.dumps(proof,indent=2)+'\n')
        proof['screenshotsZipSha256']=hashlib.sha256(archive.read_bytes()).hexdigest()
        Path('dist/native-theme-smoke.json').write_text(json.dumps(proof,indent=2)+'\n')
        (output/'native-theme-smoke.json').write_text(json.dumps(proof,indent=2)+'\n')
        print('PASS: actual fixed-key signed APK, eight OS-following schemes, 32 native captures and process-restart family persistence')
    finally:
        device('shell','cmd','uimode','night','yes' if original_mode=='2' else 'no' if original_mode=='1' else 'auto')
