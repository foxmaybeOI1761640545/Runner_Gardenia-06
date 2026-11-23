package de.dennisguse.opentracks.p0.routes;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import de.dennisguse.opentracks.data.CustomContentProvider;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.p0.P05HistoryFixtures;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

/** Native private-file semantics and real provider row snapshots; no Activity or GPS/recording commands. */
@RunWith(AndroidJUnit4.class)
public class P08RouteRepositoryPlatformTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final File root = new File(context.getFilesDir(), "p08-repo-platform/" + UUID.randomUUID());
    private RouteRepository repository() { return new RouteRepository(root); }
    static Route fixture(int n) {
        return new Route(new UUID(8, n), "Plan " + n, "fixture.gpx", "d".repeat(64), n, Route.SourceType.TRK, 123,
                Map.of("name", "Original " + n, "desc", "Synthetic androidTest route"), List.of(
                new RouteSegment(List.of(new RoutePoint(1, 103, 12.0, Map.of("name", "Start")), new RoutePoint(1.001, 103, null, Map.of()))),
                new RouteSegment(List.of(new RoutePoint(2, 104, null, Map.of("time", "2026-11-23T09:00:00"))))));
    }
    private File routeFile(int n) { return new File(root, "routes/" + fixture(n).id() + ".route"); }
    static void removeTree(File f) throws IOException { if (!f.exists()) return; if (f.isDirectory()) { File[] children = f.listFiles(); if (children == null) throw new IOException("Cannot list test directory"); for (File child : children) removeTree(child); } Files.delete(f.toPath()); }
    @After public void cleanup() throws Exception { removeTree(root); }
    @Test public void nativeAtomicBatchRenameDeleteAndReopen() throws Exception {
        var r = repository(); r.add(List.of(fixture(1), fixture(2)), false); byte[] geometry = Files.readAllBytes(routeFile(1).toPath());
        r.rename(fixture(1).id(), "本地名称"); var reopened = repository(); assertEquals("本地名称", reopened.get(fixture(1).id()).displayName());
        assertEquals("Original 1", reopened.get(fixture(1).id()).originalName()); assertEquals(2, reopened.get(fixture(1).id()).segments().size());
        assertArrayEquals(geometry, Files.readAllBytes(routeFile(1).toPath())); assertTrue(reopened.delete(fixture(2).id())); assertFalse(routeFile(2).exists());
        assertEquals(1, repository().verifyAll().entries().size()); assertTrue(repository().verifyAll().problems().isEmpty());
    }
    @Test public void nativeDuplicateSourceNeedsExplicitAdditionalCopy() throws Exception {
        var r = repository(); r.add(List.of(fixture(1)), false); Route extra = new Route(UUID.randomUUID(), "Extra", "fixture.gpx", "d".repeat(64), 1, Route.SourceType.TRK, 124, fixture(1).metadata(), fixture(1).segments());
        try { r.add(List.of(extra), false); fail("Silent duplicate"); } catch (RouteRepository.DuplicateImport e) { assertEquals(1, e.matches().size()); }
        assertEquals(1, r.list().entries().size()); r.add(List.of(extra), true); assertEquals(2, repository().list().entries().size());
    }
    @Test public void nativeMirrorRecoveryAndFutureSchemaFailClosed() throws Exception {
        var r = repository(); r.add(List.of(fixture(1)), false); r.rename(fixture(1).id(), "Alias");
        File index = new File(root, "index.bin"); Files.write(index.toPath(), new byte[]{1}); assertEquals("Alias", repository().get(fixture(1).id()).displayName());
        assertTrue(repository().list().repaired()); byte[] bytes = Files.readAllBytes(index.toPath()); bytes[11] = 2; Files.write(index.toPath(), bytes);
        try { repository().list(); fail("Future schema must not be repaired by older reader"); } catch (RouteCodec.FutureVersion expected) { assertArrayEquals(bytes, Files.readAllBytes(index.toPath())); }
    }
    @Test public void nativePrecommitIoRollsBackWholeSelection() throws Exception {
        repository().add(List.of(fixture(1)), false); var failing = new RouteRepository(root, at -> { if (at.equals("PROMOTED_0")) throw new IOException("Synthetic disk interruption"); });
        try { failing.add(List.of(fixture(2), fixture(3)), false); fail(); } catch (IOException expected) { assertTrue(expected.getMessage().contains("rolled back")); }
        assertEquals(List.of(fixture(1).id()), repository().list().entries().stream().map(RouteEntry::id).toList()); assertFalse(routeFile(2).exists()); assertFalse(routeFile(3).exists());
    }
    @Test public void nativeDamagedFileIsVisibleAndNeverReturnedAsValidGeometry() throws Exception {
        repository().add(List.of(fixture(1)), false); byte[] bytes = Files.readAllBytes(routeFile(1).toPath()); bytes[20] ^= 1; Files.write(routeFile(1).toPath(), bytes);
        assertEquals(1, repository().verifyAll().entries().size()); assertFalse(repository().verifyAll().problems().isEmpty());
        try { repository().get(fixture(1).id()); fail(); } catch (IOException expected) { assertTrue(expected.getMessage().contains("integrity")); }
        assertFalse(repository().repair().problems().isEmpty()); assertArrayEquals(bytes, Files.readAllBytes(routeFile(1).toPath()));
    }
    @Test public void nativeHistoryAllRowsPreferencesAndOfflineMapRemainByteIdentical() throws Exception {
        var id = P05HistoryFixtures.create(context, "Synthetic P08 retained history", true);
        File map = new File(context.getFilesDir(), "offline-maps/p08-test-" + UUID.randomUUID() + ".map");
        assertTrue(map.getParentFile().isDirectory() || map.getParentFile().mkdirs()); Files.write(map.toPath(), new byte[]{1, 8, 0, 4, 3});
        try {
            var before = history(); String prefs = preferences(); String mapHash = RouteCodec.sha256(Files.readAllBytes(map.toPath()));
            var r = repository(); r.add(List.of(fixture(1), fixture(2)), false); r.rename(fixture(1).id(), "Local alias"); assertNotNull(repository().get(fixture(1).id())); r.delete(fixture(2).id()); r.verifyAll(); r.repair(); r.delete(fixture(1).id());
            var after = history(); assertEquals(before.toString(), after.toString()); assertEquals(prefs, preferences()); assertEquals(mapHash, RouteCodec.sha256(Files.readAllBytes(map.toPath())));
            assertTrue(before.getJSONObject("TRACKS").getInt("rows") >= 1); assertTrue(before.getJSONObject("TRACKPOINTS").getInt("rows") >= 2); assertTrue(before.getJSONObject("HEART_RATE_SAMPLES").getInt("rows") >= 3);
            try (var client = context.getContentResolver().acquireContentProviderClient(CustomContentProvider.UrlType.TRACKS.getUri())) {
                assertNotNull(client); var provider = client.getLocalContentProvider(); assertNotNull(provider); var field = CustomContentProvider.class.getDeclaredField("db"); field.setAccessible(true); assertEquals(43, ((SQLiteDatabase)field.get(provider)).getVersion());
            }
            var json = new org.json.JSONObject().put("source", InstrumentationRegistry.getArguments().getString("buildCommit")).put("api", android.os.Build.VERSION.SDK_INT).put("schema", 43)
                    .put("before", before).put("after", after).put("preferencesSha256", prefs).put("offlineMapSentinelSha256", mapHash).put("passed", true)
                    .put("scope", "Actual provider all-row snapshots in TestApplication in-memory SQLite; actual private files/prefs. Real database upgrade and real map remain phaseF.");
            String output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir"); File base = output == null ? new File(context.getExternalMediaDirs()[0], "additional_test_output") : new File(output);
            File folder = new File(base, "p08-repository"); assertTrue(folder.isDirectory() || folder.mkdirs()); Files.writeString(new File(folder, "isolation.json").toPath(), json.toString(2));
        } finally { new ContentProviderUtils(context).deleteTrack(context, id); Files.deleteIfExists(map.toPath()); }
    }
    private org.json.JSONObject history() throws Exception {
        var result = new org.json.JSONObject();
        for (var type : List.of(CustomContentProvider.UrlType.TRACKS, CustomContentProvider.UrlType.TRACKPOINTS, CustomContentProvider.UrlType.HEART_RATE_SAMPLES)) {
            var bytes = new ByteArrayOutputStream(); int rows = 0;
            try (var data = new DataOutputStream(bytes); Cursor c = context.getContentResolver().query(type.getUri(), null, null, null, "_id ASC")) {
                assertNotNull(c); for (String column : c.getColumnNames()) data.writeUTF(column);
                while (c.moveToNext()) { rows++; for (int i = 0; i < c.getColumnCount(); i++) { int kind = c.getType(i); data.writeInt(kind); switch (kind) {
                    case Cursor.FIELD_TYPE_NULL -> {} case Cursor.FIELD_TYPE_INTEGER -> data.writeLong(c.getLong(i)); case Cursor.FIELD_TYPE_FLOAT -> data.writeDouble(c.getDouble(i));
                    case Cursor.FIELD_TYPE_BLOB -> { byte[] b = c.getBlob(i); data.writeInt(b.length); data.write(b); } default -> data.writeUTF(c.getString(i));
                } } }
            }
            result.put(type.name(), new org.json.JSONObject().put("rows", rows).put("sha256", RouteCodec.sha256(bytes.toByteArray())));
        }
        return result;
    }
    private String preferences() throws Exception {
        var sorted = new TreeMap<String,String>(); PreferenceManager.getDefaultSharedPreferences(context).getAll().forEach((key, value) -> sorted.put(key, value instanceof Set<?> set ? new TreeSet<>(set.stream().map(Object::toString).toList()).toString() : value.getClass().getName() + ":" + value));
        return RouteCodec.sha256(sorted.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
