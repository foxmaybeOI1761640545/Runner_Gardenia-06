"""Validate explicit versions for CI, automatic tag promotion, and signed release builds."""
import os
import re
import subprocess
import sys


def git(*args):
    return subprocess.check_output(['git', *args], text=True).strip()


def properties(text):
    return dict(line.strip().split('=', 1) for line in text.splitlines()
                if line.strip() and not line.lstrip().startswith('#'))


def validate_previous_version_codes(head, current_name, current_code):
    for tag in git('tag', '--list', 'v*').splitlines():
        if tag == current_name:
            continue
        ancestor = subprocess.run(['git', 'merge-base', '--is-ancestor', tag, head],
                                  stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if ancestor.returncode == 1:
            continue
        if ancestor.returncode != 0:
            raise SystemExit(f'Cannot establish release ancestry for {tag}.')
        result = subprocess.run(['git', 'show', f'{tag}:version.properties'],
                                capture_output=True, text=True)
        if result.returncode:
            raise SystemExit(f'Prior version tag {tag} lacks version.properties; review required.')
        previous = properties(result.stdout)
        previous_code = int(previous['VERSION_CODE'])
        if current_code <= previous_code:
            raise SystemExit(
                f'VERSION_CODE {current_code} must exceed preceding release {tag} ({previous_code}).'
            )


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

if '--promote' in sys.argv:
    if ref != 'refs/heads/main':
        raise SystemExit('Automatic version tags may only be created from main.')
    head = git('rev-parse', 'HEAD')
    if head != os.environ.get('GITHUB_SHA'):
        raise SystemExit('The tag candidate must be the exact commit that passed main CI.')
    subprocess.run(['git', 'merge-base', '--is-ancestor', head, 'origin/main'], check=True)
    validate_previous_version_codes(head, name, code)
    print(f'{name} is eligible for automatic tagging.')

if '--signed' in sys.argv:
    if ref != 'refs/tags/' + name:
        raise SystemExit('Signed builds require an exact version tag.')
    head = git('rev-parse', 'HEAD')
    if git('rev-parse', f'{ref}^{{commit}}') != head:
        raise SystemExit('Tag does not point at the checked-out commit.')
    subprocess.run(['git', 'merge-base', '--is-ancestor', head, 'origin/main'], check=True)
    validate_previous_version_codes(head, name, code)
    print(f'{name} passed signed-release version policy.')
