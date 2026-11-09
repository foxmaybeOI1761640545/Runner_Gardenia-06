"""Exact-source P0.5 test/review/sign/publish routing; approval commits cannot change code."""
import hashlib,json,os,subprocess,sys
from pathlib import Path
import p05_pipeline as validation
from publish_release import GitHub
from p05_version import VERSION,TAG
BRANCH=validation.BRANCH
REPO=validation.REPO
MARKER='[p05-release-accepted-ui]'
PUBLICATION_MARKER='[p05-publish-accepted-apk]'
APPROVAL=Path('.github/p05/visual-approval.json')
PUBLICATION=Path('.github/p05/publication-approval.json')
SIGNED_PNGS=80

def git(*args):return subprocess.check_output(['git',*args],text=True).strip()
def trusted():validation.trusted()
def protected_refs():return validation.protected_refs()
def activation_paths(paths):
    if not paths or any(p!=str(APPROVAL) and not p.startswith('docs/') for p in paths):raise ValueError('Signing activation may only add visual approval and reports')
def publication_paths(paths):
    if not paths or any(p!=str(PUBLICATION) and not p.startswith('docs/') for p in paths):raise ValueError('Publication cannot change accepted app/test/version/build/signing code')
def approval_record():
    a=json.loads(APPROVAL.read_text())
    required={'passed':True,'kind':'ACTUAL_NATIVE_VISUAL_REVIEW','schemes':8,'layoutPngs':48,'statePngs':192,'metricPngs':8,'settingsPngs':16,'historyPngs':32,'fullRegressionPassed':True,'rawJUnitVerified':True}
    if any(a.get(k)!=v for k,v in required.items()):raise ValueError('Complete actual-source visual/full-test approval required')
    return a

def accepted_jobs(api,a):
    run=api.request(api.base+'/actions/runs/'+a['testRunId'])
    if run['head_sha']!=a['source'] or run['head_branch']!=BRANCH or run['event']!='push' or run['conclusion']!='success':raise ValueError('Approved exact-source full test run did not succeed')
    jobs=api.request(api.base+'/actions/runs/'+a['testRunId']+'/jobs?per_page=100&filter=all')['jobs']
    for name in ('checks','native-window-helper'):
        if not any(j['name']==name and j['conclusion']=='success' for j in jobs):raise ValueError('Missing complete original/native gate: '+name)
    return jobs

def accepted_build(api,p):
    run=api.request(api.base+'/actions/runs/'+p['buildRunId'])
    if run['head_sha']!=p['buildWorkflowCommit'] or run['head_branch']!=BRANCH or run['event']!='push' or run['conclusion']!='success':raise ValueError('Accepted fixed-key build run did not succeed')
    jobs=api.request(api.base+'/actions/runs/'+p['buildRunId']+'/jobs?per_page=100&filter=all')['jobs']
    if not any(j['name']=='signed' and j['conclusion']=='success' for j in jobs):raise ValueError('Signed install/upgrade/native gate did not succeed')
    if p.get('signedNativeVisualReviewPassed') is not True or p.get('signedPngsReviewed')!=SIGNED_PNGS:raise ValueError('Actual signed native screenshots have not been reviewed')

def artifact(api,identifier,name,run,workflow,digest):
    m=api.request(api.base+'/actions/artifacts/'+str(identifier))
    if m['expired'] or m['name']!=name or m['workflow_run']['id']!=int(run) or m['workflow_run']['head_sha']!=workflow or m.get('digest')!='sha256:'+digest:raise ValueError('Immutable artifact differs: '+name)
    return m

def scope():
    trusted();protected_refs()
    if git('rev-parse','HEAD')!=os.environ['GITHUB_SHA']:raise ValueError('Wrong workflow checkout')
    message=json.loads(Path(os.environ['GITHUB_EVENT_PATH']).read_text())['head_commit']['message']
    if MARKER in message and PUBLICATION_MARKER in message:raise ValueError('Ambiguous sign/publication activation')
    values={'mode':'checks','source':os.environ['GITHUB_SHA'],'test-run':os.environ['GITHUB_RUN_ID']}
    if MARKER in message or PUBLICATION_MARKER in message:
        a=approval_record();api=GitHub();accepted_jobs(api,a)
        subprocess.run(['git','merge-base','--is-ancestor',a['source'],'HEAD'],check=True)
        if git('rev-parse',a['source']+':src/main')!=a['mainTree'] or git('rev-parse','HEAD:src/main')!=a['mainTree']:raise ValueError('Reviewed app tree changed')
        artifact(api,a['reportsArtifactId'],'P05-reports-'+a['testRunId'],a['testRunId'],a['source'],a['reportsArtifactSha256'])
        artifact(api,a['visualArtifactId'],'P05-visual-'+a['testRunId'],a['testRunId'],a['source'],a['visualArtifactSha256'])
        mode='sign' if MARKER in message else 'publish'
        if mode=='sign':activation_paths(git('diff','--name-only',a['source'],'HEAD').splitlines())
        else:
            p=json.loads(PUBLICATION.read_text());publication_paths(git('diff','--name-only',p['buildWorkflowCommit'],'HEAD').splitlines());accepted_build(api,p)
            if p['source']!=a['source']:raise ValueError('Published APK source differs from full review')
            artifact(api,p['formalArtifactId'],'RunnerGardenia-P05-Release-'+p['buildRunId'],p['buildRunId'],p['buildWorkflowCommit'],p['formalArtifactSha256'])
        values={'mode':mode,'source':a['source'],'test-run':a['testRunId']}
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'],'a') as out:
            for k,v in values.items():out.write(k+'='+v+'\n')
    print(json.dumps(values,indent=2));return values

if __name__=='__main__':scope()
