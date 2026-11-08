"""Full P0/P0.2/P0.3 regression adapter and explicit post-visual-review signing activation."""
import hashlib,json,os,subprocess,sys
from pathlib import Path
import p0_pipeline as original
import collect_p02_home_ui,collect_p03_theme_ui,collect_p04_ui
from publish_release import GitHub
from p04_version import VERSION,TAG,check
BASE='cebf4682cec672fbde022c54bda3b442ff0663ab'
REPO='foxmaybeOI1761640545/Runner_Gardenia-06'
BRANCH='feature/theme-system-p0-3-ui-refinement'
MARKER='[p04-release-accepted-ui]'
PUBLICATION_MARKER='[p04-publish-accepted-apk]'
PUBLICATION=Path('.github/p04/publication-approval.json')
APPROVAL=Path('.github/p04/visual-approval.json')
COLLECT=original.collect_p0_ui

def git(*args):return subprocess.check_output(['git',*args],text=True).strip()
def configuration():
 c=json.loads(Path('.github/p04/stage.json').read_text())
 if c!={'stage':10,'fullRegression':True,'classes':[],'signed':True}:raise ValueError('Refinement requires all original and new business/visual tests')
 return c

def protected_refs():
 baseline=json.loads(Path('docs/theme-system/ui-refinement/BASELINE.json').read_text())
 refs={line.split('\t')[1]:line.split('\t')[0] for line in git('ls-remote','--heads','--tags','origin').splitlines()}
 for name,sha in baseline['protectedRefs'].items():
  if refs.get(name)!=sha:raise ValueError('Protected original reference changed: '+name)
 subprocess.run(['git','merge-base','--is-ancestor',BASE,'HEAD'],check=True)
 return baseline

def trusted():
 if os.environ.get('GITHUB_REPOSITORY')!=REPO or os.environ.get('GITHUB_REF')!='refs/heads/'+BRANCH or os.environ.get('GITHUB_EVENT_NAME')!='push':raise ValueError('Untrusted refinement workflow context')

def activation_paths(paths):
 if not paths or any(p!='.github/p04/visual-approval.json' and not p.startswith('docs/') for p in paths):raise ValueError('Visual acceptance cannot conceal product/version/test/workflow/signing changes')
 return True

def approval_record():
 if not APPROVAL.exists():raise ValueError('Actual native visual review has not approved signing')
 a=json.loads(APPROVAL.read_text())
 if a.get('passed') is not True or a.get('kind')!='ACTUAL_NATIVE_VISUAL_REVIEW' or a.get('schemes')!=8 or a.get('viewports')!=[[360,800],[390,844],[412,915]] or a.get('fixturePngs')!=192 or a.get('businessRegressionPassed') is not True:raise ValueError('Native visual/business approval incomplete')
 return a

def accepted_jobs(api,a):
 run=api.request(api.base+'/actions/runs/'+a['testRunId'])
 if run['head_sha']!=a['source'] or run['head_branch']!=BRANCH or run['event']!='push' or run['conclusion']!='success':raise ValueError('Approved full test run/source did not actually succeed')
 jobs=api.request(api.base+'/actions/runs/'+a['testRunId']+'/jobs?per_page=100&filter=all')['jobs']
 for name in ['checks','native-window-helper']:
  if not any(j['name']==name and j['conclusion']=='success' for j in jobs):raise ValueError('Approved complete gate missing: '+name)
 return jobs

def publication_paths(paths):
 if not paths or any(p!='.github/p04/publication-approval.json' and not p.startswith('docs/') for p in paths):raise ValueError('Accepted APK publication cannot modify code/tests/version/signing/workflow')
 return True

def accepted_build(api,p):
 run=api.request(api.base+'/actions/runs/'+p['buildRunId'])
 if run['head_sha']!=p['buildWorkflowCommit'] or run['head_branch']!=BRANCH or run['event']!='push' or run['conclusion']!='success':raise ValueError('Accepted fixed-key build run did not succeed')
 jobs=api.request(api.base+'/actions/runs/'+p['buildRunId']+'/jobs?per_page=100&filter=all')['jobs']
 if not any(j['name']=='signed' and j['conclusion']=='success' for j in jobs):raise ValueError('Actual signed APK/upgrade/UI acceptance job absent')
 if p.get('signedNativeVisualReviewPassed') is not True or p.get('signedPngsReviewed')!=32:raise ValueError('Actual signed screenshots not visually reviewed')
 return jobs

def artifact(api,identifier,name,run,workflow_sha,digest):
 meta=api.request(api.base+'/actions/artifacts/'+str(identifier))
 if meta['expired'] or meta['name']!=name or meta['workflow_run']['id']!=int(run) or meta['workflow_run']['head_sha']!=workflow_sha or meta.get('digest')!='sha256:'+digest:raise ValueError('Immutable gate artifact differs: '+name)
 return meta

def scope():
 trusted();protected_refs()
 if git('rev-parse','HEAD')!=os.environ['GITHUB_SHA']:raise ValueError('Wrong workflow checkout')
 message=json.loads(Path(os.environ['GITHUB_EVENT_PATH']).read_text())['head_commit']['message']
 if MARKER in message and PUBLICATION_MARKER in message:raise ValueError('Ambiguous acceptance activation')
 mode='checks';values={'mode':mode,'source':os.environ['GITHUB_SHA'],'test-run':os.environ['GITHUB_RUN_ID']}
 if MARKER in message or PUBLICATION_MARKER in message:
  a=approval_record();subprocess.run(['git','merge-base','--is-ancestor',a['source'],'HEAD'],check=True);api=GitHub();accepted_jobs(api,a)
  if git('rev-parse',a['source']+':src/main')!=a['mainTree'] or git('rev-parse','HEAD:src/main')!=a['mainTree']:raise ValueError('Reviewed application tree differs')
  artifact(api,a['reportsArtifactId'],'P04-reports-'+a['testRunId'],a['testRunId'],a['source'],a['reportsArtifactSha256'])
  mode='sign' if MARKER in message else 'publish'
  if mode=='sign':activation_paths(git('diff','--name-only',a['source'],'HEAD').splitlines())
  else:
   p=json.loads(PUBLICATION.read_text());publication_paths(git('diff','--name-only',p['buildWorkflowCommit'],'HEAD').splitlines());accepted_build(api,p)
   if p['source']!=a['source']:raise ValueError('Published APK source differs from visually approved source')
   artifact(api,p['formalArtifactId'],'RunnerGardenia-P04-Release-'+p['buildRunId'],p['buildRunId'],p['buildWorkflowCommit'],p['formalArtifactSha256'])
  values={'mode':mode,'source':a['source'],'test-run':a['testRunId'],'reports-artifact':str(a['reportsArtifactId'])}
  if mode=='publish':values['formal-artifact']=str(p['formalArtifactId'])
 if os.environ.get('GITHUB_OUTPUT'):
  with open(os.environ['GITHUB_OUTPUT'],'a') as out:
   for k,v in values.items():out.write(k+'='+v+'\n')
 print(json.dumps(values,indent=2));return values


def prepare():
 trusted();protected_refs();configuration();sha=git('rev-parse','HEAD')
 if sha!=os.environ['GITHUB_SHA']:raise ValueError('Wrong exact-source regression checkout')
 code=check(sha)
 plan={'versionName':VERSION,'versionCode':code,'commit':sha,'stage':10,'branch':BRANCH,'runId':os.environ['GITHUB_RUN_ID'],'publishing':False,'testTag':TAG,'refinement':True,'visualApprovalRequiredBeforeSigning':True}
 Path('release-plan.json').write_text(json.dumps(plan,indent=2)+'\n');print(json.dumps(plan,indent=2))
 if os.environ.get('GITHUB_OUTPUT'):
  with open(os.environ['GITHUB_OUTPUT'],'a') as out:out.write('stage=10\nfull=true\nsigned=true\nclasses=\n')

def collect(matrix=False):
 count=COLLECT(matrix);source=json.loads(Path('release-plan.json').read_text())['commit']
 collect_p02_home_ui.collect(source);collect_p03_theme_ui.collect(source);collect_p04_ui.collect(source);return count

def gate():
 original.configuration=configuration;original.protected_refs=protected_refs;original.collect_p0_ui=collect;original.gate()

if __name__=='__main__':
 {'prepare':prepare,'gate':gate,'refs':protected_refs,'scope':scope}[sys.argv[1]]()
