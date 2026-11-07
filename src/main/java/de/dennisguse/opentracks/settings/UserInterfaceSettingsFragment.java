package de.dennisguse.opentracks.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.dennisguse.opentracks.R;

public class UserInterfaceSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_user_interface);
        findPreference(getString(R.string.gardenia_appearance_key)).setOnPreferenceClickListener(preference->{((SettingsActivity)requireActivity()).openScreen(getString(R.string.gardenia_appearance_key));return true;});

        Preference customLayoutPreference = findPreference(getString(R.string.stats_custom_layout_key));
        customLayoutPreference.setOnPreferenceClickListener((preference) -> {
            Intent intent = new Intent(getContext(), SettingsCustomLayoutListActivity.class);
            startActivity(intent);
            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_ui_title);
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        DialogFragment dialogFragment = null;

        if (preference instanceof ResetDialogPreference) {
            dialogFragment = ResetDialogPreference.ResetPreferenceDialog.newInstance(preference.getKey());
        }

        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getParentFragmentManager(), getClass().getSimpleName());
            return;
        }

        super.onDisplayPreferenceDialog(preference);
    }
}
