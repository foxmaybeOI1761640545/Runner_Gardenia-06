package de.dennisguse.opentracks.p0.share;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.DocumentBuilderFactory;

/** A standalone test-package process/UID. Framework APIs only; no target Application or provider reads. */
public class ShareReceiptActivity extends Activity {
 public static final String RECEIPT="p05.receipt",TOKEN="RunnerGardeniaShareReceipt";
 @Override public void onCreate(Bundle saved){super.onCreate(saved);var text=new android.widget.TextView(this);text.setText("Reading shared files in receiver UID");setContentView(text);new Thread(this::receive,"distinct-uid-share-receiver").start();}
 private static byte[] read(InputStream input)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] block=new byte[16384];int count;while((count=input.read(block))!=-1){if(out.size()+count>64*1024*1024)throw new IOException("Test receiver input exceeds bound");out.write(block,0,count);}return out.toByteArray();}
 private static String sha(byte[] bytes)throws Exception{StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();}
 private static JSONObject gpx(byte[] bytes)throws Exception{
  var f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);
  var doc=f.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));var root=doc.getDocumentElement();
  if(!"http://www.topografix.com/GPX/1/1".equals(root.getNamespaceURI())||!"1.1".equals(root.getAttribute("version")))throw new IOException("Invalid GPX namespace/version");
  var records=doc.getElementsByTagNameNS("urn:runner-gardenia:gpx:1","workout");if(records.getLength()!=1)throw new IOException("Missing workout identity");
  return new JSONObject().put("workoutId",((org.w3c.dom.Element)records.item(0)).getAttribute("id")).put("rawSamples",doc.getElementsByTagNameNS("urn:runner-gardenia:gpx:1","sample").getLength()).put("sha256",sha(bytes)).put("bytes",bytes.length);
 }
 private JSONObject file(Uri uri,String mime)throws Exception{
  JSONObject r=new JSONObject().put("uri",uri.toString()).put("permission",checkUriPermission(uri,android.os.Process.myPid(),android.os.Process.myUid(),Intent.FLAG_GRANT_READ_URI_PERMISSION));
  try(InputStream input=getContentResolver().openInputStream(uri)){
   if(input==null)throw new IOException("No received stream");byte[] bytes=read(input);r.put("sha256",sha(bytes)).put("bytes",bytes.length).put("read",true);
   if("application/zip".equals(mime)){
    JSONArray entries=new JSONArray();Set<String> names=new HashSet<>();
    try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry e;while((e=zip.getNextEntry())!=null){if(e.isDirectory()||e.getName().contains("/")||e.getName().contains("..")||!names.add(e.getName())||names.size()>200)throw new IOException("Invalid/duplicate archive entry");entries.put(gpx(read(zip)).put("name",e.getName()));zip.closeEntry();}}
    r.put("entries",entries);
   }else r.put("gpx",gpx(bytes));
  }catch(SecurityException denied){r.put("read",false).put("errorClass",denied.getClass().getName());}
  catch(Exception failed){r.put("read",false).put("errorClass",failed.getClass().getName()).put("error",String.valueOf(failed.getMessage()));}
  return r;
 }
 private void receive(){
  Bundle reply=new Bundle();Intent intent=getIntent();Bundle channel=intent.getBundleExtra(RECEIPT);
  try{
   JSONObject proof=new JSONObject().put("package",getPackageName()).put("uid",android.os.Process.myUid()).put("pid",android.os.Process.myPid()).put("action",intent.getAction()).put("mime",intent.getType()).put("flags",intent.getFlags());
   ArrayList<Uri> uris=new ArrayList<>();Object streams=intent.getExtras().get(Intent.EXTRA_STREAM);
   if(streams instanceof Uri uri)uris.add(uri);else if(streams instanceof ArrayList<?> all)for(Object value:all){if(!(value instanceof Uri))throw new IOException("Non URI stream");uris.add((Uri)value);}else throw new IOException("No actual EXTRA_STREAM");
   JSONArray clip=new JSONArray();if(intent.getClipData()!=null)for(int i=0;i<intent.getClipData().getItemCount();i++)clip.put(String.valueOf(intent.getClipData().getItemAt(i).getUri()));
   JSONArray files=new JSONArray();for(Uri uri:uris)files.put(file(uri,intent.getType()));proof.put("clip",clip).put("files",files);reply.putString("json",proof.toString());
  }catch(Throwable failure){reply.putString("exception",failure.toString());}
  try{
   reply.putString("nonce",channel.getString("nonce"));IBinder callback=channel.getBinder("binder");Parcel data=Parcel.obtain(),response=Parcel.obtain();
   try{data.writeInterfaceToken(TOKEN);data.writeBundle(reply);callback.transact(IBinder.FIRST_CALL_TRANSACTION,data,response,0);response.readException();}finally{data.recycle();response.recycle();}
  }catch(Exception failure){android.util.Log.e("P05Receiver","Cannot deliver test receipt",failure);}
  finally{runOnUiThread(this::finish);}
 }
}
