"""Exercise actual optimized-APK root Back, then explicitly reopen Home for the next probe."""
import json
import time
from pathlib import Path

PACKAGE='com.fox.runner'
checks=[]

def focus(device):
    return next((line.strip() for line in device('shell','dumpsys','window').splitlines() if 'mCurrentFocus=' in line),'')

def root_back_home(device):
    before=focus(device)
    if not any(name in before for name in ('P0Activity','P0HistoryActivity','P0SensorsActivity','P0SettingsActivity')):raise ValueError('Root Back probe requires a real top-level Activity')
    device('shell','input','keyevent','4')
    until=time.monotonic()+15
    while time.monotonic()<until:
        after=focus(device)
        if after and PACKAGE not in after and 'mCurrentFocus=null' not in after:break
        time.sleep(.2)
    else:raise ValueError('Root Back revealed another application page instead of backgrounding the task')
    # API35 can leave `am start -W` waiting for a launch-drawn event when
    # REORDER_TO_FRONT resumes an existing Activity that is already drawn.
    # Issue the start without the shell's launch wait, then independently
    # require the actual Home window below. Do not replace the Back probe.
    device('shell','am','start','-f','0x30020000','-n',PACKAGE+'/de.dennisguse.opentracks.p0.app.P0Activity')
    until=time.monotonic()+15
    while time.monotonic()<until:
        if 'P0Activity' in focus(device):break
        time.sleep(.2)
    else:raise ValueError('Explicit Home reselect did not restore the task')
    checks.append({'before':before,'afterRootBack':after,'backgrounded':True,'homeExplicitlyReselected':True})

def save():
    info=json.loads(Path('dist/build-info.json').read_text())
    if len(checks)<30:raise ValueError('Missing repeated signed root Back checks across themes')
    Path('dist/p071-signed-navigation.json').write_text(json.dumps({'passed':True,'source':info['commit'],'apkSha256':info['apkSha256'],'rootBackChecks':checks,'physicalDevices':'NEEDS_DEVICE_VALIDATION'},indent=2)+'\n')
