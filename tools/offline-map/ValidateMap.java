import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.reader.MapFile;

public final class ValidateMap {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: ValidateMap <map-file> <samples.csv>");
        File file = new File(args[0]);
        if (!file.isFile() || file.length() < 1024) throw new IllegalStateException("Map file is missing or too small");
        MapFile map = new MapFile(file);
        try {
            if (map.getMapFileInfo().fileSize != file.length()) throw new IllegalStateException("Map header size mismatch");
            byte zoom = map.startZoomLevel();
            LatLong center = map.boundingBox().getCenterPoint();
            assertReadable(map, center, zoom, "map-center");
            int count = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(args[1]))) {
                reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split(",");
                    if (fields.length != 3) throw new IllegalStateException("Invalid sample row: " + line);
                    String city = fields[0];
                    double lon = Double.parseDouble(fields[1]);
                    double lat = Double.parseDouble(fields[2]);
                    assertReadable(map, new LatLong(lat, lon), zoom, city);
                    count++;
                }
            }
            if (count != 14) throw new IllegalStateException("Expected 14 city sample points, got " + count);
            System.out.println("MAP_VALID=true");
            System.out.println("MAP_BYTES=" + file.length());
            System.out.println("MAP_START_ZOOM=" + zoom);
            System.out.println("CITY_SAMPLE_POINTS_VALIDATED=" + count);
            System.out.println("MAP_CENTER=" + center.latitude + "," + center.longitude);
        } finally {
            map.close();
        }
    }

    private static void assertReadable(MapFile map, LatLong point, byte zoom, String label) throws Exception {
        Tile tile = new Tile(
            MercatorProjection.longitudeToTileX(point.longitude, zoom),
            MercatorProjection.latitudeToTileY(point.latitude, zoom),
            zoom, 256
        );
        if (map.readMapData(tile) == null) throw new IllegalStateException("No readable map data for " + label);
    }
}
