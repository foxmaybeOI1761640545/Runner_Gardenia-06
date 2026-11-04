package de.dennisguse.opentracks.p0.app;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.P0SensorsBinding;
import de.dennisguse.opentracks.p0.recording.P0RecordingViewModel;

/** Screen lifetime only attaches observation; the foreground service owns connections. */
public class P0SensorsActivity extends AbstractActivity {
    private P0SensorsBinding binding;
    private P0RecordingViewModel model;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        model=new ViewModelProvider(this).get(P0RecordingViewModel.class);
        setSupportActionBar(binding.p0SensorToolbar);
        if(getSupportActionBar()!=null){getSupportActionBar().setTitle(R.string.p0_devices);getSupportActionBar().setDisplayHomeAsUpEnabled(true);}
        if(state==null)getSupportFragmentManager().beginTransaction()
                .replace(R.id.p0_sensor_container,new P0SensorsFragment()).commit();
    }
    @Override protected void onStart(){super.onStart();model.attach();}
    @Override protected void onStop(){model.detach();super.onStop();}
    @NonNull @Override protected View createRootView(){binding=P0SensorsBinding.inflate(getLayoutInflater());return binding.getRoot();}
}
