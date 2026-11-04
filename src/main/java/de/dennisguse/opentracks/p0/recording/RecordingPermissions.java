package de.dennisguse.opentracks.p0.recording;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import androidx.core.content.ContextCompat;
import de.dennisguse.opentracks.util.PermissionRequester;

public final class RecordingPermissions {
    private RecordingPermissions() {}
    public static boolean hasBluetooth(Context context) {
        String permission=Build.VERSION.SDK_INT>=31?Manifest.permission.BLUETOOTH_CONNECT:Manifest.permission.BLUETOOTH;
        return ContextCompat.checkSelfPermission(context,permission)==PackageManager.PERMISSION_GRANTED;
    }
    public static boolean hasAny(Context context) {
        return PermissionRequester.GPS.hasPermission(context)||hasBluetooth(context);
    }
    public static int foregroundTypes(Context context) {
        return types(ForegroundMode.select(PermissionRequester.GPS.hasPermission(context),hasBluetooth(context)));
    }
    public static int foregroundTypes(Context context,boolean recording) {
        return types(ForegroundMode.select(recording&&PermissionRequester.GPS.hasPermission(context),hasBluetooth(context)));
    }
    public static int types(ForegroundMode mode) {
        return switch(mode) {
            case NONE -> 0;
            case GPS -> ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
            case BLUETOOTH -> ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;
            case GPS_AND_BLUETOOTH -> ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION|ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;
        };
    }
}
