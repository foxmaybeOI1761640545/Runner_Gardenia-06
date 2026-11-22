"""Actual fixed-key P0.6.1 upgrade; native navigation adapted to the interactive P0.7 home map."""
import re,time
from pathlib import Path
from p07_version import VERSION
from p061_release_smoke import run
from p061_release_theme_ui import capture as themes

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
        raise ValueError('Actual P0.7 settings entry missing: '+text)
    def adapted_tap(identity):
        if identity!='p04_route':return tap(identity)
        if 'P0Activity' not in focus():raise ValueError('Full map must be opened from the actual home')
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
                current=focus()
                if 'P0Activity' in current:return result
                if 'P0SettingsActivity' in current:device('shell','input','keyevent','4');time.sleep(.3)
                else:time.sleep(.2)
            raise ValueError('Native map/settings back stack did not return to home')
        return device(*args,**kwargs)
    return adapted_device,adapted_tap

def capture(device,dump,screenshot,tap,start,output):themes(device,dump,screenshot,tap,start,output,version=VERSION)

if __name__=='__main__':
    run(post_verify=capture,baseline_spec=('6cee1958a23bfd1a1fe3b6a9b547717c528cbc75',6779,'b6df2f1ad0431c5a8ccd805209d7452171eb5fdbd611bc2b8f3f4d9bcf4d8e11'),
        map_validation=True,capture_adapter=navigation_adapter,output_name='p07-release-smoke',preserve_gps_preferences=True)
