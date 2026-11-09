package de.dennisguse.opentracks.p0.app;

import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceViewHolder;
import de.dennisguse.opentracks.R;

/** Six small destinations, using the existing default preference store. */
public class P05SettingsHubFragment extends PreferenceFragmentCompat {
    public static final String[] CATEGORIES={"appearance","motion","devices","history","privacy","info"};
    public static final int[] TITLES={R.string.p05_appearance,R.string.p05_motion,R.string.p05_devices,R.string.p05_history_data,R.string.p05_privacy,R.string.p05_info};
    private static final int[] DETAILS={R.string.p05_appearance_summary,R.string.p05_motion_summary,R.string.p05_devices_summary,R.string.p05_history_summary,R.string.p05_privacy_summary,R.string.p05_info_summary};
    private static final int[] ICONS={R.drawable.p05_palette,R.drawable.p04_run,R.drawable.p04_heart,R.drawable.p04_history,R.drawable.p05_lock,R.drawable.ic_baseline_info_24};
    static Preference row(android.content.Context context){
        Preference preference=new Preference(context){
            @Override public void onBindViewHolder(PreferenceViewHolder holder){
                super.onBindViewHolder(holder);
                holder.itemView.setMinimumHeight(Math.round(64*context.getResources().getDisplayMetrics().density));
            }
        };
        preference.setSingleLineTitle(false);return preference;
    }
    @Override public void onCreatePreferences(Bundle saved,String root){
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(requireContext()));
        for(int i=0;i<CATEGORIES.length;i++){
            final String category=CATEGORIES[i];Preference entry=row(requireContext());
            entry.setKey("p05_category_"+category);entry.setTitle(TITLES[i]);entry.setSummary(DETAILS[i]);entry.setIcon(ICONS[i]);
            entry.setOnPreferenceClickListener(p->{((P0SettingsActivity)requireActivity()).openCategory(category);return true;});getPreferenceScreen().addPreference(entry);
        }
    }
    @Override public void onResume(){super.onResume();((P0SettingsActivity)requireActivity()).getSupportActionBar().setTitle(R.string.p05_settings_title);}
}
