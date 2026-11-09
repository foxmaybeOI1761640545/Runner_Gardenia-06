package de.dennisguse.opentracks.p0.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import de.dennisguse.opentracks.BuildConfig;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/** Category pages delegate to the original unit, rate, BLE and history paths. */
public class P05SettingsSectionFragment extends PreferenceFragmentCompat {
    private static final String CATEGORY="category";
    public static P05SettingsSectionFragment create(String category){
        var fragment=new P05SettingsSectionFragment();Bundle args=new Bundle();args.putString(CATEGORY,category);fragment.setArguments(args);return fragment;
    }
    public String category(){return requireArguments().getString(CATEGORY);}
    @Override public void onCreatePreferences(Bundle saved,String root){
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(requireContext()));
        switch(category()){
            case "motion" -> motion();
            case "devices" -> link("p05_manage_devices",R.string.p05_manage_devices,R.string.p05_devices_summary,P0SensorsActivity.class);
            case "history" -> link("p05_manage_history",R.string.p05_manage_history,R.string.p05_history_summary,P0HistoryActivity.class);
            case "privacy" -> {info("p05_local_data",R.string.p05_local_data,R.string.p05_local_data_detail);info("p05_user_export",R.string.p05_user_export,R.string.p05_user_export_detail);}
            case "info" -> {text("p05_app_name",R.string.p05_app_name,getString(R.string.app_name));text("p05_version",R.string.p05_version,BuildConfig.VERSION_NAME);text("p05_build",R.string.p05_build,Integer.toString(BuildConfig.VERSION_CODE));}
            default -> throw new IllegalArgumentException("Unknown settings category");
        }
    }
    private void motion(){
        ListPreference units=list(R.string.stats_units_key,R.string.settings_stats_units_title,R.array.stats_units_options,R.array.stats_units_values,R.string.stats_units_default);
        ListPreference rate=list(R.string.stats_rate_key,R.string.settings_stats_rate_title,rateEntries(units.getValue()),R.array.stats_rate_values,R.string.stats_rate_default);
        units.setOnPreferenceChangeListener((preference,value)->{rate.setEntries(rateEntries((String)value));return true;});
        Preference reset=P05SettingsHubFragment.row(requireContext());reset.setKey("p05_reset_metrics");reset.setTitle(R.string.p05_reset_metrics);
        reset.setSummary(R.string.p05_reset_metrics_summary);
        reset.setOnPreferenceClickListener(p->{PreferencesUtils.resetDashboardMetricLayout(PreferencesUtils.getGardeniaTheme());Toast.makeText(requireContext(),R.string.p05_metrics_reset,Toast.LENGTH_SHORT).show();return true;});getPreferenceScreen().addPreference(reset);
    }
    private int rateEntries(String units){return "NAUTICAL".equals(units)?R.array.stats_rate_nautical_options:"METRIC".equals(units)?R.array.stats_rate_metric_options:R.array.stats_rate_imperial_options;}
    private ListPreference list(int key,int title,int entries,int values,int fallback){
        ListPreference p=new ListPreference(requireContext());p.setKey(getString(key));p.setTitle(title);p.setSingleLineTitle(false);p.setEntries(entries);p.setEntryValues(values);p.setDefaultValue(getString(fallback));p.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());getPreferenceScreen().addPreference(p);return p;
    }
    private void link(String key,int title,int detail,Class<?> destination){Preference p=P05SettingsHubFragment.row(requireContext());p.setKey(key);p.setTitle(title);p.setSummary(detail);p.setOnPreferenceClickListener(v->{startActivity(destination==P0HistoryActivity.class?P0HistoryActivity.managementIntent(requireContext()):new Intent(requireContext(),destination));return true;});getPreferenceScreen().addPreference(p);}
    private void info(String key,int title,int detail){text(key,title,getString(detail));}
    private void text(String key,int title,String value){Preference p=P05SettingsHubFragment.row(requireContext());p.setKey(key);p.setTitle(title);p.setSummary(value);p.setSelectable(false);getPreferenceScreen().addPreference(p);}
    @Override public void onResume(){super.onResume();for(int i=0;i<P05SettingsHubFragment.CATEGORIES.length;i++)if(P05SettingsHubFragment.CATEGORIES[i].equals(category()))((P0SettingsActivity)requireActivity()).getSupportActionBar().setTitle(P05SettingsHubFragment.TITLES[i]);}
}
