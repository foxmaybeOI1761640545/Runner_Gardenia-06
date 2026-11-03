package de.dennisguse.opentracks.ui;

import android.view.View;
import android.widget.TextView;
import java.time.Duration;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.sensors.HeartRateStatus;

/** Shared rendering seam for the real recording panel and deterministic UI instrumentation. */
public final class HeartRatePanel {
    private HeartRatePanel() {}
    public static void bind(View root, HeartRate value, HeartRateStatus status) {
        var context = root.getContext();
        TextView current = root.findViewById(R.id.heart_rate_current);
        TextView state = root.findViewById(R.id.heart_rate_status);
        TextView updated = root.findViewById(R.id.heart_rate_updated);
        long ageMillis = status.lastReceived() != null && status.observedAt() != null
                ? Math.max(0,Duration.between(status.lastReceived(),status.observedAt()).toMillis()) : -1;
        long age = ageMillis < 0 ? -1 : ageMillis / 1000;
        boolean fresh = value != null && value.isValid() && ageMillis >= 0 && ageMillis <= 5000
                && status.state() == de.dennisguse.opentracks.sensors.driver.BleConnectionSession.State.CONNECTED && !status.contactLost();
        current.setText(fresh ? context.getString(R.string.hr_value,value.getBPM()) : context.getString(R.string.hr_empty_value));
        int message = switch (status.state()) {
            case DISCONNECTED -> R.string.hr_none;
            case CONNECTING, DISCOVERING -> R.string.hr_connecting;
            case SUBSCRIBING -> R.string.hr_subscribing;
            case CONNECTED -> status.contactLost() ? R.string.hr_contact_lost : fresh ? R.string.hr_connected : R.string.hr_stale;
            case RECONNECT_WAIT -> R.string.hr_retry;
            case BLUETOOTH_OFF -> R.string.hr_bluetooth_off;
            case PERMISSION_DENIED -> R.string.hr_permission;
            case UNAVAILABLE -> R.string.hr_unavailable;
        };
        String label = context.getString(message);
        state.setText(status.device() == null ? label : context.getString(R.string.hr_device_state,status.device(),label));
        updated.setText(age < 0 ? context.getString(R.string.hr_never) : context.getString(R.string.hr_age,age));
    }
}
