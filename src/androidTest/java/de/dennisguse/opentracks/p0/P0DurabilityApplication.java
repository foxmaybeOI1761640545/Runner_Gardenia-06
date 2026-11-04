package de.dennisguse.opentracks.p0;

/** A test-only file database; the upstream default TestApplication remains in-memory. */
public class P0DurabilityApplication extends de.dennisguse.opentracks.TestApplication {
    @Override public String getDatabaseName(){return "p0-durability.db";}
}
