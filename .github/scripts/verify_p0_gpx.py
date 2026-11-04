"""Validate real Android exporter outputs with the official, locally pinned GPX 1.1 XSD."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET

REQUIRED={'raw-complete','heart-only','gps-only','segments','legacy','decimal-escaping','local-file','gps-gap'}


def verify():
    sources=list(Path('build/outputs').glob('**/p0-gpx'))
    if len(sources)!=1:raise ValueError('Actual Android GPX output directory missing or ambiguous')
    files=sorted(sources[0].glob('*.gpx'))
    folder=Path('build/reports/p0-gpx');folder.mkdir(parents=True,exist_ok=True)
    for file in files:shutil.copyfile(file,folder/file.name)
    if not REQUIRED.issubset({file.stem for file in files}):raise ValueError('Missing mandatory actual exporter fixtures')
    schema=Path('.github/schemas/gpx-1.1.xsd')
    # Preserve the official schema content; the checked-in copy only normalizes CRLF to LF.
    expected=Path('.github/schemas/gpx-1.1.sha256').read_text().split()[0]
    if hashlib.sha256(schema.read_bytes()).hexdigest()!=expected:raise ValueError('GPX schema checksum changed')
    subprocess.run(['java','.github/scripts/ValidateP0Gpx.java',str(schema),*map(str,files)],check=True)
    evidence=[]
    for file in files:
        root=ET.parse(file).getroot()
        evidence.append({'file':file.name,'sha256':hashlib.sha256(file.read_bytes()).hexdigest(),
                         'points':len(root.findall('.//{http://www.topografix.com/GPX/1/1}trkpt')),
                         'samples':len(root.findall('.//{urn:runner-gardenia:gpx:1}sample'))})
    proof={'passed':True,'commit':os.environ['GITHUB_SHA'],'files':evidence,'schemaSha256':expected}
    (folder/'result.json').write_text(json.dumps(proof,indent=2)+'\n');print(json.dumps(proof,indent=2))


if __name__=='__main__':verify()
