"""Resolve resource-table paths instead of assuming source paths survive APK optimization."""
from pathlib import PurePosixPath
import subprocess
import zipfile


def read_packaged_xml(analyzer, apk, resource_name):
    raw = subprocess.check_output([
        str(analyzer), 'resources', 'value', '--config', 'default',
        '--name', resource_name, '--type', 'xml', str(apk)], text=True).strip()
    entry = raw.strip('"').lstrip('/')
    path = PurePosixPath(entry)
    if not entry.startswith('res/') or '..' in path.parts:
        raise RuntimeError('Resource table did not return a safe APK XML path.')
    with zipfile.ZipFile(apk) as archive:
        if entry not in archive.namelist():
            raise RuntimeError('Resolved XML resource is absent from the APK: ' + entry)
    print('Verified packaged XML resource:', resource_name, '->', entry)
    return subprocess.check_output([
        str(analyzer), 'resources', 'xml', '--file', entry, str(apk)], text=True).strip()
