package de.dennisguse.opentracks.p0.map;

import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Test APK only. Exercises the exact SAF content-stream import path with CC0 fixture bytes. */
public final class P06FixtureProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){return "application/octet-stream";}
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME});c.addRow(new Object[]{uri.getLastPathSegment()});return c;}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        if(!"r".equals(mode))throw new FileNotFoundException("Read only");
        try{
            File file=new File(getContext().getCacheDir(),"p06-provider-"+("broken.map".equals(uri.getLastPathSegment())?"broken":"valid"));
            try(var out=new FileOutputStream(file)){
                if("broken.map".equals(uri.getLastPathSegment()))out.write(new byte[80]);
                else try(var in=getContext().getAssets().open("real.map".equals(uri.getLastPathSegment())?"p061/real.map":"p06/gardenia-synthetic.map")){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}
            }
            return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
        }catch(IOException failure){throw new FileNotFoundException(failure.toString());}
    }
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){
        if(!"real.map".equals(u.getLastPathSegment()))throw new UnsupportedOperationException();
        File file=new File(getContext().getCacheDir(),"p06-provider-valid");
        return file.isFile()&&file.delete()?1:0;
    }
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
