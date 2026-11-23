package de.dennisguse.opentracks.p0.routes;

import java.util.UUID;

/** Checked index summary; file contents are verified on open, never assumed valid from its name. */
public record RouteEntry(UUID id, String displayName, String sourceFileName, String sourceSha256,
                         int sourceOrdinal, Route.SourceType sourceType, long importedAtMillis,
                         int pointCount, int segmentCount, Double distanceMeters, long fileBytes, String fileSha256) {
    public RouteEntry {
        if (id == null || displayName == null || displayName.isBlank() || displayName.length() > Route.MAX_DISPLAY_NAME_CHARS
                || sourceFileName != null && sourceFileName.length() > 2048 || !hash(sourceSha256) || !hash(fileSha256)
                || sourceOrdinal < 1 || sourceType == null || importedAtMillis < 0 || pointCount < 1 || pointCount > RouteLimits.DEFAULT.points()
                || segmentCount < 1 || segmentCount > RouteLimits.DEFAULT.segments() || fileBytes < 1 || fileBytes > RouteStorageLimits.MAX_ROUTE_FILE_BYTES
                || distanceMeters != null && (!Double.isFinite(distanceMeters) || distanceMeters < 0)) throw new IllegalArgumentException("Invalid route index summary");
    }
    static boolean hash(String value) { return value != null && value.matches("[0-9a-f]{64}"); }
    static RouteEntry of(Route route, byte[] file) {
        var length = route.distanceMeters();
        return new RouteEntry(route.id(), route.displayName(), route.sourceFileName(), route.sourceSha256(), route.sourceOrdinal(),
                route.sourceType(), route.importedAtMillis(), route.pointCount(), route.segments().size(),
                length.isPresent() ? length.getAsDouble() : null, file.length, RouteCodec.sha256(file));
    }
    RouteEntry renamed(String alias) {
        return new RouteEntry(id, alias, sourceFileName, sourceSha256, sourceOrdinal, sourceType, importedAtMillis,
                pointCount, segmentCount, distanceMeters, fileBytes, fileSha256);
    }
    public String duplicateKey() { return sourceSha256 + ":" + sourceOrdinal; }
}
