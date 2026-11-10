"""Revalidate the immutable c509 signed APK; never build, sign, change app/tests or reuse failed evidence."""
import hashlib,json,os,shutil,subprocess,sys
from pathlib import Path
sys.path.insert(0,str(Path.cwd()/'.github/scripts'))
import p05_release as route
import p05_signing as signing
from publish_release import GitHub,verify_apk
SOURCE='c5093ed3ac9514b6655afb5ed022024d8776f6cb'
SIGNING_COMMIT='7f586e6848f43922e6e26a1dc8bcb30c8e4f89e9'
SIGNING_RUN='42754884300'
CANDIDATE=13734494648
DIGEST='ed55a93d986c7c54a0271427293e4b3859c5c48893f8cc0b6f88eef8dc57e7ce'
APK_SHA='9603a836bdc51b15fb3e5390709e30a46c3496e6d4560e594426554ffe8af042'
MARKER='[p05-recover-accepted-apk]'
ALLOWED={'.github/scripts/p05_runtime_recovery.py','.github/scripts/p05_release_runtime_smoke.py','.github/workflows/p05-signed-runtime-recovery.yml','.github/tests/test_p05_runtime_recovery.py'}
TEMP=Path(os.environ.get('RUNNER_TEMP','work'))/'p05-runtime-recovery'
def delta(paths):
 if not paths or any(p not in ALLOWED and not p.startswith('docs/') for p in paths):raise ValueError('Recovery cannot change original app, tests, version, signing or publishing')
def git(*args):return subprocess.check_output(['git',*args],text=True).strip()
def context():
 route.trusted();route.protected_refs()
 if git('rev-parse','HEAD')!=os.environ['GITHUB_SHA']:raise ValueError('Wrong recovery checkout')
 message=json.loads(Path(os.environ['GITHUB_EVENT_PATH']).read_text())['head_commit']['message']
 if MARKER not in message:raise ValueError('Explicit immutable APK recovery required')
 delta(git('diff','--name-only',SIGNING_COMMIT,'HEAD').splitlines())
 if git('rev-parse','HEAD:src/main')!=git('rev-parse',SOURCE+':src/main'):raise ValueError('App tree changed')
 api=GitHub();a=route.approval_record();route.accepted_jobs(api,a)
 if a['source']!=SOURCE:raise ValueError('Recovery source differs')
 run=api.request(api.base+'/actions/runs/'+SIGNING_RUN)
 if run['head_sha']!=SIGNING_COMMIT or run['conclusion']!='failure':raise ValueError('Original failed runtime run identity differs')
 jobs=api.request(api.base+'/actions/runs/'+SIGNING_RUN+'/jobs?per_page=100')['jobs'];job=next(j for j in jobs if j['name']=='signed')
 failed=[s['name'] for s in job['steps'] if s['conclusion']=='failure']
 if failed!=['API35 signed install, real GPS inputs, recording recovery, upgrade and eight native schemes']:raise ValueError('Original signing/build failed; cannot recover these bytes')
 if not any(s['name']=='Build reviewed source with original fixed key, shrink, align and verify' and s['conclusion']=='success' for s in job['steps']):raise ValueError('No original successful fixed-key build')
 route.artifact(api,CANDIDATE,'RunnerGardenia-P05-Candidate-'+SIGNING_RUN,SIGNING_RUN,SIGNING_COMMIT,DIGEST)
 TEMP.mkdir(exist_ok=True)
 for name in ('p05_runtime_recovery.py','p05_release_runtime_smoke.py'):shutil.copyfile(Path('.github/scripts')/name,TEMP/name)
 helper=json.loads(Path('build/reports/p05-validation/helpers.json').read_text())
 if helper['sourceCommit']!=os.environ['GITHUB_SHA'] or not helper['passed'] or helper['executed']<=0 or any(helper[k] for k in ('failures','errors','skipped')):raise ValueError('Actual recovery helper tests failed')
 (TEMP/'context.json').write_text(json.dumps({'mode':'recovery','workflowCommit':os.environ['GITHUB_SHA'],'approval':a,'helpers':helper,'harnessSha256':hashlib.sha256((TEMP/'p05_release_runtime_smoke.py').read_bytes()).hexdigest()},indent=2)+'\n')
def load():
 route.trusted();route.protected_refs();c=json.loads((TEMP/'context.json').read_text());api=GitHub()
 if c['workflowCommit']!=os.environ['GITHUB_SHA'] or git('rev-parse','HEAD')!=SOURCE:raise ValueError('Wrong exact-source recovery identity')
 delta(git('diff','--name-only',SIGNING_COMMIT,c['workflowCommit']).splitlines())
 if git('rev-parse','HEAD:src/main')!=c['approval']['mainTree']:raise ValueError('App tree changed')
 route.accepted_jobs(api,c['approval'])
 if hashlib.sha256((TEMP/'p05_release_runtime_smoke.py').read_bytes()).hexdigest()!=c['harnessSha256']:raise ValueError('Runtime harness changed after trusted context')
 return c,api
signing.load=load
# Existing gate validates actual c509 full JUnit/PNG/GPX/offline/process/cross-UID evidence.
def inputs():
 signing.inputs();c,api=load();signing.unpack(api,CANDIDATE,'RunnerGardenia-P05-Candidate-'+SIGNING_RUN,SIGNING_RUN,SIGNING_COMMIT,DIGEST,'dist')
 info=json.loads(Path('dist/build-info.json').read_text())
 if info['commit']!=SOURCE or info['buildRunId']!=SIGNING_RUN or info['buildWorkflowCommit']!=SIGNING_COMMIT or info['apkSha256']!=APK_SHA or info['versionCode']!=6776 or not info['signatureVerified'] or not info['alignmentVerified']:raise ValueError('Original fixed-key candidate identity differs')
 raw=Path('dist/RunnerGardenia-P0-Release.apk').read_bytes();verify_apk(raw,info)
 info.update(originalSigningRunId=SIGNING_RUN,originalSigningWorkflowCommit=SIGNING_COMMIT,buildRunId=os.environ['GITHUB_RUN_ID'],buildWorkflowCommit=c['workflowCommit'],runtimeRecovery=True)
 Path('dist/build-info.json').write_text(json.dumps(info,indent=2)+'\n')
 Path('dist/runtime-recovery.json').write_text(json.dumps({'source':SOURCE,'apkSha256':APK_SHA,'originalSigningRunId':SIGNING_RUN,'originalSigningWorkflowCommit':SIGNING_COMMIT,'candidateArtifactId':CANDIDATE,'candidateArtifactSha256':DIGEST,'recoveryRunId':os.environ['GITHUB_RUN_ID'],'recoveryWorkflowCommit':c['workflowCommit'],'harnessSha256':c['harnessSha256'],'helpers':c['helpers'],'rebuilt':False,'signedAgain':False},indent=2)+'\n')
def smoke():
 load()
 import importlib.util
 spec=importlib.util.spec_from_file_location('recovered_smoke',TEMP/'p05_release_runtime_smoke.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
 from p05_release_theme_ui import capture
 m.run(post_verify=capture)
def finalize():
 signing.finalize();signing.bundle()
 if hashlib.sha256(Path('dist/RunnerGardenia-P0-Release.apk').read_bytes()).hexdigest()!=APK_SHA:raise ValueError('Recovery changed APK bytes')
if __name__=='__main__':{'context':context,'inputs':inputs,'smoke':smoke,'finalize':finalize}[sys.argv[1]]()
