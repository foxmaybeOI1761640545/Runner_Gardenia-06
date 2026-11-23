package de.dennisguse.opentracks.p0.routes;

/** Shared import/codec budgets; no DOM or unbounded metadata/point allocation. */
public record RouteLimits(long fileBytes, int points, int segments, int routes,
                          int xmlDepth, int fieldChars, int metadataChars) {
    public static final RouteLimits DEFAULT = new RouteLimits(16L * 1024 * 1024,
            20_000, 1_024, 128, 32, 2_048, 1_048_576);
    public RouteLimits {
        if (fileBytes < 1 || points < 1 || segments < 1 || routes < 1 || xmlDepth < 1
                || fieldChars < 1 || metadataChars < 1) throw new IllegalArgumentException("Positive route budgets required");
    }
}
