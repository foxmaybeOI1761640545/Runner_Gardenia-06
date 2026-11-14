package de.dennisguse.opentracks.p0.map;

import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.reader.MapFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Worker-thread-only private copies. A failed import never replaces the selected map. */
public final class OfflineMapRepository {
    public record Entry(String id,String name,long bytes) {}
    public record State(List<Entry> entries,String selected) {
        public Entry current(){return entries.stream().filter(e->e.id().equals(selected)).findFirst().orElse(null);}
    }
    private static final Object LOCK=new Object();
    private static final long MAX_BYTES=2L*1024*1024*1024;
    private final File directory;
    public OfflineMapRepository(Context context){directory=new File(context.getFilesDir(),"offline-maps");}
    private AtomicFile index(){return new AtomicFile(new File(directory,"index.json"));}
    private File path(String id){if(!id.matches("[0-9a-f-]{36}"))throw new IllegalArgumentException("Invalid map identifier");return new File(directory,id+".map");}
    public State read() throws IOException {
        synchronized(LOCK){
            if(!directory.exists())return new State(List.of(),null);
            // A killed process cannot execute importMap's finally block. The same
            // lock excludes an active copy while reclaiming only incomplete files.
            File[] abandoned=directory.listFiles((dir,name)->name.matches("[0-9a-f-]{36}\\.pending"));
            if(abandoned!=null)for(File file:abandoned)if(file.isFile())file.delete();
            try(var input=index().openRead()){
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1){if(bytes.size()>1024*1024)throw new IOException("Map index too large");bytes.write(buffer,0,n);}
                JSONObject json=new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
                if(json.getInt("schema")!=1)throw new IOException("Unsupported map index");
                JSONArray array=json.getJSONArray("maps");List<Entry> entries=new ArrayList<>();
                for(int i=0;i<array.length();i++){JSONObject row=array.getJSONObject(i);String id=row.getString("id");File file=path(id);if(file.isFile())entries.add(new Entry(id,row.getString("name"),file.length()));}
                String selected=json.optString("selected","");return new State(List.copyOf(entries),selected);
            }catch(FileNotFoundException missing){return new State(List.of(),null);}
            catch(org.json.JSONException|IllegalArgumentException invalid){throw new IOException("Invalid local map index",invalid);}
        }
    }
    private void write(State state)throws IOException{
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create map storage");
        AtomicFile atomic=index();FileOutputStream output=null;
        try{
            JSONArray maps=new JSONArray();for(Entry e:state.entries())maps.put(new JSONObject().put("id",e.id()).put("name",e.name()));
            byte[] data=new JSONObject().put("schema",1).put("selected",state.selected()).put("maps",maps).toString().getBytes(StandardCharsets.UTF_8);
            output=atomic.startWrite();output.write(data);atomic.finishWrite(output);
        }catch(Exception failure){if(output!=null)atomic.failWrite(output);throw new IOException("Cannot save map selection",failure);}
    }
    public State importMap(InputStream input,String displayName)throws IOException{
        synchronized(LOCK){
            State previous=read();if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create map storage");
            String id=UUID.randomUUID().toString();File pending=new File(directory,id+".pending"),target=path(id);
            try{
                long count=0;try(FileOutputStream output=new FileOutputStream(pending)){
                    byte[] buffer=new byte[65536];int n;while((n=input.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();count+=n;if(count>MAX_BYTES)throw new IOException("Map exceeds 2 GiB limit");output.write(buffer,0,n);}output.getFD().sync();
                }
                if (pending.length()!=count) throw new IOException("Incomplete map copy");
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                validate(pending);
                if(!pending.renameTo(target))throw new IOException("Cannot retain imported map");
                String name=displayName==null?"Local map":displayName.replaceAll("[\\p{Cntrl}/\\\\]","_").strip();if(name.isEmpty())name="Local map";if(name.length()>160)name=name.substring(0,160);
                List<Entry> maps=new ArrayList<>(previous.entries());maps.add(new Entry(id,name,count));State next=new State(List.copyOf(maps),id);write(next);return next;
            }catch(IOException|RuntimeException failure){target.delete();throw new IOException("Invalid, unsupported or unreadable map",failure);}
            finally{pending.delete();}
        }
    }
    public State select(String id)throws IOException{
        synchronized(LOCK){State old=read();if(old.entries().stream().noneMatch(e->e.id().equals(id)))throw new IOException("Map no longer available");validate(path(id));State next=new State(old.entries(),id);write(next);return next;}
    }
    /** Caller closes its renderer before deletion. Index is committed before unlinking. */
    public State delete(String id)throws IOException{
        synchronized(LOCK){State old=read();List<Entry> rows=old.entries().stream().filter(e->!e.id().equals(id)).toList();String selected=Objects.equals(old.selected(),id)?(rows.isEmpty()?null:rows.get(0).id()):old.selected();State next=new State(rows,selected);write(next);File file=path(id);if(file.exists()&&!file.delete())throw new IOException("Cannot remove map file");return next;}
    }
    public MapFile open(Entry entry)throws IOException{
        synchronized(LOCK){try{return new MapFile(path(entry.id()));}catch(RuntimeException invalid){throw new IOException("Map cannot be opened",invalid);}}
    }
    private static void validate(File file)throws IOException{
        MapFile map=null;
        try{
            map=new MapFile(file);if(map.getMapFileInfo().fileSize!=file.length())throw new IOException("Truncated map");
            LatLong center=map.boundingBox().getCenterPoint();byte zoom=map.startZoomLevel();
            Tile tile=new Tile(MercatorProjection.longitudeToTileX(center.longitude,zoom),MercatorProjection.latitudeToTileY(center.latitude,zoom),zoom,256);
            if(map.readMapData(tile)==null)throw new IOException("Unreadable map data");
        }catch(RuntimeException invalid){throw new IOException("Invalid Mapsforge map",invalid);}finally{if(map!=null)map.close();}
    }
}
