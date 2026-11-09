package de.dennisguse.opentracks.p0.app;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.p0.data.WorkoutRepository;
import de.dennisguse.opentracks.p0.data.WorkoutSummary;
import de.dennisguse.opentracks.p0.export.P0GpxFiles;

/** One history worker. Query generations never invalidate a confirmed operation or its result. */
public class P0HistoryViewModel extends AndroidViewModel {
    public record State(List<WorkoutSummary> rows,WorkoutSummary selected,boolean protectedWorkout,boolean loading,String error,boolean more) {}
    public record ExportResult(android.content.Intent share,String message) {}
    public record Selection(boolean active,List<Long> ids){public Selection{ids=List.copyOf(ids);}}
    public record Operation(boolean busy,int done,int total,boolean packing) {}
    private final MutableLiveData<ExportResult> exported=new MutableLiveData<>();
    private final MutableLiveData<String> operationError=new MutableLiveData<>();
    private final MutableLiveData<State> state=new MutableLiveData<>(new State(List.of(),null,false,true,null,false));
    private final MutableLiveData<Selection> selection=new MutableLiveData<>(new Selection(false,List.of()));
    private final MutableLiveData<Operation> operation=new MutableLiveData<>(new Operation(false,0,0,false));
    private final LinkedHashSet<Long> chosen=new LinkedHashSet<>();
    private final WorkoutRepository repository;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final de.dennisguse.opentracks.chart.HeartRateChartHistory chartHistory=new de.dennisguse.opentracks.chart.HeartRateChartHistory();
    private final MutableLiveData<List<de.dennisguse.opentracks.chart.HeartRatePlot.Point>> chartPoints=new MutableLiveData<>(List.of());
    public LiveData<List<de.dennisguse.opentracks.chart.HeartRatePlot.Point>> chartPoints(){return chartPoints;}
    private Long selectedId;
    private int limit=50,generation,operationToken;
    private volatile boolean closed;
    public P0HistoryViewModel(Application app){super(app);repository=new WorkoutRepository(app);}
    public LiveData<State> state(){return state;}
    public LiveData<ExportResult> exported(){return exported;}
    public LiveData<String> operationError(){return operationError;}
    public LiveData<Selection> selection(){return selection;}
    public LiveData<Operation> operation(){return operation;}
    public boolean busy(){return operation.getValue().busy();}
    public void consumeExport(){exported.setValue(null);}
    public Long selectedId(){return selectedId;}
    public void open(Long id){selectedId=id!=null&&id>0?id:null;if(selectedId!=null)exitSelection();refresh();}
    public void more(){limit=Math.addExact(limit,50);refresh();}
    public void restoreSelection(boolean active,long[] ids){chosen.clear();for(long id:ids)if(id>0)chosen.add(id);publishSelection(active);}
    public void manage(){publishSelection(true);}
    public void toggle(long id){if(busy())return;if(chosen.contains(id))chosen.remove(id);else chosen.add(id);publishSelection(true);}
    public void selectAllLoaded(){if(busy())return;for(var row:state.getValue().rows())chosen.add(row.track().id().id());publishSelection(true);}
    public void exitSelection(){chosen.clear();publishSelection(false);}
    private void publishSelection(boolean active){selection.setValue(new Selection(active,new ArrayList<>(chosen)));}
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
    private boolean begin(int total){if(closed||busy()||total==0)return false;operationError.setValue(null);operation.setValue(new Operation(true,0,total,false));operationToken++;return true;}
    private String reason(Throwable failure){return failure.getMessage()==null?getApplication().getString(R.string.p0_operation_failed):failure.getMessage();}
    private void validate(long id){
        if(Thread.currentThread().isInterrupted())throw new IllegalStateException(getApplication().getString(R.string.p05_interrupted));
        var key=new Track.Id(id);
        if(repository.find(key)==null)throw new IllegalStateException(getApplication().getString(R.string.p0_workout_missing));
        if(repository.isProtected(key))throw new IllegalStateException(getApplication().getString(R.string.p0_workout_protected));
    }
    public void deleteSelected(){if(selectedId!=null&&!state.getValue().loading())deleteSnapshot(List.of(selectedId));}
    public void deleteSnapshot(List<Long> selected){
        List<Long> ids=List.copyOf(selected);if(new HashSet<>(ids).size()!=ids.size())throw new IllegalArgumentException("Duplicate IDs");if(!begin(ids.size()))return;
        int token=operationToken;
        worker.execute(()->{
            List<Long> deleted=new ArrayList<>();Long current=null;String error=null;
            try{
                // A protected/missing member rejects the whole set before any deletion.
                for(long id:ids){current=id;validate(id);}
                for(long id:ids){
                    current=id;validate(id);repository.delete(new Track.Id(id));
                    if(repository.find(new Track.Id(id))!=null)throw new IllegalStateException(getApplication().getString(R.string.p05_delete_failed));
                    deleted.add(id);int done=deleted.size();main.post(()->{if(!closed&&token==operationToken)operation.setValue(new Operation(true,done,ids.size(),false));});
                }
            }catch(RuntimeException failure){error=getApplication().getString(R.string.p05_record_error,current,reason(failure));}
            boolean failed=error!=null;String message=error==null?getApplication().getString(R.string.p05_deleted,deleted.size()):getApplication().getString(R.string.p05_delete_partial,deleted.size(),ids.size(),error);
            main.post(()->{if(!closed&&token==operationToken){chosen.removeAll(deleted);if(selectedId!=null&&deleted.contains(selectedId))selectedId=null;publishSelection(selection.getValue().active()&&!chosen.isEmpty());operation.setValue(new Operation(false,deleted.size(),ids.size(),false));refresh();if(failed)operationError.setValue(message);exported.setValue(new ExportResult(null,message));}});
        });
    }
    public void exportSelected(Uri destination){if(selectedId!=null&&!state.getValue().loading()&&!state.getValue().protectedWorkout())exportSnapshot(List.of(selectedId),false,destination);}
    public void exportSnapshot(List<Long> selected,boolean zip,Uri destination){
        List<Long> ids=List.copyOf(selected);if(!begin(ids.size()))return;int token=operationToken;
        worker.execute(()->{
            P0GpxFiles.Batch batch=null;boolean keep=false;android.content.Intent share=null;String message;int finished=0;
            try{
                P0GpxFiles files=new P0GpxFiles(getApplication());List<Track.Id> keys=ids.stream().map(Track.Id::new).toList();
                batch=files.prepareBatch(keys,zip,(done,total,packing)->main.post(()->{if(!closed&&token==operationToken)operation.setValue(new Operation(true,done,total,packing));}));
                if(destination==null){share=files.share(batch);keep=true;message=getApplication().getString(R.string.p05_share_ready,ids.size());}
                else {files.save(zip?batch.zip():batch.gpx().get(0),destination);message=getApplication().getString(R.string.p05_saved,ids.size());}
                finished=ids.size();
            }catch(java.io.InterruptedIOException failure){message=getApplication().getString(R.string.p05_interrupted);}
            catch(P0GpxFiles.BatchFailure failure){message=getApplication().getString(R.string.p05_record_error,failure.workoutId==null?"—":failure.workoutId,reason(failure));}
            catch(java.io.IOException|RuntimeException failure){message=destination==null?getApplication().getString(R.string.p0_export_failed):getApplication().getString(R.string.p05_document_failed);}
            finally{if(batch!=null&&(!keep||closed))batch.close();}
            ExportResult result=new ExportResult(share,message);int done=finished;
            main.post(()->{if(!closed&&token==operationToken){operation.setValue(new Operation(false,done,ids.size(),false));if(done!=ids.size())operationError.setValue(result.message());exported.setValue(result);}});
        });
    }
    @Override protected void onCleared(){closed=true;generation++;operationToken++;worker.shutdownNow();super.onCleared();}
}
