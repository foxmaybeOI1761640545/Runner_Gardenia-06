package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.Test;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.reader.MapFile;

/** Independent actual-file acceptance. CI supplies the verified artifact outside Git/APK. */
public class RealMapFileTest {
    @Test public void actualMapDigestAndTargetCitiesContainFeatures() throws Exception {
        String path=System.getenv("RG_REAL_MAP");assertNotNull("CI must supply the actual 100MB map",path);
        File file=new File(path);assertEquals(105834033L,file.length());
        var digest=MessageDigest.getInstance("SHA-256");
        try(var input=new FileInputStream(file)){byte[] buffer=new byte[65536];int n;while((n=input.read(buffer))!=-1)digest.update(buffer,0,n);}
        assertEquals("5412067262dace98d9f95b02621446516e7d98e1debbb6a314d575cae022ee1e",HexFormat.of().formatHex(digest.digest()));
        MapFile map=new MapFile(file);
        try {
            assertEquals(file.length(),map.getMapFileInfo().fileSize);
            double[][] points={{23.1291,113.2644},{22.5431,114.0579},{22.271,113.5767},{23.0218,113.1219},{23.0207,113.7518},{22.5159,113.3926},{22.5787,113.0815},{23.1115,114.4152},{23.0469,112.4651},{22.7876,115.3753},{23.5438,116.3557},{23.3535,116.6822},{23.6567,116.6226},{24.513,117.6471},{23.701,117.429},{24.446,117.818},{24.117,117.613}};
            for(var point:points){byte zoom=12;var data=map.readMapData(new Tile(MercatorProjection.longitudeToTileX(point[1],zoom),MercatorProjection.latitudeToTileY(point[0],zoom),zoom,256));
                assertNotNull(data);assertTrue("Expected real target-city features at "+java.util.Arrays.toString(point),!data.ways.isEmpty()||!data.pois.isEmpty());}
        } finally {map.close();}
    }
}
