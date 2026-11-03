package de.dennisguse.opentracks.settings.bluetooth;

import static org.junit.Assert.*;
import org.junit.Test;

public class BluetoothLeAdapterTest {
    @Test public void allResultsAreRetainedAndDeduplicatedByAddress() {
        BluetoothLeAdapter adapter=new BluetoothLeAdapter();
        adapter.add("first","One",-70,false);adapter.add("second","Two",-65,false);adapter.add("third","Three",-55,true);
        assertEquals(3,adapter.getCount());
        adapter.add("second",null,-40,true);assertEquals(3,adapter.getCount());
        assertEquals("Two",adapter.get(1).getNameOrAddress());assertEquals(Integer.valueOf(-40),adapter.get(1).getRssi());assertTrue(adapter.get(1).isConnected());
    }
    @Test public void unnamedDeviceUsesAddressAndSavedRowsHaveNoFakeSignal() {
        BluetoothLeAdapter adapter=new BluetoothLeAdapter();adapter.add("remembered-address","");
        assertEquals("remembered-address",adapter.get(0).getNameOrAddress());assertNull(adapter.get(0).getRssi());assertFalse(adapter.get(0).isConnected());
    }
}
