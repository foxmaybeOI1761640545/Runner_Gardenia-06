"""Read-only independent APK v2 signature/content digest/certificate/alignment check; no SDK or signing key."""
import argparse,hashlib,json,struct,zipfile
from pathlib import Path
from cryptography import x509
from cryptography.hazmat.primitives import hashes,serialization
from cryptography.hazmat.primitives.asymmetric import padding,ec,dsa,rsa
p=argparse.ArgumentParser();p.add_argument('apk');p.add_argument('--certificate',required=True);p.add_argument('--sha256',required=True);p.add_argument('--out',required=True);a=p.parse_args();apk=Path(a.apk);raw=apk.read_bytes();assert hashlib.sha256(raw).hexdigest()==a.sha256
u32=lambda b:struct.unpack('<I',b)[0]
u64=lambda b:struct.unpack('<Q',b)[0]
def lp(b,pos=0):
 assert pos+4<=len(b);size=u32(b[pos:pos+4]);end=pos+4+size;assert end<=len(b);return b[pos+4:end],end
def sequence(b):
 rows=[];pos=0
 while pos<len(b):item,pos=lp(b,pos);rows.append(item)
 assert pos==len(b);return rows
# EOCD comment must end at the actual file boundary.
poss=[i for i in range(max(0,len(raw)-65557),len(raw)-21) if raw[i:i+4]==b'PK\x05\x06' and i+22+struct.unpack('<H',raw[i+20:i+22])[0]==len(raw)]
assert len(poss)==1;eocd=poss[0];cd=u32(raw[eocd+16:eocd+20]);assert cd+u32(raw[eocd+12:eocd+16])==eocd
assert raw[cd-16:cd]==b'APK Sig Block 42';size=u64(raw[cd-24:cd-16]);start=cd-size-8;assert start>=0 and u64(raw[start:start+8])==size
pairs={};pos=start+8
while pos<cd-24:
 length=u64(raw[pos:pos+8]);pos+=8;assert length>=4 and pos+length<=cd-24;identifier=u32(raw[pos:pos+4]);assert identifier not in pairs;pairs[identifier]=raw[pos+4:pos+length];pos+=length
assert pos==cd-24 and 0x7109871a in pairs
signers,end=lp(pairs[0x7109871a]);assert end==len(pairs[0x7109871a]);signers=sequence(signers);assert len(signers)==1
signer=signers[0];signed,pos=lp(signer);signatures,pos=lp(signer,pos);public,pos=lp(signer,pos);assert pos==len(signer)
digests,pos=lp(signed);certificates,pos=lp(signed,pos);attributes,pos=lp(signed,pos);assert signed[pos:] in (b'',b'\0\0\0\0')
certs=sequence(certificates);assert certs;cert=x509.load_der_x509_certificate(certs[0]);fingerprint=cert.fingerprint(hashes.SHA256()).hex().upper();assert fingerprint==a.certificate.upper()
key=cert.public_key();assert key.public_bytes(serialization.Encoding.DER,serialization.PublicFormat.SubjectPublicKeyInfo)==public
signature_records={}
for row in sequence(signatures):
 algorithm=u32(row[:4]);signature,end=lp(row,4);assert end==len(row) and algorithm not in signature_records;signature_records[algorithm]=signature
supported=[n for n in (0x0104,0x0103,0x0102,0x0101,0x0202,0x0201,0x0301) if n in signature_records];assert supported;algorithm=supported[0];signature=signature_records[algorithm];hash_algorithm=hashes.SHA512() if algorithm in (0x0104,0x0102,0x0202) else hashes.SHA256();hash_function=hashlib.sha512 if hash_algorithm.digest_size==64 else hashlib.sha256
if algorithm in (0x0103,0x0104):assert isinstance(key,rsa.RSAPublicKey);key.verify(signature,signed,padding.PKCS1v15(),hash_algorithm)
elif algorithm in (0x0101,0x0102):assert isinstance(key,rsa.RSAPublicKey);key.verify(signature,signed,padding.PSS(mgf=padding.MGF1(hash_algorithm),salt_length=hash_algorithm.digest_size),hash_algorithm)
elif algorithm in (0x0201,0x0202):assert isinstance(key,ec.EllipticCurvePublicKey);key.verify(signature,signed,ec.ECDSA(hash_algorithm))
else:assert isinstance(key,dsa.DSAPublicKey);key.verify(signature,signed,hash_algorithm)
expected={}
for row in sequence(digests):
 identifier=u32(row[:4]);digest,end=lp(row,4);assert end==len(row) and identifier not in expected;expected[identifier]=digest
assert set(expected)==set(signature_records) and len(expected[algorithm])==hash_algorithm.digest_size
end_record=bytearray(raw[eocd:]);end_record[16:20]=struct.pack('<I',start);chunks=[]
for section in (raw[:start],raw[cd:eocd],end_record):
 for offset in range(0,len(section),1024*1024):
  chunk=section[offset:offset+1024*1024];chunks.append(hash_function(b'\xa5'+struct.pack('<I',len(chunk))+chunk).digest())
actual=hash_function(b'\x5a'+struct.pack('<I',len(chunks))+b''.join(chunks)).digest();assert actual==expected[algorithm]
unaligned=[];stored=0
with zipfile.ZipFile(apk) as z:
 assert z.testzip() is None
 for entry in z.infolist():
  if entry.compress_type!=zipfile.ZIP_STORED:continue
  stored+=1;offset=entry.header_offset;name_len,extra_len=struct.unpack('<HH',raw[offset+26:offset+30]);data=offset+30+name_len+extra_len
  if data%4:unaligned.append(entry.filename)
assert not unaligned
result={'passed':True,'apkSha256':a.sha256,'apkBytes':len(raw),'certificateSha256':fingerprint,'apkV2CryptographicSignatureVerified':True,'apkV2AllContentDigestVerified':True,'signatureAlgorithm':hex(algorithm),'contentChunks':len(chunks),'zipCrcVerified':True,'storedEntries4ByteAligned':stored,'scope':'Independent local cryptography check of APK v2 signed data, full APK chunk digests, original certificate and 4-byte ZIP stored-entry alignment. Android platform apksigner/aapt checks remain separate Actions evidence. No SDK or signing key used.'}
Path(a.out).write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
