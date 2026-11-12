package de.dennisguse.opentracks.p0.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.P0HistoryBinding;
import de.dennisguse.opentracks.p0.data.WorkoutSummary;
import de.dennisguse.opentracks.p0.gps.MotionFormat;

public class P0HistoryActivity extends AbstractActivity {
    private static final String WORKOUT_ID="p0.workoutId";
    private P0HistoryBinding binding;
    private P0HistoryViewModel model;
    private List<WorkoutSummary> rows=List.of();
    private ArrayAdapter<WorkoutSummary> adapter;
    private static final String MANAGEMENT="p05.management",CHOSEN="p05.chosen",MODE="p05.mode",PENDING="p05.pending",PENDING_ZIP="p05.pendingZip";
    private long[] pendingIds;
    private boolean pendingZip;
    private final androidx.activity.result.ActivityResultLauncher<String> document=registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/gpx+xml") {
                @NonNull @Override public Intent createIntent(@NonNull Context context,@NonNull String name){return super.createIntent(context,name).addCategory(Intent.CATEGORY_OPENABLE);}
            },this::documentResult);
    private final androidx.activity.result.ActivityResultLauncher<String> zipDocument=registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip") {
                @NonNull @Override public Intent createIntent(@NonNull Context context,@NonNull String name){return super.createIntent(context,name).addCategory(Intent.CATEGORY_OPENABLE);}
            },this::documentResult);
    public static Intent intent(Context context,long id){return new Intent(context,P0HistoryActivity.class).putExtra(WORKOUT_ID,id);}
    public static Intent managementIntent(Context context){return new Intent(context,P0HistoryActivity.class).putExtra(MANAGEMENT,true);}
    private static long[] array(java.util.List<Long> ids){return ids.stream().mapToLong(Long::longValue).toArray();}
    private static java.util.List<Long> list(long[] ids){return java.util.Arrays.stream(ids).boxed().toList();}
    private void documentResult(android.net.Uri uri){long[] ids=pendingIds;boolean zip=pendingZip;pendingIds=null;if(uri!=null&&ids!=null)model.exportSnapshot(list(ids),zip,uri);if(model!=null)render(model.state().getValue());}
    private String date(java.time.Instant time){return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(time);}
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);P0Navigation.bind(this);model=new ViewModelProvider(this).get(P0HistoryViewModel.class);
        setSupportActionBar(binding.p0HistoryToolbar);
        if(getSupportActionBar()!=null)getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_2) {
            @Override public int getCount(){return rows.size();}
            @Override public WorkoutSummary getItem(int index){return rows.get(index);}
            @Override public long getItemId(int index){return rows.get(index).track().id().id();}
            @Override public boolean hasStableIds(){return true;}
            @NonNull @Override public View getView(int index,View recycled,@NonNull ViewGroup parent) {
                View row=recycled!=null?recycled:getLayoutInflater().inflate(R.layout.p05_history_row,parent,false);
                WorkoutSummary value=getItem(index);
                ((TextView)row.findViewById(android.R.id.text1)).setText(value.track().name());
                ((TextView)row.findViewById(android.R.id.text2)).setText(getString(R.string.p0_history_row,date(value.track().statistics().startTime()),value.track().statistics().totalDistance().toM(),MotionFormat.duration(value.track().statistics().totalDuration())));
                boolean selecting=model.selection().getValue().active(),checked=model.selection().getValue().ids().contains(value.track().id().id());
                var check=(android.widget.CheckBox)row.findViewById(R.id.p05_history_checked);check.setVisibility(selecting?View.VISIBLE:View.GONE);check.setChecked(checked);row.setActivated(checked);
                androidx.core.view.ViewCompat.setStateDescription(row,selecting?getString(checked?R.string.gardenia_selected:R.string.p05_not_selected):null);
                return row;
            }
        };
        binding.p0HistoryList.setAdapter(adapter);
        binding.p0HistoryList.setOnItemClickListener((parent,view,index,id)->{long key=rows.get(index).track().id().id();if(model.selection().getValue().active())model.toggle(key);else model.open(key);});
        binding.p0HistoryList.setOnItemLongClickListener((parent,view,index,id)->{if(!model.busy())model.toggle(rows.get(index).track().id().id());return true;});
        binding.p05BatchSelectAll.setOnClickListener(v->model.selectAllLoaded());binding.p05BatchCancel.setOnClickListener(v->model.exitSelection());
        binding.p05BatchShare.setOnClickListener(v->{var ids=model.selection().getValue().ids();if(ids.size()==1)model.exportSnapshot(ids,false,null);else P05BatchDialog.create(array(ids),false).show(getSupportFragmentManager(),"p05-batch-dialog");});
        binding.p05BatchExport.setOnClickListener(v->saveSelection(model.selection().getValue().ids()));
        binding.p05BatchDelete.setOnClickListener(v->P05BatchDialog.create(array(model.selection().getValue().ids()),true).show(getSupportFragmentManager(),"p05-batch-dialog"));
        getSupportFragmentManager().setFragmentResultListener("p05-batch-action",this,(key,result)->{var ids=list(result.getLongArray("ids"));if("delete".equals(result.getString("action")))model.deleteSnapshot(ids);else model.exportSnapshot(ids,"zip".equals(result.getString("action")),null);});
        binding.p0LoadMore.setOnClickListener(view->model.more());
        binding.p0SaveGpx.setOnClickListener(view->saveGpx());
        binding.p03ExportOptions.setOnClickListener(view->new GpxExportDialog().show(getSupportFragmentManager(),"gardenia-gpx-export"));
        getSupportFragmentManager().setFragmentResultListener("gardenia-gpx-action",this,(key,result)->{if(result.getInt("action")==0)saveGpx();else model.exportSelected(null);});
        model.chartPoints().observe(this,points->{binding.p03HistoryChart.setHeartRateOnly();binding.p03HistoryChart.setChartByDistance(false);binding.p03HistoryChart.setRawHeartRatePoints(points);binding.p03HistoryChart.setContentDescription(getString(points.isEmpty()?R.string.gardenia_chart_empty:R.string.gardenia_chart_summary,points.size()));});
        binding.p0ShareGpx.setOnClickListener(view->model.exportSelected(null));
        binding.p0DeleteWorkout.setOnClickListener(view->{if(model.selectedId()!=null)P05BatchDialog.create(new long[]{model.selectedId()},true).show(getSupportFragmentManager(),"p05-batch-dialog");});
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed(){if(model.busy()){android.widget.Toast.makeText(P0HistoryActivity.this,R.string.p05_please_wait,android.widget.Toast.LENGTH_SHORT).show();return;}if(model.selection().getValue().active())model.exitSelection();else if(model.selectedId()!=null)model.open(null);else{setEnabled(false);getOnBackPressedDispatcher().onBackPressed();}}
        });
        if(saved!=null){pendingIds=saved.getLongArray(PENDING);pendingZip=saved.getBoolean(PENDING_ZIP);long[] restored=saved.getLongArray(CHOSEN);model.restoreSelection(saved.getBoolean(MODE),restored==null?new long[0]:restored);}
        else if(getIntent().getBooleanExtra(MANAGEMENT,false))model.manage();
        model.operationError().observe(this,value->render(model.state().getValue()));model.state().observe(this,this::render);model.selection().observe(this,value->render(model.state().getValue()));model.operation().observe(this,value->render(model.state().getValue()));
        model.exported().observe(this,result->{
            if(result==null)return;model.consumeExport();
            android.widget.Toast.makeText(this,result.message(),android.widget.Toast.LENGTH_LONG).show();
            if(result.share()!=null)try{startActivity(Intent.createChooser(result.share(),getString(R.string.p05_share)));}
            catch(android.content.ActivityNotFoundException unavailable){android.widget.Toast.makeText(this,R.string.p0_export_failed,android.widget.Toast.LENGTH_LONG).show();}
        });
        long selected=saved!=null?saved.getLong(WORKOUT_ID,0):getIntent().getLongExtra(WORKOUT_ID,0);
        model.open(selected>0?selected:null);
    }
    @Override public boolean onCreateOptionsMenu(android.view.Menu menu){
        if(model!=null&&model.selectedId()!=null)menu.add(0,R.id.p06_map_container,0,R.string.p06_offline_map).setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }
    @Override public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item){
        if(item.getItemId()==R.id.p06_map_container&&model.selectedId()!=null){startActivity(de.dennisguse.opentracks.p0.map.P06MapActivity.intent(this,model.selectedId()));return true;}
        return super.onOptionsItemSelected(item);
    }
    @Override protected void onStart(){super.onStart();model.refresh();}
    @Override protected void onSaveInstanceState(@NonNull Bundle out){out.putLong(WORKOUT_ID,model.selectedId()==null?0:model.selectedId());out.putBoolean(MODE,model.selection().getValue().active());out.putLongArray(CHOSEN,array(model.selection().getValue().ids()));out.putLongArray(PENDING,pendingIds);out.putBoolean(PENDING_ZIP,pendingZip);super.onSaveInstanceState(out);}
    private void saveGpx(){if(model.selectedId()!=null)saveSelection(java.util.List.of(model.selectedId()));}
    private void saveSelection(java.util.List<Long> ids){if(ids.isEmpty()||pendingIds!=null||model.busy())return;pendingIds=array(ids);pendingZip=ids.size()>1;if(pendingZip)zipDocument.launch("RunnerGardenia-Export.zip");else document.launch("RunnerGardenia-"+ids.get(0)+".gpx");render(model.state().getValue());}
    private void render(P0HistoryViewModel.State state) {
        boolean detail=model.selectedId()!=null;boolean selecting=!detail&&model.selection().getValue().active(),busy=model.busy();int selectedCount=model.selection().getValue().ids().size();boolean actionable=selectedCount>0&&!busy&&!state.loading()&&pendingIds==null;
        binding.p05BatchToolbar.setVisibility(selecting?View.VISIBLE:View.GONE);binding.p05SelectedCount.setText(getString(R.string.p05_selected_count,selectedCount));
        binding.p05BatchShare.setEnabled(actionable);binding.p05BatchExport.setEnabled(actionable);binding.p05BatchDelete.setEnabled(actionable);binding.p05BatchSelectAll.setEnabled(!busy&&!state.loading()&&!state.rows().isEmpty());binding.p05BatchCancel.setEnabled(!busy);
        for(int id:new int[]{R.id.p04_nav_run,R.id.p0_all_history,R.id.p04_nav_heart,R.id.p03_settings})findViewById(id).setEnabled(!busy);
        if(getSupportActionBar()!=null)getSupportActionBar().setTitle(detail?R.string.p0_workout:R.string.p0_history);
        binding.p0HistoryRows.setVisibility(detail?View.GONE:View.VISIBLE);binding.p0WorkoutPane.setVisibility(detail?View.VISIBLE:View.GONE);invalidateOptionsMenu();
        String message=busy?getString(model.operation().getValue().packing()?R.string.p05_packing:R.string.p05_processing,model.operation().getValue().done(),model.operation().getValue().total()):model.operationError().getValue()!=null?model.operationError().getValue():state.error()!=null?state.error():state.loading()?getString(R.string.p0_loading):!detail&&state.rows().isEmpty()?getString(R.string.p0_no_workouts):null;
        binding.p0HistoryStatus.setText(message);binding.p0HistoryStatus.setVisibility(message==null?View.GONE:View.VISIBLE);
        rows=state.rows();adapter.notifyDataSetChanged();binding.p0HistoryList.setEnabled(!state.loading()&&!busy);
        binding.p0LoadMore.setVisibility(!detail&&state.more()?View.VISIBLE:View.GONE);binding.p0LoadMore.setEnabled(!state.loading()&&!busy);
        WorkoutSummary selected=state.selected();
        binding.p03ExportOptions.setEnabled(selected!=null&&!state.loading()&&!busy&&pendingIds==null&&!state.protectedWorkout());
        binding.p0DeleteWorkout.setEnabled(selected!=null&&!state.loading()&&!busy&&pendingIds==null&&!state.protectedWorkout());
        binding.p0SaveGpx.setEnabled(selected!=null&&!state.loading()&&!busy&&pendingIds==null&&!state.protectedWorkout());
        binding.p0ShareGpx.setEnabled(selected!=null&&!state.loading()&&!busy&&pendingIds==null&&!state.protectedWorkout());
        binding.p0WorkoutProtected.setVisibility(detail&&state.protectedWorkout()?View.VISIBLE:View.GONE);
        if(!detail||selected==null){binding.p0WorkoutDetails.setText("");return;}
        var track=selected.track();var stats=track.statistics();
        String identity=getString(R.string.p0_workout_identity,track.name(),getString(track.activityType().getLocalizedStringId()),date(stats.startTime()),date(stats.stopTime()),track.id().id());
        String motion=getString(R.string.p0_workout_motion,MotionFormat.duration(stats.totalDuration()),MotionFormat.duration(stats.movingDuration()),stats.totalDistance().toM(),MotionFormat.speed(stats.getAverageSpeed().toMPS()),MotionFormat.speed(stats.maxSpeed().toMPS()),MotionFormat.pace(stats.getAverageSpeed().toMPS()));
        int source=switch(selected.heartRateOrigin()){case RAW_NOTIFICATIONS->R.string.p0_hr_raw;case LEGACY_TRACK_POINTS->R.string.p0_hr_legacy;case NONE->R.string.p0_hr_none;};
        var heart=selected.heartRate();
        String summary=heart.count()==0?getString(source):getString(R.string.p0_workout_heart,getString(source),heart.count(),heart.minimum(),heart.average(),heart.maximum());
        binding.p0WorkoutDetails.setText(identity+"\n\n"+motion+"\n\n"+summary);
    }
    @NonNull @Override protected View createRootView(){binding=P0HistoryBinding.inflate(getLayoutInflater());return binding.getRoot();}
}
