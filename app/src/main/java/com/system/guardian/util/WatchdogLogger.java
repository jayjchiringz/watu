package com.system.guardian.util;

import android.content.Context;
import com.system.guardian.CrashLogger;

public class WatchdogLogger {
    public static void log(Context context, String msg) {
        CrashLogger.log(context, "WatchdogService", msg);
    }

    public static void logError(Context context, String error) {
        CrashLogger.log(context, "WatchdogService", "❌ " + error);
    }
}
