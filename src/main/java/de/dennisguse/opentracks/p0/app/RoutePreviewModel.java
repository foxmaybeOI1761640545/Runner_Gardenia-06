package de.dennisguse.opentracks.p0.app;

import android.app.Application;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import de.dennisguse.opentracks.data.CustomContentProvider;

/** Incremental UI-only reader. The existing recorder remains the sole writer/owner. */
public final class RoutePreviewModel extends AndroidViewModel {
    public record Result(List<RouteGeometry.Point> points,boolean partial,boolean failed) {}
    private final MutableLiveData<Result> route=new MutableLiveData<>(new Result(List.of(),false,false));
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private Long selected,loaded;private long lastId,lastTime=-1;private boolean busy,closed,gap=true,partial;
    private final List<RouteGeometry.Point> points=new ArrayList<>();
    public RoutePreviewModel(Application app){super(app);}
    public LiveData<Result> route(){return route;}
    public void refresh(Long track){
        boolean changed=!Objects.equals(selected,track);selected=track;
        if(changed&&!closed)route.setValue(new Result(List.of(),false,false));
        if(busy||closed)return;
        if(track==null){route.setValue(new Result(List.of(),false,false));return;}
        busy=true;worker.execute(()->{
            Result result;
            try {
                if(!Objects.equals(loaded,track)){loaded=track;lastId=0;lastTime=-1;points.clear();gap=true;partial=false;}
                try(Cursor c=getApplication().getContentResolver().query(CustomContentProvider.UrlType.TRACKPOINTS.getUri(),new String[]{"_id","type","time","latitude","longitude"},"trackid=? AND _id>?",new String[]{track.toString(),Long.toString(lastId)},"_id")){
                    if(c==null)throw new IllegalStateException("Route cursor unavailable");
                    while(c.moveToNext()){
                        lastId=c.getLong(0);int type=c.getInt(1);long time=c.getLong(2);
                        if(type!=0){if(type==-2||type==-1||type==1)gap=true;continue;}
                        if(c.isNull(3)||c.isNull(4)){gap=true;continue;}
                        RouteGeometry.Point p=new RouteGeometry.Point(c.getDouble(3)/1E6,c.getDouble(4)/1E6,gap||lastTime<0||time-lastTime>15000);
                        if(!p.valid()){gap=true;continue;}points.add(p);lastTime=time;gap=false;
                        if(points.size()>16000){List<RouteGeometry.Point> reduced=RouteGeometry.bounded(points,4000);points.clear();points.addAll(reduced);partial=true;}
                    }
                }
                result=new Result(RouteGeometry.bounded(points,2000),partial,false);
            }catch(RuntimeException error){android.util.Log.w("GardeniaRoute","Cannot read recorded route",error);result=new Result(List.of(),false,true);}
            Result completed=result;main.post(()->{busy=false;if(closed)return;if(Objects.equals(selected,track))route.setValue(completed);else refresh(selected);});
        });
    }
    @Override protected void onCleared(){closed=true;worker.shutdownNow();super.onCleared();}
}
