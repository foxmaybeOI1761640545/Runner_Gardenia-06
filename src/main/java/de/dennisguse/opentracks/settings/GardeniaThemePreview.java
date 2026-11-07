package de.dennisguse.opentracks.settings;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.ui.util.ThemeUtils;

/** Small native Drawable diagrams; no bitmap/web preview or remote asset. */
final class GardeniaThemePreview extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ThemeUtils.Family family;
    private final int background,accent,ink;
    private final float density;
    GardeniaThemePreview(Context context,ThemeUtils.Family family) {
        this.family=family;density=context.getResources().getDisplayMetrics().density;
        int[] colors=switch(family){
            case PRECISION->new int[]{R.color.gardenia_precision_background,R.color.gardenia_precision_accent,R.color.gardenia_precision_foreground};
            case QUIET->new int[]{R.color.gardenia_quiet_background,R.color.gardenia_quiet_accent,R.color.gardenia_quiet_foreground};
            case EXPLORER->new int[]{R.color.gardenia_explorer_background,R.color.gardenia_explorer_accent,R.color.gardenia_explorer_foreground};
            case NEON->new int[]{R.color.gardenia_neon_background,R.color.gardenia_neon_accent,R.color.gardenia_neon_foreground};
        };
        background=ContextCompat.getColor(context,colors[0]);accent=ContextCompat.getColor(context,colors[1]);ink=ContextCompat.getColor(context,colors[2]);
    }
    @Override public void draw(@NonNull Canvas canvas) {
        canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/80f,getBounds().height()/64f);
        paint.setStyle(Paint.Style.FILL);paint.setColor(background);canvas.drawRoundRect(0,0,80,64,8,8,paint);
        paint.setColor(accent);canvas.drawRoundRect(7,49,73,58,3,3,paint);paint.setColor(ink);paint.setStrokeWidth(1);
        if(family==ThemeUtils.Family.PRECISION){for(int y:new int[]{20,34})canvas.drawLine(7,y,73,y,paint);canvas.drawLine(40,20,40,44,paint);}
        else if(family==ThemeUtils.Family.EXPLORER){paint.setStyle(Paint.Style.STROKE);for(int y:new int[]{8,16,24}){Path p=new Path();p.moveTo(4,y);p.cubicTo(35,y+30,52,y-12,76,y+12);canvas.drawPath(p,paint);}}
        else if(family==ThemeUtils.Family.NEON){paint.setStyle(Paint.Style.STROKE);paint.setColor(accent);paint.setStrokeWidth(2);Path p=new Path();p.moveTo(6,30);p.lineTo(24,30);p.lineTo(32,17);p.lineTo(40,41);p.lineTo(49,24);p.lineTo(56,30);p.lineTo(74,30);canvas.drawPath(p,paint);}
        else {paint.setStrokeWidth(3);canvas.drawLine(27,24,53,24,paint);paint.setStrokeWidth(1);canvas.drawLine(20,37,60,37,paint);}
        paint.setStyle(Paint.Style.FILL);canvas.restore();
    }
    @Override public int getIntrinsicWidth(){return Math.round(80*density);}
    @Override public int getIntrinsicHeight(){return Math.round(64*density);}
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(@Nullable ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
