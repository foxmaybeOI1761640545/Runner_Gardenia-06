"""Require all T01–T18 from raw JUnit; never substitute screenshots for execution."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import xml.etree.ElementTree as ET
from collect_p02_home_ui import png_size

CLASS='de.dennisguse.opentracks.p0.P071NavigationTest'
OUT=Path('build/reports/p071-navigation')

def cases(folder):
    found={}
    for path in folder.rglob('TEST-*.xml'):
        for case in ET.parse(path).getroot().iter('testcase'):
            if case.get('classname')!=CLASS:continue
            name=case.get('name','');code=name.split('_')[0]
            if code not in {f'T{i:02}' for i in range(1,19)} or code in found:raise ValueError('Unexpected/duplicate navigation case')
            if any(case.find(tag) is not None for tag in ('failure','error','skipped')):raise ValueError('Navigation case did not pass: '+name)
            found[code]=name
    if set(found)!={f'T{i:02}' for i in range(1,19)}:raise ValueError('Missing navigation scenarios')
    return {'tests':18,'executed':18,'passed':18,'failures':0,'errors':0,'skipped':0,'cases':found}

def collect(focused=False):
    source=os.environ['GITHUB_SHA'];report=cases(Path('build/outputs/androidTest-results'))
    OUT.mkdir(parents=True,exist_ok=True)
    roots=list(Path('build/outputs').glob('**/p0-ui'))
    if len(roots)!=1:raise ValueError('Missing unambiguous native output')
    rows=[]
    for path in sorted(roots[0].glob('p071-*.png')):
        raw=path.read_bytes();png_size(raw)
        shutil.copy2(path,OUT/path.name);rows.append({'file':path.name,'sha256':hashlib.sha256(raw).hexdigest()})
    if len(rows)!=25:raise ValueError('All 18 scenarios and eight palettes must be captured')
    result={'passed':True,'source':source,'runId':os.environ['GITHUB_RUN_ID'],'navigation':report,'screenshots':rows,'physicalDevices':'NEEDS_DEVICE_VALIDATION','visualReviewRequired':True,'scope':'focused' if focused else 'full-regression'}
    (OUT/('focused.json' if focused else 'full.json')).write_text(json.dumps(result,indent=2)+'\n')
    target=OUT/('focused-junit' if focused else 'full-junit');target.mkdir(exist_ok=True)
    for path in Path('build/outputs/androidTest-results').rglob('TEST-*.xml'):shutil.copy2(path,target/path.name)
    return result

def evidence(root,source):
    result=json.loads((root/OUT/'full.json').read_text())
    actual=cases(root/'build/outputs/androidTest-results')
    if result['source']!=source or result['scope']!='full-regression' or result['navigation']!=actual or not result['passed']:raise ValueError('Navigation proof differs from raw full regression')
    if len(result['screenshots'])!=25 or len({r['file'] for r in result['screenshots']})!=25:raise ValueError('Incomplete navigation visuals')
    for row in result['screenshots']:
        if Path(row['file']).name!=row['file']:raise ValueError('Unsafe navigation screenshot path')
        data=(root/OUT/row['file']).read_bytes();png_size(data)
        if hashlib.sha256(data).hexdigest()!=row['sha256']:raise ValueError('Navigation screenshot changed')
    return result

if __name__=='__main__':collect(sys.argv[1:] == ['focused'])
