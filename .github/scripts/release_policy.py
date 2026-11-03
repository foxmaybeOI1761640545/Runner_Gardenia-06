"""Allocate one immutable version plan, then tag only the exact tested commit."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess

MARKER = 'Runner-Gardenia-Plan: '
APK_MARKER = 'Runner-Gardenia-APK-SHA256: '
NAME = re.compile(r'(v\d+\.\d+\.\d+-rc\d+\.)(\d+)')


def git(*args):
    return subprocess.check_output(['git', *args], text=True).strip()


def properties(text):
    return dict(line.strip().split('=', 1) for line in text.splitlines()
                if line.strip() and not line.lstrip().startswith('#'))


def validate(plan):
    if not NAME.fullmatch(plan['versionName']) or not 0 < plan['versionCode'] <= 2100000000:
        raise SystemExit('Invalid release version plan.')
    if not re.fullmatch('[0-9a-f]{40}', plan['commit']):
        raise SystemExit('Invalid planned commit.')


def tag_plan(tag):
    message = git('for-each-ref', '--format=%(contents)', 'refs/tags/' + tag)
    for line in message.splitlines():
        if line.startswith(MARKER):
            plan = json.loads(line[len(MARKER):])
            validate(plan)
            if plan['versionName'] != tag or git('rev-parse', tag + '^{commit}') != plan['commit']:
                raise SystemExit('Tag annotation disagrees with its target or version.')
            return plan
    # Keep legacy tags, including the existing rc1.1, completely unchanged.
    previous = properties(git('show', tag + ':version.properties'))
    return {'versionName': tag, 'versionCode': int(previous['VERSION_CODE']),
            'commit': git('rev-parse', tag + '^{commit}'), 'runId': None}


def load(path):
    plan = json.loads(Path(path).read_text())
    validate(plan)
    if git('rev-parse', 'HEAD') != plan['commit']:
        raise SystemExit('Checkout differs from the planned/tested commit.')
    return plan


def prepare(output):
    seed = properties(Path('version.properties').read_text())
    match = NAME.fullmatch(seed['VERSION_NAME'])
    if not match:
        raise SystemExit('version.properties must define a vX.Y.Z-rcN.M series.')
    head = git('rev-parse', 'HEAD')
    if os.environ.get('GITHUB_SHA') and os.environ['GITHUB_SHA'] != head:
        raise SystemExit('Checkout differs from the workflow event SHA.')
    publishing = (os.environ.get('GITHUB_EVENT_NAME') == 'push'
                  and os.environ.get('GITHUB_REF') == 'refs/heads/main'
                  and os.environ.get('GITHUB_REPOSITORY') == 'foxmaybeOI1761640545/Runner_Gardenia-06')
    run_id = os.environ.get('GITHUB_RUN_ID', 'local')
    previous = [tag_plan(tag) for tag in git('tag', '--list', 'v*').splitlines()]
    resumed = [item for item in previous if item.get('runId') == run_id]
    if resumed:
        if len(resumed) != 1 or resumed[0]['commit'] != head:
            raise SystemExit('This run already owns an incompatible tag.')
        plan = resumed[0]
    else:
        series = match[1]
        sequence = max([int(match[2]) - 1] + [int(NAME.fullmatch(p['versionName'])[2])
                       for p in previous if NAME.fullmatch(p['versionName'])
                       and NAME.fullmatch(p['versionName'])[1] == series]) + 1
        code = max([int(seed['VERSION_CODE'])] + [p['versionCode'] + 1 for p in previous])
        plan = {'versionName': series + str(sequence), 'versionCode': code,
                'commit': head, 'runId': run_id, 'publishing': publishing}
    if bool(plan.get('publishing')) != publishing:
        raise SystemExit('Release plan does not match this event.')
    validate(plan)
    Path(output).write_text(json.dumps(plan, indent=2) + '\n')
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a') as target:
            target.write(f"publishing={str(publishing).lower()}\nversion={plan['versionName']}\n")
    print(json.dumps(plan, indent=2))


def apply(path):
    plan = load(path)
    Path('version.properties').write_text(
        f"VERSION_NAME={plan['versionName']}\nVERSION_CODE={plan['versionCode']}\n"
        f"VERSION_NAME_FULL={plan['versionName']}-{plan['commit'][:12]}\n")
    print(f"Applied {plan['versionName']} / {plan['versionCode']} at {plan['commit']}")


def create_tag(path, gate_path, signed_path='dist'):
    from signed_bundle import validate_bundle
    plan = load(path)
    if not plan['publishing'] or os.environ.get('GITHUB_REF') != 'refs/heads/main':
        raise SystemExit('Only main push runs can publish.')
    gate = json.loads(Path(gate_path).read_text())
    if gate['plan'] != plan or not gate['passed']:
        raise SystemExit('Missing successful test gate for this exact plan.')
    signed = validate_bundle(plan, Path(signed_path))
    subprocess.run(['git', 'fetch', 'origin', 'main', '--tags'], check=True)
    subprocess.run(['git', 'merge-base', '--is-ancestor', plan['commit'], 'origin/main'], check=True)
    tag = plan['versionName']
    tags = git('tag', '--list', 'v*').splitlines()
    if tag in tags:
        if tag_plan(tag) != plan:
            raise SystemExit('Existing tag belongs to another commit/run; never move it.')
        verify_tag_digest(tag, signed['apkSha256'])
        print('Reusing this run\'s exact immutable tag and APK digest.')
        return
    for other in tags:
        if tag_plan(other)['versionCode'] >= plan['versionCode']:
            raise SystemExit('A newer version was allocated concurrently; refusing duplicate code.')
    message = ('Runner Gardenia ' + tag + '\n\n' + MARKER + json.dumps(plan, sort_keys=True)
               + '\n' + APK_MARKER + signed['apkSha256'])
    subprocess.run(['git', 'tag', '-a', tag, plan['commit'], '-m', message], check=True)
    subprocess.run(['git', 'push', 'origin', 'refs/tags/' + tag], check=True)


def verify_tag(path):
    plan = load(path)
    if tag_plan(plan['versionName']) != plan:
        raise SystemExit('Signed build requires the exact tested immutable tag plan.')


def verify_tag_digest(tag, checksum):
    message = git('for-each-ref', '--format=%(contents)', 'refs/tags/' + tag)
    recorded = [line[len(APK_MARKER):] for line in message.splitlines() if line.startswith(APK_MARKER)]
    if recorded != [checksum]:
        raise SystemExit('Immutable tag APK digest differs or is missing.')



if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=['prepare', 'apply', 'tag', 'verify-tag'])
    parser.add_argument('--plan', default='release-plan.json')
    parser.add_argument('--gate', default='gate/instrumentation.json')
    parser.add_argument('--signed', default='dist')
    args = parser.parse_args()
    if args.command == 'prepare': prepare(args.plan)
    elif args.command == 'apply': apply(args.plan)
    elif args.command == 'tag': create_tag(args.plan, args.gate, args.signed)
    else: verify_tag(args.plan)
