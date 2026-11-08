package de.dennisguse.opentracks.p0.app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import androidx.core.view.ViewCompat;
import com.google.android.material.button.MaterialButton;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;

/** Four existing destinations, one consistent native navigation component. No fake tabs. */
public final class P0Navigation {
    private P0Navigation(){}
    public static void bind(Activity activity){bind(activity,activity.getClass());}
    public static void bind(Activity activity,Class<?> activeDestination){
        int[] ids={R.id.p04_nav_run,R.id.p0_all_history,R.id.p04_nav_heart,R.id.p03_settings};
        Class<?>[] destinations={P0Activity.class,P0HistoryActivity.class,P0SensorsActivity.class,P0SettingsActivity.class};
        var family=PreferencesUtils.getGardeniaTheme();int accent=ThemeUtils.color(activity,R.attr.gardeniaAccentText),muted=ThemeUtils.color(activity,com.google.android.material.R.attr.colorOnSurfaceVariant),tint=ThemeUtils.color(activity,com.google.android.material.R.attr.colorPrimaryContainer);
        for(int i=0;i<ids.length;i++){
            MaterialButton button=activity.findViewById(ids[i]);if(button==null)continue;Class<?> target=destinations[i];boolean selected=target.equals(activeDestination);
            button.setSelected(selected);ViewCompat.setStateDescription(button,selected?activity.getString(R.string.gardenia_selected):null);button.setIconTint(ColorStateList.valueOf(selected?accent:muted));button.setTextColor(selected?accent:muted);button.setBackgroundTintList(ColorStateList.valueOf(selected&&family!=ThemeUtils.Family.QUIET?tint:Color.TRANSPARENT));button.setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);button.setCornerRadius((int)(activity.getResources().getDisplayMetrics().density*(family==ThemeUtils.Family.NEON?0:family==ThemeUtils.Family.EXPLORER?4:12)));button.setStrokeWidth(family==ThemeUtils.Family.NEON&&selected?1:0);button.setStrokeColor(ColorStateList.valueOf(accent));
            button.setOnClickListener(v->{if(!selected)activity.startActivity(new Intent(activity,target).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));});
        }
        if(family==ThemeUtils.Family.QUIET){for(int id:new int[]{R.id.p04_gps_status,R.id.p0_devices}){var view=activity.findViewById(id);if(view instanceof MaterialButton button){button.setStrokeWidth(0);button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));}}}
    }
}
