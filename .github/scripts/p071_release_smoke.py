"""P0.7.0 fixed-key upgrade with all prior runtime assertions and current root navigation."""
import re
import time
from p071_version import VERSION
from p071_runtime import run
from p071_theme_ui import capture as themes
from p071_signed_back import save

def navigation_adapter(device,dump,tap):
    def focus():return next((r for r in device('shell','dumpsys','window').splitlines() if 'mCurrentFocus=' in r),'')
    def select(text):
        until=time.monotonic()+15
        while time.monotonic()<until:
            for node in dump().iter('node'):
                if node.get('text')==text:
                    b=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
                    if len(b)==4 and b[2]>b[0] and b[3]>b[1]:device('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(.3);return
            device('shell','input','swipe','360','1100','360','400','200');time.sleep(.2)
        raise ValueError('Native settings entry missing: '+text)
    def adapted_tap(identity):
        if identity!='p04_route':return tap(identity)
        if 'P0Activity' not in focus():raise ValueError('Full map must open from actual Home')
        tap('p03_settings');select('Privacy and offline');select('Offline map')
        until=time.monotonic()+15
        while time.monotonic()<until:
            if 'P06MapActivity' in focus():return
            time.sleep(.2)
        raise ValueError('Settings did not open the actual full map')
    def adapted_device(*args,**kwargs):
        if args==('shell','input','keyevent','4') and 'P06MapActivity' in focus():
            result=device(*args,**kwargs)
            until=time.monotonic()+15
            while time.monotonic()<until:
                if 'P0SettingsActivity' in focus():
                    # The actual internal map return is Settings; the next probe
                    # explicitly reselects Home, never relies on old tab history.
                    device('shell','input','keyevent','4');time.sleep(.3);tap('p04_nav_run');return result
                time.sleep(.2)
            raise ValueError('Map did not return to its Settings parent')
        return device(*args,**kwargs)
    return adapted_device,adapted_tap

def capture(device,dump,screenshot,tap,start,output):themes(device,dump,screenshot,tap,start,output,version=VERSION)

if __name__=='__main__':
    run(post_verify=capture,baseline_spec=('467f2086c0f712e04430382203da2bde0c870acb',6780,'3c6ad819e740f82b24063d3be6da9e259f69b959bb9a54c1145b6974b085beb7'),map_validation=True,capture_adapter=navigation_adapter,output_name='p071-release-smoke',preserve_gps_preferences=True)
    save()
