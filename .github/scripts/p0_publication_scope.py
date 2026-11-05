"""Route only an explicitly requested, unchanged-APK publication; product changes fail closed."""
import json
import os
from pathlib import Path
import subprocess

BUILD='3116e5fa7a56ce6f299ea45d5a45745f1aed4466'
MARKER='[p0-existing-apk]'
ALLOWED={'.github/workflows/p0-core.yml','.github/scripts/p0_publication_scope.py',
         '.github/scripts/p0_publish_existing.py','.github/tests/test_p0_publication_scope.py','AGENTS.md'}


def publication_only(message,paths):
    if MARKER not in message:return False
    if not paths or any(path not in ALLOWED and not path.startswith('docs/') for path in paths):
        raise ValueError('Existing-APK publication cannot include product, version, signing or test changes')
    return True


def main():
    if (os.environ['GITHUB_REPOSITORY']!='foxmaybeOI1761640545/Runner_Gardenia-06'
            or os.environ['GITHUB_REF']!='refs/heads/feature/p0-core-migration'
            or os.environ['GITHUB_EVENT_NAME']!='push'):raise ValueError('Wrong publication repository/ref/event')
    subprocess.run(['git','merge-base','--is-ancestor',BUILD,'HEAD'],check=True)
    if subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()!=os.environ['GITHUB_SHA']:raise ValueError('Wrong checkout SHA')
    event=json.loads(Path(os.environ['GITHUB_EVENT_PATH']).read_text())
    paths=subprocess.check_output(['git','diff','--name-only',BUILD,'HEAD'],text=True).splitlines()
    publish=publication_only(event['head_commit']['message'],paths)
    with open(os.environ['GITHUB_OUTPUT'],'a') as output:output.write('publication='+str(publish).lower()+'\n')
    print('Existing verified APK only; no Gradle/build/signing job required.' if publish else 'Normal complete P0 build/test gates required.')


if __name__=='__main__':main()
