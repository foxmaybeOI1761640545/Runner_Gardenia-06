package de.dennisguse.opentracks.p0.app;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.GardeniaSettingsBinding;
import de.dennisguse.opentracks.settings.GardeniaThemeSettingsFragment;

/** P0's settings entry uses the shared native appearance preference screen. */
public class P0SettingsActivity extends AbstractActivity {
    private GardeniaSettingsBinding binding;
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);setSupportActionBar(binding.gardeniaSettingsToolbar);
        if(getSupportActionBar()!=null){getSupportActionBar().setTitle(R.string.gardenia_appearance_title);getSupportActionBar().setDisplayHomeAsUpEnabled(true);}
        if(saved==null)getSupportFragmentManager().beginTransaction().replace(R.id.gardenia_settings_container,new GardeniaThemeSettingsFragment()).commit();
    }
    @NonNull @Override protected View createRootView(){binding=GardeniaSettingsBinding.inflate(getLayoutInflater());return binding.getRoot();}
}
