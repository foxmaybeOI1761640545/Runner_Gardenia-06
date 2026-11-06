"""Fail-closed recovery routing for the one already accepted P0.2 APK; no new signing."""
import json
import os
from pathlib import Path
import subprocess

BUILD='4454d716a5ae010b92d3eefa1ac1c0ebedde0063'
RUN=42251766485
FORMAL=13522176163
REPORTS=13521268691
FORMAL_ZIP='d577d196ae400878265dacaeaa9f0682fae93c9cfd9232bf90c755f42440618d'
REPORTS_ZIP='83a480c051849b806aab2bc5af9d4d0a0a4e781edb94d04fe17f4529a22c86a9'
APK_SHA='48c4a855402b7bec0b310794a228f15323ef8f09b2eb81d4f6913ab56ac6d046'
MARKER='[p02-existing-apk]'
ALLOWED={'.github/workflows/p02-gps-ui.yml','.github/scripts/publish_release.py','.github/scripts/p02_publish.py',
         '.github/scripts/p02_publication_scope.py','.github/scripts/p02_publish_existing.py',
         '.github/tests/test_p02_publication.py','.github/tests/test_p02_publication_scope.py',
         '.github/tests/test_github_media.py','BUILDING_P02.md'}


def publication_only(message,paths):
    if MARKER not in message:return False
    if not paths or any(p not in ALLOWED and not p.startswith('docs/') for p in paths):raise ValueError('Recovery cannot change product, version, signing, GPS/BLE or acceptance tests')
    return True


def context():
    if (os.environ['GITHUB_REPOSITORY']!='foxmaybeOI1761640545/Runner_Gardenia-06' or os.environ['GITHUB_REF']!='refs/heads/feature/p0-2-gps-ui'
            or os.environ['GITHUB_EVENT_NAME']!='push'):raise ValueError('Untrusted recovery event')
    subprocess.run(['git','merge-base','--is-ancestor',BUILD,'HEAD'],check=True)
    if subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()!=os.environ['GITHUB_SHA']:raise ValueError('Wrong recovery checkout')
    event=json.loads(Path(os.environ['GITHUB_EVENT_PATH']).read_text())
    paths=subprocess.check_output(['git','diff','--name-only',BUILD,'HEAD'],text=True).splitlines()
    return publication_only(event['head_commit']['message'],paths)


if __name__=='__main__':
    recovering=context()
    with open(os.environ['GITHUB_OUTPUT'],'a') as f:f.write('publication='+str(recovering).lower()+'\n')
    print('Recover the immutable accepted APK only; no Gradle/build/signing' if recovering else 'Normal full version/test/signing gates required')
