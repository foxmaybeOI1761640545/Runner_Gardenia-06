package de.dennisguse.opentracks.p0;
import static org.junit.Assert.*;
import org.junit.Test;
import de.dennisguse.opentracks.p0.recording.ForegroundMode;
public class ForegroundModeTest {
    @Test public void gpsWorksWhenBluetoothIsDenied() {assertEquals(ForegroundMode.GPS,ForegroundMode.select(true,false));}
    @Test public void bluetoothPreviewCanWorkWithoutGps() {assertEquals(ForegroundMode.BLUETOOTH,ForegroundMode.select(false,true));}
    @Test public void bothCapabilitiesKeepBothTypes() {assertEquals(ForegroundMode.GPS_AND_BLUETOOTH,ForegroundMode.select(true,true));}
    @Test public void noPermissionsMeansNoForegroundType() {assertEquals(ForegroundMode.NONE,ForegroundMode.select(false,false));}
}
