import sys
from pathlib import Path
import tempfile
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from p071_navigation import cases,CLASS

class NavigationGateTest(unittest.TestCase):
    def xml(self,folder,count=18,child='',duplicate=False):
        text='<testsuite>'+''.join(f'<testcase classname="{CLASS}" name="T{i:02}_scenario">{child if i==1 else ""}</testcase>' for i in range(1,count+1))
        if duplicate:text+=f'<testcase classname="{CLASS}" name="T01_duplicate"/>'
        (folder/'TEST-navigation.xml').write_text(text+'</testsuite>')
    def test_complete_eighteen_cases(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d);self.xml(p);self.assertEqual(18,cases(p)['passed'])
    def test_missing_report_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaises(ValueError):cases(Path(d))
    def test_partial_cases_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d);self.xml(p,17)
            with self.assertRaises(ValueError):cases(p)
    def test_failure_error_and_skip_rejected(self):
        for child in ('<failure/>','<error/>','<skipped/>'):
            with self.subTest(child=child),tempfile.TemporaryDirectory() as d:
                p=Path(d);self.xml(p,child=child)
                with self.assertRaises(ValueError):cases(p)
    def test_duplicate_case_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d);self.xml(p,duplicate=True)
            with self.assertRaises(ValueError):cases(p)
