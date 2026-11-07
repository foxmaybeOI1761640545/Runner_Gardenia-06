"""Reuse the accepted signed/upgrade/GPX suite, then capture the actual signed app's eight schemes."""
from p0_release_smoke import run
from p03_release_theme_ui import capture
if __name__=='__main__':run(p02=True,p03=True,post_verify=capture)
