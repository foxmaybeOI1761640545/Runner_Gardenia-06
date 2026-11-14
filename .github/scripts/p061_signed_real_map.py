"""Real 100MB map on the optimized fixed-key APK, using native SAF and ordinary emulator GNSS."""
import hashlib,json,re,threading,time,zipfile,xml.etree.ElementTree as ET
from pathlib import Path
from p061_real_map import NAME,SHA,BYTES
from p06_release_map_ui import png_pixels,verify_track_pixels
PACKAGE='com.fox.runner'

def verify_pixels(raw,tree):
 node=next((n for n in tree.iter('node') if n.get('resource-id')==PACKAGE+':id/p06_map_container'),None)
 bounds=[] if node is None else list(map(int,re.findall(r'\d+',node.get('bounds',''))))
 if len(bounds)!=4:raise ValueError('Actual map canvas bounds missing')
 left,top,right,bottom=bounds
 colors=png_pixels(raw,[(int(left+(right-left)*x/10),int(top+(bottom-top)*y/10)) for x in (2,3,4,6,7,8) for y in (2,3,4,6,7,8)])
 if len(set(colors))<3:raise ValueError('Real-map screenshot has insufficient native feature variation')
 return {'sampleColors':len(set(colors)),'realMapFeatureVariation':True}

def capture(device,dump,screenshot,tap,start,output):
 info=json.loads(Path('dist/build-info.json').read_text());rows=[]
 def click(node):
  b=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
  if len(b)!=4 or b[2]<=b[0] or b[3]<=b[1]:raise ValueError('Real map native control invisible')
  device('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(.3)
 def text(value,description=False):
  until=time.monotonic()+15
  while time.monotonic()<until:
   for node in dump().iter('node'):
    if node.get('content-desc' if description else 'text')==value:click(node);return
   time.sleep(.3)
  raise ValueError('Real map native control missing: '+value)
 def home():
  device('shell','am','force-stop',PACKAGE);device('shell','am','start','-W','-f','0x10008000','-n',PACKAGE+'/de.dennisguse.opentracks.introduction.IntroductionActivity')
  until=time.monotonic()+20
  while time.monotonic()<until:
   if any(n.get('resource-id')==PACKAGE+':id/p0_title' for n in dump().iter('node')):return
   device('shell','input','swipe','360','400','360','1200','200');time.sleep(.3)
  raise ValueError('Real-map runtime cannot reach home')
 def picker(filename):
  tap('p06_import_map');until=time.monotonic()+25
  while time.monotonic()<until:
   nodes=list(dump().iter('node'))
   if any(n.get('text')==filename for n in nodes):text(filename);return
   drawer=next((n for n in nodes if n.get('content-desc') in ('Show roots','Open navigation drawer')),None)
   if drawer is not None:click(drawer);text('Downloads');continue
   download=next((n for n in nodes if n.get('text')=='Downloads'),None)
   if download is not None:click(download);continue
   time.sleep(.3)
  raise ValueError('System SAF did not offer the actual map')
 def ready():
  until=time.monotonic()+90
  while time.monotonic()<until:
   tree=dump()
   if any(NAME in n.get('text','') and 'local map' in n.get('text','') for n in tree.iter('node')):time.sleep(3);return
   time.sleep(.5)
  raise ValueError('Optimized APK could not import/open the actual 100MB map')
 def retained():
  directory='/data/user/0/'+PACKAGE+'/files/offline-maps/'
  index=json.loads(device('shell','cat',directory+'index.json'));file=directory+index['selected']+'.map'
  if device('shell','stat','-c','%s',file)!=str(BYTES) or device('shell','sha256sum',file).split()[0]!=SHA:raise ValueError('Optimized APK private map differs from actual source bytes')
  return file
 def preferences():
  path='/data/user/0/'+PACKAGE+'/shared_prefs/'+PACKAGE+'_preferences.xml'
  root=ET.fromstring(device('shell','cat',path));keys={'sensorGPS','minSamplingInterval'}
  # Keep exact stored values and absence as well; key names come from production resources.
  for resource in ('settings_sensor_gps_key','min_sampling_interval_key'):
   for xml in Path('src/main/res/values').glob('*.xml'):
    for node in ET.parse(xml).getroot():
     if node.get('name')==resource and node.text:keys.add(node.text)
  return {key:next((n.text if n.tag=='string' else n.get('value') for n in root if n.get('name')==key),None) for key in keys}
 def wait_tiles():
  until=time.monotonic()+60
  while time.monotonic()<until:
   text('More options',description=True);text('Map diagnostics (local)')
   end=time.monotonic()+20;diagnostic=None
   while time.monotonic()<end:
    for node in dump().iter('node'):
     value=node.get('text','')
     if '"visibleTiles"' in value and value.strip().startswith('{'):diagnostic=json.loads(value);break
    if diagnostic is not None:break
    time.sleep(.25)
   if diagnostic is None:raise ValueError('Actual signed map diagnostic dialog unavailable')
   text('OK');expected=[t for t in diagnostic['visibleTiles'] if t['readerSupportsTile']]
   if expected and all(t['rendererTileState']=='CACHED' for t in expected):time.sleep(.5);return diagnostic
   time.sleep(.5)
  raise ValueError('Signed native viewport did not complete every file-supported tile')
 def photo(name,track=False):
  diagnostic=wait_tiles();tree=dump();focus=next((l.strip() for l in device('shell','dumpsys','window').splitlines() if 'mCurrentFocus=' in l),'')
  if 'P06MapActivity' not in focus:raise ValueError('Actual signed map Activity must be focused')
  filename='p061-signed-real-'+name;screenshot(filename);raw=(output/(filename+'.png')).read_bytes();pixels=verify_pixels(raw,tree)
  if track:pixels.update(verify_track_pixels(raw,tree,32))
  xml=ET.tostring(tree,encoding='utf-8');(output/(filename+'.xml')).write_bytes(xml)
  rows.append({'file':filename+'.png','sha256':hashlib.sha256(raw).hexdigest(),'nativeWindowFile':filename+'.xml','nativeWindowSha256':hashlib.sha256(xml).hexdigest(),'focusedWindow':focus,'pixels':pixels,'viewportDiagnostic':diagnostic})
 source=Path('work/real-map')/NAME
 if source.stat().st_size!=BYTES:raise ValueError('Actual map must be supplied to signed runtime')
 device('push',source,'/sdcard/Download/'+NAME,timeout=120)
 device('shell','cmd','connectivity','airplane-mode','enable')
 if device('shell','settings','get','global','airplane_mode_on')!='1':raise ValueError('Actual airplane mode required')
 home();tap('p04_route');picker(NAME);ready();retained();photo('imported-z7')
 tap('p06_my_position')
 for _ in range(3):device('emu','geo','fix','113.2644','23.1291','12','8','4');time.sleep(1)
 for _ in range(5):tap('p06_zoom_in');time.sleep(.35)
 time.sleep(3);photo('detail-z12')
 for _ in range(4):tap('p06_zoom_in');time.sleep(.35)
 time.sleep(3);photo('detail-z16')
 old_font=device('shell','settings','get','system','font_scale')
 try:
  device('shell','wm','size','640x1136');device('shell','wm','density','320');device('shell','settings','put','system','font_scale','1.5');time.sleep(3);ready()
  nodes=list(dump().iter('node'))
  for action in ('p06_import_map','p06_manage_maps','p06_my_position','p06_fit_track'):
   node=next((n for n in nodes if n.get('resource-id')==PACKAGE+':id/'+action),None)
   if node is None or (action!='p06_fit_track' and node.get('enabled')!='true'):raise ValueError('Large-font map action unavailable: '+action)
   # Fit is actionable only when a route exists; the live-route scene exercises its enabled state.
   b=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
   if len(b)!=4 or b[3]-b[1]<96 or b[2]<=b[0]:raise ValueError('Large-font map action clipped: '+action)
  photo('320dp-large-font')
 finally:
  device('shell','wm','size','reset');device('shell','wm','density','reset')
  device('shell','settings','put','system','font_scale',old_font if old_font!='null' else '1.0');time.sleep(2)

 device('shell','rm','/sdcard/Download/'+NAME);home();tap('p04_route');ready();retained();tap('p06_my_position')
 for _ in range(3):device('emu','geo','fix','113.2644','23.1291','12','8','4');time.sleep(1)
 photo('new-process')
 home();tap('p0_start')
 for _ in range(4):device('emu','geo','fix','113.2644','23.1291','12','8','4');time.sleep(1)
 stop=threading.Event();errors=[]
 def feed():
  try:
   i=0
   while not stop.is_set():device('emu','geo','fix','113.2644',str(23.1291+min(i,40)*.000018),'12','8','4');i+=1;stop.wait(1)
  except Exception as error:errors.append(str(error))
 thread=threading.Thread(target=feed);thread.start()
 try:
  time.sleep(3);before=preferences();tap('p04_route');ready();tap('p06_my_position');time.sleep(2);tap('p06_fit_track');time.sleep(2);photo('live-route',True)
  device('shell','input','keyevent','4');time.sleep(2)
  if not any('Recording' in n.get('text','') for n in dump().iter('node')):raise ValueError('Closing real map stopped the workout')
  if preferences()!=before:raise ValueError('Real map changed GPS user preferences')
  tap('p04_route');ready();tap('p06_fit_track');time.sleep(2);picker('Gardenia-broken.map');time.sleep(2);retained()
  if not any('Cannot import or open this map' in n.get('text','') for n in dump().iter('node')):raise ValueError('Real map corrupt replacement was not rejected')
  photo('invalid-retains',True);device('shell','input','keyevent','4');tap('p0_finish');time.sleep(1)
 finally:stop.set();thread.join(timeout=10)
 if thread.is_alive() or errors:raise ValueError('Real-map GNSS fixture failed: '+str(errors))
 (output/'real-map-meminfo.txt').write_text(device('shell','dumpsys','meminfo',PACKAGE))
 archive=Path('dist/real-map-screenshots.zip')
 with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
  for row in rows:
   for key in ('file','nativeWindowFile'):z.write(output/row[key],row[key])
 proof={'passed':True,'commit':info['commit'],'apkSha256':info['apkSha256'],'mapSha256':SHA,'mapBytes':BYTES,'safSystemPicker':True,'privateFileShaVerified':True,'sourceDeleted':True,'newProcessReload':True,'airplaneMode':True,'recordingSurvivesMapClose':True,'gpsPreferencesUnchanged':True,'largeFont320dpVerified':True,'screenshots':rows,'screenshotsZipSha256':hashlib.sha256(archive.read_bytes()).hexdigest(),'hardware':'NEEDS_DEVICE_VALIDATION'}
 if len(rows)!=7:raise ValueError('Signed actual map screenshot set incomplete')
 Path('dist/real-map-smoke.json').write_text(json.dumps(proof,indent=2)+'\n')

def verify(folder,info):
 proof=json.loads((folder/'real-map-smoke.json').read_text());raw=(folder/'real-map-screenshots.zip').read_bytes()
 if proof.get('commit')!=info['commit'] or proof.get('apkSha256')!=info['apkSha256'] or proof.get('mapSha256')!=SHA or proof.get('mapBytes')!=BYTES or len(proof.get('screenshots',[]))!=7 or hashlib.sha256(raw).hexdigest()!=proof['screenshotsZipSha256'] or any(proof.get(k) is not True for k in ('passed','safSystemPicker','privateFileShaVerified','sourceDeleted','newProcessReload','airplaneMode','recordingSurvivesMapClose','gpsPreferencesUnchanged','largeFont320dpVerified')):raise ValueError('Exact signed actual-map runtime proof missing')
 import io
 with zipfile.ZipFile(io.BytesIO(raw)) as z:
  for row in proof['screenshots']:
   if not any(t['readerSupportsTile'] for t in row['viewportDiagnostic']['visibleTiles']) or any(t['readerSupportsTile'] and t['rendererTileState']!='CACHED' for t in row['viewportDiagnostic']['visibleTiles']):raise ValueError('Signed partial viewport cannot pass')
   png=z.read(row['file']);xml=z.read(row['nativeWindowFile']);tree=ET.fromstring(xml)
   if hashlib.sha256(png).hexdigest()!=row['sha256'] or hashlib.sha256(xml).hexdigest()!=row['nativeWindowSha256'] or 'P06MapActivity' not in row['focusedWindow']:raise ValueError('Actual signed map native evidence differs')
   verify_pixels(png,tree)
   if any(v in row['file'] for v in ('live-route','invalid-retains')):verify_track_pixels(png,tree,32)
 return proof
