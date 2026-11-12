import copy,sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
import test_p051_final_gate as previous_tests
from p06_signing import validate_gate
from p06_release import activation_paths,publication_paths
from p06_map_evidence import validate_summary
from p06_map_durability import parse_result

class MapReleaseGateTests(unittest.TestCase):
    def valid(self):
        plan,report,source=previous_tests.FinalP05GateTests().valid();plan['versionName']='v0.1.0-p0.6'
        names=['imported-airplane','zoom-pan','recreated-airplane','current-position','live-track','segmented-track','historical-track','invalid-file-retains-map']
        names+=['theme-'+f+'-'+m for f in ['PRECISION','QUIET','EXPLORER','NEON'] for m in ['dark','light']]
        receipt={k:True for k in ['passed','airplaneMode','safImport','zoomAndPan','currentPositionWithoutWorkout','liveTrack','historicalTrack','segmentBreaks','recreation','invalidFileRetainsMap']}
        receipt.update(sourceCommit=source,screenshots=[dict(file='p06-'+name+'.png') for name in names]);report['offlineMapEvidence']=receipt
        report['offlineMapProcessDurability']=dict(commit=source,passed=True,airplaneMode=True,forceStopped=True,nativeTilesRenderedAfterRestart=True,privateMapRetained=True,phases={'write':{'pid':10},'read':{'pid':20}})
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
