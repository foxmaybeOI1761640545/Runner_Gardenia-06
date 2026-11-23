package de.dennisguse.opentracks.p0.routes;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.Assert.*;

public class GpxRouteImporterTest {
    private static final String OPEN = "<gpx xmlns='" + GpxRouteImporter.NAMESPACE + "' version='1.1' creator='unit'>";
    private static String gpx(String body) { return OPEN + body + "</gpx>"; }
    private static String point(String lat, String lon) { return "<rtept lat='" + lat + "' lon='" + lon + "'/>"; }
    private static String rte(int points) { return "<rte>" + point("0", "0").repeat(points) + "</rte>"; }
    private static GpxRouteImporter.Preview parse(String xml) throws Exception {
        return new GpxRouteImporter().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "planned.gpx", 123);
    }
    private static GpxRouteImporter.Preview limited(String xml, RouteLimits limits) throws Exception {
        return new GpxRouteImporter(limits).parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "planned.gpx", 123);
    }
    private static GpxRouteImporter.ImportException reject(String xml) throws Exception {
        try { parse(xml); throw new AssertionError("Accepted invalid document"); }
        catch (GpxRouteImporter.ImportException expected) { return expected; }
    }
    private static void rejectsLimit(String xml, RouteLimits limits) throws Exception {
        try { limited(xml, limits); fail("Limit was ignored"); }
        catch (GpxRouteImporter.ImportException expected) { assertEquals(GpxRouteImporter.ErrorCode.LIMIT, expected.code()); assertNull(expected.rejectedPreview()); }
    }
    @Test public void standardRteKeepsRouteAndPointMetadataSeparate() throws Exception {
        var p = parse(gpx("<rte><name>Plan</name><desc>description</desc><type>running</type><cmt>comment</cmt><number>7</number>"
                + "<rtept lat='1.3' lon='103.8'><name>Via</name><ele>12.5</ele><time>2026-11-23T09:00:00+08:00</time><sym>Flag</sym><hdop>1.2</hdop></rtept>"
                + point("1.301", "103.801") + "</rte>"));
        Route r = p.candidates().get(0).route(); assertEquals("Plan", r.displayName()); assertEquals("description", r.description());
        assertEquals("running", r.metadata().get("type")); assertEquals("7", r.metadata().get("number"));
        var via = r.segments().get(0).points().get(0); assertEquals("Via", via.metadata().get("name")); assertEquals(12.5, via.elevation(), 0);
        assertEquals("2026-11-23T09:00:00+08:00", via.metadata().get("time")); assertEquals("Flag", via.metadata().get("sym"));
        assertEquals("planned.gpx", r.sourceFileName()); assertEquals(123, r.importedAtMillis()); assertEquals(Route.SourceType.RTE, r.sourceType()); assertEquals(2, r.pointCount());
    }
    @Test public void prefixedNamespaceWorks() throws Exception {
        var p = parse("<g:gpx xmlns:g='" + GpxRouteImporter.NAMESPACE + "' version='1.1'><g:rte><g:name>Prefixed</g:name><g:rtept lat='0' lon='0'/></g:rte></g:gpx>");
        assertEquals("Prefixed", p.candidates().get(0).route().originalName());
    }
    @Test public void namespaceFreeCompatibilityIsExplicit() throws Exception {
        var p = parse("<gpx version='1.1'>" + rte(1) + "</gpx>"); assertFalse(p.warnings().isEmpty()); assertEquals(1, p.validRoutes());
    }
    @Test public void otherVersionAndForeignRootAreRejected() throws Exception {
        for (String xml : List.of("<gpx version='1.0'>" + rte(1) + "</gpx>", "<gpx xmlns='urn:foreign' version='1.1'>" + rte(1) + "</gpx>", "<other/>"))
            assertEquals(GpxRouteImporter.ErrorCode.UNSUPPORTED_GPX, reject(xml).code());
    }
    @Test public void multipleRoutesStayIndependentInDocumentOrder() throws Exception {
        var p = parse(gpx("<rte><name>A</name>" + point("0", "0") + "</rte><rte><name>B</name>" + point("1", "1") + "</rte>"));
        assertEquals(2, p.validRoutes()); assertEquals("A", p.candidates().get(0).route().displayName()); assertEquals("B", p.candidates().get(1).route().displayName());
        assertNotEquals(p.candidates().get(0).route().id(), p.candidates().get(1).route().id()); assertEquals(2, p.candidates().get(1).ordinal());
    }
    @Test public void tracksAndSegmentsAreNeverJoinedForDistance() throws Exception {
        var p = parse(gpx("<trk><name>T</name><trkseg><trkpt lat='0' lon='0'/><trkpt lat='0' lon='0.001'/></trkseg>"
                + "<trkseg><trkpt lat='50' lon='50'/><trkpt lat='50' lon='50.001'/></trkseg></trk>"));
        Route r = p.candidates().get(0).route(); assertEquals(Route.SourceType.TRK, r.sourceType()); assertEquals(2, r.segments().size());
        assertEquals(4, r.pointCount()); assertTrue(r.distanceMeters().orElseThrow() < 200); assertTrue(r.distanceMeters().orElseThrow() > 180);
    }
    @Test public void multipleTracksAndMixedSourcesRemainSeparate() throws Exception {
        var p = parse(gpx("<trk><trkseg><trkpt lat='0' lon='0'/></trkseg></trk>" + rte(1) + "<trk><trkseg><trkpt lat='1' lon='1'/></trkseg></trk>"));
        assertEquals(3, p.validRoutes()); assertEquals(Route.SourceType.TRK, p.candidates().get(0).type()); assertEquals(Route.SourceType.RTE, p.candidates().get(1).type());
    }
    @Test public void emptySegmentsStayExplicit() throws Exception {
        Route r = parse(gpx("<trk><trkseg/><trkseg><trkpt lat='0' lon='0'/></trkseg><trkseg/></trk>")).candidates().get(0).route();
        assertEquals(3, r.segments().size()); assertTrue(r.segments().get(0).points().isEmpty()); assertTrue(r.distanceMeters().isEmpty());
    }
    @Test public void missingOptionalFieldsAndSinglePointHaveNoFabricatedDistance() throws Exception {
        Route r = parse(gpx(rte(1))).candidates().get(0).route(); assertEquals("Route 1", r.displayName()); assertEquals("", r.originalName());
        assertEquals("", r.description()); assertNull(r.segments().get(0).points().get(0).elevation()); assertTrue(r.distanceMeters().isEmpty());
    }
    @Test public void onePointInEachSegmentHasUnavailableLength() throws Exception {
        Route r = parse(gpx("<trk><trkseg><trkpt lat='0' lon='0'/></trkseg><trkseg><trkpt lat='45' lon='45'/></trkseg></trk>")).candidates().get(0).route();
        assertTrue(r.distanceMeters().isEmpty());
    }
    @Test public void duplicateCoordinatesRetainMetadataAndHaveKnownZeroLength() throws Exception {
        var c = parse(gpx("<rte>" + point("0", "0") + "<rtept lat='0' lon='0'><name>Different annotation</name></rtept></rte>")).candidates().get(0);
        assertEquals(2, c.route().pointCount()); assertEquals(0, c.route().distanceMeters().orElseThrow(), 0); assertFalse(c.warnings().isEmpty());
        assertEquals("Different annotation", c.route().segments().get(0).points().get(1).metadata().get("name"));
    }
    @Test public void invalidCoordinateInvalidatesWholeCandidateWithoutGapJoining() throws Exception {
        for (String bad : List.of("90.1", "-90.1", "NaN", "Infinity", "0x1.0p0", "1e3", "", "bad")) {
            var p = parse(gpx("<rte>" + point("0", "0") + point(bad, "0") + point("1", "1") + "</rte>" + rte(1)));
            assertEquals(1, p.validRoutes()); assertFalse(p.candidates().get(0).valid()); assertTrue(p.candidates().get(0).error().contains("point 2")); assertNull(p.candidates().get(0).route());
        }
    }
    @Test public void longitudeBoundsAndMissingAttributesAreRejected() throws Exception {
        for (String item : List.of(point("0", "180.01"), point("0", "-180.01"), "<rtept lon='0'/>", "<rtept lat='0'/>")) {
            var e = reject(gpx("<rte>" + item + "</rte>")); assertEquals(GpxRouteImporter.ErrorCode.NO_VALID_ROUTES, e.code()); assertNotNull(e.rejectedPreview());
        }
    }
    @Test public void coordinateExtremesAndNegativeElevationAreValid() throws Exception {
        Route r = parse(gpx("<rte><rtept lat='-90' lon='-180'><ele>-430.5</ele></rtept>" + point("90", "180") + "</rte>")).candidates().get(0).route();
        assertTrue(r.distanceMeters().orElseThrow() > 20_000_000); assertEquals(-430.5, r.segments().get(0).points().get(0).elevation(), 0);
    }
    @Test public void invalidOptionalElevationIsMarkedInvalidRatherThanDropped() throws Exception {
        var e = reject(gpx("<rte><rtept lat='0' lon='0'><ele>NaN</ele></rtept></rte>")); assertTrue(e.rejectedPreview().candidates().get(0).error().contains("elevation"));
    }
    @Test public void standaloneWaypointsAreNotRoutes() throws Exception {
        var e = reject(gpx("<wpt lat='0' lon='0'><name>Only point</name></wpt>")); assertEquals(GpxRouteImporter.ErrorCode.NO_VALID_ROUTES, e.code());
        assertEquals(1, e.rejectedPreview().ignoredWaypoints()); assertTrue(e.rejectedPreview().candidates().isEmpty());
        var p = parse(gpx("<wpt lat='0' lon='0'/>" + rte(1))); assertEquals(1, p.validRoutes()); assertEquals(1, p.ignoredWaypoints());
    }
    @Test public void partialValidityIsVisibleAfterCompleteRead() throws Exception {
        var p = parse(gpx(rte(1) + "<rte><name>Empty</name></rte>" + "<trk><trkseg><trkpt lat='0' lon='0'/></trkseg></trk>"));
        assertEquals(3, p.candidates().size()); assertEquals(2, p.validRoutes()); assertEquals("Empty", p.candidates().get(1).name()); assertNotNull(p.candidates().get(1).error());
    }
    @Test public void allInvalidRoutesExposeRejectedPreviewWithReasons() throws Exception {
        var e = reject(gpx("<rte/><trk/>")); assertEquals(GpxRouteImporter.ErrorCode.NO_VALID_ROUTES, e.code());
        assertEquals(2, e.rejectedPreview().candidates().size()); assertTrue(e.rejectedPreview().candidates().stream().noneMatch(GpxRouteImporter.Candidate::valid));
    }
    @Test public void extensionSubtreesAndForeignElementsCannotCreateRoutesOrOverwriteNames() throws Exception {
        var p = parse(gpx("<extensions>" + rte(1) + "</extensions><rte><name>Original</name><extensions><name>Wrong</name>"
                + point("3", "3") + "</extensions><rtept lat='0' lon='0'><name xmlns='urn:foreign'>Wrong</name></rtept></rte>"));
        assertEquals(1, p.validRoutes()); Route r = p.candidates().get(0).route(); assertEquals("Original", r.originalName()); assertEquals(1, r.pointCount()); assertTrue(r.segments().get(0).points().get(0).metadata().isEmpty());
    }
    @Test public void standardLinksAndLiteralTimezoneFreeTimeAreRetained() throws Exception {
        Route r = parse(gpx("<rte><link href='https://example.invalid/offline-reference'><text>Source</text><type>text/plain</type></link>"
                + "<rtept lat='0' lon='0'><time>2026-11-23T09:00:00</time><link href='urn:via'><text>Via link</text></link></rtept></rte>")).candidates().get(0).route();
        assertEquals("Source", r.metadata().get("link.1.text")); assertEquals("https://example.invalid/offline-reference", r.metadata().get("link.1.href"));
        assertEquals("2026-11-23T09:00:00", r.segments().get(0).points().get(0).metadata().get("time"));
        assertTrue(r.segments().get(0).points().get(0).metadata().containsValue("Via link"));
    }
    @Test public void repeatedNamesAreExplicitAndWarningGrowthIsBounded() throws Exception {
        var c = parse(gpx("<rte><name>First</name>" + "<name>Again</name>".repeat(1000) + point("0", "0") + point("0", "0") + "</rte>")).candidates().get(0);
        assertEquals("First", c.route().originalName()); assertFalse(c.warnings().isEmpty()); assertTrue(c.warnings().size() <= 32);
    }
    @Test public void emptyFileIsRejected() throws Exception { assertEquals(GpxRouteImporter.ErrorCode.EMPTY_FILE, reject("").code()); }
    @Test public void malformedAndTrailingXmlNeverReturnEarlierValidRoute() throws Exception {
        for (String xml : List.of(OPEN + rte(1) + "<rte>", gpx(rte(1)) + "junk", gpx(rte(1)) + "<gpx/>")) {
            var e = reject(xml); assertEquals(GpxRouteImporter.ErrorCode.XML, e.code()); assertNull(e.rejectedPreview());
        }
    }
    @Test public void malformedEncodingAndUnknownDeclarationAreRejected() throws Exception {
        byte[] raw = gpx("<rte><name>Bad</name>" + point("0", "0") + "</rte>").getBytes(StandardCharsets.UTF_8); int index = new String(raw, StandardCharsets.UTF_8).indexOf("Bad"); raw[index] = (byte)0xff;
        try { new GpxRouteImporter().parse(new ByteArrayInputStream(raw), "bad.gpx", 1); fail(); }
        catch (GpxRouteImporter.ImportException e) { assertNull(e.rejectedPreview()); }
        assertEquals(GpxRouteImporter.ErrorCode.ENCODING, reject("<?xml version='1.0' encoding='made-up-encoding'?>" + gpx(rte(1))).code());
    }
    @Test public void externalInternalAndParameterDtdAreRejected() throws Exception {
        for (String dtd : List.of("<!DOCTYPE gpx SYSTEM 'http://127.0.0.1:9/never'>", "<!DOCTYPE gpx [<!ENTITY x SYSTEM 'file:///etc/passwd'>]>",
                "<!DOCTYPE gpx [<!ENTITY x 'expanded'>]>", "<!DOCTYPE gpx [<!ENTITY % x SYSTEM 'http://127.0.0.1:9/no'>%x;]>")) {
            var e = reject(dtd + gpx(rte(1))); assertEquals(GpxRouteImporter.ErrorCode.XML, e.code()); assertTrue(e.getMessage().contains("DTD")); assertNull(e.rejectedPreview());
        }
    }
    @Test public void schemaLocationIsNeverDownloaded() throws Exception {
        var p = parse("<gpx version='1.1' xmlns='" + GpxRouteImporter.NAMESPACE + "' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='"
                + GpxRouteImporter.NAMESPACE + " http://127.0.0.1:9/not-downloaded.xsd'>" + rte(1) + "</gpx>"); assertEquals(1, p.validRoutes());
    }
    @Test public void providerReadFailureClosesStreamAndDoesNotExposePartialCandidates() throws Exception {
        final boolean[] closed = {false};
        InputStream broken = new InputStream() { int count; final byte[] bytes = (OPEN + rte(1) + " ".repeat(100)).getBytes(StandardCharsets.UTF_8);
            @Override public int read() throws IOException { if (count == bytes.length) throw new IOException("provider interrupted file"); return bytes[count++]; }
            @Override public void close() { closed[0] = true; }
        };
        try { new GpxRouteImporter().parse(broken, "broken.gpx", 1); fail(); }
        catch (GpxRouteImporter.ImportException e) { assertEquals(GpxRouteImporter.ErrorCode.READ, e.code()); assertNull(e.rejectedPreview()); }
        assertTrue(closed[0]);
    }
    @Test public void failedOpenIsReadErrorRatherThanEmptyFile() throws Exception {
        try { new GpxRouteImporter().parse(new InputStream() { @Override public int read() throws IOException { throw new IOException("access denied"); } }, null, 1); fail(); }
        catch (GpxRouteImporter.ImportException e) { assertEquals(GpxRouteImporter.ErrorCode.READ, e.code()); assertTrue(e.getMessage().contains("access denied")); }
    }
    @Test public void threadCancellationIsRejectedAndClosesOwnedStream() throws Exception {
        final boolean[] closed = {false}; InputStream input = new FilterInputStream(new ByteArrayInputStream(gpx(rte(1)).getBytes(StandardCharsets.UTF_8))) { @Override public void close() throws IOException { closed[0] = true; super.close(); } };
        Thread.currentThread().interrupt();
        try { new GpxRouteImporter().parse(input, "cancel.gpx", 1); fail(); }
        catch (GpxRouteImporter.ImportException e) { assertEquals(GpxRouteImporter.ErrorCode.INTERRUPTED, e.code()); }
        finally { Thread.interrupted(); }
        assertTrue(closed[0]);
    }
    @Test public void exactAndOverByteBudgetAreChecked() throws Exception {
        String xml = gpx(rte(1)); int bytes = xml.getBytes(StandardCharsets.UTF_8).length;
        assertEquals(1, limited(xml, new RouteLimits(bytes, 10, 10, 10, 32, 2048, 10000)).validRoutes());
        rejectsLimit(xml, new RouteLimits(bytes - 1, 10, 10, 10, 32, 2048, 10000));
    }
    @Test public void actualDefault16MiBFileBoundaryIsEnforced() throws Exception {
        int size = (int)RouteLimits.DEFAULT.fileBytes(); String head = OPEN + "<!--", tail = "-->" + rte(1) + "</gpx>";
        String exact = head + "x".repeat(size - head.length() - tail.length()) + tail;
        assertEquals(1, parse(exact).validRoutes()); assertEquals(GpxRouteImporter.ErrorCode.LIMIT, reject(exact + " ").code());
    }
    @Test public void actualDefault20000PointBoundaryIncludesInvalidCandidates() throws Exception {
        assertEquals(20_000, parse(gpx(rte(20_000))).candidates().get(0).points());
        assertEquals(GpxRouteImporter.ErrorCode.LIMIT, reject(gpx(rte(20_001))).code());
        String bad = "<rte>" + point("91", "0").repeat(20_000) + "</rte>";
        assertEquals(GpxRouteImporter.ErrorCode.LIMIT, reject(gpx(bad + rte(1))).code());
    }
    @Test public void actualDefaultSegmentBoundaryCountsEmptySegments() throws Exception {
        String point = "<trkseg><trkpt lat='0' lon='0'/></trkseg>";
        assertEquals(1024, parse(gpx("<trk>" + "<trkseg/>".repeat(1023) + point + "</trk>")).candidates().get(0).segments());
        assertEquals(GpxRouteImporter.ErrorCode.LIMIT, reject(gpx("<trk>" + "<trkseg/>".repeat(1024) + point + "</trk>")).code());
    }
    @Test public void actualDefaultRouteBoundaryCountsInvalidRoutes() throws Exception {
        assertEquals(128, parse(gpx(rte(1).repeat(128))).validRoutes());
        assertEquals(GpxRouteImporter.ErrorCode.LIMIT, reject(gpx("<rte/>".repeat(128) + rte(1))).code());
    }
    @Test public void xmlDepthBoundaryIsEnforcedEvenInsideIgnoredExtensions() throws Exception {
        assertEquals(1, parse(gpx("<extensions>" + "<x>".repeat(30) + "</x>".repeat(30) + "</extensions>" + rte(1))).validRoutes());
        assertEquals(GpxRouteImporter.ErrorCode.LIMIT, reject(gpx("<extensions>" + "<x>".repeat(31) + "</x>".repeat(31) + "</extensions>" + rte(1))).code());
    }
    @Test public void fieldAndTotalMetadataLimitsHaveExactBoundaries() throws Exception {
        String xml = gpx("<rte><name>" + "a".repeat(2048) + "</name>" + point("0", "0") + "</rte>");
        assertEquals(2048, parse(xml).candidates().get(0).route().originalName().length());
        assertEquals(GpxRouteImporter.ErrorCode.LIMIT, reject(xml.replace("a".repeat(2048), "a".repeat(2049))).code());
        String small = gpx("<rte><name>abc</name>" + point("0", "0") + "</rte>");
        assertEquals(1, limited(small, new RouteLimits(10000, 10, 10, 10, 32, 2048, 8)).validRoutes());
        rejectsLimit(small, new RouteLimits(10000, 10, 10, 10, 32, 2048, 7));
    }
    @Test public void emptyLinkMetadataConsumesBudgetRatherThanGrowingWithoutBound() throws Exception {
        String xml = gpx("<rte>" + "<link href=''/ >".replace("/ >", "/>").repeat(100) + point("0", "0") + "</rte>");
        rejectsLimit(xml, new RouteLimits(10000, 10, 10, 10, 32, 2048, 50));
    }
    @Test public void utf16EncodingPreservesNamesAndHashesActualBytes() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-16'?>" + gpx("<rte><name>离线路线</name>" + point("0", "0") + "</rte>");
        byte[] bytes = xml.getBytes(StandardCharsets.UTF_16); var preview = new GpxRouteImporter().parse(new ByteArrayInputStream(bytes), "中文.gpx", 123);
        assertEquals("离线路线", preview.candidates().get(0).route().originalName());
        StringBuilder expected = new StringBuilder(); for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) expected.append(String.format("%02x", value & 255));
        assertEquals(expected.toString(), preview.sha256());
    }
    @Test public void longDisplayAliasNeverSplitsUnicodeSurrogatePair() throws Exception {
        String name = "😀".repeat(1024); Route route = parse(gpx("<rte><name>" + name + "</name>" + point("0", "0") + "</rte>")).candidates().get(0).route();
        assertEquals(name, route.originalName()); assertTrue(route.displayName().length() <= 256);
        assertEquals(route.displayName(), new String(route.displayName().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
    }
    @Test public void utf8BomIsAccepted() throws Exception {
        byte[] xml = gpx(rte(1)).getBytes(StandardCharsets.UTF_8); byte[] bytes = new byte[xml.length + 3];
        bytes[0] = (byte)0xef; bytes[1] = (byte)0xbb; bytes[2] = (byte)0xbf; System.arraycopy(xml, 0, bytes, 3, xml.length);
        assertEquals(1, new GpxRouteImporter().parse(new ByteArrayInputStream(bytes), "bom.gpx", 1).validRoutes());
    }
    @Test public void sourceHashCoversOriginalBytesAndMissingFilenameStaysUnknown() throws Exception {
        byte[] bytes = gpx(rte(1)).getBytes(StandardCharsets.UTF_8); var p = new GpxRouteImporter().parse(new ByteArrayInputStream(bytes), null, 2);
        StringBuilder hash = new StringBuilder(); for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) hash.append(String.format("%02x", b & 255));
        assertEquals(hash.toString(), p.sha256()); assertEquals(p.sha256(), p.candidates().get(0).route().sourceSha256()); assertNull(p.fileName()); assertNull(p.candidates().get(0).route().sourceFileName());
    }
    @Test public void modelIsImmutableAndRenamePreservesOriginalData() throws Exception {
        Route r = parse(gpx("<rte><name>Original</name>" + point("0", "0") + "</rte>")).candidates().get(0).route(); Route renamed = r.renamed(" Local ");
        assertEquals(r.id(), renamed.id()); assertEquals(r.duplicateKey(), renamed.duplicateKey()); assertEquals("Original", renamed.originalName()); assertEquals("Local", renamed.displayName()); assertEquals(r.segments(), renamed.segments());
        try { r.metadata().put("name", "changed"); fail(); } catch (UnsupportedOperationException expected) { }
        try { r.segments().get(0).points().clear(); fail(); } catch (UnsupportedOperationException expected) { }
        for (String invalid : List.of("", " ", "x".repeat(257))) try { r.renamed(invalid); fail(); } catch (IllegalArgumentException expected) { }
        assertEquals(256, r.renamed("x".repeat(256)).displayName().length());
    }
    @Test public void geographyUsesReliableGreatCircleAndHandlesDateLine() {
        assertEquals(111195.0802, RouteDistance.meters(0, 0, 0, 1), 0.01); assertEquals(222390.1605, RouteDistance.meters(0, 179, 0, -179), 0.01);
        assertEquals(0, RouteDistance.meters(10, 20, 10, 20), 0); assertTrue(RouteDistance.total(List.of()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new RoutePoint(Double.NaN, 0, null, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RoutePoint(0, 181, null, Map.of()));
    }
}
