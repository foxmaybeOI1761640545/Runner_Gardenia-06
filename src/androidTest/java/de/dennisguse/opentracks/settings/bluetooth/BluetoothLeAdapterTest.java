package de.dennisguse.opentracks.settings.bluetooth;

import static org.junit.Assert.*;
import org.junit.Test;

public class BluetoothLeAdapterTest {
    @Test public void deviceRowShowsNameMacSignalAndConnection() {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            android.content.Context context=new androidx.appcompat.view.ContextThemeWrapper(
                    androidx.test.core.app.ApplicationProvider.getApplicationContext(),de.dennisguse.opentracks.R.style.DayNightDynamicTheme);
            BluetoothLeAdapter adapter=new BluetoothLeAdapter();adapter.add("AA:BB:CC:DD:EE:FF","Generic SIG strap",-45,true);
            android.view.View row=adapter.getView(0,null,new android.widget.FrameLayout(context));
            String text=((android.widget.TextView)row.findViewById(android.R.id.text1)).getText().toString();
            assertTrue(text.contains("Generic SIG strap"));assertTrue(text.contains("AA:BB:CC:DD:EE:FF"));
            assertTrue(text.contains(context.getString(de.dennisguse.opentracks.R.string.sensor_scan_rssi,-45)));
            assertTrue(text.contains(context.getString(de.dennisguse.opentracks.R.string.sensor_scan_connected)));
        });
    }
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
