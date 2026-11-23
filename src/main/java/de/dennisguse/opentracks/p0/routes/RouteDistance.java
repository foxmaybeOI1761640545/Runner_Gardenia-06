package de.dennisguse.opentracks.p0.routes;

import java.util.List;
import java.util.OptionalDouble;

/** Great-circle lengths on the mean Earth sphere, computed only within each segment. */
public final class RouteDistance {
    public static final double EARTH_METERS = 6_371_008.8;
    private RouteDistance() {}
    public static double meters(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double sinLat = Math.sin((p2 - p1) / 2), sinLon = Math.sin(Math.toRadians(lon2 - lon1) / 2);
        double a = sinLat * sinLat + Math.cos(p1) * Math.cos(p2) * sinLon * sinLon;
        return 2 * EARTH_METERS * Math.atan2(Math.sqrt(Math.max(0, Math.min(1, a))), Math.sqrt(Math.max(0, 1 - a)));
    }
    public static OptionalDouble total(List<RouteSegment> segments) {
        double total = 0; boolean edge = false;
        for (RouteSegment segment : segments) {
            RoutePoint previous = null;
            for (RoutePoint point : segment.points()) {
                if (previous != null) {
                    total += meters(previous.latitude(), previous.longitude(), point.latitude(), point.longitude());
                    edge = true;
                }
                previous = point;
            }
        }
        return edge && Double.isFinite(total) ? OptionalDouble.of(total) : OptionalDouble.empty();
    }
}
