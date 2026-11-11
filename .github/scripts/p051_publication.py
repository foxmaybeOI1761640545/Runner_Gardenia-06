"""Only transfer the reviewed immutable signed APK; no build or signing here."""
from publish_release import GitHub
from p051_signing import inputs,bundle
from p051_publish import publish
if __name__=='__main__':
 inputs();publish(GitHub(),verify_bundle=bundle)
