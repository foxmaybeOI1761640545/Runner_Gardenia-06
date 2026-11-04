package de.dennisguse.opentracks.p0.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.P0HomeBinding;

/** Small offline application entry. Sensors and repositories remain outside this Activity. */
public class P0Activity extends AbstractActivity {
    private P0HomeBinding binding;
    private ActivityResultLauncher<String[]> locationRequest;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        locationRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                result -> updateLocationPermission());
        binding.p0LocationPermission.setOnClickListener(view ->
                locationRequest.launch(P0PermissionPolicy.location().toArray(new String[0])));
        updateLocationPermission();
    }

    private void updateLocationPermission() {
        boolean precise = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        binding.p0LocationStatus.setText(precise ? R.string.p0_location_granted : R.string.p0_location_needed);
    }

    @NonNull @Override protected View createRootView() {
        binding = P0HomeBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }
}
