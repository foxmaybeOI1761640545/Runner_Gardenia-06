package de.dennisguse.opentracks.settings;

import android.os.Bundle;
import android.widget.RadioButton;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceViewHolder;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.ui.util.ThemeUtils;

/** Exactly four native family choices. Android, rather than a stored flag, owns appearance. */
public class GardeniaThemeSettingsFragment extends PreferenceFragmentCompat {
    @Override public void onCreatePreferences(Bundle saved,String root) {
        var screen=getPreferenceManager().createPreferenceScreen(requireContext());setPreferenceScreen(screen);
        PreferenceCategory heading=new PreferenceCategory(requireContext());heading.setTitle(R.string.gardenia_follow_system);screen.addPreference(heading);
        for(ThemeUtils.Family family:ThemeUtils.Family.values()) {
            Preference choice=new Preference(requireContext()) {
                @Override public void onBindViewHolder(PreferenceViewHolder holder) {
                    super.onBindViewHolder(holder);
                    boolean selected=PreferencesUtils.getGardeniaTheme()==family;
                    ((RadioButton)holder.findViewById(R.id.gardenia_choice_radio)).setChecked(selected);
                    holder.itemView.setSelected(selected);
                    androidx.core.view.ViewCompat.setStateDescription(holder.itemView,selected?getString(R.string.gardenia_selected):null);
                    holder.itemView.setMinimumHeight(Math.round(80*getResources().getDisplayMetrics().density));
                }
            };
            choice.setKey("gardenia_choice_"+family.id);choice.setTitle(family.title);
            int detail=switch(family){case PRECISION->R.string.gardenia_precision_detail;case QUIET->R.string.gardenia_quiet_detail;case EXPLORER->R.string.gardenia_explorer_detail;case NEON->R.string.gardenia_neon_detail;};
            choice.setSummary(getString(detail)+(PreferencesUtils.getGardeniaTheme()==family?" · "+getString(R.string.gardenia_selected):""));
            choice.setIcon(new GardeniaThemePreview(requireContext(),family));
            choice.setWidgetLayoutResource(R.layout.gardenia_theme_choice);
            choice.setOnPreferenceClickListener(preference->{
                if(PreferencesUtils.getGardeniaTheme()!=family){PreferencesUtils.setGardeniaTheme(family);requireActivity().recreate();}
                return true;
            });heading.addPreference(choice);
        }
    }
    @Override public void onResume(){super.onResume();((androidx.appcompat.app.AppCompatActivity)requireActivity()).getSupportActionBar().setTitle(R.string.gardenia_appearance_title);}
}
