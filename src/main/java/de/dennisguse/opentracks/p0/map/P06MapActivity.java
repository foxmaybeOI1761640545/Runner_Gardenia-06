package de.dennisguse.opentracks.p0.map;

import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import java.io.*;
import java.util.List;
import java.util.concurrent.*;
import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.P06OfflineMapBinding;
import de.dennisguse.opentracks.p0.app.*;
import de.dennisguse.opentracks.p0.recording.*;
import de.dennisguse.opentracks.services.*;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/** Read-only map screen. All document, index and map-file IO runs on its serial worker. */
public class P06MapActivity extends AbstractActivity {
    private static final String TRACK="p06.track";
    private P06OfflineMapBinding binding;
    private OfflineMapView map;
    private OfflineMapRepository repository;
    private OfflineMapRepository.State maps=new OfflineMapRepository.State(List.of(),null);
    private RoutePreviewModel route;
    private P0RecordingViewModel recording;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private boolean closed,started,busy,gpsWanted,centerPending,initialFitDone;
    private Long trackId;
    private List<RouteGeometry.Point> points=List.of();
    private de.dennisguse.opentracks.data.models.Position position;
    private TrackRecordingService gpsService;
    private final TrackRecordingServiceConnection gpsConnection=new TrackRecordingServiceConnection((service,self)->{gpsService=service;if(started&&gpsWanted)acquireGps();});
    private final androidx.activity.result.ActivityResultLauncher<String[]> document=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{if(uri!=null)importDocument(uri);});
    private final androidx.activity.result.ActivityResultLauncher<String[]> permission=registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),result->{if(hasLocation()){gpsWanted=true;acquireGps();}else message(R.string.p06_map_location_denied);});
    public static Intent intent(Context context,Long track){Intent intent=new Intent(context,P06MapActivity.class);if(track!=null)intent.putExtra(TRACK,track);return intent;}
    @Override protected View createRootView(){binding=P06OfflineMapBinding.inflate(getLayoutInflater());return binding.getRoot();}
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);setSupportActionBar(binding.p06MapToolbar);getSupportActionBar().setTitle(R.string.p06_offline_map);getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        repository=new OfflineMapRepository(this);map=new OfflineMapView(this);binding.p06MapContainer.addView(map,new android.widget.FrameLayout.LayoutParams(-1,-1));
        trackId=getIntent().hasExtra(TRACK)?getIntent().getLongExtra(TRACK,0):null;gpsWanted=saved!=null&&saved.getBoolean("gpsWanted");
        binding.p06ImportMap.setOnClickListener(v->document.launch(new String[]{"*/*"}));
        binding.p06ManageMaps.setOnClickListener(v->manageMaps());
        binding.p06FitTrack.setOnClickListener(v->map.fitRoute());
        binding.p06MyPosition.setOnClickListener(v->{centerPending=true;if(hasLocation()){gpsWanted=true;acquireGps();if(map.centerPosition())centerPending=false;else message(R.string.p06_map_waiting_gps);}else permission.launch(P0PermissionPolicy.location().toArray(new String[0]));});
        route=new ViewModelProvider(this).get(RoutePreviewModel.class);recording=new ViewModelProvider(this).get(P0RecordingViewModel.class);
        route.route().observe(this,result->{points=result.points();map.bind(points,position);binding.p06FitTrack.setEnabled(!points.isEmpty());if(!initialFitDone&&!points.isEmpty()&&map.mapView()!=null){initialFitDone=true;map.post(map::fitRoute);}if(result.failed())message(R.string.p06_map_route_failed);});
        recording.snapshot().observe(this,snapshot->{
            position=snapshot.freshGps()?snapshot.position():null;map.bind(points,position);
            if(centerPending&&map.centerPosition()){centerPending=false;if(maps.current()!=null)binding.p06MapStatus.setText(getString(R.string.p06_map_ready,maps.current().name()));}
            Long id=trackId;
            if(id==null&&(snapshot.session().phase()==RecordingSession.Phase.RECORDING||snapshot.session().phase()==RecordingSession.Phase.PAUSED))id=snapshot.session().trackId();
            route.refresh(id);
        });
        load(null);
    }
    @Override public boolean onCreateOptionsMenu(android.view.Menu menu){menu.add(0,1,0,R.string.p06_map_licenses);return true;}
    @Override public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item){
        if(item.getItemId()==1){String[] files={"Mapsforge-0.25.0-README.md","Mapsforge-LGPL-3.0.txt","Mapsforge-GPL-3.0.txt","AndroidSVG-NOTICE.txt","AndroidSVG-Apache-2.0.txt"};
            new AlertDialog.Builder(this).setTitle(R.string.p06_map_licenses).setItems(files,(dialog,which)->worker.execute(()->{
                try(var input=getAssets().open("p06-licenses/"+files[which]);var output=new ByteArrayOutputStream()){
                    byte[] bytes=new byte[4096];int n;while((n=input.read(bytes))!=-1)output.write(bytes,0,n);String text=output.toString(java.nio.charset.StandardCharsets.UTF_8.name());
                    main.post(()->{if(closed)return;var content=new android.widget.TextView(this);content.setText(text);content.setTextIsSelectable(true);content.setPadding(24,24,24,24);var scroll=new android.widget.ScrollView(this);scroll.addView(content);new AlertDialog.Builder(this).setTitle(files[which]).setView(scroll).setPositiveButton(android.R.string.ok,null).show();});
                }catch(IOException failure){main.post(()->{if(!closed)message(R.string.p0_operation_failed);});}
            })).setNegativeButton(android.R.string.cancel,null).show();return true;}
        return super.onOptionsItemSelected(item);
    }
    private boolean hasLocation(){return de.dennisguse.opentracks.util.PermissionRequester.GPS.hasPermission(this);}
    private void acquireGps(){
        if(!started||!gpsWanted||gpsService==null||!hasLocation())return;
        try{
            PreferencesUtils.setString(R.string.settings_sensor_gps_key,de.dennisguse.opentracks.sensors.driver.SensorType.INTERNAL.getPreferenceValue());
            PreferencesUtils.setString(R.string.min_sampling_interval_key,"1");
            gpsService.acquireMapGps(this);
        }catch(RuntimeException failure){message(R.string.p06_map_location_denied);}
    }
    @Override protected void onStart(){super.onStart();started=true;recording.attach();gpsConnection.bindWithStart(this);}
    @Override protected void onStop(){started=false;if(gpsService!=null)gpsService.releaseMapGps(this);gpsService=null;gpsConnection.unbind(this);recording.detach();super.onStop();}
    @Override protected void onSaveInstanceState(@NonNull Bundle state){state.putBoolean("gpsWanted",gpsWanted);super.onSaveInstanceState(state);}
    @Override protected void onDestroy(){closed=true;map.clear();worker.shutdownNow();super.onDestroy();}
    private void setBusy(boolean value){busy=value;binding.p06ImportMap.setEnabled(!value);binding.p06ManageMaps.setEnabled(!value);}
    private void message(int resource){binding.p06MapStatus.setText(resource);}
    private interface Operation {OfflineMapRepository.State run()throws IOException;}
    private void load(Operation operation){
        if(busy||closed)return;setBusy(true);message(R.string.p06_map_loading);
        worker.execute(()->{
            OfflineMapRepository.State state;org.mapsforge.map.reader.MapFile opened=null;IOException error=null;
            try{state=operation==null?repository.read():operation.run();if(state.current()!=null)opened=repository.open(state.current());}
            catch(IOException failure){state=null;error=failure;}
            var result=state;var file=opened;var problem=error;
            main.post(()->{if(closed){if(file!=null)file.close();return;}setBusy(false);
                if(problem!=null){message(R.string.p06_map_failed);return;}
                maps=result;if(file!=null){map.load(file);map.bind(points,position);if(!initialFitDone&&!points.isEmpty()){initialFitDone=true;map.post(map::fitRoute);}binding.p06MapStatus.setText(getString(R.string.p06_map_ready,result.current().name()));}
                else{map.clear();message(R.string.p06_map_empty);}
            });
        });
    }
    private void importDocument(Uri uri){
        load(()->{
            String name="Local map";
            try(var cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}
            catch(RuntimeException ignored){/* The copy can still succeed when metadata is unavailable. */}
            try(InputStream input=getContentResolver().openInputStream(uri)){if(input==null)throw new IOException("Document unavailable");return repository.importMap(input,name);}
            catch(RuntimeException failure){throw new IOException("Document unreadable",failure);}
        });
    }
    private void manageMaps(){
        if(maps.entries().isEmpty()){message(R.string.p06_map_empty);return;}
        String[] names=maps.entries().stream().map(OfflineMapRepository.Entry::name).toArray(String[]::new);
        new AlertDialog.Builder(this).setTitle(R.string.p06_map_choose).setItems(names,(dialog,which)->{
            var chosen=maps.entries().get(which);
            new AlertDialog.Builder(this).setTitle(chosen.name()).setPositiveButton(R.string.p06_offline_map,(d,w)->load(()->repository.select(chosen.id())))
                    .setNeutralButton(R.string.p06_map_delete,(d,w)->new AlertDialog.Builder(this).setMessage(getString(R.string.p06_map_delete_confirm,chosen.name())).setPositiveButton(R.string.p06_map_delete,(confirm,button)->{map.clear();load(()->repository.delete(chosen.id()));}).setNegativeButton(android.R.string.cancel,null).show())
                    .setNegativeButton(android.R.string.cancel,null).show();
        }).setNegativeButton(android.R.string.cancel,null).show();
    }
}
