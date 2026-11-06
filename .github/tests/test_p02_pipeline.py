import json
from pathlib import Path
import sys
import unittest
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
import p02_pipeline as pipeline

class P02IsolationTests(unittest.TestCase):
    def config(self,**values):
        config=dict(phase=3,stage=9,fullRegression=True,classes=[],signed=False);config.update(values)
        with patch.object(pipeline.Path,'read_text',return_value=json.dumps(config)):
            return pipeline.configuration()
    def test_incomplete_and_scoped_regression_never_authorize_phase(self):
        for values in [dict(fullRegression=False),dict(classes=['OnlyNewGpsTests']),dict(phase=2),dict(stage=3)]:
            with self.assertRaises(ValueError):self.config(**values)
    def test_development_and_ordinary_checks_cannot_request_signing(self):
        for phase in [3,4,5,7]:
            with self.assertRaises(ValueError):self.config(phase=phase,signed=True)
        with self.assertRaises(ValueError):self.config(phase=6,signed=True,stage=9)
        self.assertTrue(self.config(phase=6,signed=True,stage=10)['signed'])
    def test_changed_original_p0_branch_is_detected_without_resetting_it(self):
        snapshot=dict(baselineCommit='a'*40,protectedRemoteBranches={'refs/heads/feature/p0-core-migration':'b'*40},protectedTags={})
        with patch.object(pipeline.Path,'read_text',return_value=json.dumps(snapshot)),patch.object(pipeline.subprocess,'check_output',return_value='c'*40+'\trefs/heads/feature/p0-core-migration\n'),patch.object(pipeline.subprocess,'run') as run:
            with self.assertRaises(ValueError):pipeline.protected_refs()
            run.assert_not_called()
