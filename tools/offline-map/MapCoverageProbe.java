import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;

/** Pinned-reader diagnostics on actual file bytes; an empty tile is not automatically an error. */
class MapCoverageProbe {
    public static void main(String[] args) throws Exception {
        MapFile map=new MapFile(new File(args[0]));
        try {
            Map<String,String> cache=new HashMap<>();
            System.out.println("latitude,longitude,zoom,tileX,tileY,supported,readable,ways,pois,water,highways");
            for(String line:Files.readAllLines(Path.of(args[1]))){
                String[] fields=line.split(",");double lat=Double.parseDouble(fields[0]),lon=Double.parseDouble(fields[1]);
                byte zoom=fields.length>2?Byte.parseByte(fields[2]):12;
                Tile tile=new Tile(MercatorProjection.longitudeToTileX(lon,zoom),MercatorProjection.latitudeToTileY(lat,zoom),zoom,256);
                String key=zoom+"/"+tile.tileX+"/"+tile.tileY;
                String row=cache.get(key);
                if(row==null){
                    var data=map.readMapData(tile);int roads=0;
                    if(data!=null)for(var way:data.ways)if(way.tags.stream().anyMatch(t->t.key.equals("highway")))roads++;
                    row=zoom+","+tile.tileX+","+tile.tileY+","+map.supportsTile(tile)+","+(data!=null)+","+(data==null?-1:data.ways.size())+","+(data==null?-1:data.pois.size())+","+(data!=null&&data.isWater)+","+roads;
                    cache.put(key,row);
                }
                System.out.println(lat+","+lon+","+row);
            }
        } finally {map.close();}
    }
}
