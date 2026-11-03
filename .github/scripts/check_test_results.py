"""Fail closed on missing, incomplete, inconsistent or failed instrumentation XML."""
import json
import os
from pathlib import Path
import re
import xml.etree.ElementTree as ET


def count(node, name):
    value = node.get(name)
    if value is None or not value.isdecimal():
        raise ValueError('Missing or invalid report count: ' + name)
    return int(value)


def inspect(root_path, source_path):
    files = sorted(root_path.rglob('TEST-*.xml'))
    if not files:
        raise ValueError('Instrumentation reports are missing.')
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    suites = []
    cases = {}
    for path in files:
        root = ET.parse(path).getroot()
        leaves = [node for node in root.iter('testsuite') if node.find('testsuite') is None]
        if not leaves:
            raise ValueError('No test suites found in ' + str(path))
        per_file = dict.fromkeys(totals, 0)
        for suite in leaves:
            declared = {key: count(suite, key) for key in totals}
            children = suite.findall('testcase')
            actual = {'tests': len(children), 'failures': sum(c.find('failure') is not None for c in children),
                      'errors': sum(c.find('error') is not None for c in children),
                      'skipped': sum(c.find('skipped') is not None for c in children)}
            if declared != actual:
                raise ValueError('Report totals disagree with actual cases: ' + suite.get('name', ''))
            for key in totals:
                totals[key] += actual[key]
                per_file[key] += actual[key]
            suites.append({'name': suite.get('name', ''), **actual})
            for case in children:
                identity = (case.get('classname', suite.get('name')), case.get('name'))
                if not all(identity) or identity in cases:
                    raise ValueError('Missing or duplicate test identity: ' + str(identity))
                cases[identity] = case
        if root.tag == 'testsuites':
            for key in totals:
                if count(root, key) != per_file[key]:
                    raise ValueError('Aggregate test count mismatch: ' + key)
    expected_classes = set()
    for source in source_path.rglob('*.java'):
        text = source.read_text()
        if re.search(r'^\s*@Test\b', text, re.M):
            package = re.search(r'^package ([\w.]+);', text, re.M)
            expected_classes.add(package[1] + '.' + source.stem)
    if not expected_classes:
        raise ValueError('No configured instrumentation test classes found.')
    missing = sorted(expected_classes - {identity[0] for identity in cases})
    required = {
        ('de.dennisguse.opentracks.TimezoneEnvironmentTest', 'instrumentationDefaultIsSingapore'),
        ('de.dennisguse.opentracks.TimezoneEnvironmentTest', 'emulatorSystemTimezoneIsSingapore'),
        ('de.dennisguse.opentracks.TimezoneEnvironmentTest', 'timezoneRuleRestoresPreviousDefaultOnFailure'),
        ('de.dennisguse.opentracks.services.TrackRecordingServicePreferenceLifecycleTest', 'callbackAfterDestroyDoesNotAccessReleasedManagers'),
        ('de.dennisguse.opentracks.services.TrackRecordingServicePreferenceLifecycleTest', 'queuedPreferenceSnapshotIsIgnoredAfterListenerUnregister'),
    }
    missing_required = sorted(f'{owner}.{name}' for owner, name in required
                              if (owner, name) not in cases or cases[(owner, name)].find('skipped') is not None)
    details = []
    for identity, case in cases.items():
        for failure in (*case.findall('failure'), *case.findall('error')):
            details.append('.'.join(identity) + '\n' + failure.get('message', '') + '\n' + (failure.text or ''))
    return {**totals, 'total': totals['tests'], 'executed': totals['tests'] - totals['skipped'],
            'missingClasses': missing, 'missingRequiredTests': missing_required, 'suites': suites,
            'failureDetails': details,
            'passed': totals['tests'] > totals['skipped'] and not totals['failures']
                      and not totals['errors'] and not missing and not missing_required}


def main():
    plan = json.loads(Path('release-plan.json').read_text())
    try:
        report = inspect(Path('build/outputs/androidTest-results'), Path('src/androidTest/java'))
    except (ValueError, ET.ParseError, OSError) as error:
        report = {'passed': False, 'diagnostic': str(error)}
    report['plan'] = plan
    folder = Path('build/reports/ci-validation')
    folder.mkdir(parents=True, exist_ok=True)
    (folder / 'instrumentation.json').write_text(json.dumps(report, indent=2) + '\n')
    text = '## Instrumentation report gate\n\n```json\n' + json.dumps(report, indent=2) + '\n```\n'
    print(text)
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as target: target.write(text)
    if not report['passed']:
        raise SystemExit('Instrumentation report gate failed.')


if __name__ == '__main__': main()
