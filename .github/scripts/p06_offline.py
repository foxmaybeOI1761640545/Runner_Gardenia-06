"""P06 adds one private map Activity; all original exact offline restrictions still apply."""
import verify_p0_offline as original

def enable_map_activity():
    original.COMPONENTS={**original.COMPONENTS,'activity':original.COMPONENTS['activity']|{original.PREFIX+'p0.map.P06MapActivity'}}

def verify():
    enable_map_activity();original.verify()
if __name__=='__main__':verify()
