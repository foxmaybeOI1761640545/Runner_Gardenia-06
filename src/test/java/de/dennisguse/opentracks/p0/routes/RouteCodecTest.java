package de.dennisguse.opentracks.p0.routes;

import org.junit.Test;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.Assert.*;

public class RouteCodecTest {
    private static Route route() {
        return new Route(UUID.fromString("00000000-0000-4000-8000-000000000001"), "初始路线", "planned.gpx", "a".repeat(64), 1, Route.SourceType.TRK, 1234,
                Map.of("name", "原始名称", "desc", "描述"), List.of(new RouteSegment(List.of(new RoutePoint(12.345678, 98.123456, 456.75, Map.of("name", "途经点", "time", "2026-11-23T09:00:00+08:00")))), new RouteSegment(List.of())));
    }
    private static Route with(Route r, Map<String,String> metadata, List<RouteSegment> segments) {
        return new Route(r.id(), r.displayName(), r.sourceFileName(), r.sourceSha256(), r.sourceOrdinal(), r.sourceType(), r.importedAtMillis(), metadata, segments);
    }
    private static byte[] rehash(byte[] bytes) throws Exception {
        int length = ByteBuffer.wrap(bytes, 12, 4).getInt(); MessageDigest hash = MessageDigest.getInstance("SHA-256"); hash.update(bytes, 16, length);
        System.arraycopy(hash.digest(), 0, bytes, 16 + length, 32); return bytes;
    }
    private static int find(byte[] source, byte[] needle) {
        outer: for (int at = 16; at <= source.length - 32 - needle.length; at++) {
            for (int i = 0; i < needle.length; i++) if (source[at + i] != needle[i]) continue outer;
            return at;
        }
        throw new AssertionError("Expected encoded field absent");
    }
    @Test public void schemaEnvelopeHasDocumentedHeaderLengthAndActualSha256() throws Exception {
        byte[] bytes = RouteCodec.route(route()); assertEquals("RGROUTE!", new String(bytes, 0, 8, StandardCharsets.US_ASCII));
        assertEquals(1, ByteBuffer.wrap(bytes, 8, 4).getInt()); assertEquals(bytes.length - 48, ByteBuffer.wrap(bytes, 12, 4).getInt());
        var digest = MessageDigest.getInstance("SHA-256"); digest.update(bytes, 16, bytes.length - 48); assertArrayEquals(digest.digest(), Arrays.copyOfRange(bytes, bytes.length - 32, bytes.length));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", RouteCodec.sha256("abc".getBytes(StandardCharsets.US_ASCII)));
    }
    @Test public void routeRoundTripPreservesEveryStandardFieldAndEmptySegment() throws Exception { Route original = route(); assertEquals(original, RouteCodec.route(RouteCodec.route(original))); }
    @Test public void canonicalEncodingDoesNotDependOnMapIterationOrder() throws Exception {
        Route original = route(); Map<String,String> first = new LinkedHashMap<>(), second = new LinkedHashMap<>(); first.put("name", "X"); first.put("desc", "Y"); second.put("desc", "Y"); second.put("name", "X");
        assertArrayEquals(RouteCodec.route(with(original, first, original.segments())), RouteCodec.route(with(original, second, original.segments())));
    }
    @Test public void everyTruncationAndExtraTrailingByteIsRejected() throws Exception {
        byte[] bytes = RouteCodec.route(route());
        for (int length = 0; length < bytes.length; length++) { byte[] truncated = Arrays.copyOf(bytes, length); assertThrows(IOException.class, () -> RouteCodec.route(truncated)); }
        assertThrows(IOException.class, () -> RouteCodec.route(Arrays.copyOf(bytes, bytes.length + 1)));
    }
    @Test public void checksumHeaderAndPayloadLengthCorruptionAreRejected() throws Exception {
        byte[] bytes = RouteCodec.route(route());
        for (int offset : List.of(0, 12, 40, bytes.length - 1)) { byte[] bad = bytes.clone(); bad[offset] ^= 1; assertThrows(IOException.class, () -> RouteCodec.route(bad)); }
    }
    @Test public void unknownFutureVersionIsSpecificAndOldInvalidVersionIsRejected() throws Exception {
        byte[] bytes = RouteCodec.route(route()); ByteBuffer.wrap(bytes, 8, 4).putInt(2); assertThrows(RouteCodec.FutureVersion.class, () -> RouteCodec.route(bytes));
        ByteBuffer.wrap(bytes, 8, 4).putInt(0); assertThrows(IOException.class, () -> RouteCodec.route(bytes));
    }
    @Test public void oversizedStringLengthIsRejectedBeforeAllocation() throws Exception {
        byte[] bytes = RouteCodec.route(route()); ByteBuffer.wrap(bytes, 32, 4).putInt(Integer.MAX_VALUE); rehash(bytes); assertThrows(IOException.class, () -> RouteCodec.route(bytes));
    }
    @Test public void malformedUtf8WithValidChecksumIsRejected() throws Exception {
        byte[] bytes = RouteCodec.route(route()); bytes[36] = (byte)0xff; rehash(bytes); assertThrows(IOException.class, () -> RouteCodec.route(bytes));
    }
    @Test public void invalidCoordinateWithValidChecksumIsRejected() throws Exception {
        byte[] bytes = RouteCodec.route(route()); byte[] value = ByteBuffer.allocate(8).putDouble(12.345678).array(); int at = find(bytes, value);
        ByteBuffer.wrap(bytes, at, 8).putDouble(Double.NaN); rehash(bytes); assertThrows(IOException.class, () -> RouteCodec.route(bytes));
    }
    @Test public void unexpectedExtraPayloadFieldsAreRejectedEvenWhenChecksumMatches() throws Exception {
        byte[] bytes = RouteCodec.route(route()); byte[] extra = RouteCodec.envelope(RouteCodec.ROUTE_MAGIC, RouteStorageLimits.MAX_ROUTE_FILE_BYTES, output -> { output.write(bytes, 16, bytes.length - 48); output.writeInt(0); });
        assertThrows(IOException.class, () -> RouteCodec.route(extra));
    }
    @Test public void malformedOptionalBooleanIsRejected() throws Exception {
        byte[] bytes = RouteCodec.route(route()); int latitude = find(bytes, ByteBuffer.allocate(8).putDouble(12.345678).array()); bytes[latitude + 16] = 2; rehash(bytes);
        assertThrows(IOException.class, () -> RouteCodec.route(bytes));
    }
    @Test public void fieldLengthAndUnicodeEncodingAreBoundedOnWrite() {
        Route original = route(); assertThrows(IOException.class, () -> RouteCodec.route(with(original, Map.of("desc", "x".repeat(2049)), original.segments())));
        assertThrows(IOException.class, () -> RouteCodec.route(with(original, Map.of("desc", "\ud800"), original.segments())));
    }
    @Test public void actualMetadataBudgetHasExactBoundary() throws Exception {
        Route original = route(); Map<String,String> values = new LinkedHashMap<>();
        for (int i = 0; i < 510; i++) values.put(String.format("key%04d", i), "x".repeat(2048)); values.put("key0510", "x".repeat(8));
        assertEquals(1_048_576, values.entrySet().stream().mapToInt(entry -> entry.getKey().length() + entry.getValue().length() + 1).sum());
        Route exact = with(original, values, List.of(new RouteSegment(List.of(new RoutePoint(0, 0, null, Map.of())))));
        assertEquals(exact, RouteCodec.route(RouteCodec.route(exact))); values.put("extra", "");
        assertThrows(IOException.class, () -> RouteCodec.route(with(original, values, exact.segments())));
    }
    @Test public void actualPointBudgetHasExactBoundary() throws Exception {
        Route original = route(); RoutePoint point = new RoutePoint(0, 0, null, Map.of());
        byte[] exact = RouteCodec.route(with(original, Map.of(), List.of(new RouteSegment(Collections.nCopies(20_000, point))))); assertEquals(20_000, RouteCodec.route(exact).pointCount());
        assertThrows(IOException.class, () -> RouteCodec.route(with(original, Map.of(), List.of(new RouteSegment(Collections.nCopies(20_001, point))))));
    }
    @Test public void actualSegmentBudgetCountsEmptySegments() throws Exception {
        List<RouteSegment> segments = new ArrayList<>(); segments.add(new RouteSegment(List.of(new RoutePoint(0, 0, null, Map.of()))));
        for (int i = 1; i < 1024; i++) segments.add(new RouteSegment(List.of()));
        assertEquals(1024, RouteCodec.route(RouteCodec.route(with(route(), Map.of(), segments))).segments().size()); segments.add(new RouteSegment(List.of()));
        assertThrows(IOException.class, () -> RouteCodec.route(with(route(), Map.of(), segments)));
    }
    @Test public void byteBudgetRejectsBeforeLargeDecodeAndBoundsEncoder() throws Exception {
        assertThrows(IOException.class, () -> RouteCodec.route(new byte[RouteStorageLimits.MAX_ROUTE_FILE_BYTES + 1]));
        assertThrows(IOException.class, () -> RouteCodec.index(new byte[RouteStorageLimits.MAX_INDEX_BYTES + 1]));
        byte[] exact = RouteCodec.envelope(RouteCodec.ROUTE_MAGIC, 96, output -> output.write(new byte[48])); assertEquals(96, exact.length);
        assertEquals(48, RouteCodec.open(exact, RouteCodec.ROUTE_MAGIC, 96).available());
        assertThrows(IOException.class, () -> RouteCodec.envelope(RouteCodec.ROUTE_MAGIC, 96, output -> output.write(new byte[49])));
    }
    @Test public void indexRoundTripPreservesAliasAndExactIntegrityReference() throws Exception {
        Route route = route(); RouteEntry entry = RouteEntry.of(route, RouteCodec.route(route)).renamed("Local alias");
        assertEquals(List.of(entry), RouteCodec.index(RouteCodec.index(List.of(entry)))); assertNotEquals(route.displayName(), entry.displayName());
    }
    @Test public void duplicateIndexedUuidIsRejectedOnWriteAndRead() throws Exception {
        RouteEntry entry = RouteEntry.of(route(), RouteCodec.route(route())); assertThrows(IOException.class, () -> RouteCodec.index(List.of(entry, entry)));
        byte[] bytes = RouteCodec.index(List.of(entry)); byte[] duplicate = RouteCodec.envelope(RouteCodec.INDEX_MAGIC, RouteStorageLimits.MAX_INDEX_BYTES,
                output -> { output.writeInt(2); output.write(bytes, 20, bytes.length - 52); output.write(bytes, 20, bytes.length - 52); });
        assertThrows(IOException.class, () -> RouteCodec.index(duplicate));
    }
    @Test public void actualIndexCapacityHasExactBoundary() throws Exception {
        List<RouteEntry> entries = new ArrayList<>(); Route original = route(); byte[] file = RouteCodec.route(original); RouteEntry template = RouteEntry.of(original, file);
        for (int i = 0; i < 1024; i++) entries.add(new RouteEntry(UUID.randomUUID(), template.displayName(), template.sourceFileName(), template.sourceSha256(), i + 1,
                template.sourceType(), template.importedAtMillis(), template.pointCount(), template.segmentCount(), template.distanceMeters(), template.fileBytes(), template.fileSha256()));
        assertEquals(1024, RouteCodec.index(RouteCodec.index(entries)).size()); entries.add(template); assertThrows(IOException.class, () -> RouteCodec.index(entries));
    }
    @Test public void journalKeepsNewRetiredAndAdoptedFileOwnershipSeparate() throws Exception {
        var added = new RouteCodec.FileRef(UUID.randomUUID(), "a".repeat(64)); var retired = new RouteCodec.FileRef(UUID.randomUUID(), "b".repeat(64)); var adopted = new RouteCodec.FileRef(UUID.randomUUID(), "c".repeat(64));
        var journal = new RouteCodec.Journal(true, "d".repeat(64), "e".repeat(64), List.of(added), List.of(retired), List.of(adopted));
        assertEquals(journal, RouteCodec.journal(RouteCodec.journal(journal))); byte[] bad = RouteCodec.journal(journal); bad[40] ^= 1; assertThrows(IOException.class, () -> RouteCodec.journal(bad));
    }
    @Test public void duplicateJournalUuidAndInvalidHashAreRejected() throws Exception {
        var file = new RouteCodec.FileRef(UUID.randomUUID(), "a".repeat(64));
        assertThrows(IOException.class, () -> RouteCodec.journal(new RouteCodec.Journal(true, "b".repeat(64), "c".repeat(64), List.of(file, file), List.of(), List.of())));
        assertThrows(IllegalArgumentException.class, () -> new RouteCodec.Journal(true, "bad", "c".repeat(64), List.of(), List.of(), List.of()));
    }
}
