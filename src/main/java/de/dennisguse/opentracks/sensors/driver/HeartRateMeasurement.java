package de.dennisguse.opentracks.sensors.driver;

import java.util.ArrayList;
import java.util.List;

/** Bluetooth SIG 0x2A37 payload. RR values retain their exact 1/1024-second units. */
public record HeartRateMeasurement(int bpm, int flags, Boolean contactDetected,
                                   Integer energyExpended, List<Integer> rrIntervals1024) {
    public HeartRateMeasurement {
        rrIntervals1024 = List.copyOf(rrIntervals1024);
    }

    public static HeartRateMeasurement parse(byte[] data) {
        if (data == null || data.length < 2) return null;
        int flags = data[0] & 0xff;
        if ((flags & 0xe0) != 0) return null; // reserved by the SIG specification
        boolean wide = (flags & 1) != 0;
        int offset = wide ? 3 : 2;
        if (data.length < offset) return null;
        int bpm = wide ? unsigned16(data, 1) : data[1] & 0xff;
        Boolean contact = (flags & 4) != 0 ? (flags & 2) != 0 : null;
        Integer energy = null;
        if ((flags & 8) != 0) {
            if (data.length < offset + 2) return null;
            energy = unsigned16(data, offset);
            offset += 2;
        }
        List<Integer> rr = new ArrayList<>();
        if ((flags & 16) != 0) {
            if (data.length == offset || (data.length - offset) % 2 != 0) return null;
            while (offset < data.length) {
                int value = unsigned16(data, offset);
                if (value == 0) return null;
                rr.add(value);
                offset += 2;
            }
        }
        if (offset != data.length) return null;
        return new HeartRateMeasurement(bpm, flags, contact, energy, rr);
    }

    private static int unsigned16(byte[] data, int offset) {
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }

    public boolean hasUsableHeartRate() {
        return bpm > 0 && !Boolean.FALSE.equals(contactDetected);
    }
}
