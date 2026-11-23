import sys
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import p071_signed_back as back


class SignedBackProbeTest(unittest.TestCase):
    def setUp(self):
        back.checks.clear()

    def test_actual_background_then_existing_home_is_required(self):
        calls = []
        windows = iter([
            'mCurrentFocus=com.fox.runner/P0SettingsActivity',
            'mCurrentFocus=null',
            'mCurrentFocus=com.android.launcher/Launcher',
            'mCurrentFocus=com.android.launcher/Launcher',
            'mCurrentFocus=com.fox.runner/P0Activity',
        ])
        def device(*args):
            calls.append(args)
            if args == ('shell', 'dumpsys', 'window'):
                return next(windows)
            if args[:3] == ('shell', 'am', 'start') and '-W' in args:
                raise TimeoutError('API35 existing-drawn Activity launch wait')
            return ''
        with patch.object(back.time, 'sleep'):
            back.root_back_home(device)
        self.assertEqual(1, len(back.checks))
        self.assertTrue(back.checks[0]['backgrounded'])
        self.assertIn(('shell', 'input', 'keyevent', '4'), calls)

    def test_returning_to_another_root_never_counts_as_background(self):
        def device(*args):
            return 'mCurrentFocus=com.fox.runner/P0HistoryActivity'
        with patch.object(back.time, 'monotonic', side_effect=[0, 1, 16]), patch.object(back.time, 'sleep'):
            with self.assertRaisesRegex(ValueError, 'instead of backgrounding'):
                back.root_back_home(device)
        self.assertEqual([], back.checks)

    def test_background_without_actual_home_restore_is_rejected(self):
        windows = iter(['mCurrentFocus=com.fox.runner/P0Activity',
                        'mCurrentFocus=com.android.launcher/Launcher',
                        'mCurrentFocus=com.android.launcher/Launcher'])
        def device(*args):
            return next(windows) if args == ('shell', 'dumpsys', 'window') else ''
        with patch.object(back.time, 'monotonic', side_effect=[0, 1, 0, 1, 16]), patch.object(back.time, 'sleep'):
            with self.assertRaisesRegex(ValueError, 'did not restore'):
                back.root_back_home(device)
        self.assertEqual([], back.checks)

    def test_secondary_window_is_not_a_root_probe(self):
        with self.assertRaisesRegex(ValueError, 'real top-level'):
            back.root_back_home(lambda *args: 'mCurrentFocus=com.fox.runner/P06MapActivity')
        self.assertEqual([], back.checks)
