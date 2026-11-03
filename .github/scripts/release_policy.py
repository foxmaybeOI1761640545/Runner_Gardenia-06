"""Validate explicit versions; signed builds additionally require a trusted exact tag."""
import os
import re
import subprocess
import sys


def git(*args):
    return subprocess.check_output(['git', *args], text=True).strip()


def properties(text):
    return dict(line.strip().split('=', 1) for line in text.splitlines()
                if line.strip() and not line.lstrip().startswith('#'))


with open('version.properties') as source:
    version = properties(source.read())
name = version['VERSION_NAME']
code = int(version['VERSION_CODE'])
if not re.fullmatch(r'v\d+\.\d+\.\d+(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?', name):
    raise SystemExit('Invalid explicit version name.')
if not 0 < code <= 2100000000:
    raise SystemExit('Invalid Android version code.')
print(f'Expected APK: com.fox.runner, versionName={name}, versionCode={code}')
ref = os.environ.get('GITHUB_REF', '')
if ref.startswith('refs/tags/') and ref != 'refs/tags/' + name:
    raise SystemExit('Tag must exactly match VERSION_NAME.')
if '--signed' in sys.argv:
    if ref != 'refs/tags/' + name:
        raise SystemExit('Signed builds require an existing exact version tag, including manual builds.')
    head = git('rev-parse', 'HEAD')
    if git('rev-parse', f'{ref}^{{commit}}') != head:
        raise SystemExit('Tag does not point at the checked-out commit.')
    subprocess.run(['git', 'merge-base', '--is-ancestor', head, 'origin/main'], check=True)
    for tag in git('tag', '--list', 'v*').splitlines():
        if tag == name:
            continue
        # Only older tags on this lineage constrain the version code.
        ancestor = subprocess.run(['git', 'merge-base', '--is-ancestor', tag, head],
                                  stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if ancestor.returncode == 1:
            continue
        if ancestor.returncode != 0:
            raise SystemExit('Cannot establish prior release ancestry.')
        result = subprocess.run(['git', 'show', f'{tag}:version.properties'],
                                capture_output=True, text=True)
        if result.returncode:
            raise SystemExit('Prior version tag lacks explicit version metadata; review required.')
        previous = properties(result.stdout)
        if code <= int(previous['VERSION_CODE']):
            raise SystemExit('VERSION_CODE must exceed every preceding release on this lineage.')
