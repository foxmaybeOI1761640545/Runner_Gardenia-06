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
    private final androidx.activity.result.ActivityResultLauncher<String> document=registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/gpx+xml"),uri->{if(uri!=null)model.exportSelected(uri);});
    public static Intent intent(Context context,long id){return new Intent(context,P0HistoryActivity.class).putExtra(WORKOUT_ID,id);}
    private String date(java.time.Instant time){return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(time);}
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);model=new ViewModelProvider(this).get(P0HistoryViewModel.class);
        setSupportActionBar(binding.p0HistoryToolbar);
        if(getSupportActionBar()!=null)getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_2) {
            @Override public int getCount(){return rows.size();}
            @Override public WorkoutSummary getItem(int index){return rows.get(index);}
            @NonNull @Override public View getView(int index,View recycled,@NonNull ViewGroup parent) {
                View row=recycled!=null?recycled:getLayoutInflater().inflate(android.R.layout.simple_list_item_2,parent,false);
                WorkoutSummary value=getItem(index);
                ((TextView)row.findViewById(android.R.id.text1)).setText(value.track().name());
                ((TextView)row.findViewById(android.R.id.text2)).setText(getString(R.string.p0_history_row,date(value.track().statistics().startTime()),value.track().statistics().totalDistance().toM(),MotionFormat.duration(value.track().statistics().totalDuration())));
                return row;
            }
        };
        binding.p0HistoryList.setAdapter(adapter);
        binding.p0HistoryList.setOnItemClickListener((parent,view,index,id)->model.open(rows.get(index).track().id().id()));
        binding.p0LoadMore.setOnClickListener(view->model.more());
        binding.p0SaveGpx.setOnClickListener(view->document.launch("RunnerGardenia-"+model.selectedId()+".gpx"));
        binding.p0ShareGpx.setOnClickListener(view->model.exportSelected(null));
        binding.p0DeleteWorkout.setOnClickListener(view->new AlertDialog.Builder(this).setTitle(R.string.p0_delete_workout)
                .setMessage(R.string.p0_delete_workout_confirm).setNegativeButton(android.R.string.cancel,null)
                .setPositiveButton(R.string.p0_delete_workout,(dialog,which)->model.deleteSelected()).show());
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed(){if(model.selectedId()!=null)model.open(null);else{setEnabled(false);getOnBackPressedDispatcher().onBackPressed();}}
        });
        model.state().observe(this,this::render);
        model.exported().observe(this,result->{
            if(result==null)return;model.consumeExport();
            android.widget.Toast.makeText(this,result.message(),android.widget.Toast.LENGTH_LONG).show();
            if(result.share()!=null)try{startActivity(Intent.createChooser(result.share(),getString(R.string.p0_share_gpx)));}
            catch(android.content.ActivityNotFoundException unavailable){android.widget.Toast.makeText(this,R.string.p0_export_failed,android.widget.Toast.LENGTH_LONG).show();}
        });
        long selected=saved!=null?saved.getLong(WORKOUT_ID,0):getIntent().getLongExtra(WORKOUT_ID,0);
        model.open(selected>0?selected:null);
    }
    @Override protected void onStart(){super.onStart();model.refresh();}
    @Override protected void onSaveInstanceState(@NonNull Bundle out){out.putLong(WORKOUT_ID,model.selectedId()==null?0:model.selectedId());super.onSaveInstanceState(out);}
    private void render(P0HistoryViewModel.State state) {
        boolean detail=model.selectedId()!=null;
        if(getSupportActionBar()!=null)getSupportActionBar().setTitle(detail?R.string.p0_workout:R.string.p0_history);
        binding.p0HistoryRows.setVisibility(detail?View.GONE:View.VISIBLE);binding.p0WorkoutPane.setVisibility(detail?View.VISIBLE:View.GONE);
        String message=state.error()!=null?state.error():state.loading()?getString(R.string.p0_loading):!detail&&state.rows().isEmpty()?getString(R.string.p0_no_workouts):null;
        binding.p0HistoryStatus.setText(message);binding.p0HistoryStatus.setVisibility(message==null?View.GONE:View.VISIBLE);
        rows=state.rows();adapter.notifyDataSetChanged();binding.p0HistoryList.setEnabled(!state.loading());
        binding.p0LoadMore.setVisibility(!detail&&state.more()?View.VISIBLE:View.GONE);binding.p0LoadMore.setEnabled(!state.loading());
        WorkoutSummary selected=state.selected();
        binding.p0DeleteWorkout.setEnabled(selected!=null&&!state.loading()&&!state.protectedWorkout());
        binding.p0SaveGpx.setEnabled(selected!=null&&!state.loading()&&!state.protectedWorkout());
        binding.p0ShareGpx.setEnabled(selected!=null&&!state.loading()&&!state.protectedWorkout());
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
