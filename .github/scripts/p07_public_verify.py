"""Independent anonymous public APK download on a fresh runner; no signing environment."""
import hashlib,json,os,urllib.request
from pathlib import Path
from p07_version import CERT,VERSION,REPO,TAG,APK
from publish_release import verify_apk

if __name__=='__main__':
    info=json.loads(Path('receipt/build-info.json').read_text());receipt=json.loads(Path('receipt/publication.json').read_text())
    if info['certificateSha256']!=CERT or info['versionName']!=VERSION or info['applicationId']!='com.fox.runner' or receipt['tag']!=TAG or receipt['apkSha256']!=info['apkSha256']:raise ValueError('Wrong public release identity')
    url=receipt['publicDownloadUrl']
    if not url.startswith('https://api.github.com/repos/'+REPO+'/releases/assets/'):raise ValueError('Unexpected public asset destination')
    with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'Runner-Gardenia-independent-public-check','Accept':'application/octet-stream'}),timeout=120) as response:
        raw=response.read()
        if response.status!=200:raise ValueError('Anonymous public APK unavailable')
    if len(raw)!=info['apkBytes'] or hashlib.sha256(raw).hexdigest()!=info['apkSha256']:raise ValueError('Independent public APK differs')
    verify_apk(raw,info)
    out=Path('public-verification');out.mkdir()
    (out/'receipt.json').write_text(json.dumps({'passed':True,'independentRunner':True,'authenticatedDownload':False,'signingKeyUsed':False,'runId':os.environ['GITHUB_RUN_ID'],'source':info['commit'],'release':receipt['release'],'versionName':VERSION,'versionCode':info['versionCode'],'applicationId':'com.fox.runner','apkBytes':len(raw),'apkSha256':info['apkSha256'],'certificateSha256':CERT},indent=2)+'\n')
