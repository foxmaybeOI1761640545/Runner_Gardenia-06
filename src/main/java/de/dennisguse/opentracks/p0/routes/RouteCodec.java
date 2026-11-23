package de.dennisguse.opentracks.p0.routes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Bounded, versioned UTF8 binary envelopes. SHA256 detects truncation/corruption, not malicious authentication. */
final class RouteCodec {
    static final String ROUTE_MAGIC = "RGROUTE!", INDEX_MAGIC = "RGIDXBIN", JOURNAL_MAGIC = "RGTXNLOG";
    static final class FutureVersion extends IOException { FutureVersion(int version) { super("Unsupported future route-store schema " + version); } }
    interface Encoder { void write(DataOutputStream output) throws IOException; }
    record FileRef(UUID id, String sha256) { FileRef { if (id == null || !RouteEntry.hash(sha256)) throw new IllegalArgumentException("Invalid journal file"); } }
    record Journal(boolean beforeExisted, String beforeSha256, String afterSha256, List<FileRef> added, List<FileRef> retired, List<FileRef> adopted) {
        Journal { if (!RouteEntry.hash(beforeSha256) || !RouteEntry.hash(afterSha256)) throw new IllegalArgumentException("Invalid journal index hash"); added = List.copyOf(added); retired = List.copyOf(retired); adopted = List.copyOf(adopted); }
    }
    private RouteCodec() {}
    static String sha256(byte[] bytes) {
        byte[] digest = digest(bytes, 0, bytes.length); StringBuilder out = new StringBuilder(64);
        for (byte value : digest) { out.append(Character.forDigit((value >>> 4) & 15, 16)); out.append(Character.forDigit(value & 15, 16)); }
        return out.toString();
    }
    private static byte[] digest(byte[] bytes, int offset, int count) {
        try { var hash = MessageDigest.getInstance("SHA-256"); hash.update(bytes, offset, count); return hash.digest(); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static final class BoundedBuffer extends ByteArrayOutputStream {
        final int limit;
        BoundedBuffer(int limit) { this.limit = limit; }
        private void check(int added) { if (added < 0 || (long)count + added > limit) throw new IllegalArgumentException("Encoded route-store byte limit exceeded"); }
        @Override public synchronized void write(int value) { check(1); super.write(value); }
        @Override public synchronized void write(byte[] bytes, int offset, int length) { check(length); super.write(bytes, offset, length); }
    }
    static byte[] envelope(String magic, int maxBytes, Encoder encoder) throws IOException {
        try {
            var payload = new BoundedBuffer(maxBytes - 48); var output = new DataOutputStream(payload); encoder.write(output); output.flush();
            byte[] body = payload.toByteArray(); var result = new ByteArrayOutputStream(body.length + 48); var data = new DataOutputStream(result);
            data.write(magic.getBytes(StandardCharsets.US_ASCII)); data.writeInt(RouteStorageLimits.SCHEMA); data.writeInt(body.length);
            data.write(body); data.write(digest(body, 0, body.length)); data.flush(); return result.toByteArray();
        } catch (IllegalArgumentException invalid) { throw new IOException("Invalid or oversized route-store data", invalid); }
    }
    static DataInputStream open(byte[] bytes, String magic, int maxBytes) throws IOException {
        if (bytes.length < 48 || bytes.length > maxBytes) throw new IOException("Invalid route-store file size");
        var input = new DataInputStream(new ByteArrayInputStream(bytes)); byte[] actualMagic = new byte[8]; input.readFully(actualMagic);
        if (!MessageDigest.isEqual(actualMagic, magic.getBytes(StandardCharsets.US_ASCII))) throw new IOException("Invalid route-store header");
        int version = input.readInt(); if (version > RouteStorageLimits.SCHEMA) throw new FutureVersion(version);
        if (version != RouteStorageLimits.SCHEMA) throw new IOException("Invalid route-store schema");
        int size = input.readInt(); if (size < 0 || size != bytes.length - 48) throw new IOException("Truncated route-store payload");
        byte[] stored = new byte[32]; System.arraycopy(bytes, bytes.length - 32, stored, 0, 32);
        if (!MessageDigest.isEqual(stored, digest(bytes, 16, size))) throw new IOException("Route-store content checksum differs");
        return new DataInputStream(new ByteArrayInputStream(bytes, 16, size));
    }
    static void rejectFutureHeader(byte[] header, String magic) throws IOException {
        if (header.length >= 12 && new String(header, 0, 8, StandardCharsets.US_ASCII).equals(magic)) {
            int version = ByteBuffer.wrap(header, 8, 4).getInt(); if (version > RouteStorageLimits.SCHEMA) throw new FutureVersion(version);
        }
    }
    private static boolean flag(DataInputStream input) throws IOException {
        int flag = input.readUnsignedByte(); if (flag > 1) throw new IOException("Invalid route-store boolean"); return flag == 1;
    }
    private static void end(DataInputStream input) throws IOException { if (input.available() != 0) throw new IOException("Unexpected trailing route-store fields"); }
    private static void string(DataOutputStream output, String value, int maxChars, boolean nullable) throws IOException {
        if (value == null) { if (!nullable) throw new IOException("Missing route field"); output.writeInt(-1); return; }
        if (value.length() > maxChars) throw new IOException("Route field exceeds character limit");
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).encode(java.nio.CharBuffer.wrap(value));
            byte[] bytes = new byte[encoded.remaining()]; encoded.get(bytes); output.writeInt(bytes.length); output.write(bytes);
        } catch (CharacterCodingException invalid) { throw new IOException("Invalid UTF8 route field", invalid); }
    }
    private static String string(DataInputStream input, int maxChars, boolean nullable) throws IOException {
        int length = input.readInt(); if (length == -1 && nullable) return null;
        if (length < 0 || length > maxChars * 4L || length > input.available()) throw new IOException("Invalid route string length");
        byte[] bytes = new byte[length]; input.readFully(bytes);
        try {
            String value = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            if (value.length() > maxChars) throw new IOException("Decoded route string exceeds limit"); return value;
        } catch (CharacterCodingException invalid) { throw new IOException("Invalid UTF8 route field", invalid); }
    }
    private static void uuid(DataOutputStream output, UUID id) throws IOException { output.writeLong(id.getMostSignificantBits()); output.writeLong(id.getLeastSignificantBits()); }
    private static UUID uuid(DataInputStream input) throws IOException { return new UUID(input.readLong(), input.readLong()); }
    private static final class Budget {
        int points, metadata;
        void text(String key, String value) throws IOException { metadata += key.length() + value.length() + 1; if (metadata > RouteLimits.DEFAULT.metadataChars()) throw new IOException("Route metadata budget exceeded"); }
    }
    private static void metadata(DataOutputStream output, Map<String,String> metadata, Budget budget) throws IOException {
        output.writeInt(metadata.size());
        for (var entry : new TreeMap<>(metadata).entrySet()) {
            budget.text(entry.getKey(), entry.getValue()); string(output, entry.getKey(), 2048, false); string(output, entry.getValue(), 2048, false);
        }
    }
    private static Map<String,String> metadata(DataInputStream input, Budget budget) throws IOException {
        int count = input.readInt(); if (count < 0 || count > RouteLimits.DEFAULT.metadataChars() || count > input.available() / 8) throw new IOException("Invalid metadata field count");
        Map<String,String> result = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String key = string(input, 2048, false), value = string(input, 2048, false); budget.text(key, value);
            if (result.putIfAbsent(key, value) != null) throw new IOException("Duplicate stored metadata key");
        }
        return result;
    }
    static byte[] route(Route route) throws IOException {
        return envelope(ROUTE_MAGIC, RouteStorageLimits.MAX_ROUTE_FILE_BYTES, output -> {
            Budget budget = new Budget(); uuid(output, route.id()); string(output, route.displayName(), 256, false); string(output, route.sourceFileName(), 2048, true);
            string(output, route.sourceSha256(), 64, false); output.writeInt(route.sourceOrdinal()); output.writeInt(route.sourceType().ordinal()); output.writeLong(route.importedAtMillis());
            metadata(output, route.metadata(), budget);
            if (route.segments().size() > RouteLimits.DEFAULT.segments()) throw new IOException("Route segment budget exceeded");
            output.writeInt(route.segments().size());
            for (RouteSegment segment : route.segments()) {
                budget.points += segment.points().size(); if (budget.points > RouteLimits.DEFAULT.points()) throw new IOException("Route point budget exceeded");
                output.writeInt(segment.points().size());
                for (RoutePoint point : segment.points()) {
                    output.writeDouble(point.latitude()); output.writeDouble(point.longitude()); output.writeBoolean(point.elevation() != null);
                    if (point.elevation() != null) output.writeDouble(point.elevation()); metadata(output, point.metadata(), budget);
                }
            }
        });
    }
    static Route route(byte[] bytes) throws IOException {
        try (DataInputStream input = open(bytes, ROUTE_MAGIC, RouteStorageLimits.MAX_ROUTE_FILE_BYTES)) {
            Budget budget = new Budget(); UUID id = uuid(input); String name = string(input, 256, false), fileName = string(input, 2048, true), source = string(input, 64, false);
            int ordinal = input.readInt(), kind = input.readInt(); long importedAt = input.readLong();
            if (kind < 0 || kind >= Route.SourceType.values().length) throw new IOException("Invalid route source type");
            Map<String,String> metadata = metadata(input, budget); int count = input.readInt();
            if (count < 1 || count > RouteLimits.DEFAULT.segments()) throw new IOException("Invalid route segment count");
            List<RouteSegment> segments = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int points = input.readInt(); if (points < 0 || (long)budget.points + points > RouteLimits.DEFAULT.points() || points > input.available() / 21) throw new IOException("Invalid route point count");
                budget.points += points; List<RoutePoint> values = new ArrayList<>();
                for (int j = 0; j < points; j++) {
                    double latitude = input.readDouble(), longitude = input.readDouble(); Double elevation = flag(input) ? input.readDouble() : null;
                    values.add(new RoutePoint(latitude, longitude, elevation, metadata(input, budget)));
                }
                segments.add(new RouteSegment(values));
            }
            end(input); return new Route(id, name, fileName, source, ordinal, Route.SourceType.values()[kind], importedAt, metadata, segments);
        } catch (IllegalArgumentException invalid) { throw new IOException("Invalid stored route", invalid); }
    }
    static byte[] index(List<RouteEntry> entries) throws IOException {
        if (entries.size() > RouteStorageLimits.MAX_ROUTES) throw new IOException("Route library capacity exceeded");
        return envelope(INDEX_MAGIC, RouteStorageLimits.MAX_INDEX_BYTES, output -> {
            output.writeInt(entries.size()); var ids = new HashSet<UUID>();
            for (RouteEntry entry : entries) {
                if (!ids.add(entry.id())) throw new IOException("Duplicate route UUID in index");
                uuid(output, entry.id()); string(output, entry.displayName(), 256, false); string(output, entry.sourceFileName(), 2048, true); string(output, entry.sourceSha256(), 64, false);
                output.writeInt(entry.sourceOrdinal()); output.writeInt(entry.sourceType().ordinal()); output.writeLong(entry.importedAtMillis()); output.writeInt(entry.pointCount()); output.writeInt(entry.segmentCount());
                output.writeBoolean(entry.distanceMeters() != null); if (entry.distanceMeters() != null) output.writeDouble(entry.distanceMeters());
                output.writeLong(entry.fileBytes()); string(output, entry.fileSha256(), 64, false);
            }
        });
    }
    static List<RouteEntry> index(byte[] bytes) throws IOException {
        try (DataInputStream input = open(bytes, INDEX_MAGIC, RouteStorageLimits.MAX_INDEX_BYTES)) {
            int count = input.readInt(); if (count < 0 || count > RouteStorageLimits.MAX_ROUTES) throw new IOException("Invalid route library count");
            List<RouteEntry> entries = new ArrayList<>(); var ids = new HashSet<UUID>();
            for (int i = 0; i < count; i++) {
                UUID id = uuid(input); if (!ids.add(id)) throw new IOException("Duplicate indexed UUID");
                String name = string(input, 256, false), fileName = string(input, 2048, true), source = string(input, 64, false);
                int ordinal = input.readInt(), type = input.readInt(); if (type < 0 || type >= Route.SourceType.values().length) throw new IOException("Invalid indexed source type");
                long importedAt = input.readLong(); int points = input.readInt(), segments = input.readInt(); Double distance = flag(input) ? input.readDouble() : null;
                long fileBytes = input.readLong(); String hash = string(input, 64, false);
                entries.add(new RouteEntry(id, name, fileName, source, ordinal, Route.SourceType.values()[type], importedAt, points, segments, distance, fileBytes, hash));
            }
            end(input); return List.copyOf(entries);
        } catch (IllegalArgumentException invalid) { throw new IOException("Invalid route index", invalid); }
    }
    static byte[] journal(Journal journal) throws IOException {
        return envelope(JOURNAL_MAGIC, RouteStorageLimits.MAX_INDEX_BYTES, output -> {
            output.writeBoolean(journal.beforeExisted()); string(output, journal.beforeSha256(), 64, false); string(output, journal.afterSha256(), 64, false);
            refs(output, journal.added()); refs(output, journal.retired()); refs(output, journal.adopted());
        });
    }
    private static void refs(DataOutputStream output, List<FileRef> files) throws IOException {
        if (files.size() > RouteStorageLimits.MAX_ROUTES) throw new IOException("Journal file count exceeded"); output.writeInt(files.size());
        var ids = new HashSet<UUID>();
        for (FileRef file : files) { if (!ids.add(file.id())) throw new IOException("Duplicate journal UUID"); uuid(output, file.id()); string(output, file.sha256(), 64, false); }
    }
    private static List<FileRef> refs(DataInputStream input) throws IOException {
        int count = input.readInt(); if (count < 0 || count > RouteStorageLimits.MAX_ROUTES) throw new IOException("Invalid journal file count");
        List<FileRef> files = new ArrayList<>(); var ids = new HashSet<UUID>();
        for (int i = 0; i < count; i++) { UUID id = uuid(input); if (!ids.add(id)) throw new IOException("Duplicate journal UUID"); files.add(new FileRef(id, string(input, 64, false))); }
        return files;
    }
    static Journal journal(byte[] bytes) throws IOException {
        try (var input = open(bytes, JOURNAL_MAGIC, RouteStorageLimits.MAX_INDEX_BYTES)) {
            boolean before = flag(input); String beforeHash = string(input, 64, false), afterHash = string(input, 64, false);
            if (!RouteEntry.hash(beforeHash) || !RouteEntry.hash(afterHash)) throw new IOException("Invalid journal index hash");
            var added = refs(input); var retired = refs(input); var adopted = refs(input); end(input); return new Journal(before, beforeHash, afterHash, added, retired, adopted);
        } catch (IllegalArgumentException invalid) { throw new IOException("Invalid transaction journal", invalid); }
    }
}
