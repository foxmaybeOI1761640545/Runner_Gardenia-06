package de.dennisguse.opentracks.p0.routes;

import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Test APK only. Non-seekable provider pipes, unknown MIME, missing metadata and interrupted reads. */
public final class P08DocumentFixtureProvider extends ContentProvider {
    static final String HEAD = "<gpx version='1.1' xmlns='http://www.topografix.com/GPX/1/1'>";
    static final String FIRST = "<rte><name>Seaside plan</name><desc>Synthetic GPX import fixture</desc><rtept lat='1.3' lon='103.8'><name>Start</name><ele>12</ele></rtept><rtept lat='1.301' lon='103.8'/></rte>";
    static final String SECOND = "<rte><name>Single lookout</name><rtept lat='1.31' lon='103.81'/></rte>";
    static final String TRACK = "<trk><name>Two separate paths</name><trkseg><trkpt lat='1.3' lon='103.8'/><trkpt lat='1.301' lon='103.8'/></trkseg><trkseg><trkpt lat='1.4' lon='103.9'/></trkseg></trk>";
    static String xml(String name) {
        if (name.contains("empty")) return "";
        if (name.contains("malformed")) return HEAD + FIRST + "<rte>";
        if (name.contains("wpt")) return HEAD + "<wpt lat='0' lon='0'/></gpx>";
        if (name.contains("mixed")) return HEAD + FIRST + "<rte><name>Invalid latitude</name><rtept lat='91' lon='0'/></rte>" + TRACK + "</gpx>";
        return HEAD + FIRST + SECOND + TRACK + "</gpx>";
    }
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "text/plain"; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        if (uri.getLastPathSegment().contains("metadata")) throw new UnsupportedOperationException("Synthetic provider has no display name");
        MatrixCursor cursor = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME}); cursor.addRow(new Object[]{uri.getLastPathSegment()}); return cursor;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String name = uri.getLastPathSegment(); if (!"r".equals(mode) || name.contains("missing")) throw new FileNotFoundException("Synthetic document is no longer available");
        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createReliablePipe();
            new Thread(() -> {
                try (var output = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) {
                    if (name.contains("slow")) { output.write((HEAD + "<!--").getBytes(StandardCharsets.UTF_8)); for (int n = 0; n < 50; n++) { SystemClock.sleep(100); output.write('x'); } output.write(("-->" + FIRST + "</gpx>").getBytes(StandardCharsets.UTF_8)); }
                    else if (name.contains("interrupted")) { output.write((HEAD + FIRST).getBytes(StandardCharsets.UTF_8)); pipe[1].closeWithError("Synthetic provider interruption"); }
                    else output.write(xml(name).getBytes(StandardCharsets.UTF_8));
                } catch (IOException expectedWhenReaderCancels) { /* Closing a selected pipe is the cancellation contract. */ }
            }, "p08-fixture-pipe").start();
            return pipe[0];
        } catch (IOException e) { throw new FileNotFoundException(e.toString()); }
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String where, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String where, String[] args) { throw new UnsupportedOperationException(); }
}
