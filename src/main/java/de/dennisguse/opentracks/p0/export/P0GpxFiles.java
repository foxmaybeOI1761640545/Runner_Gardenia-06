package de.dennisguse.opentracks.p0.export;

import android.content.Context;
import android.content.Intent;
import android.content.ClipData;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import de.dennisguse.opentracks.p0.data.WorkoutRepository;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Track;

/** Private cache + user-initiated Android document/share mechanisms. No network or storage permission. */
public final class P0GpxFiles {
    private static final long MAX_AGE=7L*24*60*60*1000;
    private static final Set<String> ACTIVE=ConcurrentHashMap.newKeySet();
    private final Context context;
    public P0GpxFiles(Context context){this.context=context.getApplicationContext();}
    public File prepare(Track.Id id) throws IOException {
        File directory=new File(context.getCacheDir(),"p0_exports");
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create private export directory");
        File[] old=directory.listFiles();
        if(old!=null)for(File file:old)if(file.lastModified()<System.currentTimeMillis()-7L*24*60*60*1000)file.delete();
        File file=new File(directory,"RunnerGardenia-"+id.id()+"-"+UUID.randomUUID()+".gpx");
        boolean complete=false;
        try {
            try(FileOutputStream output=new FileOutputStream(file)) {
                new P0GpxExporter(context).write(id,output);output.getFD().sync();
            }
            complete=true;
        }finally {if(!complete)file.delete();}
        return file;
    }
    public void save(File source,Uri target) throws IOException {
        try(var input=new FileInputStream(source);var output=context.getContentResolver().openOutputStream(target,"wt")) {
            if(output==null)throw new IOException("Document provider returned no stream");
            byte[] buffer=new byte[16384];int count;
            while((count=input.read(buffer))!=-1){
                if(Thread.currentThread().isInterrupted())throw new java.io.InterruptedIOException();
                output.write(buffer,0,count);
            }
            output.flush();
        }
    }
    public Intent share(File source) {
        Uri uri=FileProvider.getUriForFile(context,context.getPackageName()+".fileprovider",source);
        Intent intent=new Intent(Intent.ACTION_SEND).setType("application/gpx+xml").putExtra(Intent.EXTRA_STREAM,uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("Runner Gardenia GPX",uri));
        return intent;
    }
    /** Completed immutable files remain private and available to receiving apps until cache expiry. */
    public record Batch(File directory,List<File> gpx,File zip) implements AutoCloseable {
        public Batch {gpx=List.copyOf(gpx);}
        @Override public void close(){remove(directory);}
    }
    public static final class BatchFailure extends IOException {
        public final Long workoutId;
        BatchFailure(Long id,String message,Throwable cause){super(message,cause);workoutId=id;}
    }
    @FunctionalInterface public interface Progress {void completed(int done,int total,boolean packing);}
    private static void background(){if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new IllegalStateException("Batch file IO requires a worker");}
    private static void interrupted()throws java.io.InterruptedIOException{if(Thread.currentThread().isInterrupted())throw new java.io.InterruptedIOException("Export interrupted");}
    private File directory()throws IOException{
        File root=new File(context.getCacheDir(),"p0_exports");
        if(!root.isDirectory()&&!root.mkdirs())throw new IOException(context.getString(R.string.p05_cache_failed));return root;
    }
    private static void remove(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)remove(child);file.delete();}
    public void cleanStale()throws IOException{
        background();File[] old=directory().listFiles();
        if(old!=null)for(File file:old){
            boolean pending=file.getName().startsWith(".pending-");
            if(!ACTIVE.contains(file.getAbsolutePath())&&(pending||file.lastModified()<System.currentTimeMillis()-MAX_AGE))remove(file);
        }
    }
    private void check(WorkoutRepository repository,Track.Id id)throws IOException{
        interrupted();
        if(repository.find(id)==null)throw new BatchFailure(id.id(),context.getString(R.string.p0_workout_missing),null);
        if(repository.isProtected(id))throw new BatchFailure(id.id(),context.getString(R.string.p0_workout_protected),null);
    }
    /** Every selected record is checked before any output, and again before publishing the complete set. */
    public Batch prepareBatch(List<Track.Id> ids,boolean zip,Progress progress)throws IOException{
        background();interrupted();
        if(ids.isEmpty()||new HashSet<>(ids).size()!=ids.size())throw new IllegalArgumentException("Unique selected workout IDs required");
        WorkoutRepository repository=new WorkoutRepository(context);
        for(Track.Id id:ids)check(repository,id);
        cleanStale();String unique=UUID.randomUUID().toString();File root=directory(),pending=new File(root,".pending-"+unique);
        ACTIVE.add(pending.getAbsolutePath());boolean complete=false;Long current=null;
        try{
            if(!pending.mkdir())throw new IOException(context.getString(R.string.p05_cache_failed));
            List<String> names=new ArrayList<>();
            for(Track.Id id:ids){
                current=id.id();check(repository,id);String name="Workout-"+id.id()+".gpx";File target=new File(pending,name);
                try(FileOutputStream output=new FileOutputStream(target)){new P0GpxExporter(context).write(id,output);output.getFD().sync();}
                names.add(name);progress.completed(names.size(),ids.size(),false);interrupted();
            }
            String zipName=null;
            if(zip){
                zipName="RunnerGardenia-Export-"+java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss",java.util.Locale.ROOT).format(java.time.LocalDateTime.now())+"-"+unique+".zip";
                try(FileOutputStream file=new FileOutputStream(new File(pending,zipName));ZipOutputStream archive=new ZipOutputStream(file)){
                    int done=0;byte[] buffer=new byte[16384];
                    for(String name:names){
                        interrupted();current=ids.get(done).id();archive.putNextEntry(new ZipEntry(name));
                        try(FileInputStream input=new FileInputStream(new File(pending,name))){int size;while((size=input.read(buffer))!=-1){interrupted();archive.write(buffer,0,size);}}
                        archive.closeEntry();progress.completed(++done,ids.size(),true);
                    }
                    interrupted();archive.finish();archive.flush();file.getFD().sync();
                }
            }
            for(Track.Id id:ids)check(repository,id);interrupted();
            File ready=new File(root,"batch-"+unique);if(!pending.renameTo(ready))throw new IOException(context.getString(R.string.p05_cache_failed));
            List<File> values=new ArrayList<>();for(String name:names)values.add(new File(ready,name));
            Batch result=new Batch(ready,values,zipName==null?null:new File(ready,zipName));complete=true;return result;
        }catch(BatchFailure failure){throw failure;}
        catch(java.io.InterruptedIOException failure){throw failure;}
        catch(IOException|RuntimeException failure){throw new BatchFailure(current,failure.getMessage()==null?context.getString(R.string.p0_export_failed):failure.getMessage(),failure);}
        finally{ACTIVE.remove(pending.getAbsolutePath());if(!complete)remove(pending);}
    }
    public Intent share(Batch batch){
        if(batch.zip()!=null){Intent intent=share(batch.zip());intent.setType("application/zip");intent.setClipData(ClipData.newRawUri("Runner Gardenia ZIP",intent.getParcelableExtra(Intent.EXTRA_STREAM)));return intent;}
        if(batch.gpx().size()==1)return share(batch.gpx().get(0));
        ArrayList<Uri> uris=new ArrayList<>();for(File file:batch.gpx())uris.add(FileProvider.getUriForFile(context,context.getPackageName()+".fileprovider",file));
        Intent intent=new Intent(Intent.ACTION_SEND_MULTIPLE).setType("application/gpx+xml").putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ClipData clip=ClipData.newRawUri("Runner Gardenia GPX",uris.get(0));for(int i=1;i<uris.size();i++)clip.addItem(new ClipData.Item(uris.get(i)));intent.setClipData(clip);return intent;
    }
}
