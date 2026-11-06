"""Recover the original accepted signed bundle with raw reports; never rebuild or re-sign."""
import io
import json
import os
from pathlib import Path
import subprocess
import zipfile
from publish_release import GitHub,digest,verify_apk
from p02_publication_scope import BUILD,RUN,FORMAL,REPORTS,FORMAL_ZIP,REPORTS_ZIP,APK_SHA,context
from p02_version import APK,CERT,VERSION
from check_test_results import inspect
from collect_p02_home_ui import inspect as inspect_ui
from verify_p0_release import validate_gate
from verify_p0_offline import inspect as offline
import xml.etree.ElementTree as ET
import p02_pipeline
import p02_publish


def download(api,identifier,expected):
    meta=api.request(api.base+f'/actions/artifacts/{identifier}')
    if meta['expired'] or meta['workflow_run']['id']!=RUN or meta['workflow_run']['head_sha']!=BUILD or meta['digest']!='sha256:'+expected:raise ValueError('Accepted immutable Artifact unavailable/changed')
    raw=subprocess.check_output(['gh','api',f"repos/{os.environ['GITHUB_REPOSITORY']}/actions/artifacts/{identifier}/zip"])
    if digest(raw)!=expected:raise ValueError('Original Artifact ZIP checksum mismatch')
    return zipfile.ZipFile(io.BytesIO(raw))


def main():
    if not context():raise ValueError('Explicit unchanged-APK recovery scope required')
    p02_pipeline.protected_refs();api=GitHub()
    run=api.request(api.base+f'/actions/runs/{RUN}')
    if run['head_sha']!=BUILD or run['head_branch']!='feature/p0-2-gps-ui':raise ValueError('Original source/run differs')
    jobs=api.request(api.base+f'/actions/runs/{RUN}/jobs?per_page=100&filter=all')['jobs']
    for name in ('checks','Optimized fixed-key P0.2 Release and P0.1 upgrade'):
        if not any(j['name']==name and j['conclusion']=='success' for j in jobs):raise ValueError('Original test/sign/install job did not pass')
    # The original run failed only in publication. Never turn that historical result green by fiction.
    with download(api,REPORTS,REPORTS_ZIP) as archive:
        root=Path('gate').resolve();root.mkdir()
        for name in archive.namelist():
            path=(root/name).resolve()
            if not path.is_relative_to(root):raise ValueError('Unsafe original Artifact member')
            if name.endswith('/'):continue
            path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(archive.read(name))
    with download(api,FORMAL,FORMAL_ZIP) as archive:
        if set(archive.namelist())!={APK,'build-info.json','SHA256SUMS','runtime-smoke.json'}:raise ValueError('Unexpected accepted bundle contents')
        folder=Path('dist');folder.mkdir()
        for name in archive.namelist():(folder/name).write_bytes(archive.read(name))
    info=json.loads((folder/'build-info.json').read_text());runtime=json.loads((folder/'runtime-smoke.json').read_text())
    plan=json.loads(Path('gate/release-plan.json').read_text());report=json.loads(Path('gate/build/reports/p0-validation/tests.json').read_text())
    config=json.loads(subprocess.check_output(['git','show',BUILD+':.github/p02/stage.json'],text=True))
    validate_gate(plan,report,config,BUILD)
    if str(plan['runId'])!=str(RUN) or info['plan']!=plan:raise ValueError('Original tested plan differs')
    for scope,where,source,require_environment in [('android','gate/build/outputs/androidTest-results','src/androidTest/java',True),('unit','gate/build/test-results/testReproducibleDebugUnitTest','src/test/java',False)]:
        actual=inspect(Path(where),Path(source),require_environment=require_environment)
        if not actual['passed'] or any(actual[k]!=report[scope][k] for k in ('tests','executed','failures','errors','skipped')):raise ValueError('Original actual XML differs from accepted gate')
    inspect_ui(Path('gate/build/reports/p0-ui'),BUILD)
    if (info['commit']!=BUILD or info['versionName']!=VERSION or info['versionCode']!=6773 or info['certificateSha256']!=CERT
            or info['apkSha256']!=APK_SHA or info['apkBytes']!=2753061 or info['debuggable'] is not False
            or any(info.get(k) is not True for k in ('signatureVerified','alignmentVerified','runtimeSmokeVerified','r8TaskExecuted','resourceOptimizationTaskExecuted'))):raise ValueError('Wrong/unaccepted formal APK')
    if (runtime.get('passed') is not True or runtime['commit']!=BUILD or runtime['apkSha256']!=APK_SHA or runtime['upgradeFromVersionCode']!=6772
            or runtime.get('deviceConfigurationRetained') is not True or runtime.get('preparationCreatesNoWorkout') is not True):raise ValueError('Actual original P0.1 upgrade proof missing')
    raw=(folder/APK).read_bytes()
    if digest(raw)!=APK_SHA or len(raw)!=2753061 or (folder/'SHA256SUMS').read_text()!=APK_SHA+'  '+APK+'\n':raise ValueError('Original APK bytes/checksum changed')
    verify_apk(raw,info)
    tools=Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0'
    subprocess.run([str(tools/'zipalign'),'-c','-P','16','4',str(folder/APK)],check=True)
    offline(ET.fromstring(subprocess.check_output(['apkanalyzer','manifest','print',str(folder/APK)],text=True)),True)
    # Substitute only after independently validating the pinned original bytes, real XML,
    # same-source UI/storage/GPX evidence, public certificate and actual installed-upgrade proof.
    def accepted_bundle(requested):
        if requested.resolve()!=folder.resolve() or digest((requested/APK).read_bytes())!=APK_SHA:raise ValueError('Recovery bundle changed')
        return info
    p02_publish.bundle=accepted_bundle
    p02_publish.publish(api,folder)
    result={'acceptedBuildCommit':BUILD,'acceptedRun':RUN,'formalArtifact':FORMAL,'reportArtifact':REPORTS,
            'publicationCodeCommit':os.environ['GITHUB_SHA'],'publicationRun':os.environ['GITHUB_RUN_ID'],
            'apkSha256':APK_SHA,'noBuildOrSigning':True,'originalRunConclusion':run['conclusion']}
    (folder/'recovery.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))


if __name__=='__main__':main()
