package de.dennisguse.opentracks.p0.app;

import java.util.ArrayList;
import java.util.List;

/** Read-only local projection. Never estimates distance or adds positions to a workout. */
public final class RouteGeometry {
    public record Point(double latitude,double longitude,boolean breakBefore) {
        public boolean valid(){return Double.isFinite(latitude)&&Double.isFinite(longitude)&&Math.abs(latitude)<=90&&Math.abs(longitude)<=180;}
    }
    public record ScreenPoint(float x,float y,boolean breakBefore) {}
    private RouteGeometry(){}
    private static double wrapped(double value){return ((value+540)%360)-180;}
    public static List<ScreenPoint> project(List<Point> points,float width,float height,float padding){
        List<Point> valid=points.stream().filter(Point::valid).toList();if(valid.isEmpty())return List.of();
        double latitude=valid.get(0).latitude(),longitude=valid.get(0).longitude();
        double cos=Math.max(.000001,Math.cos(Math.toRadians(latitude)));
        double minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY,minY=minX,maxY=maxX;
        for(Point p:valid){double x=wrapped(p.longitude()-longitude)*cos,y=latitude-p.latitude();minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}
        double dx=Math.max(.00008,maxX-minX),dy=Math.max(.00008,maxY-minY);
        double scale=Math.min(Math.max(1,width-2*padding)/dx,Math.max(1,height-2*padding)/dy);
        double centerX=(minX+maxX)/2,centerY=(minY+maxY)/2;List<ScreenPoint> result=new ArrayList<>();
        boolean gap=true;for(Point p:points){if(!p.valid()){gap=true;continue;}double x=wrapped(p.longitude()-longitude)*cos,y=latitude-p.latitude();result.add(new ScreenPoint((float)(width/2+(x-centerX)*scale),(float)(height/2+(y-centerY)*scale),gap||p.breakBefore()));gap=false;}
        return List.copyOf(result);
    }
    public static List<Point> bounded(List<Point> input,int budget){
        if(input.size()<=budget)return List.copyOf(input);
        // Keep endpoints of every retained segment; never join across discarded gap markers.
        int stride=(int)Math.ceil((double)input.size()/budget);List<Point> result=new ArrayList<>();boolean gap=true;
        for(int i=0;i<input.size();i++){
            Point p=input.get(i);gap|=p.breakBefore();
            if(i==0||i==input.size()-1||i%stride==0||p.breakBefore()||(i+1<input.size()&&input.get(i+1).breakBefore())){result.add(new Point(p.latitude(),p.longitude(),gap));gap=false;}
        }
        if(result.size()>budget*2){List<Point> capped=new ArrayList<>();boolean skippedGap=true;int step=(int)Math.ceil((double)result.size()/budget);for(int i=0;i<result.size();i++){Point p=result.get(i);skippedGap|=p.breakBefore();if(i%step==0||i==result.size()-1){capped.add(new Point(p.latitude(),p.longitude(),skippedGap));skippedGap=false;}}return List.copyOf(capped);}
        return List.copyOf(result);
    }
}
