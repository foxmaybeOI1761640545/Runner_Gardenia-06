package de.dennisguse.opentracks.p0.app;

import android.content.Context;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.ble.HeartRateCommands;
import de.dennisguse.opentracks.p0.gps.GpsPreparation;
import de.dennisguse.opentracks.p0.gps.MotionFormat;
import de.dennisguse.opentracks.p0.recording.*;
import de.dennisguse.opentracks.sensors.*;
import de.dennisguse.opentracks.sensors.driver.BleConnectionSession;
import de.dennisguse.opentracks.ui.HeartRatePanel;

/** Immutable presentation data. The only production source is the existing recording snapshot. */
public record DashboardState(RecordingSession.Phase phase,boolean busy,boolean timedOut,String distance,String duration,String speed,String pace,String bpm,String heartState,String device,String updated,String reconnect,String detail,String gpsDetails,String accuracy,Position currentGps,boolean heartConnected,boolean heartFresh,String error){
    public static DashboardState from(Context context,P0RecordingViewModel.Snapshot value){
        var phase=value.session().phase();boolean recording=phase==RecordingSession.Phase.RECORDING;
        boolean hasWorkout=recording||phase==RecordingSession.Phase.PAUSED||phase==RecordingSession.Phase.FINISHED;
        Statistics stats=hasWorkout&&value.data().track()!=null&&value.session().trackId()!=null&&value.data().track().id().id()==value.session().trackId()?value.data().trackStatistics():Statistics.DEFAULT;
        Position p=value.position();boolean fix=recording&&value.freshGps()&&value.gps()==GpsStatusValue.GPS_SIGNAL_FIX&&p!=null&&p.hasValidLocation();
        double speed=fix&&p.hasSpeed()?p.speed().toMPS():Double.NaN;
        boolean timeout=phase==RecordingSession.Phase.ACQUIRING_GPS&&value.preparation().state()==GpsPreparation.State.TIMED_OUT;
        int explanation=switch(phase){case IDLE->R.string.p02_idle_detail;case ACQUIRING_GPS->timeout?R.string.p02_gps_timeout:R.string.p02_acquiring_detail;case RECORDING->fix?R.string.p02_gps_ready:R.string.p02_gps_weak;case PAUSED->R.string.p02_paused_detail;case FINISHED->R.string.p02_finished_detail;};
        var status=value.preview().status();var hr=value.preview().value();var withoutAddress=new HeartRateStatus(status.state(),status.lastReceived(),status.observedAt(),null,status.contactLost());
        var described=HeartRatePanel.describe(context,hr,withoutAddress);boolean fresh=!described.bpm().equals(context.getString(R.string.hr_empty_value));
        boolean connected=status.state()==BleConnectionSession.State.CONNECTED;
        String device=hr!=null&&hr.sample()!=null?hr.sample().deviceName():null;
        if(device==null||device.isBlank())device=status.device();
        // An identifier is never used as a display name on the dashboard.
        if(device==null||device.isBlank()||device.matches("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))device=context.getString(connected?R.string.p04_heart_device:R.string.p04_heart_none);
        String last=status.lastReceived()==null?"—":DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(status.lastReceived());
        String accuracy=fix&&p.hasHorizontalAccuracy()?String.format(Locale.ROOT,"±%.1f m",p.horizontalAccuracy().toM()):"—";
        String location=fix?context.getString(R.string.p02_gps_details,p.latitude(),p.longitude(),accuracy,p.time()==null?"—":DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(p.time())):"—";
        return new DashboardState(phase,value.busy(),timeout,MotionFormat.distance(stats.totalDistance().toM()),MotionFormat.duration(stats.totalDuration()),MotionFormat.speed(speed),MotionFormat.pace(speed),described.bpm(),connected&&status.lastReceived()==null&&!status.contactLost()?context.getString(R.string.p04_heart_wait):described.state(),device,context.getString(R.string.p02_last_heart,last),context.getString(!HeartRateCommands.connectionEnabled(context)?R.string.p02_reconnect_stopped:HeartRateCommands.autoReconnectEnabled(context)?R.string.p02_reconnect_on:R.string.p02_reconnect_off),context.getString(explanation),location,accuracy,fix?p:null,connected,fresh,value.error());
    }
}
