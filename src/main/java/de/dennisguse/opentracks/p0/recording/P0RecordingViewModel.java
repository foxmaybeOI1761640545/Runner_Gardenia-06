package de.dennisguse.opentracks.p0.recording;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.data.*;
import de.dennisguse.opentracks.sensors.*;
import de.dennisguse.opentracks.services.*;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/** Application-context gateway. Unbinding a screen never stops a started recording service. */
public class P0RecordingViewModel extends AndroidViewModel {
    public record Snapshot(RecordingSession session,RecordingData data,GpsStatusValue gps,Position position,HeartRatePreview preview,String error,boolean busy) {}
    private final MutableLiveData<Snapshot> snapshot=new MutableLiveData<>();
    private final MutableLiveData<List<WorkoutSummary>> history=new MutableLiveData<>(List.of());
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final WorkoutRepository repository;
    private final SessionStore store;
    private RecordingSession session;
    private RecordingData data=RecordingData.NOT_RECORDING;
    private GpsStatusValue gps=GpsStatusValue.GPS_NONE;
    private String error;
    private boolean attached,closed,busy;
    private TrackRecordingService observed;
    private ActivityType pendingSport;
    private boolean pendingResume;
    private final TrackRecordingServiceConnection connection=new TrackRecordingServiceConnection((service,self)->observe(service));
    private final Observer<RecordingData> dataObserver=value->{if(value!=null){data=value;emit();}};
    private final Observer<GpsStatusValue> gpsObserver=value->{gps=value;emit();};
    private final Observer<RecordingStatus> statusObserver=this::onStatus;
    private void onStatus(RecordingStatus value) {
        if(value!=null&&value.isRecording()) {
            session=RecordingSession.recover(value.trackId().id(),true);store.save(session);
        } else if(session.phase()==RecordingSession.Phase.RECORDING) {
            session=session.finish();store.save(session);reloadHistory();
        }
        emit();
    }
    private final Runnable previewTick=new Runnable() {
        @Override public void run() {if(attached&&!closed){emit();handler.postDelayed(this,1000);}}
    };

    public P0RecordingViewModel(Application application) {
        super(application);repository=new WorkoutRepository(application);store=new SessionStore(application);
        session=store.recover();emit();reloadHistory();
    }
    public LiveData<Snapshot> snapshot() {return snapshot;}
    public LiveData<List<WorkoutSummary>> history() {return history;}
    public void attach() {
        if(attached)return;attached=true;connection.bindWithStart(getApplication());
        handler.post(previewTick);reloadHistory();
    }
    public void detach() {
        attached=false;handler.removeCallbacks(previewTick);unobserve();connection.unbind(getApplication());
    }
    private void observe(TrackRecordingService service) {
        if(!attached||closed){connection.unbind(getApplication());return;}
        unobserve();observed=service;
        service.getRecordingStatusObservable().observeForever(statusObserver);
        service.getRecordingDataObservable().observeForever(dataObserver);
        service.getGpsStatusObservable().observeForever(gpsObserver);
        emit();
    }
    private void unobserve() {
        if(observed==null)return;
        observed.getRecordingStatusObservable().removeObserver(statusObserver);
        observed.getRecordingDataObservable().removeObserver(dataObserver);
        observed.getGpsStatusObservable().removeObserver(gpsObserver);observed=null;
    }
    public void pendingStart(ActivityType type) {pendingSport=type;pendingResume=false;}
    public void pendingResume() {pendingSport=null;pendingResume=true;}
    public void completePermission(boolean granted) {
        ActivityType type=pendingSport;boolean resume=pendingResume;pendingSport=null;pendingResume=false;
        if(!granted){error=getApplication().getString(R.string.p0_location_needed);emit();return;}
        if(resume)resume();else if(type!=null)start(type);
    }
    public void start(ActivityType type) {
        if(session.phase()==RecordingSession.Phase.RECORDING||session.phase()==RecordingSession.Phase.PAUSED)return;
        command(true,service->{
            configureGps();PreferencesUtils.setDefaultActivityLocalized(getApplication().getString(type.getLocalizedStringId()));
            Track.Id id=service.isRecording()?service.getRecordingStatus().trackId():service.startNewTrack();
            if(id==null)throw new IllegalStateException("GPS permission unavailable");
            session=RecordingSession.recover(id.id(),true);store.save(session);reloadHistory();
        });
    }
    private void configureGps() {
        // Both are existing driver/filter settings: one-second requests, retain each valid GPS fix.
        PreferencesUtils.setString(R.string.settings_sensor_gps_key,de.dennisguse.opentracks.sensors.driver.SensorType.INTERNAL.getPreferenceValue());
        PreferencesUtils.setString(R.string.min_sampling_interval_key,"1");
        PreferencesUtils.setString(R.string.recording_distance_interval_key,"0");
    }
    public void pause() {
        if(session.phase()!=RecordingSession.Phase.RECORDING)return;
        command(false,service->{session=session.pause();service.endCurrentTrack();service.stopIdleSensors();store.save(session);reloadHistory();});
    }
    public void resume() {
        if(session.phase()!=RecordingSession.Phase.PAUSED)return;
        command(true,service->{
            configureGps();Track.Id id=new Track.Id(session.trackId());
            if(!service.isRecording())service.resumeTrack(id);
            if(!service.isRecording())throw new IllegalStateException("Recording no longer exists");
            session=session.resume();store.save(session);reloadHistory();
        });
    }
    public void finish() {
        if(session.phase()!=RecordingSession.Phase.RECORDING&&session.phase()!=RecordingSession.Phase.PAUSED)return;
        command(false,service->{
            session=session.finish();service.endCurrentTrack();service.stopIdleSensors();store.save(session);reloadHistory();
        });
    }
    public void previewHeartRate(boolean reconnect) {
        if(!de.dennisguse.opentracks.p0.ble.HeartRateCommands.hasSelection())return;
        command(true,service->{
            boolean alreadyStarted=service.areSensorsStarted();
            boolean wasEnabled=de.dennisguse.opentracks.p0.ble.HeartRateCommands.connectionEnabled(getApplication());
            de.dennisguse.opentracks.p0.ble.HeartRateCommands.setConnectionEnabled(getApplication(),true);
            service.tryStartSensors();
            if(reconnect&&alreadyStarted&&wasEnabled)de.dennisguse.opentracks.p0.ble.HeartRateCommands.reconnect(getApplication());
        });
    }
    public void disconnectHeartRate() {
        command(false,service->{
            try {de.dennisguse.opentracks.p0.ble.HeartRateCommands.setConnectionEnabled(getApplication(),false);}
            finally {
                de.dennisguse.opentracks.p0.ble.HeartRateCommands.disconnect(getApplication());
                service.stopIdleSensors();
            }
        });
    }
    public void setHeartRateAutoReconnect(boolean enabled) {
        try {de.dennisguse.opentracks.p0.ble.HeartRateCommands.setAutoReconnect(getApplication(),enabled);}
        catch(RuntimeException failure){reportError(failure);emit();}
    }
    private void command(boolean foreground,Consumer<TrackRecordingService> action) {
        if(busy||closed)return;busy=true;error=null;emit();
        TrackRecordingServiceConnection.Callback callback=(service,self)->{
            try {action.accept(service);}catch(RuntimeException failure){reportError(failure);}
            finally {busy=false;emit();}
        };
        try {
            if(foreground)TrackRecordingServiceConnection.executeForeground(getApplication(),callback);
            else TrackRecordingServiceConnection.execute(getApplication(),callback);
        } catch(RuntimeException failure){busy=false;reportError(failure);emit();}
    }
    public void reloadHistory() {
        if(closed)return;
        Long id=session.trackId();boolean finalSnapshot=session.phase()!=RecordingSession.Phase.RECORDING;
        worker.execute(()->{
            try {
                List<WorkoutSummary> rows=repository.listNewest(5);history.postValue(rows);
                if(id!=null&&finalSnapshot) {
                    WorkoutSummary saved=repository.find(new Track.Id(id));
                    handler.post(()->{if(!closed&&saved!=null&&session.trackId()!=null&&session.trackId().equals(id)&&session.phase()!=RecordingSession.Phase.RECORDING){data=new RecordingData(saved.track(),null,null,null);emit();}});
                }
            } catch(RuntimeException failure){handler.post(()->{if(!closed){reportError(failure);emit();}});}
        });
    }
    private void reportError(RuntimeException failure) {
        android.util.Log.e("P0Recording","Operation failed",failure);
        error=getApplication().getString(de.dennisguse.opentracks.R.string.p0_operation_failed);
    }
    private void emit() {
        HeartRatePreview preview=observed!=null?observed.getHeartRatePreview():new HeartRatePreview(null,HeartRateStatus.disconnected());
        snapshot.setValue(new Snapshot(session,data,gps,observed!=null?observed.getGpsPreview():null,preview,error,busy));
    }
    @Override protected void onCleared() {
        closed=true;detach();worker.shutdownNow();super.onCleared();
    }
}
