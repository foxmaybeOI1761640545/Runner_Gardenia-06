package de.dennisguse.opentracks.p0.routes;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

/** Independent immutable planned route. Local alias is separate from original GPX metadata. */
public record Route(UUID id, String displayName, String sourceFileName, String sourceSha256,
                    int sourceOrdinal, SourceType sourceType, long importedAtMillis,
                    Map<String, String> metadata, List<RouteSegment> segments) {
    public enum SourceType { RTE, TRK }
    public static final int MAX_DISPLAY_NAME_CHARS = 256;
    public Route {
        if (id == null || displayName == null || displayName.isBlank()
                || displayName.length() > MAX_DISPLAY_NAME_CHARS || sourceFileName != null && sourceFileName.length() > 2_048
                || sourceSha256 == null || !sourceSha256.matches("[0-9a-f]{64}") || sourceOrdinal < 1
                || sourceType == null || importedAtMillis < 0) throw new IllegalArgumentException("Invalid planned-route identity");
        metadata = Map.copyOf(metadata); segments = List.copyOf(segments);
        if (segments.stream().mapToLong(s -> s.points().size()).sum() == 0) throw new IllegalArgumentException("No route points");
    }
    public int pointCount() { return segments.stream().mapToInt(s -> s.points().size()).sum(); }
    public OptionalDouble distanceMeters() { return RouteDistance.total(segments); }
    public String originalName() { return metadata.getOrDefault("name", ""); }
    public String description() { return metadata.getOrDefault("desc", ""); }
    public Route renamed(String name) {
        if (name == null) throw new IllegalArgumentException("A local name is required");
        return new Route(id, name.strip(), sourceFileName, sourceSha256, sourceOrdinal, sourceType,
                importedAtMillis, metadata, segments);
    }
    public String duplicateKey() { return sourceSha256 + ":" + sourceOrdinal; }
}
