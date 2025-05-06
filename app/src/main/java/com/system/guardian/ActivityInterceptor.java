package com.system.guardian;

import android.content.Context;
import android.util.Log;
import android.app.Activity;

public class ActivityInterceptor {

    private static String lastKilledActivity = "";
    private static long lastKillTime = 0;

    public static void killActivity(Context context, String activityName) {
        long now = System.currentTimeMillis();
        if (!activityName.equals(lastKilledActivity) || now - lastKillTime > 5000) { // 5 seconds
            CrashLogger.log(context, "ActivityInterceptor", "👻 GhostMode Killing: " + activityName, Log.WARN);
            lastKilledActivity = activityName;
            lastKillTime = now;
        }
    }

    public static void check(Activity activity) {
        /*
        if (GuardianStateCache.isGhostModeEnabled) {
            // If ghost mode is active, immediately kill the activity to stay hidden
            activity.finish();
        }
        */
    }
}
