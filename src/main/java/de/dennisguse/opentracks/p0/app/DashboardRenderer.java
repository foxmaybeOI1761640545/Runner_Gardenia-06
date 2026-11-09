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
        float density=activity.getResources().getDisplayMetrics().density;
        int corner=(int)(density*switch(family){case PRECISION->10;case QUIET->16;case EXPLORER->5;case NEON->18;});
        for(var button:new com.google.android.material.button.MaterialButton[]{b.p0Start,b.p0Pause,b.p0Resume,b.p0Finish,b.p0CancelGps,b.p0ContinueGps})button.setCornerRadius(corner);
        b.p04GpsStatus.setCornerRadius((int)(density*(family==ThemeUtils.Family.EXPLORER?4:20)));b.p0Devices.setCornerRadius((int)(density*(family==ThemeUtils.Family.EXPLORER?4:20)));
        if(family==ThemeUtils.Family.NEON)((TextView)b.getRoot().findViewById(R.id.p04_page_title)).setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        if(family==ThemeUtils.Family.QUIET){b.p02DistanceValue.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));for(int id:new int[]{R.id.p02_time_value,R.id.p02_pace_value,R.id.p0_heart_rate,R.id.p04_accuracy})((TextView)b.getRoot().findViewById(id)).setGravity(android.view.Gravity.CENTER);}
    }
    private DashboardState latest;
    private final int[] containers={R.id.p05_secondary_1,R.id.p05_secondary_2,R.id.p05_secondary_3};
    private final int[] labels={R.id.p05_primary_label,R.id.p05_secondary_1_label,R.id.p05_secondary_2_label,R.id.p05_secondary_3_label};
    public void bind(DashboardState value){
        latest=value;
        var phase=value.phase();boolean idle=phase==RecordingSession.Phase.IDLE||phase==RecordingSession.Phase.FINISHED,acquiring=phase==RecordingSession.Phase.ACQUIRING_GPS,recording=phase==RecordingSession.Phase.RECORDING,paused=phase==RecordingSession.Phase.PAUSED;
        int title=switch(phase){case IDLE->R.string.p02_idle;case ACQUIRING_GPS->value.timedOut()?R.string.p02_gps_waiting:R.string.p02_acquiring_gps;case RECORDING->R.string.p0_recording;case PAUSED->R.string.p0_paused;case FINISHED->R.string.p0_finished;};
        b.p0RecordingState.setText(title);b.p02StatusDetail.setText(value.detail());
        // A fixed GPS chip already states readiness; retain all warnings and preparation detail.
        show(b.p02StatusDetail,!(family==ThemeUtils.Family.NEON&&recording&&value.currentGps()!=null));
        show(b.p0Start,idle);show(b.p0Pause,recording);show(b.p0Resume,paused);show(b.p0Finish,recording||paused);show(b.p0Sport,idle);show(b.p0CancelGps,acquiring);show(b.p0ContinueGps,value.timedOut());show(b.p0LocationSettings,acquiring);
        for(View v:new View[]{b.p0Start,b.p0Pause,b.p0Resume,b.p0Finish,b.p0CancelGps,b.p0ContinueGps,b.p0Running,b.p0Cycling})v.setEnabled(!value.busy());
        bindMetrics(value);
        b.p02HeartState.setText(value.heartState());b.p02HeartDevice.setText(value.device());b.p02HeartUpdated.setText(value.updated());b.p02HeartReconnect.setText(value.reconnect());
        b.p02HeartState.setTextColor(ThemeUtils.color(activity,value.heartFresh()?R.attr.gardeniaSuccess:R.attr.gardeniaWarning));
        b.p02StatusDetail.setTextColor(ThemeUtils.color(activity,acquiring||paused?R.attr.gardeniaWarning:recording&&value.currentGps()==null?R.attr.gardeniaDanger:com.google.android.material.R.attr.colorOnSurfaceVariant));
        ((TextView)b.getRoot().findViewById(R.id.p04_accuracy)).setText(value.accuracy());
        var gps=(com.google.android.material.button.MaterialButton)b.getRoot().findViewById(R.id.p04_gps_status);
        gps.setText(value.currentGps()!=null?R.string.p05_gps_fixed:acquiring?R.string.p05_gps_searching:R.string.p05_gps_unavailable);
        int gpsDescription=value.currentGps()!=null?R.string.p04_gps_fixed:acquiring?R.string.p04_gps_searching:R.string.p04_gps_unavailable;
        gps.setContentDescription(activity.getString(R.string.p05_gps_action,activity.getString(gpsDescription)));
        b.p0Devices.setText(value.heartConnected()?R.string.p05_heart_connected:R.string.p05_heart_disconnected);
        b.p0Devices.setContentDescription(activity.getString(R.string.p05_heart_action,value.heartState()));
        b.p0Metrics.setText(value.gpsDetails());show(b.p0Metrics,value.currentGps()!=null);
        b.p0Error.setText(value.error());show(b.p0Error,value.error()!=null);
    }
    private void bindMetrics(DashboardState value){
        var layout=PreferencesUtils.getDashboardMetricLayout(family);
        TextView[] views=family==ThemeUtils.Family.NEON?new TextView[]{b.p0HeartRate,b.p02DistanceValue,b.p02TimeValue,b.p02PaceValue}:new TextView[]{b.p02DistanceValue,b.p02TimeValue,b.p02PaceValue,b.p0HeartRate};
        boolean speed=PreferencesUtils.isReportSpeed(PreferencesUtils.getDefaultActivityTypeLocalized());
        for(int i=0;i<4;i++){
            var metric=layout.positions().get(i);
            String text=switch(metric){case DISTANCE->value.distance();case DURATION->value.duration();case PACE_SPEED->speed?value.speed():value.pace();case HEART_RATE->value.heartFresh()?value.bpm():"--";};
            String title=activity.getString(switch(metric){case DISTANCE->R.string.p02_distance;case DURATION->R.string.p02_time;case PACE_SPEED->speed?R.string.p02_speed:R.string.p02_pace;case HEART_RATE->R.string.p02_heart_heading;});
            ((TextView)b.getRoot().findViewById(labels[i])).setText(title);
            if(i==0&&family==ThemeUtils.Family.NEON){
                // A wrap-content TextView can reserve its original 72sp height
                // while auto-sizing only the glyphs. Set its measured text size.
                androidx.core.widget.TextViewCompat.setAutoSizeTextTypeWithDefaults(views[i],androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
                views[i].setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,metric==DashboardMetric.HEART_RATE?(value.heartFresh()?70:30):40);
            }
            readout(views[i],text,i==0?.25f:.5f);
            if(i>0){final int slot=i-1;View button=b.getRoot().findViewById(containers[slot]);button.setContentDescription(activity.getString(R.string.p05_swap_action,title,text));
                if(!button.hasOnClickListeners())androidx.core.view.ViewCompat.setAccessibilityDelegate(button,new androidx.core.view.AccessibilityDelegateCompat(){@Override public void onInitializeAccessibilityNodeInfo(View host,androidx.core.view.accessibility.AccessibilityNodeInfoCompat info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(android.widget.Button.class.getName());}});
                if(!button.hasOnClickListeners())button.setOnClickListener(clicked->{var next=PreferencesUtils.getDashboardMetricLayout(family).swapSecondary(slot);PreferencesUtils.setDashboardMetricLayout(family,next);bindMetrics(latest);clicked.requestFocus();clicked.announceForAccessibility(activity.getString(R.string.p05_primary_changed,((TextView)b.getRoot().findViewById(R.id.p05_primary_label)).getText()));});
            }
        }
    }
    public static void readout(TextView v,String text,float unitScale){
        SpannableString value=new SpannableString(text);int split=0;while(split<text.length()&&"0123456789.,:—–-+±′″".indexOf(text.charAt(split))>=0)split++;
        if(split>0&&split<text.length())value.setSpan(new RelativeSizeSpan(Math.min(1f,Math.max(unitScale,11f/(v.getTextSize()/v.getResources().getDisplayMetrics().scaledDensity)))),split,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);v.setText(value);v.setContentDescription(text);
    }
    private static void show(View v,boolean visible){v.setVisibility(visible?View.VISIBLE:View.GONE);}
}
