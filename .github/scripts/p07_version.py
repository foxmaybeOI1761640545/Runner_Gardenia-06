"""P0.7 allocation and read-only conflict checks; never edits refs or releases."""
import json,subprocess
from pathlib import Path
from publish_release import GitHub
from release_policy import ledger_history,properties

VERSION='v0.1.0-p0.7.0'
TAG='test-p0-'+VERSION
CERT='E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22'
APK='RunnerGardenia-P0-Release.apk'
BRANCH='feature/p0-7-to-p1-0-offline-navigation-clean'
REPO='foxmaybeOI1761640545/Runner_Gardenia-06'
BASELINE='ae68319361c1277e97e4fe9bfc9ae52de88146e5'

def git(*args):return subprocess.check_output(['git',*args],text=True).strip()
def allocation():
    rows=ledger_history();old=json.loads(git('show',BASELINE+':release-ledger.json'))['allocations']
    if rows[:len(old)]!=old or len(rows)!=len(old)+1:raise ValueError('Historical allocations changed')
    row=rows[-1]
    if row['versionName']!=VERSION or row['classification']!='reserved-p07-test' or row['versionCode']!=max(r['versionCode'] for r in old)+1:raise ValueError('Invalid unique P0.7 reservation')
    return row

def remote_history(api,commit,code):
    releases=[];page=1
    while True:
        batch=api.request(api.base+f'/releases?per_page=100&page={page}');releases+=batch
        if len(batch)<100:break
        page+=1
    for release in releases:
        assets=api.assets(release);hits=[a for a in assets if a['name']=='build-info.json']
        if not hits and not assets and release['tag_name']==TAG and release['draft'] and release['target_commitish']==commit:continue
        if len(hits)!=1:raise ValueError('Release lacks unambiguous build-info: '+release['tag_name'])
        info=json.loads(api.request(hits[0]['url'],binary=True))
        if release['tag_name']==TAG:
            if (info['commit'],info['versionName'],info['versionCode'])!=(commit,VERSION,code):raise ValueError('P0.7 Release already belongs to different bytes/source')
        elif info['versionCode']>=code:raise ValueError('Another Release owns this or a newer code')
    row=allocation()
    for line in git('ls-remote','--heads','--tags','origin').splitlines():
        sha,ref=line.split('\t')
        if ref.endswith('^{}'):continue
        # checkout fetch-depth0 supplies all remote objects; a concurrent ref is fetched read-only.
        if subprocess.run(['git','cat-file','-e',sha+'^{commit}'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode:
            subprocess.run(['git','fetch','--no-tags','origin',sha],check=True,stdout=subprocess.DEVNULL)
        if ref.startswith('refs/heads/'):
            p=subprocess.run(['git','show',sha+':release-ledger.json'],text=True,capture_output=True)
            if p.returncode:continue
            for other in json.loads(p.stdout)['allocations']:
                if other['versionCode']>=code and other!=row:raise ValueError('Conflicting remote allocation: '+ref)
        else:
            value=properties(git('show',sha+':version.properties'))
            if int(value['VERSION_CODE'])>=code and (ref!='refs/tags/'+TAG or git('rev-parse',sha+'^{commit}')!=commit or value['VERSION_NAME']!=VERSION):raise ValueError('Conflicting remote Tag: '+ref)
    return releases

def check(commit):
    row=allocation();p=properties(Path('version.properties').read_text())
    if p['VERSION_NAME']!=VERSION or int(p['VERSION_CODE'])!=row['versionCode']:raise ValueError('Version properties differ from reservation')
    remote_history(GitHub(),commit,row['versionCode']);return row['versionCode']
