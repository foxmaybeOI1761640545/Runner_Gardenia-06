package de.dennisguse.opentracks.p0.routes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.P08RouteImportBinding;
import de.dennisguse.opentracks.p0.gps.MotionFormat;
import de.dennisguse.opentracks.p0.routes.GpxRouteImporter;
import java.util.*;

/** Ordinary secondary destination; only an explicit document choice can begin a route import. */
public final class RouteImportActivity extends AbstractActivity {
    public static final String IMPORTED_IDS = "p08.imported.ids";
    private P08RouteImportBinding binding;
    private RouteImportViewModel model;
    private GpxRouteImporter.Preview displayed;
    private boolean restoreLost;
    private final Map<Integer,CheckBox> boxes = new HashMap<>();
    private final androidx.activity.result.ActivityResultLauncher<String[]> document = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> { if (uri != null) model.read(uri); });
    @Override protected View createRootView() { binding = P08RouteImportBinding.inflate(getLayoutInflater()); return binding.getRoot(); }
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved); setSupportActionBar(binding.p08ImportToolbar);
        if (getSupportActionBar() != null) { getSupportActionBar().setTitle(R.string.p08_import_title); getSupportActionBar().setDisplayHomeAsUpEnabled(true); }
        model = new ViewModelProvider(this).get(RouteImportViewModel.class);
        restoreLost = saved != null && saved.getBoolean("hadPreview") && model.state().getValue().phase() == RouteImportViewModel.Phase.EMPTY;
        binding.p08ImportChoose.setOnClickListener(v -> {
            if (model.state().getValue().phase() == RouteImportViewModel.Phase.READING) { model.cancelReading(); return; }
            if (model.state().getValue().phase() == RouteImportViewModel.Phase.SUCCESS) { finish(); return; }
            try { document.launch(new String[]{"*/*"}); } catch (RuntimeException unavailable) { model.pickerFailed(unavailable); }
        });
        binding.p08ImportCommit.setOnClickListener(v -> {
            if (model.selectedDuplicates()) new AlertDialog.Builder(this).setTitle(R.string.p08_duplicate_title).setMessage(R.string.p08_duplicate_message)
                    .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.p08_import_extra_copy, (d, w) -> model.commit(true)).show();
            else model.commit(false);
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (model.state().getValue().phase() == RouteImportViewModel.Phase.COMMITTING) return;
                model.cancelReading(); finish();
            }
        });
        model.state().observe(this, this::render);
    }
    @Override protected void onSaveInstanceState(Bundle out) { out.putBoolean("hadPreview", model.state().getValue().preview() != null || model.busy()); super.onSaveInstanceState(out); }
    private void render(RouteImportViewModel.State state) {
        var phase = state.phase(); boolean reading = phase == RouteImportViewModel.Phase.READING, committing = phase == RouteImportViewModel.Phase.COMMITTING;
        binding.p08ImportProgress.setVisibility(reading || committing ? View.VISIBLE : View.GONE);
        binding.p08ImportChoose.setEnabled(!committing);
        binding.p08ImportChoose.setText(reading ? R.string.p08_cancel_reading : phase == RouteImportViewModel.Phase.SUCCESS ? R.string.p08_done : R.string.p08_choose_gpx);
        binding.p08ImportCommit.setEnabled(phase == RouteImportViewModel.Phase.PREVIEW && !state.selected().isEmpty());
        binding.p08ImportCommit.setText(getString(R.string.p08_import_selected_count, state.selected().size()));
        String status = switch (phase) {
            case EMPTY -> getString(restoreLost ? R.string.p08_preview_lost : R.string.p08_choose_hint);
            case READING -> getString(R.string.p08_reading, state.bytesRead() / 1024);
            case PREVIEW -> state.error() == null ? getString(R.string.p08_preview_counts, state.preview().candidates().size(), state.preview().validRoutes()) : getString(R.string.p08_import_error, state.error());
            case COMMITTING -> getString(R.string.p08_saving_routes);
            case SUCCESS -> getString(R.string.p08_import_success, state.imported().size());
            case CANCELLED -> getString(R.string.p08_import_cancelled);
            case ERROR -> getString(R.string.p08_import_error, state.error());
        };
        binding.p08ImportStatus.setText(status);
        if (state.preview() != displayed) {
            displayed = state.preview(); boxes.clear(); binding.p08ImportCandidates.removeAllViews();
            if (displayed == null) { binding.p08ImportFile.setText(""); binding.p08ImportWarnings.setText(""); }
            else {
                binding.p08ImportFile.setText(displayed.fileName() == null ? getString(R.string.p08_unknown_file) : displayed.fileName());
                List<String> warnings = new ArrayList<>(displayed.warnings());
                if (displayed.ignoredWaypoints() > 0) warnings.add(getString(R.string.p08_waypoints_ignored, displayed.ignoredWaypoints()));
                binding.p08ImportWarnings.setText(String.join("\n", warnings));
                Set<String> duplicateKeys = new HashSet<>(); for (var entry : state.duplicates()) duplicateKeys.add(entry.duplicateKey());
                for (var candidate : displayed.candidates()) {
                    CheckBox box = new CheckBox(this); box.setId(View.generateViewId()); box.setTag("p08-candidate-" + candidate.ordinal()); box.setTextSize(16); box.setPadding(0, 12, 0, 12); box.setMinHeight(Math.round(64 * getResources().getDisplayMetrics().density));
                    String text = candidate.name() + "\n" + getString(R.string.p08_candidate_summary, candidate.points(), candidate.segments(), candidate.type().name(), candidate.valid() && candidate.route().distanceMeters().isPresent() ? MotionFormat.distance(candidate.route().distanceMeters().getAsDouble()) : getString(R.string.p08_unavailable));
                    if (!candidate.valid()) text += "\n" + getString(R.string.p08_invalid_route, candidate.error());
                    else if (duplicateKeys.contains(candidate.route().duplicateKey())) text += "\n" + getString(R.string.p08_already_imported);
                    if (!candidate.warnings().isEmpty()) text += "\n" + String.join("\n", candidate.warnings());
                    box.setText(text); box.setEnabled(candidate.valid() && phase == RouteImportViewModel.Phase.PREVIEW); box.setOnCheckedChangeListener((button, checked) -> model.selected(candidate.ordinal(), checked));
                    boxes.put(candidate.ordinal(), box); binding.p08ImportCandidates.addView(box);
                }
            }
        }
        for (var candidate : state.preview() == null ? List.<GpxRouteImporter.Candidate>of() : state.preview().candidates()) {
            CheckBox box = boxes.get(candidate.ordinal()); box.setEnabled(candidate.valid() && phase == RouteImportViewModel.Phase.PREVIEW);
            boolean checked = state.selected().contains(candidate.ordinal()); if (box.isChecked() != checked) box.setChecked(checked);
        }
        if (phase == RouteImportViewModel.Phase.SUCCESS) {
            Intent result = new Intent().putStringArrayListExtra(IMPORTED_IDS, new ArrayList<>(state.imported().stream().map(UUID::toString).toList())); setResult(RESULT_OK, result);
        }
    }
}
