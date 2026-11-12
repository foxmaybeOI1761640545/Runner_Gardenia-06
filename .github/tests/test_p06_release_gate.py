import copy,sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
import test_p051_final_gate as previous_tests
from p06_signing import validate_gate
from p06_release import activation_paths,publication_paths
from p06_map_evidence import validate_summary
from p06_map_durability import parse_result

class MapReleaseGateTests(unittest.TestCase):
    def test_home_viewport_restoration_requires_real_home_and_retains_title_assertion(self):
        import xml.etree.ElementTree as ET
        from p06_release_theme_ui import scrolled_home,page_matches
        tree=ET.fromstring('<hierarchy><node resource-id="com.fox.runner:id/p02_home_scroll"/></hierarchy>')
        self.assertFalse(page_matches(tree,'home'))
        self.assertTrue(scrolled_home(tree,'home','mCurrentFocus=P0Activity'))
        for page,focus in [('home','mCurrentFocus=P06MapActivity'),('home','mCurrentFocus=Application Error'),('history','mCurrentFocus=P0Activity')]:
            with self.subTest(page=page,focus=focus):self.assertFalse(scrolled_home(tree,page,focus))
        restored=ET.fromstring('<hierarchy><node resource-id="com.fox.runner:id/p02_home_scroll"/><node resource-id="com.fox.runner:id/p0_title"/></hierarchy>')
        self.assertFalse(scrolled_home(restored,'home','mCurrentFocus=P0Activity'))
        self.assertTrue(page_matches(restored,'home'))
        self.assertFalse(scrolled_home(ET.fromstring('<hierarchy/>'),'home','mCurrentFocus=P0Activity'))
    def valid(self):
        plan,report,source=previous_tests.FinalP05GateTests().valid();plan['versionName']='v0.1.0-p0.6'
        names=['imported-airplane','zoom-pan','recreated-airplane','current-position','live-track','segmented-track','historical-track','invalid-file-retains-map']
        names+=['theme-'+f+'-'+m for f in ['PRECISION','QUIET','EXPLORER','NEON'] for m in ['dark','light']]
        receipt={k:True for k in ['passed','airplaneMode','safImport','zoomAndPan','currentPositionWithoutWorkout','liveTrack','historicalTrack','segmentBreaks','recreation','invalidFileRetainsMap']}
        receipt.update(sourceCommit=source,screenshots=[dict(file='p06-'+name+'.png') for name in names]);report['offlineMapEvidence']=receipt
        report['offlineMapProcessDurability']=dict(commit=source,passed=True,airplaneMode=True,forceStopped=True,nativeTilesRenderedAfterRestart=True,privateMapRetained=True,pendingImportCleaned=True,phases={'write':{'pid':10},'read':{'pid':20}})
        return plan,report,source
    def test_signing_requires_exact_map_and_all_legacy_gates(self):
        validate_gate(*self.valid())
        for field in ['airplaneMode','safImport','currentPositionWithoutWorkout','segmentBreaks']:
            plan,report,source=self.valid();report['offlineMapEvidence'][field]=False
            with self.subTest(field=field),self.assertRaises(ValueError):validate_gate(plan,report,source)
        plan,report,source=self.valid();report['offlineMapProcessDurability']['phases']['read']['pid']=10
        with self.assertRaises(ValueError):validate_gate(plan,report,source)
    def test_stale_or_incomplete_map_images_fail(self):
        _,report,source=self.valid();receipt=report['offlineMapEvidence']
        for changed in [{**receipt,'sourceCommit':'b'*40},{**receipt,'screenshots':receipt['screenshots'][:-1]}]:
            with self.assertRaises(ValueError):validate_summary(changed,source)
    def test_p06_approval_paths_cannot_change_workflow_or_product(self):
        activation_paths(['.github/p06/visual-approval.json','docs/p0-6/01-review.md']);publication_paths(['.github/p06/publication-approval.json'])
        for check in [activation_paths,publication_paths]:
            with self.assertRaises(ValueError):check(['src/main/java/Changed.java'])
            with self.assertRaises(ValueError):check(['.github/workflows/p06-offline-map.yml'])
    def test_process_probe_requires_one_actual_success_and_pid(self):
        text='INSTRUMENTATION_STATUS: p06MapPid=123\nOK (1 test)\nINSTRUMENTATION_CODE: -1\n'
        self.assertEqual(parse_result(text)['executed'],1)
        for bad in [text.replace('123','0'),text.replace('1 test','0 tests'),text+'FAILURES!!!']:
            with self.assertRaises(ValueError):parse_result(bad)
    def test_native_png_sampler_decodes_all_five_filters_without_redecoding_per_pixel(self):
        import struct,zlib,binascii
        from p06_release_map_ui import png_pixels
        def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',binascii.crc32(kind+data)&0xffffffff)
        rows={0:([10,20,30,40,50,60],[70,80,90,100,110,120]),1:([10,20,30,30,30,30],[70,80,90,30,30,30]),2:([10,20,30,40,50,60],[60,60,60,60,60,60]),3:([10,20,30,35,40,45],[65,70,75,45,45,45]),4:([10,20,30,30,30,30],[60,60,60,30,30,30])}
        for kind,(first,second) in rows.items():
            raw=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',2,2,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(bytes([kind]+first+[kind]+second)))+chunk(b'IEND',b'')
            with self.subTest(filter=kind):self.assertEqual([(10,20,30),(40,50,60),(70,80,90),(100,110,120)],png_pixels(raw,[(0,0),(1,0),(0,1),(1,1)]))

    def test_signed_history_requires_real_teal_route_not_only_local_basemap(self):
        import struct,zlib,binascii,xml.etree.ElementTree as ET
        from p06_release_map_ui import verify_track_pixels
        def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',binascii.crc32(kind+data)&0xffffffff)
        def picture(line):
            rows=[]
            for y in range(80):
                pixels=[]
                for x in range(24):
                    color=(210,240,180) if x<18 else (170,220,240)
                    if y==5:color=(100,100,100)
                    if line and 10<=x<=14 and (12<=y<=65 if line=='route' else 35<=y<=43):color=(0,127,134)
                    pixels.extend(color)
                rows.append(bytes([0]+pixels))
            return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',24,80,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(rows)))+chunk(b'IEND',b'')
        tree=ET.fromstring('<hierarchy><node resource-id="com.fox.runner:id/p06_map_container" bounds="[0,0][24,80]"/></hierarchy>')
        for invalid in [None,'point']:
            with self.subTest(scene=invalid),self.assertRaises(ValueError):verify_track_pixels(picture(invalid),tree,32)
        self.assertGreaterEqual(verify_track_pixels(picture('route'),tree,32)['trackSpanPixels'],32)
