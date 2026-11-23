"""Clean-branch-only P0.7 sign/publish admission. Approval changes cannot alter tested code."""
import hashlib,json,os,subprocess
from pathlib import Path
from publish_release import GitHub
from p071_version import BRANCH,REPO

APPROVAL=Path('.github/p071/visual-approval.json')
PUBLICATION=Path('.github/p071/publication-approval.json')
SIGN_MARKER='[p071-sign-reviewed-source]'
PUBLISH_MARKER='[p071-publish-reviewed-apk]'
def git(*args):return subprocess.check_output(['git',*args],text=True).strip()
def trusted():
    if os.environ.get('GITHUB_REPOSITORY')!=REPO or os.environ.get('GITHUB_EVENT_NAME')!='push' or os.environ.get('GITHUB_REF')!='refs/heads/'+BRANCH:raise ValueError('Release requires the exact clean branch push')
def allowed(paths,approval):
    if not paths or any(p!=str(approval) and not p.startswith('docs/') for p in paths):raise ValueError('Activation may only change its approval record and documentation')
def refs():
    actual={ref:sha for sha,ref in (row.split('\t') for row in git('ls-remote','--heads','--tags','origin').splitlines())}
    for row in Path('docs/p0-7-1/protected-refs.txt').read_text().splitlines():
        sha,ref=row.split('\t')
        if actual.get(ref)!=sha:raise ValueError('Protected historical ref changed: '+ref)
def artifact(api,identifier,name,run,source,digest):
    row=api.request(api.base+'/actions/artifacts/'+str(identifier))
    if row['expired'] or row['name']!=name or row['workflow_run']['id']!=int(run) or row['workflow_run']['head_sha']!=source or row.get('digest')!='sha256:'+digest:raise ValueError('Immutable artifact differs: '+name)
    return row
def accepted_run(api,run,source,path,job):
    row=api.request(api.base+'/actions/runs/'+str(run))
    if row['head_sha']!=source or row['head_branch']!=BRANCH or row['event']!='push' or row['conclusion']!='success' or row['path']!=path:raise ValueError('Exact-source required workflow did not succeed')
    jobs=api.request(api.base+'/actions/runs/'+str(run)+'/jobs?per_page=100')['jobs']
    if not any(j['name']==job and j['conclusion']=='success' for j in jobs):raise ValueError('Required job did not execute successfully: '+job)
def approval():
    a=json.loads(APPROVAL.read_text())
    if any(a.get(k) is not True for k in ('stageAccepted','fullRegressionPassed','rawJUnitVerified','visualReviewPassed')) or not a.get('reviewedScreenshots'):raise ValueError('Actual complete source-bound test and visual acceptance is required')
    return a
def scope():
    trusted();refs()
    if git('rev-parse','HEAD')!=os.environ['GITHUB_SHA']:raise ValueError('Wrong workflow checkout')
    message=(json.loads(Path(os.environ['GITHUB_EVENT_PATH']).read_text()).get('head_commit') or {}).get('message','')
    if SIGN_MARKER in message and PUBLISH_MARKER in message:raise ValueError('Ambiguous release activation')
    mode='sign' if SIGN_MARKER in message else 'publish' if PUBLISH_MARKER in message else 'inactive'
    values={'mode':mode}
    if mode!='inactive':
        a=approval();api=GitHub();accepted_run(api,a['testRunId'],a['source'],'.github/workflows/p071-navigation.yml','stage-validation')
        subprocess.run(['git','merge-base','--is-ancestor',a['source'],'HEAD'],check=True)
        if git('rev-parse',a['source']+':src/main')!=a['mainTree'] or git('rev-parse','HEAD:src/main')!=a['mainTree']:raise ValueError('Reviewed production tree changed')
        artifact(api,a['reportsArtifactId'],a['reportsArtifactName'],a['testRunId'],a['source'],a['reportsArtifactSha256'])
        if mode=='sign':allowed(git('diff','--name-only',a['source'],'HEAD').splitlines(),APPROVAL)
        else:
            p=json.loads(PUBLICATION.read_text());accepted_run(api,p['buildRunId'],p['buildWorkflowCommit'],'.github/workflows/p071-release.yml','signed')
            if p['source']!=a['source'] or p.get('signedVisualReviewPassed') is not True or p.get('signedPngsReviewed')!=99:raise ValueError('All actual signed screenshots require review')
            allowed(git('diff','--name-only',p['buildWorkflowCommit'],'HEAD').splitlines(),PUBLICATION)
            artifact(api,p['formalArtifactId'],p['formalArtifactName'],p['buildRunId'],p['buildWorkflowCommit'],p['formalArtifactSha256'])
        values.update(source=a['source'],testRunId=a['testRunId'])
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'],'a') as f:
            for k,v in values.items():f.write(k+'='+str(v)+'\n')
    print(json.dumps(values));return values

if __name__=='__main__':scope()
