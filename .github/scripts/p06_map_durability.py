"""Two successful instrumentation processes with a force-stop between them; debug-only, no secrets."""
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import time


def parse_result(output):
    if any(marker in output for marker in ('FAILURES!!!', 'INSTRUMENTATION_FAILED', 'Process crashed')):
        raise ValueError('Instrumentation failed or crashed')
    counts=re.findall(r'\bOK \((\d+) tests?\)',output)
    pids=re.findall(r'^INSTRUMENTATION_STATUS: p06MapPid=(\d+)\s*$',output,re.M)
    if counts!=['1'] or len(pids)!=1 or int(pids[0])<=0 or not re.search(r'^INSTRUMENTATION_CODE: -1\s*$',output,re.M):
        raise ValueError('Missing complete one-test instrumentation evidence/PID')
    return {'tests':1,'executed':1,'failures':0,'errors':0,'skipped':0,'pid':int(pids[0])}


def apk(metadata,expected):
    payload=json.loads(metadata.read_text())
    if payload['applicationId']!=expected or len(payload['elements'])!=1:
        raise ValueError('Unexpected debug/test APK identity or count')
    file=(metadata.parent/payload['elements'][0]['outputFile']).resolve()
    if not file.is_relative_to(metadata.parent.resolve()) or not file.is_file():
        raise ValueError('Missing APK from actual output metadata')
    return file


def run():
    report=Path('build/reports/p06-map-durability');report.mkdir(parents=True,exist_ok=True)
    sdk=Path(os.environ['ANDROID_HOME']);adb=sdk/'platform-tools/adb';emulator=sdk/'emulator/emulator'
    manager=shutil.which('avdmanager')
    if not manager or not adb.is_file() or not emulator.is_file():raise ValueError('Installed API35 toolchain is required')
    app=apk(Path('build/outputs/apk/reproducible/debug/output-metadata.json'),'com.fox.runner.debug')
    tests=apk(Path('build/outputs/apk/androidTest/reproducible/debug/output-metadata.json'),'com.fox.runner.debug.test')
    temporary=Path(tempfile.mkdtemp(prefix='p06-map-durability-',dir=os.environ['RUNNER_TEMP']))
    avd='p06-map-durability';serial='emulator-5564';process=None
    def device(*args,check=True,timeout=30):
        return subprocess.run([str(adb),'-s',serial,*args],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=timeout,check=check)
    try:
        subprocess.run([manager,'create','avd','-n',avd,'-k','system-images;android-35;default;x86_64','-d','pixel_6','-p',str(temporary/'avd'),'-f'],input='no\n',text=True,check=True,timeout=60)
        with (report/'emulator.log').open('w') as log:
            process=subprocess.Popen([str(emulator),'-avd',avd,'-port','5564','-no-window','-no-audio','-no-snapshot','-no-boot-anim','-partition-size','2048','-gpu','swiftshader_indirect','-memory','2048','-cores','2'],stdout=log,stderr=subprocess.STDOUT)
            print('Booting dedicated API35 storage validation device',flush=True)
            deadline=time.monotonic()+240
            while time.monotonic()<deadline:
                if process.poll() is not None:raise RuntimeError('Dedicated emulator exited during boot')
                try:
                    if device('shell','getprop','sys.boot_completed',check=False,timeout=10).stdout.strip()=='1':break
                except subprocess.TimeoutExpired:pass
                time.sleep(2)
            else:raise TimeoutError('Dedicated emulator boot timed out')
            device('install','-r','-t','-g',str(app),timeout=120);device('install','-r','-t','-g',str(tests),timeout=120)
            registered=device('shell','pm','list','instrumentation').stdout
            (report/'instrumentation.txt').write_text(registered)
            component='com.fox.runner.debug.test/de.dennisguse.opentracks.p0.P0DurabilityRunner'
            if component not in registered:raise ValueError('Dedicated storage test runner is not registered in installed test APK')
            device('shell','cmd','connectivity','airplane-mode','enable')
            if device('shell','settings','get','global','airplane_mode_on').stdout.strip()!='1':raise ValueError('Actual airplane mode required')
            phases={}
            for phase in ('write','read'):
                result=device('shell','am','instrument','-w','-r','-e','class','de.dennisguse.opentracks.p0.map.P06MapPersistenceTest','-e','p06MapPhase',phase,'-e','timezone','Asia/Singapore','-e','buildCommit',os.environ['GITHUB_SHA'],component,check=False,timeout=120)
                (report/(phase+'.txt')).write_text(result.stdout)
                if result.returncode:raise RuntimeError('Instrumentation '+phase+' failed: '+result.stdout)
                phases[phase]=parse_result(result.stdout)
                print('Completed file-backed instrumentation phase: '+phase,flush=True)
                if phase=='write':
                    device('shell','am','force-stop','com.fox.runner.debug')
                    if device('shell','pidof','com.fox.runner.debug',check=False).stdout.strip():raise ValueError('Target process survived force-stop')
            if phases['write']['pid']==phases['read']['pid']:raise ValueError('Read phase did not run in a new process')
            timezone=device('shell','getprop','persist.sys.timezone').stdout.strip()
            if timezone!='Asia/Singapore':raise ValueError('Device default timezone was not Singapore')
            device('pull','/sdcard/Android/media/com.fox.runner.debug/additional_test_output/p0-ui/p06-new-process.png',str(report/'new-process.png'))
            import hashlib
            png=report/'new-process.png'
            if not png.read_bytes().startswith(b'\x89PNG\r\n\x1a\n'):raise ValueError('Actual restarted map screenshot missing')
            proof={'passed':True,'commit':os.environ['GITHUB_SHA'],'privateMapRetained':True,'nativeTilesRenderedAfterRestart':True,'airplaneMode':True,'forceStopped':True,'timezone':timezone,'phases':phases,'screenshotSha256':hashlib.sha256(png.read_bytes()).hexdigest()}
            (report/'result.json').write_text(json.dumps(proof,indent=2)+'\n');print(json.dumps(proof,indent=2))
    finally:
        try:(report/'logcat.txt').write_text(device('logcat','-d','-v','threadtime',check=False,timeout=15).stdout)
        except (OSError,subprocess.TimeoutExpired):pass
        if process is not None and process.poll() is None:
            process.terminate()
            try:process.wait(timeout=15)
            except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
        subprocess.run([manager,'delete','avd','-n',avd],check=False,stdout=subprocess.DEVNULL,timeout=30)
        shutil.rmtree(temporary,ignore_errors=True)


if __name__=='__main__':run()
