"""P0.2 branch adapter; reuse all existing strict report/GPX/durability/offline gates."""
import json
import os
from pathlib import Path
import subprocess
import p0_pipeline as p0


def protected_refs():
    baseline=json.loads(Path('docs/reports/branches/feature-p0-2-gps-ui/baseline.json').read_text())
    actual={line.split('\t')[1]:line.split('\t')[0] for line in subprocess.check_output(
        ['git','ls-remote','--heads','--tags','origin'],text=True).splitlines()}
    for ref,sha in {**baseline['protectedRemoteBranches'],**baseline['protectedTags']}.items():
        if actual.get(ref)!=sha:raise ValueError('Protected original ref changed: '+ref)
    subprocess.run(['git','merge-base','--is-ancestor',baseline['baselineCommit'],'HEAD'],check=True)
    return baseline


def configuration():
    data=json.loads(Path('.github/p02/stage.json').read_text())
    if data.get('phase') not in (3,4,5,6,7) or data.get('fullRegression') is not True or data.get('classes')!=[]:
        raise ValueError('P0.2 phases always require the complete original and new suites')
    if data.get('stage') not in (9,10) or data.get('signed') is not (data['phase']==6):
        raise ValueError('Only accepted final phase may request fixed-key signing')
    if data['signed'] and data['stage']!=10:raise ValueError('Final signing requires stage10 gates')
    return data


def prepare():
    baseline=protected_refs();config=configuration()
    if os.environ.get('GITHUB_REF')!='refs/heads/'+baseline['branch'] or os.environ.get('GITHUB_EVENT_NAME')!='push' or os.environ.get('GITHUB_REPOSITORY')!=baseline['repository']:
        raise ValueError('P0.2 only accepts exact isolated branch pushes')
    sha=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
    if sha!=os.environ['GITHUB_SHA']:raise ValueError('Wrong source checkout')
    version=dict(line.split('=',1) for line in Path('version.properties').read_text().splitlines() if line)
    version['VERSION_NAME_FULL']=version['VERSION_NAME']+'-'+sha[:12]
    Path('version.properties').write_text(''.join(k+'='+v+'\n' for k,v in version.items()))
    plan={'versionName':version['VERSION_NAME'],'versionCode':int(version['VERSION_CODE']),
          'commit':sha,'stage':config['stage'],'p02Phase':config['phase'],'branch':baseline['branch'],
          'runId':os.environ['GITHUB_RUN_ID'],'publishing':False}
    Path('release-plan.json').write_text(json.dumps(plan,indent=2)+'\n')
    with open(os.environ['GITHUB_OUTPUT'],'a') as out:
        out.write(f"stage={config['stage']}\nfull=true\nsigned={str(config['signed']).lower()}\nclasses=\n")
    print(json.dumps(plan,indent=2))


if __name__=='__main__':
    import sys
    p0.configuration=configuration
    p0.protected_refs=protected_refs
    if sys.argv[1]=='prepare':prepare()
    elif sys.argv[1]=='gate':p0.gate()
    elif sys.argv[1]=='refs':protected_refs();print('All original branches/tags unchanged')
    else:raise SystemExit('Unknown command')
