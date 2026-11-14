"""Actual optimized signed APK: native pages, persisted metrics and batch files, no test APK."""
import hashlib,json,os,re,sqlite3,subprocess,tempfile,time,zipfile,xml.etree.ElementTree as ET
from pathlib import Path
from p04_release_theme_ui import png_pixel,FAMILIES
from p061_version import VERSION
PACKAGE='com.fox.runner'
PAGE_IDS={'home':'p0_title','heart':'p0_sensor_toolbar','history':'p0_history_list','hub':'gardenia_settings_toolbar','appearance':'gardenia_settings_toolbar','motion':'gardenia_settings_toolbar','info':'gardenia_settings_toolbar','selection':'p05_selected_count','share':'alertTitle','delete':'alertTitle','offlineMap':'p06_map_container'}
PAGE_ACTIVITIES={p:('P06MapActivity' if p=='offlineMap' else 'P0Activity' if p=='home' else 'P0SensorsActivity' if p=='heart' else 'P0HistoryActivity' if p in ('history','selection','share','delete') else 'P0SettingsActivity') for p in PAGE_IDS}

def page_matches(tree,page):
    nodes=list(tree.iter('node'));ids={n.get('resource-id') for n in nodes};texts={n.get('text','') for n in nodes}
    identity=PAGE_IDS[page];identity=identity if identity.startswith('android:') else PACKAGE+':id/'+identity
    if identity not in ids:return False
    if page=='history':return PACKAGE+':id/p05_selected_count' not in ids
    if page=='hub':return len({'Theme and appearance','Workout and display','Heart rate and devices','History and data','Privacy and offline','App information'} & texts)>=2
    if page=='appearance':return {t for _,t in FAMILIES}.issubset(texts)
    if page=='motion':return {'Preferred units','Preferred rate','Restore default metric layout'}.issubset(texts)
    if page=='info':return {'Release version','Build number'}.issubset(texts)
    if page=='share':return 'Separate GPX files' in texts and any(t.startswith('Share selected records') for t in texts)
    if page=='delete':return any('Permanently delete selected records' in t for t in texts)
    return True

def scrolled_home(tree,page,focus):
    """A map return can restore the home ScrollView below its title."""
    ids={n.get('resource-id') for n in tree.iter('node')}
    return (page=='home' and 'P0Activity' in focus
        and PACKAGE+':id/p02_home_scroll' in ids
        and PACKAGE+':id/p0_title' not in ids)

def capture(device,dump,screenshot,tap,start,output):
    info=json.loads(Path('dist/build-info.json').read_text());rows=[];schemes=[];batches=[];root='/data/user/0/'+PACKAGE
    pref=root+'/shared_prefs/'+PACKAGE+'_preferences.xml';palettes={x['id']:x for x in json.loads(Path('src/main/assets/gardenia-theme-tokens.json').read_text())['themes']}
    def preferences():return {n.get('name'):n.text if n.tag=='string' else n.get('value') for n in ET.fromstring(device('shell','cat',pref))}
    def select(text,long=False):
        for attempt in range(8):
            for node in dump().iter('node'):
                if node.get('text')==text:
                    b=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
                    if len(b)==4 and b[2]>b[0] and b[3]>b[1]:
                        x=str((b[0]+b[2])//2);y=str((b[1]+b[3])//2)
                        device('shell','input','swipe',x,y,x,y,'1000') if long else device('shell','input','tap',x,y)
                        time.sleep(.25);return
            device('shell','input','swipe','360','1100','360','400','200')
        raise ValueError('Native preference/record unavailable: '+text)
    def await_page(page):
        until=time.monotonic()+15
        while time.monotonic()<until:
            tree=dump();windows=device('shell','dumpsys','window');focus=next((x.strip() for x in windows.splitlines() if 'mCurrentFocus=' in x),'')
            if page_matches(tree,page) and PAGE_ACTIVITIES[page] in focus:time.sleep(.2);return dump(),focus
            if scrolled_home(tree,page,focus):
                # Normal native touch scroll only after the actual home Activity
                # is focused. Keep requiring the original visible title below.
                device('shell','input','swipe','360','400','360','1200','200')
            time.sleep(.15)
        print(json.dumps({'expectedNativePage':page,'actualFocus':focus,'visibleNativeIds':sorted({n.get('resource-id','') for n in tree.iter('node')})}),flush=True)
        raise ValueError('Focused native page unavailable: '+page)
    def photo(family,mode,page):
        tree,focus=await_page(page);name=f'p06-release-{family}-{mode}-{page}';screenshot(name);raw=(output/(name+'.png')).read_bytes();xml=ET.tostring(tree,encoding='utf-8');(output/(name+'.xml')).write_bytes(xml)
        if page=='offlineMap':
            from p06_release_map_ui import verify_pixels
            verify_pixels(raw,tree)
        actual=None
        if page in ('home','hub'):
            actual=png_pixel(raw,2,300);expected=tuple(bytes.fromhex(palettes[family][mode]['background'][1:]))
            if actual!=expected:raise ValueError('Signed native palette differs: '+str((family,mode,page,actual,expected)))
        rows.append({'file':name+'.png','sha256':hashlib.sha256(raw).hexdigest(),'nativeWindowFile':name+'.xml','nativeWindowSha256':hashlib.sha256(xml).hexdigest(),'family':family,'appearance':mode,'page':page,'nativePageVerified':True,'focusedWindow':focus,'renderedBackgroundVerified':actual is not None,'renderedBackgroundRgb':actual})
        print('Actual signed native page '+name,flush=True)
    def home():
        device('shell','am','force-stop',PACKAGE);device('shell','am','start','-W','-f','0x10008000','-n',PACKAGE+'/de.dennisguse.opentracks.introduction.IntroductionActivity');await_page('home')
    def back(page):device('shell','input','keyevent','4');await_page(page)
    def settings(page):
        tap('p03_settings');await_page('hub')
        if page!='hub':select({'appearance':'Theme and appearance','motion':'Workout and display','info':'App information'}[page]);await_page(page)
    def snapshot():
        with tempfile.TemporaryDirectory(prefix='p05-signed-db-',dir=os.environ['RUNNER_TEMP']) as temporary:
            device('pull',root+'/databases/',temporary,timeout=60);paths=list(Path(temporary).rglob('database.db'))
            if len(paths)!=1:raise ValueError('Actual signed database missing')
            with sqlite3.connect(paths[0]) as db:
                data={name:db.execute('SELECT * FROM '+name+' ORDER BY _id').fetchall() for name in ('tracks','trackpoints','heart_rate_samples')}
                ids={str(row[0]) for row in data['tracks']}
            raw=json.dumps(data,default=lambda x:x.hex() if isinstance(x,bytes) else str(x),sort_keys=True).encode();return hashlib.sha256(raw).hexdigest(),ids
    def cached(suffix):return {p for p in device('shell','find',root+'/cache/p0_exports','-name','*'+suffix,check=False).splitlines() if '/batch-' in p}
    def chooser():
        until=time.monotonic()+10
        while time.monotonic()<until:
            focus=device('shell','dumpsys','window')
            if any(s in focus for s in ('ChooserActivity','ResolverActivity','intentresolver')):return
            time.sleep(.2)
        raise ValueError('Actual Android Sharesheet did not become visible')
    def actual_share(family,kind):
        before=cached('.zip' if kind=='zip' else '.gpx')
        # Dialog buttons use Android framework IDs; find their actual native bounds.
        button('android:id/button1');chooser();screenshot('p05-release-'+family+'-'+kind+'-system-sharesheet')
        until=time.monotonic()+15;created=set()
        while time.monotonic()<until:
            created=cached('.zip' if kind=='zip' else '.gpx')-before
            if len(created)==(1 if kind=='zip' else len(expected_ids)):break
            time.sleep(.2)
        if len(created)!=(1 if kind=='zip' else len(expected_ids)):raise ValueError('Signed batch silently omitted files')
        actual={};files=[]
        for i,path in enumerate(sorted(created)):
            local=output/f'p05-{family}-{kind}-{i}{".zip" if kind=="zip" else ".gpx"}';device('pull',path,local);files.append({'file':local.name,'sha256':hashlib.sha256(local.read_bytes()).hexdigest()})
            if kind=='zip':
                with zipfile.ZipFile(local) as archive:
                    if len(set(archive.namelist()))!=len(expected_ids) or len(archive.namelist())!=len(expected_ids) or archive.testzip() is not None:raise ValueError('Signed ZIP entries incomplete/CRC failed')
                    for name in archive.namelist():
                        if Path(name).name!=name or not name.endswith('.gpx'):raise ValueError('Unsafe signed ZIP entry')
                        raw=archive.read(name);entry=output/('p05-'+family+'-zip-'+name);entry.write_bytes(raw);identity=parse(entry);actual[identity]=hashlib.sha256(raw).hexdigest()
            else:actual[parse(local)]=hashlib.sha256(local.read_bytes()).hexdigest()
        if set(actual)!=expected_ids:raise ValueError('Signed batch IDs differ from selected actual records')
        if kind=='multiple' and actual!=zip_expected[family]:raise ValueError('Direct GPX differs from the same selected ZIP entries')
        if kind=='zip':zip_expected[family]=actual
        batches.append({'family':family,'kind':kind,'workoutIds':sorted(actual),'gpxSha256':actual,'files':files,'actualSystemSharesheet':True,'schemaVerified':True})
        device('shell','input','keyevent','4');await_page('selection')
    def parse(path):
        doc=ET.parse(path).getroot();record=doc.find('./{http://www.topografix.com/GPX/1/1}extensions/{urn:runner-gardenia:gpx:1}workout')
        if record is None:raise ValueError('Signed GPX identity missing')
        subprocess.run(['java','.github/scripts/ValidateP0Gpx.java','.github/schemas/gpx-1.1.xsd',str(path)],check=True)
        return record.get('id')
    def button(identity):
        for node in dump().iter('node'):
            if node.get('resource-id')==identity:
                b=list(map(int,re.findall(r'\d+',node.get('bounds',''))));device('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));return
        raise ValueError('Native dialog button missing: '+identity)
    before_signature,expected_ids=snapshot();zip_expected={}
    if len(expected_ids)<2:raise ValueError('Upgrade and actual recording fixtures both required')
    animation_keys=('window_animation_scale','transition_animation_scale','animator_duration_scale');saved_animations={k:device('shell','settings','get','global',k) for k in animation_keys};saved_mode=device('shell','cmd','uimode','night').split(':')[-1].strip()
    device('shell','wm','size','720x1600');device('shell','wm','density','320');device('shell','settings','put','system','font_scale','1.3')
    try:
        for k in animation_keys:device('shell','settings','put','global',k,'0')
        for family,title in FAMILIES:
            home();settings('appearance');select(title);await_page('appearance')
            if preferences().get('gardenia_theme_family')!=family:raise ValueError('Native family choice not persisted')
            for mode in ('light','dark'):
                device('shell','cmd','uimode','night','no' if mode=='light' else 'yes');
                if device('shell','cmd','uimode','night').split(':')[-1].strip()!=('no' if mode=='light' else 'yes'):raise ValueError('Actual OS appearance differs')
                await_page('appearance');photo(family,mode,'appearance');back('hub');photo(family,mode,'hub')
                select('Workout and display');await_page('motion');photo(family,mode,'motion');back('hub');select('App information');tree,_=await_page('info')
                texts={n.get('text') for n in tree.iter('node')}
                if VERSION not in texts or str(info['versionCode']) not in texts:raise ValueError('Actual formal app info version differs')
                expected_label=next(n.text for n in ET.parse('src/main/res/values/p0.xml').getroot() if n.get('name')=='p0_title')
                if expected_label not in texts:raise ValueError('App information must display the actual Runner Gardenia application label')
                photo(family,mode,'info');back('hub');back('home');photo(family,mode,'home');tap('p04_route');await_page('offlineMap');time.sleep(1);photo(family,mode,'offlineMap');back('home');tap('p0_devices');await_page('heart');photo(family,mode,'heart');back('home');tap('p0_all_history');await_page('history');photo(family,mode,'history')
                select('Synthetic retained P0 workout',long=True);await_page('selection');tap('p05_batch_select_all');photo(family,mode,'selection');tap('p05_batch_share');await_page('share');photo(family,mode,'share')
                if mode=='light':actual_share(family,'zip')
                else:button('android:id/button2');await_page('selection')
                tap('p05_batch_delete');await_page('delete');photo(family,mode,'delete');button('android:id/button2');await_page('selection')
                if mode=='light':tap('p05_batch_share');await_page('share');select('Separate GPX files');actual_share(family,'multiple')
                tap('p05_batch_cancel');await_page('history');back('home')
                # Exact swap and a genuinely new app process, through real native views and prefs.
                defaults=['HEART_RATE','DISTANCE','DURATION','PACE_SPEED'] if family=='neon' else ['DISTANCE','DURATION','PACE_SPEED','HEART_RATE'];expected=list(defaults);expected[0],expected[1]=expected[1],expected[0]
                all_before=preferences();tap('p05_secondary_1');until=time.monotonic()+5
                while preferences().get('gardenia_metric_layout_'+family)!=','.join(expected):
                    if time.monotonic()>until:raise ValueError('Exact native metric swap did not persist')
                    time.sleep(.1)
                home();tree,_=await_page('home');primary=next((n.get('text') for n in tree.iter('node') if n.get('resource-id')==PACKAGE+':id/p05_primary_label'),None)
                if primary!=('Distance' if family=='neon' else 'Active time'):raise ValueError('New signed process did not restore primary metric')
                settings('motion');select('Restore default metric layout');until=time.monotonic()+5
                while 'gardenia_metric_layout_'+family in preferences():
                    if time.monotonic()>until:raise ValueError('Current theme reset did not remove its override')
                    time.sleep(.1)
                now=preferences()
                for other,_ in FAMILIES:
                    if other!=family and now.get('gardenia_metric_layout_'+other)!=all_before.get('gardenia_metric_layout_'+other):raise ValueError('Reset changed another theme layout')
                back('hub');back('home');tree,_=await_page('home')
                primary=next((n.get('text') for n in tree.iter('node') if n.get('resource-id')==PACKAGE+':id/p05_primary_label'),None)
                if primary!=('Heart rate' if family=='neon' else 'Distance'):raise ValueError('Reset did not render the actual family default')
                settings('appearance')
                schemes.append({'family':family,'appearance':mode,'passed':True,'renderedPaletteVerified':True,'settingsPagePreserved':True,'metricProcessPersistence':True,'currentThemeResetOnly':True})
        after_signature,after_ids=snapshot()
        if after_signature!=before_signature or after_ids!=expected_ids:raise ValueError('Signed sharing/cancelled deletion changed original workout/GPS/heart data')
        proof={'passed':True,'commit':info['commit'],'apkSha256':info['apkSha256'],'api':int(device('shell','getprop','ro.build.version.sdk')),'policy':'FOLLOW_SYSTEM','schemes':schemes,'screenshots':rows,'nativePageChecks':len(rows),'processRestartPersistence':True,'metricProcessPersistence':True,'historyBatchVerified':len(batches)==8,'sharePreservesData':True,'appVersionVerified':True,'sourceDataSignature':before_signature,'batchShares':batches,'syntheticUpgradeAndGpsInputs':True,'physicalBleTested':False,'wechat':'NEEDS_DEVICE_VALIDATION','workflowCommit':os.environ['GITHUB_SHA'],'evaluationRunId':os.environ['GITHUB_RUN_ID']}
        if proof['api']!=35 or len(rows)!=88 or len(schemes)!=8 or len(batches)!=8:raise ValueError('Signed native matrix incomplete')
        archive=Path('dist/theme-screenshots.zip')
        with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
            for row in rows:z.write(output/row['file'],row['file']);z.write(output/row['nativeWindowFile'],row['nativeWindowFile'])
            z.writestr('native-theme-matrix.json',json.dumps(proof,indent=2)+'\n')
        proof['screenshotsZipSha256']=hashlib.sha256(archive.read_bytes()).hexdigest();Path('dist/native-theme-smoke.json').write_text(json.dumps(proof,indent=2)+'\n');(output/'native-theme-smoke.json').write_text(json.dumps(proof,indent=2)+'\n')
    finally:
        for k,v in saved_animations.items():device('shell','settings','delete','global',k) if v=='null' else device('shell','settings','put','global',k,v)
        device('shell','cmd','uimode','night',saved_mode if saved_mode in ('yes','no','auto','custom') else 'auto')
