"""Phase admission requires immutable successful predecessor artifacts, then reruns its historical raw gate."""
import hashlib,io,json,os,subprocess,tarfile,tempfile,zipfile
from pathlib import Path,PurePosixPath
from publish_release import GitHub
from p08_version import BRANCH

def identity(receipt,phase,run,artifact):
 if receipt.get('phase')!=phase or receipt.get('stageAccepted') is not True or receipt.get('rawJUnitVerified') is not True:raise ValueError('Predecessor report not accepted')
 source=receipt['source']
 if (run.get('id'),run.get('head_sha'),run.get('head_branch'),run.get('event'),run.get('path'),run.get('status'),run.get('conclusion'))!=(receipt['runId'],source,BRANCH,'push','.github/workflows/p08-stage.yml','completed','success'):raise ValueError('Predecessor exact-source workflow did not pass')
 if (artifact.get('id'),artifact.get('name'),artifact.get('size_in_bytes'),artifact.get('digest'),artifact.get('expired'))!=(receipt['artifactId'],receipt['artifactName'],receipt['artifactBytes'],'sha256:'+receipt['artifactSha256'],False):raise ValueError('Predecessor immutable artifact identity mismatch')
 if artifact.get('workflow_run',{}).get('head_sha')!=source or artifact.get('workflow_run',{}).get('id')!=receipt['runId']:raise ValueError('Artifact belongs to another source/run')

def safe_path(name):
 p=PurePosixPath(name)
 if p.is_absolute() or '..' in p.parts or '\\' in name:raise ValueError('Unsafe evidence archive path')
 return p

def verify(phase):
 if phase=='A':return None
 prior=chr(ord(phase)-1);path=Path('docs/p0-8-0/evidence/phase-'+prior+'/acceptance.json');receipt=json.loads(path.read_text())
 api=GitHub();run=api.request(api.base+'/actions/runs/'+str(receipt['runId']));artifact=api.request(api.base+'/actions/artifacts/'+str(receipt['artifactId']));identity(receipt,prior,run,artifact)
 subprocess.run(['git','merge-base','--is-ancestor',receipt['source'],'HEAD'],check=True)
 for name in subprocess.check_output(['git','ls-tree','-r','--name-only',receipt['source'],'--','src/test','src/androidTest','src/androidTestReproducible','.github/tests'],text=True).splitlines():
  if Path(name).read_bytes()!=subprocess.check_output(['git','show',receipt['source']+':'+name]):raise ValueError('Accepted predecessor test was changed: '+name)
 data=api.request(api.base+'/actions/artifacts/'+str(receipt['artifactId'])+'/zip',binary=True)
 if len(data)!=receipt['artifactBytes'] or hashlib.sha256(data).hexdigest()!=receipt['artifactSha256']:raise ValueError('Predecessor downloaded bytes differ')
 with tempfile.TemporaryDirectory(prefix='p08-prior-') as temp:
  root=Path(temp)
  with zipfile.ZipFile(io.BytesIO(data)) as archive:
   if sum(x.file_size for x in archive.infolist())>100_000_000:raise ValueError('Evidence archive too large')
   for item in archive.infolist():
    safe_path(item.filename)
    if (item.external_attr>>16)&0o170000==0o120000:raise ValueError('Evidence archive symlink rejected')
   archive.extractall(root)
  source=subprocess.check_output(['git','archive',receipt['source'],'--','src/test','src/androidTest','.github/scripts','.github/p08'])
  with tarfile.open(fileobj=io.BytesIO(source)) as archive:
   for item in archive.getmembers():
    safe_path(item.name)
    if not (item.isfile() or item.isdir()):raise ValueError('Source archive symlink rejected')
   archive.extractall(root,filter='data')
  env={**os.environ,'GITHUB_SHA':receipt['source'],'GITHUB_RUN_ID':str(receipt['runId'])}
  result=subprocess.run(['python3','.github/scripts/p08_stage.py','gate'],cwd=root,env=env,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=60)
  if result.returncode:raise ValueError('Historical predecessor raw evidence gate failed: '+result.stdout[-4000:])
  raw=json.loads((root/'build/reports/p08-stage/gate.json').read_text())
  if not raw['passed'] or raw['plan']['phase']!=prior or raw['plan']['commit']!=receipt['source']:raise ValueError('Historical predecessor gate source/phase differs')
 return {'phase':prior,'source':receipt['source'],'runId':receipt['runId'],'artifactId':receipt['artifactId'],'artifactSha256':receipt['artifactSha256'],'historicalRawGateRerun':True}
