package de.dennisguse.opentracks.p0.gps;

import java.time.Duration;
import java.util.Locale;

public final class MotionFormat {
    public static final double MIN_DISPLAY_SPEED_MPS = 0.5;
    private MotionFormat() {}
    public static String distance(double meters) {
        if(!Double.isFinite(meters)||meters<0)return "—";
        return meters<1000?String.format(Locale.ROOT,"%.1f m",meters):String.format(Locale.ROOT,"%.2f km",meters/1000);
    }
    public static String duration(Duration duration) {
        long seconds=Math.max(0,duration.getSeconds());
        return seconds>=3600?String.format(Locale.ROOT,"%02d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60)
                :String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);
    }
    public static String pace(double metersPerSecond) {
        if(!Double.isFinite(metersPerSecond)||metersPerSecond<=MIN_DISPLAY_SPEED_MPS)return "—";
        long seconds=Math.round(1000/metersPerSecond);
        return String.format(Locale.ROOT,"%d:%02d /km",seconds/60,seconds%60);
    }
    public static String speed(double metersPerSecond) {
        if (!Double.isFinite(metersPerSecond) || metersPerSecond < 0) return "—";
        return String.format(Locale.ROOT, "%.1f km/h",
                (metersPerSecond <= MIN_DISPLAY_SPEED_MPS ? 0 : metersPerSecond) * 3.6);
    }
}
