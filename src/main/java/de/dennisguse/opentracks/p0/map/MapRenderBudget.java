package de.dennisguse.opentracks.p0.map;

import org.mapsforge.core.util.Parameters;

/** This north-up screen needs a viewport buffer, not a diagonal square for rotation. */
public final class MapRenderBudget {
    public static final long MAX_CACHE_BYTES=32L*1024*1024;
    public static final double OVERDRAW=1.0;
    private MapRenderBudget() {}
    public static void configure(){Parameters.SQUARE_FRAME_BUFFER=false;}
    public static int capacity(int tileSize){
        if(tileSize<=0)throw new IllegalArgumentException("Invalid tile size");
        return (int)Math.max(1,Math.min(64,MAX_CACHE_BYTES/(tileSize*(long)tileSize*4)));
    }
}
