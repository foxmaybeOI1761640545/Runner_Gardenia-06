"""P0 cannot silently broaden signing or omit the configured stage tests."""
import json
from pathlib import Path
import sys
import unittest
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
import p0_pipeline as p0

class P0ConfigurationTests(unittest.TestCase):
    def config(self,**values):
        data={'stage':2,'fullRegression':False,'signed':False,'classes':['de.dennisguse.opentracks.p0.P0ActivitySmokeTest']};data.update(values)
        with patch.object(p0.Path,'read_text',return_value=json.dumps(data)):return p0.configuration()
    def test_empty_scope_cannot_pass(self):
        with self.assertRaises(ValueError):self.config(classes=[])
    def test_signing_cannot_start_before_final_complete_regression(self):
        for stage in range(1,10):
            with self.assertRaises(ValueError):self.config(stage=stage,signed=True)
        with self.assertRaises(ValueError):self.config(stage=10,signed=True,fullRegression=False)
        self.assertTrue(self.config(stage=10,signed=True,fullRegression=True)['signed'])
    def test_final_regression_cannot_be_narrowed(self):
        with self.assertRaises(ValueError):self.config(stage=9,fullRegression=False)
    def test_class_arguments_cannot_inject_gradle_options(self):
        with self.assertRaises(ValueError):self.config(classes=['Test; echo secret'])
