package de.dennisguse.opentracks.ui;

import static org.junit.Assert.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ContextThemeWrapper;
import android.widget.TextView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.time.Instant;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.sensors.HeartRateStatus;
import de.dennisguse.opentracks.sensors.driver.BleConnectionSession;

public class HeartRatePanelTest {
    private View panel() {
        return LayoutInflater.from(new ContextThemeWrapper(ApplicationProvider.getApplicationContext(),R.style.DayNightColorTheme)).inflate(R.layout.statistics_recording,null,false);
    }
    @Test public void exactFreshnessBoundaryDoesNotShowOldBpm() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View root=panel();TextView bpm=root.findViewById(R.id.heart_rate_current);
            HeartRatePanel.bind(root,HeartRate.of(76),new HeartRateStatus(BleConnectionSession.State.CONNECTED,Instant.EPOCH,Instant.EPOCH.plusMillis(5000),"strap",false));
            assertEquals(root.getContext().getString(R.string.hr_value,76f),bpm.getText().toString());
            HeartRatePanel.bind(root,HeartRate.of(76),new HeartRateStatus(BleConnectionSession.State.CONNECTED,Instant.EPOCH,Instant.EPOCH.plusMillis(5001),"strap",false));
            assertEquals(root.getContext().getString(R.string.hr_empty_value),bpm.getText().toString());
        });
    }
    @Test public void contactLossHidesBpmAndExplainsContactProblem() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View root=panel();HeartRatePanel.bind(root,HeartRate.of(76),new HeartRateStatus(BleConnectionSession.State.CONNECTED,Instant.EPOCH,Instant.EPOCH,"strap",true));
            assertEquals(root.getContext().getString(R.string.hr_empty_value),((TextView)root.findViewById(R.id.heart_rate_current)).getText().toString());
            assertTrue(((TextView)root.findViewById(R.id.heart_rate_status)).getText().toString().contains(root.getContext().getString(R.string.hr_contact_lost)));
        });
    }
    @Test public void everyConnectionStateIsReadableWithoutInventingMeasurements() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View root=panel();for(var state:BleConnectionSession.State.values()) {
                HeartRatePanel.bind(root,null,new HeartRateStatus(state,null,null,null,false));
                assertFalse(((TextView)root.findViewById(R.id.heart_rate_status)).getText().toString().isBlank());
                assertEquals(root.getContext().getString(R.string.hr_never),((TextView)root.findViewById(R.id.heart_rate_updated)).getText().toString());
            }
        });
    }
}
