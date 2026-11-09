package de.dennisguse.opentracks.p0.app;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.GardeniaSettingsBinding;
import de.dennisguse.opentracks.settings.GardeniaThemeSettingsFragment;
import de.dennisguse.opentracks.settings.SettingsActivity;
import androidx.activity.OnBackPressedCallback;

/** Native settings hub; Android restores the current category and its back stack. */
public class P0SettingsActivity extends AbstractActivity {
    private GardeniaSettingsBinding binding;
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);P0Navigation.bind(this);setSupportActionBar(binding.gardeniaSettingsToolbar);
        if(getSupportActionBar()!=null){getSupportActionBar().setTitle(R.string.p05_settings_title);getSupportActionBar().setDisplayHomeAsUpEnabled(true);}
        if(saved==null){
            getSupportFragmentManager().beginTransaction().replace(R.id.gardenia_settings_container,new P05SettingsHubFragment()).commitNow();
            if(getString(R.string.gardenia_appearance_key).equals(getIntent().getStringExtra(SettingsActivity.FRAGMENT_KEY)))openCategory("appearance");
        }
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){
            @Override public void handleOnBackPressed(){
                if(getSupportFragmentManager().getBackStackEntryCount()>0)getSupportFragmentManager().popBackStackImmediate();
                else {setEnabled(false);getOnBackPressedDispatcher().onBackPressed();}
            }
        });
    }
    public void openCategory(String category){
        androidx.fragment.app.Fragment page="appearance".equals(category)?new GardeniaThemeSettingsFragment():P05SettingsSectionFragment.create(category);
        getSupportFragmentManager().beginTransaction().replace(R.id.gardenia_settings_container,page).addToBackStack(category).commit();
    }
    @NonNull @Override protected View createRootView(){binding=GardeniaSettingsBinding.inflate(getLayoutInflater());return binding.getRoot();}
}
