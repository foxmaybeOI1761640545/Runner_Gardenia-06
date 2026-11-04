package de.dennisguse.opentracks.p0;

import android.graphics.Bitmap;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Captures actual emulator pixels; synthetic data is identified by the test and filename. */
public final class P0UiCapture {
    private P0UiCapture() {}
    public static void save(String name) throws IOException {
        var instrumentation=InstrumentationRegistry.getInstrumentation();instrumentation.waitForIdleSync();
        android.os.SystemClock.sleep(300);
        String configured=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
        android.content.Context context=ApplicationProvider.getApplicationContext();
        File directory=new File(configured!=null?new File(configured):new File(context.getExternalMediaDirs()[0],"additional_test_output"),"p0-ui");
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create UI evidence directory");
        Bitmap image=instrumentation.getUiAutomation().takeScreenshot();
        if(image==null)throw new IOException("No emulator screenshot");
        try(FileOutputStream stream=new FileOutputStream(new File(directory,name+".png"))) {
            if(!image.compress(Bitmap.CompressFormat.PNG,100,stream))throw new IOException("Cannot save screenshot");
        }finally{image.recycle();}
    }
}
