package de.dennisguse.opentracks.p0.routes.ui;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import de.dennisguse.opentracks.p0.routes.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** One worker per secondary screen; immutable summaries and verified detail never share a workout store. */
public final class RouteLibraryViewModel extends AndroidViewModel {
    public record State(List<RouteEntry> entries, List<RouteRepository.Problem> problems, UUID requested, Route route,
                        boolean loading, boolean repaired, boolean deleted, String error) {
        public State { entries = List.copyOf(entries); problems = List.copyOf(problems); }
        public RouteEntry entry() { return entries.stream().filter(e -> e.id().equals(requested)).findFirst().orElse(null); }
    }
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(List.of(), List.of(), null, null, false, false, false, null));
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean closed;
    private Future<?> task;
    public RouteLibraryViewModel(@NonNull Application application) { super(application); }
    public LiveData<State> state() { return state; }
    private RouteRepository repo() { return new RouteRepository(new File(getApplication().getFilesDir(), "route-library")); }
    private interface Operation { RouteRepository.Snapshot run(RouteRepository repository) throws IOException; }
    private void execute(UUID id, boolean deleted, Operation operation) {
        State old = state.getValue(); if (closed || old.loading()) return;
        state.setValue(new State(old.entries(), old.problems(), id, old.route(), true, old.repaired(), false, null));
        task = worker.submit(() -> {
            List<RouteEntry> entries = old.entries(); List<RouteRepository.Problem> problems = old.problems(); Route route = null; boolean repaired = old.repaired(); String error = null; boolean done = false;
            try {
                RouteRepository repository = repo(); var snapshot = operation.run(repository); entries = snapshot.entries(); problems = snapshot.problems(); repaired = snapshot.repaired(); done = deleted;
                if (id != null && !deleted) route = repository.get(id);
            } catch (IOException | RuntimeException failure) { error = failure.getClass().getSimpleName() + ": " + failure.getMessage(); }
            State next = new State(entries, problems, id, route, false, repaired, done, error);
            main.post(() -> { if (!closed) state.setValue(next); });
        });
    }
    public void refresh(UUID id) { execute(id, false, RouteRepository::list); }
    public void rename(UUID id, String name) { execute(id, false, repository -> repository.rename(id, name)); }
    public void delete(UUID id) { execute(id, true, repository -> { repository.delete(id); return repository.list(); }); }
    public void verifyFiles() { execute(state.getValue().requested(), false, RouteRepository::verifyAll); }
    public void repair() { execute(state.getValue().requested(), false, RouteRepository::repair); }
    public void dismissRepairNotice() { execute(state.getValue().requested(), false, repository -> { repository.acknowledgeRepairNotice(); return repository.list(); }); }
    @Override protected void onCleared() { closed = true; if (task != null) task.cancel(true); worker.shutdownNow(); }
}
