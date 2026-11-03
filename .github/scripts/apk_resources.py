"""Resolve optimized APK paths and compare typed resource IDs, not decoded XML text."""
from pathlib import PurePosixPath
import re
import subprocess
import zipfile


def resolve_packaged_xml(analyzer, apk, resource_name):
    raw = subprocess.check_output([
        str(analyzer), 'resources', 'value', '--config', 'default',
        '--name', resource_name, '--type', 'xml', str(apk)], text=True).strip()
    entry = raw.strip('"').lstrip('/')
    if not entry.startswith('res/') or '..' in PurePosixPath(entry).parts:
        raise RuntimeError('Resource table did not return a safe APK XML path.')
    with zipfile.ZipFile(apk) as archive:
        if entry not in archive.namelist():
            raise RuntimeError('Resolved XML resource is absent from the APK: ' + entry)
    return entry


def check_binding_dumps(resources, tree, expected_package):
    application_ids = set()
    values = set()
    current = False
    for line in resources.splitlines():
        header = re.match(r'\s*resource (0x[0-9a-fA-F]+) (?:\S+:)?(\w+/\S+)', line)
        if header:
            current = header[2] == 'string/applicationId'
            if current: application_ids.add(int(header[1], 16))
        elif current:
            value = re.fullmatch(r'\s*\([^)]*\)\s+"([^"]*)"\s*', line)
            if value: values.add(value[1])
    if len(application_ids) != 1 or values != {expected_package}:
        raise RuntimeError('The applicationId resource does not resolve uniquely to the expected package.')
    attributes = re.findall(
        r'^\s*A:\s+(?:http://schemas.android.com/apk/res/android|android):targetPackage'
        r'\(0x[0-9a-fA-F]+\)=(.+)$', tree, re.M)
    if not attributes:
        raise RuntimeError('Packaged shortcuts contain no Android targetPackage binding.')
    expected_id = next(iter(application_ids))
    for value in attributes:
        reference = re.fullmatch(r'@0x([0-9a-fA-F]+)', value.strip())
        literal = re.fullmatch(r'"([^"]*)"(?:\s+\(Raw:.*\))?', value.strip())
        if reference and int(reference[1], 16) == expected_id:
            continue
        if literal and literal[1] == expected_package:
            continue
        raise RuntimeError('Packaged shortcut targets a different application resource/package.')
    return expected_id


def verify_shortcut_binding(analyzer, aapt2, apk, expected_package):
    entry = resolve_packaged_xml(analyzer, apk, 'shortcuts')
    resources = subprocess.check_output([str(aapt2), 'dump', 'resources', str(apk)], text=True)
    tree = subprocess.check_output([str(aapt2), 'dump', 'xmltree', str(apk), '--file', entry], text=True)
    resource_id = check_binding_dumps(resources, tree, expected_package)
    print(f'Verified shortcuts: {entry}; target resource 0x{resource_id:08x} -> {expected_package}')
