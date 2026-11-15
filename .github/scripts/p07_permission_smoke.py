"""Actual denied and granted GPS permissions in distinct API35 processes; no signing keys."""
import hashlib,json,os,re,shutil,subprocess,tempfile,time
from pathlib import Path
from p0_durability import apk

def run():
 out=Path('build/reports/p07-permissions');out.mkdir(parents=True,exist_ok=True)
 sdk=Path(os.environ['ANDROID_HOME']);work=Path(tempfile.mkdtemp(prefix='p07-permissions-',dir=os.environ['RUNNER_TEMP']))
 name='gardenia-p07-permissions';serial='emulator-5562';process=None;phases={}
 def device(*args,check=True,timeout=30):
  p=subprocess.run([str(sdk/'platform-tools/adb'),'-s',serial,*map(str,args)],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=timeout)
  if check and p.returncode:raise RuntimeError(p.stdout)
  return p.stdout.strip()
 try:
  app=apk(Path('build/outputs/apk/reproducible/debug/output-metadata.json'),'com.fox.runner.debug')
  tests=apk(Path('build/outputs/apk/androidTest/reproducible/debug/output-metadata.json'),'com.fox.runner.debug.test')
  subprocess.run(['avdmanager','create','avd','-n',name,'-k','system-images;android-35;default;x86_64','-d','pixel_6','-p',str(work/'avd'),'-f'],input='no\n',text=True,check=True,timeout=60)
  with (out/'emulator.log').open('w') as log:
   process=subprocess.Popen([str(sdk/'emulator/emulator'),'-avd',name,'-port','5562','-no-window','-no-audio','-no-snapshot','-no-boot-anim','-gpu','swiftshader_indirect','-memory','2048','-cores','2'],stdout=log,stderr=subprocess.STDOUT)
   deadline=time.monotonic()+240
   while time.monotonic()<deadline:
    if process.poll() is not None:raise ValueError('Permission emulator exited')
    if device('shell','getprop','sys.boot_completed',check=False,timeout=10)=='1':break
    time.sleep(2)
   else:raise TimeoutError('Permission emulator boot')
   device('shell','input','keyevent','224');device('shell','wm','dismiss-keyguard')
   device('install','-r','-t',app,timeout=120);device('install','-r','-t',tests,timeout=120)
   component='com.fox.runner.debug.test/de.dennisguse.opentracks.TestRunner'
   if component not in device('shell','pm','list','instrumentation'):raise ValueError('Actual normal test runner missing')
   for phase in ('denied','granted'):
    if phase=='granted':
     for permission in ('ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION'):device('shell','pm','grant','com.fox.runner.debug','android.permission.'+permission)
    raw=device('shell','am','instrument','-w','-r','-e','class','de.dennisguse.opentracks.p0.map.P07PermissionStateTest','-e','expectedGpsPermission',phase,'-e','timezone','Asia/Singapore','-e','buildCommit',os.environ['GITHUB_SHA'],component,check=False,timeout=120)
    (out/(phase+'.txt')).write_text(raw+'\n')
    if any(s in raw for s in ('FAILURES!!!','INSTRUMENTATION_FAILED','Process crashed')) or re.findall(r'\bOK \((\d+) tests?\)',raw)!=['1'] or not re.search(r'^INSTRUMENTATION_CODE: -1\s*$',raw,re.M):raise ValueError('Permission instrumentation failed/incomplete: '+phase)
    if re.findall(r'^INSTRUMENTATION_STATUS: p07ActualGpsPermission=(.+)$',raw,re.M)!=[phase]:raise ValueError('Wrong actual OS permission state')
    pids=re.findall(r'^INSTRUMENTATION_STATUS: p07PermissionPid=(\d+)\s*$',raw,re.M)
    if len(pids)!=1:raise ValueError('Actual target PID missing')
    device('pull','/sdcard/Android/media/com.fox.runner.debug/additional_test_output/p0-ui/p07-permission-'+phase+'.png',out/(phase+'.png'))
    data=(out/(phase+'.png')).read_bytes()
    if not data.startswith(b'\x89PNG\r\n\x1a\n'):raise ValueError('Actual permission screenshot missing')
    phases[phase]={'tests':1,'executed':1,'failures':0,'errors':0,'skipped':0,'pid':int(pids[0]),'actualGpsPermission':phase,'screenshotSha256':hashlib.sha256(data).hexdigest()}
    device('shell','am','force-stop','com.fox.runner.debug')
    if device('shell','pidof','com.fox.runner.debug',check=False):raise ValueError('Target survived force-stop')
   if phases['denied']['pid']==phases['granted']['pid']:raise ValueError('Permission probe did not use new processes')
   proof={'passed':True,'source':os.environ['GITHUB_SHA'],'api':int(device('shell','getprop','ro.build.version.sdk')),'phases':phases,'visualReviewRequired':True}
   if proof['api']!=35:raise ValueError('Wrong actual API')
   (out/'result.json').write_text(json.dumps(proof,indent=2)+'\n')
 finally:
  try:(out/'logcat.txt').write_text(device('logcat','-d','-v','threadtime',check=False,timeout=15))
  except (OSError,subprocess.SubprocessError):pass
  if process is not None and process.poll() is None:
   process.terminate()
   try:process.wait(timeout=15)
   except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
  subprocess.run(['avdmanager','delete','avd','-n',name],check=False,stdout=subprocess.DEVNULL,timeout=30)
  shutil.rmtree(work,ignore_errors=True)

if __name__=='__main__':run()
