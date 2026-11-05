import copy
from pathlib import Path
import sys
import unittest
sys.path.insert(0,str(Path(__file__).parents[1]/'scripts'))
from verify_p0_release import validate_gate


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
