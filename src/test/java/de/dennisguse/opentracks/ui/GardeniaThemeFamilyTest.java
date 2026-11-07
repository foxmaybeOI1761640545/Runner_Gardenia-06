package de.dennisguse.opentracks.ui;
import static org.junit.Assert.*;
import org.junit.Test;
import de.dennisguse.opentracks.ui.util.ThemeUtils;
public class GardeniaThemeFamilyTest {
    @Test public void exactlyFourDistinctNativeFamilies(){assertEquals(4,ThemeUtils.Family.values().length);assertEquals(4,java.util.Arrays.stream(ThemeUtils.Family.values()).map(f->f.homeLayout).distinct().count());}
    @Test public void invalidAndLegacyFamilyIdsUsePrecision(){assertEquals(ThemeUtils.Family.PRECISION,ThemeUtils.Family.fromId(null));assertEquals(ThemeUtils.Family.PRECISION,ThemeUtils.Family.fromId("5"));}
    @Test public void persistedIdsRoundTrip(){for(var f:ThemeUtils.Family.values())assertSame(f,ThemeUtils.Family.fromId(f.id));}
}
