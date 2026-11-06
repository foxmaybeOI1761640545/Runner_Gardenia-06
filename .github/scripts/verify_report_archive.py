"""Validate exact historical evidence and report links; never runs Android or signs."""
import hashlib
import json
from pathlib import Path
import re
import struct
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
EVIDENCE = ROOT / 'docs/reports/evidence'


def verify(remote=False):
    manifests = sorted((EVIDENCE / 'artifacts').glob('*/manifest.json'))
    if not manifests:
        raise ValueError('No recovered Artifact manifests')
    seen = set()
    entries = 0
    for manifest in manifests:
        data = json.loads(manifest.read_text())
        if data['artifact'].get('digest') != 'sha256:' + data['archiveSha256']:
            raise ValueError('Unverified source archive: ' + str(manifest))
        for row in data['files']:
            p = (EVIDENCE / row['object']).resolve()
            if not p.is_relative_to(EVIDENCE / 'objects') or not p.is_file():
                raise ValueError('Missing/unsafe original evidence: ' + row['object'])
            raw = p.read_bytes()
            if len(raw) != row['bytes'] or hashlib.sha256(raw).hexdigest() != row['sha256']:
                raise ValueError('Evidence bytes changed: ' + row['object'])
            if p.suffix == '.png':
                if raw[:8] != b'\x89PNG\r\n\x1a\n' or min(struct.unpack('>II', raw[16:24])) < 100:
                    raise ValueError('Invalid source PNG')
            elif p.suffix == '.xml':
                ET.fromstring(raw)
            elif p.suffix == '.json':
                json.loads(raw)
            if re.search(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|gh[pousr]_[A-Za-z0-9]{30,}', raw):
                raise ValueError('Sensitive source must not be archived: ' + row['object'])
            entries += 1
            seen.add(p)
    if seen != {p.resolve() for p in (EVIDENCE / 'objects').rglob('*') if p.is_file()}:
        raise ValueError('Unindexed archive objects')
    for row in json.loads((EVIDENCE / 'git-inventory.json').read_text())['files']:
        p = ROOT / row['path']
        # AGENTS is intentionally updated for the new task; original is retained in Git.
        if p.name != 'AGENTS.md' and hashlib.sha256(p.read_bytes()).hexdigest() != row['sha256']:
            raise ValueError('Original report/resource changed: ' + row['path'])
    broken = []
    for p in (ROOT / 'docs/reports').rglob('*.md'):
        if 'codex-local' in p.parts or 'releases' in p.parts:
            continue  # Immutable historical originals retain their original link context.
        for link in re.findall(r'\[[^\]]*\]\(([^)]+)\)', p.read_text()):
            if link.startswith(('https://', 'http://', '#')):
                continue
            target = (p.parent / link.split('#', 1)[0]).resolve()
            if not target.is_relative_to(ROOT) or not target.exists():
                broken.append(str(p.relative_to(ROOT)) + ': ' + link)
    if broken:
        raise ValueError('Broken archive links: ' + repr(broken))
    for row in json.loads((EVIDENCE / 'codex-local/provenance.json').read_text()):
        if hashlib.sha256((ROOT / row['path']).read_bytes()).hexdigest() != row['sha256']:
            raise ValueError('Codex-only original altered: ' + row['path'])
    releases = list((EVIDENCE / 'releases').glob('*/verified-assets.json'))
    if len(releases) != 4:
        raise ValueError('Missing historical Release verification')
    for path in releases:
        for asset in json.loads(path.read_text())['verifiedAssets']:
            if asset['retainedInGit']:
                raw = (path.parent / asset['name']).read_bytes()
                if len(raw) != asset['bytes'] or hashlib.sha256(raw).hexdigest() != asset['sha256']:
                    raise ValueError('Release document/checksum changed')
            elif not asset['signatureVerification']['verified']:
                raise ValueError('Unverified historical APK')
    counts = json.loads((EVIDENCE / 'xml-counts.json').read_text())
    for kind, expected in [('Android', 379), ('JVM', 88)]:
        rows = [r for r in counts if r['runId'] == '41974364095' and r['kind'] == kind]
        if len(rows) != 1 or rows[0]['tests'] != expected or rows[0]['failures'] or rows[0]['errors']:
            raise ValueError('Accepted P0.1 raw XML not confirmed: ' + kind)
    current = dict(line.split('\t')[::-1] for line in subprocess.check_output(
        ['git', 'ls-remote', '--heads', '--tags', 'origin'], cwd=ROOT, text=True).splitlines())
    for line in (EVIDENCE / 'protected-refs.txt').read_text().splitlines():
        sha, ref = line.split('\t')
        if current.get(ref) != sha:
            raise ValueError('Protected historical ref changed: ' + ref)
    result = {'passed': True, 'artifacts': len(manifests), 'originalMembers': entries,
              'uniqueObjects': len(seen), 'protectedRefsUnchanged': True}
    if remote:
        branch = 'refs/remotes/origin/feature/p0-2-gps-ui'
        subprocess.run(['git', 'fetch', 'origin',
                        '+refs/heads/feature/p0-2-gps-ui:' + branch], cwd=ROOT, check=True)
        paths = subprocess.check_output(['git', 'ls-files', 'docs/reports', 'docs/screenshots',
                                         'docs/p0-evidence'], cwd=ROOT, text=True).splitlines()
        # Blob hashes include every report, screenshot, manifest and checksum in these trees.
        tree = dict((line.split('\t', 1)[1], line.split()[2]) for line in subprocess.check_output(
            ['git', 'ls-tree', '-r', branch], cwd=ROOT, text=True).splitlines())
        for name in paths:
            actual = subprocess.check_output(['git', 'hash-object', name], cwd=ROOT, text=True).strip()
            if tree.get(name) != actual:
                raise ValueError('Remote bytes differ/missing: ' + name)
        result['remoteFilesVerified'] = len(paths)
        result['remoteCommit'] = subprocess.check_output(['git', 'rev-parse', branch], cwd=ROOT, text=True).strip()
    print(json.dumps(result, indent=2))
    return result


if __name__ == '__main__':
    import sys
    verify('--remote' in sys.argv)
