"""Tag only an accepted signed/installed APK, then resume a verified non-Latest draft."""
import datetime
import hashlib
import json
import os
import re
from pathlib import Path
import urllib.parse
from publish_release import GitHub,digest
from p02_signing import bundle
from p02_version import TAG,VERSION,APK,CERT,remote_history

MARKER='Runner-Gardenia-P02: '


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
    return (f"[TEST][P0.2] {VERSION}\n\n"
        "GPS 定位准备阶段：可靠定位前不创建运动、不计时和累计距离；支持取消/超时继续等待，GPS 丢失时运动继续。\n"
        "运动首页优化：运动状态、距离/时间/速度/配速、心率摘要及随状态变化的操作。\n\n"
        "BLE 自动连接本轮仅完成源码审查，未修复 H303 首次连接无 BPM 的实机问题。\n"
        "H303/RF 断连重连、户外 GNSS 精度/实际1Hz、锁屏长期运行和耗电均为 NEEDS_DEVICE_VALIDATION。\n\n"
        f"- Application ID: `com.fox.runner`\n- Version Name: `{VERSION}`\n- Version Code: `{info['versionCode']}`\n"
        f"- Source: `{info['commit']}`\n- APK bytes: {info['apkBytes']}\n- APK SHA-256: `{info['apkSha256']}`\n"
        f"- Certificate SHA-256: `{CERT}`\n- Actions: {info['run']}\n"
        f"- Test counts: `{json.dumps(info['tests'],ensure_ascii=False)}`\n"
        "- Fixed-key signature, non-debuggable identity, alignment, R8/resources, P0.1 in-place upgrade, retained GPS/raw HR/device configuration and GPX export verified on API35 AOSP.\n\n"
        "Android8+：下载 APK，核对 SHA256SUMS，直接覆盖 P0.1 安装；保留应用数据，不要卸载或清除数据。测试版，请先备份重要运动记录。\n"
        "全部历史/当前报告：https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/feature/p0-2-gps-ui/docs/reports/README.md\n")


def helper_counts(api,run):
    jobs=api.request(api.base+f'/actions/runs/{run}/jobs?per_page=100&filter=all')['jobs']
    selected=[j for j in jobs if j['name']=='checks' and j['conclusion']=='success']
    if not selected:raise ValueError('Successful same-run checks job missing')
    selected=[max(selected,key=lambda j:j['id'])]
    log=api.request(api.base+f"/actions/jobs/{selected[0]['id']}/logs",binary=True,api_binary=True).decode('utf-8')
    matches=list(re.finditer(r'Ran (\d+) tests in [^\r\n]+',log))
    if len(matches)!=1 or int(matches[0][1])<=0:raise ValueError('Actual helper execution count missing')
    tail=log[matches[0].end():].splitlines()[:5]
    if not any(re.search(r'(?:^|Z )OK\s*$',line) for line in tail) or any('skipped=' in line for line in tail):raise ValueError('Helper regression not complete/clean')
    return {'tests':int(matches[0][1]),'executed':int(matches[0][1]),'failures':0,'errors':0,'skipped':0,'jobId':selected[0]['id']}


def publish(api,folder=Path('dist')):
    info=bundle(folder);remote_history(api,info['commit'],info['versionCode'])
    helpers=helper_counts(api,info['plan']['runId'])
    if verify_tag(api,info,True) is None:
        record={k:info[k] for k in ('commit','versionName','versionCode','apkSha256','certificateSha256')}
        annotation=MARKER+json.dumps(record,sort_keys=True)
        tag=api.request(api.base+'/git/tags','POST',{'tag':TAG,'message':'P0.2 test release; accepted fixed-key APK\n'+annotation,
            'object':info['commit'],'type':'commit','tagger':{'name':'github-actions[bot]','email':'41898282+github-actions[bot]@users.noreply.github.com','date':datetime.datetime.now(datetime.timezone.utc).isoformat()}})
        try:api.request(api.base+'/git/refs','POST',{'ref':'refs/tags/'+TAG,'sha':tag['sha']})
        except RuntimeError:
            if verify_tag(api,info,True) is None:raise
    verify_tag(api,info)
    title='[TEST][P0.2] '+VERSION;body=notes(info)
    (folder/'RELEASE_NOTES.md').write_text(body)
    (folder/'test-summary.json').write_text(json.dumps({'commit':info['commit'],'run':info['run'],'tests':info['tests'],
        'helperTests':helpers,'runtimeSmokeVerified':True,'upgradeFromVersionCode':6772,'hardware':'NEEDS_DEVICE_VALIDATION','bleProductionChanged':False},indent=2)+'\n')
    names=['build-info.json',APK,'SHA256SUMS','runtime-smoke.json','test-summary.json','RELEASE_NOTES.md']
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
    receipt={'tag':TAG,'release':final['html_url'],'commit':info['commit'],'versionCode':info['versionCode'],'apk':assets[APK]['browser_download_url'],
        'apkSha256':info['apkSha256'],'apkBytes':info['apkBytes'],'certificateSha256':CERT,'draft':False,'prerelease':True,'latest':False,
        'assets':{n:{'bytes':len(raw),'sha256':digest(raw),'url':assets[n]['browser_download_url']} for n,raw in payloads.items()}}
    (folder/'publication.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt,indent=2))


if __name__=='__main__':publish(GitHub())
