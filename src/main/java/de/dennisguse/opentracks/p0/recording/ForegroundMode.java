package de.dennisguse.opentracks.p0.recording;

/** Permission denial of one sensor must not prevent using an independent sensor. */
public enum ForegroundMode {
    NONE, GPS, BLUETOOTH, GPS_AND_BLUETOOTH;
    public static ForegroundMode select(boolean gps,boolean bluetooth) {
        return gps?(bluetooth?GPS_AND_BLUETOOTH:GPS):(bluetooth?BLUETOOTH:NONE);
    }
}
