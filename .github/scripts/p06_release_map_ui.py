"""Actual signed APK map import/render/GPS/history/restart in flight mode; no test APK."""
import hashlib,json,re,time,threading,zipfile
from pathlib import Path
import xml.etree.ElementTree as ET
from p04_release_theme_ui import png_pixel
PACKAGE='com.fox.runner'

def verify_pixels(raw):
    # Sample actual map canvas away from bars. Synthetic park is green in the
    # unmodified bundled Mapsforge theme; neither blank tiles nor track strokes
    # can supply this distributed polygon fill.
    import struct
    width,height=struct.unpack('>II',raw[16:24]);colors=[]
    for x in range(width//5,4*width//5,max(1,width//8)):
        for y in range(height*2//5,height*4//5,max(1,height//10)):
            colors.append(png_pixel(raw,x,y))
    green=sum(g>r+10 and g>b+10 for r,g,b in colors)
    if green<3 or len(set(colors))<3:raise ValueError('Actual synthetic park/roads have not rendered in native offline canvas')
    return {'distributedParkPixels':green,'sampleColors':len(set(colors))}

def capture_runtime(device,dump,screenshot,tap,start,output):
    info=json.loads(Path('dist/build-info.json').read_text());rows=[]
    def click_node(node):
        bounds=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
        if len(bounds)!=4 or bounds[2]<=bounds[0] or bounds[3]<=bounds[1]:raise ValueError('Native map control not visible')
        device('shell','input','tap',str((bounds[0]+bounds[2])//2),str((bounds[1]+bounds[3])//2));time.sleep(.3)
    def text(value,description=False):
        for _ in range(12):
            for node in dump().iter('node'):
                if node.get('content-desc' if description else 'text')==value:click_node(node);return
            time.sleep(.25)
        raise ValueError('Actual document control missing: '+value)
    def wait_map():
        until=time.monotonic()+20
        while time.monotonic()<until:
            tree=dump();ids={n.get('resource-id') for n in tree.iter('node')}
            if PACKAGE+':id/p06_map_container' in ids and any('Gardenia-synthetic.map' in n.get('text','') and 'local map' in n.get('text','') for n in tree.iter('node')):
                time.sleep(1);return
            time.sleep(.3)
        raise ValueError('Installed signed APK did not open imported local map')
    def picker(filename):
        tap('p06_import_map');until=time.monotonic()+15
        while time.monotonic()<until:
            tree=dump();nodes=list(tree.iter('node'))
            if any(n.get('text')==filename for n in nodes):text(filename);return
            drawer=next((n for n in nodes if n.get('content-desc') in ('Show roots','Open navigation drawer')),None)
            if drawer is not None:click_node(drawer);text('Downloads');time.sleep(.5);continue
            download=next((n for n in nodes if n.get('text')=='Downloads'),None)
            if download is not None:click_node(download);time.sleep(.5);continue
            time.sleep(.3)
        raise ValueError('Real SAF picker did not offer local fixture '+filename)
    def photo(name):
        tree=dump();windows=device('shell','dumpsys','window');focus=next((l.strip() for l in windows.splitlines() if 'mCurrentFocus=' in l),'')
        if 'P06MapActivity' not in focus:raise ValueError('Signed native map is not the focused page')
        filename='p06-signed-map-'+name;screenshot(filename);raw=(output/(filename+'.png')).read_bytes();pixels=verify_pixels(raw);xml=ET.tostring(tree,encoding='utf-8');(output/(filename+'.xml')).write_bytes(xml)
        rows.append({'file':filename+'.png','sha256':hashlib.sha256(raw).hexdigest(),'nativeWindowFile':filename+'.xml','nativeWindowSha256':hashlib.sha256(xml).hexdigest(),'focusedWindow':focus,'pixels':pixels})
    def home():
        device('shell','am','force-stop',PACKAGE);start();time.sleep(1)
    device('shell','mkdir','-p','/sdcard/Download');device('push','src/androidTest/assets/p06/gardenia-synthetic.map','/sdcard/Download/Gardenia-synthetic.map')
    bad=output/'Gardenia-broken.map';bad.write_bytes(b'unsupported offline-map fixture');device('push',bad,'/sdcard/Download/Gardenia-broken.map')
    device('shell','cmd','connectivity','airplane-mode','enable')
    if device('shell','settings','get','global','airplane_mode_on')!='1':raise ValueError('Actual flight mode is required')
    home();tap('p04_route');picker('Gardenia-synthetic.map');wait_map();photo('imported-airplane')
    # Persisted copy must survive losing the external document and a new process.
    device('shell','rm','/sdcard/Download/Gardenia-synthetic.map');home();tap('p04_route');wait_map();photo('history-restarted')
    device('shell','input','keyevent','4');tap('p0_start');
    for _ in range(4):device('emu','geo','fix','103.8','1.3','12','8','4');time.sleep(1)
    # Feed the ordinary emulator GNSS driver while native UI commands run.
    stop=threading.Event();errors=[]
    def feed():
        try:
            i=0
            while not stop.is_set():device('emu','geo','fix','103.8',str(1.3+min(i,40)*.000018),'12','8','4');i+=1;stop.wait(1)
        except Exception as failure:errors.append(str(failure))
    thread=threading.Thread(target=feed);thread.start()
    try:
        time.sleep(3);tap('p04_route');wait_map();tap('p06_my_position');time.sleep(2);photo('live-current-gps')
        picker('Gardenia-broken.map');time.sleep(1)
        if not any('Cannot import or open this map' in n.get('text','') for n in dump().iter('node')):raise ValueError('Signed APK did not reject corrupt map')
        photo('invalid-retains-map');device('shell','input','keyevent','4');tap('p0_finish');time.sleep(1)
    finally:stop.set();thread.join(timeout=10)
    if thread.is_alive() or errors:raise ValueError('Ordinary emulator GNSS feeder failed: '+str(errors))
    # The extra workout above uses normal UI/driver commands. The inherited
    # native batch capture next snapshots all retained and newly recorded rows.
    proof={'passed':True,'commit':info['commit'],'apkSha256':info['apkSha256'],'airplaneMode':True,'safImport':True,'originalDocumentRemoved':True,'newProcessReload':True,'liveGpsAndTrack':True,'historyTrack':True,'invalidRetainsMap':True,'screenshots':rows}
    if len(rows)!=4:raise ValueError('Signed map runtime matrix incomplete')
    archive=Path('dist/offline-map-screenshots.zip')
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
        for row in rows:z.write(output/row['file'],row['file']);z.write(output/row['nativeWindowFile'],row['nativeWindowFile'])
    proof['screenshotsZipSha256']=hashlib.sha256(archive.read_bytes()).hexdigest();Path('dist/offline-map-smoke.json').write_text(json.dumps(proof,indent=2)+'\n')

def verify_proof(folder,info):
    proof=json.loads((folder/'offline-map-smoke.json').read_text());raw=(folder/'offline-map-screenshots.zip').read_bytes()
    if proof.get('commit')!=info['commit'] or proof.get('apkSha256')!=info['apkSha256'] or any(proof.get(k) is not True for k in ('passed','airplaneMode','safImport','originalDocumentRemoved','newProcessReload','liveGpsAndTrack','historyTrack','invalidRetainsMap')) or len(proof.get('screenshots',[]))!=4 or hashlib.sha256(raw).hexdigest()!=proof['screenshotsZipSha256']:raise ValueError('Exact signed map runtime/airplane/restart evidence missing')
    import io
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        for row in proof['screenshots']:
            png=z.read(row['file']);xml=z.read(row['nativeWindowFile'])
            if hashlib.sha256(png).hexdigest()!=row['sha256'] or hashlib.sha256(xml).hexdigest()!=row['nativeWindowSha256'] or 'P06MapActivity' not in row['focusedWindow']:raise ValueError('Signed map focused image/XML changed')
            if not any(n.get('resource-id')==PACKAGE+':id/p06_map_container' for n in ET.fromstring(xml).iter('node')):raise ValueError('Native map page absent')
            verify_pixels(png)
    return proof
