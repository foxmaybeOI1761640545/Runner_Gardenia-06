package de.dennisguse.opentracks.p0.routes.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.P08RouteDetailBinding;
import de.dennisguse.opentracks.p0.routes.*;
import java.util.UUID;

/** Verified planned-route detail; local aliases and deletions only affect the independent route store. */
public final class RouteDetailActivity extends AbstractActivity {
    public static final String ID = "p08.route.id", ACTION = "p08.route.action";
    private P08RouteDetailBinding binding;
    private RouteLibraryViewModel model;
    private UUID id;
    private int pendingAction, restoredScroll;
    private boolean restorePending;
    public static Intent intent(Context c, UUID id) { return new Intent(c, RouteDetailActivity.class).putExtra(ID, id.toString()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); }
    @Override protected View createRootView() { binding = P08RouteDetailBinding.inflate(getLayoutInflater()); return binding.getRoot(); }
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved); restorePending=saved!=null;if(saved!=null)restoredScroll=saved.getInt("detailScroll");setSupportActionBar(binding.p08DetailToolbar);
        if (getSupportActionBar() != null) { getSupportActionBar().setTitle(R.string.p08_detail_title); getSupportActionBar().setDisplayHomeAsUpEnabled(true); }
        model = new ViewModelProvider(this).get(RouteLibraryViewModel.class);
        try { id = UUID.fromString(getIntent().getStringExtra(ID)); } catch (IllegalArgumentException | NullPointerException invalid) { binding.p08DetailStatus.setText(R.string.p08_route_missing); binding.p08DetailRename.setEnabled(false); binding.p08DetailDelete.setEnabled(false); return; }
        pendingAction = saved == null ? getIntent().getIntExtra(ACTION, 0) : 0;
        binding.p08DetailRename.setOnClickListener(v -> rename()); binding.p08DetailDelete.setOnClickListener(v -> delete()); model.state().observe(this, this::render);
    }
    @Override protected void onStart() { super.onStart(); if (id != null) model.refresh(id); }
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("detailScroll",binding.p08DetailScroll.getScrollY());super.onSaveInstanceState(out);}
    private void render(RouteLibraryViewModel.State state) {
        if (state.deleted()) { finish(); return; }
        binding.p08DetailProgress.setVisibility(state.loading() ? View.VISIBLE : View.GONE); var entry = state.entry();
        binding.p08DetailRename.setEnabled(!state.loading() && state.route() != null); binding.p08DetailDelete.setEnabled(!state.loading() && entry != null);
        binding.p08DetailStatus.setText(state.loading() ? getString(R.string.p08_library_loading) : state.error() != null ? getString(R.string.p08_detail_error, state.error()) : entry == null ? getString(R.string.p08_route_missing) : getString(R.string.p08_local_route_hint));
        if (entry != null) {
            binding.p08DetailName.setText(entry.displayName());
            String metadata = getString(R.string.p08_detail_summary, RouteUiFormat.distance(this, entry), entry.pointCount(), entry.segmentCount(), RouteUiFormat.date(entry.importedAtMillis()), RouteUiFormat.filename(this, entry.sourceFileName()), entry.sourceType().name());
            if (state.route() != null) {
                Route route = state.route(); metadata += "\n\n" + getString(R.string.p08_original_name, route.originalName().isEmpty() ? getString(R.string.p08_not_provided) : route.originalName());
                metadata += "\n\n" + getString(R.string.p08_description, route.description().isEmpty() ? getString(R.string.p08_not_provided) : route.description());
                if (route.metadata().containsKey("type")) metadata += "\n\n" + getString(R.string.p08_route_type, route.metadata().get("type"));
            }
            binding.p08DetailMetadata.setText(metadata);
            if(restorePending&&!state.loading()){
                restorePending=false;binding.p08DetailScroll.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){
                    @Override public boolean onPreDraw(){binding.p08DetailScroll.getViewTreeObserver().removeOnPreDrawListener(this);binding.p08DetailScroll.scrollTo(0,restoredScroll);return true;}
                });
            }
        }
        if (!state.loading() && entry != null && pendingAction != 0) { int action = pendingAction; pendingAction = 0; if (action == 1 && state.route() != null) rename(); else if (action == 2) delete(); }
    }
    private void rename() {
        var state = model.state().getValue(); if (state.loading() || state.route() == null) return;
        EditText input = new EditText(this); input.setId(R.id.p08_rename_input); input.setSingleLine(true); input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Route.MAX_DISPLAY_NAME_CHARS)}); input.setText(state.route().displayName()); input.setSelection(input.length()); input.setHint(R.string.p08_local_name_hint);
        int padding = Math.round(20 * getResources().getDisplayMetrics().density); var holder = new android.widget.FrameLayout(this); holder.setPadding(padding, 0, padding, 0); holder.addView(input);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.p08_rename_route).setMessage(R.string.p08_rename_explanation).setView(holder).setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> { String name = input.getText().toString().strip(); if (name.isEmpty()) { input.setError(getString(R.string.p08_name_required)); return; } model.rename(id, name); dialog.dismiss(); })); dialog.show();
    }
    private void delete() {
        var state = model.state().getValue(); if (state.loading() || state.entry() == null) return;
        new AlertDialog.Builder(this).setTitle(R.string.p08_delete_route).setMessage(getString(R.string.p08_delete_confirm, state.entry().displayName())).setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.p08_delete_route, (d, w) -> model.delete(id)).show();
    }
}
