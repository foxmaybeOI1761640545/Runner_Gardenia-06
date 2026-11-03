"""Inspect complete instrumentation XML reports, including failures and skipped tests."""
import json
import os
from pathlib import Path
import xml.etree.ElementTree as ET

files = sorted(Path('build/outputs/androidTest-results').rglob('TEST-*.xml'))
executed = failures = skipped = 0
suites = []
details = []
classes = set()
for path in files:
    root = ET.parse(path).getroot()
    for suite in ([root] if root.tag == 'testsuite' else root.findall('.//testsuite')):
        tests = int(suite.get('tests', 0))
        ignored = int(suite.get('skipped', 0))
        errors = int(suite.get('failures', 0)) + int(suite.get('errors', 0))
        executed += tests
        skipped += ignored
        failures += errors
        suites.append({'name': suite.get('name', ''), 'tests': tests, 'skipped': ignored, 'failures': errors})
        for case in suite.findall('testcase'):
            classes.add(case.get('classname', suite.get('name', '')))
            for failure in (*case.findall('failure'), *case.findall('error')):
                details.append(f"{case.get('classname')}.{case.get('name')}\n{failure.get('message', '')}\n{failure.text or ''}")
required = {
    'de.dennisguse.opentracks.TimezoneEnvironmentTest',
    'de.dennisguse.opentracks.services.TrackRecordingServicePreferenceLifecycleTest',
    'de.dennisguse.opentracks.io.file.exporter.KMLTrackExporterTest',
    'de.dennisguse.opentracks.io.file.importer.ExportImportTest',
    'de.dennisguse.opentracks.share.TrackShareSummaryGeneratorTest',
}
missing = sorted(required - classes)
report = {'total': executed, 'executed': executed - skipped, 'skipped': skipped,
          'failuresAndErrors': failures, 'missingRequiredClasses': missing,
          'suites': suites, 'failureDetails': details}
folder = Path('build/reports/ci-validation')
folder.mkdir(parents=True, exist_ok=True)
(folder / 'instrumentation.json').write_text(json.dumps(report, indent=2) + '\n')
summary = [f'## Instrumentation XML report\n\nTotal: {executed}; executed: {executed - skipped}; skipped: {skipped}; failures/errors: {failures}.\n',
           'Runtime timezone is checked by `TimezoneEnvironmentTest.instrumentationDefaultIsSingapore` (Asia/Singapore, UTC+08:00).\n',
           '| Suite | Tests | Skipped | Failures/errors |\n|---|---:|---:|---:|']
for suite in suites:
    summary.append(f"| {suite['name']} | {suite['tests']} | {suite['skipped']} | {suite['failures']} |")
if missing:
    summary.append('\nMissing required test classes: ' + ', '.join(missing))
for detail in details:
    summary.append('\n```text\n' + detail[:16000] + '\n```')
text = '\n'.join(summary) + '\n'
print(text)
if os.environ.get('GITHUB_STEP_SUMMARY'):
    with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as target:
        target.write(text)
if executed - skipped <= 0 or failures or missing:
    raise SystemExit('Instrumentation tests must actually execute and pass, including regression tests.')
