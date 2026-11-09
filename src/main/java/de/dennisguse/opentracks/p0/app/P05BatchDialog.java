package de.dennisguse.opentracks.p0.app;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.dennisguse.opentracks.R;

/** Immutable selected IDs survive rotation with the confirmation, never read later UI selection. */
public class P05BatchDialog extends DialogFragment {
 public static P05BatchDialog create(long[] ids,boolean deleting){var f=new P05BatchDialog();var b=new Bundle();b.putLongArray("ids",ids);b.putBoolean("deleting",deleting);f.setArguments(b);return f;}
 private void result(String action){Bundle b=new Bundle();b.putLongArray("ids",requireArguments().getLongArray("ids"));b.putString("action",action);getParentFragmentManager().setFragmentResult("p05-batch-action",b);}
 @NonNull @Override public Dialog onCreateDialog(Bundle saved){
  long[] ids=requireArguments().getLongArray("ids");var builder=new MaterialAlertDialogBuilder(requireContext()).setNegativeButton(android.R.string.cancel,null);
  if(requireArguments().getBoolean("deleting"))return builder.setTitle(R.string.p0_delete_workout).setMessage(getString(R.string.p05_delete_confirm,ids.length)).setPositiveButton(R.string.p0_delete_workout,(d,w)->result("delete")).create();
  return builder.setTitle(getString(R.string.p05_share_title,ids.length)).setSingleChoiceItems(new CharSequence[]{getString(R.string.p05_zip_recommended),getString(R.string.p05_direct_gpx)},0,null)
    .setPositiveButton(R.string.p05_share,(d,w)->result(((AlertDialog)d).getListView().getCheckedItemPosition()==0?"zip":"gpx")).create();
 }
}
