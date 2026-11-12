"""Read one retained failed synthetic signing report; never promote or sign it."""
import base64,hashlib,io,json,os,zipfile
from pathlib import Path
from publish_release import GitHub
RUN=43229996107
ARTIFACT=13951083715
WORKFLOW='2c246f8ef8f07d2a85f5913bc0636d2d0f055f41'
SOURCE='7c27ab2e304a66761d871aeedeed5409627b4677'
DIGEST='82f649baa4ff8eec7c7e23fa0ac84f7fd58afc68578a257e330beee5f56d529c'

def run():
    if os.environ['GITHUB_EVENT_NAME']!='push' or os.environ['GITHUB_REF']!='refs/heads/feature/p0-6-offline-map':raise ValueError('Trusted P06 diagnostic push required')
    api=GitHub();failed=api.request(api.base+f'/actions/runs/{RUN}');meta=api.request(api.base+f'/actions/artifacts/{ARTIFACT}')
    if failed['conclusion']!='failure' or failed['head_sha']!=WORKFLOW or meta['workflow_run']['id']!=RUN or meta['digest']!='sha256:'+DIGEST or meta['expired']:raise ValueError('Retained failed report identity differs')
    raw=api.request(meta['archive_download_url'],binary=True)
    if hashlib.sha256(raw).hexdigest()!=DIGEST:raise ValueError('Retained diagnostic archive digest differs')
    payload=io.BytesIO();names=[]
    with zipfile.ZipFile(io.BytesIO(raw)) as z,zipfile.ZipFile(payload,'w',zipfile.ZIP_DEFLATED) as out:
        for name in z.namelist():
            if Path(name).is_absolute() or '..' in Path(name).parts or Path(name).suffix.lower() in ('.apk','.jks','.keystore'):raise ValueError('Unsafe diagnostic payload')
            leaf=Path(name).name
            if leaf in ('last-native-screen.png','last-native-window.xml','last-native-focus.txt','logcat.txt') or leaf.startswith(('p06-signed-map-','p06-release-neon-light-home','p06-release-neon-light-offlineMap')):
                out.writestr(name,z.read(name));names.append(name)
        listing=sorted(n for n in z.namelist() if '/p06-release-' in n and n.endswith('.png'));print('Actually captured signed native files: '+json.dumps(listing),flush=True)
        out.writestr('retained-diagnostic-identity.json',json.dumps({'failedRun':RUN,'artifactId':ARTIFACT,'artifactSha256':DIGEST,'workflowCommit':WORKFLOW,'source':SOURCE,'accepted':False,'files':names},indent=2)+'\n')
    if not any(Path(n).name=='last-native-window.xml' for n in names):raise ValueError('Actual failure window missing')
    data=payload.getvalue();chunks=[]
    for offset in range(0,len(data),512*1024):
        part=data[offset:offset+512*1024];blob=api.request(api.base+'/git/blobs','POST',{'encoding':'base64','content':base64.b64encode(part).decode()});chunks.append({'blob':blob['sha'],'bytes':len(part),'sha256':hashlib.sha256(part).hexdigest()})
    index={'source':SOURCE,'runId':os.environ['GITHUB_RUN_ID'],'workflowCommit':os.environ['GITHUB_SHA'],'evidencePassed':False,'signedReview':False,'diagnosticOnly':True,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),'chunks':chunks,'originalArtifactPreserved':True,'gitRefUpdated':False,'signingKeyMaterialIncluded':False}
    blob=api.request(api.base+'/git/blobs','POST',{'encoding':'utf-8','content':json.dumps(index,indent=2)+'\n'})
    Path('retained-diagnostic-transfer.json').write_text(json.dumps({**index,'indexBlob':blob['sha']},indent=2)+'\n')
    with open(os.environ['GITHUB_OUTPUT'],'a') as out:out.write('index='+blob['sha']+'\n')
    print(json.dumps(index,indent=2))
if __name__=='__main__':run()
