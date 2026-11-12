"""Actual signed APK map import/render/GPS/history/restart in flight mode; no test APK."""
import hashlib,json,re,time,threading,zipfile
from pathlib import Path
import xml.etree.ElementTree as ET
import struct,zlib
PACKAGE='com.fox.runner'

def png_pixels(raw,coordinates):
    if raw[:8]!=b'\x89PNG\r\n\x1a\n':raise ValueError('Native PNG missing')
    offset=8;compressed=bytearray();width=height=channels=0
    while offset<len(raw):
        size=struct.unpack_from('>I',raw,offset)[0];kind=raw[offset+4:offset+8];data=raw[offset+8:offset+8+size];offset+=size+12
        if kind==b'IHDR':
            width,height,depth,color,_,_,interlace=struct.unpack('>IIBBBBB',data)
            if depth!=8 or color not in (2,6) or interlace:raise ValueError('Unsupported native PNG')
            channels=4 if color==6 else 3
        elif kind==b'IDAT':compressed.extend(data)
    if not coordinates or any(not(0<=x<width and 0<=y<height) for x,y in coordinates):raise ValueError('Invalid native pixel coordinates')
    decoded=zlib.decompress(compressed);stride=width*channels;previous=bytearray(stride);wanted=set(coordinates);found={}
    for y in range(max(y for _,y in coordinates)+1):
        at=y*(stride+1);kind=decoded[at];row=bytearray(decoded[at+1:at+1+stride])
        if kind not in range(5):raise ValueError('Invalid PNG filter')
        if kind:
            for i in range(stride):
                left=row[i-channels] if i>=channels else 0;up=previous[i];corner=previous[i-channels] if i>=channels else 0
                if kind==1:value=left
                elif kind==2:value=up
                elif kind==3:value=(left+up)//2
                else:
                    predict=left+up-corner;a=abs(predict-left);b=abs(predict-up);c=abs(predict-corner);value=left if a<=b and a<=c else up if b<=c else corner
                row[i]=(row[i]+value)&255
        for x,py in wanted:
            if py==y:found[x,py]=tuple(row[x*channels:x*channels+3])
        previous=row
    return [found[point] for point in coordinates]


def verify_pixels(raw,tree=None):
    # The fixture's distributed green park fill cannot come from blank tiles,
    # the purple position marker or the teal track overlay. Decode once/image.
    width,height=struct.unpack('>II',raw[16:24]);bounds=None
    if tree is not None:
        node=next((n for n in tree.iter('node') if n.get('resource-id')==PACKAGE+':id/p06_map_container'),None)
        if node is not None:bounds=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
    if not bounds or len(bounds)!=4:bounds=[0,height//4,width,height*9//10]
    left,top,right,bottom=bounds
    coordinates=[(int(left+(right-left)*x/10),int(top+(bottom-top)*y/10)) for x in range(2,9,2) for y in range(2,9,2)]
    colors=png_pixels(raw,coordinates);green=sum(g>r+10 and g>b+10 for r,g,b in colors)
    if green<3:raise ValueError('Actual synthetic park has not rendered in native offline canvas')
    return {'distributedParkPixels':green,'sampleColors':len(set(colors))}

def verify_track_pixels(raw,tree,min_span=0):
    node=next((n for n in tree.iter('node') if n.get('resource-id')==PACKAGE+':id/p06_map_container'),None)
    bounds=[] if node is None else list(map(int,re.findall(r'\d+',node.get('bounds',''))))
    if len(bounds)!=4:raise ValueError('Actual route canvas bounds missing')
    left,top,right,bottom=bounds;center=(left+right)//2
    coordinates=[(x,y) for x in range(max(left,center-8),min(right,center+9),2) for y in range(top+2,bottom-2,3)]
    colors=png_pixels(raw,coordinates)
    hits=[y for (_,y),(r,g,b) in zip(coordinates,colors) if r<30 and 100<=g<=155 and 110<=b<=165 and abs(g-b)<25]
    span=max(hits)-min(hits) if hits else 0
    if len(hits)<3 or span<min_span:raise ValueError('Actual selected workout track has not rendered in native offline canvas')
    return {'trackPixels':len(hits),'trackSpanPixels':span}


def capture_runtime(device,dump,screenshot,tap,start,output,history_id,history_title):
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
    def photo(name,track=False):
        tree=dump();windows=device('shell','dumpsys','window');focus=next((l.strip() for l in windows.splitlines() if 'mCurrentFocus=' in l),'')
        if 'P06MapActivity' not in focus:raise ValueError('Signed native map is not the focused page')
        filename='p06-signed-map-'+name;screenshot(filename);raw=(output/(filename+'.png')).read_bytes();pixels=verify_pixels(raw,tree)
        if track:pixels.update(verify_track_pixels(raw,tree,32 if name=='history-restarted' else 0))
        xml=ET.tostring(tree,encoding='utf-8');(output/(filename+'.xml')).write_bytes(xml)
        rows.append({'file':filename+'.png','sha256':hashlib.sha256(raw).hexdigest(),'nativeWindowFile':filename+'.xml','nativeWindowSha256':hashlib.sha256(xml).hexdigest(),'focusedWindow':focus,'pixels':pixels})
    def home():
        # The inherited upgrade smoke ends in a historical-detail/share task.
        # Use the same clear-task launcher as signed theme capture, then wait
        # for the actual home window before searching its scrollable route card.
        device('shell','am','force-stop',PACKAGE)
        device('shell','am','start','-W','-f','0x10008000','-n',PACKAGE+'/de.dennisguse.opentracks.introduction.IntroductionActivity')
        until=time.monotonic()+15
        while time.monotonic()<until:
            tree=dump();focus=device('shell','dumpsys','window')
            if 'P0Activity' in next((line for line in focus.splitlines() if 'mCurrentFocus=' in line),'') and any(n.get('resource-id')==PACKAGE+':id/p0_title' for n in tree.iter('node')):return
            time.sleep(.3)
        raise ValueError('Signed map runtime could not reach the actual home window')
    device('shell','mkdir','-p','/sdcard/Download');device('push','src/androidTest/assets/p06/gardenia-synthetic.map','/sdcard/Download/Gardenia-synthetic.map')
    bad=output/'Gardenia-broken.map';bad.write_bytes(b'unsupported offline-map fixture');device('push',bad,'/sdcard/Download/Gardenia-broken.map')
    device('shell','cmd','connectivity','airplane-mode','enable')
    if device('shell','settings','get','global','airplane_mode_on')!='1':raise ValueError('Actual flight mode is required')
    home();tap('p04_route');picker('Gardenia-synthetic.map');wait_map();photo('imported-airplane')
    # Persisted copy must survive losing the external document and a new process.
    device('shell','rm','/sdcard/Download/Gardenia-synthetic.map');home();tap('p0_all_history');text(history_title)
    until=time.monotonic()+15
    while not any(('ID '+str(history_id)) in n.get('text','') for n in dump().iter('node')):
        if time.monotonic()>until:raise ValueError('Native history detail did not open the recorded workout ID')
        time.sleep(.3)
    text('More options',description=True);text('Offline map');wait_map();tap('p06_fit_track');time.sleep(1);photo('history-restarted',track=True)
    # A historical map must be entered through History, not an idle home session.
    home();tap('p0_start');
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
        time.sleep(3);tap('p04_route');wait_map();tap('p06_my_position');time.sleep(2);photo('live-current-gps',track=True)
        picker('Gardenia-broken.map');time.sleep(1)
        if not any('Cannot import or open this map' in n.get('text','') for n in dump().iter('node')):raise ValueError('Signed APK did not reject corrupt map')
        photo('invalid-retains-map',track=True);device('shell','input','keyevent','4');tap('p0_finish');time.sleep(1)
    finally:stop.set();thread.join(timeout=10)
    if thread.is_alive() or errors:raise ValueError('Ordinary emulator GNSS feeder failed: '+str(errors))
    # The extra workout above uses normal UI/driver commands. The inherited
    # native batch capture next snapshots all retained and newly recorded rows.
    proof={'passed':True,'commit':info['commit'],'apkSha256':info['apkSha256'],'airplaneMode':True,'safImport':True,'originalDocumentRemoved':True,'newProcessReload':True,'liveGpsAndTrack':True,'historyTrack':True,'historyEntryThroughNativeUi':True,'historicalWorkoutId':history_id,'invalidRetainsMap':True,'screenshots':rows}
    if len(rows)!=4:raise ValueError('Signed map runtime matrix incomplete')
    archive=Path('dist/offline-map-screenshots.zip')
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
        for row in rows:z.write(output/row['file'],row['file']);z.write(output/row['nativeWindowFile'],row['nativeWindowFile'])
    proof['screenshotsZipSha256']=hashlib.sha256(archive.read_bytes()).hexdigest();Path('dist/offline-map-smoke.json').write_text(json.dumps(proof,indent=2)+'\n')

def verify_proof(folder,info):
    proof=json.loads((folder/'offline-map-smoke.json').read_text());raw=(folder/'offline-map-screenshots.zip').read_bytes()
    if proof.get('commit')!=info['commit'] or proof.get('apkSha256')!=info['apkSha256'] or any(proof.get(k) is not True for k in ('passed','airplaneMode','safImport','originalDocumentRemoved','newProcessReload','liveGpsAndTrack','historyTrack','historyEntryThroughNativeUi','invalidRetainsMap')) or len(proof.get('screenshots',[]))!=4 or hashlib.sha256(raw).hexdigest()!=proof['screenshotsZipSha256']:raise ValueError('Exact signed map runtime/airplane/restart evidence missing')
    if not isinstance(proof.get('historicalWorkoutId'),int) or proof['historicalWorkoutId']<=0:raise ValueError('Selected native history workout ID missing')
    expected={'p06-signed-map-'+name+'.png' for name in ('imported-airplane','history-restarted','live-current-gps','invalid-retains-map')}
    if {row.get('file') for row in proof['screenshots']}!=expected:raise ValueError('Signed map runtime scenes differ from required coverage')
    import io
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        for row in proof['screenshots']:
            png=z.read(row['file']);xml=z.read(row['nativeWindowFile'])
            if hashlib.sha256(png).hexdigest()!=row['sha256'] or hashlib.sha256(xml).hexdigest()!=row['nativeWindowSha256'] or 'P06MapActivity' not in row['focusedWindow']:raise ValueError('Signed map focused image/XML changed')
            if not any(n.get('resource-id')==PACKAGE+':id/p06_map_container' for n in ET.fromstring(xml).iter('node')):raise ValueError('Native map page absent')
            tree=ET.fromstring(xml);verify_pixels(png,tree)
            if any(name in row['file'] for name in ('history-restarted','live-current-gps','invalid-retains-map')):verify_track_pixels(png,tree,32 if 'history-restarted' in row['file'] else 0)
    return proof
