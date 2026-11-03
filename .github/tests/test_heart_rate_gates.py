"""Gate regression fixtures. These negative PNG fixtures are never real UI evidence."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
import check_test_results as reports
import collect_heart_rate_ui as screenshots


class UnitEvidenceTests(unittest.TestCase):
    def fixture(self,root,body='<testcase classname="example.ParserTest" name="realCase"/>',tests=1,skips=0,failures=0,errors=0):
        source=root/'source';source.mkdir();(source/'ParserTest.java').write_text('package example;\nclass ParserTest {\n@Test public void realCase() {}\n}')
        (root/'TEST-parser.xml').write_text(f'<testsuite name="example.ParserTest" tests="{tests}" failures="{failures}" errors="{errors}" skipped="{skips}">{body}</testsuite>')
        return source
    def test_actual_jvm_execution_is_counted_separately(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);source=self.fixture(root)
            r=reports.inspect(root,source,require_environment=False)
            self.assertTrue(r['passed']);self.assertEqual(1,r['executed']);self.assertEqual([],r['missingRequiredTests'])
    def test_no_source_or_missing_xml_cannot_be_described_as_executed(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            with self.assertRaises(ValueError):reports.inspect(root,root,require_environment=False)
            source=self.fixture(root);(source/'ParserTest.java').unlink()
            with self.assertRaises(ValueError):reports.inspect(root,source,require_environment=False)
    def test_all_skipped_or_missing_test_class_blocks_the_gate(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);source=self.fixture(root,'<testcase classname="example.ParserTest" name="realCase"><skipped/></testcase>',skips=1)
            self.assertFalse(reports.inspect(root,source,require_environment=False)['passed'])
            (source/'MissingTest.java').write_text('package example;\nclass MissingTest {\n@Test public void missing() {}\n}')
            self.assertEqual(['example.MissingTest'],reports.inspect(root,source,require_environment=False)['missingClasses'])
    def test_unit_errors_failures_and_inconsistent_counts_are_not_hidden(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);source=self.fixture(root,'<testcase classname="example.ParserTest" name="realCase"><error>fixture</error></testcase>',errors=1)
            r=reports.inspect(root,source,require_environment=False);self.assertFalse(r['passed']);self.assertEqual(1,r['errors']);self.assertEqual(0,r['failures'])
            (root/'TEST-parser.xml').write_text((root/'TEST-parser.xml').read_text().replace('tests="1"','tests="2"'))
            with self.assertRaises(ValueError):reports.inspect(root,source,require_environment=False)


class UiEvidenceTests(unittest.TestCase):
    def test_missing_capture_directory_is_a_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            with self.assertRaises(ValueError):screenshots.collect(root/'outputs',root/'reports')
    def test_incomplete_capture_manifest_is_a_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);source=root/'outputs'/'heart-rate-ui';source.mkdir(parents=True)
            (source/'manifest.json').write_text(json.dumps({'physicalBleTested':False,'timezone':'Asia/Singapore','screenshots':[]}))
            with self.assertRaises(ValueError):screenshots.collect(root/'outputs',root/'reports')
    def test_offset_alias_cannot_replace_the_singapore_timezone_identity(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);source=root/'outputs'/'heart-rate-ui';source.mkdir(parents=True)
            (source/'manifest.json').write_text(json.dumps({'physicalBleTested':False,'timezone':'UTC+08:00','screenshots':[]}))
            with self.assertRaises(ValueError):screenshots.collect(root/'outputs',root/'reports')
    def test_named_files_without_png_pixels_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);source=root/'outputs'/'heart-rate-ui';source.mkdir(parents=True)
            suffixes=('home','recording-connected','recording-stale','recording-reconnecting','history','time-chart','distance-chart','settings','scan','scan-fixture')
            captures=[]
            for size in ('phone','narrow'):
                for theme in ('light','dark'):
                    for suffix in suffixes:
                        name=f'{size}-{theme}-{suffix}.png';(source/name).write_bytes(b'negative test fixture, not a PNG')
                        captures.append({'file':name,'width':720,'height':1280,'nightMode':32 if theme=='dark' else 16,'widthDp':360})
            (source/'manifest.json').write_text(json.dumps({'physicalBleTested':False,'timezone':'Asia/Singapore','screenshots':captures}))
            with self.assertRaises(ValueError):screenshots.collect(root/'outputs',root/'reports')
