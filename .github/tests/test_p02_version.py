import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
sys.path.insert(0,str(Path(__file__).parents[1]/'scripts'))
from p02_version import allocated_code,remote_history,VERSION,TAG

class VersionTests(unittest.TestCase):
    def fixture(self):
        entries=[{'versionName':f'v0.1.0-rc1.{n}','versionCode':6765+n,'commit':'b'*40} for n in (4,5,6)]
        entries+=[{'versionName':'v0.1.0-p0.1','versionCode':6772,'commit':'c'*40},
                  {'versionName':VERSION,'versionCode':6773,'commit':None,'allocationBasisCommit':'b'*40,'classification':'reserved-p02-test','tag':TAG}]
        return {'schema':1,'codeHighWater':6773,'seriesHighWater':{'v0.1.0-rc1.':6,'v0.1.0-p0.':2},'allocations':entries}
    def local(self,operation):
        original=Path.cwd()
        with tempfile.TemporaryDirectory() as t:
            os.chdir(t)
            try:operation(self.fixture())
            finally:os.chdir(original)
    def test_published_p0_codes_and_reservation_survive_even_with_no_git_tags(self):
        def run(data):
            Path('release-ledger.json').write_text(json.dumps(data));self.assertEqual(6773,allocated_code(data))
        self.local(run)
    def test_duplicate_or_skipped_next_code_is_rejected(self):
        def run(data):
            for code in (6772,6774):
                data['allocations'][-1]['versionCode']=code;data['codeHighWater']=code;Path('release-ledger.json').write_text(json.dumps(data))
                with self.assertRaises((ValueError,SystemExit)):allocated_code(data)
        self.local(run)
    def test_only_explicit_reservation_can_omit_a_future_source(self):
        def run(data):
            data['allocations'][-2]['commit']=None;Path('release-ledger.json').write_text(json.dumps(data))
            with self.assertRaises(SystemExit):allocated_code(data)
        self.local(run)
    def test_empty_own_draft_can_resume_but_unknown_version_cannot(self):
        class API:
            base='https://api.github.com/unit'
            def __init__(self,tag):self.tag=tag
            def request(self,url):return [{'tag_name':self.tag,'draft':True,'target_commitish':'b'*40}]
            def assets(self,r):return []
        self.assertEqual(1,len(remote_history(API(TAG),'b'*40,6773)))
        with self.assertRaises(ValueError):remote_history(API('unknown'),'b'*40,6773)
