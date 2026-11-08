"""Exercise the real shell window helper on API35 before signed acceptance."""
import hashlib,json,os,shutil,subprocess,tempfile,time,zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

sdk=Path(os.environ['ANDROID_HOME']);out=Path('build/reports/native-window-helper');out.mkdir(parents=True,exist_ok=True)
work=Path(tempfile.mkdtemp(prefix='native-window-',dir=os.environ['RUNNER_TEMP']))
serial='emulator-5564';name='gardenia-window-selftest';process=None
def device(*args,check=True,timeout=30):
    p=subprocess.run([str(sdk/'platform-tools/adb'),'-s',serial,*map(str,args)],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=timeout)
    if check and p.returncode:raise RuntimeError(p.stdout)
    return p.stdout.strip()
try:
    classes=work/'classes';classes.mkdir();dex=work/'dex';dex.mkdir();android=sdk/'platforms/android-37.0/android.jar'
    subprocess.run(['javac','--release','8','-cp',str(android),'-d',str(classes),'.github/scripts/NativeWindowDump.java'],check=True)
    subprocess.run([str(sdk/'build-tools/36.0.0/d8'),'--lib',str(android),'--min-api','35','--output',str(dex),str(classes/'NativeWindowDump.class')],check=True)
    jar=work/'helper.jar'
    with zipfile.ZipFile(jar,'w') as z:z.write(dex/'classes.dex','classes.dex')
    subprocess.run(['avdmanager','create','avd','-n',name,'-k','system-images;android-35;default;x86_64','-d','pixel_6','-p',str(work/'avd'),'-f'],input='no\n',text=True,check=True,timeout=60)
    with (out/'emulator.log').open('w') as log:
        process=subprocess.Popen([str(sdk/'emulator/emulator'),'-avd',name,'-port','5564','-no-window','-no-audio','-no-snapshot','-no-boot-anim','-gpu','swiftshader_indirect','-memory','2048','-cores','2'],stdout=log,stderr=subprocess.STDOUT)
        until=time.monotonic()+240
        while time.monotonic()<until:
            if process.poll() is not None:raise ValueError('Helper test emulator exited')
            if device('shell','getprop','sys.boot_completed',check=False,timeout=10)=='1':break
            time.sleep(2)
        else:raise TimeoutError('Helper test emulator boot')
        device('root');device('wait-for-device');device('push',jar,'/data/local/tmp/gardenia-native-window.jar')
        device('shell','input','keyevent','224');device('shell','wm','dismiss-keyguard')
        device('shell','input','keyevent','3');time.sleep(2)
        # Two separate shell processes must each produce a new, nonempty actual tree.
        for i in range(2):
            path=f'/sdcard/gardenia-window-{i}.xml';device('shell','rm','-f',path)
            status=device('shell','CLASSPATH=/data/local/tmp/gardenia-native-window.jar','app_process','/system/bin','NativeWindowDump',path,timeout=20)
            if 'FRESH_NATIVE_WINDOW '+path not in status:raise ValueError('Fresh marker missing')
            raw=device('shell','cat',path);root=ET.fromstring(raw)
            nodes=list(root.iter('node'))
            if len(nodes)<3 or not any(n.get('package')=='com.android.launcher3' for n in nodes):raise ValueError('Actual visible Launcher tree missing')
            if not any(n.get('resource-id') for n in nodes):raise ValueError('Native view IDs missing')
            (out/f'window-{i}.xml').write_text(raw)
        proof={'passed':True,'commit':os.environ['GITHUB_SHA'],'api':int(device('shell','getprop','ro.build.version.sdk')),'freshProcesses':2,'helperSha256':hashlib.sha256(Path('.github/scripts/NativeWindowDump.java').read_bytes()).hexdigest(),'appInstalled':False,'signingKeyUsed':False}
        if proof['api']!=35:raise ValueError('Helper must execute on API35')
        (out/'result.json').write_text(json.dumps(proof,indent=2)+'\n');print('PASS: fresh real native window snapshots in two API35 shell processes')
finally:
    try:(out/'logcat.txt').write_text(device('logcat','-d','-v','threadtime',check=False,timeout=15))
    except (OSError,subprocess.SubprocessError):pass
    if process is not None and process.poll() is None:
        process.terminate()
        try:process.wait(timeout=15)
        except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
    subprocess.run(['avdmanager','delete','avd','-n',name],check=False,stdout=subprocess.DEVNULL,timeout=30)
    shutil.rmtree(work,ignore_errors=True)
