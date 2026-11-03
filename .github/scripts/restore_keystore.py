"""Restore existing signing material without ever logging its contents."""
import base64
import os
from pathlib import Path

required = (
    'ANDROID_KEYSTORE_BASE64', 'ANDROID_KEYSTORE_PASSWORD',
    'ANDROID_KEY_ALIAS', 'ANDROID_KEY_PASSWORD', 'ANDROID_SIGNING_CERT_SHA256',
)
if any(not os.environ.get(name) for name in required):
    raise SystemExit('Required signing secrets or certificate fingerprint are missing.')
try:
    encoded = ''.join(os.environ['ANDROID_KEYSTORE_BASE64'].split())
    data = base64.b64decode(encoded, validate=True)
    if not data:
        raise ValueError()
except ValueError:
    raise SystemExit('Invalid keystore encoding.') from None
path = Path(os.environ['ANDROID_KEYSTORE_FILE'])
with path.open('xb') as target:
    target.write(data)
path.chmod(0o600)
