package de.dennisguse.opentracks.p0.routes.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;
import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.P08RouteLibraryBinding;
import de.dennisguse.opentracks.databinding.P08RouteRowBinding;
import de.dennisguse.opentracks.p0.routes.*;
import java.util.*;

/** Private planned routes only; secondary caller stack is preserved without root-navigation flags. */
public final class RouteLibraryActivity extends AbstractActivity {
    private P08RouteLibraryBinding binding;
    private RouteLibraryViewModel model;
    private final Rows rows = new Rows();
    private boolean opening, restorePending, loadRequested;
    private int restoredPosition, restoredTop;
    public static Intent intent(Context c) { return new Intent(c, RouteLibraryActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); }
    @Override protected View createRootView() { binding = P08RouteLibraryBinding.inflate(getLayoutInflater()); return binding.getRoot(); }
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        restorePending=saved!=null; if(saved!=null){restoredPosition=saved.getInt("listPosition");restoredTop=saved.getInt("listTop");}
        setSupportActionBar(binding.p08LibraryToolbar);
        if (getSupportActionBar() != null) { getSupportActionBar().setTitle(R.string.p08_library_title); getSupportActionBar().setDisplayHomeAsUpEnabled(true); }
        model = new ViewModelProvider(this).get(RouteLibraryViewModel.class); binding.p08RouteList.setAdapter(rows);
        binding.p08RouteList.setOnItemClickListener((parent, view, position, id) -> open(rows.getItem(position), 0));
        binding.p08LibraryImport.setOnClickListener(v -> { if (opening) return; opening = true; startActivity(new Intent(this, RouteImportActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)); });
        model.state().observe(this, this::render);
    }
    @Override protected void onStart() { super.onStart(); loadRequested=true; model.refresh(null); }
    @Override protected void onResume() { super.onResume(); opening = false; }
    @Override protected void onSaveInstanceState(Bundle out) {out.putInt("listPosition",binding.p08RouteList.getFirstVisiblePosition());View first=binding.p08RouteList.getChildAt(0);out.putInt("listTop",first==null?0:first.getTop());super.onSaveInstanceState(out);}
    private void open(RouteEntry entry, int action) { if (opening) return; opening = true; startActivity(RouteDetailActivity.intent(this, entry.id()).putExtra(RouteDetailActivity.ACTION, action)); }
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, R.string.p08_refresh); menu.add(0, 2, 1, R.string.p08_verify_files); menu.add(0, 3, 2, R.string.p08_repair_index); menu.add(0, 4, 3, R.string.p08_dismiss_recovery); menu.add(0, 5, 4, R.string.p08_review_issues); return true;
    }
    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        var value = model.state().getValue(); for (int i = 0; i < menu.size(); i++) menu.getItem(i).setEnabled(!value.loading()); menu.findItem(4).setVisible(value.repaired()); menu.findItem(5).setVisible(!value.problems().isEmpty()); return super.onPrepareOptionsMenu(menu);
    }
    @Override public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case 1 -> model.refresh(null); case 2 -> model.verifyFiles();
            case 3 -> new AlertDialog.Builder(this).setTitle(R.string.p08_repair_index).setMessage(R.string.p08_repair_confirm).setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, (d, w) -> model.repair()).show();
            case 4 -> model.dismissRepairNotice();
            case 5 -> { var text = new TextView(this); text.setPadding(24, 16, 24, 16); text.setTextIsSelectable(true); text.setText(String.join("\n\n", model.state().getValue().problems().stream().map(RouteRepository.Problem::reason).toList())); var scroll = new ScrollView(this); scroll.addView(text); new AlertDialog.Builder(this).setTitle(R.string.p08_review_issues).setView(scroll).setPositiveButton(android.R.string.ok, null).show(); }
            default -> { return super.onOptionsItemSelected(item); }
        }
        return true;
    }
    private void render(RouteLibraryViewModel.State value) {
        binding.p08LibraryProgress.setVisibility(value.loading() ? View.VISIBLE : View.GONE);
        String status = value.loading() ? getString(R.string.p08_library_loading) : value.error() != null ? getString(R.string.p08_library_error, value.error()) : getString(R.string.p08_library_count, value.entries().size());
        for (int i = 0; i < Math.min(3, value.problems().size()); i++) status += "\n" + value.problems().get(i).reason();
        if (value.problems().size() > 3) status += "\n" + getString(R.string.p08_more_problems, value.problems().size() - 3);
        binding.p08LibraryStatus.setText(status); binding.p08RouteList.setEnabled(!value.loading());
        binding.p08RouteList.setVisibility(value.entries().isEmpty() ? View.GONE : View.VISIBLE); binding.p08LibraryEmpty.setVisibility(value.entries().isEmpty() ? View.VISIBLE : View.GONE);
        binding.p08LibraryEmpty.setText(value.loading() ? R.string.p08_library_loading : value.error() != null ? R.string.p08_library_retry_hint : R.string.p08_library_empty);
        rows.entries = value.entries(); rows.problems = value.problems(); rows.busy = value.loading(); rows.notifyDataSetChanged();
        if(restorePending&&loadRequested&&!value.loading()){restorePending=false;binding.p08RouteList.setSelectionFromTop(Math.max(0,Math.min(restoredPosition,rows.getCount()-1)),restoredTop);}
        invalidateOptionsMenu();
    }
    private final class Rows extends BaseAdapter {
        List<RouteEntry> entries = List.of(); List<RouteRepository.Problem> problems = List.of(); boolean busy;
        @Override public int getCount() { return entries.size(); }
        @Override public RouteEntry getItem(int position) { return entries.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View convert, ViewGroup parent) {
            P08RouteRowBinding row = convert == null ? P08RouteRowBinding.inflate(getLayoutInflater(), parent, false) : P08RouteRowBinding.bind(convert); var entry = getItem(position);
            row.p08RouteName.setText(entry.displayName()); row.p08RouteSummary.setText(getString(R.string.p08_library_row_summary, RouteUiFormat.distance(RouteLibraryActivity.this, entry), entry.pointCount(), entry.segmentCount()));
            row.p08RouteDate.setText(getString(R.string.p08_imported_date, RouteUiFormat.date(entry.importedAtMillis())));
            String source = getString(R.string.p08_source_file, RouteUiFormat.filename(RouteLibraryActivity.this, entry.sourceFileName()));
            if (problems.stream().anyMatch(p -> entry.id().equals(p.id()))) source += "\n" + getString(R.string.p08_file_unavailable);
            row.p08RouteSource.setText(source); row.p08RouteMore.setEnabled(!busy); row.p08RouteMore.setContentDescription(getString(R.string.p08_actions_for_route, entry.displayName()));
            row.p08RouteMore.setOnClickListener(v -> { var menu = new PopupMenu(RouteLibraryActivity.this, v); menu.getMenu().add(0, 1, 0, R.string.p08_rename_route); menu.getMenu().add(0, 2, 1, R.string.p08_delete_route); menu.setOnMenuItemClickListener(item -> { open(entry, item.getItemId()); return true; }); menu.show(); });
            return row.getRoot();
        }
    }
}
