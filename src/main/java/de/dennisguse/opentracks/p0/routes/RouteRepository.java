package de.dennisguse.opentracks.p0.routes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Worker-thread-only private file repository. No Android, history database or recording commands. */
public final class RouteRepository {
    private static final Object LOCK = new Object();
    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    public record Problem(UUID id, String reason) {}
    public record Snapshot(List<RouteEntry> entries, List<Problem> problems, boolean repaired, boolean cleanupPending) {
        public Snapshot { entries = List.copyOf(entries); problems = List.copyOf(problems); }
    }
    public record Mutation(List<UUID> added, Snapshot snapshot) { public Mutation { added = List.copyOf(added); } }
    public static final class DuplicateImport extends IOException {
        private final List<RouteEntry> matches;
        DuplicateImport(List<RouteEntry> matches) { super("This GPX source/route was already imported; explicitly choose an extra copy"); this.matches = List.copyOf(matches); }
        public List<RouteEntry> matches() { return matches; }
    }
    /** A deliberate Error at a checkpoint models process death; normal IO failures exercise rollback. */
    @FunctionalInterface public interface FaultInjector { void at(String checkpoint) throws IOException; }
    private static final FaultInjector NONE = checkpoint -> {};
    private final File requested;
    private final FaultInjector faults;
    public RouteRepository(File directory) { this(directory, NONE); }
    public RouteRepository(File directory, FaultInjector faults) { this.requested = Objects.requireNonNull(directory).getAbsoluteFile(); this.faults = Objects.requireNonNull(faults); }
    private static final class Notes {
        final List<Problem> problems = new ArrayList<>(); boolean repaired, pending;
        void add(UUID id, String reason) { problems.add(new Problem(id, reason)); }
    }
    private record State(List<RouteEntry> entries, byte[] bytes, boolean existed) {}
    private record CandidateIndex(List<RouteEntry> entries, byte[] bytes, String error) {}

    public Snapshot list() throws IOException {
        synchronized (LOCK) { Notes notes = new Notes(); ready(notes, false); return snapshot(load(notes), notes, false); }
    }
    public Snapshot verifyAll() throws IOException {
        synchronized (LOCK) { Notes notes = new Notes(); ready(notes, false); return snapshot(load(notes), notes, true); }
    }
    /** Returns null for a deleted UUID. A damaged route throws, never returns fabricated geometry. */
    public Route get(UUID id) throws IOException {
        Objects.requireNonNull(id);
        synchronized (LOCK) {
            Notes notes = new Notes(); ready(notes, false); State state = load(notes);
            RouteEntry entry = state.entries().stream().filter(row -> row.id().equals(id)).findFirst().orElse(null);
            return entry == null ? null : verified(entry);
        }
    }
    public List<RouteEntry> duplicates(List<Route> candidates) throws IOException {
        synchronized (LOCK) {
            Notes notes = new Notes(); ready(notes, false); Set<String> keys = new HashSet<>(); for (Route route : candidates) keys.add(route.duplicateKey());
            return load(notes).entries().stream().filter(entry -> keys.contains(entry.duplicateKey())).toList();
        }
    }
    public Mutation add(List<Route> routes, boolean allowExtraCopies) throws IOException {
        synchronized (LOCK) {
            if (routes == null || routes.isEmpty()) throw new IllegalArgumentException("Select at least one valid route");
            Notes notes = new Notes(); ready(notes, true); State before = load(notes);
            if ((long)before.entries().size() + routes.size() > RouteStorageLimits.MAX_ROUTES) throw new IOException("Route library exceeds 1024-route capacity");
            Set<UUID> ids = new HashSet<>(); Set<String> keys = new HashSet<>();
            for (RouteEntry entry : before.entries()) ids.add(entry.id());
            List<RouteEntry> duplicate = new ArrayList<>(); Map<UUID,byte[]> added = new LinkedHashMap<>(); List<RouteEntry> after = new ArrayList<>(before.entries());
            for (Route route : routes) {
                if (!ids.add(route.id())) throw new IOException("Route UUID already exists; existing routes cannot be overwritten");
                if (!allowExtraCopies && !keys.add(route.duplicateKey())) throw new IOException("The selection contains the same source route more than once");
                duplicate.addAll(before.entries().stream().filter(row -> row.duplicateKey().equals(route.duplicateKey())).toList());
                byte[] bytes = RouteCodec.route(route); Route checked = RouteCodec.route(bytes);
                if (!checked.equals(route)) throw new IOException("Staged route round-trip differs");
                added.put(route.id(), bytes); after.add(RouteEntry.of(route, bytes));
            }
            if (!allowExtraCopies && !duplicate.isEmpty()) throw new DuplicateImport(duplicate);
            commit(before, after, added, List.of(), notes);
            return new Mutation(new ArrayList<>(added.keySet()), snapshot(new State(List.copyOf(after), RouteCodec.index(after), true), notes, false));
        }
    }
    public Snapshot rename(UUID id, String name) throws IOException {
        String alias = validName(name); Objects.requireNonNull(id);
        synchronized (LOCK) {
            Notes notes = new Notes(); ready(notes, true); State before = load(notes); List<RouteEntry> after = new ArrayList<>(before.entries());
            int at = indexOf(after, id); if (at < 0) throw new IOException("Route no longer exists");
            if (after.get(at).displayName().equals(alias)) return snapshot(before, notes, false);
            verified(after.get(at)); after.set(at, after.get(at).renamed(alias));
            commit(before, after, Map.of(), List.of(), notes); return snapshot(new State(after, RouteCodec.index(after), true), notes, false);
        }
    }
    public boolean delete(UUID id) throws IOException {
        Objects.requireNonNull(id);
        synchronized (LOCK) {
            Notes notes = new Notes(); ready(notes, true); State before = load(notes); List<RouteEntry> after = new ArrayList<>(before.entries());
            int at = indexOf(after, id); if (at < 0) return false;
            RouteEntry entry = after.remove(at); List<RouteCodec.FileRef> retired = exists(routePath(id)) ? List.of(new RouteCodec.FileRef(id, entry.fileSha256())) : List.of();
            commit(before, after, Map.of(), retired, notes); return true;
        }
    }
    /** Adopt valid orphan files only after resolving transactions; retain missing/corrupt indexed rows visibly. */
    public Snapshot repair() throws IOException {
        synchronized (LOCK) {
            Notes notes = new Notes(); ready(notes, true); State before = load(notes); List<RouteEntry> rows = scan(before.entries(), notes);
            byte[] next = RouteCodec.index(rows);
            if (!RouteCodec.sha256(before.bytes()).equals(RouteCodec.sha256(next))) {
                List<RouteCodec.FileRef> adopted = rows.stream().filter(entry -> indexOf(before.entries(), entry.id()) < 0)
                        .map(entry -> new RouteCodec.FileRef(entry.id(), entry.fileSha256())).toList();
                commit(before, rows, Map.of(), List.of(), adopted, notes);
            }
            else if (before.existed()) writeAtomic(indexMirror(), next);
            notes.repaired = true; return snapshot(new State(rows, next, true), notes, true);
        }
    }
    /** Dismiss only a diagnostic notice; unresolved transaction warnings remain visible on every read. */
    public void acknowledgeRepairNotice() throws IOException {
        synchronized (LOCK) { Notes notes = new Notes(); ready(notes, false); remove(child("repair.notice")); }
    }
    private void remember(Notes notes, String message) throws IOException {
        writeAtomic(child("repair.notice"), ("1\n" + message).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        notes.repaired = true; notes.add(null, message);
    }
    private void readNotice(Notes notes) throws IOException {
        File file = child("repair.notice"); if (!exists(file)) return;
        try {
            String text = java.nio.charset.StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(read(file, 1024))).toString();
            if (!text.startsWith("1\n")) throw new IOException("Unsupported recovery notice");
            notes.repaired = true; notes.add(null, text.substring(2));
        } catch (InterruptedIOException cancelled) { throw cancelled; }
        catch (IOException corrupt) { notes.add(null, "Route recovery notice is unavailable: " + corrupt.getMessage()); }
    }
    private static int indexOf(List<RouteEntry> entries, UUID id) { for (int i = 0; i < entries.size(); i++) if (entries.get(i).id().equals(id)) return i; return -1; }
    private static String validName(String name) {
        if (name == null || name.strip().isEmpty() || name.strip().length() > Route.MAX_DISPLAY_NAME_CHARS) throw new IllegalArgumentException("Local name must contain 1 to 256 characters");
        return name.strip();
    }
    private File base() throws IOException {
        if (Files.isSymbolicLink(requested.toPath())) throw new IOException("Route-library root cannot be a symbolic link");
        File root = requested.getCanonicalFile(); if (root.exists() && !root.isDirectory()) throw new IOException("Route-library root is not a directory"); return root;
    }
    private File safe(File file) throws IOException {
        File root = base(); var path = file.getAbsoluteFile().toPath().normalize(); var rootPath = root.toPath();
        if (!path.startsWith(rootPath)) throw new IOException("Route path escaped private storage");
        for (var current = path; current != null && current.startsWith(rootPath); current = current.getParent())
            if (Files.isSymbolicLink(current)) throw new IOException("Symbolic links are forbidden in route storage");
        if (!file.getCanonicalFile().toPath().startsWith(rootPath)) throw new IOException("Route path escaped canonical storage"); return file;
    }
    private File child(String name) throws IOException { return safe(new File(base(), name)); }
    private File indexFile() throws IOException { return child("index.bin"); }
    private File indexMirror() throws IOException { return child("index.mirror"); }
    private File routesDir() throws IOException { return child("routes"); }
    private File pendingDir() throws IOException { return child("pending"); }
    private File garbageDir() throws IOException { return child("garbage"); }
    private File routePath(UUID id) throws IOException { return safe(new File(routesDir(), id + ".route")); }
    private static boolean exists(File file) { return Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS); }
    private void mkdir(File directory) throws IOException { safe(directory); Files.createDirectories(directory.toPath()); safe(directory); }
    private void createStorage() throws IOException { mkdir(base()); mkdir(routesDir()); mkdir(pendingDir()); mkdir(garbageDir()); }
    private static void interrupted() throws InterruptedIOException { if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Route storage interrupted"); }
    private byte[] read(File file, int maximum) throws IOException {
        safe(file); if (!Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS)) throw new IOException("Route-store file missing or not regular: " + file.getName());
        if (file.length() > maximum) throw new IOException("Route-store file exceeds byte budget: " + file.getName());
        try (var input = new FileInputStream(file); var output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) != -1) { interrupted(); if ((long)output.size() + count > maximum) throw new IOException("Route-store read exceeds byte budget"); output.write(buffer, 0, count); }
            return output.toByteArray();
        }
    }
    private void futureHeader(File file, String magic) throws IOException {
        if (!exists(file)) return; safe(file);
        if (!Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS)) throw new IOException("Route-store header is not a regular file");
        byte[] head = new byte[12]; int size = 0;
        try (var input = new FileInputStream(file)) { while (size < head.length) { int n = input.read(head, size, head.length - size); if (n < 0) break; size += n; } }
        RouteCodec.rejectFutureHeader(java.util.Arrays.copyOf(head, size), magic);
    }
    private void write(File file, byte[] bytes) throws IOException {
        safe(file); if (exists(file)) throw new IOException("Refusing to overwrite staged data");
        try (var output = new FileOutputStream(file)) {
            for (int offset = 0; offset < bytes.length; offset += 8192) { interrupted(); output.write(bytes, offset, Math.min(8192, bytes.length - offset)); }
            output.getFD().sync();
        }
        if (file.length() != bytes.length) throw new IOException("Incomplete route-store write");
    }
    private void writeAtomic(File target, byte[] bytes) throws IOException {
        safe(target); mkdir(target.getParentFile()); File temp = safe(new File(target.getParentFile(), target.getName() + ".tmp-" + UUID.randomUUID()));
        try { write(temp, bytes); Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        finally { if (exists(temp)) Files.delete(temp.toPath()); }
    }
    private void move(File source, File target) throws IOException {
        safe(source); safe(target); if (exists(target)) throw new IOException("Route target already exists");
        if (!Files.isRegularFile(source.toPath(), LinkOption.NOFOLLOW_LINKS)) throw new IOException("Route source is not a regular file");
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
    }
    private void remove(File file) throws IOException { safe(file); Files.deleteIfExists(file.toPath()); }
    private void removeTree(File directory) throws IOException {
        safe(directory); if (!exists(directory)) return;
        if (!Files.isDirectory(directory.toPath(), LinkOption.NOFOLLOW_LINKS)) { remove(directory); return; }
        try (var children = Files.newDirectoryStream(directory.toPath())) { for (var child : children) removeTree(child.toFile()); }
        remove(directory);
    }
    private CandidateIndex candidate(File file) throws IOException {
        if (!exists(file)) return null; futureHeader(file, RouteCodec.INDEX_MAGIC);
        if (file.length() > RouteStorageLimits.MAX_INDEX_BYTES) return new CandidateIndex(null, null, "Index exceeds byte limit");
        byte[] bytes = read(file, RouteStorageLimits.MAX_INDEX_BYTES);
        try { return new CandidateIndex(RouteCodec.index(bytes), bytes, null); }
        catch (RouteCodec.FutureVersion future) { throw future; }
        catch (IOException corrupt) { return new CandidateIndex(null, bytes, corrupt.getMessage()); }
    }
    private State load(Notes notes) throws IOException {
        CandidateIndex primary = candidate(indexFile());
        if (primary != null && primary.error() == null) return new State(primary.entries(), primary.bytes(), true);
        CandidateIndex mirror = candidate(indexMirror());
        if (mirror != null && mirror.error() == null) {
            remember(notes, "Route index recovery from the committed mirror was required"); writeAtomic(indexFile(), mirror.bytes());
            return new State(mirror.entries(), mirror.bytes(), true);
        }
        List<RouteEntry> rebuilt = scan(List.of(), notes); byte[] bytes = RouteCodec.index(rebuilt);
        if (primary != null || mirror != null || !rebuilt.isEmpty()) {
            createStorage(); remember(notes, "Route index recovery was required; local aliases may have reverted to original import names");
            writeAtomic(indexFile(), bytes); writeAtomic(indexMirror(), bytes);
            return new State(rebuilt, bytes, true);
        }
        return new State(List.of(), bytes, false);
    }
    private List<RouteEntry> scan(List<RouteEntry> previous, Notes notes) throws IOException {
        Map<UUID,RouteEntry> rows = new LinkedHashMap<>(); for (var entry : previous) rows.put(entry.id(), entry);
        File directory = routesDir(); if (!exists(directory)) return List.copyOf(rows.values()); safe(directory); int seen = 0;
        try (var children = Files.newDirectoryStream(directory.toPath())) {
            for (var path : children) {
                if (++seen > RouteStorageLimits.MAX_ROUTES * 2) throw new IOException("Too many route files to repair safely");
                String name = path.getFileName().toString();
                if (!name.matches(UUID_PATTERN + "\\.route")) { notes.add(null, "Unknown route file preserved: " + name); continue; }
                UUID id = UUID.fromString(name.substring(0, 36)); File file = safe(path.toFile()); futureHeader(file, RouteCodec.ROUTE_MAGIC);
                try {
                    byte[] bytes = read(file, RouteStorageLimits.MAX_ROUTE_FILE_BYTES); Route route = RouteCodec.route(bytes);
                    if (!route.id().equals(id)) throw new IOException("Route UUID differs from its file name");
                    RouteEntry old = rows.get(id);
                    if (old != null) {
                        if (!RouteCodec.sha256(bytes).equals(old.fileSha256())) throw new IOException("Existing route integrity differs; repair cannot bless changed geometry");
                        verified(old); route = route.renamed(old.displayName());
                    }
                    rows.put(id, RouteEntry.of(route, bytes)); if (rows.size() > RouteStorageLimits.MAX_ROUTES) throw new IOException("Route library capacity exceeded during repair");
                } catch (RouteCodec.FutureVersion future) { throw future; }
                catch (InterruptedIOException cancelled) { throw cancelled; }
                catch (IOException invalid) { notes.add(id, "Route file retained but unavailable: " + invalid.getMessage()); }
            }
        }
        return List.copyOf(rows.values());
    }
    private Route verified(RouteEntry entry) throws IOException {
        File file = routePath(entry.id()); futureHeader(file, RouteCodec.ROUTE_MAGIC); byte[] bytes = read(file, RouteStorageLimits.MAX_ROUTE_FILE_BYTES);
        if (bytes.length != entry.fileBytes() || !RouteCodec.sha256(bytes).equals(entry.fileSha256())) throw new IOException("Route file integrity check failed: " + entry.id());
        Route route = RouteCodec.route(bytes); RouteEntry actual = RouteEntry.of(route, bytes);
        if (!actual.renamed(entry.displayName()).equals(entry)) throw new IOException("Route file/index metadata differ: " + entry.id());
        return route.renamed(entry.displayName());
    }
    private Snapshot snapshot(State state, Notes notes, boolean verify) throws IOException {
        for (RouteEntry entry : state.entries()) {
            File file = routePath(entry.id());
            if (!exists(file)) notes.add(entry.id(), "Route file is missing");
            else if (file.length() != entry.fileBytes()) notes.add(entry.id(), "Route file size differs from index");
            else if (verify) try { verified(entry); } catch (RouteCodec.FutureVersion future) { throw future; }
            catch (InterruptedIOException cancelled) { throw cancelled; } catch (IOException corrupt) { notes.add(entry.id(), corrupt.getMessage()); }
        }
        return new Snapshot(state.entries(), notes.problems, notes.repaired, notes.pending);
    }
    private void ready(Notes notes, boolean mutation) throws IOException {
        interrupted(); if (!exists(base())) return;
        futureHeader(indexFile(), RouteCodec.INDEX_MAGIC); // Future schema never gets replaced by a mirror or cleanup.
        readNotice(notes);
        File pending = pendingDir(); int count = 0;
        if (exists(pending)) try (var transactions = Files.newDirectoryStream(pending.toPath())) {
            for (var path : transactions) {
                if (++count > RouteStorageLimits.MAX_PENDING_TRANSACTIONS) throw new IOException("Too many pending transactions");
                String name = path.getFileName().toString();
                if (!name.matches(UUID_PATTERN)) { notes.add(null, "Unknown pending data preserved: " + name); continue; }
                File tx = safe(path.toFile());
                try { recover(tx, notes); }
                catch (IOException unresolved) {
                    if (committedAndValid(tx)) { notes.pending = true; notes.add(null, "Committed routes are intact; temporary cleanup awaits recovery: " + unresolved.getMessage()); }
                    else throw new IOException("Unresolved route transaction; index rebuilding is blocked", unresolved);
                }
            }
        }
        if (mutation && notes.pending) throw new IOException("Complete pending route cleanup before another change");
        cleanGarbage(notes);
        if (mutation && notes.pending) throw new IOException("Complete pending route cleanup before another change");
        cleanIndexTemps();
    }
    private void cleanIndexTemps() throws IOException {
        if (!exists(base())) return;
        try (var files = Files.newDirectoryStream(base().toPath())) {
            for (var path : files) if (path.getFileName().toString().matches("(index\\.(bin|mirror)|repair\\.notice)\\.tmp-" + UUID_PATTERN)) remove(path.toFile());
        }
    }
    /** Retire an entire resolved journal atomically before deleting ANY of its recovery records. */
    private void retireJournal(File tx, Notes notes, boolean signal) throws IOException {
        mkdir(garbageDir()); File discarded = safe(new File(garbageDir(), tx.getName()));
        if (exists(discarded)) throw new IOException("Retired transaction UUID collision");
        safe(tx); Files.move(tx.toPath(), discarded.toPath(), StandardCopyOption.ATOMIC_MOVE);
        try { if (signal) faults.at("JOURNAL_RETIRED"); removeTree(discarded); }
        catch (IOException | RuntimeException cleanup) { notes.pending = true; notes.add(null, "Route change is committed/resolved; temporary cleanup deferred: " + cleanup.getMessage()); }
    }
    private void cleanGarbage(Notes notes) throws IOException {
        File folder = garbageDir(); if (!exists(folder)) return; int count = 0;
        try (var children = Files.newDirectoryStream(folder.toPath())) {
            for (var path : children) {
                if (++count > RouteStorageLimits.MAX_PENDING_TRANSACTIONS) throw new IOException("Too much retired transaction data");
                if (!path.getFileName().toString().matches(UUID_PATTERN)) { notes.add(null, "Unknown retired data preserved"); continue; }
                try { removeTree(path.toFile()); notes.repaired = true; notes.add(null, "Resolved transaction temporary files reclaimed after interruption"); }
                catch (IOException unavailable) { notes.pending = true; notes.add(null, "Resolved transaction data awaits cleanup: " + unavailable.getMessage()); }
            }
        }
    }
    private record Transaction(RouteCodec.Journal journal, byte[] before, byte[] after, List<RouteEntry> previous, List<RouteEntry> next) {}
    private Transaction transaction(File tx) throws IOException {
        safe(tx); RouteCodec.Journal journal = RouteCodec.journal(read(safe(new File(tx, "prepared.bin")), RouteStorageLimits.MAX_INDEX_BYTES));
        byte[] before = read(safe(new File(tx, "before.index")), RouteStorageLimits.MAX_INDEX_BYTES), after = read(safe(new File(tx, "after.index")), RouteStorageLimits.MAX_INDEX_BYTES);
        if (!RouteCodec.sha256(before).equals(journal.beforeSha256()) || !RouteCodec.sha256(after).equals(journal.afterSha256())) throw new IOException("Transaction index checksum differs");
        List<RouteEntry> old = RouteCodec.index(before), next = RouteCodec.index(after); Set<UUID> oldIds = new HashSet<>(), newIds = new HashSet<>();
        for (var entry : old) oldIds.add(entry.id()); for (var entry : next) newIds.add(entry.id());
        Set<UUID> additions = new HashSet<>(), removals = new HashSet<>();
        for (var file : journal.added()) {
            int at = indexOf(next, file.id());
            if (oldIds.contains(file.id()) || at < 0 || !next.get(at).fileSha256().equals(file.sha256()) || !additions.add(file.id())) throw new IOException("Unsafe added-file journal");
        }
        for (var file : journal.adopted()) {
            int at = indexOf(next, file.id());
            if (oldIds.contains(file.id()) || at < 0 || !next.get(at).fileSha256().equals(file.sha256()) || !additions.add(file.id())) throw new IOException("Unsafe adopted-file journal");
        }
        for (UUID id : newIds) if (!oldIds.contains(id) && !additions.contains(id)) throw new IOException("Index addition missing journal file");
        for (var file : journal.retired()) {
            int at = indexOf(old, file.id());
            if (newIds.contains(file.id()) || at < 0 || !old.get(at).fileSha256().equals(file.sha256()) || !removals.add(file.id())) throw new IOException("Unsafe retired-file journal");
        }
        return new Transaction(journal, before, after, old, next);
    }
    private boolean committedAndValid(File tx) throws IOException {
        if (!exists(safe(new File(tx, "prepared.bin")))) return false;
        Transaction data = transaction(tx); if (!exists(indexFile())) return false;
        byte[] actual = read(indexFile(), RouteStorageLimits.MAX_INDEX_BYTES);
        if (!RouteCodec.sha256(actual).equals(data.journal().afterSha256())) return false;
        List<RouteCodec.FileRef> verifiedFiles = new ArrayList<>(data.journal().added()); verifiedFiles.addAll(data.journal().adopted());
        for (var file : verifiedFiles) {
            int at = indexOf(data.next(), file.id()); try { verified(data.next().get(at)); } catch (IOException invalid) { return false; }
        }
        for (RouteEntry old : data.previous()) if (indexOf(data.next(), old.id()) < 0 && exists(routePath(old.id()))) return false;
        return true;
    }
    private boolean recover(File tx, Notes notes) throws IOException {
        if (!exists(safe(new File(tx, "prepared.bin")))) { remember(notes, "Incomplete route staging was removed; previous routes retained"); retireJournal(tx, notes, false); return false; }
        Transaction data = transaction(tx); boolean committed = committedAndValid(tx);
        if (committed) writeAtomic(indexMirror(), data.after());
        else {
            for (var file : data.journal().added()) remove(routePath(file.id()));
            for (var file : data.journal().retired()) {
                File retired = safe(new File(new File(tx, "retired"), file.id() + ".route")), original = routePath(file.id());
                if (exists(retired)) { if (exists(original)) throw new IOException("Retired route has a conflicting live file"); move(retired, original); }
            }
            if (data.journal().beforeExisted()) { writeAtomic(indexFile(), data.before()); writeAtomic(indexMirror(), data.before()); }
            else { remove(indexFile()); remove(indexMirror()); }
        }
        remember(notes, committed ? "Committed route transaction cleanup completed" : "Interrupted route change rolled back; previous routes retained");
        retireJournal(tx, notes, false); return committed;
    }
    private void commit(State before, List<RouteEntry> after, Map<UUID,byte[]> added, List<RouteCodec.FileRef> retired, Notes notes) throws IOException {
        commit(before, after, added, retired, List.of(), notes);
    }
    private void commit(State before, List<RouteEntry> after, Map<UUID,byte[]> added, List<RouteCodec.FileRef> retired, List<RouteCodec.FileRef> adopted, Notes notes) throws IOException {
        createStorage(); byte[] next = RouteCodec.index(after); File tx = safe(new File(pendingDir(), UUID.randomUUID().toString()));
        if (exists(tx)) throw new IOException("Transaction UUID collision"); mkdir(tx); File staged = safe(new File(tx, "staged")), oldFiles = safe(new File(tx, "retired")); mkdir(staged); mkdir(oldFiles);
        try {
            List<RouteCodec.FileRef> additions = new ArrayList<>();
            for (var entry : added.entrySet()) {
                if (exists(routePath(entry.getKey()))) throw new IOException("Existing route UUID cannot be replaced");
                File file = safe(new File(staged, entry.getKey() + ".route")); write(file, entry.getValue()); byte[] checked = read(file, RouteStorageLimits.MAX_ROUTE_FILE_BYTES);
                if (!RouteCodec.sha256(checked).equals(RouteCodec.sha256(entry.getValue())) || !RouteCodec.route(checked).id().equals(entry.getKey())) throw new IOException("Staged route verification failed");
                additions.add(new RouteCodec.FileRef(entry.getKey(), RouteCodec.sha256(checked)));
            }
            for (var file : adopted) {
                int at = indexOf(after, file.id());
                if (at < 0 || indexOf(before.entries(), file.id()) >= 0 || !after.get(at).fileSha256().equals(file.sha256())) throw new IOException("Invalid orphan adoption");
                verified(after.get(at));
            }
            faults.at("STAGED");
            write(safe(new File(tx, "before.index")), before.bytes()); write(safe(new File(tx, "after.index")), next);
            var journal = new RouteCodec.Journal(before.existed(), RouteCodec.sha256(before.bytes()), RouteCodec.sha256(next), additions, retired, adopted);
            writeAtomic(safe(new File(tx, "prepared.bin")), RouteCodec.journal(journal)); transaction(tx); faults.at("PREPARED");
            int index = 0;
            for (var file : additions) { move(safe(new File(staged, file.id() + ".route")), routePath(file.id())); faults.at("PROMOTED_" + index++); }
            index = 0;
            for (var file : retired) { move(routePath(file.id()), safe(new File(oldFiles, file.id() + ".route"))); faults.at("RETIRED_" + index++); }
            faults.at("BEFORE_INDEX"); writeAtomic(indexFile(), next); faults.at("INDEX_COMMITTED");
            if (!committedAndValid(tx)) throw new IOException("Committed route index/files disagree"); faults.at("VERIFIED");
            faults.at("BEFORE_MIRROR"); writeAtomic(indexMirror(), next); faults.at("BEFORE_CLEANUP"); retireJournal(tx, notes, true);
        } catch (IOException | RuntimeException failure) {
            boolean interrupted = Thread.interrupted();
            try {
                boolean committed = false;
                try { committed = recover(tx, notes); }
                catch (IOException recovery) {
                    if (committedAndValid(tx)) { notes.pending = true; notes.add(null, "Routes committed; temporary cleanup deferred: " + recovery.getMessage()); return; }
                    failure.addSuppressed(recovery); throw new IOException("Route change failed; pending recovery retained", failure);
                }
                if (committed) { notes.add(null, "Route commit independently verified after storage interruption"); return; }
                throw new IOException("Route change failed and was rolled back", failure);
            } finally { if (interrupted) Thread.currentThread().interrupt(); }
        }
    }
}
