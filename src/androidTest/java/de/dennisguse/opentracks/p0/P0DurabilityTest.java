package de.dennisguse.opentracks.p0;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import de.dennisguse.opentracks.Startup;
import de.dennisguse.opentracks.data.*;
import de.dennisguse.opentracks.data.models.*;
import de.dennisguse.opentracks.p0.data.WorkoutRepository;
import de.dennisguse.opentracks.util.TestEnvironmentUtils;

/** Default suite: logical round trip. Dedicated file Runner: two real processes separated by force-stop. */
public class P0DurabilityTest {
    private static final UUID FIXTURE=UUID.fromString("3fbde951-3aa1-4ec3-815b-7a969da9707a");
    private static final Instant BASE=Instant.parse("2026-11-04T02:00:00Z");
    private final Context context=ApplicationProvider.getApplicationContext();
    private void write() {
        TestEnvironmentUtils.resetTrackRecordingServiceAndDeleteTracks();
        var store=new ContentProviderUtils(context);
        Track.Id id=store.insertTrack(new Track(null,FIXTURE,"Synthetic durable workout","","",ActivityType.RUNNING,ZoneOffset.UTC,Statistics.DEFAULT));
        for(int second=1;second<=2;second++) {
            store.insertTrackPoint(new TrackPoint(TrackPoint.Type.TRACKPOINT,new Position(BASE.plusSeconds(second),1.3+second*0.0001,103.8,Distance.of(2),Altitude.WGS84.of(12),null,null,Speed.of(3))),id);
            HeartRateSample sample=new HeartRateSample(BASE.plusSeconds(second),"AA:BB:CC:DD:EE:FF","Synthetic durable strap",true,12,List.of(1024,1000)).captured(BASE.plusSeconds(second),"AA:BB:CC:DD:EE:FF","Synthetic durable strap","durability-session");
            new HeartRateRepository(context).insert(id,new HeartRate(76,sample));
        }
    }
    private Track.Id id() {
        Track track=new ContentProviderUtils(context).getTrack(FIXTURE);
        assertNotNull("Durable workout UUID must survive reopening",track);
        return track.id();
    }
    private void read() {
        var id=id();var repository=new WorkoutRepository(context);var rows=repository.heartRates(id);
        assertEquals(2,rows.size());assertEquals(List.of(BASE.plusSeconds(1),BASE.plusSeconds(2)),rows.stream().map(value->value.sample().capturedAt()).toList());
        assertNotEquals(rows.get(0).sample().sampleId(),rows.get(1).sample().sampleId());
        assertEquals("durability-session",rows.get(1).sample().connectionId());assertEquals(List.of(1024,1000),rows.get(1).sample().rrIntervals1024());
        assertEquals("AA:BB:CC:DD:EE:FF",rows.get(1).sample().deviceAddress());assertEquals(Boolean.TRUE,rows.get(1).sample().contactDetected());assertEquals(Integer.valueOf(12),rows.get(1).sample().energyExpended());
        int[] points={0};repository.forEachPoint(id,point->{assertTrue(point.position().hasLocation());assertEquals(Distance.of(2),point.position().horizontalAccuracy());points[0]++;});assertEquals(2,points[0]);
        assertEquals(76,repository.find(id).heartRate().average(),0);
    }
    @Test public void committedGpsAndRawHeartRateRemainReadable() {
        String phase=InstrumentationRegistry.getArguments().getString("p0DurabilityPhase","both");
        assertTrue(List.of("both","write","read").contains(phase));assertEquals("Asia/Singapore",java.util.TimeZone.getDefault().getID());
        if(!phase.equals("read"))write();
        if(!phase.equals("write"))read();
        if("true".equals(InstrumentationRegistry.getArguments().getString("p05MetricProbe"))){
            var families=de.dennisguse.opentracks.ui.util.ThemeUtils.Family.values();
            var originalFamily=de.dennisguse.opentracks.settings.PreferencesUtils.getGardeniaTheme();
            for(var family:families){
                var expected=de.dennisguse.opentracks.p0.app.DashboardMetricLayout.defaults(family==de.dennisguse.opentracks.ui.util.ThemeUtils.Family.NEON).swapSecondary(family.ordinal()%3);
                if(phase.equals("write"))de.dennisguse.opentracks.settings.PreferencesUtils.setDashboardMetricLayout(family,expected);
                else {
                    assertEquals(expected,de.dennisguse.opentracks.settings.PreferencesUtils.getDashboardMetricLayout(family));
                    de.dennisguse.opentracks.settings.PreferencesUtils.setGardeniaTheme(family);
                    try(var screen=androidx.test.core.app.ActivityScenario.launch(de.dennisguse.opentracks.p0.app.P0Activity.class)){
                        screen.onActivity(a->{assertEquals(expected,de.dennisguse.opentracks.settings.PreferencesUtils.getDashboardMetricLayout(family));int label=switch(expected.primary()){case DISTANCE->de.dennisguse.opentracks.R.string.p02_distance;case DURATION->de.dennisguse.opentracks.R.string.p02_time;case HEART_RATE->de.dennisguse.opentracks.R.string.p02_heart_heading;case PACE_SPEED->de.dennisguse.opentracks.settings.PreferencesUtils.isReportSpeed(de.dennisguse.opentracks.settings.PreferencesUtils.getDefaultActivityTypeLocalized())?de.dennisguse.opentracks.R.string.p02_speed:de.dennisguse.opentracks.R.string.p02_pace;};assertEquals(a.getString(label),((android.widget.TextView)a.findViewById(de.dennisguse.opentracks.R.id.p05_primary_label)).getText().toString());});
                    }
                }
            }
            de.dennisguse.opentracks.settings.PreferencesUtils.setGardeniaTheme(originalFamily);
            var prefs=androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
            String source=InstrumentationRegistry.getArguments().getString("buildCommit");
            if(phase.equals("write"))assertTrue(prefs.edit().putString("p05_metric_probe_source",source).commit());
            else {assertEquals(source,prefs.getString("p05_metric_probe_source",null));for(var family:families)de.dennisguse.opentracks.settings.PreferencesUtils.resetDashboardMetricLayout(family);}
        }

        if(!phase.equals("both")) {
            assertEquals("p0-durability.db",((Startup)context.getApplicationContext()).getDatabaseName());
            try(android.database.sqlite.SQLiteDatabase independent=android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath("p0-durability.db").getPath(),null,android.database.sqlite.SQLiteDatabase.OPEN_READONLY);
                var cursor=independent.rawQuery("SELECT COUNT(*) FROM heart_rate_samples WHERE trackid=?",new String[]{Long.toString(id().id())})) {
                assertTrue(cursor.moveToFirst());assertEquals(2,cursor.getLong(0));assertEquals(43,independent.getVersion());
            }
            android.os.Bundle status=new android.os.Bundle();status.putString("p0DurabilityPid",Integer.toString(android.os.Process.myPid()));
            status.putString("p0DurabilityPhase",phase);InstrumentationRegistry.getInstrumentation().sendStatus(0,status);
        }
    }
}
