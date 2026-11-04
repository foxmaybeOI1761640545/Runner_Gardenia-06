package de.dennisguse.opentracks.p0.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.P0HomeBinding;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.data.WorkoutSummary;
import de.dennisguse.opentracks.p0.gps.MotionFormat;
import de.dennisguse.opentracks.p0.recording.*;
import de.dennisguse.opentracks.sensors.GpsStatusValue;
import de.dennisguse.opentracks.ui.HeartRatePanel;

/** P0 rendering and permission prompts; service and database work live behind the ViewModel. */
public class P0Activity extends AbstractActivity {
    private P0HomeBinding binding;
    private P0RecordingViewModel model;
    private ActivityResultLauncher<String[]> locationRequest;
    private ActivityResultLauncher<String> notificationRequest;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        model=new ViewModelProvider(this).get(P0RecordingViewModel.class);
        locationRequest=registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                result->{updateLocationPermission();model.completePermission(hasLocation());});
        notificationRequest=registerForActivityResult(new ActivityResultContracts.RequestPermission(),result->updateLocationPermission());
        binding.p0LocationPermission.setOnClickListener(view->locationRequest.launch(P0PermissionPolicy.location().toArray(new String[0])));
        binding.p0NotificationPermission.setOnClickListener(view->{if(android.os.Build.VERSION.SDK_INT>=33)notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS);});
        binding.p0Start.setOnClickListener(view->{
            ActivityType type=binding.p0Cycling.isChecked()?ActivityType.CYCLING:ActivityType.RUNNING;
            if(hasLocation())model.start(type);
            else {model.pendingStart(type);locationRequest.launch(P0PermissionPolicy.location().toArray(new String[0]));}
        });
        binding.p0Pause.setOnClickListener(view->model.pause());
        binding.p0Resume.setOnClickListener(view->{
            if(hasLocation())model.resume();
            else {model.pendingResume();locationRequest.launch(P0PermissionPolicy.location().toArray(new String[0]));}
        });
        binding.p0Finish.setOnClickListener(view->model.finish());
        binding.p0Devices.setEnabled(true);
        binding.p0Devices.setOnClickListener(view->startActivity(new android.content.Intent(this,P0SensorsActivity.class)));
        binding.p0AllHistory.setOnClickListener(view->startActivity(new android.content.Intent(this,P0HistoryActivity.class)));
        model.snapshot().observe(this,this::render);
        model.history().observe(this,this::renderHistory);
        updateLocationPermission();
    }
    @Override protected void onStart() {super.onStart();model.attach();}
    @Override protected void onStop() {model.detach();super.onStop();}
    private boolean hasLocation() {return ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    private void updateLocationPermission() {
        binding.p0LocationStatus.setText(hasLocation()?R.string.p0_location_granted:R.string.p0_location_needed);
        binding.p0LocationPermission.setVisibility(hasLocation()?View.GONE:View.VISIBLE);
        boolean notification=android.os.Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
        binding.p0NotificationPermission.setVisibility(notification?View.GONE:View.VISIBLE);
    }
    private void render(P0RecordingViewModel.Snapshot value) {
        RecordingSession.Phase phase=value.session().phase();
        int title=switch(phase) {case READY->R.string.p0_ready;case RECORDING->R.string.p0_recording;case PAUSED->R.string.p0_paused;case FINISHED->R.string.p0_finished;};
        binding.p0RecordingState.setText(title);
        boolean idle=phase==RecordingSession.Phase.READY||phase==RecordingSession.Phase.FINISHED;
        binding.p0Start.setEnabled(idle&&!value.busy());binding.p0Pause.setEnabled(phase==RecordingSession.Phase.RECORDING&&!value.busy());
        binding.p0Resume.setEnabled(phase==RecordingSession.Phase.PAUSED&&!value.busy());binding.p0Finish.setEnabled(!idle&&!value.busy());
        binding.p0Running.setEnabled(idle&&!value.busy());binding.p0Cycling.setEnabled(idle&&!value.busy());binding.p0Devices.setEnabled(!value.busy());
        Statistics stats=value.data().track()!=null?value.data().trackStatistics():Statistics.DEFAULT;
        Position position=null;
        if(value.data().sensorDataSet()!=null&&value.data().sensorDataSet().position()!=null)
            position=value.data().sensorDataSet().position().data();
        boolean fix=value.gps()==GpsStatusValue.GPS_SIGNAL_FIX||value.gps()==GpsStatusValue.GPS_SIGNAL_BAD;
        double speed=fix&&position!=null&&position.hasSpeed()?position.speed().toMPS():Double.NaN;
        String location="—";
        if(fix&&position!=null&&position.hasLocation()) {
            String accuracy=position.hasHorizontalAccuracy()?String.format(Locale.ROOT,"±%.1f m",position.horizontalAccuracy().toM()):"—";
            location=String.format(Locale.ROOT,"%.6f, %.6f · %s",position.latitude(),position.longitude(),accuracy);
            if(position.time()!=null)location+="\n"+DateTimeFormatter.ISO_LOCAL_TIME.withZone(ZoneId.systemDefault()).format(position.time());
        }
        binding.p0Metrics.setText(getString(R.string.p0_metrics_format,MotionFormat.duration(stats.totalDuration()),MotionFormat.duration(stats.movingDuration()),stats.totalDistance().toM(),MotionFormat.speed(speed),MotionFormat.pace(speed),getString(value.gps().message),location));
        var heart=HeartRatePanel.describe(this,value.preview().value(),value.preview().status());
        binding.p0HeartRate.setText(heart.bpm()+"\n"+heart.state()+"\n"+heart.age());
        binding.p0Error.setText(value.error());binding.p0Error.setVisibility(value.error()==null?View.GONE:View.VISIBLE);
        updateLocationPermission();
    }
    private void renderHistory(List<WorkoutSummary> rows) {
        binding.p0HistoryList.removeAllViews();
        for(WorkoutSummary row:rows.subList(0,Math.min(rows.size(),5))) {
            TextView text=new TextView(this);text.setPadding(0,12,0,12);
            String hr=row.heartRate().count()==0?"—":String.format(Locale.ROOT,"%.0f / %.0f / %.0f bpm",row.heartRate().minimum(),row.heartRate().average(),row.heartRate().maximum());
            text.setText(row.track().name()+"\n"+String.format(Locale.ROOT,"%.0f m · %s · HR %s",row.track().statistics().totalDistance().toM(),MotionFormat.duration(row.track().statistics().totalDuration()),hr));
            binding.p0HistoryList.addView(text);
            text.setOnClickListener(view->startActivity(P0HistoryActivity.intent(this,row.track().id().id())));
        }
    }
    @NonNull @Override protected View createRootView() {binding=P0HomeBinding.inflate(getLayoutInflater());return binding.getRoot();}
}
