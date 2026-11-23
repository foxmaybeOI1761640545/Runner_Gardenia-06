package de.dennisguse.opentracks.p0.routes;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.Assert.*;

/** Actual ART/Android SAX security and budgets; no Activity, GPS owner, service or history writes. */
@RunWith(AndroidJUnit4.class)
public class P08GpxParserPlatformTest {
    private static final String ROOT = "<gpx version='1.1' xmlns='" + GpxRouteImporter.NAMESPACE + "'>";
    private static String point() { return "<rtept lat='0' lon='0'/>"; }
    private static GpxRouteImporter.Preview parse(String xml) throws Exception {
        return new GpxRouteImporter().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "platform.gpx", 123);
    }
    private static GpxRouteImporter.ImportException rejected(String xml) throws Exception {
        try { parse(xml); throw new AssertionError("Invalid GPX accepted on Android"); }
        catch (GpxRouteImporter.ImportException expected) { return expected; }
    }
    @Test public void nativeNamespacesAndMultipleSegmentsArePreserved() throws Exception {
        var p = parse("<g:gpx version='1.1' xmlns:g='" + GpxRouteImporter.NAMESPACE + "'><g:rte><g:name>规划</g:name><g:rtept lat='0' lon='0'/></g:rte>"
                + "<g:trk><g:trkseg><g:trkpt lat='0' lon='1'/></g:trkseg><g:trkseg><g:trkpt lat='40' lon='40'/></g:trkseg></g:trk></g:gpx>");
        assertEquals(2, p.validRoutes()); assertEquals("规划", p.candidates().get(0).route().originalName()); assertEquals(2, p.candidates().get(1).route().segments().size());
        assertTrue(p.candidates().get(1).route().distanceMeters().isEmpty());
    }
    @Test public void nativeSaxRejectsInternalExternalAndParameterDtd() throws Exception {
        for (String declaration : List.of("<!DOCTYPE gpx SYSTEM 'http://127.0.0.1:9/never'>", "<!DOCTYPE gpx [<!ENTITY x SYSTEM 'file:///etc/passwd'>]>",
                "<!DOCTYPE gpx [<!ENTITY x 'expanded'>]>", "<!DOCTYPE gpx [<!ENTITY % x SYSTEM 'http://127.0.0.1:9/no'>%x;]>")) {
            var e = rejected(declaration + ROOT + "<rte>" + point() + "</rte></gpx>"); assertEquals(GpxRouteImporter.ErrorCode.XML, e.code()); assertTrue(e.getMessage().contains("DTD")); assertNull(e.rejectedPreview());
        }
    }
    @Test public void nativeUtf16AndLiteralMetadataSurvive() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-16'?>" + ROOT + "<rte><name>海边路线</name><rtept lat='0' lon='0'><name>起点</name><time>2026-11-23T09:00:00</time></rtept></rte></gpx>";
        var p = new GpxRouteImporter().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_16)), "中文.gpx", 1);
        assertEquals("海边路线", p.candidates().get(0).route().originalName()); assertEquals("起点", p.candidates().get(0).route().segments().get(0).points().get(0).metadata().get("name"));
        assertEquals("2026-11-23T09:00:00", p.candidates().get(0).route().segments().get(0).points().get(0).metadata().get("time"));
    }
    @Test public void nativeActual20000PointBoundary() throws Exception {
        long start = android.os.SystemClock.elapsedRealtime();
        assertEquals(20_000, parse(ROOT + "<rte>" + point().repeat(20_000) + "</rte></gpx>").candidates().get(0).points());
        assertEquals(GpxRouteImporter.ErrorCode.LIMIT, rejected(ROOT + "<rte>" + point().repeat(20_001) + "</rte></gpx>").code());
        measurement("points20000", start);
    }
    @Test public void nativeActual16MiBFileBoundary() throws Exception {
        long start = android.os.SystemClock.elapsedRealtime(); String head = ROOT + "<!--", tail = "--><rte>" + point() + "</rte></gpx>";
        String exact = head + "x".repeat((int)RouteLimits.DEFAULT.fileBytes() - head.length() - tail.length());
        exact += tail; assertEquals(1, parse(exact).validRoutes()); assertEquals(GpxRouteImporter.ErrorCode.LIMIT, rejected(exact + " ").code()); measurement("bytes16MiB", start);
    }
    @Test public void nativeMalformedDocumentHasNoPartialSuccessButInvalidCandidatesRemainVisible() throws Exception {
        var p = parse(ROOT + "<rte>" + point() + "</rte><rte><rtept lat='91' lon='0'/></rte></gpx>");
        assertEquals(2, p.candidates().size()); assertEquals(1, p.validRoutes()); assertFalse(p.candidates().get(1).valid());
        var e = rejected(ROOT + "<rte>" + point() + "</rte><rte>"); assertNull(e.rejectedPreview()); assertEquals(GpxRouteImporter.ErrorCode.XML, e.code());
    }
    @Test public void nativeCancelledReaderReturnsNoRoute() throws Exception {
        Thread.currentThread().interrupt();
        try { parse(ROOT + "<rte>" + point() + "</rte></gpx>"); fail(); }
        catch (GpxRouteImporter.ImportException e) { assertEquals(GpxRouteImporter.ErrorCode.INTERRUPTED, e.code()); assertNull(e.rejectedPreview()); }
        finally { Thread.interrupted(); }
    }
    @Test public void nativeWaypointIsNotRouteAndSinglePointHasNoDistance() throws Exception {
        var e = rejected(ROOT + "<wpt lat='0' lon='0'/></gpx>"); assertEquals(GpxRouteImporter.ErrorCode.NO_VALID_ROUTES, e.code()); assertEquals(1, e.rejectedPreview().ignoredWaypoints());
        assertTrue(parse(ROOT + "<rte>" + point() + "</rte></gpx>").candidates().get(0).route().distanceMeters().isEmpty());
        assertEquals(222390.1605, RouteDistance.meters(0, 179, 0, -179), 0.02);
    }
    private static void measurement(String name, long start) throws Exception {
        String output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
        var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        var base = output != null ? new java.io.File(output) : new java.io.File(context.getExternalMediaDirs()[0], "additional_test_output");
        var folder = new java.io.File(base, "p08-parser"); assertTrue(folder.isDirectory() || folder.mkdirs());
        Runtime runtime = Runtime.getRuntime();
        var json = new org.json.JSONObject().put("source", InstrumentationRegistry.getArguments().getString("buildCommit"))
                .put("api", android.os.Build.VERSION.SDK_INT).put("case", name).put("elapsedMillis", android.os.SystemClock.elapsedRealtime() - start)
                .put("maxJavaHeapBytes", runtime.maxMemory()).put("currentJavaHeapUsedBytes", runtime.totalMemory() - runtime.freeMemory())
                .put("scope", "Actual Android SAX boundary timing/current heap, not a sampled total-process memory peak");
        try (var stream = new java.io.FileOutputStream(new java.io.File(folder, name + ".json"))) { stream.write(json.toString(2).getBytes(StandardCharsets.UTF_8)); }
    }
}
