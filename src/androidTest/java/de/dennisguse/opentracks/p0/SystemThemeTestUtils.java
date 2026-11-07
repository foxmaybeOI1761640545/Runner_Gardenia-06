package de.dennisguse.opentracks.p0;
import android.os.ParcelFileDescriptor;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
public final class SystemThemeTestUtils {
    private SystemThemeTestUtils(){}
    public static String shell(String command)throws Exception {
        try(ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);var input=new FileInputStream(fd.getFileDescriptor())){return new String(input.readAllBytes(),StandardCharsets.UTF_8).trim();}
    }
    public static String savedMode()throws Exception{return shell("settings get secure ui_night_mode");}
    public static void restore(String value)throws Exception{shell("cmd uimode night "+(value.equals("2")?"yes":value.equals("1")?"no":"auto"));}
    public static void night(boolean night)throws Exception{shell("cmd uimode night "+(night?"yes":"no"));}
}
