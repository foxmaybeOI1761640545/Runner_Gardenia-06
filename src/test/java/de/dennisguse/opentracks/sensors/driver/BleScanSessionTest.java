package de.dennisguse.opentracks.sensors.driver;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.List;

public class BleScanSessionTest {
    @Test public void scanAcceptsOnlyCurrentResults() { BleScanSession s=new BleScanSession();int first=s.begin(true,true);int second=s.begin(true,true);assertFalse(s.accepts(first));assertTrue(s.accepts(second)); }
    @Test public void timeoutFinishesScan() { BleScanSession s=new BleScanSession();int t=s.begin(true,true);assertTrue(s.complete(t));assertFalse(s.accepts(t));assertEquals(BleScanSession.State.COMPLETE,s.state()); }
    @Test public void cancelRejectsLateCallbacks() { BleScanSession s=new BleScanSession();int t=s.begin(true,true);s.cancel();assertFalse(s.fail(t));assertFalse(s.accepts(t)); }
    @Test public void rejectedPermissionNeverScans() { BleScanSession s=new BleScanSession();int t=s.begin(false,true);assertFalse(s.accepts(t));assertEquals(BleScanSession.State.PERMISSION_DENIED,s.state()); }
    @Test public void powerOffNeverScans() { BleScanSession s=new BleScanSession();int t=s.begin(true,false);assertFalse(s.accepts(t));assertEquals(BleScanSession.State.BLUETOOTH_OFF,s.state()); }
    @Test public void failedScanCanBeRestarted() { BleScanSession s=new BleScanSession();int t=s.begin(true,true);assertTrue(s.fail(t));assertTrue(s.accepts(s.begin(true,true))); }
    @Test public void permissionsRemainMinimalAcrossAndroidVersions() {
        assertEquals(List.of("android.permission.ACCESS_FINE_LOCATION"),BluetoothPermissionPolicy.forApi(30));
        assertEquals(List.of("android.permission.BLUETOOTH_SCAN","android.permission.BLUETOOTH_CONNECT"),BluetoothPermissionPolicy.forApi(31));
        assertEquals(BluetoothPermissionPolicy.forApi(31),BluetoothPermissionPolicy.forApi(35));
    }
}
