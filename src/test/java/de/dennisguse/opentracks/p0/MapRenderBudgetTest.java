package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import org.junit.Test;
import org.mapsforge.core.model.*;
import org.mapsforge.core.util.*;
import org.mapsforge.map.controller.FrameBufferController;
import org.mapsforge.map.util.MapPositionUtil;
import de.dennisguse.opentracks.p0.map.MapRenderBudget;

public class MapRenderBudgetTest {
    private int tiles(Dimension frame,int size,double lat,double lon,byte zoom){
        var box=MapPositionUtil.getBoundingBox(new MapPosition(new LatLong(lat,lon),zoom),Rotation.NULL_ROTATION,size,frame,.5f,.5f);
        return (MercatorProjection.longitudeToTileX(box.maxLongitude,zoom)-MercatorProjection.longitudeToTileX(box.minLongitude,zoom)+1)
                *(MercatorProjection.latitudeToTileY(box.minLatitude,zoom)-MercatorProjection.latitudeToTileY(box.maxLatitude,zoom)+1);
    }
    @Test public void legacyRotationSquareCanExceedTheBoundedTileCache(){
        boolean square=Parameters.SQUARE_FRAME_BUFFER;
        try{Parameters.SQUARE_FRAME_BUFFER=true;var frame=FrameBufferController.calculateFrameBufferDimension(new Dimension(1080,1650),1.2);
            assertTrue("Legacy offscreen background tiles compete with visible data",tiles(frame,768,22.5431,114.0579,(byte)7)>MapRenderBudget.capacity(768));
        }finally{Parameters.SQUARE_FRAME_BUFFER=square;}
    }
    @Test public void configuredPhoneViewportsFitBudgetAcrossTargetCitiesAndZooms(){
        boolean square=Parameters.SQUARE_FRAME_BUFFER;
        try{MapRenderBudget.configure();
            int[][] profiles={{1080,1650,768},{1236,2050,768},{720,950,512},{2400,450,704}};
            double[][] cities={{23.1291,113.2644},{23.0218,113.1219},{22.271,113.5767},{22.5431,114.0579},{24.513,117.6471},{23.701,117.429}};
            for(var profile:profiles){var view=new Dimension(profile[0],profile[1]);var frame=FrameBufferController.calculateFrameBufferDimension(view,MapRenderBudget.OVERDRAW);
                assertEquals(view,frame);
                for(var city:cities)for(byte zoom:new byte[]{7,12,16})assertTrue(tiles(frame,profile[2],city[0],city[1],zoom)<=MapRenderBudget.capacity(profile[2]));
            }
        }finally{Parameters.SQUARE_FRAME_BUFFER=square;}
    }
    @Test public void cacheRetainsItsExplicitByteAndEntryBounds(){
        for(int size:new int[]{256,512,704,768,1024}){int count=MapRenderBudget.capacity(size);assertTrue(count>0&&count<=64);assertTrue(count*size*(long)size*4<=MapRenderBudget.MAX_CACHE_BYTES);}
    }
}
