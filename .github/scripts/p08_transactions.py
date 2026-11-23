"""Actual API35 process death inside route transactions. Interrupted producers are NOT passed tests."""
import json,os,re,shutil,subprocess,tempfile,time
from pathlib import Path
from p0_durability import apk
CASES={'addPrepared':'PREPARED','addPromoted':'PROMOTED_0','addCommitted':'INDEX_COMMITTED','addGarbage':'JOURNAL_RETIRED','deleteRetired':'RETIRED_0','deleteCommitted':'INDEX_COMMITTED','renamePrepared':'PREPARED','renameCommitted':'INDEX_COMMITTED'}
def validate_case(proof,name,checkpoint,source):
 if proof.get('source')!=source or proof.get('case')!=name or proof.get('checkpoint')!=checkpoint or proof.get('api')!=35 or proof.get('passed') is not True or proof.get('persistentDirectory')!='files/route-library':raise ValueError('Wrong or stale recovery evidence')
 if not isinstance(proof.get('producerPid'),int) or not isinstance(proof.get('recoveryPid'),int) or min(proof['producerPid'],proof['recoveryPid'])<=0 or proof['producerPid']==proof['recoveryPid']:raise ValueError('Missing distinct real process IDs')
 committed=name.endswith('Committed') or name=='addGarbage';numbers=[1,2,3] if name.startswith('add') and committed else [1,2] if name.startswith('delete') and not committed else [1]
 import uuid
 if proof.get('committed')!=committed or proof.get('ids')!=[str(uuid.UUID(int=(8<<64)+n)) for n in numbers] or not re.fullmatch('[0-9a-f]{64}',proof.get('oldRouteSha256','')):raise ValueError('Partial batch or invalid retained geometry proof')
def run():
 report=Path('build/reports/p08-transactions');report.mkdir(parents=True,exist_ok=True)
 sdk=Path(os.environ['ANDROID_HOME']);adb=sdk/'platform-tools/adb';emulator=sdk/'emulator/emulator';manager=shutil.which('avdmanager')
 if not manager or not adb.is_file() or not emulator.is_file():raise ValueError('Installed API35 toolchain required')
 app=apk(Path('build/outputs/apk/reproducible/debug/output-metadata.json'),'com.fox.runner.debug');tests=apk(Path('build/outputs/apk/androidTest/reproducible/debug/output-metadata.json'),'com.fox.runner.debug.test')
 temporary=Path(tempfile.mkdtemp(prefix='p08-transactions-',dir=os.environ['RUNNER_TEMP']));avd='p08-transactions';serial='emulator-5568';process=None;producer=None
 def device(*args,check=True,timeout=30):return subprocess.run([str(adb),'-s',serial,*args],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=timeout,check=check)
 def private(name):return device('shell','run-as','com.fox.runner.debug','cat','files/p08-proof-'+name+'.json',check=False,timeout=10)
 try:
  subprocess.run([manager,'create','avd','-n',avd,'-k','system-images;android-35;default;x86_64','-d','pixel_6','-p',str(temporary/'avd'),'-f'],input='no\n',text=True,check=True,timeout=60)
  with (report/'emulator.log').open('w') as log:
   process=subprocess.Popen([str(emulator),'-avd',avd,'-port','5568','-no-window','-no-audio','-no-snapshot','-no-boot-anim','-partition-size','2048','-gpu','swiftshader_indirect','-memory','2048','-cores','2'],stdout=log,stderr=subprocess.STDOUT)
   print('Booting dedicated API35 transaction recovery device',flush=True);deadline=time.monotonic()+240
   while time.monotonic()<deadline:
    if process.poll() is not None:raise RuntimeError('Emulator exited during boot')
    try:
     if device('shell','getprop','sys.boot_completed',check=False,timeout=10).stdout.strip()=='1':break
    except subprocess.TimeoutExpired:pass
    time.sleep(2)
   else:raise TimeoutError('Emulator boot timed out')
   device('install','-r','-t','-g',str(app),timeout=120);device('install','-r','-t','-g',str(tests),timeout=120)
   component='com.fox.runner.debug.test/de.dennisguse.opentracks.p0.routes.P08RouteTransactionRunner';registered=device('shell','pm','list','instrumentation').stdout;(report/'instrumentation.txt').write_text(registered)
   if component not in registered:raise ValueError('Dedicated transaction runner not registered')
   source=os.environ['GITHUB_SHA'];cases={}
   for name,checkpoint in CASES.items():
    common=['shell','am','instrument','-w','-r','-e','timezone','Asia/Singapore','-e','buildCommit',source,'-e','case',name]
    with (report/(name+'-producer.txt')).open('w') as producerlog:
     producer=subprocess.Popen([str(adb),'-s',serial,*common,'-e','checkpoint',checkpoint,'-e','mode','producer',component],stdout=producerlog,stderr=subprocess.STDOUT,text=True)
     deadline=time.monotonic()+45;ready=None
     while time.monotonic()<deadline:
      if producer.poll() is not None:raise ValueError('Producer exited before kill checkpoint; inspect '+name+'-producer.txt')
      raw=private('ready')
      if raw.returncode==0:
       candidate=json.loads(raw.stdout)
       if candidate.get('source')==source and candidate.get('case')==name and candidate.get('checkpoint')==checkpoint:ready=candidate;break
      time.sleep(.2)
     if ready is None:raise TimeoutError('Producer did not reach actual transaction checkpoint')
     pid=device('shell','pidof','com.fox.runner.debug').stdout.strip()
     if pid!=str(ready['producerPid']):raise ValueError('Rendezvous PID is not target process')
     device('shell','am','force-stop','com.fox.runner.debug')
     if device('shell','pidof','com.fox.runner.debug',check=False).stdout.strip():raise ValueError('Producer survived force-stop')
     producer.wait(timeout=30);producer=None
    (report/(name+'-ready.json')).write_text(json.dumps(ready,indent=2)+'\n')
    recovery=device(*common,'-e','mode','verify',component,check=False,timeout=90);(report/(name+'-verify.txt')).write_text(recovery.stdout)
    if recovery.returncode or not re.search(r'^INSTRUMENTATION_CODE: -1\s*$',recovery.stdout,re.M) or not re.search(r'^INSTRUMENTATION_RESULT: p08Verified=true\s*$',recovery.stdout,re.M):raise ValueError('Recovery verification failed: '+recovery.stdout)
    raw=private('result')
    if raw.returncode:raise ValueError('Recovery result file missing')
    proof=json.loads(raw.stdout);validate_case(proof,name,checkpoint,source)
    if proof['producerPid']!=ready['producerPid'] or proof['oldRouteSha256']!=ready['oldRouteSha256']:raise ValueError('Verifier does not correspond to killed producer')
    proof['forceStopped']=True;proof['producerOutcome']='INTENTIONALLY_INTERRUPTED_NOT_A_PASSED_JUNIT_TEST';cases[name]=proof
    device('shell','am','force-stop','com.fox.runner.debug');print('Recovered actual terminated transaction: '+name,flush=True)
   timezone=device('shell','getprop','persist.sys.timezone').stdout.strip()
   if timezone!='Asia/Singapore':raise ValueError('Wrong device timezone')
   result={'passed':True,'source':source,'api':35,'timezone':timezone,'cases':cases,'scope':'Eight externally force-stopped transactions in real private persistent route files. Separate from JUnit counts; no workout database cross-process claim.'}
   (report/'result.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
 finally:
  try:(report/'logcat.txt').write_text(device('logcat','-d','-v','threadtime',check=False,timeout=15).stdout)
  except (OSError,subprocess.TimeoutExpired):pass
  if producer is not None and producer.poll() is None:producer.terminate();producer.wait(timeout=10)
  if process is not None and process.poll() is None:
   process.terminate()
   try:process.wait(timeout=15)
   except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
  subprocess.run([manager,'delete','avd','-n',avd],check=False,stdout=subprocess.DEVNULL,timeout=30);shutil.rmtree(temporary,ignore_errors=True)
if __name__=='__main__':run()
