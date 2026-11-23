package de.dennisguse.opentracks.p0.routes;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Bounded namespace-aware GPX1.1 route reader; no history repository, network or Android dependencies. */
public final class GpxRouteImporter {
    public static final String NAMESPACE = "http://www.topografix.com/GPX/1/1";
    public enum ErrorCode { EMPTY_FILE, XML, UNSUPPORTED_GPX, LIMIT, READ, ENCODING, INTERRUPTED, NO_VALID_ROUTES }
    public record Candidate(int ordinal, String name, Route.SourceType type, int points, int segments,
                            Route route, String error, List<String> warnings) {
        public Candidate { warnings = List.copyOf(warnings); }
        public boolean valid() { return route != null && error == null; }
    }
    public record Preview(String fileName, String sha256, List<Candidate> candidates,
                          int ignoredWaypoints, List<String> warnings) {
        public Preview { candidates = List.copyOf(candidates); warnings = List.copyOf(warnings); }
        public long validRoutes() { return candidates.stream().filter(Candidate::valid).count(); }
    }
    public static final class ImportException extends IOException {
        private final ErrorCode code;
        private final Preview rejectedPreview;
        ImportException(ErrorCode code, String message, Throwable cause, Preview preview) {
            super(message, cause); this.code = code; this.rejectedPreview = preview;
        }
        public ErrorCode code() { return code; }
        /** Only a fully read well-formed document may expose invalid candidate details. */
        public Preview rejectedPreview() { return rejectedPreview; }
    }
    private static final class Rejected extends SAXException {
        final ErrorCode code;
        Rejected(ErrorCode code, String message) { super(message); this.code = code; }
    }
    private final RouteLimits limits;
    public GpxRouteImporter() { this(RouteLimits.DEFAULT); }
    public GpxRouteImporter(RouteLimits limits) { this.limits = limits; }

    /** Takes ownership of the stream. A failed/truncated document never returns partially parsed routes. */
    public Preview parse(InputStream input, String fileName, long importedAtMillis) throws ImportException {
        if (input == null) throw failure(ErrorCode.READ, "The document provider returned no input stream", null, null);
        if (fileName != null && fileName.length() > 2_048) {
            try { input.close(); } catch (IOException ignored) { }
            throw failure(ErrorCode.LIMIT, "Source file name exceeds 2048 characters", null, null);
        }
        LimitedInputStream bounded = new LimitedInputStream(input, limits.fileBytes());
        Handler handler = new Handler(limits);
        try (bounded) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream hashed = new DigestInputStream(bounded, digest);
            // SAX implementations close their InputSource. Retain ownership until hash/EOF validation completes.
            InputStream source = new FilterInputStream(hashed) {
                @Override public void close() {}
                @Override public boolean markSupported() { return false; }
                @Override public void mark(int limit) {}
                @Override public void reset() throws IOException { throw new IOException("GPX stream is forward-only"); }
                @Override public long skip(long amount) throws IOException {
                    long skipped = 0; byte[] buffer = new byte[4096];
                    while (skipped < amount) {
                        int count = read(buffer, 0, (int)Math.min(buffer.length, amount - skipped));
                        if (count < 0) break; skipped += count;
                    }
                    return skipped;
                }
            };
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true); factory.setValidating(false);
            try { factory.setXIncludeAware(false); } catch (UnsupportedOperationException ignored) { }
            XMLReader reader = factory.newSAXParser().getXMLReader();
            securityFeature(reader, XMLConstants.FEATURE_SECURE_PROCESSING, true);
            securityFeature(reader, "http://xml.org/sax/features/external-general-entities", false);
            securityFeature(reader, "http://xml.org/sax/features/external-parameter-entities", false);
            securityFeature(reader, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            // Mandatory lexical DTD rejection, including internal entities, is the portable safety gate.
            reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            reader.setContentHandler(handler); reader.setEntityResolver(handler); reader.setErrorHandler(handler);
            reader.parse(new InputSource(source));
            byte[] drain = new byte[4096]; while (hashed.read(drain) != -1) { /* Hash all bytes through EOF. */ }
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest.digest()) { hex.append(Character.forDigit((value >>> 4) & 15, 16)); hex.append(Character.forDigit(value & 15, 16)); }
            String sha = hex.toString();
            Preview preview = handler.preview(fileName, sha, importedAtMillis);
            if (preview.validRoutes() == 0) throw failure(ErrorCode.NO_VALID_ROUTES,
                    "No valid rte/trk route; standalone waypoints are not planned routes", null, preview);
            return preview;
        } catch (ImportException own) { throw own; }
        catch (Exception error) {
            Throwable cause = error;
            while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
            ErrorCode code = error instanceof Rejected rejected ? rejected.code
                    : cause instanceof InterruptedIOException || Thread.currentThread().isInterrupted() ? ErrorCode.INTERRUPTED
                    : cause instanceof SizeLimit ? ErrorCode.LIMIT
                    : cause instanceof java.io.UnsupportedEncodingException || cause instanceof java.io.CharConversionException
                    || cause instanceof java.nio.charset.CharacterCodingException ? ErrorCode.ENCODING
                    : error instanceof IOException ? ErrorCode.READ
                    : bounded.count == 0 && bounded.eof ? ErrorCode.EMPTY_FILE : ErrorCode.XML;
            throw failure(code, error.getMessage() == null ? "Cannot parse GPX document" : error.getMessage(), error, null);
        }
    }
    private static void securityFeature(XMLReader reader, String name, boolean value) throws SAXException {
        try { reader.setFeature(name, value); }
        catch (SAXNotRecognizedException | SAXNotSupportedException ignored) {
            // Android/JVM differ in optional features; required lexical DTD rejection and throwing resolver remain installed.
        }
    }
    private static ImportException failure(ErrorCode code, String message, Throwable cause, Preview preview) {
        return new ImportException(code, message, cause, preview);
    }
    private static final class SizeLimit extends IOException { SizeLimit() { super("GPX exceeds input byte limit"); } }
    private static final class LimitedInputStream extends FilterInputStream {
        private final long limit; private long count; private boolean eof;
        LimitedInputStream(InputStream input, long limit) { super(input); this.limit = limit; }
        private void checked(int bytes) throws IOException {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("GPX read interrupted");
            if (bytes > 0 && (count += bytes) > limit) throw new SizeLimit();
        }
        @Override public int read() throws IOException { checked(0); int value = in.read(); eof |= value < 0; checked(value < 0 ? 0 : 1); return value; }
        @Override public int read(byte[] buffer, int offset, int length) throws IOException {
            checked(0); int value = in.read(buffer, offset, (int)Math.min(length, Math.max(1, limit - count + 1)));
            eof |= value < 0; checked(value); return value;
        }
    }
    private static final Pattern DECIMAL = Pattern.compile("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)");
    private static final Set<String> ROUTE_FIELDS = Set.of("name", "cmt", "desc", "src", "number", "type");
    private static final Set<String> POINT_FIELDS = Set.of("ele", "time", "magvar", "geoidheight", "name", "cmt", "desc", "src",
            "sym", "type", "fix", "sat", "hdop", "vdop", "pdop", "ageofdgpsdata", "dgpsid");
    private static final class Draft {
        final int ordinal, depth; final Route.SourceType type;
        final Map<String, String> metadata = new LinkedHashMap<>();
        final List<RouteSegment> segments = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        int pointCount, segmentCount; String error;
        Draft(int ordinal, int depth, Route.SourceType type) { this.ordinal = ordinal; this.depth = depth; this.type = type; }
        void invalid(String message) { if (error == null) error = message; }
    }
    private static final class Handler extends DefaultHandler2 {
        final RouteLimits limits; final List<Draft> drafts = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        int depth, points, segments, waypoints, usedText, segmentDepth, pointDepth, linkDepth, links;
        String namespace, field, fieldKey; int fieldDepth;
        StringBuilder text; Map<String, String> fieldTarget, pointMetadata, linkTarget;
        Draft current; List<RoutePoint> segment;
        Double latitude, longitude, elevation;
        Handler(RouteLimits limits) { this.limits = limits; }
        private void limit(boolean excess, String message) throws Rejected { if (excess) throw new Rejected(ErrorCode.LIMIT, message); }
        private double number(String value) {
            if (value == null || value.length() > limits.fieldChars() || !DECIMAL.matcher(value.strip()).matches()) throw new IllegalArgumentException("Missing or non-decimal value");
            double number = Double.parseDouble(value.strip()); if (!Double.isFinite(number)) throw new IllegalArgumentException("Non-finite value"); return number;
        }
        @Override public void startDTD(String name, String publicId, String systemId) throws SAXException { throw new Rejected(ErrorCode.XML, "DTD declarations are forbidden"); }
        @Override public InputSource resolveEntity(String publicId, String systemId) throws SAXException { throw new Rejected(ErrorCode.XML, "External XML entities are forbidden"); }
        @Override public InputSource resolveEntity(String name, String publicId, String base, String systemId) throws SAXException { throw new Rejected(ErrorCode.XML, "External XML entities/DTD are forbidden"); }
        @Override public void fatalError(org.xml.sax.SAXParseException error) throws SAXException { throw error; }
        @Override public void error(org.xml.sax.SAXParseException error) throws SAXException { throw error; }
        @Override public void startElement(String uri, String local, String qualified, Attributes attributes) throws SAXException {
            depth++; limit(depth > limits.xmlDepth(), "XML depth limit exceeded");
            if (depth == 1) {
                if (!"gpx".equals(local) || !(NAMESPACE.equals(uri) || uri.isEmpty()) || !"1.1".equals(attributes.getValue("version")))
                    throw new Rejected(ErrorCode.UNSUPPORTED_GPX, "Expected GPX 1.1 root and namespace");
                namespace = uri; if (uri.isEmpty()) warnings.add("GPX1.1 without namespace accepted in compatibility mode"); return;
            }
            if (!uri.equals(namespace)) return;
            if (depth == 2 && (local.equals("rte") || local.equals("trk"))) {
                limit(drafts.size() >= limits.routes(), "Route candidate limit exceeded");
                current = new Draft(drafts.size() + 1, depth, local.equals("rte") ? Route.SourceType.RTE : Route.SourceType.TRK);
                drafts.add(current); links = 0;
                if (current.type == Route.SourceType.RTE) beginSegment(depth);
                return;
            }
            if (depth == 2 && local.equals("wpt")) { waypoints++; limit(++points > limits.points(), "Point limit exceeded (including standalone waypoints)"); return; }
            if (current == null) return;
            if (current.type == Route.SourceType.TRK && depth == current.depth + 1 && local.equals("trkseg")) { beginSegment(depth); return; }
            if (segment != null && depth == segmentDepth + 1 && local.equals(current.type == Route.SourceType.RTE ? "rtept" : "trkpt")) {
                limit(++points > limits.points(), "Route point limit exceeded"); current.pointCount++; pointDepth = depth;
                latitude = longitude = elevation = null; pointMetadata = new LinkedHashMap<>();
                try { latitude = number(attributes.getValue("lat")); longitude = number(attributes.getValue("lon"));
                    if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) throw new IllegalArgumentException("Coordinate out of range");
                } catch (IllegalArgumentException bad) { current.invalid("Invalid coordinate at point " + current.pointCount + ": " + bad.getMessage()); }
                return;
            }
            boolean directPoint = pointDepth > 0 && depth == pointDepth + 1;
            boolean directRoute = pointDepth == 0 && depth == current.depth + 1;
            if ((directPoint || directRoute) && local.equals("link")) {
                linkDepth = depth; linkTarget = directPoint ? pointMetadata : current.metadata;
                links++; String href = attributes.getValue("href");
                if (href != null) put(linkTarget, "link." + links + ".href", href);
            } else if (linkDepth > 0 && depth == linkDepth + 1 && (local.equals("text") || local.equals("type"))) {
                capture(local, "link." + links + "." + local, linkTarget);
            } else if (directPoint && POINT_FIELDS.contains(local)) capture(local, local, pointMetadata);
            else if (directRoute && ROUTE_FIELDS.contains(local)) capture(local, local, current.metadata);
        }
        private void beginSegment(int atDepth) throws Rejected {
            limit(++segments > limits.segments(), "Segment limit exceeded"); current.segmentCount++;
            segment = new ArrayList<>(); segmentDepth = atDepth;
        }
        private void capture(String local, String key, Map<String, String> target) {
            field = local; fieldKey = key; fieldDepth = depth; fieldTarget = target; text = new StringBuilder();
        }
        private void put(Map<String, String> target, String key, String value) throws Rejected {
            limit(value.length() > limits.fieldChars(), "Metadata field character limit exceeded");
            usedText += key.length() + value.length() + 1; limit(usedText > limits.metadataChars(), "Total metadata character limit exceeded");
            if (target.putIfAbsent(key, value) != null && current.warnings.size() < 32) current.warnings.add("Repeated " + key + "; first value retained");
        }
        @Override public void characters(char[] chars, int start, int length) throws SAXException {
            if (text != null && depth == fieldDepth) { limit(text.length() + length > limits.fieldChars(), "Metadata field character limit exceeded"); text.append(chars, start, length); }
        }
        @Override public void endElement(String uri, String local, String qualified) throws SAXException {
            if (uri.equals(namespace) && current != null) {
                if (text != null && depth == fieldDepth && local.equals(field)) {
                    String value = text.toString().strip(); put(fieldTarget, fieldKey, value);
                    if (pointDepth > 0 && field.equals("ele") && fieldKey.equals("ele")) {
                        try { elevation = number(value); } catch (IllegalArgumentException bad) { current.invalid("Invalid elevation at point " + current.pointCount); }
                    }
                    text = null; field = fieldKey = null; fieldTarget = null;
                }
                if (depth == linkDepth && local.equals("link")) { linkDepth = 0; linkTarget = null; }
                if (pointDepth > 0 && depth == pointDepth && local.equals(current.type == Route.SourceType.RTE ? "rtept" : "trkpt")) {
                    if (current.error == null) {
                        RoutePoint point = new RoutePoint(latitude, longitude, elevation, pointMetadata);
                        if (!segment.isEmpty()) {
                            RoutePoint previous = segment.get(segment.size() - 1);
                            if (previous.latitude() == point.latitude() && previous.longitude() == point.longitude()) {
                                if (current.warnings.size() < 32 && !current.warnings.contains("Consecutive duplicate coordinates retained")) current.warnings.add("Consecutive duplicate coordinates retained");
                            }
                        }
                        segment.add(point);
                    }
                    pointDepth = 0; pointMetadata = null;
                }
                if (segment != null && depth == segmentDepth && (local.equals("trkseg") || local.equals("rte"))) {
                    current.segments.add(new RouteSegment(segment)); segment = null; segmentDepth = 0;
                }
                if (depth == current.depth && local.equals(current.type == Route.SourceType.RTE ? "rte" : "trk")) {
                    if (current.pointCount == 0) current.invalid("Route contains no points"); current = null;
                }
            }
            depth--;
        }
        Preview preview(String fileName, String sha, long importedAtMillis) {
            List<Candidate> candidates = new ArrayList<>();
            for (Draft draft : drafts) {
                String original = draft.metadata.getOrDefault("name", ""); String name = original.isBlank() ? "Route " + draft.ordinal : original;
                int end = Math.min(name.length(), Route.MAX_DISPLAY_NAME_CHARS - 1);
                if (end > 0 && Character.isHighSurrogate(name.charAt(end - 1))) end--;
                String display = name.length() <= Route.MAX_DISPLAY_NAME_CHARS ? name : name.substring(0, end) + "…";
                Route route = draft.error != null ? null : new Route(UUID.randomUUID(), display, fileName, sha, draft.ordinal,
                        draft.type, importedAtMillis, draft.metadata, draft.segments);
                candidates.add(new Candidate(draft.ordinal, name, draft.type, draft.pointCount, draft.segmentCount, route, draft.error, draft.warnings));
            }
            return new Preview(fileName, sha, candidates, waypoints, warnings);
        }
    }
}
