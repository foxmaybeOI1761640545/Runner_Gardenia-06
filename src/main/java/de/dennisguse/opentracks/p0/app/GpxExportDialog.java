package de.dennisguse.opentracks.p0.app;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.dennisguse.opentracks.R;

/** Fragment restoration preserves the native export choice dialog across a system appearance change. */
public class GpxExportDialog extends DialogFragment {
    @NonNull @Override public Dialog onCreateDialog(Bundle saved) {
        return new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.gardenia_export_title)
                .setItems(new CharSequence[]{getString(R.string.p0_save_gpx),getString(R.string.p0_share_gpx)},(dialog,which)->{
                    Bundle result=new Bundle();result.putInt("action",which);getParentFragmentManager().setFragmentResult("gardenia-gpx-action",result);
                }).setNegativeButton(android.R.string.cancel,null).create();
    }
}
