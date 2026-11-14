"""Explicit P0.6.1 allocation; preserve all published and abandoned historical codes."""
import json
from pathlib import Path
from publish_release import GitHub

VERSION='v0.1.0-p0.6.1'
TAG='test-p0-'+VERSION
CERT='E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22'
APK='RunnerGardenia-P0-Release.apk'
BASELINE_SHA='aa5ebc21c76cbca492edc66453b91d7fdecaf82bd5a69fcab208c740b4224ebc'


def allocated_code(data):
    from release_policy import ledger_history
    # The existing history validator also validates the live file's complete schema/high-water.
    actual=json.loads(Path('release-ledger.json').read_text())
    if actual!=data:raise ValueError('Allocation differs from live ledger')
    entries=ledger_history()
    chosen=[e for e in entries if e['versionName']==VERSION]
    if len(chosen)!=1 or chosen[0].get('classification')!='reserved-p061-test':raise ValueError('Missing unique P0.6.1 reservation')
    row=chosen[0];others=[e['versionCode'] for e in entries[:entries.index(row)]]
    if row['versionCode']!=max(others)+1 or data['codeHighWater']<row['versionCode']:raise ValueError('P0.6.1 allocation must follow its historical predecessors')
    return row['versionCode']


def remote_history(api,commit,code):
    releases=[];page=1
    while True:
        batch=api.request(api.base+f'/releases?per_page=100&page={page}');releases+=batch
        if len(batch)<100:break
        page+=1
    for release in releases:
        assets=[a for a in api.assets(release) if a['name']=='build-info.json']
        if not assets and release['tag_name']==TAG and release['draft'] and release['target_commitish']==commit and not api.assets(release):continue
        if len(assets)!=1:raise ValueError('Existing Release lacks unambiguous version evidence: '+release['tag_name'])
        info=json.loads(api.request(assets[0]['url'],binary=True))
        if release['tag_name']==TAG:
            if info['commit']!=commit or info['versionName']!=VERSION or info['versionCode']!=code:raise ValueError('P0.6.1 already belongs to another source/allocation')
        elif info['versionCode']>=code:raise ValueError('Published/reserved remote Version Code conflicts with P0.6.1')
    return releases


def check(commit):
    code=allocated_code(json.loads(Path('release-ledger.json').read_text()))
    properties=dict(line.split('=',1) for line in Path('version.properties').read_text().splitlines() if line and not line.startswith('#'))
    if properties['VERSION_NAME']!=VERSION or int(properties['VERSION_CODE'])!=code:raise ValueError('Properties differ from P0.6.1 ledger reservation')
    remote_history(GitHub(),commit,code)
    return code
