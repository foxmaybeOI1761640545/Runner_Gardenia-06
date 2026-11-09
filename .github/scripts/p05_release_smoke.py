"""Actual fixed-key P0.4 -> P0.5 upgrade, GPS/recovery and retained local data on API35."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import sqlite3
import subprocess
import tempfile
import time
import uuid
import zipfile
import xml.etree.ElementTree as ET

PACKAGE='com.fox.runner'
FIXTURE=991001
TITLE='Synthetic retained P0 workout'



def validate_live_metrics(metric):
    # The emulator's GNSS/NMEA round-trip is not decimal-string exact. Require
    # numeric coordinates within the short controlled route and a 5m-class fix.
    position=re.search(r'([-+]?\d+\.\d+), ([-+]?\d+\.\d+) · ±(\d+\.\d+) m',metric)
    motion=re.search(r'Speed (\d+\.\d+) km/h · Pace (\d+):(\d{2}) /km',metric)
    if not position or not motion:raise ValueError('Actual Release GPS fields missing: '+metric)
    latitude,longitude,accuracy=map(float,position.groups())
    speed=float(motion[1]);minutes=int(motion[2]);seconds=int(motion[3])
    if not (1.29999<=latitude<=1.3002 and abs(longitude-103.8)<=0.0001 and 0<accuracy<=20
            and speed>0 and 0<=seconds<60 and minutes*60+seconds>0):
        raise ValueError('Actual Release GPS metrics outside the controlled route: '+metric)

def run(p02=True,p03=True,post_verify=None,baseline_spec=("ccc59b546b5087a2038d32087cc675a3096852c3",6775,"aa5ebc21c76cbca492edc66453b91d7fdecaf82bd5a69fcab208c740b4224ebc")):
    sdk=Path(os.environ['ANDROID_HOME']);adb=sdk/'platform-tools/adb';emulator=sdk/'emulator/emulator';manager=shutil.which('avdmanager')
    output=Path('build/reports/p0-release-smoke');output.mkdir(parents=True,exist_ok=True)
    info=json.loads(Path('dist/build-info.json').read_text());apk=Path('dist/RunnerGardenia-P0-Release.apk').resolve()
    if hashlib.sha256(apk.read_bytes()).hexdigest()!=info['apkSha256']:raise ValueError('Signed APK changed before installation')
    baseline=Path(os.environ['P0_BASELINE_DIR']);old=baseline/('RunnerGardenia-P0-Release.apk' if p02 else 'runner-gardenia-v0.1.0-rc1.6-6771.apk')
    oldInfo=json.loads((baseline/'build-info.json').read_text())
    baseline_commit=('4454d716a5ae010b92d3eefa1ac1c0ebedde0063' if p03 else '3116e5fa7a56ce6f299ea45d5a45745f1aed4466') if p02 else '9b5d7b06afc1eb8e3315ba73874ac088fa4d5169'
    expected_code=(6773 if p03 else 6772) if p02 else 6771
    expected_hash=('48c4a855402b7bec0b310794a228f15323ef8f09b2eb81d4f6913ab56ac6d046' if p03 else '94d3322310e8b6050504eb4c8d8c72fa09f0190f8add35ae0100b9bc93cc6ab0') if p02 else oldInfo['apkSha256']
    if baseline_spec is not None:baseline_commit,expected_code,expected_hash=baseline_spec
    if oldInfo['commit']!=baseline_commit or oldInfo['versionCode']!=expected_code or oldInfo['apkSha256']!=expected_hash or oldInfo['versionCode']>=info['versionCode'] or hashlib.sha256(old.read_bytes()).hexdigest()!=expected_hash:raise ValueError('Baseline APK differs from preserved release')
    certificate=subprocess.check_output([str(sdk/'build-tools/36.0.0/apksigner'),'verify','--print-certs',str(old)],text=True)
    fingerprints=re.findall(r'^Signer #\d+ certificate SHA-256 digest:\s*([0-9a-fA-F]+)\s*$',certificate,re.M)
    if len(fingerprints)!=1 or fingerprints[0].upper()!=info['certificateSha256']:raise ValueError('Baseline and new fixed signing certificate differ')
    temporary=Path(tempfile.mkdtemp(prefix='p0-release-',dir=os.environ['RUNNER_TEMP']));process=None;serial='emulator-5562';avd='p0-release'
    def device(*args,check=True,timeout=40):
        result=subprocess.run([str(adb),'-s',serial,*map(str,args)],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=timeout)
        if check and result.returncode:raise RuntimeError('Device command failed: '+result.stdout)
        return result.stdout.strip()
    def screenshot(name):
        with (output/(name+'.png')).open('wb') as file:subprocess.run([str(adb),'-s',serial,'exec-out','screencap','-p'],stdout=file,check=True,timeout=30)
    def dump():
        # Never accept an old XML file when Android's dump command cannot become idle.
        device('shell','rm','-f','/sdcard/p0-window.xml')
        if p03:
            status=device('shell','CLASSPATH=/data/local/tmp/gardenia-native-window.jar','app_process','/system/bin','NativeWindowDump','/sdcard/p0-window.xml',timeout=20)
            if 'FRESH_NATIVE_WINDOW /sdcard/p0-window.xml' not in status:raise ValueError('Native window snapshot was not freshly produced: '+status)
        else:
            status=device('shell','uiautomator','dump','--compressed','/sdcard/p0-window.xml',timeout=45)
        (output/'last-dump-command.txt').write_text(status+'\n')
        text=device('shell','cat','/sdcard/p0-window.xml');(output/'last-window.xml').write_text(text);return ET.fromstring(text)
    def deny_baseline_optional_permissions():
        # The preserved upstream APK requests nearby-device/notification access on
        # launch. Complete those OS dialogs before upgrading; force-stop alone
        # does not dismiss a PermissionController-owned window.
        denied=[]
        for attempt in range(4):
            root=dump()
            buttons=[node for node in root.iter('node') if node.get('resource-id')=='com.android.permissioncontroller:id/permission_deny_button']
            if not buttons:
                (output/'baseline-optional-permissions.json').write_text(json.dumps({'denied':denied},indent=2)+'\n')
                return denied
            messages=[node.get('text','') for node in root.iter('node') if node.get('resource-id')=='com.android.permissioncontroller:id/permission_message']
            message=' '.join(messages)
            if not any(word in message.lower() for word in ('nearby devices','notifications')):
                raise ValueError('Unexpected baseline permission dialog: '+message)
            bounds=list(map(int,re.findall(r'\d+',buttons[0].get('bounds',''))))
            if len(bounds)!=4:raise ValueError('Missing system permission-denial button bounds')
            device('shell','input','tap',str((bounds[0]+bounds[2])//2),str((bounds[1]+bounds[3])//2))
            denied.append(message);time.sleep(1)
        raise ValueError('Baseline optional permission dialog did not finish')
    def wait_text(text,seconds=30):
        if baseline_spec is not None and text in {"Not recording","Acquiring GPS","Recording","Paused","Finished"}:reveal("p0_recording_state")
        deadline=time.monotonic()+seconds
        while time.monotonic()<deadline:
            root=dump()
            if any(text in node.get('text','') for node in root.iter('node')):return root
            time.sleep(0.5)
        raise ValueError('Real Release UI did not display: '+text)
    def reveal(identity):
        # Refined pages intentionally scroll; discover actual native bounds without
        # assuming GPS details, actions and device entry all occupy one viewport.
        if baseline_spec is None:return
        for attempt in range(12):
            root=dump();nodes=list(root.iter('node'));screen=list(map(int,re.findall(r'\d+',root.find('node').get('bounds',''))))
            for node in nodes:
                if node.get('resource-id')==PACKAGE+':id/'+identity:
                    bounds=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
                    if len(bounds)==4 and bounds[2]>bounds[0] and bounds[3]-bounds[1]>=16:return
            if len(screen)!=4:raise ValueError('Actual viewport bounds missing')
            x=(screen[0]+screen[2])//2;h=screen[3]-screen[1]
            top=screen[1]+int(h*.25);bottom=screen[1]+int(h*.72)
            downward=(identity=='p0_metrics')==(attempt<6)
            device('shell','input','swipe',str(x),str(bottom if downward else top),str(x),str(top if downward else bottom),'200');time.sleep(.15)
        raise ValueError('Scrollable Release control/data unavailable: '+identity)
    def tap(identity):
        reveal(identity)
        for attempt in range(5):
            root=dump()
            for node in root.iter('node'):
                if node.get('resource-id')==PACKAGE+':id/'+identity and node.get('enabled')=='true':
                    bounds=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
                    screen=list(map(int,re.findall(r'\d+',root.find('node').get('bounds',''))))
                    if len(bounds)==4 and len(screen)==4:
                        # Tap the visible part above system bars, rather than rejecting
                        # a valid low button using one hard-coded center coordinate.
                        top=max(bounds[1],screen[3]*0.05);bottom=min(bounds[3],screen[3]*0.95)
                        if bounds[2]>bounds[0] and bottom-top>=16:
                            device('shell','input','tap',str((bounds[0]+bounds[2])//2),str(int((top+bottom)/2)));return
            device('shell','input','swipe','540','1800','540','650','350')
        raise ValueError('Release control unavailable: '+identity)
    def tap_text(text):
        root=dump()
        for node in root.iter('node'):
            if node.get('text')==text:
                bounds=list(map(int,re.findall(r'\d+',node.get('bounds',''))));device('shell','input','tap',str((bounds[0]+bounds[2])//2),str((bounds[1]+bounds[3])//2));return
        raise ValueError('Release row not found: '+text)
    def database(label):
        folder=temporary/label;folder.mkdir()
        device('pull','/data/user/0/'+PACKAGE+'/databases/',str(folder),timeout=60)
        matches=list(folder.rglob('database.db'))
        if len(matches)!=1:raise ValueError('Missing actual installed app database')
        return matches[0]
    def start():device('shell','am','start','-W','-n',PACKAGE+'/de.dennisguse.opentracks.introduction.IntroductionActivity')
    try:
        subprocess.run([manager,'create','avd','-n',avd,'-k','system-images;android-35;default;x86_64','-d','pixel_6','-p',str(temporary/'avd'),'-f'],input='no\n',text=True,check=True,timeout=60)
        with (output/'emulator.log').open('w') as log:
            process=subprocess.Popen([str(emulator),'-avd',avd,'-port','5562','-no-window','-no-audio','-no-snapshot','-no-boot-anim','-partition-size','2048','-gpu','swiftshader_indirect','-memory','2048','-cores','2'],stdout=log,stderr=subprocess.STDOUT)
            deadline=time.monotonic()+240
            while time.monotonic()<deadline:
                if process.poll() is not None:raise ValueError('Release validation emulator exited')
                if device('shell','getprop','sys.boot_completed',check=False,timeout=10)=='1':break
                time.sleep(2)
            else:raise TimeoutError('Release validation emulator boot timed out')
            # Root is only for synthetic fixture inspection on this disposable AOSP test device.
            print(device('root'));device('wait-for-device');device('shell','id')
            if p03:
                # Compile only a shell test utility with the SDK already installed in Actions.
                # This does not build/install another app or create/use a signing key.
                classes=temporary/'native-window-classes';classes.mkdir();dex=temporary/'native-window-dex';dex.mkdir()
                android=sdk/'platforms/android-37.0/android.jar'
                subprocess.run(['javac','--release','8','-cp',str(android),'-d',str(classes),'.github/scripts/NativeWindowDump.java'],check=True)
                subprocess.run([str(sdk/'build-tools/36.0.0/d8'),'--lib',str(android),'--min-api','35','--output',str(dex),str(classes/'NativeWindowDump.class')],check=True)
                helper=temporary/'native-window.jar'
                with zipfile.ZipFile(helper,'w') as archive:archive.write(dex/'classes.dex','classes.dex')
                device('push',helper,'/data/local/tmp/gardenia-native-window.jar')
            device('shell','cmd','alarm','set-timezone','Asia/Singapore')
            if device('shell','getprop','persist.sys.timezone')!='Asia/Singapore':raise ValueError('Wrong Release device timezone')
            device('install','-r',old,timeout=120)
            for permission in ('ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION'):device('shell','pm','grant',PACKAGE,'android.permission.'+permission)
            start();time.sleep(2);deny_baseline_optional_permissions();device('shell','content','query','--uri','content://com.fox.runner.content/tracks',check=False)
            device('shell','am','force-stop',PACKAGE);db=database('baseline-db')
            with sqlite3.connect(db) as connection:
                if connection.execute('PRAGMA user_version').fetchone()[0]!=43:raise ValueError('Unexpected baseline schema')
                stamp=1762308000000
                connection.execute('INSERT INTO tracks (_id,uuid,name,description,activity_type,activity_type_localized,time_offset,time_start,time_stop,duration_total,duration_moving,distance,speed_max) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)',(FIXTURE,uuid.uuid4().bytes,TITLE,'Synthetic isolated upgrade fixture','running','Running',28800,stamp,stamp+1000,1000,1000,2,2))
                connection.execute('INSERT INTO trackpoints(trackid,type,time,latitude,longitude,elevation,accuracy,speed,sensor_heartrate) VALUES(?,?,?,?,?,?,?,?,?)',(FIXTURE,0,stamp,1300000,103800000,12,2,2,76))
                for second in (0,1):
                    key=str(uuid.uuid4());sample={'schema':1,'source':'ble_sig_heart_rate','sampleId':key,'connectionId':'upgrade-session','capturedAt':f'2025-11-05T02:00:0{second}Z','deviceAddress':'AA:BB:CC:DD:EE:FF','deviceName':'Synthetic retained strap','contactDetected':True,'energyExpended':12,'rr1024':[1024,1000]}
                    connection.execute('INSERT INTO heart_rate_samples(trackid,time,sample_key,bpm,metadata) VALUES(?,?,?,?,?)',(FIXTURE,stamp+second*1000,key,76,json.dumps(sample)))
                connection.commit();connection.execute('PRAGMA wal_checkpoint(TRUNCATE)')
            remote='/data/user/0/'+PACKAGE+'/databases/database.db';uid=device('shell','stat','-c','%u',remote)
            device('shell','rm','-f',remote+'-wal',remote+'-shm');device('push',db,remote);device('shell','chown',uid+':'+uid,remote)
            pref='/data/user/0/'+PACKAGE+'/shared_prefs/'+PACKAGE+'_preferences.xml';local=temporary/'preferences.xml';device('pull',pref,local)
            tree=ET.parse(local);root=tree.getroot()
            for node in list(root):
                if node.get('name') in ('bluetoothSensor','p0_hr_connection_enabled','p0_hr_auto_reconnect','statsUnits','statsRate','gardenia_theme_family'):root.remove(node)
            ET.SubElement(root,'string',{'name':'bluetoothSensor'}).text='AA:BB:CC:DD:EE:FF';ET.SubElement(root,'boolean',{'name':'p0_hr_connection_enabled','value':'true'})
            ET.SubElement(root,'boolean',{'name':'p0_hr_auto_reconnect','value':'false'})
            for key,value in [('statsUnits','METRIC'),('statsRate','SPEED'),('gardenia_theme_family','precision')]:ET.SubElement(root,'string',{'name':key}).text=value
            tree.write(local,encoding='utf-8',xml_declaration=True);device('push',local,pref);device('shell','chown',uid+':'+uid,pref)
            print('Installing verified fixed-key P0 APK over baseline without clearing data',flush=True);device('install','-r',apk,timeout=120)
            upgraded_preferences=ET.fromstring(device('shell','cat',pref))
            retained_settings={n.get('name'):n.text if n.tag=='string' else n.get('value') for n in upgraded_preferences}
            expected_settings={'statsUnits':'METRIC','statsRate':'SPEED','gardenia_theme_family':'precision','bluetoothSensor':'AA:BB:CC:DD:EE:FF','p0_hr_connection_enabled':'true','p0_hr_auto_reconnect':'false'}
            if any(retained_settings.get(k)!=v for k,v in expected_settings.items()):raise ValueError('P0.4 upgrade lost existing theme/unit/rate/device preferences')
            device('shell','cmd','location','set-location-enabled','false' if p02 else 'true');start();wait_text('Not recording' if p02 else 'Ready');screenshot('release-home-gps-only')
            device('emu','geo','fix','103.8','1.3','12','8','4');tap('p0_start')
            if p02:
                wait_text('Acquiring GPS');screenshot('release-acquiring-no-workout')
                pending=database('pending-db')
                with sqlite3.connect(pending) as connection:
                    if connection.execute('SELECT COUNT(*) FROM tracks WHERE _id!=?',(FIXTURE,)).fetchone()[0]!=0:raise ValueError('GPS preparation created a workout')
                current=dump();values={n.get('resource-id'):n.get('text','') for n in current.iter('node')}
                if values.get(PACKAGE+':id/p02_time_value')!='00:00' or values.get(PACKAGE+':id/p02_distance_value')!='0.0 m':raise ValueError('GPS wait counted as activity')
                device('shell','cmd','location','set-location-enabled','true')
                for index in range(3):device('emu','geo','fix','103.8',str(1.3+index*0.000001),'12','8','4');time.sleep(1)
            wait_text('Recording')
            for index in range(8):device('emu','geo','fix','103.8',str(1.3+index*0.00002),'12','8','4');time.sleep(1)
            reveal('p0_metrics');wait_text('1.300');metrics=dump();metric=next(node.get('text','') for node in metrics.iter('node') if node.get('resource-id')==PACKAGE+':id/p0_metrics');
            fields={n.get('resource-id'):n.get('text','') for n in metrics.iter('node')}
            speed_value=fields.get(PACKAGE+':id/p02_pace_value','')
            # Use the actual native settings UI to switch the same PACE_SPEED slot.
            tap('p03_settings');wait_text('Workout and display');tap_text('Workout and display');wait_text('Preferred rate');tap_text('Preferred rate');tap_text('Pace (min/km)')
            device('shell','input','keyevent','4');device('shell','input','keyevent','4');wait_text('Recording')
            for index in range(3):device('emu','geo','fix','103.8','1.30014','12','8','4');time.sleep(1)
            reveal('p0_metrics');metrics=dump();fields={n.get('resource-id'):n.get('text','') for n in metrics.iter('node')}
            pace_value=fields.get(PACKAGE+':id/p02_pace_value','')
            metric+='\nSpeed '+speed_value+' · Pace '+pace_value
            if next((n.text for n in ET.fromstring(device('shell','cat',pref)) if n.get('name')=='statsRate'),None)!='PACE':raise ValueError('Native rate preference did not persist')
            validate_live_metrics(metric)
            screenshot('release-recording-gps-only');tap('p0_pause');wait_text('Paused');device('shell','am','force-stop',PACKAGE)
            paused=database('paused-db')
            with sqlite3.connect(paused) as connection:
                row=connection.execute('SELECT _id,distance FROM tracks WHERE _id!=? ORDER BY _id DESC LIMIT 1',(FIXTURE,)).fetchone()
                if row is None:raise ValueError('Real Release workout was not saved')
                workout,before=row
            device('emu','geo','fix','103.8','1.30064');time.sleep(3);start();wait_text('Paused');tap('p0_resume');wait_text('Recording')
            for index in range(5):device('emu','geo','fix','103.8',str(1.30064+index*0.00002),'12','8','4');time.sleep(1)
            tap('p0_finish');wait_text('Finished');tap('p0_devices');wait_text('Selected strap');screenshot('release-settings-no-bluetooth-permission')
            device('shell','input','keyevent','4');tap('p0_all_history');wait_text(TITLE);screenshot('release-retained-history')
            tap_text(TITLE);wait_text('Samples 2');screenshot('release-retained-workout-detail');tap('p0_share_gpx')
            deadline=time.monotonic()+30;exported=[]
            while time.monotonic()<deadline:
                exported=device('shell','find','/data/user/0/'+PACKAGE+'/cache/p0_exports','-name','*.gpx',check=False).splitlines()
                if len(exported)==1 and exported[0].startswith('/data/user/0/'+PACKAGE+'/cache/p0_exports/'):break
                time.sleep(0.5)
            if len(exported)!=1:raise ValueError('Actual optimized Release did not export GPX')
            gpx=output/'release-export.gpx';device('pull',exported[0],gpx)
            document=ET.parse(gpx).getroot()
            if len(document.findall('.//{http://www.topografix.com/GPX/1/1}trkpt'))!=1 or len(document.findall('.//{urn:runner-gardenia:gpx:1}sample'))!=2:raise ValueError('Optimized Release GPX lost baseline GPS/raw HR samples')
            subprocess.run(['java','.github/scripts/ValidateP0Gpx.java','.github/schemas/gpx-1.1.xsd',str(gpx)],check=True)
            device('shell','am','force-stop',PACKAGE);final=database('final-db')
            with sqlite3.connect(final) as connection:
                if connection.execute('PRAGMA user_version').fetchone()[0]!=43:raise ValueError('Schema unexpectedly changed')
                if connection.execute('SELECT name FROM tracks WHERE _id=?',(FIXTURE,)).fetchone()!=(TITLE,):raise ValueError('Baseline workout lost')
                if connection.execute('SELECT COUNT(*) FROM trackpoints WHERE trackid=?',(FIXTURE,)).fetchone()[0]!=1 or connection.execute('SELECT COUNT(*) FROM heart_rate_samples WHERE trackid=?',(FIXTURE,)).fetchone()[0]!=2:raise ValueError('Baseline GPS/HR rows lost')
                stats=connection.execute('SELECT distance,duration_total,time_stop-time_start FROM tracks WHERE _id=?',(workout,)).fetchone()
                points=connection.execute('SELECT COUNT(*) FROM trackpoints WHERE trackid=? AND latitude IS NOT NULL AND type=0',(workout,)).fetchone()[0]
                segments=connection.execute('SELECT COUNT(*) FROM trackpoints WHERE trackid=? AND type=-2',(workout,)).fetchone()[0]
                if points<2 or not 0<stats[0]<100 or stats[0]-before>=35 or segments<2 or stats[2]-stats[1]<3000:raise ValueError('Actual GPS/pause/resume acceptance failed: '+str((points,stats,before,segments)))
            device('pull',pref,temporary/'final-preferences.xml')
            preferences=ET.parse(temporary/'final-preferences.xml').getroot()
            selected=next((n.text for n in preferences if n.get('name')=='bluetoothSensor'),None)
            enabled=next((n.get('value') for n in preferences if n.get('name')=='p0_hr_connection_enabled'),None)
            if selected!='AA:BB:CC:DD:EE:FF' or enabled!='true':raise ValueError('Upgrade lost device configuration')
            permissions=device('shell','dumpsys','package',PACKAGE)
            if re.search(r'android.permission.BLUETOOTH_CONNECT: granted=true',permissions) or re.search(r'android.permission.POST_NOTIFICATIONS: granted=true',permissions):raise ValueError('GPS-only smoke accidentally granted Bluetooth/notification permission')
            if post_verify is not None:post_verify(device,dump,screenshot,tap,start,output)
            proof={'passed':True,'commit':info['commit'],'apkSha256':info['apkSha256'],'installed':True,'upgradeFromVersionCode':oldInfo['versionCode'],'deviceConfigurationRetained':True,'unitAndRateUpgradeRetained':True,'themeUpgradeRetained':True,'nativeRateSwitchKeepsRecording':True,'preparationCreatesNoWorkout':p02,'retainedGpsPoints':1,'retainedRawHeartSamples':2,'gpsPoints':points,'manualSegments':segments,'gpsDistanceMeters':stats[0],'activeMillis':stats[1],'wallMillis':stats[2],'forceStopRecovery':True,'bluetoothAndNotificationPermissionDenied':True,'timezone':'Asia/Singapore','physicalBleTested':False,'liveGpsFieldsVerified':True,'releaseGpxSchemaVerified':True,'releaseExportGpsPoints':1,'releaseExportRawSamples':2}
            (output/'result.json').write_text(json.dumps(proof,indent=2)+'\n');shutil.copyfile(output/'result.json','dist/runtime-smoke.json');info['runtimeSmokeVerified']=True;info['upgradeFromVersionCode']=oldInfo['versionCode'];Path('dist/build-info.json').write_text(json.dumps(info,indent=2)+'\n');print(json.dumps(proof,indent=2))
    finally:
        try:screenshot('last-native-screen')
        except (OSError,subprocess.SubprocessError):pass
        try:(output/'logcat.txt').write_text(device('logcat','-d','-v','threadtime',check=False,timeout=15))
        except (OSError,subprocess.TimeoutExpired):pass
        if process is not None and process.poll() is None:
            process.terminate()
            try:process.wait(timeout=15)
            except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
        subprocess.run([manager,'delete','avd','-n',avd],check=False,stdout=subprocess.DEVNULL,timeout=30);shutil.rmtree(temporary,ignore_errors=True)


if __name__=='__main__':
    from p05_release_theme_ui import capture
    run(post_verify=capture)
