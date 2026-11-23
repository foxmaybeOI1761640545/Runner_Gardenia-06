package de.dennisguse.opentracks.p0.routes;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import static org.junit.Assert.*;

public class RouteRepositoryTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private File root() { return new File(temporary.getRoot(), "route-library"); }
    private RouteRepository repo() { return new RouteRepository(root()); }
    private Route route(int ordinal) { return new Route(UUID.randomUUID(), "Plan " + ordinal, "source.gpx", "a".repeat(64), ordinal,
            Route.SourceType.TRK, 1234, Map.of("name", "Original " + ordinal, "desc", "description"),
            List.of(new RouteSegment(List.of(new RoutePoint(0, 0, 12.0, Map.of("name", "Start")), new RoutePoint(0, .001, null, Map.of()))),
                    new RouteSegment(List.of(new RoutePoint(10, 10, null, Map.of("time", "2026-11-23T09:00:00")))))); }
    private File file(Route route) { return new File(root(), "routes/" + route.id() + ".route"); }
    private File index() { return new File(root(), "index.bin"); }
    private File mirror() { return new File(root(), "index.mirror"); }
    private static void broken(File file) throws IOException { Files.write(file.toPath(), "corrupt".getBytes(StandardCharsets.UTF_8)); }
    private static void emptyPending(File root) throws IOException { File pending = new File(root, "pending"); if (pending.exists()) try (var paths = Files.list(pending.toPath())) { assertEquals(0, paths.count()); } }
    private static final class Killed extends Error {}
    private RouteRepository crashing(String checkpoint) { return new RouteRepository(root(), at -> { if (checkpoint.equals(at)) throw new Killed(); }); }
    private RouteRepository failing(String checkpoint) { return new RouteRepository(root(), at -> { if (checkpoint.equals(at)) throw new IOException("injected " + at); }); }
    private static boolean afterCommit(String checkpoint) { return List.of("INDEX_COMMITTED", "VERIFIED", "BEFORE_MIRROR", "BEFORE_CLEANUP", "JOURNAL_RETIRED").contains(checkpoint); }
    @Test public void emptyQueryDoesNotCreateStorageOrData() throws Exception {
        assertTrue(repo().list().entries().isEmpty()); assertNull(repo().get(UUID.randomUUID())); assertFalse(root().exists());
    }
    @Test public void oneRouteRoundTripsWithStableUuidAndMetadata() throws Exception {
        Route route = route(1); var result = repo().add(List.of(route), false); assertEquals(List.of(route.id()), result.added());
        assertEquals(route, repo().get(route.id())); assertEquals(1, result.snapshot().entries().size()); assertTrue(result.snapshot().problems().isEmpty());
        assertTrue(index().isFile()); assertTrue(mirror().isFile()); assertTrue(file(route).isFile()); emptyPending(root());
    }
    @Test public void batchImportRetainsOrderAndSegmentBreaks() throws Exception {
        Route first = route(1), second = route(2); repo().add(List.of(first, second), false);
        assertEquals(List.of(first.id(), second.id()), repo().list().entries().stream().map(RouteEntry::id).toList());
        assertEquals(2, repo().get(first.id()).segments().size()); assertEquals(3, repo().get(first.id()).pointCount()); assertTrue(repo().get(first.id()).distanceMeters().orElseThrow() < 112);
    }
    @Test public void newRepositoryInstanceReopensPersistentRoute() throws Exception {
        Route route = route(1); repo().add(List.of(route), false); assertEquals(route, new RouteRepository(root()).get(route.id())); assertTrue(repo().verifyAll().problems().isEmpty());
    }
    @Test public void renameChangesOnlyIndexAliasAndSurvivesRestart() throws Exception {
        Route route = route(1); repo().add(List.of(route), false); byte[] before = Files.readAllBytes(file(route).toPath());
        repo().rename(route.id(), "Local alias"); Route loaded = repo().get(route.id()); assertEquals("Local alias", loaded.displayName()); assertEquals(route.originalName(), loaded.originalName());
        assertEquals(route.segments(), loaded.segments()); assertArrayEquals(before, Files.readAllBytes(file(route).toPath()));
        assertEquals("Local alias", new RouteRepository(root()).list().entries().get(0).displayName());
    }
    @Test public void deleteCleansOnlySelectedRouteAndRetainsSiblingStores() throws Exception {
        File map = new File(temporary.getRoot(), "offline-maps.map"), history = new File(temporary.getRoot(), "history.db"); Files.writeString(map.toPath(), "retained map"); Files.writeString(history.toPath(), "retained history");
        Route first = route(1), second = route(2); repo().add(List.of(first, second), false); byte[] other = Files.readAllBytes(file(second).toPath());
        assertTrue(repo().delete(first.id())); assertFalse(file(first).exists()); assertNull(repo().get(first.id())); assertEquals(second, repo().get(second.id()));
        assertArrayEquals(other, Files.readAllBytes(file(second).toPath())); assertEquals("retained map", Files.readString(map.toPath())); assertEquals("retained history", Files.readString(history.toPath())); emptyPending(root());
    }
    @Test public void duplicateSourceIsExplicitAndExtraCopyNeverOverwrites() throws Exception {
        Route first = route(1), copy = route(1); repo().add(List.of(first), false); byte[] bytes = Files.readAllBytes(file(first).toPath());
        try { repo().add(List.of(copy), false); fail(); } catch (RouteRepository.DuplicateImport expected) { assertEquals(first.id(), expected.matches().get(0).id()); }
        assertEquals(1, repo().list().entries().size()); assertEquals(1, repo().duplicates(List.of(copy)).size());
        repo().add(List.of(copy), true); assertEquals(2, repo().list().entries().size()); assertArrayEquals(bytes, Files.readAllBytes(file(first).toPath()));
        assertThrows(IOException.class, () -> repo().add(List.of(first), true)); assertEquals(2, repo().list().entries().size());
    }
    @Test public void repeatedSelectionIdentityIsRejectedBeforeAnyFormalFile() throws Exception {
        Route first = route(1), second = route(1); assertThrows(IOException.class, () -> repo().add(List.of(first, second), false));
        assertTrue(repo().list().entries().isEmpty()); assertFalse(file(first).exists()); assertFalse(file(second).exists());
    }
    @Test public void invalidBatchEncodingLeavesEveryPreviousRouteIntact() throws Exception {
        Route previous = route(1), good = route(2); repo().add(List.of(previous), false); byte[] old = Files.readAllBytes(index().toPath());
        Route bad = new Route(UUID.randomUUID(), "Bad", "bad.gpx", "b".repeat(64), 3, Route.SourceType.RTE, 10,
                Map.of("desc", "x".repeat(2049)), previous.segments());
        assertThrows(IOException.class, () -> repo().add(List.of(good, bad), false)); assertArrayEquals(old, Files.readAllBytes(index().toPath())); assertFalse(file(good).exists()); assertEquals(previous, repo().get(previous.id())); emptyPending(root());
    }
    @Test public void corruptFileIsVisibleAndCannotReturnFakeGeometry() throws Exception {
        Route damaged = route(1), good = route(2); repo().add(List.of(damaged, good), false); byte[] bytes = Files.readAllBytes(file(damaged).toPath()); bytes[40] ^= 1; Files.write(file(damaged).toPath(), bytes);
        assertThrows(IOException.class, () -> repo().get(damaged.id())); assertEquals(2, repo().list().entries().size());
        assertTrue(repo().verifyAll().problems().stream().anyMatch(p -> damaged.id().equals(p.id()))); assertEquals(good, repo().get(good.id()));
    }
    @Test public void missingFileRetainsVisibleMetadataUntilExplicitDelete() throws Exception {
        Route route = route(1); repo().add(List.of(route), false); Files.delete(file(route).toPath());
        assertEquals(1, repo().list().entries().size()); assertFalse(repo().list().problems().isEmpty()); assertThrows(IOException.class, () -> repo().get(route.id()));
        assertTrue(repo().delete(route.id())); assertTrue(repo().list().entries().isEmpty());
    }
    @Test public void primaryIndexRecoveryPreservesRenamedAliasAndPersistentNotice() throws Exception {
        Route route = route(1); repo().add(List.of(route), false); repo().rename(route.id(), "Renamed"); broken(index());
        var restored = repo().list(); assertTrue(restored.repaired()); assertEquals("Renamed", restored.entries().get(0).displayName()); assertEquals("Renamed", repo().get(route.id()).displayName());
        assertTrue(new RouteRepository(root()).list().repaired()); repo().acknowledgeRepairNotice(); assertFalse(repo().list().repaired());
    }
    @Test public void bothCorruptIndexesRebuildStableIdsAndReportLostAlias() throws Exception {
        Route route = route(1); repo().add(List.of(route), false); repo().rename(route.id(), "Temporary alias"); byte[] bytes = Files.readAllBytes(file(route).toPath()); broken(index()); broken(mirror());
        var restored = repo().list(); assertTrue(restored.repaired()); assertEquals(route.id(), restored.entries().get(0).id()); assertEquals(route.displayName(), restored.entries().get(0).displayName());
        assertTrue(restored.problems().stream().anyMatch(p -> p.reason().contains("aliases"))); assertArrayEquals(bytes, Files.readAllBytes(file(route).toPath()));
    }
    @Test public void unknownFutureIndexFailsClosedWithoutReplacingAnything() throws Exception {
        Route route = route(1); repo().add(List.of(route), false); byte[] future = Files.readAllBytes(index().toPath()); java.nio.ByteBuffer.wrap(future, 8, 4).putInt(2); Files.write(index().toPath(), future);
        byte[] mirror = Files.readAllBytes(mirror().toPath()); assertThrows(RouteCodec.FutureVersion.class, () -> repo().list()); assertArrayEquals(future, Files.readAllBytes(index().toPath())); assertArrayEquals(mirror, Files.readAllBytes(mirror().toPath()));
    }
    @Test public void unknownFutureRouteBlocksRebuildRatherThanDroppingIt() throws Exception {
        Route route = route(1); repo().add(List.of(route), false); byte[] future = Files.readAllBytes(file(route).toPath()); java.nio.ByteBuffer.wrap(future, 8, 4).putInt(2); Files.write(file(route).toPath(), future); broken(index()); broken(mirror());
        byte[] before = Files.readAllBytes(index().toPath()); assertThrows(RouteCodec.FutureVersion.class, () -> repo().list()); assertArrayEquals(before, Files.readAllBytes(index().toPath())); assertArrayEquals(future, Files.readAllBytes(file(route).toPath()));
    }
    @Test public void invalidOrMalformedAliasCannotChangeOriginalIndex() throws Exception {
        Route route = route(1); repo().add(List.of(route), false); byte[] before = Files.readAllBytes(index().toPath());
        for (String name : List.of("", " ", "x".repeat(257))) assertThrows(IllegalArgumentException.class, () -> repo().rename(route.id(), name));
        assertThrows(IOException.class, () -> repo().rename(route.id(), "\ud800")); assertArrayEquals(before, Files.readAllBytes(index().toPath()));
        repo().rename(route.id(), "x".repeat(256)); assertEquals(256, repo().get(route.id()).displayName().length());
    }
    @Test public void missingUuidNeverDeletesOtherRoutes() throws Exception {
        Route route = route(1); repo().add(List.of(route), false); assertFalse(repo().delete(UUID.randomUUID())); assertNull(repo().get(UUID.randomUUID())); assertEquals(route, repo().get(route.id()));
    }
    @Test public void symbolicRouteFileCannotReadOrDeleteOutsideStorage() throws Exception {
        Route route = route(1); repo().add(List.of(route), false); File outside = new File(temporary.getRoot(), "outside.map"); Files.writeString(outside.toPath(), "untouched"); Files.delete(file(route).toPath());
        Files.createSymbolicLink(file(route).toPath(), outside.toPath());
        try { assertThrows(IOException.class, () -> repo().get(route.id())); assertThrows(IOException.class, () -> repo().delete(route.id())); assertEquals("untouched", Files.readString(outside.toPath())); }
        finally { Files.delete(file(route).toPath()); }
    }
    @Test public void symbolicRootIsRejectedWithoutTouchingTarget() throws Exception {
        File target = temporary.newFolder("outside-root"); Files.createSymbolicLink(root().toPath(), target.toPath());
        try { assertThrows(IOException.class, () -> repo().add(List.of(route(1)), false)); assertEquals(0, target.list().length); }
        finally { Files.delete(root().toPath()); }
    }
    @Test public void concurrentRepositoryInstancesSerializeWholeTransactions() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> { for (int i = 1; i <= 10; i++) new RouteRepository(root()).add(List.of(route(i)), false); return true; });
            var second = executor.submit(() -> { for (int i = 11; i <= 20; i++) new RouteRepository(root()).add(List.of(route(i)), false); return true; });
            assertTrue(first.get()); assertTrue(second.get()); assertEquals(20, repo().verifyAll().entries().size()); assertTrue(repo().verifyAll().problems().isEmpty()); emptyPending(root());
        } finally { executor.shutdownNow(); }
    }
    @Test public void killedMultiImportIsAllBeforeOrAllAfterCommitAtEveryCheckpoint() throws Exception {
        for (String checkpoint : List.of("STAGED", "PREPARED", "PROMOTED_0", "PROMOTED_1", "BEFORE_INDEX", "INDEX_COMMITTED", "VERIFIED", "BEFORE_MIRROR", "BEFORE_CLEANUP", "JOURNAL_RETIRED")) {
            File folder = temporary.newFolder(); RouteRepository normal = new RouteRepository(folder); Route previous = route(1), first = route(2), second = route(3); normal.add(List.of(previous), false);
            byte[] old = Files.readAllBytes(new File(folder, "routes/" + previous.id() + ".route").toPath());
            var crash = new RouteRepository(folder, at -> { if (at.equals(checkpoint)) throw new Killed(); });
            assertThrows(Killed.class, () -> crash.add(List.of(first, second), false));
            RouteRepository recovered = new RouteRepository(folder); assertEquals(checkpoint, afterCommit(checkpoint) ? 3 : 1, recovered.list().entries().size()); assertEquals(previous, recovered.get(previous.id()));
            assertArrayEquals(old, Files.readAllBytes(new File(folder, "routes/" + previous.id() + ".route").toPath()));
            assertEquals(afterCommit(checkpoint), recovered.get(first.id()) != null); assertEquals(afterCommit(checkpoint), recovered.get(second.id()) != null); emptyPending(folder);
            assertEquals(afterCommit(checkpoint) ? 3 : 1, new RouteRepository(folder).list().entries().size());
        }
    }
    @Test public void killedDeleteRestoresBeforeCommitAndNeverResurrectsAfterCommit() throws Exception {
        for (String checkpoint : List.of("PREPARED", "RETIRED_0", "BEFORE_INDEX", "INDEX_COMMITTED", "BEFORE_MIRROR", "BEFORE_CLEANUP", "JOURNAL_RETIRED")) {
            File folder = temporary.newFolder(); RouteRepository normal = new RouteRepository(folder); Route removed = route(1), kept = route(2); normal.add(List.of(removed, kept), false);
            var crash = new RouteRepository(folder, at -> { if (at.equals(checkpoint)) throw new Killed(); }); assertThrows(Killed.class, () -> crash.delete(removed.id()));
            RouteRepository recovered = new RouteRepository(folder); assertEquals(afterCommit(checkpoint) ? 1 : 2, recovered.list().entries().size()); assertEquals(kept, recovered.get(kept.id()));
            assertEquals(!afterCommit(checkpoint), recovered.get(removed.id()) != null); recovered.repair(); assertEquals(!afterCommit(checkpoint), recovered.get(removed.id()) != null); emptyPending(folder);
        }
    }
    @Test public void killedRenameKeepsOriginalFileAndCorrectAlias() throws Exception {
        for (String checkpoint : List.of("PREPARED", "BEFORE_INDEX", "INDEX_COMMITTED", "BEFORE_CLEANUP")) {
            File folder = temporary.newFolder(); RouteRepository normal = new RouteRepository(folder); Route route = route(1); normal.add(List.of(route), false); File stored = new File(folder, "routes/" + route.id() + ".route"); byte[] before = Files.readAllBytes(stored.toPath());
            var crash = new RouteRepository(folder, at -> { if (at.equals(checkpoint)) throw new Killed(); }); assertThrows(Killed.class, () -> crash.rename(route.id(), "Renamed"));
            assertEquals(afterCommit(checkpoint) ? "Renamed" : route.displayName(), new RouteRepository(folder).get(route.id()).displayName()); assertArrayEquals(before, Files.readAllBytes(stored.toPath())); emptyPending(folder);
        }
    }
    @Test public void normalIoFailureRollsBackBeforeIndexAndReportsCommittedAfterIndex() throws Exception {
        Route previous = route(1), added = route(2); repo().add(List.of(previous), false); byte[] before = Files.readAllBytes(index().toPath());
        assertThrows(IOException.class, () -> failing("BEFORE_INDEX").add(List.of(added), false)); assertArrayEquals(before, Files.readAllBytes(index().toPath())); assertNull(repo().get(added.id()));
        var committed = failing("INDEX_COMMITTED").add(List.of(added), false); assertEquals(List.of(added.id()), committed.added()); assertEquals(added, repo().get(added.id())); assertEquals(previous, repo().get(previous.id())); emptyPending(root());
    }
    @Test public void transactionRecoveryPrecedesRebuildOfBothCorruptIndexes() throws Exception {
        Route previous = route(1), added = route(2), another = route(3); repo().add(List.of(previous), false);
        assertThrows(Killed.class, () -> crashing("PROMOTED_0").add(List.of(added, another), false)); broken(index()); broken(mirror());
        assertEquals(1, repo().list().entries().size()); assertEquals(previous, repo().get(previous.id())); assertFalse(file(added).exists()); assertFalse(file(another).exists()); emptyPending(root());
    }
    @Test public void corruptPreparedJournalFailsClosedWithoutIndexRebuild() throws Exception {
        Route previous = route(1), added = route(2); repo().add(List.of(previous), false); byte[] index = Files.readAllBytes(index().toPath());
        assertThrows(Killed.class, () -> crashing("PREPARED").add(List.of(added), false)); File tx = new File(root(), "pending").listFiles()[0], marker = new File(tx, "prepared.bin"); byte[] valid = Files.readAllBytes(marker.toPath()); broken(marker);
        assertThrows(IOException.class, () -> repo().list()); assertArrayEquals(index, Files.readAllBytes(index().toPath())); assertFalse(file(added).exists());
        Files.write(marker.toPath(), valid); assertEquals(1, repo().list().entries().size()); assertEquals(previous, repo().get(previous.id())); emptyPending(root());
    }
    @Test public void corruptRouteCanStillBeDeletedAndRollbackRestoresIdenticalCorruptBytes() throws Exception {
        Route route = route(1); repo().add(List.of(route), false); broken(file(route)); byte[] damaged = Files.readAllBytes(file(route).toPath());
        assertThrows(Killed.class, () -> crashing("RETIRED_0").delete(route.id())); assertEquals(1, repo().list().entries().size()); assertArrayEquals(damaged, Files.readAllBytes(file(route).toPath()));
        assertTrue(repo().delete(route.id())); assertTrue(repo().list().entries().isEmpty()); assertFalse(file(route).exists());
    }
    @Test public void explicitRepairAdoptsOnlyValidOrphansAndKeepsLocalAliases() throws Exception {
        Route known = route(1), orphan = route(2); repo().add(List.of(known), false); repo().rename(known.id(), "Alias"); Files.write(file(orphan).toPath(), RouteCodec.route(orphan));
        UUID badId = UUID.randomUUID(); File bad = new File(root(), "routes/" + badId + ".route"); broken(bad);
        var repaired = repo().repair(); assertEquals(2, repaired.entries().size()); assertEquals("Alias", repo().get(known.id()).displayName()); assertEquals(orphan, repo().get(orphan.id()));
        assertTrue(repaired.problems().stream().anyMatch(p -> badId.equals(p.id()))); assertTrue(bad.isFile());
    }
    @Test public void repairCannotBlessUnexpectedValidReplacementGeometry() throws Exception {
        Route original = route(1); repo().add(List.of(original), false);
        Route changed = new Route(original.id(), original.displayName(), original.sourceFileName(), original.sourceSha256(), original.sourceOrdinal(),
                original.sourceType(), original.importedAtMillis(), original.metadata(), List.of(new RouteSegment(List.of(new RoutePoint(40, 40, null, Map.of())))));
        byte[] previousIndex = Files.readAllBytes(index().toPath()); Files.write(file(original).toPath(), RouteCodec.route(changed));
        assertThrows(IOException.class, () -> repo().get(original.id())); var repaired = repo().repair();
        assertTrue(repaired.problems().stream().anyMatch(problem -> original.id().equals(problem.id()) && problem.reason().contains("integrity")));
        assertThrows(IOException.class, () -> repo().get(original.id())); assertArrayEquals(previousIndex, Files.readAllBytes(index().toPath()));
    }
    @Test public void interruptedOrphanAdoptionNeverDeletesPreexistingOrphan() throws Exception {
        for (String checkpoint : List.of("PREPARED", "INDEX_COMMITTED")) {
            File folder = temporary.newFolder(); RouteRepository normal = new RouteRepository(folder); Route known = route(1), orphan = route(2); normal.add(List.of(known), false);
            File orphanFile = new File(folder, "routes/" + orphan.id() + ".route"); byte[] original = RouteCodec.route(orphan); Files.write(orphanFile.toPath(), original);
            var crash = new RouteRepository(folder, at -> { if (at.equals(checkpoint)) throw new Killed(); }); assertThrows(Killed.class, crash::repair);
            assertEquals(afterCommit(checkpoint) ? 2 : 1, new RouteRepository(folder).list().entries().size()); assertArrayEquals(original, Files.readAllBytes(orphanFile.toPath()));
            normal.repair(); assertEquals(orphan, normal.get(orphan.id())); assertEquals(known, normal.get(known.id())); emptyPending(folder);
        }
    }
    @Test public void unpreparedAndRecognizedAtomicTempsAreCleanedWithoutTouchingUnknownFiles() throws Exception {
        Route known = route(1); repo().add(List.of(known), false); File pending = new File(root(), "pending/" + UUID.randomUUID()); assertTrue(pending.mkdir()); broken(new File(pending, "truncated-stage"));
        File temp = new File(root(), "index.bin.tmp-" + UUID.randomUUID()), unknown = new File(root(), "user-note.txt"); broken(temp); broken(unknown);
        assertEquals(1, repo().list().entries().size()); assertFalse(temp.exists()); assertTrue(unknown.exists()); assertFalse(pending.exists()); assertEquals(known, repo().get(known.id()));
    }
    @Test public void partialRetiredCleanupNeedsNoJournalAndNeverBlocksCommittedRoutes() throws Exception {
        Route first = route(1), second = route(2); repo().add(List.of(first), false);
        assertThrows(Killed.class, () -> crashing("JOURNAL_RETIRED").add(List.of(second), false)); emptyPending(root());
        File retired = new File(root(), "garbage").listFiles()[0]; Files.delete(new File(retired, "before.index").toPath()); Files.delete(new File(retired, "prepared.bin").toPath());
        assertEquals(2, repo().list().entries().size()); assertEquals(first, repo().get(first.id())); assertEquals(second, repo().get(second.id())); assertEquals(0, new File(root(), "garbage").list().length);
    }
    @Test public void cleanupIoAfterAtomicJournalRetirementReportsCommittedRoutesAndRecovers() throws Exception {
        Route route = route(1); var committed = failing("JOURNAL_RETIRED").add(List.of(route), false);
        assertTrue(committed.snapshot().cleanupPending()); assertEquals(1, new File(root(), "garbage").list().length);
        assertEquals(route, repo().get(route.id())); assertEquals(0, new File(root(), "garbage").list().length);
    }
    @Test public void actualLibraryCapacity1024AcceptsBoundaryAndRejectsNextWithoutDamage() throws Exception {
        List<Route> routes = new ArrayList<>(); for (int i = 1; i <= 1024; i++) routes.add(route(i));
        repo().add(routes, false); assertEquals(1024, repo().list().entries().size()); byte[] before = Files.readAllBytes(index().toPath());
        assertThrows(IOException.class, () -> repo().add(List.of(route(1025)), false)); assertArrayEquals(before, Files.readAllBytes(index().toPath())); assertEquals(routes.get(0), repo().get(routes.get(0).id())); assertEquals(routes.get(1023), repo().get(routes.get(1023).id()));
    }
}
