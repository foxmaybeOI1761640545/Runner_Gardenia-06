"""Publish verified files through a resumable draft, never replace a published APK."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request


class SafeRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        redirected = super().redirect_request(req, fp, code, msg, headers, newurl)
        if redirected and urllib.parse.urlsplit(req.full_url).netloc != urllib.parse.urlsplit(newurl).netloc:
            redirected.remove_header('Authorization')
        return redirected


class GitHub:
    def __init__(self):
        self.token = os.environ['GH_TOKEN']
        self.repo = os.environ['GITHUB_REPOSITORY']
        self.base = 'https://api.github.com/repos/' + self.repo
        self.opener = urllib.request.build_opener(SafeRedirect())

    def request(self, url, method='GET', data=None, binary=False, optional=False):
        if not url.startswith('https://') or urllib.parse.urlsplit(url).hostname not in {'api.github.com', 'uploads.github.com'}:
            raise RuntimeError('Unexpected GitHub API destination.')
        headers = {'Authorization': 'Bearer ' + self.token, 'User-Agent': 'Runner-Gardenia-release',
                   'Accept': 'application/octet-stream' if binary else 'application/vnd.github+json',
                   'X-GitHub-Api-Version': '2022-11-28'}
        if isinstance(data, dict):
            data = json.dumps(data).encode()
            headers['Content-Type'] = 'application/json'
        elif data is not None:
            headers['Content-Type'] = 'application/octet-stream'
        for attempt in range(3):
            try:
                with self.opener.open(urllib.request.Request(url, data=data, headers=headers, method=method), timeout=90) as response:
                    content = response.read()
                    return content if binary else json.loads(content) if content else None
            except urllib.error.HTTPError as error:
                if optional and error.code == 404:
                    return None
                if method != 'GET' or error.code < 500 or attempt == 2:
                    raise RuntimeError(f'GitHub {method} failed with HTTP {error.code}') from None
            except (urllib.error.URLError, TimeoutError):
                if method != 'GET' or attempt == 2:
                    raise RuntimeError(f'GitHub {method} network request failed; rerun failed jobs to resume.') from None
            time.sleep(2 ** attempt)

    def release(self, tag):
        return self.request(self.base + '/releases/tags/' + urllib.parse.quote(tag, safe=''), optional=True)

    def assets(self, release):
        result = []
        page = 1
        while True:
            batch = self.request(release['assets_url'] + f'?per_page=100&page={page}')
            result.extend(batch)
            if len(batch) < 100:
                return result
            page += 1


def digest(data):
    return hashlib.sha256(data).hexdigest()


def verify_identity(info, expected):
    for field in ('commit', 'applicationId', 'versionName', 'versionCode', 'certificateSha256', 'variant'):
        if info.get(field) != expected[field]:
            raise RuntimeError('Existing release identity differs: ' + field)


def verify_apk(data, info):
    if digest(data) != info['apkSha256']:
        raise RuntimeError('APK checksum differs from verified build information.')
    temporary = Path(os.environ['RUNNER_TEMP']) / 'gardenia-publication.apk'
    temporary.write_bytes(data)
    try:
        tool = Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0/apksigner'
        output = subprocess.check_output([str(tool), 'verify', '--verbose', '--print-certs', str(temporary)], text=True)
        fingerprints = re.findall(r'^Signer #\d+ certificate SHA-256 digest:\s*([0-9a-fA-F]+)\s*$', output, re.M)
        if len(fingerprints) != 1 or fingerprints[0].upper() != info['certificateSha256']:
            raise RuntimeError('Published APK certificate differs from the independently verified signer.')
        analyzer = shutil.which('apkanalyzer')
        for field, expected in [('application-id', 'com.fox.runner'), ('version-name', info['versionName']),
                                ('version-code', str(info['versionCode'])), ('debuggable', 'false')]:
            actual = subprocess.check_output([analyzer, 'manifest', field, str(temporary)], text=True).strip()
            if actual != expected:
                raise RuntimeError('Published APK identity mismatch: ' + field)
    finally:
        temporary.unlink(missing_ok=True)


def notes(info):
    return (f"Runner Gardenia {info['versionName']} — 测试预发布版本\n\n"
            f"- Commit: `{info['commit']}`\n- Application ID: `{info['applicationId']}`\n"
            f"- Version Name: `{info['versionName']}`\n- Version Code: `{info['versionCode']}`\n"
            f"- Certificate SHA-256: `{info['certificateSha256']}`\n- APK SHA-256: `{info['apkSha256']}`\n"
            f"- Build: {info['run']}\n\n"
            "下载 APK 并核对 SHA256SUMS；Android 8.0 或以上可安装。可与官方 OpenTracks 并存。\n"
            "后续更新请保留应用，使用相同包名、固定签名和更高 Version Code；不要卸载以保留本地数据。\n")


def publish(api, folder):
    expected = json.loads((folder / 'build-info.json').read_text())
    plan = expected['plan']
    if plan['commit'] != expected['commit'] or plan['versionName'] != expected['versionName'] or plan['versionCode'] != expected['versionCode']:
        raise RuntimeError('Signed build information differs from tested version plan.')
    actual_tag = subprocess.check_output(['git', 'rev-parse', expected['versionName'] + '^{commit}'], text=True).strip()
    if actual_tag != expected['commit']:
        raise RuntimeError('Remote checkout tag differs from the tested source.')
    filename = f"runner-gardenia-{expected['versionName']}-{expected['versionCode']}.apk"
    if [p.name for p in folder.glob('*.apk')] != [filename]:
        raise RuntimeError('Exactly the expected signed APK is required.')
    local_apk = (folder / filename).read_bytes()
    verify_apk(local_apk, expected)
    local_sums = f"{expected['apkSha256']}  {filename}\n".encode()
    if (folder / 'SHA256SUMS').read_bytes() != local_sums:
        raise RuntimeError('Transferred SHA256SUMS is invalid.')
    release = api.release(expected['versionName'])
    if release is None:
        try:
            release = api.request(api.base + '/releases', 'POST', {
                'tag_name': expected['versionName'], 'target_commitish': expected['commit'],
                'name': 'Runner Gardenia ' + expected['versionName'], 'body': notes(expected),
                'draft': True, 'prerelease': True, 'make_latest': 'false'})
        except RuntimeError:
            # The server may have committed the draft before the connection was interrupted.
            release = api.release(expected['versionName'])
            if release is None:
                raise
    if release['tag_name'] != expected['versionName'] or not release['prerelease']:
        raise RuntimeError('Existing release is not the expected pre-release.')
    assets = {asset['name']: asset for asset in api.assets(release)}
    if set(assets) - {'build-info.json', filename, 'SHA256SUMS'}:
        raise RuntimeError('Release contains unexpected assets; refusing to modify it.')
    canonical = expected
    canonical_bytes = (folder / 'build-info.json').read_bytes()
    if 'build-info.json' in assets:
        canonical_bytes = api.request(assets['build-info.json']['url'], binary=True)
        canonical = json.loads(canonical_bytes)
        verify_identity(canonical, expected)
    elif assets:
        raise RuntimeError('Existing assets have no trusted source manifest; refusing overwrite.')
    if filename in assets:
        apk_data = api.request(assets[filename]['url'], binary=True)
        verify_apk(apk_data, canonical)
    else:
        if not release['draft']:
            raise RuntimeError('Published release is incomplete; refusing to replace assets.')
        apk_data = local_apk
        verify_apk(apk_data, canonical)
    payloads = {'build-info.json': canonical_bytes, filename: apk_data,
                'SHA256SUMS': f"{canonical['apkSha256']}  {filename}\n".encode()}
    for name, content in payloads.items():
        if name in assets:
            current = api.request(assets[name]['url'], binary=True)
            if digest(current) != digest(content):
                raise RuntimeError('Existing asset differs; refusing overwrite: ' + name)
        else:
            if not release['draft']:
                raise RuntimeError('Published release lacks a required asset.')
            upload = release['upload_url'].split('{')[0] + '?' + urllib.parse.urlencode({'name': name})
            api.request(upload, 'POST', content)
    if release['draft']:
        release = api.request(release['url'], 'PATCH', {
            'draft': False, 'prerelease': True, 'make_latest': 'false',
            'name': 'Runner Gardenia ' + expected['versionName'], 'body': notes(canonical)})
    # Read back every required asset after publication, not just the upload responses.
    final = api.release(expected['versionName'])
    if final['draft'] or not final['prerelease']:
        raise RuntimeError('Release publication was not confirmed.')
    final_assets = {asset['name']: asset for asset in api.assets(final)}
    for name, content in payloads.items():
        if name not in final_assets or digest(api.request(final_assets[name]['url'], binary=True)) != digest(content):
            raise RuntimeError('Published asset verification failed: ' + name)
    receipt = {'tag': expected['versionName'], 'commit': expected['commit'], 'versionCode': expected['versionCode'],
               'release': final['html_url'], 'apk': final_assets[filename]['browser_download_url'],
               'apkSha256': canonical['apkSha256'], 'certificateSha256': canonical['certificateSha256']}
    (folder / 'publication.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps(receipt, indent=2))
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as target:
            target.write('## Published pre-release\n\n```json\n' + json.dumps(receipt, indent=2) + '\n```\n')


if __name__ == '__main__':
    publish(GitHub(), Path('dist'))
