"""Verify approved full evidence, sign once in android-release, preserve immutable bytes."""
import hashlib,io,json,os,shutil,subprocess,sys,zipfile,xml.etree.ElementTree as ET
from pathlib import Path
import p071_release as admission
from p071_version import VERSION,CERT,APK,check
from publish_release import GitHub,verify_apk

def context():
    v=admission.scope()
    if v['mode']=='inactive':raise ValueError('No release activation')
    c={'mode':v['mode'],'workflowCommit':os.environ['GITHUB_SHA'],'approval':admission.approval()}
    if v['mode']=='publish':c['publication']=json.loads(admission.PUBLICATION.read_text())
    (Path(os.environ['RUNNER_TEMP'])/'p071-context.json').write_text(json.dumps(c)+'\n')
def load():
    admission.trusted();admission.refs();c=json.loads((Path(os.environ['RUNNER_TEMP'])/'p071-context.json').read_text());a=c['approval'];api=GitHub()
    if c['workflowCommit']!=os.environ['GITHUB_SHA'] or admission.git('rev-parse','HEAD')!=a['source']:raise ValueError('Signing checkout differs from reviewed source')
    admission.accepted_run(api,a['testRunId'],a['source'],'.github/workflows/p071-navigation.yml','stage-validation')
    approval=admission.APPROVAL if c['mode']=='sign' else admission.PUBLICATION
    base=a['source'] if c['mode']=='sign' else c['publication']['buildWorkflowCommit']
    admission.allowed(admission.git('diff','--name-only',base,c['workflowCommit']).splitlines(),approval)
    return c,api
def unpack(api,identifier,name,run,source,digest,folder):
    meta=admission.artifact(api,identifier,name,run,source,digest);raw=api.request(meta['archive_download_url'],binary=True)
    if hashlib.sha256(raw).hexdigest()!=digest:raise ValueError('Downloaded artifact digest differs')
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        if any(Path(n).is_absolute() or '..' in Path(n).parts for n in z.namelist()):raise ValueError('Unsafe artifact path')
        z.extractall(folder)
def inputs():
    c,api=load();a=c['approval'];unpack(api,a['reportsArtifactId'],a['reportsArtifactName'],a['testRunId'],a['source'],a['reportsArtifactSha256'],'gate')
    shutil.copy2('gate/release-plan.json','release-plan.json')
    if c['mode']=='publish':
        p=c['publication'];unpack(api,p['formalArtifactId'],p['formalArtifactName'],p['buildRunId'],p['buildWorkflowCommit'],p['formalArtifactSha256'],'dist')
    gate()
def gate():
    c,_=load();a=c['approval'];plan=json.loads(Path('gate/release-plan.json').read_text());g=json.loads(Path('gate/build/reports/p071-stage/gate.json').read_text())
    if plan!=json.loads(Path('release-plan.json').read_text()) or g.get('plan')!=plan or g.get('passed') is not True or plan.get('stage')!=3 or plan.get('commit')!=a['source'] or not plan.get('fullRegression') or plan.get('signed') is not False or plan.get('publishing') is not False or str(plan['runId'])!=str(a['testRunId']):raise ValueError('Missing exact-source full P0.7 gate')
    from check_test_results import inspect
    for key,path,src,minimum,skips in [('android','build/outputs/androidTest-results','src/androidTest/java',489,1),('jvm','build/test-results/testReproducibleDebugUnitTest','src/test/java',167,0)]:
        actual=inspect(Path('gate')/path,Path(src),key=='android')
        if not actual['passed'] or actual['tests']<minimum or actual['skipped']!=skips or any(actual[k]!=g[key][k] for k in ('tests','executed','failures','errors','skipped')):raise ValueError('Raw execution differs: '+key)
    historical=[(t.get('classname'),t.get('name')) for p in Path('gate/build/outputs/androidTest-results').rglob('TEST-*.xml') for t in ET.parse(p).getroot().iter('testcase') if t.find('skipped') is not None]
    if historical!=[('de.dennisguse.opentracks.services.TrackRecordingServiceStateMachineTest','recording_stopGPS_noop')]:raise ValueError('Additional/changed Skip')
    h=g['helpers']
    if not h['passed'] or h['tests']<156 or h['tests']!=h['executed'] or h['source']!=a['source'] or any(h[k] for k in ('failures','errors','skipped')):raise ValueError('Helper execution missing')
    if plan.get('versionName')!=VERSION or plan.get('versionCode')!=check(a['source']):raise ValueError('Wrong reservation')
    from p071_delivery import gate as delivery
    d=delivery(a['source'],Path('gate'))
    reviewed=a['reviewedScreenshots']
    if len(reviewed)!=len(d['screenshots']) or any(r.get('reviewed') is not True for r in reviewed) or {(r['file'],r['sha256']) for r in reviewed}!={(r['file'],r['sha256']) for r in d['screenshots']}:raise ValueError('Actual native review differs from complete tested screenshots')
    if g['gpsFocused']['source']!=a['source'] or g['gpsFocused']['tests']['executed']!=45 or not g['gpsFocused']['tests']['passed']:raise ValueError('Handoff/original GPS regression missing')
    permission=g['permissions'];recovery=permission['phases']['recovery-denied']
    if not permission['passed'] or permission['source']!=a['source'] or not recovery['p0RecoveryRejected'] or recovery['actualGpsPermission']!='denied':raise ValueError('Actual denied recovery missing')
    return plan,{**g,'unit':g['jvm']}
def existing():
    gate();c,api=load();run=os.environ['GITHUB_RUN_ID'];rows=api.request(api.base+f'/actions/runs/{run}/artifacts?per_page=100')['artifacts'];values={}
    for kind,label in [('formal','Release'),('candidate','Candidate')]:
        name='RunnerGardenia-P071-'+label+'-'+run;hits=[r for r in rows if r['name']==name]
        if len(hits)>1:raise ValueError('Ambiguous immutable signed artifact')
        if hits:admission.artifact(api,hits[0]['id'],name,run,c['workflowCommit'],hits[0]['digest'].removeprefix('sha256:'))
        values[kind+'-id']=str(hits[0]['id']) if hits else ''
    with open(os.environ['GITHUB_OUTPUT'],'a') as f:
        for k,v in values.items():f.write(k+'='+v+'\n')
def verify():
    if os.environ.get('ANDROID_SIGNING_CERT_SHA256','').replace(':','').strip().upper()!=CERT:raise ValueError('Independent fixed certificate differs')
    import verify_p0_release as original
    from p06_offline import enable_map_activity
    enable_map_activity();original.gate=gate;original.verify();c,_=load();p=Path('dist/build-info.json');info=json.loads(p.read_text())
    log=(Path(os.environ['RUNNER_TEMP'])/'p071-release-gradle.log').read_text()
    for task in ('minifyReproducibleReleaseWithR8','optimizeReproducibleReleaseResources'):
        if task not in log:raise ValueError('Actual optimization task evidence absent: '+task)
    info.update(testRunId=c['approval']['testRunId'],buildRunId=os.environ['GITHUB_RUN_ID'],buildWorkflowCommit=c['workflowCommit'],mainTree=c['approval']['mainTree'],r8TaskExecuted=True,resourceOptimizationTaskExecuted=True)
    p.write_text(json.dumps(info,indent=2)+'\n')
def finalize():
    plan,g=gate();folder=Path('dist');info=json.loads((folder/'build-info.json').read_text())
    (folder/'test-summary.json').write_text(json.dumps({'source':plan['commit'],'helpers':g['helpers'],'jvm':g['jvm'],'android':g['android'],'historicalSkip':g['historicalSkip'],'physicalDevices':'NEEDS_DEVICE_VALIDATION'},indent=2)+'\n')
    (folder/'RELEASE_NOTES.md').write_text(f"""[TEST][P0.7.1] {VERSION}

修复四个一级页面的返回历史；根页面返回将任务置于后台，保留页面实例。内部分类、历史详情和弹窗按原顺序返回，防止快速重复切换产生重复启动。没有修改 GPS/BLE/运动服务或数据库。

Application ID: com.fox.runner
Version Code: {info['versionCode']}
Source: {info['commit']}
APK SHA256: {info['apkSha256']}
Certificate SHA256: {CERT}

T01～T18、Python/JVM/API35、真实离线地图、GPX1.1、持久化及固定签名 P0.7.0 覆盖升级已执行；唯一历史 Skip 不计通过。模拟器使用合成 GPS/BLE 数据，实体手机的边缘/预测性手势、GNSS、BLE 射频及 OEM 后台策略仍待验证。

P0.8.0 将在本版本独立发布、公开下载验证和交付报告全部完成后开始；本 APK 不包含路线库或路线跟随。
""")
    (folder/'DEVELOPMENT_COMMITS.txt').write_text(admission.git('log','--reverse','--format=%H %s','ae68319361c1277e97e4fe9bfc9ae52de88146e5..'+info['commit'])+'\n')
    with zipfile.ZipFile(folder/'key-test-receipts.zip','w',zipfile.ZIP_DEFLATED) as z:
        for root in (Path('gate'),Path('build/reports/p071-release-smoke')):
            for p in root.rglob('*'):
                if p.is_file() and p.suffix in ('.json','.xml','.gpx'):z.write(p,str(p))
    names=[p.name for p in folder.iterdir() if p.is_file() and p.name!='ARTIFACT-MANIFEST.json']
    (folder/'ARTIFACT-MANIFEST.json').write_text(json.dumps({n:hashlib.sha256((folder/n).read_bytes()).hexdigest() for n in sorted(names)},indent=2)+'\n')
    bundle()
def bundle(folder=Path('dist')):
    plan,g=gate();c,_=load();info=json.loads((folder/'build-info.json').read_text());runtime=json.loads((folder/'runtime-smoke.json').read_text());native=json.loads((folder/'native-theme-smoke.json').read_text())
    if info.get('plan')!=plan or info.get('commit')!=plan['commit'] or info.get('certificateSha256')!=CERT or info.get('mainTree')!=c['approval']['mainTree'] or info.get('applicationId')!='com.fox.runner' or info.get('versionName')!=VERSION or info.get('versionCode')!=plan['versionCode']:raise ValueError('Wrong formal APK identity')
    if any(info.get(k) is not True for k in ('signatureVerified','alignmentVerified','runtimeSmokeVerified','r8TaskExecuted','resourceOptimizationTaskExecuted')):raise ValueError('Actual APK verification incomplete')
    if runtime.get('commit')!=info['commit'] or runtime.get('apkSha256')!=info['apkSha256'] or runtime.get('upgradeFromVersionCode')!=6780 or any(runtime.get(k) is not True for k in ('passed','deviceConfigurationRetained','gpsSettingsUpgradeRetained','gpsPreferencesRetainedAfterWorkout','baselineMapRetained','preparationCreatesNoWorkout','forceStopRecovery','releaseGpxSchemaVerified','nativeRateSwitchKeepsRecording')):raise ValueError('Actual P0.7.0 retained-data/GPS upgrade incomplete')
    if native.get('commit')!=info['commit'] or native.get('apkSha256')!=info['apkSha256'] or native.get('nativePageChecks')!=88 or len(native.get('screenshots',[]))!=88 or len(native.get('schemes',[]))!=8 or any(native.get(k) is not True for k in ('passed','processRestartPersistence','metricProcessPersistence','historyBatchVerified','sharePreservesData','appVersionVerified')):raise ValueError('Actual signed native UI checks incomplete')
    navigation=json.loads((folder/'p071-signed-navigation.json').read_text())
    if navigation.get('passed') is not True or navigation.get('source')!=info['commit'] or navigation.get('apkSha256')!=info['apkSha256'] or len(navigation.get('rootBackChecks',[]))<30 or any(row.get('backgrounded') is not True or row.get('homeExplicitlyReselected') is not True for row in navigation['rootBackChecks']):raise ValueError('Actual signed root Back probes incomplete')
    from p06_release_map_ui import verify_proof
    from p061_signed_real_map import verify as actual_map
    map_proof=verify_proof(folder,info);real_proof=actual_map(folder,info)
    rawzip=(folder/'theme-screenshots.zip').read_bytes()
    if hashlib.sha256(rawzip).hexdigest()!=native['screenshotsZipSha256']:raise ValueError('Native screenshot archive differs')
    from p061_release_theme_ui import page_matches,PAGE_ACTIVITIES
    with zipfile.ZipFile(io.BytesIO(rawzip)) as z:
        for row in native['screenshots']:
            png=z.read(row['file']);xml=z.read(row['nativeWindowFile'])
            if hashlib.sha256(png).hexdigest()!=row['sha256'] or hashlib.sha256(xml).hexdigest()!=row['nativeWindowSha256'] or not page_matches(ET.fromstring(xml),row['page']) or PAGE_ACTIVITIES[row['page']] not in row['focusedWindow']:raise ValueError('Actual signed PNG/native window differs')
    manifest=json.loads((folder/'ARTIFACT-MANIFEST.json').read_text())
    if set(manifest)!={p.name for p in folder.iterdir() if p.is_file() and p.name not in ('ARTIFACT-MANIFEST.json','publication.json')}:raise ValueError('Formal payload set changed')
    for name,digest in manifest.items():
        if Path(name).name!=name or hashlib.sha256((folder/name).read_bytes()).hexdigest()!=digest:raise ValueError('Formal payload digest differs')
    raw=(folder/APK).read_bytes()
    if len(list(folder.glob('*.apk')))!=1 or len(raw)!=info['apkBytes'] or hashlib.sha256(raw).hexdigest()!=info['apkSha256']:raise ValueError('Signed APK bytes differ')
    if c['mode']=='publish':
        p=c['publication']
        if any(info[k]!=p[k] for k in ('apkSha256','buildRunId','buildWorkflowCommit')):raise ValueError('Signed review selected different bytes')
        rows=native['screenshots']+map_proof['screenshots']+real_proof['screenshots'];reviewed=p['reviewedSignedScreenshots']
        if len(reviewed)!=99 or len(rows)!=99 or any(r.get('reviewed') is not True for r in reviewed) or {(r['file'],r['sha256']) for r in reviewed}!={(r['file'],r['sha256']) for r in rows}:raise ValueError('Actual signed visual review differs')
    verify_apk(raw,info);return info

if __name__=='__main__':{'context':context,'inputs':inputs,'gate':gate,'existing':existing,'verify':verify,'finalize':finalize,'bundle':bundle}[sys.argv[1]]()
