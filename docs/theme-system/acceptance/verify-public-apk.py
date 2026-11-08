"""Read-only independent APK v2 signature/content and manifest identity verification.

Uses existing Python cryptography; no SDK, build, signing, keys or Android tooling.
"""
import hashlib, json, struct, sys, zipfile
from pathlib import Path
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, ec

def u32(b, p=0): return struct.unpack_from('<I', b, p)[0]
def lp(b, p=0):
    n = u32(b,p)
    if p+4+n>len(b): raise ValueError('Truncated length field')
    return b[p+4:p+4+n], p+4+n
def records(b):
    p=0
    while p<len(b):
        item,p=lp(b,p)
        yield item
def manifest(raw):
    strings=[]; attrs={};p=8
    while p<len(raw):
        kind,header,size=struct.unpack_from('<HHI',raw,p)
        if size<header or p+size>len(raw):raise ValueError('Invalid AXML chunk')
        if kind==1:
            count,styles,flags,start,style_start=struct.unpack_from('<IIIII',raw,p+8)
            offsets=[u32(raw,p+header+4*i) for i in range(count)]
            def length8(q):
                a=raw[q];q+=1
                if a&128:a=((a&127)<<8)|raw[q];q+=1
                return a,q
            def length16(q):
                a=struct.unpack_from('<H',raw,q)[0];q+=2
                if a&32768:a=((a&32767)<<16)|struct.unpack_from('<H',raw,q)[0];q+=2
                return a,q
            for off in offsets:
                q=p+start+off
                if flags&256:
                    _,q=length8(q);n,q=length8(q);strings.append(raw[q:q+n].decode('utf8'))
                else:
                    n,q=length16(q);strings.append(raw[q:q+2*n].decode('utf-16le'))
        if kind==0x102:
            ext=p+header
            name=u32(raw,ext+4)
            if strings[name]=='manifest':
                start,step,count=struct.unpack_from('<HHH',raw,ext+8)
                for i in range(count):
                    a=ext+start+i*step;ns,name,value=struct.unpack_from('<III',raw,a)
                    data_type=raw[a+15];data=u32(raw,a+16)
                    attrs[strings[name]]=strings[value] if value!=0xffffffff else strings[data] if data_type==3 else data
                return attrs
        p+=size
    raise ValueError('Missing manifest')

def verify(path, expected_version="v0.1.0-p0.3", expected_code=6774):
    raw=Path(path).read_bytes();eocd=raw.rfind(b'PK\x05\x06')
    if eocd<0 or eocd+22+struct.unpack_from('<H',raw,eocd+20)[0]!=len(raw):raise ValueError('EOCD invalid')
    cd=u32(raw,eocd+16)
    if raw[cd-16:cd]!=b'APK Sig Block 42':raise ValueError('APK signature block missing')
    size=struct.unpack_from('<Q',raw,cd-24)[0];block=cd-size-8
    if struct.unpack_from('<Q',raw,block)[0]!=size:raise ValueError('APK block length mismatch')
    p=block+8;v2=None
    while p<cd-24:
        n=struct.unpack_from('<Q',raw,p)[0];entry=raw[p+8:p+8+n];p+=8+n
        if u32(entry)==0x7109871a:v2=entry[4:]
    if v2 is None:raise ValueError('V2 signature missing')
    signers,_=lp(v2)
    rows=[]
    for signer in records(signers):
        signed,p=lp(signer);sigs,p=lp(signer,p);pub,p=lp(signer,p)
        digests,p=lp(signed);certs,p=lp(signed,p)
        certificates=list(records(certs));cert=x509.load_der_x509_certificate(certificates[0])
        key=cert.public_key()
        if key.public_bytes(serialization.Encoding.DER,serialization.PublicFormat.SubjectPublicKeyInfo)!=pub:raise ValueError('Signer key differs from certificate')
        algorithms={0x0101:(hashes.SHA256,padding.PSS(mgf=padding.MGF1(hashes.SHA256()),salt_length=32)),0x0103:(hashes.SHA256,padding.PKCS1v15()),0x0102:(hashes.SHA512,padding.PSS(mgf=padding.MGF1(hashes.SHA512()),salt_length=64)),0x0104:(hashes.SHA512,padding.PKCS1v15()),0x0201:(hashes.SHA256,None),0x0202:(hashes.SHA512,None)}
        signature_map={u32(v):lp(v,4)[0] for v in records(sigs)}
        digest_map={u32(v):lp(v,4)[0] for v in records(digests)}
        algorithm=next((a for a in signature_map if a in algorithms),None)
        if algorithm is None:raise ValueError('Unsupported signer algorithm')
        hash_class,pad=algorithms[algorithm]
        if pad:key.verify(signature_map[algorithm],signed,pad,hash_class())
        else:key.verify(signature_map[algorithm],signed,ec.ECDSA(hash_class()))
        hash_fn=hashlib.sha256 if hash_class==hashes.SHA256 else hashlib.sha512
        patched=bytearray(raw[eocd:]);struct.pack_into('<I',patched,16,block)
        chunks=[]
        for segment in [raw[:block],raw[cd:eocd],patched]:
            for i in range(0,len(segment),1024*1024):
                chunk=segment[i:i+1024*1024];chunks.append(hash_fn(b'\xa5'+struct.pack('<I',len(chunk))+chunk).digest())
        content_digest=hash_fn(b'\x5a'+struct.pack('<I',len(chunks))+b''.join(chunks)).digest()
        if content_digest!=digest_map[algorithm]:raise ValueError('APK content signature digest mismatch')
        rows.append({'certificateSha256':cert.fingerprint(hashes.SHA256()).hex().upper(),'algorithm':hex(algorithm),'signatureVerified':True,'contentDigestVerified':True})
    with zipfile.ZipFile(path) as archive:identity=manifest(archive.read('AndroidManifest.xml'))
    result={'file':str(path),'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest(),'package':identity['package'],'versionName':identity['versionName'],'versionCode':int(identity['versionCode']),'signers':rows}
    assert result['package']=='com.fox.runner'
    assert result['versionName']==expected_version and result['versionCode']==expected_code
    assert len(rows)==1 and rows[0]['certificateSha256']=='E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22'
    return result
if __name__=='__main__':print(json.dumps(verify(sys.argv[1]),indent=2))
