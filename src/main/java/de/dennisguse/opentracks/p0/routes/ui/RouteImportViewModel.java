package de.dennisguse.opentracks.p0.routes.ui;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import de.dennisguse.opentracks.p0.routes.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Rotation-retained two-phase import. All provider/repository IO belongs to one worker. */
public final class RouteImportViewModel extends AndroidViewModel {
    public enum Phase { EMPTY, READING, PREVIEW, COMMITTING, SUCCESS, ERROR, CANCELLED }
    public record State(Phase phase, GpxRouteImporter.Preview preview, Set<Integer> selected,
                        List<RouteEntry> duplicates, long bytesRead, String error, List<UUID> imported) {
        public State { selected = Set.copyOf(selected); duplicates = List.copyOf(duplicates); imported = List.copyOf(imported); }
    }
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Phase.EMPTY, null, Set.of(), List.of(), 0, null, List.of()));
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicReference<InputStream> stream = new AtomicReference<>();
    private Future<?> operation;
    private volatile long generation;
    private volatile boolean closed;
    public RouteImportViewModel(@NonNull Application application) { super(application); }
    public LiveData<State> state() { return state; }
    public boolean busy() { Phase p = state.getValue().phase(); return p == Phase.READING || p == Phase.COMMITTING; }
    private RouteRepository repository() { return new RouteRepository(new File(getApplication().getFilesDir(), "route-library")); }
    private void publish(long token, State value) { main.post(() -> { if (!closed && token == generation) state.setValue(value); }); }
    public void read(Uri uri) {
        if (busy() || closed || uri == null) return;
        long token = ++generation; state.setValue(new State(Phase.READING, null, Set.of(), List.of(), 0, null, List.of()));
        operation = worker.submit(() -> {
            GpxRouteImporter.Preview preview = null;
            try {
                String name = null;
                try (var cursor = getApplication().getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) name = cursor.getString(0);
                } catch (RuntimeException unavailableMetadata) { /* Display name is optional; content stream decides GPX validity. */ }
                InputStream input = getApplication().getContentResolver().openInputStream(uri);
                if (input == null) throw new IOException("Document provider returned no input stream");
                stream.set(input);
                try (input) {
                    if (closed || token != generation || Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Import cancelled");
                    var counted = new FilterInputStream(input) {
                        long bytes, lastUpdate;
                        void progress(int amount) {
                            if (amount <= 0) return; bytes += amount; long now = android.os.SystemClock.elapsedRealtime();
                            if (now - lastUpdate >= 100) { lastUpdate = now; publish(token, new State(Phase.READING, null, Set.of(), List.of(), bytes, null, List.of())); }
                        }
                        @Override public int read() throws IOException { int value = in.read(); progress(value == -1 ? 0 : 1); return value; }
                        @Override public int read(byte[] b, int off, int len) throws IOException { int n = in.read(b, off, len); progress(n); return n; }
                    };
                    preview = new GpxRouteImporter().parse(counted, name, System.currentTimeMillis());
                } finally { stream.compareAndSet(input, null); }
                if (closed || token != generation || Thread.currentThread().isInterrupted()) return;
                List<Route> valid = preview.candidates().stream().filter(GpxRouteImporter.Candidate::valid).map(GpxRouteImporter.Candidate::route).toList();
                var duplicates = repository().duplicates(valid);
                publish(token, new State(Phase.PREVIEW, preview, Set.of(), duplicates, 0, null, List.of()));
            } catch (GpxRouteImporter.ImportException error) {
                publish(token, new State(error.code() == GpxRouteImporter.ErrorCode.INTERRUPTED ? Phase.CANCELLED : Phase.ERROR,
                        error.rejectedPreview(), Set.of(), List.of(), 0, error.code() + ": " + error.getMessage(), List.of()));
            } catch (IOException | RuntimeException error) {
                publish(token, new State(Phase.ERROR, preview, Set.of(), List.of(), 0, error.getClass().getSimpleName() + ": " + error.getMessage(), List.of()));
            }
        });
    }
    public void selected(int ordinal, boolean checked) {
        State current = state.getValue(); if (current.phase() != Phase.PREVIEW || current.preview() == null) return;
        if (current.preview().candidates().stream().noneMatch(c -> c.ordinal() == ordinal && c.valid())) return;
        var next = new HashSet<>(current.selected()); if (checked) next.add(ordinal); else next.remove(ordinal);
        state.setValue(new State(Phase.PREVIEW, current.preview(), next, current.duplicates(), 0, null, List.of()));
    }
    public boolean selectedDuplicates() {
        State current = state.getValue(); if (current.preview() == null) return false;
        Set<String> keys = new HashSet<>(); for (var row : current.duplicates()) keys.add(row.duplicateKey());
        return current.preview().candidates().stream().anyMatch(c -> current.selected().contains(c.ordinal()) && c.valid() && keys.contains(c.route().duplicateKey()));
    }
    public void commit(boolean explicitlyAllowExtraCopies) {
        State current = state.getValue(); if (closed || current.phase() != Phase.PREVIEW || current.selected().isEmpty()) return;
        if (selectedDuplicates() && !explicitlyAllowExtraCopies) return;
        List<Route> selected = current.preview().candidates().stream().filter(c -> c.valid() && current.selected().contains(c.ordinal())).map(GpxRouteImporter.Candidate::route).toList();
        long token = ++generation; state.setValue(new State(Phase.COMMITTING, current.preview(), current.selected(), current.duplicates(), 0, null, List.of()));
        operation = worker.submit(() -> {
            try {
                RouteRepository repo = repository(); var result = repo.add(selected, explicitlyAllowExtraCopies);
                if (!result.added().equals(selected.stream().map(Route::id).toList())) throw new IOException("Committed selection identity differs");
                for (Route route : selected) if (!route.equals(repo.get(route.id()))) throw new IOException("Committed route verification differs; inspect the route library");
                publish(token, new State(Phase.SUCCESS, current.preview(), current.selected(), current.duplicates(), 0, null, result.added()));
            } catch (RouteRepository.DuplicateImport duplicate) {
                publish(token, new State(Phase.PREVIEW, current.preview(), current.selected(), duplicate.matches(), 0, duplicate.getMessage(), List.of()));
            } catch (IOException | RuntimeException error) {
                publish(token, new State(Phase.ERROR, current.preview(), current.selected(), current.duplicates(), 0, error.getClass().getSimpleName() + ": " + error.getMessage(), List.of()));
            }
        });
    }
    public void pickerFailed(RuntimeException error) {
        if (busy()) return; ++generation; state.setValue(new State(Phase.ERROR, null, Set.of(), List.of(), 0, error.getClass().getSimpleName() + ": " + error.getMessage(), List.of()));
    }
    /** Cancellation is available only before formal commit. Closing a provider stream never blocks UI. */
    public void cancelReading() {
        if (state.getValue().phase() != Phase.READING) return;
        ++generation; if (operation != null) operation.cancel(true); closeStream();
        state.setValue(new State(Phase.CANCELLED, null, Set.of(), List.of(), 0, null, List.of()));
    }
    private void closeStream() {
        InputStream input = stream.getAndSet(null);
        if (input != null) { Thread closer = new Thread(() -> { try { input.close(); } catch (IOException ignored) { } }, "gpx-import-close"); closer.setDaemon(true); closer.start(); }
    }
    @Override protected void onCleared() { closed = true; ++generation; if (operation != null) operation.cancel(true); closeStream(); worker.shutdownNow(); }
}
