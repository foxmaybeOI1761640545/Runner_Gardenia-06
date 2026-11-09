package de.dennisguse.opentracks.p0;
import static org.junit.Assert.*;
import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import androidx.test.core.app.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import de.dennisguse.opentracks.data.*;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.app.P0HistoryActivity;
import de.dennisguse.opentracks.p0.data.*;
import de.dennisguse.opentracks.p0.export.*;
import de.dennisguse.opentracks.p0.recording.RecordingSession;
import de.dennisguse.opentracks.p0.share.ShareReceiptActivity;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** Real GPX/ZIP files, real SQLite inputs, real exported receiver in a distinct UID/process. */
public class P05BatchFilesTest {
 private final Context context=ApplicationProvider.getApplicationContext();private final List<Track.Id> ids=new ArrayList<>();
 private final Map<Track.Id,String> original=new HashMap<>();
 @Before public void before()throws Exception{TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();new SessionStore(context).save(RecordingSession.ready());for(int i=0;i<3;i++){var id=P05HistoryFixtures.create(context,"Same synthetic batch name",true);ids.add(id);original.put(id,source(id));}}
 @After public void after(){Thread.interrupted();new SessionStore(context).save(RecordingSession.ready());}
 private static String sha(byte[] raw)throws Exception{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));}
 private String source(Track.Id id)throws Exception{var out=new ByteArrayOutputStream();new P0GpxExporter(context).write(id,out);return sha(out.toByteArray());}
 private void unchanged()throws Exception{for(var id:ids){assertNotNull(new WorkoutRepository(context).find(id));assertEquals(3,new HeartRateRepository(context).read(id).size());assertEquals(original.get(id),source(id));}}
 private File output(String folder){String configured=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");File root=new File(configured!=null?new File(configured):new File(context.getExternalMediaDirs()[0],"additional_test_output"),folder);assertTrue(root.isDirectory()||root.mkdirs());return root;}
 private JSONObject receive(Intent actual,String name,boolean grant)throws Exception{
  var instrumentation=InstrumentationRegistry.getInstrumentation();int targetUid=context.getApplicationInfo().uid,testUid=instrumentation.getContext().getApplicationInfo().uid;assertNotEquals(targetUid,testUid);
  CountDownLatch done=new CountDownLatch(1);AtomicReference<Bundle> receipt=new AtomicReference<>();AtomicInteger caller=new AtomicInteger(-1);String nonce=UUID.randomUUID().toString();
  Binder binder=new Binder(){@Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
   if(code!=IBinder.FIRST_CALL_TRANSACTION)return super.onTransact(code,data,reply,flags);data.enforceInterface(ShareReceiptActivity.TOKEN);caller.set(Binder.getCallingUid());receipt.set(data.readBundle(getClass().getClassLoader()));reply.writeNoException();done.countDown();return true;
  }};
  Bundle channel=new Bundle();channel.putBinder("binder",binder);channel.putString("nonce",nonce);
  Intent request=new Intent(actual).setComponent(new ComponentName(instrumentation.getContext().getPackageName(),ShareReceiptActivity.class.getName())).putExtra(ShareReceiptActivity.RECEIPT,channel).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  ArrayList<Uri> uris=new ArrayList<>();if(Intent.ACTION_SEND_MULTIPLE.equals(actual.getAction()))uris.addAll(actual.getParcelableArrayListExtra(Intent.EXTRA_STREAM));else uris.add(actual.getParcelableExtra(Intent.EXTRA_STREAM));
  for(Uri uri:uris){assertEquals("content",uri.getScheme());context.revokeUriPermission(instrumentation.getContext().getPackageName(),uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}
  if(!grant){request.setAction("de.dennisguse.opentracks.p0.TEST_WITHOUT_GRANT").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);request.setClipData(null);} // SEND migration otherwise automatically restores a read grant.
  context.startActivity(request);assertTrue("Distinct UID receiver timed out",done.await(20,TimeUnit.SECONDS));assertEquals(testUid,caller.get());assertEquals(nonce,receipt.get().getString("nonce"));assertNull(receipt.get().getString("exception"));
  JSONObject proof=new JSONObject(receipt.get().getString("json"));assertEquals(testUid,proof.getInt("uid"));assertNotEquals(android.os.Process.myPid(),proof.getInt("pid"));assertEquals(instrumentation.getContext().getPackageName(),proof.getString("package"));
  JSONArray rows=proof.getJSONArray("files");assertEquals(uris.size(),rows.length());assertEquals(grant?uris.size():0,proof.getJSONArray("clip").length());
  for(int i=0;i<rows.length();i++){JSONObject file=rows.getJSONObject(i);assertEquals(uris.get(i).toString(),file.getString("uri"));assertEquals(grant,file.getBoolean("read"));
   if(grant){assertEquals(android.content.pm.PackageManager.PERMISSION_GRANTED,file.getInt("permission"));assertEquals(uris.get(i).toString(),proof.getJSONArray("clip").getString(i));try(InputStream input=context.getContentResolver().openInputStream(uris.get(i))){assertEquals(sha(input.readAllBytes()),file.getString("sha256"));}}
   else assertEquals(SecurityException.class.getName(),file.getString("errorClass"));
  }
  proof.put("permissionContractPassed",true).put("api",Build.VERSION.SDK_INT).put("authenticatedBinderUid",caller.get()).put("targetUid",targetUid).put("negativeControl",!grant).put("sourceCommit",InstrumentationRegistry.getArguments().getString("buildCommit","local"));Files.writeString(new File(output("p05-share"),name+".json").toPath(),proof.toString(2));return proof;
 }
 @Test public void distinctUidReceivesSingleMultipleAndZipWithVerifiedGrantsAndEveryGpx()throws Exception{
  P0GpxFiles files=new P0GpxFiles(context);
  try(ActivityScenario<P0HistoryActivity> host=ActivityScenario.launch(P0HistoryActivity.class)){
   for(String format:new String[]{"single","multiple","zip"}){
    List<Track.Id> selected=format.equals("single")?ids.subList(0,1):ids;
    try(var batch=files.prepareBatch(selected,format.equals("zip"),(d,t,z)->{})){
     Intent share=files.share(batch);assertEquals(format.equals("multiple")?Intent.ACTION_SEND_MULTIPLE:Intent.ACTION_SEND,share.getAction());assertEquals(format.equals("zip")?"application/zip":"application/gpx+xml",share.getType());assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION,share.getFlags());
     receive(share,format+"-denied",false);JSONObject proof=receive(share,format+"-received",true);Set<Long> received=new HashSet<>();
     for(int i=0;i<proof.getJSONArray("files").length();i++){
      JSONObject row=proof.getJSONArray("files").getJSONObject(i);
      if(format.equals("zip")){var entries=row.getJSONArray("entries");assertEquals(selected.size(),entries.length());for(int n=0;n<entries.length();n++){var entry=entries.getJSONObject(n);long id=entry.getLong("workoutId");assertTrue(received.add(id));assertEquals(3,entry.getInt("rawSamples"));assertEquals(original.get(new Track.Id(id)),entry.getString("sha256"));assertEquals("Workout-"+id+".gpx",entry.getString("name"));}}
      else {var entry=row.getJSONObject("gpx");long id=entry.getLong("workoutId");assertTrue(received.add(id));assertEquals(3,entry.getInt("rawSamples"));assertEquals(original.get(new Track.Id(id)),entry.getString("sha256"));}
     }
     assertEquals(new HashSet<>(selected.stream().map(Track.Id::id).toList()),received);
     for(File gpx:batch.gpx())Files.copy(gpx.toPath(),new File(output("p0-gpx"),"batch-"+format+"-"+gpx.getName()).toPath(),StandardCopyOption.REPLACE_EXISTING);
     unchanged();
    }
   }
  }
 }
 @Test public void missingProtectedInvalidAndInterruptedBatchesNeverExposePartialFiles()throws Exception{
  P0GpxFiles files=new P0GpxFiles(context);List<Track.Id> mixed=new ArrayList<>(ids);mixed.add(new Track.Id(3000000000L));
  try{files.prepareBatch(mixed,false,(d,t,z)->{});fail("Missing record cannot be silently skipped");}catch(P0GpxFiles.BatchFailure expected){assertEquals(Long.valueOf(3000000000L),expected.workoutId);}
  for(boolean paused:new boolean[]{false,true}){RecordingSession session=RecordingSession.ready().begin(ids.get(1).id());new SessionStore(context).save(paused?session.pause():session);
   try{files.prepareBatch(ids,true,(d,t,z)->{});fail("Active/paused record must protect the entire batch");}catch(P0GpxFiles.BatchFailure expected){assertEquals(Long.valueOf(ids.get(1).id()),expected.workoutId);}finally{new SessionStore(context).save(RecordingSession.ready());}}
  for(boolean zip:new boolean[]{false,true}){
   try{files.prepareBatch(ids,zip,(d,t,packing)->{if(d==1&&packing==zip)Thread.currentThread().interrupt();});fail("Interrupted generation cannot succeed");}catch(InterruptedIOException expected){}finally{Thread.interrupted();}
   File[] pending=new File(context.getCacheDir(),"p0_exports").listFiles(f->f.getName().startsWith(".pending-"));assertNotNull(pending);assertEquals(0,pending.length);unchanged();
  }
  var invalid=P05HistoryFixtures.create(context,"Invalid coordinate fixture",false);new ContentProviderUtils(context).insertTrackPoint(new TrackPoint(null,TrackPoint.Type.TRACKPOINT,new Position(java.time.Instant.EPOCH,91d,103.8,Distance.of(2),null,null,null,null),null,null,null,null,null,null),invalid);
  mixed=new ArrayList<>(ids);mixed.add(invalid);try{files.prepareBatch(mixed,true,(d,t,z)->{});fail("Invalid selected record must fail the complete set");}catch(P0GpxFiles.BatchFailure expected){assertEquals(Long.valueOf(invalid.id()),expected.workoutId);assertTrue(expected.getMessage().contains("coordinates"));}unchanged();
 }
 @Test public void orphanAndTtlCleanupAndWorkerGuardAreReal()throws Exception{
  P0GpxFiles files=new P0GpxFiles(context);File root=new File(context.getCacheDir(),"p0_exports");assertTrue(root.isDirectory()||root.mkdirs());
  File pending=new File(root,".pending-simulated-process-death");assertTrue(pending.mkdirs());Files.writeString(new File(pending,"Workout-incomplete.gpx").toPath(),"incomplete");
  File old=new File(root,"batch-stale-fixture");assertTrue(old.mkdirs());Files.writeString(new File(old,"old.gpx").toPath(),"old");assertTrue(old.setLastModified(System.currentTimeMillis()-8L*24*60*60*1000));
  try(var completed=files.prepareBatch(ids,false,(d,t,z)->{})){files.cleanStale();assertFalse(pending.exists());assertFalse(old.exists());assertTrue(completed.gpx().get(0).exists());}
  AtomicReference<Throwable> failure=new AtomicReference<>();InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{try{files.prepareBatch(ids,false,(d,t,z)->{});}catch(Throwable expected){failure.set(expected);}});assertTrue(failure.get() instanceof IllegalStateException);unchanged();
 }
}
