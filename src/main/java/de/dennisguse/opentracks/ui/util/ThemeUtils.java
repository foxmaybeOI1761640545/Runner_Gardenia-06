package de.dennisguse.opentracks.ui.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import com.google.android.material.R;

public class ThemeUtils {

    private ThemeUtils() {
    }

    /** Persisted family only; appearance is always resolved by Android night resources. */
    public enum Family {
        PRECISION("precision", "Precision Pro", de.dennisguse.opentracks.R.style.GardeniaPrecision, de.dennisguse.opentracks.R.layout.gardenia_home_precision),
        QUIET("quiet", "Quiet Minimal", de.dennisguse.opentracks.R.style.GardeniaQuiet, de.dennisguse.opentracks.R.layout.gardenia_home_quiet),
        EXPLORER("explorer", "Outdoor Explorer", de.dennisguse.opentracks.R.style.GardeniaExplorer, de.dennisguse.opentracks.R.layout.gardenia_home_explorer),
        NEON("neon", "Neon Pulse", de.dennisguse.opentracks.R.style.GardeniaNeon, de.dennisguse.opentracks.R.layout.gardenia_home_neon);
        public final String id, title;
        public final int style, homeLayout;
        Family(String id, String title, int style, int homeLayout) { this.id=id;this.title=title;this.style=style;this.homeLayout=homeLayout; }
        public static Family fromId(String id) { for(Family family:values())if(family.id.equals(id))return family;return PRECISION; }
    }
    public static int color(Context context, int attribute) {
        TypedValue value=new TypedValue();
        if(!context.getTheme().resolveAttribute(attribute,value,true))throw new IllegalArgumentException("Missing semantic color: "+attribute);
        return value.resourceId!=0?ContextCompat.getColor(context,value.resourceId):value.data;
    }
    public static int colorOr(Context context,int attribute,int fallback) {
        TypedValue value=new TypedValue();return context.getTheme().resolveAttribute(attribute,value,true)?color(context,attribute):fallback;
    }
    public static boolean isNight(Context context) {
        return (context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Get the material design default background color.
     */
    public static int getBackgroundColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorSurface, typedValue, true);

        return ContextCompat.getColor(context, typedValue.resourceId);
    }

    public static int getTextColorPrimary(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true);

        return ContextCompat.getColor(context, typedValue.resourceId);
    }

    public static int getTextColorSecondary(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorSecondary, typedValue, true);

        return ContextCompat.getColor(context, typedValue.resourceId);
    }

    public static int getFontSizeSmallInPx(Context context) {
        try(TypedArray typedArray = context.obtainStyledAttributes(R.style.TextAppearance_MaterialComponents_Body2, new int[]{android.R.attr.textSize})) {
            return typedArray.getDimensionPixelSize(0, 12);
        }
    }

    public static int getFontSizeMediumInPx(Context context) {
        try(TypedArray typedArray = context.obtainStyledAttributes(R.style.TextAppearance_MaterialComponents_Body1, new int[]{android.R.attr.textSize})) {
            return typedArray.getDimensionPixelSize(0, 15);
        }
    }

    public static int getPhotoHeight(Context context) {
        try(TypedArray typeArray = context.obtainStyledAttributes(new int[]{android.R.attr.listPreferredItemHeight})) {
            int height = typeArray.getDimensionPixelSize(0, 128);
            return 2 * height;
        }
    }
}
