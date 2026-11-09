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

        P0Navigation.bind(this);
        binding.p04GpsStatus.setOnClickListener(view->startActivity(new android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        dashboard=new DashboardRenderer(this,binding);
        routeModel=new ViewModelProvider(this).get(RoutePreviewModel.class);
        routeModel.route().observe(this,value->{currentRoute=value;binding.p04Route.bind(value,currentGps);});
        chartModel=new ViewModelProvider(this).get(PreviewChartModel.class);
        model.snapshot().observe(this,this::render);
        updateLocationPermission();
    }
    @Override protected void onResume(){super.onResume();if(dashboard!=null&&model!=null&&model.snapshot().getValue()!=null)dashboard.bind(DashboardState.from(this,model.snapshot().getValue()));}
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
    private DashboardRenderer dashboard;
    private RoutePreviewModel routeModel;
    private de.dennisguse.opentracks.data.models.Position currentGps;
    private RoutePreviewModel.Result currentRoute=new RoutePreviewModel.Result(java.util.List.of(),false,false);
    private void render(P0RecordingViewModel.Snapshot value) {
        DashboardState state=DashboardState.from(this,value);dashboard.bind(state);currentGps=state.currentGps();
        chartModel.bind(this,binding.p03HeartChart,value.preview());routeModel.refresh(value.session().trackId());
        binding.p04Route.bind(currentRoute,currentGps);updateLocationPermission();
    }
    @NonNull @Override protected View createRootView() {binding=P0HomeBinding.bind(getLayoutInflater().inflate(PreferencesUtils.getGardeniaTheme().homeLayout,null,false));return binding.getRoot();}
}
