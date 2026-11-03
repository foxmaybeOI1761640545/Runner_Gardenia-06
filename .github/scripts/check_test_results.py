"""A successful no-test Gradle task does not satisfy instrumentation coverage."""
from pathlib import Path
import xml.etree.ElementTree as ET

files = list(Path('build/outputs/androidTest-results').rglob('TEST-*.xml'))
executed = failures = skipped = 0
for path in files:
    root = ET.parse(path).getroot()
    suites = [root] if root.tag == 'testsuite' else root.findall('.//testsuite')
    for suite in suites:
        executed += int(suite.get('tests', 0))
        skipped += int(suite.get('skipped', 0))
        failures += int(suite.get('failures', 0)) + int(suite.get('errors', 0))
print(f'Instrumentation results: total={executed}, skipped={skipped}, failures/errors={failures}')
if executed - skipped <= 0 or failures:
    raise SystemExit('Instrumentation tests must actually execute and pass.')
