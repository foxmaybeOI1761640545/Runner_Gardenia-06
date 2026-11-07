package de.dennisguse.opentracks.p0.app;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.p0.data.WorkoutRepository;
import de.dennisguse.opentracks.p0.data.WorkoutSummary;

/** Worker-only history queries and deletion. Activity renders immutable projections. */
public class P0HistoryViewModel extends AndroidViewModel {
    public record State(List<WorkoutSummary> rows,WorkoutSummary selected,boolean protectedWorkout,boolean loading,String error,boolean more) {}
    public record ExportResult(android.content.Intent share,String message) {}
    private final MutableLiveData<ExportResult> exported=new MutableLiveData<>();
    private final MutableLiveData<State> state=new MutableLiveData<>(new State(List.of(),null,false,true,null,false));
    private final WorkoutRepository repository;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final de.dennisguse.opentracks.chart.HeartRateChartHistory chartHistory=new de.dennisguse.opentracks.chart.HeartRateChartHistory();
    private final MutableLiveData<List<de.dennisguse.opentracks.chart.HeartRatePlot.Point>> chartPoints=new MutableLiveData<>(List.of());
    public LiveData<List<de.dennisguse.opentracks.chart.HeartRatePlot.Point>> chartPoints(){return chartPoints;}
    private Long selectedId;
    private int limit=50,generation;
    private volatile boolean closed;
    public P0HistoryViewModel(Application app){super(app);repository=new WorkoutRepository(app);}
    public LiveData<State> state(){return state;}
    public LiveData<ExportResult> exported(){return exported;}
    public void consumeExport(){exported.setValue(null);}
    public Long selectedId(){return selectedId;}
    public void open(Long id){selectedId=id!=null&&id>0?id:null;refresh();}
    public void more(){limit=Math.addExact(limit,50);refresh();}
    public void refresh() {
        if(closed)return;
        int token=++generation,window=limit;Long id=selectedId;
        State previous=state.getValue();state.setValue(new State(previous.rows(),previous.selected(),previous.protectedWorkout(),true,null,previous.more()));
        worker.execute(()->{
            try {
                List<WorkoutSummary> rows=id==null?repository.listNewest(window):List.of();
                WorkoutSummary selected=id==null?null:repository.find(new Track.Id(id));
                var points=selected==null?List.<de.dennisguse.opentracks.chart.HeartRatePlot.Point>of():chartHistory.load(getApplication(),selected.track().id()).points();
                boolean locked=selected!=null&&repository.isProtected(selected.track().id());
                String error=id!=null&&selected==null?getApplication().getString(R.string.p0_workout_missing):null;
                main.post(()->{if(!closed&&token==generation){chartPoints.setValue(points);state.setValue(new State(rows,selected,locked,false,error,id==null&&rows.size()==window));}});
            }catch(RuntimeException failure){main.post(()->{if(!closed&&token==generation)state.setValue(new State(List.of(),null,false,false,getApplication().getString(R.string.p0_operation_failed),false));});}
        });
    }
    public void deleteSelected() {
        if(selectedId==null||state.getValue().loading())return;
        long id=selectedId;int token=++generation;
        State previous=state.getValue();state.setValue(new State(previous.rows(),previous.selected(),previous.protectedWorkout(),true,null,false));
        worker.execute(()->{
            try {
                repository.delete(new Track.Id(id));
                main.post(()->{if(!closed&&token==generation){selectedId=null;refresh();}});
            }catch(RuntimeException failure){
                boolean locked=repository.isProtected(new Track.Id(id));
                main.post(()->{if(!closed&&token==generation)state.setValue(new State(previous.rows(),previous.selected(),locked,false,getApplication().getString(R.string.p0_operation_failed),false));});
            }
        });
    }
    public void exportSelected(android.net.Uri destination) {
        if(selectedId==null||state.getValue().loading()||state.getValue().protectedWorkout())return;
        long id=selectedId;int token=++generation;
        State previous=state.getValue();state.setValue(new State(previous.rows(),previous.selected(),previous.protectedWorkout(),true,null,false));
        worker.execute(()->{
            java.io.File file=null;android.content.Intent share=null;boolean shared=false;String message;
            try {
                var files=new de.dennisguse.opentracks.p0.export.P0GpxFiles(getApplication());
                file=files.prepare(new Track.Id(id));
                if(destination==null){share=files.share(file);shared=true;message=getApplication().getString(R.string.p0_export_share_ready);}
                else {files.save(file,destination);message=getApplication().getString(R.string.p0_export_saved);}
            }catch(java.io.IOException|RuntimeException failure){message=getApplication().getString(R.string.p0_export_failed);}
            finally {if(file!=null&&!shared)file.delete();}
            ExportResult result=new ExportResult(share,message);
            main.post(()->{if(!closed&&token==generation){state.setValue(new State(previous.rows(),previous.selected(),previous.protectedWorkout(),false,null,previous.more()));exported.setValue(result);}});
        });
    }
    @Override protected void onCleared(){closed=true;generation++;worker.shutdownNow();super.onCleared();}
}
