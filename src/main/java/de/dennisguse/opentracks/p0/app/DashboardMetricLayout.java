package de.dennisguse.opentracks.p0.app;
import java.util.*;
/** Immutable ordered positions. Changes have no recording or sensor responsibilities. */
public record DashboardMetricLayout(List<DashboardMetric> positions) {
 public DashboardMetricLayout {
  positions=List.copyOf(positions);
  if(positions.size()!=4 || new HashSet<>(positions).size()!=4)throw new IllegalArgumentException("Four unique metrics required");
 }
 public static DashboardMetricLayout defaults(boolean heartFirst){return new DashboardMetricLayout(heartFirst?List.of(DashboardMetric.HEART_RATE,DashboardMetric.DISTANCE,DashboardMetric.DURATION,DashboardMetric.PACE_SPEED):List.of(DashboardMetric.DISTANCE,DashboardMetric.DURATION,DashboardMetric.PACE_SPEED,DashboardMetric.HEART_RATE));}
 public DashboardMetric primary(){return positions.get(0);}
 public DashboardMetric secondary(int slot){if(slot<0||slot>2)throw new IllegalArgumentException("Invalid secondary slot");return positions.get(slot+1);}
 public DashboardMetricLayout swapSecondary(int slot){secondary(slot);var next=new ArrayList<>(positions);Collections.swap(next,0,slot+1);return new DashboardMetricLayout(next);}
 public String serialize(){return String.join(",",positions.stream().map(Enum::name).toList());}
 public static DashboardMetricLayout restoreOrDefault(String stored,boolean heartFirst){try{return new DashboardMetricLayout(Arrays.stream(stored.split(",",-1)).map(DashboardMetric::valueOf).toList());}catch(RuntimeException failure){return defaults(heartFirst);}}
}
