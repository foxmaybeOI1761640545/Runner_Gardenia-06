package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import java.io.File;
import org.junit.Test;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.reader.MapFile;

public class OfflineMapFixtureTest {
    @Test public void selfAuthoredFixtureContainsActualVectorWays(){
        MapFile map=new MapFile(new File("src/androidTest/assets/p06/gardenia-synthetic.map"));
        try{byte zoom=16;var center=map.startPosition();var result=map.readMapData(new Tile(MercatorProjection.longitudeToTileX(center.longitude,zoom),MercatorProjection.latitudeToTileY(center.latitude,zoom),zoom,256));assertNotNull(result);assertFalse("Fixture must contain actual map features",result.ways.isEmpty());assertEquals(1.3,center.latitude,0);assertEquals(103.8,center.longitude,0);}finally{map.close();}
    }
}
