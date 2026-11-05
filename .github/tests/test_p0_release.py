import copy
from pathlib import Path
import sys
import unittest
sys.path.insert(0,str(Path(__file__).parents[1]/'scripts'))
from verify_p0_release import validate_gate
from p0_release_smoke import validate_live_metrics


class SigningGateTests(unittest.TestCase):
    def fixture(self):
        plan={'stage':10,'commit':'tested','publishing':False}
        report={'plan':plan,'passed':True}
        for key in ('android','unit'):report[key]={'passed':True,'executed':1,'failures':0,'errors':0}
        for key in ('processDurability','gpxSchema','offline'):report[key]={'passed':True,'commit':'tested'}
        return plan,report,{'stage':10,'signed':True,'fullRegression':True}
    def test_exact_passed_branch_gate_is_accepted(self):
        validate_gate(*self.fixture(),'tested')
    def test_another_commit_or_publishing_plan_is_rejected(self):
        with self.assertRaises(ValueError):validate_gate(*self.fixture(),'other')
        p,r,c=self.fixture();p['publishing']=True
        with self.assertRaises(ValueError):validate_gate(p,r,c,'tested')
    def test_no_execution_or_failed_tests_never_authorize_signing(self):
        for field,value in [('executed',0),('failures',1),('errors',1)]:
            p,r,c=self.fixture();r['android'][field]=value
            with self.assertRaises(ValueError):validate_gate(p,r,c,'tested')
    def test_missing_or_other_commit_storage_evidence_blocks_signing(self):
        p,r,c=self.fixture();r['processDurability']['commit']='other'
        with self.assertRaises(ValueError):validate_gate(p,r,c,'tested')


class ActualGpsMetricTests(unittest.TestCase):
    VALID='Time 00:14 · Moving 00:14\nDistance 13.5 m\nSpeed 7.4 km/h · Pace 8:06 /km\nGPS fixed and ready\n1.300140, 103.799998 · ±5.0 m\n13:28:13.479'
    def test_actual_gnss_round_trip_uses_numeric_tolerance(self):
        validate_live_metrics(self.VALID)
    def test_blank_or_zero_motion_fields_do_not_pass(self):
        for old,new in [('Speed 7.4','Speed —'),('Pace 8:06','Pace —'),('Speed 7.4','Speed 0.0'),('8:06','8:99')]:
            with self.assertRaises(ValueError):validate_live_metrics(self.VALID.replace(old,new))
    def test_wrong_coordinates_or_accuracy_fail(self):
        for old,new in [('103.799998','104.799998'),('1.300140','3.300140'),('±5.0','±500.0')]:
            with self.assertRaises(ValueError):validate_live_metrics(self.VALID.replace(old,new))
