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

def scope():
 trusted();protected_refs()
 if git('rev-parse','HEAD')!=os.environ['GITHUB_SHA']:raise ValueError('Wrong workflow checkout')
 message=json.loads(Path(os.environ['GITHUB_EVENT_PATH']).read_text())['head_commit']['message']
 activate=MARKER in message
 values={'release':'false','source':os.environ['GITHUB_SHA'],'test-run':os.environ['GITHUB_RUN_ID']}
 if activate:
  a=approval_record();subprocess.run(['git','merge-base','--is-ancestor',a['source'],'HEAD'],check=True)
  activation_paths(git('diff','--name-only',a['source'],'HEAD').splitlines());accepted_jobs(GitHub(),a)
  if git('rev-parse',a['source']+':src/main')!=a['mainTree'] or git('rev-parse','HEAD:src/main')!=a['mainTree']:raise ValueError('Reviewed application tree differs')
  values={'release':'true','source':a['source'],'test-run':a['testRunId'],'reports-artifact':str(a['reportsArtifactId'])}
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
