package de.dennisguse.opentracks.p0.map;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.widget.FrameLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;
import org.junit.Test;
import org.mapsforge.core.model.LatLong;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.p0.P0UiCapture;
import de.dennisguse.opentracks.p0.app.RouteGeometry;

/** Real 105,834,033-byte file; never substituted by the 751-byte fixture. */
public class P061RealMapTest {
    private final Context context=ApplicationProvider.getApplicationContext();
    private static final String SHA="5412067262dace98d9f95b02621446516e7d98e1debbb6a314d575cae022ee1e";
    private OfflineMapView view(P06MapActivity a){return (OfflineMapView)((FrameLayout)a.findViewById(R.id.p06_map_container)).getChildAt(0);}
    private String shell(String command)throws Exception{
        try(var fd=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);var input=new FileInputStream(fd.getFileDescriptor());var out=new ByteArrayOutputStream()){
            byte[] b=new byte[4096];int n;while((n=input.read(b))!=-1)out.write(b,0,n);return out.toString(java.nio.charset.StandardCharsets.UTF_8);}
    }
    private String hash(File file)throws Exception{
        var hash=MessageDigest.getInstance("SHA-256");try(var input=new FileInputStream(file)){byte[] b=new byte[65536];int n;while((n=input.read(b))!=-1)hash.update(b,0,n);}return HexFormat.of().formatHex(hash.digest());
    }
    private void ready(ActivityScenario<P06MapActivity> screen)throws Exception{
        long until=SystemClock.uptimeMillis()+60000;boolean[] ready={false};
        do{screen.onActivity(a->{var v=view(a);ready[0]=v.mapView()!=null&&v.renderedTileCount()>0;});if(ready[0])return;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<until);
        fail("Actual map must finish importing and render native vector tiles");
    }
    private File privateFile(OfflineMapRepository.State state){return new File(context.getFilesDir(),"offline-maps/"+state.selected()+".map");}
    @Test public void realMapSafImportFlightModeFeaturesRenderRecreateAndIndependentRestart()throws Exception{
        String phase=InstrumentationRegistry.getArguments().getString("p061RealPhase","both");
        assertTrue(List.of("both","write","read").contains(phase));
        String flight=shell("settings get global airplane_mode_on").strip();
        var repository=new OfflineMapRepository(context);var receipt=new JSONObject().put("sourceCommit",InstrumentationRegistry.getArguments().getString("buildCommit"))
                .put("phase",phase).put("pid",android.os.Process.myPid()).put("mapBytes",105834033L).put("mapSha256",SHA);
        try {
            shell("cmd connectivity airplane-mode enable");assertEquals("1",shell("settings get global airplane_mode_on").strip());
            if(!phase.equals("read")){
                for(var entry:repository.read().entries())repository.delete(entry.id());
                long began=SystemClock.uptimeMillis();
                try(var screen=ActivityScenario.launch(P06MapActivity.class)){
                    Intents.init();try{
                        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK,
                            new Intent().setData(Uri.parse("content://com.fox.runner.p06.fixture/real.map")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)));
                        onView(withId(R.id.p06_import_map)).perform(click());
                        long responsive=SystemClock.uptimeMillis();screen.onActivity(a->assertFalse(a.isFinishing()));
                        assertTrue("100MB copy must not block the main looper",SystemClock.uptimeMillis()-responsive<2000);
                        ready(screen);
                    }finally{Intents.release();}
                }
                receipt.put("importMillis",SystemClock.uptimeMillis()-began);
                var state=repository.read();assertEquals(105834033L,state.current().bytes());assertEquals(SHA,hash(privateFile(state)));
                // Remove the provider's external source copy: read phase can use only the private file.
                File provider=new File(InstrumentationRegistry.getInstrumentation().getContext().getCacheDir(),"p06-provider-valid");
                assertTrue(!provider.exists()||provider.delete());
                // A partial/corrupt import must leave the selected complete file untouched.
                try{repository.importMap(new ByteArrayInputStream(new byte[80]),"broken.map");fail("Corrupt map accepted");}catch(IOException expected){}
                assertEquals(state.selected(),repository.read().selected());
                try{repository.importMap(new InputStream(){int n;public int read()throws IOException{if(n++>70000)throw new InterruptedIOException("Synthetic interrupted source");return 0;}},"interrupted.map");fail("Interrupted map accepted");}catch(IOException expected){}
                assertEquals(state.selected(),repository.read().selected());
                assertEquals(0,new File(context.getFilesDir(),"offline-maps").listFiles((d,n)->n.endsWith(".pending")).length);
            }
            if(!phase.equals("write")){
                var state=repository.read();assertNotNull(state.current());assertEquals(105834033L,state.current().bytes());assertEquals(SHA,hash(privateFile(state)));
                JSONArray captures=new JSONArray();
                String[] names={"Guangzhou","Foshan","Zhuhai","Shenzhen","Dongguan","Zhongshan","Jiangmen","Huizhou","Zhaoqing","Shanwei","Jieyang","Shantou","Chaozhou","Zhangzhou","Longhai","Zhangpu","Dongshan"};
                double[][] p={{23.1291,113.2644},{23.0218,113.1219},{22.271,113.5767},{22.5431,114.0579},{23.0207,113.7518},{22.5159,113.3926},{22.5787,113.0815},{23.1115,114.4152},{23.0469,112.4651},{22.7876,115.3753},{23.5438,116.3557},{23.3535,116.6822},{23.6567,116.6226},{24.513,117.6471},{24.446,117.818},{24.117,117.613},{23.701,117.429}};
                try(var screen=ActivityScenario.launch(P06MapActivity.class)){
                    ready(screen);
                    for(int i=0;i<p.length;i++)for(byte zoom:new byte[]{7,12,16}){
                        final int index=i;final byte z=zoom;
                        screen.onActivity(a->{var map=view(a).mapView();map.setCenter(new LatLong(p[index][0],p[index][1]));map.setZoomLevel(z);});
                        long until=SystemClock.uptimeMillis()+45000;boolean[] cached={false};OfflineMapView.DiagnosticViewport[] viewport={null};
                        do{screen.onActivity(a->{viewport[0]=view(a).diagnosticViewport();cached[0]=viewport[0].tiles().stream().anyMatch(OfflineMapView.TileState::cached);});if(cached[0])break;SystemClock.sleep(150);}while(SystemClock.uptimeMillis()<until);
                        assertTrue("Native real-map viewport has no rendered tile: "+names[i]+" z"+z,cached[0]);
                        org.mapsforge.map.reader.MapFile file=repository.open(state.current());String diagnostic;
                        try{diagnostic=MapDiagnostics.inspect(file,viewport[0]);}finally{file.close();}
                        if(z==12){JSONObject d=new JSONObject(diagnostic);boolean features=false;for(int t=0;t<d.getJSONArray("visibleTiles").length();t++){var tile=d.getJSONArray("visibleTiles").getJSONObject(t);features|=tile.getInt("ways")>0||tile.getInt("pois")>0;}assertTrue("Readable target features are required",features);}
                        SystemClock.sleep(500);String name="p061-real-"+names[i]+"-z"+z;P0UiCapture.save(name);
                        captures.put(new JSONObject().put("file",name+".png").put("diagnostic",new JSONObject(diagnostic)));
                    }
                    screen.onActivity(a->{var v=view(a);v.bind(List.of(new RouteGeometry.Point(23.700,117.429,false),new RouteGeometry.Point(23.702,117.430,false)),null);v.fitRoute();assertEquals(1,v.segmentCount());});
                    P0UiCapture.save("p061-real-route");
                    LatLong[] center={null};byte[] zoom={0};screen.onActivity(a->{center[0]=view(a).mapView().getModel().mapViewPosition.getCenter();zoom[0]=view(a).mapView().getModel().mapViewPosition.getZoomLevel();});
                    screen.recreate();ready(screen);screen.onActivity(a->{assertEquals(center[0],view(a).mapView().getModel().mapViewPosition.getCenter());assertEquals(zoom[0],view(a).mapView().getModel().mapViewPosition.getZoomLevel());});
                    P0UiCapture.save("p061-real-recreated");
                }
                receipt.put("screenshots",captures).put("privateFileShaVerified",true).put("airplaneMode",true).put("passed",true);
            } else receipt.put("passed",true).put("privateFileShaVerified",true).put("sourceCopyDeleted",true);
            String output=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
            File dir=new File(output!=null?new File(output):new File(context.getExternalMediaDirs()[0],"additional_test_output"),"p0-ui");dir.mkdirs();
            try(var out=new FileOutputStream(new File(dir,"p061-real-map-"+phase+".json"))){out.write(receipt.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            Bundle status=new Bundle();status.putString("p061RealPid",Integer.toString(android.os.Process.myPid()));InstrumentationRegistry.getInstrumentation().sendStatus(0,status);
        } finally {if(!flight.equals("1"))shell("cmd connectivity airplane-mode disable");}
    }
}
