"""Upload the immutable accepted P0 Artifact to the one existing Draft. No build, key or ref writes."""
import hashlib
import io
import json
import os
from pathlib import Path
import subprocess
from urllib.parse import quote
import zipfile
from publish_release import GitHub
from p0_publication_scope import BUILD,publication_only

REPO='foxmaybeOI1761640545/Runner_Gardenia-06'
TAG='test-p0-v0.1.0-p0.1'
RELEASE=434907895
RUN=41974364095
ARTIFACT=13391972069
APK='RunnerGardenia-P0-Release.apk'
SHA='94d3322310e8b6050504eb4c8d8c72fa09f0190f8add35ae0100b9bc93cc6ab0'
CERT='E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22'
DOCS='e8973de980398f9f64f72eae510a789b86338356'


def digest(data):return hashlib.sha256(data).hexdigest()


def main():
    if os.environ['GITHUB_REPOSITORY']!=REPO or os.environ['GITHUB_REF']!='refs/heads/feature/p0-core-migration' or os.environ['GITHUB_EVENT_NAME']!='push':raise ValueError('Untrusted publication context')
    event=json.loads(Path(os.environ['GITHUB_EVENT_PATH']).read_text())
    paths=subprocess.check_output(['git','diff','--name-only',BUILD,'HEAD'],text=True).splitlines()
    if not publication_only(event['head_commit']['message'],paths):raise ValueError('Publication-only scope is required')
    if subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()!=os.environ['GITHUB_SHA']:raise ValueError('Wrong checkout')
    api=GitHub();base=api.base
    run=api.request(base+f'/actions/runs/{RUN}')
    if run['conclusion']!='success' or run['head_sha']!=BUILD or run['head_branch']!='feature/p0-core-migration':raise ValueError('Accepted build run changed/failed')
    artifact=api.request(base+f'/actions/artifacts/{ARTIFACT}')
    if artifact['expired'] or artifact['workflow_run']['id']!=RUN or artifact['workflow_run']['head_sha']!=BUILD:raise ValueError('Original Artifact unavailable')
    ref=api.request(base+'/git/ref/tags/'+TAG)
    if ref['object']['type']!='tag':raise ValueError('An annotated tag is required')
    tag=api.request(base+'/git/tags/'+ref['object']['sha'])
    if tag['object']['type']!='commit' or tag['object']['sha']!=BUILD:raise ValueError('Tag targets another commit')
    # The Artifact redirect endpoint requires the GitHub API media type, unlike
    # Release asset downloads. gh follows its signed CDN redirect without exposing it.
    artifact_zip=subprocess.check_output(['gh','api',f'repos/{REPO}/actions/artifacts/{ARTIFACT}/zip'])
    with zipfile.ZipFile(io.BytesIO(artifact_zip)) as archive:
        if set(archive.namelist())!={APK,'SHA256SUMS','build-info.json','runtime-smoke.json'}:raise ValueError('Unexpected Artifact contents')
        files={name:archive.read(name) for name in archive.namelist()}
    if len(files[APK])!=2715993 or digest(files[APK])!=SHA or files['SHA256SUMS']!=(SHA+'  '+APK+'\n').encode():raise ValueError('APK changed; never substitute/rebuild')
    info=json.loads(files['build-info.json']);runtime=json.loads(files['runtime-smoke.json'])
    if (info['commit']!=BUILD or info['apkSha256']!=SHA or info['applicationId']!='com.fox.runner' or info['versionName']!='v0.1.0-p0.1' or info['versionCode']!=6772
            or info['certificateSha256']!=CERT or info['debuggable'] is not False or info['signatureVerified'] is not True
            or runtime['passed'] is not True or runtime['commit']!=BUILD or runtime['apkSha256']!=SHA):raise ValueError('Accepted identity/signature/runtime evidence mismatch')
    files['SHA256SUMS.txt']=files.pop('SHA256SUMS')
    for name in ('P0_DELIVERY_REPORT.md','P0_DEVICE_CHECKLIST.md'):files[name]=subprocess.check_output(['git','show',DOCS+':docs/'+name])
    expected={name:{'bytes':len(data),'sha256':digest(data)} for name,data in files.items()}
    def release():
        current=api.request(base+f'/releases/{RELEASE}')
        if current['tag_name']!=TAG or current['target_commitish']!=BUILD or not current['prerelease'] or not current['draft']:raise ValueError('Only the verified existing test Draft can receive assets')
        if any(asset['name'] not in files for asset in current['assets']):raise ValueError('Unexpected existing Draft asset; do not delete/replace it')
        return current
    current=release()
    for name,data in files.items():
        existing=[asset for asset in current['assets'] if asset['name']==name]
        if len(existing)>1:raise ValueError('Ambiguous existing asset')
        if existing:
            if digest(api.request(existing[0]['url'],binary=True))!=expected[name]['sha256']:raise ValueError('Existing asset differs; never overwrite')
        else:
            uploaded=api.request(current['upload_url'].split('{')[0]+'?name='+quote(name,safe=''),method='POST',data=data)
            if uploaded['name']!=name:raise ValueError('Upload did not return expected asset')
    current=release()
    if {asset['name'] for asset in current['assets']}!=set(files):raise ValueError('Missing assets')
    for asset in current['assets']:
        item=expected[asset['name']]
        if asset['state']!='uploaded' or asset['size']!=item['bytes'] or (asset.get('digest') and asset['digest']!='sha256:'+item['sha256']):raise ValueError('Asset metadata mismatch')
        if digest(api.request(asset['url'],binary=True))!=item['sha256']:raise ValueError('Downloaded Draft asset hash mismatch')
    out=Path('build/reports/p0-publication');out.mkdir(parents=True,exist_ok=True)
    proof={'draftVerified':True,'published':False,'tag':TAG,'buildCommit':BUILD,'originalRun':RUN,'originalArtifact':ARTIFACT,'apkSha256':SHA,
           'publicationRun':os.environ['GITHUB_RUN_ID'],'releaseId':RELEASE,'assets':expected,'noBuildOrSigning':True}
    (out/'result.json').write_text(json.dumps(proof,indent=2)+'\n');print(json.dumps(proof,indent=2))
    with open(os.environ['GITHUB_STEP_SUMMARY'],'a') as summary:summary.write('Verified original P0 Artifact uploaded and downloaded again. Draft remains unpublished for final external verification. No Gradle, APK build or signing operation.\n')


if __name__=='__main__':main()
