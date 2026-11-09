"""Sign an actually reviewed full-test source once; retries reuse immutable APK bytes."""
import hashlib,io,json,os,shutil,subprocess,sys,zipfile
from pathlib import Path
import p05_release as pipeline
import verify_p0_release as original
from publish_release import GitHub,verify_apk
from p05_version import VERSION,TAG,CERT,APK,check

def context():
    values=pipeline.scope()
    if values['mode'] not in ('sign','publish'):raise ValueError('Explicit visual approval activation required')
    record={'mode':values['mode'],'workflowCommit':os.environ['GITHUB_SHA'],'approval':pipeline.approval_record()}
    if values['mode']=='publish':record['publication']=json.loads(pipeline.PUBLICATION.read_text())
    (Path(os.environ['RUNNER_TEMP'])/'p05-context.json').write_text(json.dumps(record,indent=2)+'\n')

def load():
    pipeline.trusted();c=json.loads((Path(os.environ['RUNNER_TEMP'])/'p05-context.json').read_text());a=c['approval'];api=GitHub()
    if c['workflowCommit']!=os.environ['GITHUB_SHA'] or pipeline.git('rev-parse','HEAD')!=a['source'] or pipeline.git('rev-parse','HEAD:src/main')!=a['mainTree']:raise ValueError('Checkout differs from the reviewed full-test app')
    pipeline.accepted_jobs(api,a);pipeline.protected_refs()
    if c['mode']=='sign':pipeline.activation_paths(pipeline.git('diff','--name-only',a['source'],c['workflowCommit']).splitlines())
    elif c['mode']=='publish':
        p=c['publication'];pipeline.accepted_build(api,p);pipeline.publication_paths(pipeline.git('diff','--name-only',p['buildWorkflowCommit'],c['workflowCommit']).splitlines())
    else:raise ValueError('Wrong acceptance mode')
    return c,api

def unpack(api,identifier,name,run,workflow,digest,folder):
    meta=pipeline.artifact(api,identifier,name,run,workflow,digest);raw=api.request(meta['archive_download_url'],binary=True)
    if hashlib.sha256(raw).hexdigest()!=digest:raise ValueError('Immutable artifact bytes changed')
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        if any(Path(n).is_absolute() or '..' in Path(n).parts for n in z.namelist()):raise ValueError('Unsafe artifact path')
        z.extractall(folder)

def validate_gate(plan,report,source):
    if (plan.get('commit')!=source or plan.get('stage')!=5 or not plan.get('fullRegression') or plan.get('classes')!=[]
        or plan.get('signed') is not False or plan.get('publishing') is not False or report.get('plan')!=plan or report.get('passed') is not True
        or plan.get('versionName')!=VERSION):raise ValueError('Signing requires the actual exact-source complete P0.5 gate')
    for scope in ('android','unit','helpers'):
        r=report.get(scope,{})
        if r.get('passed') is not True or r.get('executed',0)<=0 or r.get('failures')!=0 or r.get('errors')!=0 or r.get('tests')!=r['executed']+r.get('skipped',-1):raise ValueError('Missing real complete execution: '+scope)
        if r.get('skipped')!=(1 if scope=='android' else 0):raise ValueError('Additional skips are not allowed')
    legacy=report.get('legacySkippedTest',{})
    if legacy.get('method')!='recording_stopGPS_noop' or legacy.get('countedAsPassed') is not False or legacy.get('unchangedFromP04') is not True:raise ValueError('Original legacy Ignore must be explicitly reported, never counted as passed')
    for scope in ('processDurability','metricProcessDurability','batchProcessDurability','gpxSchema','offline'):
        r=report.get(scope,{})
        if r.get('passed') is not True or r.get('commit')!=source:raise ValueError('Exact-source final evidence missing: '+scope)
    if report.get('shareReceipts')!=6 or report.get('historyScreenshots')!=32 or report.get('settingsScreenshots')!=16 or report.get('metricScreenshots')!=8:raise ValueError('P0.5 native/share evidence incomplete')

def inputs():
    c,api=load();a=c['approval']
    unpack(api,a['reportsArtifactId'],'P05-reports-'+a['testRunId'],a['testRunId'],a['source'],a['reportsArtifactSha256'],'gate')
    shutil.copyfile('gate/release-plan.json','release-plan.json')
    if c['mode']=='publish':
        p=c['publication'];unpack(api,p['formalArtifactId'],'RunnerGardenia-P05-Release-'+p['buildRunId'],p['buildRunId'],p['buildWorkflowCommit'],p['formalArtifactSha256'],'dist')
    gate()

def gate():
    c,_=load();a=c['approval'];plan=json.loads(Path('release-plan.json').read_text());report=json.loads(Path('gate/build/reports/p05-validation/tests.json').read_text())
    if plan!=json.loads(Path('gate/release-plan.json').read_text()) or str(plan['runId'])!=a['testRunId']:raise ValueError('Transferred evidence differs from the approved run')
    validate_gate(plan,report,a['source'])
    if plan['versionCode']!=check(a['source']):raise ValueError('Wrong ledger allocation')
    for folder,manifest,count in [('p05-visual','p05-layout-matrix.json',48),('p04-visual','p04-visual-matrix.json',192),('p05-metrics','metric-matrix.json',8),('p05-settings','settings-matrix.json',16),('p05-history','history-matrix.json',32)]:
        directory=Path('gate/build/reports')/folder;r=json.loads((directory/manifest).read_text())
        if r.get('sourceCommit')!=a['source'] or r.get('passed') is not True or len(r['screenshots'])!=count:raise ValueError('Reviewed native source/matrix differs')
        for row in r['screenshots']:
            if hashlib.sha256((directory/row['file']).read_bytes()).hexdigest()!=row['sha256']:raise ValueError('Reviewed PNG changed')
    import collect_p02_home_ui
    collect_p02_home_ui.inspect(Path('gate/build/reports/p0-ui'),a['source'])
    from collect_p05_history import validate_receipt
    shares=json.loads(Path('gate/build/reports/p05-share/result.json').read_text())
    if shares.get('commit')!=a['source'] or len(shares['receipts'])!=6:raise ValueError('Cross-UID receipt set differs')
    for kind in ('single','multiple','zip'):
        for denied in (True,False):
            path=Path('gate/build/reports/p05-share')/(kind+('-denied' if denied else '-received')+'.json')
            validate_receipt(a['source'],kind,denied,json.loads(path.read_text()))
    return plan,report

def existing():
    gate();c,api=load();run=os.environ['GITHUB_RUN_ID'];rows=api.request(api.base+f'/actions/runs/{run}/artifacts?per_page=100')['artifacts'];values={}
    for kind,name in [('formal','RunnerGardenia-P05-Release-'+run),('candidate','RunnerGardenia-P05-Candidate-'+run)]:
        hits=[r for r in rows if r['name']==name]
        if len(hits)>1:raise ValueError('Ambiguous immutable '+kind)
        if hits:pipeline.artifact(api,hits[0]['id'],name,run,c['workflowCommit'],hits[0]['digest'].removeprefix('sha256:'))
        values[kind+'-id']=str(hits[0]['id']) if hits else ''
    with open(os.environ['GITHUB_OUTPUT'],'a') as out:
        for k,v in values.items():out.write(k+'='+v+'\n')
    print(json.dumps(values))

def verify():
    if os.environ.get('ANDROID_SIGNING_CERT_SHA256','').replace(':','').strip().upper()!=CERT:raise ValueError('Original independent signer fingerprint differs')
    original.gate=gate;original.verify();c,_=load();info=json.loads(Path('dist/build-info.json').read_text())
    out=Path('build/reports/p05-release');out.mkdir(parents=True,exist_ok=True)
    shutil.copyfile('build/outputs/apk/reproducible/release/output-metadata.json',out/'output-metadata.json')
    (out/'manifest.xml').write_text(subprocess.check_output(['apkanalyzer','manifest','print','dist/'+APK],text=True))
    (out/'signature.txt').write_text(subprocess.check_output([str(Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0/apksigner'),'verify','--verbose','--print-certs','dist/'+APK],text=True))
    lines=[l for l in (Path(os.environ['RUNNER_TEMP'])/'p05-release-gradle.log').read_text().splitlines() if l.startswith('> Task :') or l.startswith('BUILD SUCCESSFUL')]
    if not any(':minifyReproducibleReleaseWithR8' in l for l in lines) or not any(':optimizeReproducibleReleaseResources' in l for l in lines):raise ValueError('Actual optimized Release tasks missing')
    (out/'optimization-tasks.txt').write_text('\n'.join(lines)+'\n')
    info.update(r8TaskExecuted=True,resourceOptimizationTaskExecuted=True,themePolicy='FOLLOW_SYSTEM',themeFamilies=['precision','quiet','explorer','neon'],themeSchemes=8,testRunId=c['approval']['testRunId'],buildRunId=os.environ['GITHUB_RUN_ID'],buildWorkflowCommit=c['workflowCommit'],mainTree=c['approval']['mainTree'],nativeVisualReview=c['approval'])
    Path('dist/build-info.json').write_text(json.dumps(info,indent=2)+'\n')

def finalize():
    plan,report=gate();info=json.loads(Path('dist/build-info.json').read_text())
    summary={'commit':info['commit'],'run':info['run'],'tests':info['tests'],'helpers':report['helpers'],'legacySkippedTest':report['legacySkippedTest'],'runtimeSmokeVerified':True,'upgradeFromVersionCode':6775,'crossUidShareReceipts':6,'wechat':'NEEDS_DEVICE_VALIDATION','hardware':'NEEDS_DEVICE_VALIDATION','bleProductionChanged':False}
    Path('dist/test-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    notes=f"[TEST][P0.5] {VERSION}\n\n四主题紧凑首页、主副指标准确交换并按主题保存；六分类设置中心；历史多选、标准多GPX/ZIP分享及二次确认批量删除。完全离线，复用原GPS/BLE/数据库/GPX路径。\n\n- Application ID: com.fox.runner\n- Version: {VERSION} / {info['versionCode']}\n- Source: {info['commit']}\n- APK SHA-256: {info['apkSha256']}\n- Certificate: {CERT}\n- Full test run: {info['testRunId']}\n- Tests: {json.dumps(info['tests'])}\n- 原有唯一Ignore单独计入Skipped，未计作通过；没有新增跳过。\n- P0.4固定签名覆盖升级、历史/GPS/心率/设置保留、GPX/ZIP与新进程恢复已在API35验证。\n- 实际原生截图与模拟数据/模拟接收回执不代表实体GNSS/BLE/耗电、TalkBack语音或微信客户端通过；这些保持NEEDS_DEVICE_VALIDATION。\n"
    Path('dist/RELEASE_NOTES.md').write_text(notes)
    Path('dist/DEVELOPMENT_COMMITS.txt').write_text(pipeline.git('log','--reverse','--format=%H %s',validation_base()+'..'+info['commit'])+'\n')
    with zipfile.ZipFile('dist/key-test-receipts.zip','w',zipfile.ZIP_DEFLATED) as z:
        for path in Path('gate').rglob('*'):
            if path.is_file() and (path.suffix in ('.json','.xml','.gpx') or path.name in ('read.txt','write.txt')):z.write(path,path.relative_to('gate'))
        for path in Path('build/reports').rglob('*'):
            if path.is_file() and path.suffix in ('.json','.xml','.gpx','.txt') and 'p0-release-smoke' in str(path):z.write(path,Path('signed-runtime')/path.relative_to('build/reports'))
    folder=Path('dist');names=[p.name for p in sorted(folder.iterdir()) if p.is_file() and p.name!='ARTIFACT-MANIFEST.json']
    (folder/'ARTIFACT-MANIFEST.json').write_text(json.dumps({n:hashlib.sha256((folder/n).read_bytes()).hexdigest() for n in names},indent=2)+'\n')

def validation_base():return 'f994d1ecd0e19a840800b2f946df3656a6b312c6'

def bundle(folder=Path('dist')):
    plan,report=gate();c,_=load();info=json.loads((folder/'build-info.json').read_text());runtime=json.loads((folder/'runtime-smoke.json').read_text());native=json.loads((folder/'native-theme-smoke.json').read_text())
    if info.get('plan')!=plan or info.get('commit')!=plan['commit'] or info.get('versionName')!=VERSION or info.get('versionCode')!=plan['versionCode'] or info.get('certificateSha256')!=CERT or info.get('applicationId')!='com.fox.runner' or info.get('variant')!='reproducibleRelease' or info.get('debuggable') is not False or info.get('mainTree')!=c['approval']['mainTree']:raise ValueError('Wrong formal APK identity/source/tree')
    if any(info.get(k) is not True for k in ('signatureVerified','alignmentVerified','runtimeSmokeVerified','r8TaskExecuted','resourceOptimizationTaskExecuted')):raise ValueError('Missing actual signing/optimization/runtime proof')
    expected={k:{f:report[k][f] for f in ('tests','executed','failures','errors','skipped')} for k in ('android','unit')}
    if info['tests']!=expected or info['testRunId']!=c['approval']['testRunId']:raise ValueError('Full test evidence differs')
    if runtime.get('passed') is not True or runtime.get('commit')!=info['commit'] or runtime.get('apkSha256')!=info['apkSha256'] or runtime.get('upgradeFromVersionCode')!=6775 or any(runtime.get(k) is not True for k in ('deviceConfigurationRetained','preparationCreatesNoWorkout','liveGpsFieldsVerified','releaseGpxSchemaVerified','forceStopRecovery','unitAndRateUpgradeRetained','themeUpgradeRetained','nativeRateSwitchKeepsRecording')):raise ValueError('Actual P0.4 upgrade/GPS/GPX/recovery proof absent')
    if native.get('passed') is not True or native.get('commit')!=info['commit'] or native.get('apkSha256')!=info['apkSha256'] or native.get('api')!=35 or native.get('nativePageChecks')!=pipeline.SIGNED_PNGS or len(native.get('schemes',[]))!=8 or len(native.get('screenshots',[]))!=pipeline.SIGNED_PNGS or any(native.get(k) is not True for k in ('processRestartPersistence','metricProcessPersistence','historyBatchVerified','sharePreservesData','appVersionVerified')):raise ValueError('Actual signed P0.5 native/metric/batch proof absent')
    rawzip=(folder/'theme-screenshots.zip').read_bytes()
    if hashlib.sha256(rawzip).hexdigest()!=native['screenshotsZipSha256']:raise ValueError('Signed screenshot ZIP changed')
    from p05_release_theme_ui import PAGE_IDS,PAGE_ACTIVITIES,page_matches
    import xml.etree.ElementTree as ET
    with zipfile.ZipFile(io.BytesIO(rawzip)) as z:
        for row in native['screenshots']:
            xml=z.read(row['nativeWindowFile']);png=z.read(row['file'])
            if hashlib.sha256(xml).hexdigest()!=row['nativeWindowSha256'] or not page_matches(ET.fromstring(xml),row['page']) or PAGE_ACTIVITIES[row['page']] not in row['focusedWindow'] or hashlib.sha256(png).hexdigest()!=row['sha256']:raise ValueError('Actual signed native focused page/XML/PNG changed')
    raw=(folder/APK).read_bytes()
    if len(list(folder.glob('*.apk')))!=1 or len(raw)!=info['apkBytes'] or hashlib.sha256(raw).hexdigest()!=info['apkSha256'] or (folder/'SHA256SUMS').read_text()!=f"{info['apkSha256']}  {APK}\n":raise ValueError('Accepted APK bytes changed')
    manifest=json.loads((folder/'ARTIFACT-MANIFEST.json').read_text())
    if set(manifest)!={p.name for p in folder.iterdir() if p.is_file() and p.name not in ('ARTIFACT-MANIFEST.json','publication.json')}:raise ValueError('Formal artifact payload set differs')
    for name,digest in manifest.items():
        if Path(name).name!=name or hashlib.sha256((folder/name).read_bytes()).hexdigest()!=digest:raise ValueError('Formal asset changed: '+name)
    if c['mode']=='publish':
        p=c['publication']
        if info['apkSha256']!=p['apkSha256'] or info['buildRunId']!=p['buildRunId'] or info['buildWorkflowCommit']!=p['buildWorkflowCommit']:raise ValueError('Visually reviewed signed bundle differs')
    verify_apk(raw,info);return info

if __name__=='__main__':{'context':context,'inputs':inputs,'gate':gate,'existing':existing,'verify':verify,'finalize':finalize,'bundle':bundle}[sys.argv[1]]()
