package de.dennisguse.opentracks.p0.routes;

import java.util.List;

/** Explicit segment boundary; empty segments remain explicit, never joined. */
public record RouteSegment(List<RoutePoint> points) {
    public RouteSegment { points = List.copyOf(points); }
}
