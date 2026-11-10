"""Independent read-only public roundtrip. Requires accepted formal bundle on disk."""
import json,hashlib,urllib.request,urllib.error,importlib.util,sys
from pathlib import Path
repo='foxmaybeOI1761640545/Runner_Gardenia-06';base='https://api.github.com/repos/'+repo;tag='test-p0-v0.1.0-p0.5';source='a8891f61122712b81bee357f40d688151a948e57';run='42761772173'
root=Path('work/phase5-formal-'+run);dist=root/'dist';out=Path('/workspace/output/RunnerGardenia-P05');out.mkdir(parents=True,exist_ok=True)
def get(url,binary=False):
 req=urllib.request.Request(url,headers={'User-Agent':'Runner-Gardenia-independent-public-review','Accept':'application/octet-stream' if binary else 'application/vnd.github+json'})
 with urllib.request.urlopen(req,timeout=120) as res:
  assert res.status==200;raw=res.read()
 return raw if binary else json.loads(raw)
release=get(base+'/releases/tags/'+tag);assert not release['draft'] and release['prerelease'] and release['target_commitish']==source and release['name']=='[TEST][P0.5] v0.1.0-p0.5'
try:latest=get(base+'/releases/latest')
except urllib.error.HTTPError as e:
 assert e.code==404;latest=None
assert latest is None or latest['id']!=release['id']
ref=get(base+'/git/ref/tags/'+tag);assert ref['object']['type']=='tag';annotated=get(base+'/git/tags/'+ref['object']['sha']);assert annotated['object']=={'sha':source,'type':'commit','url':base+'/git/commits/'+source}
assets=get(base+f"/releases/{release['id']}/assets?per_page=100");assert {a['name'] for a in assets}=={p.name for p in dist.iterdir() if p.is_file()};proof={}
for a in assets:
 raw=get(a['url'],True);original=(dist/a['name']).read_bytes();assert raw==original and a['size']==len(raw);digest=hashlib.sha256(raw).hexdigest();assert a['digest']=='sha256:'+digest;(out/a['name']).write_bytes(raw);proof[a['name']]={'id':a['id'],'bytes':len(raw),'sha256':digest,'url':a['browser_download_url']}
spec=importlib.util.spec_from_file_location('independent','docs/theme-system/acceptance/verify-public-apk.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);apk=m.verify(out/'RunnerGardenia-P0-Release.apk','v0.1.0-p0.5',6776);assert apk['sha256']=='7b0d0f48e2de5088f73ecd9272747beea4d79281d9dd8bb630cad821b1767598'
p04=get(base+'/releases/tags/test-p0-v0.1.0-p0.4');old=next(a for a in p04['assets'] if a['name']=='RunnerGardenia-P0-Release.apk');assert old['id']==724746708 and old['size']==2803864 and old['digest']=='sha256:aa5ebc21c76cbca492edc66453b91d7fdecaf82bd5a69fcab208c740b4224ebc'
oldref=get(base+'/git/ref/tags/test-p0-v0.1.0-p0.4');assert oldref['object']['sha']=='f3900d5dadd09d3b8132bd2951d674692b2a6011';oldtag=get(base+'/git/tags/'+oldref['object']['sha']);assert oldtag['object']['sha']=='ccc59b546b5087a2038d32087cc675a3096852c3'
result={'passed':True,'releaseId':release['id'],'release':release['html_url'],'tagObject':ref['object']['sha'],'tagSource':source,'prerelease':True,'latest':False,'apk':apk,'assets':proof,'p04ImmutableVerified':True,'wechat':'NEEDS_DEVICE_VALIDATION'}
(out/'independent-public-verification.json').write_text(json.dumps(result,indent=2)+'\n');(root/'independent-public-verification.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
