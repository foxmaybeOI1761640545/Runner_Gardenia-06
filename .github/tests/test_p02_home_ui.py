import hashlib
import json
from pathlib import Path
import struct
import sys
import tempfile
import zlib
from functools import lru_cache
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
import collect_p02_home_ui as ui

@lru_cache(maxsize=2)
def fixture_png(width,height):
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    header=struct.pack('>IIBBBBB',width,height,8,2,0,0,0)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',header)+chunk(b'IDAT',zlib.compress(b'\0'*(width*3+1)*height))+chunk(b'IEND',b'')

class HomeCaptureGateTests(unittest.TestCase):
    def fixture(self,root):
        rows=[]
        # Complete blank PNGs are private unit fixtures, never actual UI acceptance evidence.
        for size in ('small','large'):
            for theme in ('light','dark'):
                for state,phase in ui.STATES.items():
                    name=f'p02-home-{size}-{theme}-{state}.png';payload=fixture_png(*( (720,1280) if size=='small' else (1600,2560)))
                    (root/name).write_bytes(payload);rows.append(dict(file=name,state=state,phase=phase,nightMode=32 if theme=='dark' else 16,widthDp=360 if size=='small' else 800,sha256=hashlib.sha256(payload).hexdigest()))
        manifest=dict(sourceCommit='a'*40,timezone='Asia/Singapore',physicalBleTested=False,syntheticGps=True,screenshots=rows)
        self.write(root,manifest);return manifest
    def write(self,root,data):(root/'p02-home-matrix.json').write_text(json.dumps(data))
    def test_complete_matrix_is_tied_to_this_source_and_original_pixels(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);self.fixture(root);self.assertTrue(ui.inspect(root,'a'*40)['passed'])
            with self.assertRaises(ValueError):ui.inspect(root,'b'*40)
            image=root/'p02-home-small-light-recording.png';image.write_bytes(image.read_bytes()+b'changed')
            with self.assertRaises(ValueError):ui.inspect(root,'a'*40)
    def test_missing_or_duplicated_state_never_passes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);data=self.fixture(root);data['screenshots'][-1]=data['screenshots'][0];self.write(root,data)
            with self.assertRaises(ValueError):ui.inspect(root,'a'*40)
    def test_wrong_theme_window_or_phase_never_passes(self):
        for key,value in [('nightMode',32),('widthDp',800),('phase','PAUSED')]:
            with tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp);data=self.fixture(root);data['screenshots'][0][key]=value;self.write(root,data)
                with self.assertRaises(ValueError):ui.inspect(root,'a'*40)
    def test_hardware_claim_or_missing_timezone_is_rejected(self):
        for key,value in [('physicalBleTested',True),('syntheticGps',False),('timezone','UTC')]:
            with tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp);data=self.fixture(root);data[key]=value;self.write(root,data)
                with self.assertRaises(ValueError):ui.inspect(root,'a'*40)
