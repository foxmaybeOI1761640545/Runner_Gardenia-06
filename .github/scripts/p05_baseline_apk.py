"""Independently verify published P0.4 bytes. Never build, sign, tag or publish."""
import hashlib
import json
from pathlib import Path
import urllib.request

from p05_pipeline import protected_refs, trusted, write_report
from publish_release import GitHub, verify_apk


def verify_metadata(release, expected):
    if (release['tag_name'] != expected['tag'] or release['draft']
            or release['prerelease'] is not True or release['target_commitish'] != expected['source']):
        raise ValueError('P0.4 public Release identity changed')
    matches = [asset for asset in release['assets'] if asset['name'] == 'RunnerGardenia-P0-Release.apk']
    if len(matches) != 1:
        raise ValueError('P0.4 APK asset is missing or ambiguous')
    asset = matches[0]
    if (asset['state'] != 'uploaded' or asset['size'] != expected['apkBytes']
            or asset.get('digest') != 'sha256:' + expected['apkSha256']):
        raise ValueError('Public P0.4 asset metadata changed')
    return asset


def run():
    trusted()
    expected = protected_refs()['p04']
    api = GitHub()
    release = api.request(api.base + '/releases/tags/' + expected['tag'])
    asset = verify_metadata(release, expected)
    # This request has no Authorization header. GitHub's asset API redirects to
    # the public binary, avoiding stale cached browser-download gateway errors.
    request = urllib.request.Request(asset['url'], headers={
        'Accept': 'application/octet-stream', 'User-Agent': 'Runner-Gardenia-P05-baseline-audit'})
    with urllib.request.urlopen(request, timeout=120) as response:
        if response.status != 200:
            raise ValueError('Published P0.4 APK is not publicly downloadable')
        raw = response.read()
    if len(raw) != expected['apkBytes'] or hashlib.sha256(raw).hexdigest() != expected['apkSha256']:
        raise ValueError('Downloaded P0.4 bytes differ from immutable baseline')
    verify_apk(raw, expected)
    proof = {**expected, 'passed': True, 'publicDownloadAuthenticated': False,
             'publicDownloadUrl': asset['url'], 'signatureVerifiedBy': 'apksigner',
             'identityVerifiedBy': 'apkanalyzer', 'oldApkRebuilt': False,
             'oldApkSignedAgain': False, 'releaseModified': False}
    write_report('p04-apk.json', proof)
    print(json.dumps(proof, indent=2))


if __name__ == '__main__':
    run()
