"""Tag only an accepted signed/installed APK, then resume a verified non-Latest draft."""
import datetime
import hashlib
import json
import os
import re
from pathlib import Path
import urllib.parse
import urllib.request
from publish_release import GitHub,digest
from p03_signing import bundle
from p03_version import TAG,VERSION,APK,CERT,remote_history

MARKER='Runner-Gardenia-P03: '


def verify_tag(api,info,optional=False):
    ref=api.request(api.base+'/git/ref/tags/'+TAG,optional=optional)
    if ref is None:return None
    if ref['object']['type']!='tag':raise ValueError('Existing test tag must be annotated')
    tag=api.request(api.base+'/git/tags/'+ref['object']['sha'])
    records=[l[len(MARKER):] for l in tag['message'].splitlines() if l.startswith(MARKER)]
    expected={k:info[k] for k in ('commit','versionName','versionCode','apkSha256','certificateSha256')}
    if tag['object']['type']!='commit' or tag['object']['sha']!=info['commit'] or len(records)!=1 or json.loads(records[0])!=expected:raise ValueError('Existing tag belongs to different source/APK; never overwrite')
    return tag


def notes(info):
    return (f"[TEST][P0.3] {VERSION}\n\n"
        "四个原生主题家族：Precision Pro、Quiet Minimal、Outdoor Explorer、Neon Pulse；共八套配色。\n"
        "设置只选择家族，浅深色固定跟随 Android 系统。配置切换保留页面、运动、GPS 和心率连接状态。\n"
        "Java / XML / ViewBinding / 原生 Drawable；无 WebView，未重写 GPS、BLE、数据库或 GPX 算法。\n\n"
        f"- Application ID: `com.fox.runner`\n- Version Name: `{VERSION}`\n- Version Code: `{info['versionCode']}`\n"
        f"- Source: `{info['commit']}`\n- APK SHA-256: `{info['apkSha256']}`\n- Certificate SHA-256: `{CERT}`\n"
        f"- Actions: {info['run']}\n- Tests: `{json.dumps(info['tests'])}`\n"
        "- API35 原生主题测试、签名 APK 八套实际界面截图、P0.2 原位覆盖与保留数据 GPX 导出已验证。\n"
        "- 主题截图及原生验收收据随本 Release 提供。H303 RF、户外 GNSS 与耗电仍属原有 NEEDS_DEVICE_VALIDATION。\n"
        "- 可直接覆盖 P0.2；固定签名、相同包名和更高 Code。网页全部十套家族继续保留在设计实验室。\n")


def helper_counts(api,run):
    jobs=api.request(api.base+f'/actions/runs/{run}/jobs?per_page=100&filter=all')['jobs']
    selected=[j for j in jobs if j['name']=='checks' and j['conclusion']=='success']
    if not selected:raise ValueError('Successful same-run checks job missing')
    selected=[max(selected,key=lambda j:j['id'])]
    log=api.request(api.base+f"/actions/jobs/{selected[0]['id']}/logs",binary=True).decode('utf-8')
    matches=list(re.finditer(r'Ran (\d+) tests in [^\r\n]+',log))
    if len(matches)!=1 or int(matches[0][1])<=0:raise ValueError('Actual helper execution count missing')
    tail=log[matches[0].end():].splitlines()[:5]
    if not any(re.search(r'(?:^|Z )OK\s*$',line) for line in tail) or any('skipped=' in line for line in tail):raise ValueError('Helper regression not complete/clean')
    return {'tests':int(matches[0][1]),'executed':int(matches[0][1]),'failures':0,'errors':0,'skipped':0,'jobId':selected[0]['id']}


def publish(api,folder=Path('dist'),verify_bundle=None):
    info=(verify_bundle or bundle)(folder);remote_history(api,info['commit'],info['versionCode'])
    helpers=helper_counts(api,info['plan']['runId'])
    if verify_tag(api,info,True) is None:
        record={k:info[k] for k in ('commit','versionName','versionCode','apkSha256','certificateSha256')}
        annotation=MARKER+json.dumps(record,sort_keys=True)
        tag=api.request(api.base+'/git/tags','POST',{'tag':TAG,'message':'P0.3 test release; accepted fixed-key APK\n'+annotation,
            'object':info['commit'],'type':'commit','tagger':{'name':'github-actions[bot]','email':'41898282+github-actions[bot]@users.noreply.github.com','date':datetime.datetime.now(datetime.timezone.utc).isoformat()}})
        try:api.request(api.base+'/git/refs','POST',{'ref':'refs/tags/'+TAG,'sha':tag['sha']})
        except RuntimeError:
            if verify_tag(api,info,True) is None:raise
    verify_tag(api,info)
    title='[TEST][P0.3] '+VERSION;body=notes(info)
    (folder/'RELEASE_NOTES.md').write_text(body)
    (folder/'test-summary.json').write_text(json.dumps({'commit':info['commit'],'run':info['run'],'tests':info['tests'],
        'helperTests':helpers,'runtimeSmokeVerified':True,'upgradeFromVersionCode':6773,'hardware':'NEEDS_DEVICE_VALIDATION','bleProductionChanged':False},indent=2)+'\n')
    names=['build-info.json',APK,'SHA256SUMS','runtime-smoke.json','native-theme-smoke.json','theme-screenshots.zip','test-summary.json','RELEASE_NOTES.md']
    payloads={name:(folder/name).read_bytes() for name in names}
    def find():
        rows=[];page=1
        while True:
            batch=api.request(api.base+f'/releases?per_page=100&page={page}');rows+=batch
            if len(batch)<100:break
            page+=1
        hits=[r for r in rows if r['tag_name']==TAG]
        if len(hits)>1:raise ValueError('Ambiguous test Release')
        return hits[0] if hits else None
    release=find()
    if release is None:
        try:release=api.request(api.base+'/releases','POST',{'tag_name':TAG,'target_commitish':info['commit'],'name':title,'body':body,'draft':True,'prerelease':True,'make_latest':'false'})
        except RuntimeError:
            release=find()
            if release is None:raise
    if release['tag_name']!=TAG or not release['prerelease'] or release['target_commitish']!=info['commit']:raise ValueError('Existing release identity/type differs')
    existing={a['name']:a for a in api.assets(release)}
    if set(existing)-set(payloads):raise ValueError('Unexpected asset; do not remove or overwrite')
    for name,raw in payloads.items():
        if name in existing:
            if digest(api.request(existing[name]['url'],binary=True))!=digest(raw):raise ValueError('Existing asset bytes differ; never replace '+name)
        else:
            if not release['draft']:raise ValueError('Published Release incomplete; do not silently modify')
            api.request(release['upload_url'].split('{')[0]+'?'+urllib.parse.urlencode({'name':name}),'POST',raw)
    def verify_assets(current):
        assets={a['name']:a for a in api.assets(current)}
        if set(assets)!=set(payloads):raise ValueError('Release asset set incomplete')
        for name,raw in payloads.items():
            a=assets[name]
            if a['state']!='uploaded' or a['size']!=len(raw) or (a.get('digest') and a['digest']!='sha256:'+digest(raw)):raise ValueError('Asset metadata mismatch')
            if digest(api.request(a['url'],binary=True))!=digest(raw):raise ValueError('Downloaded asset checksum mismatch')
        return assets
    # Every Draft asset is downloaded and checked BEFORE publication.
    verify_assets(release);verify_tag(api,info)
    if release['draft']:api.request(release['url'],'PATCH',{'draft':False,'prerelease':True,'make_latest':'false','name':title,'body':body})
    final=api.request(release['url']);assets=verify_assets(final)
    if final['draft'] or not final['prerelease']:raise ValueError('Pre-release publication not confirmed')
    latest=api.request(api.base+'/releases/latest',optional=True)
    if latest and latest['id']==final['id']:raise ValueError('Test release must never be Latest')
    # Re-download publicly, without an Authorization header, and re-run APK identity/signature verification.
    public_url=assets[APK]['browser_download_url']
    expected_prefix=f"https://github.com/{api.repo}/releases/download/{TAG}/"
    if not public_url.startswith(expected_prefix):raise ValueError('Unexpected public APK URL')
    with urllib.request.urlopen(urllib.request.Request(public_url,headers={'User-Agent':'Runner-Gardenia-public-verification'}),timeout=120) as response:
        public_apk=response.read()
        if response.status!=200:raise ValueError('Public APK is not downloadable')
    if digest(public_apk)!=info['apkSha256'] or public_apk!=payloads[APK]:raise ValueError('Public APK differs from accepted immutable artifact')
    from publish_release import verify_apk
    verify_apk(public_apk,info)
    receipt={'publicDownloadVerified':True,'publicSignatureVerified':True,'tag':TAG,'release':final['html_url'],'commit':info['commit'],'versionCode':info['versionCode'],'apk':assets[APK]['browser_download_url'],
        'apkSha256':info['apkSha256'],'apkBytes':info['apkBytes'],'certificateSha256':CERT,'draft':False,'prerelease':True,'latest':False,
        'assets':{n:{'bytes':len(raw),'sha256':digest(raw),'url':assets[n]['browser_download_url']} for n,raw in payloads.items()}}
    (folder/'publication.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt,indent=2))


if __name__=='__main__':publish(GitHub())
