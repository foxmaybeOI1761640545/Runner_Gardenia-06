package de.dennisguse.opentracks;

import android.app.Application;
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
        return super.newApplication(cl, TestApplication.class.getName(), context);
    }
}
