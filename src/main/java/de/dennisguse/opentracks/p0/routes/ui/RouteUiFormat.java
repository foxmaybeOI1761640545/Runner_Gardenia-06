package de.dennisguse.opentracks.p0.routes.ui;

import android.content.Context;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.gps.MotionFormat;
import de.dennisguse.opentracks.p0.routes.RouteEntry;
import java.text.DateFormat;
import java.util.Date;

final class RouteUiFormat {
    private RouteUiFormat() {}
    static String distance(Context c, RouteEntry e) { return e.distanceMeters() == null ? c.getString(R.string.p08_unavailable) : MotionFormat.distance(e.distanceMeters()); }
    static String date(long millis) { return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(millis)); }
    static String filename(Context c, String name) { return name == null ? c.getString(R.string.p08_unknown_file) : name; }
}
