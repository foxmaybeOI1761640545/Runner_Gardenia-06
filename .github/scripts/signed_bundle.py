"""Bind the transferred, already verified APK to its tested plan before tagging.

The tag job has no signing credentials or SDK. It accepts only the same run's
signed-job artifact; that job independently checks the configured certificate.
A resumed signing job re-verifies the original bytes, rather than rebuilding.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

from release_policy import load, git, tag_plan, verify_tag_digest


def validate_bundle(plan, folder):
    info = json.loads((folder / 'build-info.json').read_text())
    if info.get('plan') != plan or any(info.get(key) != plan[key] for key in ('commit', 'versionName', 'versionCode')):
        raise SystemExit('Signed artifact differs from the exact tested plan.')
    if (info.get('applicationId') != 'com.fox.runner' or info.get('variant') != 'reproducibleRelease'
            or info.get('debuggable') is not False or info.get('signatureVerified') is not True
            or not re.fullmatch('[0-9A-F]{64}', info.get('certificateSha256', ''))):
        raise SystemExit('Signed artifact lacks the required APK identity/signature verification.')
    filename = f"runner-gardenia-{plan['versionName']}-{plan['versionCode']}.apk"
    if [p.name for p in folder.glob('*.apk')] != [filename]:
        raise SystemExit('Exactly the verified release APK is required.')
    checksum = hashlib.sha256((folder / filename).read_bytes()).hexdigest()
    if checksum != info.get('apkSha256') or (folder / 'SHA256SUMS').read_text() != f'{checksum}  {filename}\n':
        raise SystemExit('Signed APK or SHA256SUMS changed during transfer.')
    return info


def restore(plan, folder):
    # read-only token; never log token, keystore, or passwords. gh uses the normal
    # runner credential path and downloads only this run's immutable artifact.
    repo, run_id = os.environ['GITHUB_REPOSITORY'], os.environ['GITHUB_RUN_ID']
    if str(plan['runId']) != run_id:
        raise SystemExit('Cannot restore another run\'s signing artifact.')
    listing = json.loads(subprocess.check_output(
        ['gh', 'api', '--paginate', '--slurp', f'repos/{repo}/actions/runs/{run_id}/artifacts?per_page=100'], text=True))
    matches = [a for page in listing for a in page['artifacts'] if a['name'] == f'signed-apk-{run_id}']
    tagged = plan['versionName'] in git('tag', '--list').splitlines()
    if tagged and tag_plan(plan['versionName']) != plan:
        raise SystemExit('Existing version tag belongs to another run.')
    if len(matches) > 1 or (matches and matches[0]['expired']) or (tagged and not matches):
        raise SystemExit('Original signing artifact unavailable; refusing to rebuild an immutable tagged APK.')
    if matches:
        subprocess.run(['gh', 'run', 'download', run_id, '--repo', repo, '--name', f'signed-apk-{run_id}',
                        '--dir', str(folder)], check=True)
        validate_bundle(plan, folder)
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a') as out:
            out.write(f"restored={str(bool(matches)).lower()}\n")
    print('Restored original signed APK.' if matches else 'No signing artifact yet; build before creating a tag.')


def reverify(plan, folder):
    from publish_release import verify_apk
    info = validate_bundle(plan, folder)
    expected = os.environ.get('ANDROID_SIGNING_CERT_SHA256', '').strip().replace(':', '').upper()
    if not re.fullmatch('[0-9A-F]{64}', expected) or info['certificateSha256'] != expected:
        raise SystemExit('Restored APK certificate differs from independent Environment variable.')
    filename = f"runner-gardenia-{plan['versionName']}-{plan['versionCode']}.apk"
    verify_apk((folder / filename).read_bytes(), info)
    if plan['versionName'] in git('tag', '--list').splitlines():
        if tag_plan(plan['versionName']) != plan:
            raise SystemExit('Existing tag plan differs from restored APK.')
        verify_tag_digest(plan['versionName'], info['apkSha256'])
    print('Original signed APK identity, checksum and independent certificate verified.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=['restore', 'reverify'])
    parser.add_argument('--plan', default='release-plan.json')
    parser.add_argument('--folder', default='dist')
    args = parser.parse_args()
    (restore if args.command == 'restore' else reverify)(load(args.plan), Path(args.folder))
