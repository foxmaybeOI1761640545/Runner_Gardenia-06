package de.dennisguse.opentracks;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Application;
import android.provider.Settings;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.util.TimeZone;

import androidx.test.runner.AndroidJUnitRunner;

public class TestRunner extends AndroidJUnitRunner {
    private TimeZone originalTimeZone;

    @Override
    public void onCreate(Bundle arguments) {
        originalTimeZone = TimeZone.getDefault();
        String timezone = arguments.getString("timezone", "Asia/Singapore");
        if (!"Asia/Singapore".equals(timezone)) {
            throw new IllegalArgumentException("Instrumentation requires Asia/Singapore");
        }
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));
        Log.i("TestRunner", "Default test timezone: " + TimeZone.getDefault().getID());
        super.onCreate(arguments);
    }

    @Override
    public void onStart() {
        // Set the managed device itself, not just java.util.TimeZone in this process.
        getUiAutomation().adoptShellPermissionIdentity(
                Manifest.permission.SET_TIME_ZONE, Manifest.permission.WRITE_SECURE_SETTINGS);
        try {
            Context target = getTargetContext();
            Settings.Global.putInt(target.getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 0);
            target.getSystemService(AlarmManager.class).setTimeZone("Asia/Singapore");
        } finally {
            getUiAutomation().dropShellPermissionIdentity();
        }
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Singapore"));
        super.onStart();
    }

    @Override
    public void onDestroy() {
        try {
            super.onDestroy();
        } finally {
            if (originalTimeZone != null) {
                TimeZone.setDefault(originalTimeZone);
            }
        }
    }

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return super.newApplication(cl, testApplicationClass(), context);
    }
    protected String testApplicationClass(){return TestApplication.class.getName();}
}
