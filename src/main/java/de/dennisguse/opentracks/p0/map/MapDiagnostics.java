package de.dennisguse.opentracks.p0.map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mapsforge.map.reader.MapFile;

/** Explicit local diagnostic, evaluated on the file worker using an independent reader. */
public final class MapDiagnostics {
    private MapDiagnostics() {}
    public static String inspect(MapFile file, OfflineMapView.DiagnosticViewport viewport) throws Exception {
        JSONObject result = new JSONObject().put("latitude", viewport.center().latitude)
                .put("longitude", viewport.center().longitude).put("zoom", viewport.zoom())
                .put("mapFileBoundingBox", file.boundingBox().toString())
                .put("tilePixelSize",viewport.tileSize()).put("cacheCapacity",viewport.cacheCapacity())
                .put("viewWidth",viewport.viewWidth()).put("viewHeight",viewport.viewHeight())
                .put("frameWidth",viewport.frameWidth()).put("frameHeight",viewport.frameHeight())
                .put("overdrawFactor",viewport.overdraw()).put("completedTilesTotal",viewport.completedTiles())
                .put("administrativePolygon", "UNKNOWN: bounding box is not the target union")
                .put("renderer", "Native Mapsforge 0.25.0; cache absence alone is not a rendering failure");
        JSONArray tiles = new JSONArray();
        for (var state : viewport.tiles()) {
            var tile = state.tile();
            boolean supported = file.supportsTile(tile);
            var data = supported ? file.readMapData(tile) : null;
            tiles.put(new JSONObject().put("x", tile.tileX).put("y", tile.tileY)
                    .put("zoom", tile.zoomLevel).put("readerSupportsTile", supported)
                    .put("mapDataAvailable", data != null)
                    .put("ways", data == null ? -1 : data.ways.size())
                    .put("pois", data == null ? -1 : data.pois.size())
                    .put("rendererTileState", state.cached() ? "CACHED" : "NOT_CACHED_AT_CAPTURE")
                    .put("interpretation", !supported ? "OUTSIDE_FILE_BOUNDS" : data == null ? "READ_FAILED" :
                            data.ways.isEmpty() && data.pois.isEmpty() ? "NO_FEATURES_AT_THIS_ZOOM" : "HAS_FEATURES"));
        }
        return result.put("visibleTiles", tiles).toString(2);
    }
}
