"""Actual fixed-key P0.3 -> refinement upgrade; no new build or test APK on this device."""
from p0_release_smoke import run
from p04_release_theme_ui import capture
from p04_version import BASELINE_SHA
if __name__=='__main__':
 run(p02=True,p03=True,post_verify=capture,baseline_spec=('77ef053b76fd5e7c6c0c9ce1023e365fa3d7c471',6774,BASELINE_SHA))
