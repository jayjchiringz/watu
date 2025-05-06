package com.system.guardian.util;

import android.app.ActivityManager;
import android.content.Context;

import com.system.guardian.CrashLogger;

import java.io.File;
import java.io.IOException;

public class AppUtils {

    private static final String TAG = "AppUtils";

    public static boolean isAppRunning(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            for (ActivityManager.RunningAppProcessInfo proc : am.getRunningAppProcesses()) {
                if (proc.processName.equals(packageName)) {
                    CrashLogger.log(context, TAG, "✅ App running: " + packageName);
                    return true;
                }
            }
        }
        CrashLogger.log(context, TAG, "❌ App not running: " + packageName);
        return false;
    }

    public static void forceStopApp(Context context, String packageName) {
        try {
            CrashLogger.log(context, TAG, "🛑 Attempting to stop app: " + packageName);
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(packageName);
            }

            if (isDeviceRooted()) {
                Runtime.getRuntime().exec(new String[]{"su", "-c", "am force-stop " + packageName});
                CrashLogger.log(context, TAG, "✔️ Root force-stop issued: " + packageName);
            } else {
                Runtime.getRuntime().exec("am force-stop " + packageName);
                CrashLogger.log(context, TAG, "✔️ Standard force-stop issued: " + packageName);
            }

        } catch (IOException e) {
            CrashLogger.log(context, TAG, "⚠️ Failed to force-stop " + packageName + ": " + e.getMessage());
        }
    }

    public static boolean isDeviceRooted() {
        String[] paths = {
                "/system/xbin/su", "/system/bin/su", "/system/sbin/su",
                "/sbin/su", "/su/bin/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }
}
