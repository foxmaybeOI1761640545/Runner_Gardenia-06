package de.dennisguse.opentracks.p0.routes;

import java.util.Map;

/** Planned geometry and literal standard GPX metadata; never a recorded TrackPoint. */
public record RoutePoint(double latitude, double longitude, Double elevation, Map<String, String> metadata) {
    public RoutePoint {
        if (!Double.isFinite(latitude) || Math.abs(latitude) > 90
                || !Double.isFinite(longitude) || Math.abs(longitude) > 180
                || elevation != null && !Double.isFinite(elevation)) throw new IllegalArgumentException("Invalid route coordinate/elevation");
        metadata = Map.copyOf(metadata);
    }
}
