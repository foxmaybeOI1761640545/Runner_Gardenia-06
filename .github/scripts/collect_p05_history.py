"""Require exact-source native history pixels and authenticated cross-UID file receipts."""
import hashlib,json,os,shutil,struct
from pathlib import Path
FAMILIES=('precision','quiet','explorer','neon')
PAGES=('list','selection','share','delete')


def validate_receipt(source,kind,negative,r):
    if (r.get('sourceCommit')!=source or r.get('api')!=35 or r.get('permissionContractPassed') is not True
        or r['uid']<=0 or r['targetUid']<=0 or r['uid']==r['targetUid'] or r['uid']!=r['authenticatedBinderUid']
        or r['pid']<=0 or r['pid']==r['targetPid'] or r.get('negativeControl') is not negative):raise ValueError('Receiver source/API/UID contract missing')
    expected_action='de.dennisguse.opentracks.p0.TEST_WITHOUT_GRANT' if negative else ('android.intent.action.SEND_MULTIPLE' if kind=='multiple' else 'android.intent.action.SEND')
    expected_mime='application/zip' if kind=='zip' else 'application/gpx+xml'
    if r.get('action')!=expected_action or r.get('mime')!=expected_mime:raise ValueError('Actual receiving action or MIME differs')
    files=r['files'];count=3 if kind=='multiple' else 1
    if len(files)!=count or len({f['uri'] for f in files})!=count or r['clip']!=([] if negative else [f['uri'] for f in files]):raise ValueError('Receiver silently omitted/duplicated selected files')
    actual={}
    for file in files:
        if not file['uri'].startswith('content://') or file.get('read') is not (not negative):raise ValueError('Private path or incorrect URI read result')
        if negative:
            if file.get('errorClass')!='java.lang.SecurityException' or r['flags']&(1|2|64|128):raise ValueError('Negative control did not actually deny permission')
        else:
            if file['permission']!=0 or r['flags']&(1|2|64|128)!=1 or len(file['sha256'])!=64 or file['bytes']<=0:raise ValueError('Read-only URI/content evidence incomplete')
            entries=file['entries'] if kind=='zip' else [file['gpx']]
            if len(entries)!=(3 if kind=='zip' else 1):raise ValueError('GPX/ZIP lost a selected workout')
            for entry in entries:
                identity=str(entry['workoutId'])
                if identity in actual or entry['rawSamples']!=3:raise ValueError('GPX/ZIP duplicated a workout or lost raw heart-rate samples')
                actual[identity]=entry['sha256']
    if not negative:
        if (r.get('contentContractPassed') is not True or set(actual)!={str(v) for v in r['expectedWorkoutIds']} or actual!=r['expectedGpxSha256']):raise ValueError('Receiver bytes differ from every selected original workout')
    return r


def collect(source):
    roots=list(Path('build/outputs').glob('**/p0-ui'))
    if len(roots)!=1:raise ValueError('Native history screenshot output missing/ambiguous')
    root=roots[0];out=Path('build/reports/p05-history');out.mkdir(parents=True,exist_ok=True);rows=[]
    for family in FAMILIES:
        for mode in ('light','dark'):
            for page in PAGES:
                name=f'p05-history-{family}-{mode}-{page}-native.png';raw=(root/name).read_bytes()
                if raw[:8]!=b'\x89PNG\r\n\x1a\n' or struct.unpack('>II',raw[16:24])!=(720,1600):raise ValueError('Wrong actual history viewport')
                shutil.copy2(root/name,out/name)
                rows.append({'file':name,'family':family,'appearance':mode,'page':page,'widthDp':360,'fontScale':1.3,'sha256':hashlib.sha256(raw).hexdigest(),'syntheticWorkoutInputs':True})
    receipts=list(Path('build/outputs').glob('**/p05-share'))
    if len(receipts)!=1:raise ValueError('Actual distinct-UID receiver receipts missing/ambiguous')
    destination=Path('build/reports/p05-share');destination.mkdir(parents=True,exist_ok=True);proofs=[]
    for kind in ('single','multiple','zip'):
        for negative in (True,False):
            name=kind+('-denied' if negative else '-received')+'.json';r=json.loads((receipts[0]/name).read_text())
            validate_receipt(source,kind,negative,r)
            shutil.copy2(receipts[0]/name,destination/name);proofs.append(r)
    result={'passed':True,'sourceCommit':source,'api':35,'native':True,'screenshots':rows,'receiverReceipts':len(proofs),'wechat':'NEEDS_DEVICE_VALIDATION'}
    (out/'history-matrix.json').write_text(json.dumps(result,indent=2)+'\n')
    (destination/'result.json').write_text(json.dumps({'passed':True,'commit':source,'receipts':proofs,'wechat':'NEEDS_DEVICE_VALIDATION'},indent=2)+'\n')
    print('PASS: 32 actual native history screenshots and six authenticated distinct-UID receiver receipts')
    return result

if __name__=='__main__':collect(os.environ['GITHUB_SHA'])
