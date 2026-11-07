package de.dennisguse.opentracks.p0.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.P0HomeBinding;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.gps.MotionFormat;
import de.dennisguse.opentracks.p0.recording.*;
import de.dennisguse.opentracks.p0.ble.HeartRateCommands;
import de.dennisguse.opentracks.p0.gps.GpsPreparation;
import de.dennisguse.opentracks.sensors.HeartRateStatus;
import de.dennisguse.opentracks.sensors.GpsStatusValue;
import de.dennisguse.opentracks.ui.HeartRatePanel;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/** P0 rendering and permission prompts; service and database work live behind the ViewModel. */
public class P0Activity extends AbstractActivity {
    private P0HomeBinding binding;
    private P0RecordingViewModel model;
    private PreviewChartModel chartModel;
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
        binding.p0CancelGps.setOnClickListener(view->model.cancelGpsAcquisition());
        binding.p0ContinueGps.setOnClickListener(view->model.continueGpsAcquisition());
        binding.p0LocationSettings.setOnClickListener(view->startActivity(new android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        binding.p0Pause.setOnClickListener(view->model.pause());
        binding.p0Resume.setOnClickListener(view->{
            if(hasLocation())model.resume();
            else {model.pendingResume();locationRequest.launch(P0PermissionPolicy.location().toArray(new String[0]));}
        });
        binding.p0Finish.setOnClickListener(view->model.finish());
        binding.p0Devices.setEnabled(true);
        binding.p0Devices.setOnClickListener(view->startActivity(new android.content.Intent(this,P0SensorsActivity.class)));
        binding.p0AllHistory.setOnClickListener(view->startActivity(new android.content.Intent(this,P0HistoryActivity.class)));
        findViewById(R.id.p03_settings).setOnClickListener(view->startActivity(new android.content.Intent(this,P0SettingsActivity.class)));
        binding.p03FamilyLabel.setText(PreferencesUtils.getGardeniaTheme()==de.dennisguse.opentracks.ui.util.ThemeUtils.Family.EXPLORER?getString(R.string.gardenia_outdoor_label):PreferencesUtils.getGardeniaTheme().title);
        chartModel=new ViewModelProvider(this).get(PreviewChartModel.class);
        model.snapshot().observe(this,this::render);
        updateLocationPermission();
    }
    @Override protected void onStart() {super.onStart();model.attach();}
    @Override protected void onStop() {model.detach();super.onStop();}
    private boolean hasLocation() {return ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    private void updateLocationPermission() {
        binding.p0LocationStatus.setText(hasLocation()?R.string.p0_location_granted:R.string.p0_location_needed);
        binding.p0LocationStatus.setVisibility(hasLocation()?View.GONE:View.VISIBLE);
        binding.p0LocationPermission.setVisibility(hasLocation()?View.GONE:View.VISIBLE);
        boolean notification=android.os.Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
        binding.p0NotificationPermission.setVisibility(notification?View.GONE:View.VISIBLE);
    }
    private void render(P0RecordingViewModel.Snapshot value) {
        RecordingSession.Phase phase=value.session().phase();
        boolean idle=phase==RecordingSession.Phase.IDLE||phase==RecordingSession.Phase.FINISHED;
        boolean acquiring=phase==RecordingSession.Phase.ACQUIRING_GPS;
        boolean recording=phase==RecordingSession.Phase.RECORDING;
        boolean paused=phase==RecordingSession.Phase.PAUSED;
        boolean timedOut=acquiring&&value.preparation().state()==GpsPreparation.State.TIMED_OUT;
        int title=switch(phase) {
            case IDLE->R.string.p02_idle;
            case ACQUIRING_GPS->timedOut?R.string.p02_gps_waiting:R.string.p02_acquiring_gps;
            case RECORDING->R.string.p0_recording;
            case PAUSED->R.string.p0_paused;
            case FINISHED->R.string.p0_finished;
        };
        binding.p0RecordingState.setText(title);
        show(binding.p0Start,idle);show(binding.p0Pause,recording);show(binding.p0Resume,paused);
        show(binding.p0Finish,recording||paused);show(binding.p0CancelGps,acquiring);show(binding.p0ContinueGps,timedOut);
        show(binding.p0LocationSettings,acquiring);show(binding.p0Sport,idle);
        for(View action:new View[]{binding.p0Start,binding.p0Pause,binding.p0Resume,binding.p0Finish,binding.p0CancelGps,binding.p0ContinueGps})
            action.setEnabled(!value.busy());
        binding.p0Running.setEnabled(idle&&!value.busy());binding.p0Cycling.setEnabled(idle&&!value.busy());binding.p0Devices.setEnabled(!value.busy());

        // Idle/preparation never reuse a finished or synthetic earlier workout's snapshot.
        boolean hasWorkout=recording||paused||phase==RecordingSession.Phase.FINISHED;
        Statistics stats=hasWorkout&&value.data().track()!=null&&value.session().trackId()!=null
                &&value.data().track().id().id()==value.session().trackId()?value.data().trackStatistics():Statistics.DEFAULT;
        Position position=value.position();
        boolean fix=recording&&value.freshGps()&&value.gps()==GpsStatusValue.GPS_SIGNAL_FIX&&position!=null&&position.hasValidLocation();
        double speed=fix&&position.hasSpeed()?position.speed().toMPS():Double.NaN;
        binding.p02DistanceValue.setText(MotionFormat.distance(stats.totalDistance().toM()));
        binding.p02TimeValue.setText(MotionFormat.duration(stats.totalDuration()));
        binding.p02SpeedValue.setText(MotionFormat.speed(speed));binding.p02PaceValue.setText(MotionFormat.pace(speed));
        int detail=switch(phase) {
            case IDLE->R.string.p02_idle_detail;
            case ACQUIRING_GPS->timedOut?R.string.p02_gps_timeout:R.string.p02_acquiring_detail;
            case RECORDING->fix?R.string.p02_gps_ready:R.string.p02_gps_weak;
            case PAUSED->R.string.p02_paused_detail;
            case FINISHED->R.string.p02_finished_detail;
        };
        binding.p02StatusDetail.setText(detail);
        binding.p02StatusDetail.setTextColor(de.dennisguse.opentracks.ui.util.ThemeUtils.color(this,acquiring||paused?R.attr.gardeniaWarning:recording&&!fix?R.attr.gardeniaDanger:com.google.android.material.R.attr.colorOnPrimaryContainer));
        String location="—";
        if(fix) {
            String accuracy=position.hasHorizontalAccuracy()?String.format(Locale.ROOT,"±%.1f m",position.horizontalAccuracy().toM()):"—";
            String captured=position.time()==null?"—":DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(position.time());
            location=getString(R.string.p02_gps_details,position.latitude(),position.longitude(),accuracy,captured);
        }
        binding.p0Metrics.setText(location);show(binding.p0Metrics,fix);

        HeartRateStatus status=value.preview().status();
        HeartRateStatus withoutDevice=new HeartRateStatus(status.state(),status.lastReceived(),status.observedAt(),null,status.contactLost());
        var heart=HeartRatePanel.describe(this,value.preview().value(),withoutDevice);
        binding.p0HeartRate.setText(heart.bpm());binding.p02HeartState.setText(heart.state());
        binding.p02HeartState.setTextColor(de.dennisguse.opentracks.ui.util.ThemeUtils.color(this,heart.bpm().equals(getString(R.string.hr_empty_value))?R.attr.gardeniaWarning:R.attr.gardeniaSuccess));
        binding.p02HeartDevice.setText(status.device()!=null?status.device():HeartRateCommands.hasSelection()?HeartRateCommands.selected():getString(R.string.p02_no_strap));
        String last=status.lastReceived()==null?"—":DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(status.lastReceived());
        binding.p02HeartUpdated.setText(getString(R.string.p02_last_heart,last));
        binding.p02HeartReconnect.setText(!HeartRateCommands.connectionEnabled(this)?R.string.p02_reconnect_stopped:HeartRateCommands.autoReconnectEnabled(this)?R.string.p02_reconnect_on:R.string.p02_reconnect_off);
        binding.p0Error.setText(value.error());show(binding.p0Error,value.error()!=null);
        chartModel.bind(this,binding.p03HeartChart,value.preview());
        updateLocationPermission();
    }
    private static void show(View view,boolean visible) {view.setVisibility(visible?View.VISIBLE:View.GONE);}
    @NonNull @Override protected View createRootView() {binding=P0HomeBinding.bind(getLayoutInflater().inflate(PreferencesUtils.getGardeniaTheme().homeLayout,null,false));return binding.getRoot();}
}
