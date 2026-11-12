// SPDX-License-Identifier: CC0-1.0
// Self-authored synthetic geometry, not derived from OSM or user tracks.
import java.io.File;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.mapsforge.map.writer.*;
import org.mapsforge.map.writer.model.*;
import org.openstreetmap.osmosis.core.domain.v0_6.*;

public final class GenerateMap {
    public static void main(String[] args)throws Exception{
        MapWriterConfiguration c=new MapWriterConfiguration();
        c.addOutputFile(args[1]);c.loadTagMappingFile(null);c.addBboxConfiguration("1.295,103.795,1.305,103.805");
        c.addZoomIntervalConfiguration(null);c.addMapStartPosition("1.300,103.800");c.addMapStartZoom("16");
        c.setWriterVersion("Gardenia synthetic fixture / Mapsforge 0.25.0");c.setFileSpecificationVersion(3);
        c.setComment("Self-authored fictional park. CC0-1.0. Not for navigation.");
        c.setPolygonClipping(true);c.setWayClipping(true);c.setBboxEnlargement(20);c.setSimplification(0);c.setSimplificationMaxZoom((byte)12);c.addEncodingChoice("auto");c.setThreads(1);c.validate();
        var store=RAMTileBasedDataProcessor.newInstance(c);
        var factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        var xml=factory.newDocumentBuilder().parse(new File(args[0]));
        var nodes=xml.getElementsByTagName("node");
        for(int i=0;i<nodes.getLength();i++){Element n=(Element)nodes.item(i);store.addNode(new Node(Long.parseLong(n.getAttribute("id")),1,new Date(0),OsmUser.NONE,0,List.of(),Double.parseDouble(n.getAttribute("lat")),Double.parseDouble(n.getAttribute("lon"))));}
        var ways=xml.getElementsByTagName("way");
        for(int i=0;i<ways.getLength();i++){Element w=(Element)ways.item(i);List<WayNode> refs=new ArrayList<>();List<Tag> tags=new ArrayList<>();var children=w.getChildNodes();for(int j=0;j<children.getLength();j++)if(children.item(j) instanceof Element e){if(e.getTagName().equals("nd"))refs.add(new WayNode(Long.parseLong(e.getAttribute("ref"))));else if(e.getTagName().equals("tag"))tags.add(new Tag(e.getAttribute("k"),e.getAttribute("v")));}store.addWay(new Way(Long.parseLong(w.getAttribute("id")),1,new Date(0),OsmUser.NONE,0,tags,refs));}
        store.complete();MapFileWriter.writeFile(c,store);store.close();MapFileWriter.release();
    }
}
