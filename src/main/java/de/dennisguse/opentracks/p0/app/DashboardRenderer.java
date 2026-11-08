package de.dennisguse.opentracks.p0.app;

import android.app.Activity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.P0HomeBinding;
import de.dennisguse.opentracks.p0.recording.RecordingSession;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;

/** One renderer binds one state contract into four genuinely different native XML organizations. */
public final class DashboardRenderer {
    private final Activity activity;private final P0HomeBinding b;private final ThemeUtils.Family family;
    public DashboardRenderer(Activity activity,P0HomeBinding binding){this.activity=activity;b=binding;family=PreferencesUtils.getGardeniaTheme();
        if(family==ThemeUtils.Family.EXPLORER)b.p03FamilyLabel.setText(R.string.p04_field_notes);else b.p03FamilyLabel.setText(family.title);
        float density=activity.getResources().getDisplayMetrics().density;
        int corner=(int)(density*switch(family){case PRECISION->10;case QUIET->16;case EXPLORER->5;case NEON->18;});
        for(var button:new com.google.android.material.button.MaterialButton[]{b.p0Start,b.p0Pause,b.p0Resume,b.p0Finish,b.p0CancelGps,b.p0ContinueGps})button.setCornerRadius(corner);
        b.p04GpsStatus.setCornerRadius((int)(density*(family==ThemeUtils.Family.EXPLORER?4:20)));b.p0Devices.setCornerRadius((int)(density*(family==ThemeUtils.Family.EXPLORER?4:20)));
        if(family==ThemeUtils.Family.NEON)((TextView)b.getRoot().findViewById(R.id.p04_page_title)).setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        if(family==ThemeUtils.Family.QUIET){b.p02DistanceValue.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));for(int id:new int[]{R.id.p02_time_value,R.id.p02_pace_value,R.id.p0_heart_rate,R.id.p04_accuracy})((TextView)b.getRoot().findViewById(id)).setGravity(android.view.Gravity.CENTER);}
    }
    public void bind(DashboardState value){
        var phase=value.phase();boolean idle=phase==RecordingSession.Phase.IDLE||phase==RecordingSession.Phase.FINISHED,acquiring=phase==RecordingSession.Phase.ACQUIRING_GPS,recording=phase==RecordingSession.Phase.RECORDING,paused=phase==RecordingSession.Phase.PAUSED;
        int title=switch(phase){case IDLE->R.string.p02_idle;case ACQUIRING_GPS->value.timedOut()?R.string.p02_gps_waiting:R.string.p02_acquiring_gps;case RECORDING->R.string.p0_recording;case PAUSED->R.string.p0_paused;case FINISHED->R.string.p0_finished;};
        b.p0RecordingState.setText(title);b.p02StatusDetail.setText(value.detail());
        // A fixed GPS chip already states readiness; retain all warnings and preparation detail.
        show(b.p02StatusDetail,!(family==ThemeUtils.Family.NEON&&recording&&value.currentGps()!=null));
        show(b.p0Start,idle);show(b.p0Pause,recording);show(b.p0Resume,paused);show(b.p0Finish,recording||paused);show(b.p0Sport,idle);show(b.p0CancelGps,acquiring);show(b.p0ContinueGps,value.timedOut());show(b.p0LocationSettings,acquiring);
        for(View v:new View[]{b.p0Start,b.p0Pause,b.p0Resume,b.p0Finish,b.p0CancelGps,b.p0ContinueGps,b.p0Running,b.p0Cycling})v.setEnabled(!value.busy());
        readout(b.p02DistanceValue,value.distance(),.22f);b.p02TimeValue.setText(value.duration());readout(b.p02SpeedValue,value.speed(),.7f);readout(b.p02PaceValue,value.pace(),.45f);
        if(family==ThemeUtils.Family.NEON)b.p0HeartRate.setTextSize(value.heartFresh()?70:30);
        readout(b.p0HeartRate,value.bpm(),family==ThemeUtils.Family.NEON?.20f:.45f);
        b.p02HeartState.setText(value.heartState());b.p02HeartDevice.setText(value.device());b.p02HeartUpdated.setText(value.updated());b.p02HeartReconnect.setText(value.reconnect());
        b.p02HeartState.setTextColor(ThemeUtils.color(activity,value.heartFresh()?R.attr.gardeniaSuccess:R.attr.gardeniaWarning));
        b.p02StatusDetail.setTextColor(ThemeUtils.color(activity,acquiring||paused?R.attr.gardeniaWarning:recording&&value.currentGps()==null?R.attr.gardeniaDanger:com.google.android.material.R.attr.colorOnSurfaceVariant));
        ((TextView)b.getRoot().findViewById(R.id.p04_accuracy)).setText(value.accuracy());
        var gps=(com.google.android.material.button.MaterialButton)b.getRoot().findViewById(R.id.p04_gps_status);
        gps.setText(value.currentGps()!=null?R.string.p04_gps_fixed:acquiring?R.string.p04_gps_searching:R.string.p04_gps_unavailable);
        b.p0Devices.setText(value.heartConnected()?R.string.p04_connected:R.string.p04_connect);
        b.p0Metrics.setText(value.gpsDetails());show(b.p0Metrics,value.currentGps()!=null);
        b.p0Error.setText(value.error());show(b.p0Error,value.error()!=null);
    }
    public static void readout(TextView v,String text,float unitScale){
        SpannableString value=new SpannableString(text);int split=0;while(split<text.length()&&"0123456789.,:—–-+±′″".indexOf(text.charAt(split))>=0)split++;
        if(split>0&&split<text.length())value.setSpan(new RelativeSizeSpan(Math.min(1f,Math.max(unitScale,11f/(v.getTextSize()/v.getResources().getDisplayMetrics().scaledDensity)))),split,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);v.setText(value);v.setContentDescription(text);
    }
    private static void show(View v,boolean visible){v.setVisibility(visible?View.VISIBLE:View.GONE);}
}
