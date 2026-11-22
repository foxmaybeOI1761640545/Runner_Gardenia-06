"""Additional P0.7 release gates; old regressions and their assertions remain intact."""
import hashlib,json,os
from pathlib import Path

REPORTS=Path('build/reports')
def collect():
    source=os.environ['GITHUB_SHA']
    import collect_p04_ui,collect_p05_ui,collect_p05_history,collect_p02_home_ui,collect_heart_rate_ui
    from p061_pipeline import gps_evidence
    from p06_map_evidence import collect as maps
    from p061_real_map import collect as real
    collect_p04_ui.collect(source);collect_p05_ui.collect(source);collect_p05_ui.collect_metrics(source);collect_p05_ui.collect_settings(source)
    collect_p05_history.collect(source);collect_p02_home_ui.collect(source);collect_heart_rate_ui.collect()
    gps_evidence(source);maps(source);real()

def gate(source,root=Path('.')):
    reports=root/REPORTS;checks={}
    for name in ('p0-durability','p061-real-durability','p06-map-durability','p0-gpx','p0-offline','native-window-helper'):
        value=json.loads((reports/name/'result.json').read_text())
        if value.get('passed') is not True or value.get('commit')!=source:raise ValueError('Missing exact-source delivery gate: '+name)
        checks[name]=value
    for name in ('p0-durability','p061-real-durability','p06-map-durability'):
        value=checks[name]
        if value.get('forceStopped') is not True or value['phases']['write']['pid']==value['phases']['read']['pid']:raise ValueError('Durability requires actual new processes: '+name)
        for phase in value['phases'].values():
            if any(phase.get(k)!=v for k,v in {'tests':1,'executed':1,'failures':0,'errors':0,'skipped':0}.items()):raise ValueError('Incomplete process execution')
    if not checks['p0-durability'].get('p05MetricProbe') or not checks['p0-durability'].get('p05BatchProbe'):raise ValueError('Metric/batch process probes missing')
    for name in ('p061-real-durability','p06-map-durability'):
        if not checks[name].get('airplaneMode') or not checks[name].get('privateMapRetained') or not checks[name].get('nativeTilesRenderedAfterRestart'):raise ValueError('Offline real-file rendering/restart missing')
    if not checks['p06-map-durability'].get('pendingImportCleaned'):raise ValueError('Interrupted import cleanup missing')
    matrices=[('p04-visual','p04-visual-matrix.json',192),('p05-visual','p05-layout-matrix.json',48),('p05-metrics','metric-matrix.json',8),('p05-settings','settings-matrix.json',16),('p05-history','history-matrix.json',32),('p051-gps-ui','p051-gps-ui.json',12),('p06-offline-map','p06-offline-map.json',16),('p061-real-map','result.json',53)]
    for folder,name,count in matrices:
        value=json.loads((reports/folder/name).read_text())
        if not value.get('passed') or value.get('sourceCommit')!=source or len(value.get('screenshots',[]))!=count:raise ValueError('Native matrix incomplete: '+folder)
        for row in value['screenshots']:
            if hashlib.sha256((reports/folder/row['file']).read_bytes()).hexdigest()!=row['sha256']:raise ValueError('Native screenshot digest differs')
        checks[folder]={'passed':True,'source':source,'screenshots':count}
    debug=json.loads((reports/'ci-validation/debug-apk.json').read_text())
    from p07_version import VERSION,allocation
    if debug.get('applicationId')!='com.fox.runner.debug' or debug.get('versionName')!=VERSION or debug.get('versionCode')!=allocation()['versionCode']:raise ValueError('Debug identity differs')
    shares=json.loads((reports/'p05-share/result.json').read_text())
    if not shares.get('passed') or shares.get('commit')!=source or len(shares.get('receipts',[]))!=6:raise ValueError('Actual cross-UID shares missing')
    rows=[]
    native_folders={'p07-focused','p07-permissions','p04-visual','p05-visual','p05-metrics','p05-settings','p05-history','p051-gps-ui','p06-offline-map','p061-real-map','p0-ui','heart-rate-ui','p061-real-durability','p06-map-durability','native-window-helper'}
    for path in sorted(reports.rglob('*.png')):
        if path.relative_to(reports).parts[0] not in native_folders:continue
        data=path.read_bytes()
        if not data.startswith(b'\x89PNG\r\n\x1a\n'):raise ValueError('Invalid native PNG')
        rows.append({'file':str(path.relative_to(reports)),'sha256':hashlib.sha256(data).hexdigest()})
    if len(rows)<414:raise ValueError('Incomplete native visual evidence')
    return {'passed':True,'source':source,'checks':checks,'debug':debug,'screenshots':rows,'humanVisualReviewRequired':True}

if __name__=='__main__':
    import sys
    if sys.argv[1:] == ['collect']:collect()
    else:
        value=gate(os.environ['GITHUB_SHA']);(REPORTS/'p07-stage/delivery.json').write_text(json.dumps(value,indent=2)+'\n')
