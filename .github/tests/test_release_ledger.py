"""Historical allocation must survive removal of the greatest tag, not just old tags."""
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
import release_policy as policy


class LedgerTests(unittest.TestCase):
    def fixture(self):
        subprocess.run(['git','init','-b','main'],check=True,capture_output=True)
        policy.git('config','user.name','Ledger test');policy.git('config','user.email','ledger@example.invalid')
        Path('version.properties').write_text('VERSION_NAME=v0.1.0-rc1.1\nVERSION_CODE=6766\n')
        policy.git('add','.');policy.git('commit','-m','fixture')
        commit=policy.git('rev-parse','HEAD')
        entries=[{'versionName':f'v0.1.0-rc1.{n}','versionCode':6765+n,'commit':commit} for n in range(1,5)]
        ledger={'schema':1,'codeHighWater':6769,'seriesHighWater':{'v0.1.0-rc1.':4},'allocations':entries}
        Path('release-ledger.json').write_text(json.dumps(ledger))
        for entry in entries:
            policy.git('tag','-a',entry['versionName'],'-m',policy.MARKER+json.dumps(entry))
        return ledger

    def in_repository(self,operation):
        original=Path.cwd()
        with tempfile.TemporaryDirectory() as tmp:
            os.chdir(tmp)
            try:
                env={'GITHUB_RUN_ID':'new','GITHUB_EVENT_NAME':'push','GITHUB_REF':'refs/heads/main',
                     'GITHUB_REPOSITORY':'foxmaybeOI1761640545/Runner_Gardenia-06','GITHUB_OUTPUT':'','GITHUB_SHA':''}
                with patch.dict(os.environ,env):operation(self.fixture())
            finally:os.chdir(original)

    def test_deleting_highest_or_all_tags_does_not_reuse_allocated_version_or_code(self):
        def scenario(ledger):
            policy.git('tag','-d','v0.1.0-rc1.4')
            policy.prepare('plan.json');plan=json.loads(Path('plan.json').read_text())
            self.assertEqual(('v0.1.0-rc1.5',6770),(plan['versionName'],plan['versionCode']))
            for tag in policy.git('tag','--list').splitlines():policy.git('tag','-d',tag)
            policy.prepare('plan.json');self.assertEqual(plan,json.loads(Path('plan.json').read_text()))
        self.in_repository(scenario)

    def test_corrupt_high_water_duplicate_codes_and_conflicting_tags_fail_closed(self):
        def scenario(ledger):
            for bad in [dict(ledger,codeHighWater=6768),dict(ledger,seriesHighWater={'v0.1.0-rc1.':3}),
                        dict(ledger,allocations=ledger['allocations']+[ledger['allocations'][0]])]:
                Path('release-ledger.json').write_text(json.dumps(bad))
                with self.assertRaises(SystemExit):policy.prepare('plan.json')
            ledger['allocations'][0]['commit']='b'*40
            Path('release-ledger.json').write_text(json.dumps(ledger))
            with self.assertRaises(SystemExit):policy.prepare('plan.json')
        self.in_repository(scenario)

    def test_historical_name_or_code_cannot_be_tagged_again(self):
        def scenario(ledger):
            for tag in policy.git('tag','--list').splitlines():policy.git('tag','-d',tag)
            base={'commit':policy.git('rev-parse','HEAD'),'runId':'new','publishing':True}
            for name,code in [('v0.1.0-rc1.4',6770),('v0.1.0-rc1.5',6769)]:
                plan={**base,'versionName':name,'versionCode':code}
                Path('plan.json').write_text(json.dumps(plan))
                Path('gate.json').write_text(json.dumps({'plan':plan,'passed':True}))
                with patch('signed_bundle.validate_bundle',return_value={'apkSha256':'a'*64}),patch.object(policy.subprocess,'run'):
                    with self.assertRaises(SystemExit):policy.create_tag('plan.json','gate.json')
                self.assertEqual('',policy.git('tag','--list'))
        self.in_repository(scenario)


if __name__=='__main__':unittest.main()
