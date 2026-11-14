import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;

/** Undirected rendered-highway connectivity, with coordinates quantized to roughly one metre. */
class MapRoadConnectivity {
 static class Union {
  Map<Long,Integer> ids=new HashMap<>();int[] parents=new int[100000],sizes=new int[100000];int count;
  int node(double lat,double lon){long key=((long)Math.round((lat+90)*100000)<<32)|(Math.round((lon+180)*100000)&0xffffffffL);Integer old=ids.get(key);if(old!=null)return old;
   int n=count++;if(n==parents.length){parents=Arrays.copyOf(parents,n*2);sizes=Arrays.copyOf(sizes,n*2);}parents[n]=n;sizes[n]=1;ids.put(key,n);return n;}
  int root(int n){while(parents[n]!=n){parents[n]=parents[parents[n]];n=parents[n];}return n;}
  void join(int a,int b){a=root(a);b=root(b);if(a==b)return;if(sizes[a]<sizes[b]){int t=a;a=b;b=t;}parents[b]=a;sizes[a]+=sizes[b];}
 }
 public static void main(String[] args)throws Exception {
  String[] names={"Guangzhou","Foshan","Zhuhai","Shenzhen","Dongguan","Zhongshan","Jiangmen","Huizhou","Zhaoqing","Shanwei","Jieyang","Shantou","Chaozhou","Zhangzhou","Longhai","Zhangpu","Dongshan"};
  double[][] points={{23.1291,113.2644},{23.0218,113.1219},{22.271,113.5767},{22.5431,114.0579},{23.0207,113.7518},{22.5159,113.3926},{22.5787,113.0815},{23.1115,114.4152},{23.0469,112.4651},{22.7876,115.3753},{23.5438,116.3557},{23.3535,116.6822},{23.6567,116.6226},{24.513,117.6471},{24.446,117.818},{24.117,117.613},{23.701,117.429}};
  int[] near=new int[names.length];double[] distances=new double[names.length];Arrays.fill(distances,Double.MAX_VALUE);
  Union u=new Union();Set<String> tiles=new HashSet<>();long ways=0;
  MapFile map=new MapFile(new File(args[0]));
  try{for(String line:Files.readAllLines(Path.of(args[1]))){var f=line.split(",");double lat=Double.parseDouble(f[0]),lon=Double.parseDouble(f[1]);byte zoom=12;var tile=new Tile(MercatorProjection.longitudeToTileX(lon,zoom),MercatorProjection.latitudeToTileY(lat,zoom),zoom,256);
   if(!tiles.add(tile.tileX+"/"+tile.tileY))continue;var data=map.readMapData(tile);if(data==null)throw new IOException("Unreadable tile");
   for(var way:data.ways){if(way.tags.stream().noneMatch(t->t.key.equals("highway")))continue;ways++;
    for(var path:way.latLongs){int first=-1;for(var point:path){int id=u.node(point.latitude,point.longitude);if(first<0)first=id;else u.join(first,id);
     for(int i=0;i<points.length;i++){double dy=(point.latitude-points[i][0])*111320,dx=(point.longitude-points[i][1])*102500,dist=dx*dx+dy*dy;if(dist<distances[i]){distances[i]=dist;near[i]=id;}}
    }}
   }
  }}finally{map.close();}
  System.out.println("name,nearestHighwayMetres,component,componentNodes");
  for(int i=0;i<names.length;i++){int root=u.root(near[i]);System.out.println(names[i]+","+Math.sqrt(distances[i])+","+root+","+u.sizes[root]);}
  System.err.println("uniqueTiles="+tiles.size()+" highwayWaysWithDuplicates="+ways+" graphNodes="+u.count);
 }
}
