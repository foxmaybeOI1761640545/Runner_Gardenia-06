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
import de.dennisguse.opentracks.data.models.Track;

/** Private cache + user-initiated Android document/share mechanisms. No network or storage permission. */
public final class P0GpxFiles {
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
}
