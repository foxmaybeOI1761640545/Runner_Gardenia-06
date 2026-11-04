import importlib.util
from pathlib import Path
import unittest

spec=importlib.util.spec_from_file_location('p0_durability',Path(__file__).parents[1]/'scripts/p0_durability.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)


class ProcessEvidenceTests(unittest.TestCase):
    GOOD='INSTRUMENTATION_STATUS: p0DurabilityPid=1234\nOK (1 test)\nINSTRUMENTATION_CODE: -1\n'
    def test_success_requires_executed_case_and_process_identity(self):
        self.assertEqual(module.parse_result(self.GOOD)['pid'],1234)
    def test_crash_or_failure_is_never_a_successful_adb_exit(self):
        for value in ('INSTRUMENTATION_CODE: -1\nFAILURES!!!\n',self.GOOD+'Process crashed\n'):
            with self.assertRaises(ValueError):module.parse_result(value)
    def test_missing_pid_zero_cases_or_missing_finish_code_fail_closed(self):
        for value in (self.GOOD.replace('1234','0'),self.GOOD.replace('1 test','0 tests'),self.GOOD.replace('INSTRUMENTATION_CODE: -1','')):
            with self.assertRaises(ValueError):module.parse_result(value)
