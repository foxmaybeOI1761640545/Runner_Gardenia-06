package de.dennisguse.opentracks.p0.routes;

/** Version1 private-store budgets, independent from history and map storage. */
public final class RouteStorageLimits {
    private RouteStorageLimits() {}
    public static final int SCHEMA = 1;
    public static final int MAX_ROUTES = 1024;
    public static final int MAX_ROUTE_FILE_BYTES = 16 * 1024 * 1024;
    public static final int MAX_INDEX_BYTES = 8 * 1024 * 1024;
    public static final int MAX_PENDING_TRANSACTIONS = 64;
}
